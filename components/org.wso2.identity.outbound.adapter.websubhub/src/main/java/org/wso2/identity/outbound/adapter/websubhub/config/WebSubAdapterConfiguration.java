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

    private static final String ADAPTER_ENABLED_CONFIG = "adapter.websubhub.enabled";
    private static final String ADAPTER_HUB_URL_CONFIG = "adapter.websubhub.baseUrl";

    private final boolean adapterEnabled;

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
                    .orElseThrow(
                            () -> handleClientException(WEB_SUB_BASE_URL_NOT_CONFIGURED));
        }
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
}
