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

package org.wso2.carbon.event.outbound.adapter.websubhub.util;

/**
 * Keep constants required by the WebSubHub Event Adapter.
 */
public class WebSubHubEventAdapterConstants {
    private WebSubHubEventAdapterConstants() {

    }
    public static final String ADAPTER_MESSAGE_TENANT_DOMAIN = "tenant-domain";
    public static final String EVENT_ISSUER = "Asgardeo";
    public static final String AUDIENCE_BASE_URL = "https://websubhub/topics/";
    public static final String URL_SEPARATOR = "/";
    public static final String TOPIC_SEPARATOR = "-";
    public static final String PUBLISH = "publish";
    public static final String HUB_MODE = "hub.mode";
    public static final String HUB_TOPIC = "hub.topic";
    public static final String CORRELATION_ID_REQUEST_HEADER = "activityid";
    public static final String CONNECTION_POOL_MAX_CONNECTIONS = "AdaptiveAuth.MaxTotalConnections";
    public static final String CONNECTION_POOL_MAX_CONNECTIONS_PER_ROUTE = "AdaptiveAuth.MaxTotalConnectionsPerRoute";
    public static final String ADAPTER_HUB_URL_NAME = "webSubHubBaseUrl";
    public static final String ADAPTER_HUB_URL = "http://localhost:9090/hub";

}
