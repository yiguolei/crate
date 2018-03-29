/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.metadata.information;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.crate.metadata.ColumnIdent;
import io.crate.metadata.RowContextCollectorExpression;
import io.crate.metadata.RowGranularity;
import io.crate.metadata.TableIdent;
import io.crate.metadata.expressions.RowCollectExpressionFactory;
import io.crate.metadata.table.ColumnRegistrar;
import io.crate.metadata.table.SchemaInfo;
import io.crate.types.DataTypes;

import java.util.Map;

public class InformationSchemataTableInfo extends InformationTableInfo {

    public static final String NAME = "schemata";
    public static final TableIdent IDENT = new TableIdent(InformationSchemaInfo.NAME, NAME);

    public static class Columns {
        static final ColumnIdent SCHEMA_NAME = new ColumnIdent("schema_name");
    }

    public static Map<ColumnIdent, RowCollectExpressionFactory<SchemaInfo>> expressions() {
        return ImmutableMap.<ColumnIdent, RowCollectExpressionFactory<SchemaInfo>>builder()
            .put(Columns.SCHEMA_NAME,
                () -> RowContextCollectorExpression.objToBytesRef(SchemaInfo::name)).build();
    }

    private static ColumnRegistrar columnRegistrar() {
        return new ColumnRegistrar(IDENT, RowGranularity.DOC)
            .register(Columns.SCHEMA_NAME, DataTypes.STRING);
    }

    InformationSchemataTableInfo() {
        super(
            IDENT,
            columnRegistrar(),
            ImmutableList.of(Columns.SCHEMA_NAME)
        );
    }
}
