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

package org.wso2.identity.outbound.adapter.websubhub.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.MDC;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterClientException;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterException;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterServerException;
import org.wso2.identity.outbound.adapter.websubhub.internal.WebSubHubAdapterDataHolder;
import org.wso2.identity.outbound.adapter.websubhub.model.EventPayload;
import org.wso2.identity.outbound.adapter.websubhub.model.SecurityEventTokenPayload;
//import org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubCorrelationLogUtils.RequestStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils.CORRELATION_ID_MDC;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.AUDIENCE_BASE_URL;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.CORRELATION_ID_REQUEST_HEADER;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.DEREGISTER;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ERROR_TOPIC_DEREG_FAILURE_ACTIVE_SUBS;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.EVENT_ISSUER;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_BACKEND_ERROR_FROM_WEBSUB_HUB;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_EMPTY_RESPONSE_FROM_WEBSUB_HUB;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_EVENT_ORGANIZATION_NAME;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_EVENT_TOPIC;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_EVENT_URI;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_RESPONSE_FROM_WEBSUB_HUB;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_TOPIC;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_WEB_SUB_HUB_BASE_URL;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_WEB_SUB_OPERATION;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_NULL_EVENT_PAYLOAD;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_PUBLISHING_EVENT_INVALID_PAYLOAD;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.TOPIC_DEREGISTRATION_FAILURE_ACTIVE_SUBS;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.HUB_ACTIVE_SUBS;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.HUB_MODE;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.HUB_REASON;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.HUB_TOPIC;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.PAYLOAD_EVENT_JSON_KEY;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.PUBLISH;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.REGISTER;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.RESPONSE_FOR_SUCCESSFUL_OPERATION;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.URL_KEY_VALUE_SEPARATOR;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.URL_PARAM_SEPARATOR;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.URL_SEPARATOR;

/**
 * This class contains the utility method implementations required by WebSub Hub outbound adapter.
 */
public class WebSubHubAdapterUtil {

    private static final Log log = LogFactory.getLog(WebSubHubAdapterUtil.class);

    private WebSubHubAdapterUtil() {

    }

    /**
     * Build Security Event Token object for the given event payload.
     *
     * @param eventPayload Event payload object.
     * @param eventUri     Event URI.
     * @param topic        Topic name.
     * @return Security Event Token payload.
     */
    public static SecurityEventTokenPayload buildSecurityEventToken(EventPayload eventPayload, String eventUri,
                                                                    String topic) throws WebSubAdapterClientException {

        if (eventPayload == null) {
            throw handleClientException(ERROR_NULL_EVENT_PAYLOAD);
        }

        if (StringUtils.isEmpty(eventUri)) {
            throw handleClientException(ERROR_INVALID_EVENT_URI);
        }

        if (StringUtils.isEmpty(topic)) {
            throw handleClientException(ERROR_INVALID_EVENT_TOPIC);
        }

        if (StringUtils.isEmpty(eventPayload.getOrganizationName())) {
            throw handleClientException(ERROR_INVALID_EVENT_ORGANIZATION_NAME);
        }

        SecurityEventTokenPayload securityEventTokenPayload = new SecurityEventTokenPayload();
        securityEventTokenPayload.setIss(EVENT_ISSUER);
        securityEventTokenPayload.setIat(System.currentTimeMillis());
        securityEventTokenPayload.setJti(UUID.randomUUID().toString());
        securityEventTokenPayload.setAud(getAudience(topic, eventPayload.getOrganizationName()));
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

        return AUDIENCE_BASE_URL + tenantDomain + URL_SEPARATOR + topic;
    }

