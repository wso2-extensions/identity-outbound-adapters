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

package org.wso2.carbon.event.outbound.adapter.websubhub.internal.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.MDC;
import org.wso2.carbon.event.outbound.adapter.websubhub.internal.ds.WebSubHubEventAdapterDataHolder;
import org.wso2.carbon.event.outbound.adapter.websubhub.internal.model.EventPayload;
import org.wso2.carbon.event.outbound.adapter.websubhub.internal.model.SecurityEventTokenPayload;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.AUDIENCE_BASE_URL;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.CORRELATION_ID_REQUEST_HEADER;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.EVENT_ISSUER;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.HUB_MODE;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.HUB_TOPIC;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.PUBLISH;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.TOPIC_SEPARATOR;
import static org.wso2.carbon.event.outbound.adapter.websubhub.internal.util.WebSubHubEventAdapterConstants.URL_SEPARATOR;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.ContentTypes.TYPE_APPLICATION_JSON;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils.CORRELATION_ID_MDC;

/**
 * This class contains the utility method implementations.
 */
public class WebSubHubEventAdapterUtil {

    private static final Log log = LogFactory.getLog(WebSubHubEventAdapterUtil.class);

    /**
     * Build Security Event Token object for the given event payload.
     *
     * @param eventPayload Event payload object.
     * @param eventUri     Event URI.
     * @param topic        Topic name.
     * @param tenantDomain Tenant domain.
     * @return Security Event Token payload.
     */
    public static SecurityEventTokenPayload buildSecurityEventToken(EventPayload eventPayload, String eventUri,
                                                                    String topic, String tenantDomain) {

        SecurityEventTokenPayload securityEventTokenPayload = new SecurityEventTokenPayload();
        securityEventTokenPayload.setIss(EVENT_ISSUER);
        securityEventTokenPayload.setIat(System.currentTimeMillis());
        securityEventTokenPayload.setJti(UUID.randomUUID().toString());
        securityEventTokenPayload.setAud(getAudience(topic, tenantDomain));
        Map<String, EventPayload> eventMap = new HashMap<>();
        eventMap.put(eventUri, eventPayload);
        securityEventTokenPayload.setEvent(eventMap);
        return securityEventTokenPayload;
    }

    /**
     * Get audience for the given topic and tenant.
     *
     * @param topic        Topic name.
     * @param tenantDomain Tenant domain.
     * @return Audience value.
     */
    private static String getAudience(String topic, String tenantDomain) {

        StringBuilder audience = new StringBuilder(AUDIENCE_BASE_URL);
        audience = audience.append(tenantDomain).append(URL_SEPARATOR).append(topic);
        return audience.toString();
    }

    /**
     * Publish event to the web sub hub as an asynchronous API call.
     *
     * @param securityEventTokenPayload Security Event Token object.
     * @param tenantDomain              Tenant domain.
     * @param topic                     Topic name.
     * @param webSubHubBaseUrl          Web sub hub base url.
     */
    public static void makeAsyncAPICall(SecurityEventTokenPayload securityEventTokenPayload, String tenantDomain,
                                        String topic, String webSubHubBaseUrl) {

        String url = buildURL(topic, tenantDomain, webSubHubBaseUrl);
        try {
            HttpPost request = new HttpPost(url);
            request.setHeader(ACCEPT, TYPE_APPLICATION_JSON);
            request.setHeader(CONTENT_TYPE, TYPE_APPLICATION_JSON);
            request.setHeader(CORRELATION_ID_REQUEST_HEADER, getCorrelationID());

            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(securityEventTokenPayload);

            request.setEntity(new StringEntity(jsonString));
            CloseableHttpAsyncClient client = WebSubHubEventAdapterDataHolder.getInstance().getClientManager()
                    .getClient(tenantDomain);

            if (log.isDebugEnabled()) {
                log.debug("Publishing event data to WebSubHub. URL: " + url + " tenant domain: "
                        + tenantDomain + " payload:");
            }

            client.execute(request, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(final HttpResponse response) {

                    int responseCode = response.getStatusLine().getStatusCode();
                    if (log.isDebugEnabled()) {
                        log.debug("WebSubHub request completed. Response code: " + responseCode);
                    }
                    if (responseCode == 200 || responseCode == 201 || responseCode == 202 || responseCode == 204) {
                        // Check for 200 success code range.
                        if (log.isDebugEnabled()) {
                            String jsonString;
                            try {
                                jsonString = EntityUtils.toString(response.getEntity());
                                JSONParser parser = new JSONParser();
                                JSONObject json = (JSONObject) parser.parse(jsonString);
                                log.debug("Response data: " + json);
                            } catch (IOException | ParseException e) {
                                log.error("Error while reading WebSubHub event publisher response. " + e);
                            }
                        }
                    } else {
                        log.error("WebHubSub event publisher received " + responseCode + " code.");
                        String jsonString;
                        try {
                            jsonString = EntityUtils.toString(response.getEntity());
                            JSONParser parser = new JSONParser();
                            JSONObject json = (JSONObject) parser.parse(jsonString);
                            log.error("Response data: " + json);
                        } catch (IOException | ParseException e) {
                            log.error("Error while reading WebSubHub event publisher response. " + e);
                        }
                    }
                }

                @Override
                public void failed(final Exception ex) {

                    log.error("Publishing event data to WebSubHub failed. " + ex);
                }

                @Override
                public void cancelled() {

                    log.error("Publishing event data to WebSubHub cancelled.");
                }
            });
        } catch (IOException | FrameworkException e) {
            log.error("Error while publishing event data to WebSubHub. " + e);
        }
    }

    /**
     * Build url which is used to publish events of the given tenant domain and topic.
     *
     * @param topic            Topic name.
     * @param tenantDomain     Tenant domain.
     * @param webSubHubBaseUrl Web sub hub base url.
     * @return Url to publish the event.
     */
    private static String buildURL(String topic, String tenantDomain, String webSubHubBaseUrl) {

        StringBuilder tenantedTopic = new StringBuilder(tenantDomain).append(TOPIC_SEPARATOR).append(topic);
        StringBuilder eventPublishingUrl = new StringBuilder(webSubHubBaseUrl);
        eventPublishingUrl = eventPublishingUrl.append("?").append(HUB_MODE).append("=")
                .append(PUBLISH).append("&").
                append(HUB_TOPIC).append("=").append(tenantedTopic.toString());
        return eventPublishingUrl.toString();
    }

    /**
     * Get correlation id from the MDC.
     * If not then generate a random UUID and return the UUID.
     *
     * @return Correlation id
     */
    public static String getCorrelationID() {

        String correlationId;
        if (isCorrelationIDPresent()) {
            correlationId = MDC.get(CORRELATION_ID_MDC).toString();
        } else {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private static boolean isCorrelationIDPresent() {

        return MDC.get(CORRELATION_ID_MDC) != null;
    }
}
