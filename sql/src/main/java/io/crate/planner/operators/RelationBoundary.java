/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.planner.operators;

import io.crate.analyze.OrderBy;
import io.crate.analyze.relations.AbstractTableRelation;
import io.crate.analyze.relations.QueriedRelation;
import io.crate.analyze.symbol.Field;
import io.crate.analyze.symbol.Symbol;
import io.crate.planner.MultiPhasePlan;
import io.crate.planner.Plan;
import io.crate.planner.Planner;
import io.crate.planner.SubqueryPlanner;
import io.crate.planner.projection.builder.ProjectionBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static io.crate.planner.operators.LogicalPlanner.extractColumns;

/**
 * An Operator that marks the boundary of a relation.
 * In relational algebra terms this is a "no-op" operator - it doesn't apply any modifications on the source relation.
 *
 * It is used to take care of the field mapping (providing {@link LogicalPlan#expressionMapping()})
 * In addition it takes care of MultiPhase planning.
 */
public class RelationBoundary implements LogicalPlan {

    final LogicalPlan source;
    private final List<Symbol> outputs;
    private final HashMap<Symbol, Symbol> expressionMapping;
    private final QueriedRelation relation;

    public static LogicalPlan.Builder create(LogicalPlan.Builder sourceBuilder, QueriedRelation relation) {
        return (tableStats, usedBeforeNextFetch) -> {

            /*
             * Need to:
             *     a) Translate usedColumns
             *
             *     b) If columns are fetched later:
             *        Safe scalars to expressionMapping() so that they can be applied after the fetch
             *                                                                                  *
             *     c) If columns are immediately collected: Apply the scalar as part of a eval projection,
             *        So that parent operators have the values available
             *
             * Example:
             *
             * select xx, yy from (select x + x as xx, y + y as yy from t1 ...) tt where xx = 10
             *                     ^^^^^^^^^^^^^^^^^^
             *                     RelationBoundary for this
             *
             * relation.fields:     [ F.xx, F.yy ]
             * relation.outputs:    [ R.x + R.x, R.y + F.y ]
             * usedBeforeNextFetch: [ F.xx ]                        -- translated and unique cols extracted --> R.x
             * expressionMapping:   { F.xx: R.x + R.x,
             *                        F.yy: R.y + R.y }
             *
             * sourceOutputs:       [ R._fetchId, R.x ]
             *                              | evalProjection ( IC(0), add(IC(1), IC(1))
             *                              |
             * outputs:             [ F._fetchId, F.xx ]            -- mappedSource columns + usedColumns
             */
            HashMap<Symbol, Symbol> expressionMapping = new HashMap<>();
            for (Field field : relation.fields()) {
                expressionMapping.put(
                    field,
                    ((QueriedRelation) field.relation()).querySpec().outputs().get(field.index()));
            }
            Function<Symbol, Symbol> mapper = OperatorUtils.getMapper(expressionMapping);
            HashSet<Symbol> mappedUsedColumns = new HashSet<>();
            for (Symbol beforeNextFetch : usedBeforeNextFetch) {
                mappedUsedColumns.add(mapper.apply(beforeNextFetch));
            }
            return new RelationBoundary(
                sourceBuilder.build(tableStats, extractColumns(mappedUsedColumns)),
                relation,
                usedBeforeNextFetch,
                expressionMapping
            );
        };
    }

    private RelationBoundary(LogicalPlan source,
                             QueriedRelation relation,
                             Set<Symbol> usedBeforeNextFetch,
                             Map<Symbol, Symbol> expressionMapping) {
        this.expressionMapping = new HashMap<>();
        this.expressionMapping.putAll(expressionMapping);
        this.expressionMapping.putAll(source.expressionMapping());
        this.source = source;
        this.relation = relation;
        this.outputs = OperatorUtils.mappedSymbols(new ArrayList<>(usedBeforeNextFetch), expressionMapping);
    }

    private RelationBoundary(LogicalPlan source,
                            QueriedRelation relation,
                            List<Symbol> outputs,
                            HashMap<Symbol, Symbol> expressionMapping) {
        this.expressionMapping = expressionMapping;
        this.source = source;
        this.relation = relation;
        this.outputs = outputs;
    }

    @Override
    public Plan build(Planner.Context plannerContext,
                      ProjectionBuilder projectionBuilder,
                      int limit,
                      int offset,
                      @Nullable OrderBy order,
                      @Nullable Integer pageSizeHint) {
        SubqueryPlanner subqueryPlanner = new SubqueryPlanner(plannerContext);
        return MultiPhasePlan.createIfNeeded(
            source.build(plannerContext, projectionBuilder, limit, offset, order, pageSizeHint),
            subqueryPlanner.planSubQueries(relation.querySpec())
        );
    }

    @Override
    public LogicalPlan tryCollapse() {
        LogicalPlan collapsed = source.tryCollapse();
        if (collapsed == source) {
            return this;
        }
        return new RelationBoundary(source, relation, outputs, expressionMapping);
    }

    @Override
    public List<Symbol> outputs() {
        return outputs;
    }

    @Override
    public Map<Symbol, Symbol> expressionMapping() {
        return expressionMapping;
    }

    @Override
    public List<AbstractTableRelation> baseTables() {
        return source.baseTables();
    }

    @Override
    public long numExpectedRows() {
        return source.numExpectedRows();
    }

    @Override
    public String toString() {
        return "Boundary{" + source + '}';
    }
}
