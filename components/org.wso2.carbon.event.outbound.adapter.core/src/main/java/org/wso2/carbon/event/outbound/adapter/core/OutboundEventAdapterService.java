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

package org.wso2.carbon.event.outbound.adapter.core;

import java.util.List;
import java.util.Map;

/**
 * OSGI interface for the OutboundEventAdapterService.
 */
public interface OutboundEventAdapterService {

    /**
     * Retrieve all the adapter types.
     *
     * @return List of adapter types.
     */
    List<String> getOutputEventAdapterTypes();

    /**
     * Publish event to the corresponding adapter based on the given event adapter type.
     *
     * @param eventPayload Payload of the event.
     * @param eventType    Type of the event.
     * @param adapterType  Adapter to be used to publish the event.
     * @param propertyMap  Required event properties by the adapter.
     */
    void publish (Map<String, Object> eventPayload, String eventType, String adapterType,
                  Map<String, String> propertyMap);
}
