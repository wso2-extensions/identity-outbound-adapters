/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.identity.outbound.adapter.websubhub.config;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.identity.outbound.adapter.common.OutboundAdapterConfigurationProvider;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterException;

import java.nio.file.Paths;
import java.util.Optional;

import static java.util.Objects.isNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;
import static org.wso2.identity.outbound.adapter.websubhub.config.WebSubAdapterConfiguration.ADAPTER_ENABLED_CONFIG;
import static org.wso2.identity.outbound.adapter.websubhub.config.WebSubAdapterConfiguration.ADAPTER_HUB_URL_CONFIG;
import static org.wso2.identity.outbound.adapter.websubhub.config.WebSubAdapterConfiguration.ADPATER_HTTP_CLIENT_MAX_CONNECTIONS;
import static org.wso2.identity.outbound.adapter.websubhub.config.WebSubAdapterConfiguration.ADPATER_HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE;

public class WebSubAdapterConfigurationTest {

    @BeforeMethod
    public void setup() {

        String carbonConfPath = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty("carbon.config.dir.path", carbonConfPath);
    }

    @DataProvider(name = "websubAdapterPropertyProvider")
    public String[][] provideSecurityEventData() {

        return new String[][]{
                // enabled, baseUrl, max_conn, max_conn_per_route
                {null, null, null, null},
                {"true", "http://localhost:9090/hub", "50", null},
                {"true", null, "50", null},
                {"false", "http://localhost:9090/hub", null, "25"},
                {"false", null, "20", "25"}
        };
    }

    @Test(dataProvider = "websubAdapterPropertyProvider")
    public void testWebSubAdapterConfiguration(String adapterEnabled, String baseURL, String maxConnections,
                                               String maxConnectionsPerRoute) throws WebSubAdapterException {

        OutboundAdapterConfigurationProvider provider = Mockito.mock(OutboundAdapterConfigurationProvider.class);
        Mockito.when(provider.getProperty(ADAPTER_ENABLED_CONFIG)).thenReturn(Optional.ofNullable(adapterEnabled));
        Mockito.when(provider.getProperty(ADAPTER_HUB_URL_CONFIG)).thenReturn(Optional.ofNullable(baseURL));
        Mockito.when(provider.getProperty(ADPATER_HTTP_CLIENT_MAX_CONNECTIONS))
                .thenReturn(Optional.ofNullable(maxConnections));
        Mockito.when(provider.getProperty(ADPATER_HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE))
                .thenReturn(Optional.ofNullable(maxConnectionsPerRoute));

        if (Boolean.parseBoolean(adapterEnabled) && isNull(baseURL)) {
            assertThrows(WebSubAdapterException.class, () -> new WebSubAdapterConfiguration(provider));
            return;
        }

        WebSubAdapterConfiguration configuration = new WebSubAdapterConfiguration(provider);

        assertSame(configuration.isAdapterEnabled(), Boolean.parseBoolean(adapterEnabled));
        assertEquals(configuration.getWebSubHubBaseUrl(), Boolean.parseBoolean(adapterEnabled) ? baseURL : null);
        assertSame(configuration.getHttpClientMaxConnections(),
                Optional.ofNullable(maxConnections).map(Integer::parseInt).orElse(20));
        assertSame(configuration.getHttpClientMaxConnectionsPerRoute(),
                Optional.ofNullable(maxConnectionsPerRoute).map(Integer::parseInt).orElse(20));
    }
}
