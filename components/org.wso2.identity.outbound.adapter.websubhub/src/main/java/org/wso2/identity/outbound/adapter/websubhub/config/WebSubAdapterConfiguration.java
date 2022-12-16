/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.identity.outbound.adapter.common.OutboundAdapterConfigurationProvider;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterException;

import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.WEB_SUB_BASE_URL_NOT_CONFIGURED;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterUtil.handleClientException;

/**
 * WebSub Adapter Configuration.
 */
public class WebSubAdapterConfiguration {

    static final String ADAPTER_ENABLED_CONFIG = "adapter.websubhub.enabled";
    static final String ADAPTER_HUB_URL_CONFIG = "adapter.websubhub.baseUrl";
    static final String ADPATER_HTTP_CLIENT_MAX_CONNECTIONS = "adapter.websubhub.client.maxTotalConnections";
    static final String ADPATER_HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE =
            "adapter.websubhub.client.maxTotalConnectionsPerRoute";

    private static final int DEFAULT_MAX_CONNECTIONS = 20;

    private final boolean adapterEnabled;
    private final int httpClientMaxConnections;
    private final int httpClientMaxConnectionsPerRoute;
    private String webSubHubBaseUrl;

    /**
     * Initialize the {@link WebSubAdapterConfiguration}.
     *
     * @param configurationProvider Outbound adapter configuration provider.
     * @throws WebSubAdapterException on failures when creating the configuration object.
     */
    public WebSubAdapterConfiguration(OutboundAdapterConfigurationProvider configurationProvider)
            throws WebSubAdapterException {

        this.adapterEnabled =
                configurationProvider.getProperty(ADAPTER_ENABLED_CONFIG).map(Boolean::parseBoolean).orElse(false);
        if (this.adapterEnabled) {
            // If adapter is enabled, base URL is mandatory to be configured.
            this.webSubHubBaseUrl = configurationProvider.getProperty(ADAPTER_HUB_URL_CONFIG)
                    .orElseThrow(() -> handleClientException(WEB_SUB_BASE_URL_NOT_CONFIGURED));
        }
        this.httpClientMaxConnections =
                configurationProvider.getProperty(ADPATER_HTTP_CLIENT_MAX_CONNECTIONS).map(Integer::parseInt)
                        .orElse(DEFAULT_MAX_CONNECTIONS);
        this.httpClientMaxConnectionsPerRoute =
                configurationProvider.getProperty(ADPATER_HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE).map(Integer::parseInt)
                        .orElse(DEFAULT_MAX_CONNECTIONS);
    }

    /**
     * Getter method to return adapter enable configuration.
     *
     * @return whether adapter is enabled in the configurations.
     */
    public boolean isAdapterEnabled() {

        return adapterEnabled;
    }

    /**
     * Returns the base URL of the WebSub Hub.
     *
     * @return base URL of the WebSub Hub.
     */
    public String getWebSubHubBaseUrl() {

        return webSubHubBaseUrl;
    }

    /**
     * Returns the configuration value for max number of connections to use in the http connection pool manager.
     *
     * @return max number of connections to use in http connection pool manager.
     */
    public int getHttpClientMaxConnections() {

        return httpClientMaxConnections;
    }

    /**
     * Returns the configuration value for max connections per route to use in the http connection pool manager.
     *
     * @return max connections per route to use in http connection pool manager.
     */
    public int getHttpClientMaxConnectionsPerRoute() {

        return httpClientMaxConnectionsPerRoute;
    }
}
