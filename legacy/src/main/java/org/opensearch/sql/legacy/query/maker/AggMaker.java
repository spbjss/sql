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

package org.opensearch.sql.legacy.query.maker;

import com.alibaba.druid.sql.ast.expr.SQLAggregateOption;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.opensearch.common.ParsingException;
import org.opensearch.common.Strings;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.common.xcontent.json.JsonXContentParser;
import org.opensearch.join.aggregations.JoinAggregationBuilders;
import org.opensearch.script.Script;
import org.opensearch.script.ScriptType;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.BucketOrder;
import org.opensearch.search.aggregations.InternalOrder;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.aggregations.bucket.geogrid.GeoGridAggregationBuilder;
import org.opensearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.opensearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.opensearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.opensearch.search.aggregations.bucket.histogram.LongBounds;
import org.opensearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import org.opensearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.opensearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.IncludeExclude;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.opensearch.search.aggregations.metrics.GeoBoundsAggregationBuilder;
import org.opensearch.search.aggregations.metrics.PercentilesAggregationBuilder;
import org.opensearch.search.aggregations.metrics.ScriptedMetricAggregationBuilder;
import org.opensearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.opensearch.search.aggregations.support.ValuesSourceAggregationBuilder;
import org.opensearch.search.sort.SortOrder;
import org.opensearch.sql.legacy.domain.Condition;
import org.opensearch.sql.legacy.domain.Field;
import org.opensearch.sql.legacy.domain.KVValue;
import org.opensearch.sql.legacy.domain.MethodField;
import org.opensearch.sql.legacy.domain.Where;
import org.opensearch.sql.legacy.domain.Where.CONN;
import org.opensearch.sql.legacy.domain.bucketpath.Path;
import org.opensearch.sql.legacy.exception.SqlParseException;
import org.opensearch.sql.legacy.parser.ChildrenType;
import org.opensearch.sql.legacy.parser.NestedType;
import org.opensearch.sql.legacy.utils.Util;

public class AggMaker {

    /**
     * The mapping bettwen group fieldName or Alias to the KVValue.
     */
    private Map<String, KVValue> groupMap = new HashMap<>();
    private Where where;

    /**
     * 分组查的聚合函数
     *
     * @param field
     * @return
     * @throws SqlParseException
     */
    public AggregationBuilder makeGroupAgg(Field field) throws SqlParseException {

        if (field instanceof MethodField && field.getName().equals("script")) {
            MethodField methodField = (MethodField) field;
            TermsAggregationBuilder termsBuilder = AggregationBuilders.terms(methodField.getAlias())
                    .script(new Script(methodField.getParams().get(1).value.toString()));
            extendGroupMap(methodField, new KVValue("KEY", termsBuilder));
            return termsBuilder;
        }


        if (field instanceof MethodField) {

            MethodField methodField = (MethodField) field;
            if (methodField.getName().equals("filter")) {
                Map<String, Object> paramsAsMap = methodField.getParamsAsMap();
                Where where = (Where) paramsAsMap.get("where");
                return AggregationBuilders.filter(paramsAsMap.get("alias").toString(),
                        QueryMaker.explain(where));
            }
            return makeRangeGroup(methodField);
        } else {
            String termName = (Strings.isNullOrEmpty(field.getAlias())) ? field.getName() : field.getAlias();
            TermsAggregationBuilder termsBuilder = AggregationBuilders.terms(termName).field(field.getName());
            final KVValue kvValue = new KVValue("KEY", termsBuilder);
            groupMap.put(termName, kvValue);
            // map the field name with KVValue if it is not yet. The use case is when alias exist,
            // the termName is different with fieldName, both of them should be included in the map.
            groupMap.putIfAbsent(field.getName(), kvValue);
            return termsBuilder;
        }
    }


