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
import org.wso2.carbon.event.outbound.adapter.websubhub.model.EventPayload;
import org.wso2.carbon.event.outbound.adapter.websubhub.model.SecurityEventTokenPayload;

import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.ADAPTER_HUB_URL;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterUtil.buildSecurityEventToken;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterUtil.makeAsyncAPICall;

/**
 * OSGi service for publishing events using web sub hub.
 */
public class WebSubHubAdapterServiceImpl implements WebSubHubAdapterService {

    private static final Log log = LogFactory.getLog(WebSubHubAdapterServiceImpl.class);
    private String webSubHubBaseUrl = null;

    @Override
    public void publish(EventPayload eventPayload, String topicSuffix, String eventUri) {

        if (StringUtils.isEmpty(webSubHubBaseUrl)) {
            populateConfigs();
        }
        if (StringUtils.isEmpty(webSubHubBaseUrl)) {
            log.warn("WebSubHub Base URL is empty. WebSubHubEventPublisher will not engage.");
            return;
        }

        //TODO Validate that tenant domain is set
        //Getting the organization name of Event Payload object since it is the tenant domain.
        String tenantDomain = eventPayload.getOrganizationName();
        SecurityEventTokenPayload securityEventTokenPayload =
                buildSecurityEventToken(eventPayload, eventUri, topicSuffix);
        makeAsyncAPICall(securityEventTokenPayload, tenantDomain, topicSuffix, webSubHubBaseUrl);
    }

    @Override
    public void addTopic(String topic) {
        // todo logic to add the topic into the hub
    }

    @Override
    public void removeTopic(String topic) {
        // todo logic to remove the topic from the hub
    }

    /**
     * Populate the web sub hub base url configuration.
     */
    private void populateConfigs() {

        //TODO read from the configurations.
        webSubHubBaseUrl = ADAPTER_HUB_URL;
    }
}
