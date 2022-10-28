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

package org.wso2.carbon.event.outbound.adapter.websubhub;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.event.outbound.adapter.core.OutboundEventAdapter;
import org.wso2.carbon.event.outbound.adapter.core.exception.ConnectionUnavailableException;
import org.wso2.carbon.event.outbound.adapter.core.exception.OutboundEventAdapterException;
import org.wso2.carbon.event.outbound.adapter.core.exception.TestConnectionNotSupportedException;
import org.wso2.carbon.event.outbound.adapter.websubhub.internal.model.EventPayload;
import org.wso2.carbon.event.outbound.adapter.websubhub.internal.model.SecurityEventTokenPayload;
import org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;

import java.util.Map;

import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.ADAPTER_HUB_URL;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.ADAPTER_MESSAGE_TENANT_DOMAIN;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.ADAPTER_TYPE_WEBSUBHUB;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterUtil.buildSecurityEventToken;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterUtil.makeAsyncAPICall;

/**
 * The WebSubHub event adapter publishes events into a given topic in the intermediate hub to be consumed by a
 * Choreo webhook.
 */
public class WebSubHubEventAdapter implements OutboundEventAdapter {

    private static final Log log = LogFactory.getLog(WebSubHubEventAdapter.class);
    private String webSubHubBaseUrl = null;

    @Override
    public String getType() {

        return ADAPTER_TYPE_WEBSUBHUB;
    }

    @Override
    public void init() throws OutboundEventAdapterException {

    }

    @Override
    public void testConnect() throws TestConnectionNotSupportedException, ConnectionUnavailableException {

    }

    @Override
    public void connect() throws ConnectionUnavailableException {

    }

    @Override
    public void publish(Map<String, Object> payload, String eventType, Map<String, String> propertyMap) {

        if (StringUtils.isEmpty(webSubHubBaseUrl)) {
            populateConfigs();
        }
        if (StringUtils.isEmpty(webSubHubBaseUrl)) {
            log.warn("WebSubHub Base URL is empty. WebSubHubEventPublisher will not engage.");
            return;
        }

        String tenantDomain = propertyMap.get(ADAPTER_MESSAGE_TENANT_DOMAIN);
        //TODO this configurations will be read from configuration through API.
        WebSubHubEventAdapterConstants.EventType event = WebSubHubEventAdapterConstants.EventType.valueOf(eventType);
        String topic = event.getTopic();
        String eventUri = event.getEventUri();
        SecurityEventTokenPayload securityEventTokenPayload =
                buildSecurityEventTokenPayload(payload, tenantDomain, topic, eventUri, eventType);
        makeAsyncAPICall(securityEventTokenPayload, tenantDomain, topic, webSubHubBaseUrl);
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isPolled() {
        return false;
    }

    private SecurityEventTokenPayload buildSecurityEventTokenPayload(Map<String, Object> eventPayload,
        String tenantDomain, String topic, String eventUri, String type) {

        EventPayload payload = buildEventPayload(eventPayload, type, tenantDomain);
        return buildSecurityEventToken(payload, eventUri, topic, tenantDomain);
    }

    //TODO config read
    private void populateConfigs() {

        webSubHubBaseUrl = ADAPTER_HUB_URL;
    }

    private EventPayload buildEventPayload(Map<String, Object> eventPayload, String type, String tenantDomain) {

        EventPayload payload = new EventPayload();
        payload.setEventType(type);
        payload.setOrganizationId(IdentityTenantUtil.getTenantId(tenantDomain));
        payload.setOrganizationName(tenantDomain);
        payload.setPayload(eventPayload);

        return payload;
    }
}
