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

package org.wso2.identity.outbound.adapter.websubhub.service;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.identity.outbound.adapter.websubhub.WebSubHubAdapterService;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterException;
import org.wso2.identity.outbound.adapter.websubhub.internal.WebSubHubAdapterDataHolder;
import org.wso2.identity.outbound.adapter.websubhub.model.EventPayload;
import org.wso2.identity.outbound.adapter.websubhub.model.SecurityEventTokenPayload;

import java.io.IOException;

import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.DEREGISTER;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_DEREGISTERING_HUB_TOPIC;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_REGISTERING_HUB_TOPIC;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.WEB_SUB_BASE_URL_NOT_CONFIGURED;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.WEB_SUB_HUB_ADAPTER_DISABLED;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.REGISTER;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.TOPIC_SEPARATOR;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterUtil.buildSecurityEventToken;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterUtil.handleClientException;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterUtil.handleServerException;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterUtil.makeAsyncAPICall;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterUtil.makeTopicMgtAPICall;

/**
 * OSGi service for publishing events using web sub hub.
 */
public class WebSubHubAdapterServiceImpl implements WebSubHubAdapterService {

    private static final Log log = LogFactory.getLog(WebSubHubAdapterServiceImpl.class);
    private String webSubHubBaseUrl = null;

    @Override
    public void publish(EventPayload eventPayload, String topicSuffix, String eventUri) throws WebSubAdapterException {

        if (WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().isAdapterEnabled()) {
            //Getting the organization name of Event Payload object since it is the tenant domain.
            String tenantDomain = eventPayload.getOrganizationName();
            SecurityEventTokenPayload securityEventTokenPayload =
                    buildSecurityEventToken(eventPayload, eventUri, topicSuffix);
            makeAsyncAPICall(securityEventTokenPayload, tenantDomain, constructHubTopic(topicSuffix, tenantDomain),
                    getWebSubBaseURL());
        } else {
            log.warn("Event cannot be published, WebSub Hub Adapter is not enabled.");
            throw handleClientException(WEB_SUB_HUB_ADAPTER_DISABLED, "event publishing");
        }
    }

    @Override
    public void registerTopic(String topicSuffix, String tenantDomain) throws WebSubAdapterException {

        if (WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().isAdapterEnabled()) {
            try {
                makeTopicMgtAPICall(constructHubTopic(topicSuffix, tenantDomain), getWebSubBaseURL(), REGISTER);
            } catch (IOException e) {
                throw handleServerException(ERROR_REGISTERING_HUB_TOPIC, e, topicSuffix, tenantDomain);
            }
        } else {
            log.warn("WebSub Hub Topic cannot be registered, WebSub Hub Adapter is not enabled.");
            throw handleClientException(WEB_SUB_HUB_ADAPTER_DISABLED, "topic registration");
        }
    }

    @Override
    public void deregisterTopic(String topicSuffix, String tenantDomain) throws WebSubAdapterException {

        if (WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().isAdapterEnabled()) {
            try {
                makeTopicMgtAPICall(constructHubTopic(topicSuffix, tenantDomain), getWebSubBaseURL(), DEREGISTER);
            } catch (IOException e) {
                throw handleServerException(ERROR_DEREGISTERING_HUB_TOPIC, e, topicSuffix, tenantDomain);
            }
        } else {
            log.warn("WebSub Hub Topic cannot be de-registered, WebSub Hub Adapter is not enabled.");
            throw handleClientException(WEB_SUB_HUB_ADAPTER_DISABLED, "topic registration");
        }
    }

    private String getWebSubBaseURL() throws WebSubAdapterException {

        if (StringUtils.isEmpty(webSubHubBaseUrl)) {
            webSubHubBaseUrl =
                    WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().getWebSubHubBaseUrl();

            // At this point, url shouldn't be null since if adapter is enabled, url is mandatory to configured.
            // But adding this as a second level verification.
            if (StringUtils.isEmpty(webSubHubBaseUrl)) {
                log.warn("WebSubHub Base URL is empty. WebSubHubEventPublisher will not engage.");
                throw handleClientException(WEB_SUB_BASE_URL_NOT_CONFIGURED);
            }
        }
        return webSubHubBaseUrl;
    }

    private String constructHubTopic(String topicSuffix, String tenantDomain) {

        return tenantDomain + TOPIC_SEPARATOR + topicSuffix;
    }
}