    /**
     * Publish event to the web sub hub as an asynchronous API call.
     *
     * @param securityEventTokenPayload Security Event Token object.
     * @param tenantDomain              Tenant domain.
     * @param topic                     Topic name.
     * @param webSubHubBaseUrl          Web sub hub base url.
     * @throws WebSubAdapterException on error while publishing the events.
     */
    public static void makeAsyncAPICall(SecurityEventTokenPayload securityEventTokenPayload, String tenantDomain,
                                        String topic, String webSubHubBaseUrl) throws WebSubAdapterException {

        String url = buildURL(topic, webSubHubBaseUrl, PUBLISH);

        SimpleRequestBuilder requestBuilder = SimpleRequestBuilder.post(url)
                .setHeader(ACCEPT, ContentType.APPLICATION_JSON.getMimeType())
                .setHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .setHeader(CORRELATION_ID_REQUEST_HEADER, getCorrelationID());

        ObjectMapper mapper = new ObjectMapper();
        String jsonString;
        try {
            jsonString = mapper.writeValueAsString(securityEventTokenPayload);
            // Encrypt the event object in the payload.
            if (WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().isEncryptionEnabled()) {
                JSONParser jsonParser = new JSONParser();
                JSONObject payloadJSON = (JSONObject) jsonParser.parse(jsonString);
                payloadJSON.put(PAYLOAD_EVENT_JSON_KEY, EventPayloadCryptographyUtils.encryptEventPayload(
                        payloadJSON.get(PAYLOAD_EVENT_JSON_KEY).toString(), tenantDomain));
                jsonString = payloadJSON.toString();
            }
            requestBuilder.setBody(jsonString, org.apache.hc.core5.http.ContentType.APPLICATION_JSON);
        } catch (IOException | IdentityEventException | ParseException e) {
            throw handleClientException(ERROR_PUBLISHING_EVENT_INVALID_PAYLOAD);
        }

        CloseableHttpAsyncClient client = WebSubHubAdapterDataHolder.getInstance().getClientManager().getClient();

        if (log.isDebugEnabled()) {
            log.debug("Publishing event data to WebSubHub. URL: " + url + " tenant domain: " + tenantDomain);
        }

        SimpleHttpRequest request = requestBuilder.build();

        // TODO: Correlation Log for request.
//        WebSubHubCorrelationLogUtils.triggerCorrelationLogForRequest(request);
//        final long requestStartTime = System.currentTimeMillis();
        client.execute(request, new FutureCallback<SimpleHttpResponse>() {
            @Override
            public void completed(final SimpleHttpResponse response) {

                int responseCode = response.getCode();
//                String responsePhrase = response.getReasonPhrase();
                if (log.isDebugEnabled()) {
                    log.debug("WebSubHub request completed. Response code: " + responseCode);
                }

                // TODO: Correlation Log for response.
//                WebSubHubCorrelationLogUtils.triggerCorrelationLogForResponse(request, requestStartTime,
//                        RequestStatus.COMPLETED.getStatus(), String.valueOf(responseCode), responsePhrase);

                if (responseCode == 200 || responseCode == 201 || responseCode == 202 || responseCode == 204) {
                    // Check for 200 success code range.
                    if (log.isDebugEnabled()) {
                        String responseBody = response.getBodyText();
                        log.debug("Response data: " + responseBody);
                    }
                } else {
                    log.error("WebHubSub event publisher received " + responseCode + " code.");
                    String responseBody = response.getBodyText();
                    log.debug("Response data: " + responseBody);
                }
            }

            @Override
            public void failed(Exception ex) {

                // TODO: Correlation Log for error.
//                WebSubHubCorrelationLogUtils.triggerCorrelationLogForResponse(request, requestStartTime,
//                        RequestStatus.FAILED.getStatus(), ex.getMessage());
                log.error("Publishing event data to WebSubHub failed. ", ex);
            }

            @Override
            public void cancelled() {

                // TODO: Correlation Log for error 2.
//                WebSubHubCorrelationLogUtils.triggerCorrelationLogForResponse(request, requestStartTime,
//                        RequestStatus.CANCELLED.getStatus());
                log.error("Publishing event data to WebSubHub cancelled.");
            }
        });

    }

