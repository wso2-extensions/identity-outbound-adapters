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

package org.wso2.carbon.event.outbound.adapter.websubhub.services;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.event.outbound.adapter.websubhub.WebSubHubAdapterService;
import org.wso2.carbon.event.outbound.adapter.websubhub.exception.WebSubAdapterException;
import org.wso2.carbon.event.outbound.adapter.websubhub.model.EventPayload;
import org.wso2.carbon.event.outbound.adapter.websubhub.model.SecurityEventTokenPayload;
import org.wso2.identity.outbound.adapter.common.OutboundAdapterConfiguration;
import org.wso2.identity.outbound.adapter.common.exception.AdapterConfigurationException;

import java.io.IOException;

import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.ADAPTER_HUB_URL_CONFIG;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.DEREGISTER;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.ErrorMessages.ERROR_DEREGISTERING_HUB_TOPIC;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.ErrorMessages.ERROR_REGISTERING_HUB_TOPIC;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.ErrorMessages.ERROR_RETRIEVING_WEB_SUB_BASE_URL_CONFIG;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.ErrorMessages.WEB_SUB_BASE_URL_NOT_CONFIGURED;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.REGISTER;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterUtil.buildSecurityEventToken;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterUtil.handleClientException;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterUtil.handleServerException;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterUtil.makeAsyncAPICall;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterUtil.makeTopicMgtAPICall;

/**
 * OSGi service for publishing events using web sub hub.
 */
public class WebSubHubAdapterServiceImpl implements WebSubHubAdapterService {

    private static final Log log = LogFactory.getLog(WebSubHubAdapterServiceImpl.class);
    private String webSubHubBaseUrl = null;

    @Override
    public void publish(EventPayload eventPayload, String topicSuffix, String eventUri) throws WebSubAdapterException {

        //Getting the organization name of Event Payload object since it is the tenant domain.
        String tenantDomain = eventPayload.getOrganizationName();
        SecurityEventTokenPayload securityEventTokenPayload =
                buildSecurityEventToken(eventPayload, eventUri, topicSuffix);
        makeAsyncAPICall(securityEventTokenPayload, tenantDomain, topicSuffix, getWebSubBaseURL());
    }

    @Override
    public void registerTopic(String topic, String tenantDomain) throws WebSubAdapterException {

        try {
            makeTopicMgtAPICall(topic, tenantDomain, getWebSubBaseURL(), REGISTER);
        } catch (IOException e) {
            throw handleServerException(ERROR_REGISTERING_HUB_TOPIC, e, topic, tenantDomain);
        }

    }

    @Override
    public void deregisterTopic(String topic, String tenantDomain) throws WebSubAdapterException {

        try {
            makeTopicMgtAPICall(topic, tenantDomain, getWebSubBaseURL(), DEREGISTER);
        } catch (IOException e) {
            throw handleServerException(ERROR_DEREGISTERING_HUB_TOPIC, e, topic, tenantDomain);
        }
    }

    private String getWebSubBaseURL() throws WebSubAdapterException {

        if (StringUtils.isEmpty(webSubHubBaseUrl)) {
            populateConfigs();

            if (StringUtils.isEmpty(webSubHubBaseUrl)) {
                log.warn("WebSubHub Base URL is empty. WebSubHubEventPublisher will not engage.");
                throw handleClientException(WEB_SUB_BASE_URL_NOT_CONFIGURED);
            }
        }
        return webSubHubBaseUrl;
    }

    /**
     * Populate the web sub hub base url configuration.
     */
    private void populateConfigs() throws WebSubAdapterException {

        try {
            webSubHubBaseUrl = OutboundAdapterConfiguration.getInstance().getProperty(ADAPTER_HUB_URL_CONFIG)
                    .orElseThrow(() -> handleClientException(WEB_SUB_BASE_URL_NOT_CONFIGURED));
        } catch (AdapterConfigurationException e) {
            throw handleServerException(ERROR_RETRIEVING_WEB_SUB_BASE_URL_CONFIG, e);
        }
    }
}
