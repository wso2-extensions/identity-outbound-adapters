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

package org.wso2.identity.outbound.adapter.websubhub.internal;

import com.nimbusds.jose.util.DefaultResourceRetriever;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticationService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.identity.outbound.adapter.common.OutboundAdapterConfigurationProvider;
import org.wso2.identity.outbound.adapter.websubhub.WebSubHubAdapterService;
import org.wso2.identity.outbound.adapter.websubhub.config.WebSubAdapterConfiguration;
import org.wso2.identity.outbound.adapter.websubhub.service.WebSubHubAdapterServiceImpl;

/**
 * WebSubHub Outbound Event Adapter service component.
 */
@Component(
        name = "websubhub.outbound.adapter.service.component",
        immediate = true)
public class WebSubHubAdapterServiceComponent {

    private static final Log log = LogFactory.getLog(WebSubHubAdapterServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            WebSubHubAdapterServiceImpl webSubHubEventAdapter = new WebSubHubAdapterServiceImpl();
            context.getBundleContext().registerService(WebSubHubAdapterService.class.getName(),
                    webSubHubEventAdapter, null);
            WebSubHubAdapterDataHolder.getInstance().setAdapterConfiguration(new WebSubAdapterConfiguration(
                    OutboundAdapterConfigurationProvider.getInstance()));

            KeyStoreManager keyStoreMan = KeyStoreManager.getInstance(
                    MultitenantConstants.SUPER_TENANT_ID);
            WebSubHubAdapterDataHolder.getInstance().setKeyStore(keyStoreMan.getPrimaryKeyStore());
            WebSubHubAdapterDataHolder.getInstance().setKeyStorePassword(keyStoreMan.getPrimaryPrivateKeyPasssword());

            WebSubHubAdapterDataHolder.getInstance().setClientManager(new ClientManager());
            WebSubHubAdapterDataHolder.getInstance().setResourceRetriever(new DefaultResourceRetriever());

            if (log.isDebugEnabled()) {
                log.debug("Successfully activated the WebSub Hub adapter service.");
            }
        } catch (Throwable e) {
            log.error("Can not activate the WebSub Hub adapter service: " + e.getMessage(), e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("Successfully de-activated the WebSub Hub adapter service.");
        }
    }

    @Reference(
            name = "registry.service",
            service = RegistryService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRegistryService"
    )
    protected void setRegistryService(RegistryService registryService) {
        /* Reference RegistryService to guarantee that this component will wait until the registry service
        is started */
    }

    protected void unsetRegistryService(RegistryService registryService) {
        /* Reference RegistryService to guarantee that this component will wait until the registry service
        is started */
    }

    @Reference(
            name = "identity.application.authentication.framework",
            service = ApplicationAuthenticationService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationAuthenticationService"
    )
    protected void setApplicationAuthenticationService(
            ApplicationAuthenticationService applicationAuthenticationService) {
        /* reference ApplicationAuthenticationService service to guarantee that this component will wait until
        authentication framework is started */
    }

    protected void unsetApplicationAuthenticationService(
            ApplicationAuthenticationService applicationAuthenticationService) {
        /* reference ApplicationAuthenticationService service to guarantee that this component will wait until
        authentication framework is started */
    }
}
