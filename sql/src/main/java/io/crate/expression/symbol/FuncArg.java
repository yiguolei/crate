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

package io.crate.expression.symbol;

import io.crate.types.DataType;

/**
 * A single argument of a function call.
 */
public interface FuncArg {

    /**
     * Returns the {@link DataType} of this {@link Function} argument.
     * @return The DataType of the value.
     */
    DataType valueType();

    /**
     * Indicates whether a Symbol can be casted or not.
     * Typically, we only allow casting of Literals, but for example
     * even if casting a column to the Literal type is not allowed,
     * casting two columns when used on both sides of an operator or
     * function is allowed.
     * Note: Convertibility checks have to be performed nevertheless.
     *       This just indicates whether casting is allowed.
     * @return True is casting is possible, false otherwise.
     */
    boolean canBeCasted();
}
