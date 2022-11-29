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

package org.wso2.identity.outbound.adapter.websubhub;

import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterException;
import org.wso2.identity.outbound.adapter.websubhub.model.EventPayload;

/**
 * The WebSubHub event adapter service is used to publish notification events into the intermediate hub.
 */
public interface WebSubHubAdapterService {

    /**
     * Publish a given event to the intermediate hub.
     *
     * @param payload      Event payload.
     * @param topicSuffix  Suffix of the hub topic.
     * @param eventUri     URI of the event.
     * @throws WebSubAdapterException
     */
    void publish(EventPayload payload, String topicSuffix, String eventUri) throws WebSubAdapterException;

    /**
     * Register a given topic in the intermediate hub.
     *
     * @param topicSuffix  Suffix of the hub topic.
     * @param tenantDomain Tenant domain.
     * @throws  WebSubAdapterException
     */
    void registerTopic(String topicSuffix, String tenantDomain) throws WebSubAdapterException;

    /**
     * Deregister a given topic from the intermediate hub.
     *
     * @param topicSuffix  Suffix of the hub topic.
     * @param tenantDomain Tenant domain.
     * @throws  WebSubAdapterException
     */
    void deregisterTopic(String topicSuffix, String tenantDomain) throws WebSubAdapterException;
}
