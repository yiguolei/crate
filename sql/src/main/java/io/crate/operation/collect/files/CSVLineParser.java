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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CSVLineParser {

    private static CsvMapper csvMapper = new CsvMapper();
    private static List<Object> keyList;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static void parseHeader(String header) throws IOException {
        keyList = csvMapper
            .enable(CsvParser.Feature.TRIM_SPACES)
            .readerWithSchemaFor(String.class)
            .readValues(header.getBytes(StandardCharsets.UTF_8))
            .readAll();

        Set<Object> keySet  = new HashSet<>(keyList);
        if (keySet.contains("")) {
            keySet.removeAll(Collections.singleton(""));
        }

        if (keySet.size() != keyList.size()) {
            throw new IllegalArgumentException("Invalid keys!");
        }
    }

    public static byte[] parse(String row) throws IOException {
        List<Object> recordList = csvMapper
            .enable(CsvParser.Feature.TRIM_SPACES)
            .readerWithTypedSchemaFor(String.class)
            .readValues(row.getBytes(StandardCharsets.UTF_8))
            .readAll();

        return convertCSVToMap(recordList);
    }

    private static byte[] convertCSVToMap(List<Object> recordList) throws JsonProcessingException {

        if (recordList.isEmpty()) {
            return objectMapper.writeValueAsBytes(Collections.emptyMap());
        }

        if (keyList == null || keyList.size() != recordList.size()) {
            throw new IllegalArgumentException("Number of row entries is not equal to the number of columns");
        }

        HashMap<Object, Object> csvAsMap = new HashMap<>();
        for (int i = 0; i < keyList.size(); i++) {
            csvAsMap.put(keyList.get(i), recordList.get(i));
        }
        return objectMapper.writeValueAsBytes(csvAsMap);
    }
}
