/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

package io.crate.operation.collect.files;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CSVLineParser {

    private static CsvMapper csvMapper = new CsvMapper();
    private static List<Object> keyList;

    public static void parseHeader(String header) throws IOException {
        keyList = csvMapper.enable(CsvParser.Feature.TRIM_SPACES).readerWithSchemaFor(String.class).readValues(header).readAll();

        Set<Object> set  = new HashSet<>(keyList);
        keyList.removeAll(Collections.singleton(""));

        if (set.size() != keyList.size()) {
            throw new IllegalArgumentException("Invalid keys!");
        }
    }

    public static byte[] parse(String header, String row) throws IOException {
        List<Object> recordList = csvMapper.enable(CsvParser.Feature.TRIM_SPACES).readerWithSchemaFor(String.class).readValues(row).readAll();
        return convertCSVToJson(keyList, recordList);

    }

    private static byte[] convertCSVToJson(List<Object> keyList, List<Object> recordList) throws JsonProcessingException {

        if (recordList.isEmpty()) {
            return new ObjectMapper().writeValueAsBytes(Collections.emptyMap());
        }

        if (keyList.size() != recordList.size()) {
            throw new IllegalArgumentException("Number of row entries is not equal to the number of columns");
        }

        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < keyList.size(); i++) {
            map.put(keyList.get(i).toString(), recordList.get(i).toString());
        }

        return new ObjectMapper().writeValueAsBytes(map);
    }



}



