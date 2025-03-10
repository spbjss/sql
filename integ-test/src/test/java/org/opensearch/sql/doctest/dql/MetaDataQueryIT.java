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

package org.opensearch.sql.doctest.dql;

import static org.opensearch.sql.doctest.core.request.SqlRequestFormat.IGNORE_REQUEST;
import static org.opensearch.sql.doctest.core.request.SqlRequestFormat.OPENSEARCH_DASHBOARD_REQUEST;
import static org.opensearch.sql.doctest.core.response.SqlResponseFormat.IGNORE_RESPONSE;
import static org.opensearch.sql.doctest.core.response.SqlResponseFormat.TABLE_RESPONSE;

import org.opensearch.sql.doctest.core.DocTest;
import org.opensearch.sql.doctest.core.annotation.DocTestConfig;
import org.opensearch.sql.doctest.core.annotation.Section;
import org.opensearch.sql.doctest.core.builder.Example;
import org.opensearch.sql.doctest.core.builder.Requests;

@DocTestConfig(template = "dql/metadata.rst", testData = {"accounts.json", "employees_nested.json"})
public class MetaDataQueryIT extends DocTest {

  @Section(1)
  public void queryMetaData() {
    section(
        title("Querying Metadata"),
        description(
            "You can query your indices metadata by ``SHOW`` and ``DESCRIBE`` statement. These commands are",
            "very useful for database management tool to enumerate all existing indices and get basic information",
            "from the cluster."
        ),
        images("rdd/showStatement.png", "rdd/showFilter.png"),
        metadataQueryExample(
            title("Show All Indices Information"),
            description(
                "``SHOW`` statement lists all indices that match the search pattern. By using wildcard '%',",
                "information for all indices in the cluster is returned."
            ),
            post("SHOW TABLES LIKE %")
        ),
        metadataQueryExample(
            title("Show Specific Index Information"),
            description(
                "Here is an example that searches metadata for index name prefixed by 'acc'"),
            post("SHOW TABLES LIKE acc%")
        ),
        metadataQueryExample(
            title("Describe Index Fields Information"),
            description(
                "``DESCRIBE`` statement lists all fields for indices that can match the search pattern."),
            post("DESCRIBE TABLES LIKE accounts")
        )
    );
  }

  /**
   * Explain doesn't work for SHOW/DESCRIBE so skip it
   */
  private Example metadataQueryExample(String title, String description, Requests requests) {
    return example(title, description, requests,
        queryFormat(OPENSEARCH_DASHBOARD_REQUEST, TABLE_RESPONSE),
        explainFormat(IGNORE_REQUEST, IGNORE_RESPONSE)
    );
  }

}