    /**
     * Create aggregation according to the SQL function.
     *
     * @param field  SQL function
     * @param parent parentAggregation
     * @return AggregationBuilder represents the SQL function
     * @throws SqlParseException in case of unrecognized function
     */
    public AggregationBuilder makeFieldAgg(MethodField field, AggregationBuilder parent) throws SqlParseException {
        extendGroupMap(field, new KVValue("FIELD", parent));
        ValuesSourceAggregationBuilder builder;
        field.setAlias(fixAlias(field.getAlias()));
        switch (field.getName().toUpperCase()) {
            case "SUM":
                builder = AggregationBuilders.sum(field.getAlias());
                return addFieldToAgg(field, builder);
            case "MAX":
                builder = AggregationBuilders.max(field.getAlias());
                return addFieldToAgg(field, builder);
            case "MIN":
                builder = AggregationBuilders.min(field.getAlias());
                return addFieldToAgg(field, builder);
            case "AVG":
                builder = AggregationBuilders.avg(field.getAlias());
                return addFieldToAgg(field, builder);
            case "STATS":
                builder = AggregationBuilders.stats(field.getAlias());
                return addFieldToAgg(field, builder);
            case "EXTENDED_STATS":
                builder = AggregationBuilders.extendedStats(field.getAlias());
                return addFieldToAgg(field, builder);
            case "PERCENTILES":
                builder = AggregationBuilders.percentiles(field.getAlias());
                addSpecificPercentiles((PercentilesAggregationBuilder) builder, field.getParams());
                return addFieldToAgg(field, builder);
            case "TOPHITS":
                return makeTopHitsAgg(field);
            case "SCRIPTED_METRIC":
                return scriptedMetric(field);
            case "COUNT":
                extendGroupMap(field, new KVValue("COUNT", parent));
                return addFieldToAgg(field, makeCountAgg(field));
            default:
                throw new SqlParseException("the agg function not to define !");
        }
    }

    /**
     * With {@link Where} Condition.
     */
    public AggMaker withWhere(Where where) {
        this.where = where;
        return this;
    }

    private void addSpecificPercentiles(PercentilesAggregationBuilder percentilesBuilder, List<KVValue> params) {
        List<Double> percentiles = new ArrayList<>();
        for (KVValue kValue : params) {
            if (kValue.value.getClass().equals(BigDecimal.class)) {
                BigDecimal percentile = (BigDecimal) kValue.value;
                percentiles.add(percentile.doubleValue());

            } else if (kValue.value instanceof Integer) {
                percentiles.add(((Integer) kValue.value).doubleValue());
            }
        }
        if (percentiles.size() > 0) {
            double[] percentilesArr = new double[percentiles.size()];
            int i = 0;
            for (Double percentile : percentiles) {
                percentilesArr[i] = percentile;
                i++;
            }
            percentilesBuilder.percentiles(percentilesArr);
        }
    }

    private String fixAlias(String alias) {
        //because [ is not legal as alias
        return alias.replaceAll("\\[", "(").replaceAll("\\]", ")");
    }

    private AggregationBuilder addFieldToAgg(MethodField field, ValuesSourceAggregationBuilder builder)
            throws SqlParseException {
        KVValue kvValue = field.getParams().get(0);
        if (kvValue.key != null && kvValue.key.equals("script")) {
            if (kvValue.value instanceof MethodField) {
                return builder.script(new Script(((MethodField) kvValue.value).getParams().get(1).toString()));
            } else {
                return builder.script(new Script(kvValue.value.toString()));
            }

        } else if (kvValue.key != null && kvValue.value.toString().trim().startsWith("def")) {
            return builder.script(new Script(kvValue.value.toString()));
        } else if (kvValue.key != null && (kvValue.key.equals("nested") || kvValue.key.equals("reverse_nested"))) {
            NestedType nestedType = (NestedType) kvValue.value;
            nestedType.addBucketPath(Path.getMetricPath(builder.getName()));

            if (nestedType.isNestedField()) {
                builder.field("_index");
            } else {
                builder.field(nestedType.field);
            }

            AggregationBuilder nestedBuilder;

            String nestedAggName = nestedType.getNestedAggName();

            if (nestedType.isReverse()) {
                if (nestedType.path != null && nestedType.path.startsWith("~")) {
                    String realPath = nestedType.path.substring(1);
                    nestedBuilder = AggregationBuilders.nested(nestedAggName, realPath);
                    nestedBuilder = nestedBuilder.subAggregation(builder);
                    return AggregationBuilders.reverseNested(nestedAggName + "_REVERSED")
                            .subAggregation(nestedBuilder);
                } else {
                    ReverseNestedAggregationBuilder reverseNestedAggregationBuilder =
                            AggregationBuilders.reverseNested(nestedAggName);
                    if (nestedType.path != null) {
                        reverseNestedAggregationBuilder.path(nestedType.path);
                    }
                    nestedBuilder = reverseNestedAggregationBuilder;
                }
            } else {
                nestedBuilder = AggregationBuilders.nested(nestedAggName, nestedType.path);
            }

            AggregationBuilder aggregation = nestedBuilder.subAggregation(wrapWithFilterAgg(
                    nestedType,
                    builder));
            nestedType.addBucketPath(Path.getAggPath(nestedBuilder.getName()));
            return aggregation;
        } else if (kvValue.key != null && (kvValue.key.equals("children"))) {
            ChildrenType childrenType = (ChildrenType) kvValue.value;

            builder.field(childrenType.field);

            AggregationBuilder childrenBuilder;

            String childrenAggName = childrenType.field + "@CHILDREN";

            childrenBuilder = JoinAggregationBuilders.children(childrenAggName, childrenType.childType);

            return childrenBuilder;
        }

        return builder.field(kvValue.toString());
    }

