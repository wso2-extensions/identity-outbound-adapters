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

package org.wso2.carbon.event.outbound.adapter.core.internal.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.event.outbound.adapter.core.OutboundEventAdapter;
import org.wso2.carbon.event.outbound.adapter.core.OutboundEventAdapterService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OutboundEventAdapterService implementation.
 */
public class OutboundEventAdapterServiceImpl implements OutboundEventAdapterService {

    private static final Log log = LogFactory.getLog(OutboundEventAdapterServiceImpl.class);
    private final Map<String, OutboundEventAdapter> eventAdapterMap;

    public OutboundEventAdapterServiceImpl() {

        this.eventAdapterMap = new ConcurrentHashMap<>();
    }

    public void registerEventAdapter(OutboundEventAdapter outboundEventAdapter) {

        this.eventAdapterMap.put(outboundEventAdapter.getType(), outboundEventAdapter);
    }

    public void unRegisterEventAdapterFactory(OutboundEventAdapter outboundEventAdapter) {

        this.eventAdapterMap.remove(outboundEventAdapter.getType());
    }

    /**
     * Publish event to the corresponding adapter based on the given adapter type.
     *
     * @param eventPayload Payload of the event.
     * @param eventType    Type of the event.
     * @param adapterType  Adapter to be used to publish the event.
     * @param propertyMap  Required event properties by the adapter.
     */
    @Override
    public void publish(Map<String, Object> eventPayload, String eventType, String adapterType,
                        Map<String, String> propertyMap) {

        OutboundEventAdapter outboundEventAdapter = getOutboundEventAdapter(adapterType);

        if (outboundEventAdapter == null) {
            //TODO Throw error saying adapter does not exists for the given type
        } else {
            outboundEventAdapter.publish(eventPayload, eventType, propertyMap);
        }
    }

    /**
     * Retrieve all the adapter types.
     *
     * @return List of adapter types.
     */
    @Override
    public List<String> getOutputEventAdapterTypes() {

        return new ArrayList<>(eventAdapterMap.keySet());
    }

    /**
     * Retrieve the outbound adapter for a given type.
     *
     * @param eventAdapterType Event adapter type.
     * @return Outbound event adapter.
     */
    private OutboundEventAdapter getOutboundEventAdapter(String eventAdapterType) {

        return eventAdapterMap.get(eventAdapterType);
    }
}
