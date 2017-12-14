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

package io.crate.testing.udf;

import io.crate.data.Input;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.FunctionInfo;
import io.crate.metadata.Scalar;
import io.crate.operation.udf.UserDefinedFunctionMetaData;
import io.crate.types.DataTypes;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.lucene.BytesRefs;

public class DummyFunction<InputType> extends Scalar<BytesRef, InputType> {

    private final FunctionInfo info;
    private final UserDefinedFunctionMetaData metaData;

    DummyFunction(UserDefinedFunctionMetaData metaData) {
        this.info = new FunctionInfo(new FunctionIdent(metaData.schema(), metaData.name(), metaData.argumentTypes()), DataTypes.STRING);
        this.metaData = metaData;
    }

    @Override
    public FunctionInfo info() {
        return info;
    }

    @Override
    public BytesRef evaluate(Input<InputType>... args) {
        // dummy-lang functions simple print the type of the only argument
        return BytesRefs.toBytesRef("DUMMY EATS " + metaData.argumentTypes().get(0).getName());
    }
}
