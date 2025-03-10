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

package org.opensearch.sql.legacy.request;

import java.io.IOException;
import java.util.Collections;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.search.SearchModule;
import org.opensearch.sql.legacy.exception.SqlParseException;

public class SqlRequest {

    public static final SqlRequest NULL = new SqlRequest("", null);

    String sql;
    JSONObject jsonContent;
    String cursor;
    Integer fetchSize;

    public SqlRequest(final String sql, final JSONObject jsonContent) {
        this.sql = sql;
        this.jsonContent = jsonContent;
    }

    public SqlRequest(final String cursor) {
        this.cursor = cursor;
    }

    public SqlRequest(final String sql, final Integer fetchSize, final JSONObject jsonContent) {
        this.sql = sql;
        this.fetchSize = fetchSize;
        this.jsonContent = jsonContent;
    }

    private static boolean isValidJson(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    public String getSql() {
        return this.sql;
    }

    public String cursor() {
        return this.cursor;
    }

    public Integer fetchSize() {
        return this.fetchSize;
    }

    public JSONObject getJsonContent() {
        return this.jsonContent;
    }

    /**
     * JSONObject's getJSONObject method will return just the value, this helper method is to extract the key and
     * value of 'filter' and return the JSON as a string.
     */
    private String getFilterObjectAsString(JSONObject jsonContent) {
        String filterVal = jsonContent.getJSONObject("filter").toString();
        return "{\"filter\":" + filterVal + "}";
    }

    private boolean hasFilterInRequest() {
        return jsonContent != null && jsonContent.has("filter");
    }

    /**
     * Takes 'filter' parameter from JSON request if JSON request and 'filter' were given and creates a QueryBuilder
     * object out of it to add to the filterClauses of the BoolQueryBuilder.
     */
    private void addFilterFromJson(BoolQueryBuilder boolQuery) throws SqlParseException {
        try {
            String filter = getFilterObjectAsString(jsonContent);
            SearchModule searchModule = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
            XContentParser parser = XContentFactory.xContent(XContentType.JSON).
                    createParser(new NamedXContentRegistry(searchModule.getNamedXContents()),
                            LoggingDeprecationHandler.INSTANCE,
                            filter);

            // nextToken is called before passing the parser to fromXContent since the fieldName will be null if the
            // first token it parses is START_OBJECT resulting in an exception
            parser.nextToken();
            boolQuery.filter(BoolQueryBuilder.fromXContent(parser));
        } catch (IOException e) {
            throw new SqlParseException("Unable to parse 'filter' in JSON request: " + e.getMessage());
        }

    }

    public BoolQueryBuilder checkAndAddFilter(BoolQueryBuilder boolQuery) throws SqlParseException {
        if (hasFilterInRequest()) {
            // if WHERE was not given, create a new BoolQuery to add "filter" to
            boolQuery = boolQuery == null ? new BoolQueryBuilder() : boolQuery;
            addFilterFromJson(boolQuery);
        }
        return boolQuery;
    }
}