    private AggregationBuilder makeRangeGroup(MethodField field) throws SqlParseException {
        switch (field.getName().toLowerCase()) {
            case "range":
                return rangeBuilder(field);
            case "date_histogram":
                return dateHistogram(field);
            case "date_range":
            case "month":
                return dateRange(field);
            case "histogram":
                return histogram(field);
            case "geohash_grid":
                return geohashGrid(field);
            case "geo_bounds":
                return geoBounds(field);
            case "terms":
                return termsAgg(field);
            default:
                throw new SqlParseException("can define this method " + field);
        }

    }

    private AggregationBuilder geoBounds(MethodField field) throws SqlParseException {
        String aggName = gettAggNameFromParamsOrAlias(field);
        GeoBoundsAggregationBuilder boundsBuilder = AggregationBuilders.geoBounds(aggName);
        String value;
        for (KVValue kv : field.getParams()) {
            value = kv.value.toString();
            switch (kv.key.toLowerCase()) {
                case "field":
                    boundsBuilder.field(value);
                    break;
                case "wrap_longitude":
                    boundsBuilder.wrapLongitude(Boolean.getBoolean(value));
                    break;
                case "alias":
                case "nested":
                case "reverse_nested":
                case "children":
                    break;
                default:
                    throw new SqlParseException("geo_bounds err or not define field " + kv.toString());
            }
        }
        return boundsBuilder;
    }

