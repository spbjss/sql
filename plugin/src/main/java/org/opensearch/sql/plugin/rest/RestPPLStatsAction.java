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
 *
 *    Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License").
 *    You may not use this file except in compliance with the License.
 *    A copy of the License is located at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file. This file is distributed
 *    on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *    express or implied. See the License for the specific language governing
 *    permissions and limitations under the License.
 *
 */

package org.opensearch.sql.plugin.rest;

import static org.opensearch.rest.RestStatus.SERVICE_UNAVAILABLE;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.node.NodeClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.sql.legacy.executor.format.ErrorMessageFactory;
import org.opensearch.sql.legacy.metrics.Metrics;
import org.opensearch.sql.legacy.utils.LogUtils;

/**
 * PPL Node level status.
 */
public class RestPPLStatsAction extends BaseRestHandler {

  private static final Logger LOG = LogManager.getLogger(RestPPLStatsAction.class);

  /**
   * API endpoint path.
   */
  public static final String PPL_STATS_API_ENDPOINT = "/_plugins/_ppl/stats";
  public static final String PPL_LEGACY_STATS_API_ENDPOINT = "/_opendistro/_ppl/stats";

  public RestPPLStatsAction(Settings settings, RestController restController) {
    super();
  }

  @Override
  public String getName() {
    return "ppl_stats_action";
  }

  @Override
  public List<Route> routes() {
    return ImmutableList.of();
  }

  @Override
  public List<ReplacedRoute> replacedRoutes() {
    return ImmutableList.of(
        new ReplacedRoute(
            RestRequest.Method.POST, PPL_STATS_API_ENDPOINT,
            RestRequest.Method.POST, PPL_LEGACY_STATS_API_ENDPOINT),
        new ReplacedRoute(
            RestRequest.Method.GET, PPL_STATS_API_ENDPOINT,
            RestRequest.Method.GET, PPL_LEGACY_STATS_API_ENDPOINT));
  }

  @Override
  protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {

    LogUtils.addRequestId();

    try {
      return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.OK,
          Metrics.getInstance().collectToJSON()));
    } catch (Exception e) {
      LOG.error("Failed during Query PPL STATS Action.", e);

      return channel -> channel.sendResponse(new BytesRestResponse(SERVICE_UNAVAILABLE,
          ErrorMessageFactory.createErrorMessage(e, SERVICE_UNAVAILABLE.getStatus()).toString()));
    }
  }

  @Override
  protected Set<String> responseParams() {
    Set<String> responseParams = new HashSet<>(super.responseParams());
    responseParams.addAll(Arrays.asList("format", "sanitize"));
    return responseParams;
  }
}
