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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package org.opensearch.sql.legacy.util;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.opensearch.common.ParseField;
import org.opensearch.common.xcontent.ContextParser;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContent;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.search.aggregations.Aggregation;
import org.opensearch.search.aggregations.Aggregations;
import org.opensearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.opensearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.opensearch.search.aggregations.bucket.terms.DoubleTerms;
import org.opensearch.search.aggregations.bucket.terms.LongTerms;
import org.opensearch.search.aggregations.bucket.terms.ParsedDoubleTerms;
import org.opensearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.opensearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.opensearch.search.aggregations.bucket.terms.StringTerms;
import org.opensearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.opensearch.search.aggregations.metrics.MinAggregationBuilder;
import org.opensearch.search.aggregations.metrics.ParsedAvg;
import org.opensearch.search.aggregations.metrics.ParsedMax;
import org.opensearch.search.aggregations.metrics.ParsedMin;
import org.opensearch.search.aggregations.metrics.ParsedSum;
import org.opensearch.search.aggregations.metrics.ParsedValueCount;
import org.opensearch.search.aggregations.metrics.SumAggregationBuilder;
import org.opensearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import org.opensearch.search.aggregations.pipeline.ParsedPercentilesBucket;
import org.opensearch.search.aggregations.pipeline.PercentilesBucketPipelineAggregationBuilder;

public class AggregationUtils {
    private final static List<NamedXContentRegistry.Entry> entryList =
            new ImmutableMap.Builder<String, ContextParser<Object, ? extends Aggregation>>().put(
                    MinAggregationBuilder.NAME, (p, c) -> ParsedMin.fromXContent(p, (String) c))
                    .put(MaxAggregationBuilder.NAME, (p, c) -> ParsedMax.fromXContent(p, (String) c))
                    .put(SumAggregationBuilder.NAME, (p, c) -> ParsedSum.fromXContent(p, (String) c))
                    .put(AvgAggregationBuilder.NAME, (p, c) -> ParsedAvg.fromXContent(p, (String) c))
                    .put(StringTerms.NAME, (p, c) -> ParsedStringTerms.fromXContent(p, (String) c))
                    .put(LongTerms.NAME, (p, c) -> ParsedLongTerms.fromXContent(p, (String) c))
                    .put(DoubleTerms.NAME, (p, c) -> ParsedDoubleTerms.fromXContent(p, (String) c))
                    .put(ValueCountAggregationBuilder.NAME, (p, c) -> ParsedValueCount.fromXContent(p, (String) c))
                    .put(PercentilesBucketPipelineAggregationBuilder.NAME,
                         (p, c) -> ParsedPercentilesBucket.fromXContent(p, (String) c))
                    .put(DateHistogramAggregationBuilder.NAME, (p, c) -> ParsedDateHistogram.fromXContent(p, (String) c))
                    .build()
                    .entrySet()
                    .stream()
                    .map(entry -> new NamedXContentRegistry.Entry(Aggregation.class, new ParseField(entry.getKey()),
                                                                  entry.getValue()))
                    .collect(Collectors.toList());
    private final static NamedXContentRegistry namedXContentRegistry = new NamedXContentRegistry(entryList);
    private final static XContent xContent = XContentFactory.xContent(XContentType.JSON);

    /**
     * Populate {@link Aggregations} from JSON string.
     * @param json json string
     * @return {@link Aggregations}
     */
    public static Aggregations fromJson(String json) {
        try {
            XContentParser xContentParser =
                    xContent.createParser(namedXContentRegistry, LoggingDeprecationHandler.INSTANCE, json);
            xContentParser.nextToken();
            return Aggregations.fromXContent(xContentParser);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