    private AggregationBuilder termsAgg(MethodField field) throws SqlParseException {
        String aggName = gettAggNameFromParamsOrAlias(field);
        TermsAggregationBuilder terms = AggregationBuilders.terms(aggName);
        String value;
        IncludeExclude include = null, exclude = null;
        for (KVValue kv : field.getParams()) {
            if (kv.value.toString().contains("doc[")) {
                String script = kv.value + "; return " + kv.key;
                terms.script(new Script(script));
            } else {
                value = kv.value.toString();
                switch (kv.key.toLowerCase()) {
                    case "field":
                        terms.field(value);
                        break;
                    case "size":
                        terms.size(Integer.parseInt(value));
                        break;
                    case "shard_size":
                        terms.shardSize(Integer.parseInt(value));
                        break;
                    case "min_doc_count":
                        terms.minDocCount(Integer.parseInt(value));
                        break;
                    case "missing":
                        terms.missing(value);
                        break;
                    case "order":
                        if ("asc".equalsIgnoreCase(value)) {
                            terms.order(BucketOrder.key(true));
                        } else if ("desc".equalsIgnoreCase(value)) {
                            terms.order(BucketOrder.key(false));
                        } else {
                            List<BucketOrder> orderElements = new ArrayList<>();
                            try (JsonXContentParser parser = new JsonXContentParser(NamedXContentRegistry.EMPTY,
                                    LoggingDeprecationHandler.INSTANCE, new JsonFactory().createParser(value))) {
                                XContentParser.Token currentToken = parser.nextToken();
                                if (currentToken == XContentParser.Token.START_OBJECT) {
                                    orderElements.add(InternalOrder.Parser.parseOrderParam(parser));
                                } else if (currentToken == XContentParser.Token.START_ARRAY) {
                                    for (currentToken = parser.nextToken();
                                         currentToken != XContentParser.Token.END_ARRAY;
                                         currentToken = parser.nextToken()) {
                                        if (currentToken == XContentParser.Token.START_OBJECT) {
                                            orderElements.add(InternalOrder.Parser.parseOrderParam(parser));
                                        } else {
                                            throw new ParsingException(parser.getTokenLocation(),
                                                    "Invalid token in order array");
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                throw new SqlParseException("couldn't parse order: " + e.getMessage());
                            }
                            terms.order(orderElements);
                        }
                        break;
                    case "alias":
                    case "nested":
                    case "reverse_nested":
                    case "children":
                        break;
                    case "execution_hint":
                        terms.executionHint(value);
                        break;
                    case "include":
                        try (XContentParser parser = JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY,
                                LoggingDeprecationHandler.INSTANCE, value)) {
                            parser.nextToken();
                            include = IncludeExclude.parseInclude(parser);
                        } catch (IOException e) {
                            throw new SqlParseException("parse include[" + value + "] error: " + e.getMessage());
                        }
                        break;
                    case "exclude":
                        try (XContentParser parser = JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY,
                                LoggingDeprecationHandler.INSTANCE, value)) {
                            parser.nextToken();
                            exclude = IncludeExclude.parseExclude(parser);
                        } catch (IOException e) {
                            throw new SqlParseException("parse exclude[" + value + "] error: " + e.getMessage());
                        }
                        break;
                    default:
                        throw new SqlParseException("terms aggregation err or not define field " + kv.toString());
                }
            }
        }
        terms.includeExclude(IncludeExclude.merge(include, exclude));
        return terms;
    }

    private AbstractAggregationBuilder scriptedMetric(MethodField field) throws SqlParseException {
        String aggName = gettAggNameFromParamsOrAlias(field);
        ScriptedMetricAggregationBuilder scriptedMetricBuilder = AggregationBuilders.scriptedMetric(aggName);
        Map<String, Object> scriptedMetricParams = field.getParamsAsMap();
        if (!scriptedMetricParams.containsKey("map_script") && !scriptedMetricParams.containsKey("map_script_id")
                && !scriptedMetricParams.containsKey("map_script_file")) {
            throw new SqlParseException(
                    "scripted metric parameters must contain map_script/map_script_id/map_script_file parameter");
        }
        HashMap<String, Object> scriptAdditionalParams = new HashMap<>();
        HashMap<String, Object> reduceScriptAdditionalParams = new HashMap<>();
        for (Map.Entry<String, Object> param : scriptedMetricParams.entrySet()) {
            String paramValue = param.getValue().toString();
            if (param.getKey().startsWith("@")) {
                if (param.getKey().startsWith("@reduce_")) {
                    reduceScriptAdditionalParams.put(param.getKey().replace("@reduce_", ""),
                            param.getValue());
                } else {
                    scriptAdditionalParams.put(param.getKey().replace("@", ""), param.getValue());
                }
                continue;
            }

            switch (param.getKey().toLowerCase()) {
                case "map_script":
                    scriptedMetricBuilder.mapScript(new Script(paramValue));
                    break;
                case "map_script_id":
                    scriptedMetricBuilder.mapScript(new Script(ScriptType.STORED, Script.DEFAULT_SCRIPT_LANG,
                            paramValue, new HashMap<>()));
                    break;
                case "init_script":
                    scriptedMetricBuilder.initScript(new Script(paramValue));
                    break;
                case "init_script_id":
                    scriptedMetricBuilder.initScript(new Script(ScriptType.STORED, Script.DEFAULT_SCRIPT_LANG,
                            paramValue, new HashMap<>()));
                    break;
                case "combine_script":
                    scriptedMetricBuilder.combineScript(new Script(paramValue));
                    break;
                case "combine_script_id":
                    scriptedMetricBuilder.combineScript(new Script(ScriptType.STORED, Script.DEFAULT_SCRIPT_LANG,
                            paramValue, new HashMap<>()));
                    break;
                case "reduce_script":
                    scriptedMetricBuilder.reduceScript(new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG,
                            paramValue, reduceScriptAdditionalParams));
                    break;
                case "reduce_script_id":
                    scriptedMetricBuilder.reduceScript(new Script(ScriptType.STORED, Script.DEFAULT_SCRIPT_LANG,
                            paramValue, reduceScriptAdditionalParams));
                    break;
                case "alias":
                case "nested":
                case "reverse_nested":
                case "children":
                    break;
                default:
                    throw new SqlParseException("scripted_metric err or not define field " + param.getKey());
            }
        }
        if (scriptAdditionalParams.size() > 0) {
            scriptAdditionalParams.put("_agg", new HashMap<>());
            scriptedMetricBuilder.params(scriptAdditionalParams);
        }

        return scriptedMetricBuilder;
    }

