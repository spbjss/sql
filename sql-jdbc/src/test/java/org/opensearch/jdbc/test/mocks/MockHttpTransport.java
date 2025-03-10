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
 * Copyright <2019> Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package org.opensearch.jdbc.test.mocks;

import org.opensearch.jdbc.protocol.http.JsonHttpProtocol;
import org.opensearch.jdbc.transport.TransportException;
import org.opensearch.jdbc.transport.http.HttpParam;
import org.opensearch.jdbc.transport.http.HttpTransport;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class MockHttpTransport {

    public static void setupConnectionResponse(HttpTransport mockTransport, CloseableHttpResponse mockResponse)
            throws TransportException {
        when(mockTransport.doGet(eq("/"), any(Header[].class), any(), anyInt()))
                .thenReturn(mockResponse);
    }

    public static void setupQueryResponse(JsonHttpProtocol protocol,
                                          HttpTransport mockTransport, CloseableHttpResponse mockResponse)
            throws TransportException {
        when(mockTransport.doPost(
                eq(protocol.getSqlContextPath()), any(Header[].class), any(HttpParam[].class), anyString(), anyInt()))
                .thenReturn(mockResponse);
    }
}
