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

/**
 * Keep constants required by the WebSubHub Event Adapter.
 */
public class WebSubHubAdapterConstants {

    public static final String EVENT_ISSUER = "Asgardeo";
    public static final String AUDIENCE_BASE_URL = "https://websubhub/topics/";
    public static final String URL_SEPARATOR = "/";
    public static final String TOPIC_SEPARATOR = "-";
    public static final String PUBLISH = "publish";
    public static final String HUB_MODE = "hub.mode";
    public static final String HUB_TOPIC = "hub.topic";
    public static final String REGISTER = "register";
    public static final String DEREGISTER = "deregister";
    public static final String ACCEPTED = "accepted";
    public static final String RESPONSE_FOR_SUCCESSFUL_OPERATION = HUB_MODE + "=" + ACCEPTED;
    public static final String CORRELATION_ID_REQUEST_HEADER = "activityid";
    public static final Integer DEFAULT_HTTP_CONNECTION_TIMEOUT = 300;
    public static final Integer DEFAULT_HTTP_READ_TIMEOUT = 300;
    public static final Integer DEFAULT_HTTP_CONNECTION_REQUEST_TIMEOUT = 300;
    public static final Integer DEFAULT_HTTP_MAX_CONNECTIONS = 20;
    private static final String WEB_SUB_ADAPTER_ERROR_CODE_PREFIX = "WEBSUB-";

    private WebSubHubAdapterConstants() {

    }

    /**
     * Error codes related to websub adapter.
     */
    public enum ErrorMessages {

        //client errors.
        WEB_SUB_BASE_URL_NOT_CONFIGURED("60001", "WebSub Hub base URL is not configured.",
                "WebSub Hub base URL is not configured."),
        ERROR_PUBLISHING_EVENT_INVALID_PAYLOAD("60002", "Invalid payload provided.",
                "Event payload cannot be processed."),
        ERROR_NULL_EVENT_PAYLOAD("60003", "Invalid event payload input ", "Event payload input cannot be null."),
        ERROR_INVALID_EVENT_URI("60004", "Invalid event URI input", "Event URI input cannot be null or empty."),
        ERROR_INVALID_EVENT_TOPIC("60005", "Invalid event topic input", "Event topic input cannot be null or empty."),
        ERROR_INVALID_EVENT_ORGANIZATION_NAME("60006", "Invalid organization name input",
                "Event organization name input cannot be null or empty."),
        ERROR_INVALID_TOPIC("60007", "Invalid WebSub hub topic input", "WebSub hub topic cannot be null " +
                "or empty."),
        ERROR_INVALID_WEB_SUB_HUB_BASE_URL("60008", "Invalid WebSub hub base URL input", "WebSub hub base URL cannot " +
                "be null or empty."),
        ERROR_INVALID_WEB_SUB_OPERATION("60009", "Invalid WebSub operation input", "WebSub operation cannot be null " +
                "or empty."),
        WEB_SUB_HUB_ADAPTER_DISABLED("60010", "WebSub Hub adapter is disabled.",
                "Error in %s. WebSub Hub adapter is disabled."),

        //server errors.
        ERROR_REGISTERING_HUB_TOPIC("65001", "Error registering WebSub Hub topic.",
                "Server error encountered while registering the WebSub Hub topic: %s, tenant: %s."),
        ERROR_DEREGISTERING_HUB_TOPIC("65002", "Error de-registering WebSub Hub topic.",
                "Server error encountered while de-registering the WebSub Hub topic: %s, tenant: %s."),
        ERROR_BACKEND_ERROR_FROM_WEBSUB_HUB("65003", "Backend error from WebSub Hub topic management.",
                "Backend error received from WebSubHub topic management, topic: %s, operation: %s."),
        ERROR_INVALID_RESPONSE_FROM_WEBSUB_HUB("65004", "Error response from WebSub Hub.",
                "Invalid response received from WebSub Hub, topic: %s, operation: %s, payload: %s."),
        ERROR_NO_RESPONSE_FROM_WEBSUB_HUB("65005", "No Response from WebSub Hub.",
                "Didn't receive response from WebSub Hub, topic: %s, operation: %s."),
        ERROR_GETTING_ASYNC_CLIENT("65006", "Error getting the async client to publish events.",
                "Error preparing async client to publish events, tenant: %s."),
        ERROR_CREATING_SSL_CONTEXT("65007", "Error while preparing SSL context for WebSubHub http client.",
                "Server error encountered while preparing SSL context for WebSubHub http client."),
        ERROR_CREATING_ASYNC_HTTP_CLIENT("65008", "Error while creating the Async HTTP client.",
                "Server error encountered while creating the Async HTTP Client of WebSub Hub Adapter.");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessages(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return WEB_SUB_ADAPTER_ERROR_CODE_PREFIX + code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }
    }
}