    private AggregationBuilder geohashGrid(MethodField field) throws SqlParseException {
        String aggName = gettAggNameFromParamsOrAlias(field);
        GeoGridAggregationBuilder geoHashGrid = AggregationBuilders.geohashGrid(aggName);
        String value;
        for (KVValue kv : field.getParams()) {
            value = kv.value.toString();
            switch (kv.key.toLowerCase()) {
                case "precision":
                    geoHashGrid.precision(Integer.parseInt(value));
                    break;
                case "field":
                    geoHashGrid.field(value);
                    break;
                case "size":
                    geoHashGrid.size(Integer.parseInt(value));
                    break;
                case "shard_size":
                    geoHashGrid.shardSize(Integer.parseInt(value));
                    break;
                case "alias":
                case "nested":
                case "reverse_nested":
                case "children":
                    break;
                default:
                    throw new SqlParseException("geohash grid err or not define field " + kv.toString());
            }
        }
        return geoHashGrid;
    }

    private static final String TIME_FARMAT = "yyyy-MM-dd HH:mm:ss";

    private ValuesSourceAggregationBuilder dateRange(MethodField field) {
        String alias = gettAggNameFromParamsOrAlias(field);
        DateRangeAggregationBuilder dateRange = AggregationBuilders.dateRange(alias).format(TIME_FARMAT);

        String value;
        List<String> ranges = new ArrayList<>();
        for (KVValue kv : field.getParams()) {
            value = kv.value.toString();
            if ("field".equals(kv.key)) {
                dateRange.field(value);
            } else if ("format".equals(kv.key)) {
                dateRange.format(value);
            } else if ("time_zone".equals(kv.key)) {
                dateRange.timeZone(ZoneOffset.of(value));
            } else if ("from".equals(kv.key)) {
                dateRange.addUnboundedFrom(kv.value.toString());
            } else if ("to".equals(kv.key)) {
                dateRange.addUnboundedTo(kv.value.toString());
            } else if (!"alias".equals(kv.key) && !"nested".equals(kv.key) && !"children".equals(kv.key)) {
                ranges.add(value);
            }
        }

        for (int i = 1; i < ranges.size(); i++) {
            dateRange.addRange(ranges.get(i - 1), ranges.get(i));
        }

        return dateRange;
    }

    /**
     * 按照时间范围分组
     *
     * @param field
     * @return
     * @throws SqlParseException
     */
    private DateHistogramAggregationBuilder dateHistogram(MethodField field) throws SqlParseException {
        String alias = gettAggNameFromParamsOrAlias(field);
        DateHistogramAggregationBuilder dateHistogram = AggregationBuilders.dateHistogram(alias).format(TIME_FARMAT);
        String value;
        for (KVValue kv : field.getParams()) {
            if (kv.value.toString().contains("doc[")) {
                String script = kv.value + "; return " + kv.key;
                dateHistogram.script(new Script(script));
            } else {
                value = kv.value.toString();
                switch (kv.key.toLowerCase()) {
                    case "interval":
                        dateHistogram.dateHistogramInterval(new DateHistogramInterval(kv.value.toString()));
                        break;
                    case "fixed_interval":
                        dateHistogram.fixedInterval(new DateHistogramInterval(kv.value.toString()));
                        break;
                    case "field":
                        dateHistogram.field(value);
                        break;
                    case "format":
                        dateHistogram.format(value);
                        break;
                    case "time_zone":
                        dateHistogram.timeZone(ZoneOffset.of(value));
                        break;
                    case "min_doc_count":
                        dateHistogram.minDocCount(Long.parseLong(value));
                        break;
                    case "order":
                        dateHistogram.order("desc".equalsIgnoreCase(value) ? BucketOrder.key(false) :
                                BucketOrder.key(true));
                        break;
                    case "extended_bounds":
                        String[] bounds = value.split(":");
                        if (bounds.length == 2) {
                            dateHistogram.extendedBounds(new LongBounds(bounds[0], bounds[1]));
                        }
                        break;

                    case "alias":
                    case "nested":
                    case "reverse_nested":
                    case "children":
                        break;
                    default:
                        throw new SqlParseException("date range err or not define field " + kv.toString());
                }
            }
        }
        return dateHistogram;
    }

