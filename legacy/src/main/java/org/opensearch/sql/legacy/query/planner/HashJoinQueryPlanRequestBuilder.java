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

package org.opensearch.sql.legacy.query.planner;

import org.opensearch.client.Client;
import org.opensearch.sql.legacy.query.join.HashJoinElasticRequestBuilder;
import org.opensearch.sql.legacy.query.planner.core.Config;
import org.opensearch.sql.legacy.query.planner.core.QueryParams;
import org.opensearch.sql.legacy.query.planner.core.QueryPlanner;
import org.opensearch.sql.legacy.request.SqlRequest;

/**
 * QueryPlanner builder for Hash Join query. In future, different queries could have its own builders to generate
 * QueryPlanner. QueryPlanner would run all stages in its pipeline no matter how it be assembled.
 */
public class HashJoinQueryPlanRequestBuilder extends HashJoinElasticRequestBuilder {

    /**
     * Client connection to OpenSearch cluster
     */
    private final Client client;

    /**
     * Query request
     */
    private final SqlRequest request;

    /**
     * Query planner configuration
     */
    private final Config config;


    public HashJoinQueryPlanRequestBuilder(Client client, SqlRequest request) {
        this.client = client;
        this.request = request;
        this.config = new Config();
    }

    @Override
    public String explain() {
        return plan().explain();
    }

    /**
     * Planning for the query and create planner for explain/execute later.
     *
     * @return query planner
     */
    public QueryPlanner plan() {
        config.configureLimit(
                getTotalLimit(),
                getFirstTable().getHintLimit(),
                getSecondTable().getHintLimit()
        );
        config.configureTermsFilterOptimization(isUseTermFiltersOptimization());

        return new QueryPlanner(
                client,
                config,
                new QueryParams(
                        getFirstTable(),
                        getSecondTable(),
                        getJoinType(),
                        getT1ToT2FieldsComparison()
                )
        );
    }

    public Config getConfig() {
        return config;
    }
}
