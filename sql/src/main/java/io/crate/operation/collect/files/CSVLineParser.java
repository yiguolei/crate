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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class CSVLineParser {

    private static Set<String> keys;
    private static List<String> keyList;

    public static void parseHeader(byte[] header) throws IOException {
        Reader headerReader = new InputStreamReader(new ByteArrayInputStream(header), StandardCharsets.UTF_8);
        CSVParser headerParser = new CSVParser(headerReader, CSVFormat.DEFAULT.withTrim().withFirstRecordAsHeader());
        try {

            keys = headerParser.getHeaderMap().keySet();
            keyList = getListOfKeys(keys);
        } finally {
            headerReader.close();
            headerParser.close();
        }
    }

    public static byte[] parse(byte[] header, byte[] row) throws IOException {
        Reader rowReader = new InputStreamReader(new ByteArrayInputStream(row), StandardCharsets.UTF_8);

        CSVParser rowParser = CSVFormat
            .DEFAULT.withTrim().withFirstRecordAsHeader()
            .parse(rowReader);

        Set<String> records = rowParser.getHeaderMap().keySet();
        List<String> recordList = new ArrayList<>(records);

        try {
            return convertCSVToJson(keyList, recordList);
        } finally {
            rowReader.close();
            rowParser.close();
        }
    }

    private static byte[] convertCSVToJson(List<String> keyList, List<String> recordList) throws JsonProcessingException {
        if (recordList.isEmpty()) {
            return new ObjectMapper().writeValueAsBytes(Collections.emptyMap());
        }

        if (keyList.size() != recordList.size()) {
            throw new IllegalArgumentException("Number of row entries is not equal to the number of columns");
        }

        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            map.put(keyList.get(i), recordList.get(i));
        }

        return new ObjectMapper().writeValueAsBytes(map);
    }

    private static List<String> getListOfKeys(Set<String> keys) {
        keys.removeIf(item -> item == null || "".equals(item));
        return new ArrayList<>(keys);
    }

}