    private String gettAggNameFromParamsOrAlias(MethodField field) {
        String alias = field.getAlias();
        for (KVValue kv : field.getParams()) {
            if (kv.key != null && kv.key.equals("alias")) {
                alias = kv.value.toString();
            }
        }
        return alias;
    }

    private HistogramAggregationBuilder histogram(MethodField field) throws SqlParseException {
        String aggName = gettAggNameFromParamsOrAlias(field);
        HistogramAggregationBuilder histogram = AggregationBuilders.histogram(aggName);
        String value;
        for (KVValue kv : field.getParams()) {
            if (kv.value.toString().contains("doc[")) {
                String script = kv.value + "; return " + kv.key;
                histogram.script(new Script(script));
            } else {
                value = kv.value.toString();
                switch (kv.key.toLowerCase()) {
                    case "interval":
                        histogram.interval(Long.parseLong(value));
                        break;
                    case "field":
                        histogram.field(value);
                        break;
                    case "min_doc_count":
                        histogram.minDocCount(Long.parseLong(value));
                        break;
                    case "extended_bounds":
                        String[] bounds = value.split(":");
                        if (bounds.length == 2) {
                            histogram.extendedBounds(Long.valueOf(bounds[0]), Long.valueOf(bounds[1]));
                        }
                        break;
                    case "alias":
                    case "nested":
                    case "reverse_nested":
                    case "children":
                        break;
                    case "order":
                        final BucketOrder order;
                        switch (value) {
                            case "key_desc":
                                order = BucketOrder.key(false);
                                break;
                            case "count_asc":
                                order = BucketOrder.count(true);
                                break;
                            case "count_desc":
                                order = BucketOrder.count(false);
                                break;
                            case "key_asc":
                            default:
                                order = BucketOrder.key(true);
                                break;
                        }
                        histogram.order(order);
                        break;
                    default:
                        throw new SqlParseException("histogram err or not define field " + kv.toString());
                }
            }
        }
        return histogram;
    }

    /**
     * 构建范围查询
     *
     * @param field
     * @return
     */
    private RangeAggregationBuilder rangeBuilder(MethodField field) {

        // ignore alias param
        LinkedList<KVValue> params = field.getParams().stream().filter(kv -> !"alias".equals(kv.key))
                .collect(Collectors.toCollection(LinkedList::new));

        String fieldName = params.poll().toString();

        double[] ds = Util.KV2DoubleArr(params);

        RangeAggregationBuilder range = AggregationBuilders.range(field.getAlias()).field(fieldName);

        for (int i = 1; i < ds.length; i++) {
            range.addRange(ds[i - 1], ds[i]);
        }

        return range;
    }


    /**
     * Create count aggregation.
     *
     * @param field The count function
     * @return AggregationBuilder use to count result
     */
    private ValuesSourceAggregationBuilder makeCountAgg(MethodField field) {

        // Cardinality is approximate DISTINCT.
        if (SQLAggregateOption.DISTINCT.equals(field.getOption())) {

            if (field.getParams().size() == 1) {
                return AggregationBuilders.cardinality(field.getAlias()).field(field.getParams().get(0).value
                        .toString());
            } else {
                Integer precision_threshold = (Integer) (field.getParams().get(1).value);
                return AggregationBuilders.cardinality(field.getAlias()).precisionThreshold(precision_threshold)
                        .field(field.getParams().get(0).value.toString());
            }

        }

        String fieldName = field.getParams().get(0).value.toString();

        // In case of count(*) we use '_index' as field parameter to count all documents
        if ("*".equals(fieldName)) {
            KVValue kvValue = new KVValue(null, "_index");
            field.getParams().set(0, kvValue);
            return AggregationBuilders.count(field.getAlias()).field(kvValue.toString());
        } else {
            return AggregationBuilders.count(field.getAlias()).field(fieldName);
        }
    }

