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

package org.opensearch.sql.data.type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opensearch.sql.data.type.ExprCoreType.ARRAY;
import static org.opensearch.sql.data.type.ExprCoreType.BOOLEAN;
import static org.opensearch.sql.data.type.ExprCoreType.DATE;
import static org.opensearch.sql.data.type.ExprCoreType.DATETIME;
import static org.opensearch.sql.data.type.ExprCoreType.DOUBLE;
import static org.opensearch.sql.data.type.ExprCoreType.FLOAT;
import static org.opensearch.sql.data.type.ExprCoreType.INTEGER;
import static org.opensearch.sql.data.type.ExprCoreType.LONG;
import static org.opensearch.sql.data.type.ExprCoreType.SHORT;
import static org.opensearch.sql.data.type.ExprCoreType.STRING;
import static org.opensearch.sql.data.type.ExprCoreType.STRUCT;
import static org.opensearch.sql.data.type.ExprCoreType.TIME;
import static org.opensearch.sql.data.type.ExprCoreType.TIMESTAMP;
import static org.opensearch.sql.data.type.ExprCoreType.UNDEFINED;
import static org.opensearch.sql.data.type.ExprCoreType.UNKNOWN;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class ExprTypeTest {
  @Test
  public void isCompatible() {
    assertTrue(DOUBLE.isCompatible(DOUBLE));
    assertTrue(DOUBLE.isCompatible(FLOAT));
    assertTrue(DOUBLE.isCompatible(LONG));
    assertTrue(DOUBLE.isCompatible(INTEGER));
    assertTrue(DOUBLE.isCompatible(SHORT));
    assertTrue(FLOAT.isCompatible(FLOAT));
    assertTrue(FLOAT.isCompatible(LONG));
    assertTrue(FLOAT.isCompatible(INTEGER));
    assertTrue(FLOAT.isCompatible(SHORT));

    assertTrue(BOOLEAN.isCompatible(STRING));
    assertTrue(TIMESTAMP.isCompatible(STRING));
    assertTrue(DATE.isCompatible(STRING));
    assertTrue(TIME.isCompatible(STRING));
    assertTrue(DATETIME.isCompatible(STRING));
  }

  @Test
  public void isNotCompatible() {
    assertFalse(INTEGER.isCompatible(DOUBLE));
    assertFalse(STRING.isCompatible(DOUBLE));
    assertFalse(INTEGER.isCompatible(UNKNOWN));
  }

  @Test
  public void isCompatibleWithUndefined() {
    ExprCoreType.coreTypes().forEach(type -> assertTrue(type.isCompatible(UNDEFINED)));
    ExprCoreType.coreTypes().forEach(type -> assertFalse(UNDEFINED.isCompatible(type)));
  }

  @Test
  public void shouldCast() {
    assertTrue(UNDEFINED.shouldCast(STRING));
    assertTrue(STRING.shouldCast(BOOLEAN));
    assertFalse(STRING.shouldCast(STRING));
  }

  @Test
  public void getParent() {
    assertThat(((ExprType) () -> "test").getParent(), Matchers.contains(UNKNOWN));
  }

  @Test
  void legacyName() {
    assertEquals("keyword", STRING.legacyTypeName());
    assertEquals("nested", ARRAY.legacyTypeName());
    assertEquals("object", STRUCT.legacyTypeName());
    assertEquals("integer", INTEGER.legacyTypeName().toLowerCase());
  }

  // for test coverage.
  @Test
  void defaultLegacyTypeName() {
    final ExprType exprType = () -> "dummy";
    assertEquals("dummy", exprType.legacyTypeName());
  }
}
