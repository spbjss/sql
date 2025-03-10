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

package org.opensearch.sql.legacy.unittest.rewriter;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.druid.sql.ast.expr.SQLQueryExpr;
import java.sql.SQLFeatureNotSupportedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensearch.sql.legacy.rewriter.RewriteRule;
import org.opensearch.sql.legacy.rewriter.RewriteRuleExecutor;

@RunWith(MockitoJUnitRunner.class)
public class RewriteRuleExecutorTest {
    @Mock
    private RewriteRule<SQLQueryExpr> rewriter;
    @Mock
    private SQLQueryExpr expr;

    private RewriteRuleExecutor<SQLQueryExpr> ruleExecutor;

    @Before
    public void setup() {
        ruleExecutor = RewriteRuleExecutor.<SQLQueryExpr>builder().withRule(rewriter).build();
    }

    @Test
    public void optimize() throws SQLFeatureNotSupportedException {
        when(rewriter.match(expr)).thenReturn(true);

        ruleExecutor.executeOn(expr);
        verify(rewriter, times(1)).rewrite(expr);
    }

    @Test
    public void noOptimize() throws SQLFeatureNotSupportedException {
        when(rewriter.match(expr)).thenReturn(false);

        ruleExecutor.executeOn(expr);
        verify(rewriter, never()).rewrite(expr);
    }
}
