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

package org.opensearch.sql.data.model;

import com.google.common.base.Objects;
import org.opensearch.sql.data.type.ExprCoreType;
import org.opensearch.sql.data.type.ExprType;

/**
 * Expression Boolean Value.
 */
public class ExprBooleanValue extends AbstractExprValue {
  private static final ExprBooleanValue TRUE = new ExprBooleanValue(true);
  private static final ExprBooleanValue FALSE = new ExprBooleanValue(false);

  private final Boolean value;

  private ExprBooleanValue(Boolean value) {
    this.value = value;
  }

  public static ExprBooleanValue of(Boolean value) {
    return value ? TRUE : FALSE;
  }

  @Override
  public Object value() {
    return value;
  }

  @Override
  public ExprType type() {
    return ExprCoreType.BOOLEAN;
  }

  @Override
  public Boolean booleanValue() {
    return value;
  }

  @Override
  public String toString() {
    return value.toString();
  }

  @Override
  public int compare(ExprValue other) {
    return Boolean.compare(value, other.booleanValue());
  }

  @Override
  public boolean equal(ExprValue other) {
    return value.equals(other.booleanValue());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }
}
