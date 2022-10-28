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

import org.wso2.carbon.event.outbound.adapter.core.exception.ConnectionUnavailableException;
import org.wso2.carbon.event.outbound.adapter.core.exception.OutboundEventAdapterException;
import org.wso2.carbon.event.outbound.adapter.core.exception.TestConnectionNotSupportedException;

import java.util.Map;

/**
 * This interface is used to publish/subscribe messages according to
 * the event type.
 */
public interface OutboundEventAdapter {

    /**
     * Retrieve type of the event adapter.
     *
     * @return Event adapter type.
     */
    String getType();

    /**
     * The init of the adapter. This will be called only once be for connect() and testConnect().
     *
     * @throws OutboundEventAdapterException If there are any configuration errors.
     */
    void init() throws OutboundEventAdapterException;

    /**
     * Use to test the connection.
     *
     * @throws TestConnectionNotSupportedException If test connection is not supported by the adapter.
     * @throws ConnectionUnavailableException      If it cannot connect to the backend.
     */
    void testConnect() throws TestConnectionNotSupportedException, ConnectionUnavailableException;

    /**
     * Call to connect to the backend before events are published.
     *
     * @throws ConnectionUnavailableException If it cannot connect to the backend.
     */
    void connect() throws ConnectionUnavailableException;

    /**
     * To publish the events.
     *
     * @param eventPayload Payload of the event to be published.
     * @param eventType    Type of the event.
     * @param propertyMap  Required properties by the adapter.
     * @throws ConnectionUnavailableException if it cannot connect to the backend.
     */
    void publish(Map<String, Object> eventPayload, String eventType, Map<String, String> propertyMap)
            throws ConnectionUnavailableException;

    /**
     * Will be called after all publishing is done, or when ConnectionUnavailableException is thrown.
     */
    void disconnect();

    /**
     * Will be called at the end to clean all the resources consumed.
     */
    void destroy();

    /**
     * Check whether events get accumulated at the adopter and clients connect to it to collect events.
     *
     * @return Is polled.
     */
    boolean isPolled();
}
