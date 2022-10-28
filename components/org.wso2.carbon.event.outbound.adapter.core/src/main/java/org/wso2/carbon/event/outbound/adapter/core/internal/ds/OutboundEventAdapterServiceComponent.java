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

package org.wso2.carbon.event.outbound.adapter.core.internal.ds;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.event.outbound.adapter.core.OutboundEventAdapter;
import org.wso2.carbon.event.outbound.adapter.core.OutboundEventAdapterService;
import org.wso2.carbon.event.outbound.adapter.core.internal.services.OutboundEventAdapterServiceImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Outbound Event Adapter service component.
 */
@Component(
        name = "event.outbound.adapter.service",
        immediate = true)
public class OutboundEventAdapterServiceComponent {

    private static final Log log = LogFactory.getLog(OutboundEventAdapterServiceComponent.class);
    private static final List<OutboundEventAdapter> outboundEventAdapters = new ArrayList<>();

    /**
     * Initialize the Outbound Event Adapters core service.
     *
     * @param context The component context that will be passed from the OSGi environment at activation.
     */
    @Activate
    protected void activate(ComponentContext context) {

        OutboundEventAdapterServiceImpl outboundEventAdapterService = new OutboundEventAdapterServiceImpl();
        OutboundEventAdapterServiceValueHolder.setOutboundEventAdapterService(outboundEventAdapterService);
        registerOutputEventAdapters();
        context.getBundleContext().registerService(OutboundEventAdapterService.class.getName(),
                outboundEventAdapterService, null);
        try {
            if (log.isDebugEnabled()) {
                log.debug("Successfully deployed the outbound event adapter service");
            }
        } catch (RuntimeException e) {
            log.error("Can not create the output outbound adapter service ", e);
        }
    }

    private void registerOutputEventAdapters() {

        OutboundEventAdapterServiceImpl outboundEventAdapterService = OutboundEventAdapterServiceValueHolder
                .getOutboundEventAdapterService();
        for (OutboundEventAdapter outboundEventAdapter : outboundEventAdapters) {
            outboundEventAdapterService.registerEventAdapter(outboundEventAdapter);
        }
    }

    @Reference(
            name = "outbound.event.adapter.tracker.service",
            service = org.wso2.carbon.event.outbound.adapter.core.OutboundEventAdapter.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unSetEventAdapterType")
    protected void setEventAdapterType(OutboundEventAdapter outboundEventAdapter) {

        try {
            if (OutboundEventAdapterServiceValueHolder.getOutboundEventAdapterService() != null) {
                OutboundEventAdapterServiceValueHolder.getOutboundEventAdapterService()
                        .registerEventAdapter(outboundEventAdapter);
            } else {
                outboundEventAdapters.add(outboundEventAdapter);
            }
        } catch (Throwable t) {
            String outboundEventAdapterClassName = "Unknown";
            if (outboundEventAdapter != null) {
                outboundEventAdapterClassName = outboundEventAdapter.getClass().getName();
            }
            log.error("Unexpected error at initializing outbound event adapter" +
                    outboundEventAdapterClassName + ": " + t.getMessage(), t);
        }
    }

    protected void unSetEventAdapterType(OutboundEventAdapter outboundEventAdapter) {

        OutboundEventAdapterServiceValueHolder.getOutboundEventAdapterService().unRegisterEventAdapterFactory
                (outboundEventAdapter);
    }
}