    /**
     * TOPHITS查询
     *
     * @param field
     * @return
     */
    private AbstractAggregationBuilder makeTopHitsAgg(MethodField field) {
        String alias = gettAggNameFromParamsOrAlias(field);
        TopHitsAggregationBuilder topHits = AggregationBuilders.topHits(alias);
        List<KVValue> params = field.getParams();
        String[] include = null;
        String[] exclude = null;
        for (KVValue kv : params) {
            switch (kv.key) {
                case "from":
                    topHits.from((int) kv.value);
                    break;
                case "size":
                    topHits.size((int) kv.value);
                    break;
                case "include":
                    include = kv.value.toString().split(",");
                    break;
                case "exclude":
                    exclude = kv.value.toString().split(",");
                    break;
                case "alias":
                case "nested":
                case "reverse_nested":
                case "children":
                    break;
                default:
                    topHits.sort(kv.key, SortOrder.valueOf(kv.value.toString().toUpperCase()));
                    break;
            }
        }
        if (include != null || exclude != null) {
            topHits.fetchSource(include, exclude);
        }
        return topHits;
    }

    public Map<String, KVValue> getGroupMap() {
        return this.groupMap;
    }

    /**
     * Wrap the Metric Aggregation with Filter Aggregation if necessary.
     * The Filter Aggregation condition is constructed from the nested condition in where clause.
     */
    private AggregationBuilder wrapWithFilterAgg(NestedType nestedType, ValuesSourceAggregationBuilder builder)
            throws SqlParseException {
        if (where != null && where.getWheres() != null) {
            List<Condition> nestedConditionList = where.getWheres().stream()
                    .filter(condition -> condition instanceof Condition)
                    .map(condition -> (Condition) condition)
                    .filter(condition -> condition.isNestedComplex()
                            || nestedType.path.equalsIgnoreCase(condition.getNestedPath()))
                    // ignore the OR condition on nested field.
                    .filter(condition -> CONN.AND.equals(condition.getConn()))
                    .collect(Collectors.toList());
            if (!nestedConditionList.isEmpty()) {
                Where filterWhere = new Where(where.getConn());
                nestedConditionList.forEach(condition -> {
                    if (condition.isNestedComplex()) {
                        ((Where) condition.getValue()).getWheres().forEach(filterWhere::addWhere);
                    } else {
                        // Since the filter condition is used inside Nested Aggregation,remove the nested attribute.
                        condition.setNested(false);
                        condition.setNestedPath("");
                        filterWhere.addWhere(condition);
                    }
                });
                FilterAggregationBuilder filterAgg = AggregationBuilders.filter(
                        nestedType.getFilterAggName(),
                        QueryMaker.explain(filterWhere));
                nestedType.addBucketPath(Path.getAggPath(filterAgg.getName()));
                return filterAgg.subAggregation(builder);
            }
        }
        return builder;
    }

    /**
     * The groupMap is used when parsing order by to find out the corresponding field in aggregation.
     * There are two cases.
     * 1) using alias in order by, e.g. SELECT COUNT(*) as c FROM T GROUP BY age ORDER BY c
     * 2) using full name in order by, e.g. SELECT COUNT(*) as c FROM T GROUP BY age ORDER BY COUNT(*)
     * Then, the groupMap should support these two cases by maintain the mapping of
     * {alias, value} and {full_name, value}
     */
    private void extendGroupMap(Field field, KVValue value) {
        groupMap.put(field.toString(), value);
        if (!StringUtils.isEmpty(field.getAlias())) {
            groupMap.putIfAbsent(field.getAlias(), value);
        }
    }
}
