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

package io.crate.metadata.view;

import io.crate.metadata.TableIdent;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.cluster.AbstractNamedDiffable;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ViewsMetaData extends AbstractNamedDiffable<MetaData.Custom> implements MetaData.Custom {

    public static final String TYPE = "views";
    private final Map<String, String> queryByName;

    ViewsMetaData(Map<String, String> queryByName) {
        this.queryByName = queryByName;
    }

    public ViewsMetaData(StreamInput in) throws IOException {
        int numViews = in.readVInt();
        queryByName = new HashMap<>(numViews);
        for (int i = 0; i < numViews; i++) {
            queryByName.put(in.readString(), in.readString());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(queryByName.size());
        for (Map.Entry<String, String> view : queryByName.entrySet()) {
            out.writeString(view.getKey());
            out.writeString(view.getValue());
        }
    }

    @Override
    public EnumSet<MetaData.XContentContext> context() {
        return EnumSet.of(MetaData.XContentContext.GATEWAY, MetaData.XContentContext.SNAPSHOT);
    }

    @Override
    public String getWriteableName() {
        return TYPE;
    }


    /*
     * ViewsMetaData XContent has the following structure:
     *
     * <pre>
     *     {
     *       "views": {
     *         "docs.my_view": {
     *           "stmt": "select x, y from t1 where z = 'a'"
     *         }
     *       }
     *     }
     * </pre>
     *
     * Where docs.my_view is the full qualified name of the view
     * and the value of "stmt" is the analyzed SELECT statement.
     */
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(TYPE);
        for (Map.Entry<String, String> entry : queryByName.entrySet()) {
            builder.startObject(entry.getKey());
            {
                builder.field("stmt", entry.getValue());
            }
            builder.endObject();
        }
        builder.endObject();
        return builder;
    }

    public static ViewsMetaData fromXContent(XContentParser parser) throws IOException {
        Map<String, String> views = new HashMap<>();

        if (parser.nextToken() == XContentParser.Token.FIELD_NAME && parser.currentName().equals(TYPE)) {
            if (parser.nextToken() == XContentParser.Token.START_OBJECT) {
                while (parser.nextToken() == XContentParser.Token.FIELD_NAME) {
                    String viewName = parser.currentName();
                    if (parser.nextToken() == XContentParser.Token.START_OBJECT) {
                        while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                            if ("stmt".equals(parser.currentName())) {
                                parser.nextToken();
                                views.put(viewName, parser.text());
                            }
                        }
                    }
                }
            }
            if (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                // each custom metadata is packed inside an object.
                // each custom must move the parser to the end otherwise possible following customs won't be read
                throw new ElasticsearchParseException("failed to parse views, expected an object token at the end");
            }
        }
        return new ViewsMetaData(views);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewsMetaData that = (ViewsMetaData) o;
        return queryByName.equals(that.queryByName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryByName);
    }

    public boolean contains(String name) {
        return queryByName.containsKey(name);
    }

    public Iterable<String> names() {
        return queryByName.keySet();
    }

    /**
     * @return A copy of the ViewsMetaData with the new view added (or replaced in case it already existed)
     */
    public static ViewsMetaData addOrReplace(@Nullable ViewsMetaData prevViews, TableIdent name, String query) {
        HashMap<String, String> queryByName;
        if (prevViews == null) {
            queryByName = new HashMap<>();
        } else {
            queryByName = new HashMap<>(prevViews.queryByName);
        }
        queryByName.put(name.fqn(), query);
        return new ViewsMetaData(queryByName);
    }

    public RemoveResult remove(List<TableIdent> names) {
        HashMap<String, String> updatedQueryByName = new HashMap<>(this.queryByName);
        ArrayList<TableIdent> missing = new ArrayList<>(names.size());
        for (TableIdent name : names) {
            String removed = updatedQueryByName.remove(name.fqn());
            if (removed == null) {
                missing.add(name);
            }
        }
        return new RemoveResult(new ViewsMetaData(updatedQueryByName), missing);
    }

    @Nullable
    public String getStatement(TableIdent name) {
        return queryByName.get(name.fqn());
    }

    public class RemoveResult {
        private final ViewsMetaData updatedViews;
        private final List<TableIdent> missing;

        RemoveResult(ViewsMetaData updatedViews, List<TableIdent> missing) {
            this.updatedViews = updatedViews;
            this.missing = missing;
        }

        public ViewsMetaData updatedViews() {
            return updatedViews;
        }

        public List<TableIdent> missing() {
            return missing;
        }
    }
}