    /**
     * Invoke the WebSub Hub to register, unregister the topics.
     *
     * @param topic                     topic name.
     * @param webSubHubBaseUrl          WebSub Hub base URL.
     * @param operation                 whether to register, deregister the topic.
     * @throws IOException              on errors while communicating with WebSub hub.
     * @throws WebSubAdapterException   on client or server related errors.
     */
    public static void makeTopicMgtAPICall(String topic, String webSubHubBaseUrl, String operation)
            throws IOException, WebSubAdapterException {

        String topicMgtUrl = buildURL(topic, webSubHubBaseUrl, operation);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build()) {

            HttpPost httpPost = new HttpPost(topicMgtUrl);
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int responseCode = response.getStatusLine().getStatusCode();
                if (responseCode == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String responseString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                        if (RESPONSE_FOR_SUCCESSFUL_OPERATION.equals(responseString)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Success WebSub Hub operation: " + operation + ", topic: " + topic);
                            }
                        } else {
                            throw handleServerException(ERROR_INVALID_RESPONSE_FROM_WEBSUB_HUB, null, topic,
                                    operation, responseString);
                        }
                    } else {
                        String message =
                                String.format(ERROR_EMPTY_RESPONSE_FROM_WEBSUB_HUB.getDescription(), topic, operation);
                        throw handleServerException(message, ERROR_EMPTY_RESPONSE_FROM_WEBSUB_HUB.getCode());
                    }
                } else if ((responseCode == HttpStatus.SC_CONFLICT && operation.equals(REGISTER)) ||
                        (responseCode == HttpStatus.SC_NOT_FOUND && operation.equals(DEREGISTER))) {
                    // Since the endpoint responds with http status code 409 for registration or 404 for
                    // de-registration, adapter accepts the response as a success and log it as a warning.
                    // In current implementation this only happens,
                    // 1. if the topic exists when registering the topic (409).
                    // 2. if the topic doesn't exist when de-registering the topic (404).
                    HttpEntity entity = response.getEntity();
                    String responseString = "";
                    if (entity != null) {
                        responseString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    }
                    log.warn(String.format(ERROR_INVALID_RESPONSE_FROM_WEBSUB_HUB.getDescription(),
                            topic, operation, responseString));
                } else {
                    if (responseCode == HttpStatus.SC_FORBIDDEN) {
                        Map<String, String> hubResponse = parseEventHubResponse(response);
                        if (!hubResponse.isEmpty() && hubResponse.containsKey(HUB_REASON)) {
                            String errorMsg = String.format(ERROR_TOPIC_DEREG_FAILURE_ACTIVE_SUBS, topic);
                            // If topic de-registration failed due to active subscriptions, throw a client exception.
                            if (errorMsg.equals(hubResponse.get(HUB_REASON))) {
                                log.info(String.format(TOPIC_DEREGISTRATION_FAILURE_ACTIVE_SUBS.getDescription(),
                                        topic, hubResponse.get(HUB_ACTIVE_SUBS)));
                                throw handleClientException(TOPIC_DEREGISTRATION_FAILURE_ACTIVE_SUBS, topic,
                                        hubResponse.get(HUB_ACTIVE_SUBS));
                            }
                        }
                    }
                    HttpEntity entity = response.getEntity();
                    String responseString = "";
                    if (entity != null) {
                        responseString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    }
                    String message =
                            String.format(ERROR_BACKEND_ERROR_FROM_WEBSUB_HUB.getDescription(), topic, operation,
                                    responseString);
                    log.error(message + ", Response code:" + responseCode);
                    throw handleServerException(message, ERROR_BACKEND_ERROR_FROM_WEBSUB_HUB.getCode());
                }
            }
        }
    }

    /**
     * Build url which is used to publish events of the given tenant domain and topic.
     *
     * @param topic            Topic name.
     * @param webSubHubBaseUrl Web sub hub base url.
     * @return Url to publish the event.
     */
    private static String buildURL(String topic, String webSubHubBaseUrl, String operation)
            throws WebSubAdapterClientException {

        if (topic == null) {
            throw handleClientException(ERROR_INVALID_TOPIC);
        }

        if (webSubHubBaseUrl == null) {
            throw handleClientException(ERROR_INVALID_WEB_SUB_HUB_BASE_URL);
        }

        if (operation == null) {
            throw handleClientException(ERROR_INVALID_WEB_SUB_OPERATION);
        }

        return webSubHubBaseUrl + "?" + HUB_MODE + "=" + operation + "&" + HUB_TOPIC + "=" + topic;
    }

    /**
     * Get correlation id from the MDC.
     * If not then generate a random UUID and return the UUID.
     *
     * @return Correlation id
     */
    public static String getCorrelationID() {

        return Optional.ofNullable(MDC.get(CORRELATION_ID_MDC)).orElse(UUID.randomUUID().toString());
    }

    /**
     * Returns a {@link WebSubAdapterClientException} on client related errors in WebSub Adapter.
     *
     * @param error the error enum.
     * @param data  the error related data.
     * @return WebSubAdapterClientException
     */
    public static WebSubAdapterClientException handleClientException(
            WebSubHubAdapterConstants.ErrorMessages error, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new WebSubAdapterClientException(error.getMessage(), description, error.getCode());
    }

    /**
     * Returns a {@link WebSubAdapterServerException} on server related errors in WebSub Adapter.
     *
     * @param error the error enum.
     * @param data  the error related data.
     * @return WebSubAdapterServerException
     */
    public static WebSubAdapterServerException handleServerException(
            WebSubHubAdapterConstants.ErrorMessages error, Throwable throwable, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new WebSubAdapterServerException(error.getMessage(), description, error.getCode(), throwable);
    }

    /**
     * Returns a {@link WebSubAdapterServerException} on server related errors in WebSub Adapter.
     *
     * @param message the error message.
     * @param errorCode  the error code.
     * @return WebSubAdapterServerException
     */
    public static WebSubAdapterServerException handleServerException(String message, String errorCode) {

        return new WebSubAdapterServerException(message, errorCode);
    }

    /**
     * This method parses the urlencoded response from the event hub and returns the contents as a map.
     *
     * @param response Response from the event hub.
     * @return Map of the response content.
     * @throws IOException If an error occurs while parsing the response.
     */
    public static Map<String, String> parseEventHubResponse(CloseableHttpResponse response) throws IOException {

        Map<String, String> map = new HashMap<>();
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            String responseContent = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            if (log.isDebugEnabled()) {
                log.debug("Parsing response content from event hub: " + responseContent);
            }
            String[] responseParams = responseContent.split(URL_PARAM_SEPARATOR);
            for (String param : responseParams) {
                String[] keyValuePair = param.split(URL_KEY_VALUE_SEPARATOR);
                // keyValuePair should contain key and value.
                if (keyValuePair.length == 2) {
                    map.put(keyValuePair[0], keyValuePair[1]);
                }
            }
        }
        return map;
    }

}
