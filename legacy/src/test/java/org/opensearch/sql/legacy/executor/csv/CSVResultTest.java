/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 *   Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package org.opensearch.sql.legacy.executor.csv;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Unit tests for {@link CSVResult}
 */
public class CSVResultTest {

    private static final String SEPARATOR = ",";

    @Test
    public void getHeadersShouldReturnHeadersSanitized() {
        CSVResult csv = csv(headers("name", "=age"), lines(line("John", "30")));
        assertEquals(
            headers("name", "'=age"),
            csv.getHeaders()
        );
    }

    @Test
    public void getLinesShouldReturnLinesSanitized() {
        CSVResult csv = csv(
            headers("name", "city"),
            lines(
                line("John", "Seattle"),
                line("John", "=Seattle"),
                line("John", "+Seattle"),
                line("-John", "Seattle"),
                line("@John", "Seattle"),
                line("John", "Seattle=")
            )
        );

        assertEquals(
            line(
                "John,Seattle",
                "John,'=Seattle",
                "John,'+Seattle",
                "'-John,Seattle",
                "'@John,Seattle",
                "John,Seattle="
            ),
            csv.getLines()
        );
    }

    @Test
    public void getHeadersShouldReturnHeadersQuotedIfRequired() {
        CSVResult csv = csv(headers("na,me", ",,age"), lines(line("John", "30")));
        assertEquals(
            headers("\"na,me\"", "\",,age\""),
            csv.getHeaders()
        );
    }

    @Test
    public void getLinesShouldReturnLinesQuotedIfRequired() {
        CSVResult csv = csv(headers("name", "age"), lines(line("John,Smith", "30,,,")));
        assertEquals(
            line("\"John,Smith\",\"30,,,\""),
            csv.getLines()
        );
    }

    @Test
    public void getHeadersShouldReturnHeadersBothSanitizedAndQuotedIfRequired() {
        CSVResult csv = csv(headers("na,+me", ",,,=age", "=city,"), lines(line("John", "30", "Seattle")));
        assertEquals(
            headers("\"na,+me\"", "\",,,=age\"", "\"'=city,\""),
            csv.getHeaders()
        );
    }

    @Test
    public void getLinesShouldReturnLinesBothSanitizedAndQuotedIfRequired() {
        CSVResult csv = csv(
            headers("name", "city"),
            lines(
                line("John", "Seattle"),
                line("John", "=Seattle"),
                line("John", "+Sea,ttle"),
                line(",-John", "Seattle"),
                line(",,,@John", "Seattle"),
                line("John", "Seattle=")
            )
        );

        assertEquals(
            line(
                "John,Seattle",
                "John,'=Seattle",
                "John,\"'+Sea,ttle\"",
                "\",-John\",Seattle",
                "\",,,@John\",Seattle",
                "John,Seattle="
            ),
            csv.getLines()
        );
    }

    private CSVResult csv(List<String> headers, List<List<String>> lines) {
        return new CSVResult(SEPARATOR, headers, lines);
    }

    private List<String> headers(String... headers) {
        return Arrays.stream(headers).collect(Collectors.toList());
    }

    private List<String> line(String... line) {
        return Arrays.stream(line).collect(Collectors.toList());
    }

    @SafeVarargs
    private final List<List<String>> lines(List<String>... lines) {
        return Arrays.stream(lines).collect(Collectors.toList());
    }

}
