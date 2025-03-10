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

package org.opensearch.sql.opensearch.storage.script.aggregation;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opensearch.sql.data.type.ExprCoreType.INTEGER;
import static org.opensearch.sql.data.type.ExprCoreType.STRING;
import static org.opensearch.sql.expression.DSL.literal;
import static org.opensearch.sql.expression.DSL.ref;
import static org.opensearch.sql.opensearch.data.type.OpenSearchDataType.OPENSEARCH_TEXT_KEYWORD;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.index.LeafReaderContext;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.index.fielddata.ScriptDocValues;
import org.opensearch.search.lookup.LeafDocLookup;
import org.opensearch.search.lookup.LeafSearchLookup;
import org.opensearch.search.lookup.SearchLookup;
import org.opensearch.sql.expression.DSL;
import org.opensearch.sql.expression.Expression;
import org.opensearch.sql.expression.config.ExpressionConfig;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ExpressionAggregationScriptTest {

  private final DSL dsl = new ExpressionConfig().dsl(new ExpressionConfig().functionRepository());

  @Mock
  private SearchLookup lookup;

  @Mock
  private LeafSearchLookup leafLookup;

  @Mock
  private LeafReaderContext context;

  @Test
  void can_execute_expression_with_integer_field() {
    assertThat()
        .docValues("age", 30L) // DocValue only supports long
        .evaluate(
            dsl.abs(ref("age", INTEGER)))
        .shouldMatch(30);
  }

  @Test
  void can_execute_expression_with_integer_field_with_boolean_result() {
    assertThat()
        .docValues("age", 30L) // DocValue only supports long
        .evaluate(
            dsl.greater(ref("age", INTEGER), literal(20)))
        .shouldMatch(true);
  }

  @Test
  void can_execute_expression_with_text_keyword_field() {
    assertThat()
        .docValues("name.keyword", "John")
        .evaluate(
            dsl.equal(ref("name", OPENSEARCH_TEXT_KEYWORD), literal("John")))
        .shouldMatch(true);
  }

  @Test
  void can_execute_expression_with_null_field() {
    assertThat()
        .docValues("age", null)
        .evaluate(ref("age", INTEGER))
        .shouldMatch(null);
  }

  @Test
  void can_execute_expression_with_missing_field() {
    assertThat()
        .docValues("age", 30)
        .evaluate(ref("name", STRING))
        .shouldMatch(null);
  }

  private ExprScriptAssertion assertThat() {
    return new ExprScriptAssertion(lookup, leafLookup, context);
  }

  @RequiredArgsConstructor
  private static class ExprScriptAssertion {
    private final SearchLookup lookup;
    private final LeafSearchLookup leafLookup;
    private final LeafReaderContext context;
    private Object actual;

    ExprScriptAssertion docValues() {
      return this;
    }

    ExprScriptAssertion docValues(String name, Object value) {
      LeafDocLookup leafDocLookup = mockLeafDocLookup(
          ImmutableMap.of(name, new FakeScriptDocValues<>(value)));

      when(lookup.getLeafSearchLookup(any())).thenReturn(leafLookup);
      when(leafLookup.doc()).thenReturn(leafDocLookup);
      return this;
    }

    ExprScriptAssertion docValues(String name1, Object value1,
                                  String name2, Object value2) {
      LeafDocLookup leafDocLookup = mockLeafDocLookup(
          ImmutableMap.of(
              name1, new FakeScriptDocValues<>(value1),
              name2, new FakeScriptDocValues<>(value2)));

      when(lookup.getLeafSearchLookup(any())).thenReturn(leafLookup);
      when(leafLookup.doc()).thenReturn(leafDocLookup);
      return this;
    }

    ExprScriptAssertion evaluate(Expression expr) {
      ExpressionAggregationScript script =
          new ExpressionAggregationScript(expr, lookup, context, emptyMap());
      actual = script.execute();
      return this;
    }

    void shouldMatch(Object expected) {
      assertEquals(expected, actual);
    }

    private LeafDocLookup mockLeafDocLookup(Map<String, ScriptDocValues<?>> docValueByNames) {
      LeafDocLookup leafDocLookup = mock(LeafDocLookup.class);
      when(leafDocLookup.get(anyString()))
          .thenAnswer(invocation -> docValueByNames.get(invocation.<String>getArgument(0)));
      return leafDocLookup;
    }
  }

  @RequiredArgsConstructor
  private static class FakeScriptDocValues<T> extends ScriptDocValues<T> {
    private final T value;

    @Override
    public void setNextDocId(int docId) {
      throw new UnsupportedOperationException("Fake script doc values doesn't implement this yet");
    }

    @Override
    public T get(int index) {
      return value;
    }

    @Override
    public int size() {
      return 1;
    }
  }
}
