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
import org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants;

import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ENCRYPTION_KEY_ENDPOINT_URL_NOT_CONFIGURED;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.WEB_SUB_BASE_URL_NOT_CONFIGURED;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterUtil.handleClientException;

/**
 * WebSub Adapter Configuration.
 */
public class WebSubAdapterConfiguration {

    private static final String ADAPTER_ENABLED_CONFIG = "adapter.websubhub.enabled";
    private static final String ENCRYPTION_ENABLED_CONFIG = "adapter.websubhub.encryptionEnabled";
    private static final String ADAPTER_HUB_URL_CONFIG = "adapter.websubhub.baseUrl";
    private static final String HTTP_CONNECTION_TIMEOUT = "adapter.websubhub.httpConnectionTimeout";
    private static final String HTTP_READ_TIMEOUT = "adapter.websubhub.httpReadTimeout";
    private static final String HTTP_CONNECTION_REQUEST_TIMEOUT = "adapter.websubhub.httpConnectionRequestTimeout";
    private static final String DEFAULT_MAX_CONNECTIONS = "adapter.websubhub.defaultMaxConnections";
    private static final String DEFAULT_MAX_CONNECTIONS_PER_ROUTE = "adapter.websubhub.defaultMaxConnectionsPerRoute";
    public static final String ENCRYPTION_KEY_ENDPOINT_URL = "adapter.websubhub.encryptionKeyEndpointUrl";
    // Value for the encryption key cache lifespan in minutes.
    public static final String ENCRYPTION_KEY_CACHE_LIFESPAN = "adapter.websubhub.encryptionKeyCacheLifespan";
    private final boolean adapterEnabled;
    private final boolean encryptionEnabled;
    private final int httpConnectionTimeout;
    private final int httpReadTimeout;
    private final int httpConnectionRequestTimeout;
    private final int defaultMaxConnections;
    private final int defaultMaxConnectionsPerRoute;
    private final int encryptionKeyCacheLifespan;
    private String encryptionKeyEndpointUrl;
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
            // If adapter is enabled, The base URL is mandatory to be configured.
            this.webSubHubBaseUrl = configurationProvider.getProperty(ADAPTER_HUB_URL_CONFIG)
                    .orElseThrow(() -> handleClientException(WEB_SUB_BASE_URL_NOT_CONFIGURED));
        }

        this.encryptionEnabled =
                configurationProvider.getProperty(ENCRYPTION_ENABLED_CONFIG).map(Boolean::parseBoolean).orElse(false);

        if (this.encryptionEnabled) {
            // If encryption is enabled, The encryption key endpoint is mandatory to be configured.
            this.encryptionKeyEndpointUrl = configurationProvider.getProperty(ENCRYPTION_KEY_ENDPOINT_URL)
                    .orElseThrow(() -> handleClientException(ENCRYPTION_KEY_ENDPOINT_URL_NOT_CONFIGURED));
        }

        this.httpConnectionTimeout =
                configurationProvider.getProperty(HTTP_CONNECTION_TIMEOUT).map(Integer::parseInt).orElse(
                        WebSubHubAdapterConstants.DEFAULT_HTTP_CONNECTION_TIMEOUT);
        this.httpReadTimeout =
                configurationProvider.getProperty(HTTP_READ_TIMEOUT).map(Integer::parseInt)
                        .orElse(WebSubHubAdapterConstants.DEFAULT_HTTP_READ_TIMEOUT);
        this.httpConnectionRequestTimeout =
                configurationProvider.getProperty(HTTP_CONNECTION_REQUEST_TIMEOUT).map(Integer::parseInt)
                        .orElse(WebSubHubAdapterConstants.DEFAULT_HTTP_CONNECTION_REQUEST_TIMEOUT);
        this.defaultMaxConnections =
                configurationProvider.getProperty(DEFAULT_MAX_CONNECTIONS).map(Integer::parseInt)
                        .orElse(WebSubHubAdapterConstants.DEFAULT_HTTP_MAX_CONNECTIONS);
        this.defaultMaxConnectionsPerRoute =
                configurationProvider.getProperty(DEFAULT_MAX_CONNECTIONS_PER_ROUTE).map(Integer::parseInt)
                        .orElse(WebSubHubAdapterConstants.DEFAULT_HTTP_MAX_CONNECTIONS_PER_ROUTE);
        this.encryptionKeyCacheLifespan =
                configurationProvider.getProperty(ENCRYPTION_KEY_CACHE_LIFESPAN).map(Integer::parseInt)
                        .orElse(WebSubHubAdapterConstants.DEFAULT_ENCRYPTION_KEY_CACHE_LIFESPAN);
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
     * Getter method to return encryption enable configuration.
     *
     * @return whether encryption is enabled in the configurations.
     */
    public boolean isEncryptionEnabled() {

        return encryptionEnabled;
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
     * Returns the HTTP connection timeout.
     *
     * @return HTTP connection timeout.
     */
    public int getHTTPConnectionTimeout() {

        return httpConnectionTimeout;
    }

    /**
     * Returns the HTTP read timeout.
     *
     * @return HTTP Read Timeout.
     */
    public int getHttpReadTimeout() {

        return httpReadTimeout;
    }

    /**
     * Returns the http connection request timeout.
     *
     * @return http connection request timeout.
     */
    public int getHttpConnectionRequestTimeout() {

        return httpConnectionRequestTimeout;
    }

    /**
     * Returns the default max connections.
     *
     * @return default max connections.
     */
    public int getDefaultMaxConnections() {

        return defaultMaxConnections;
    }

    /**
     * Returns the default max connections per route.
     *
     * @return default max connections per route.
     */
    public int getDefaultMaxConnectionsPerRoute() {

        return defaultMaxConnectionsPerRoute;
    }

    /**
     * Returns the encryption key endpoint URL.
     *
     * @return encryption key endpoint URL.
     */
    public String getEncryptionKeyEndpointUrl() {

        return encryptionKeyEndpointUrl;
    }

    /**
     * Returns the encryption key cache lifespan.
     *
     * @return encryption key cache lifespan.
     */
    public int getEncryptionKeyCacheLifespan() {

        return encryptionKeyCacheLifespan;
    }
}
