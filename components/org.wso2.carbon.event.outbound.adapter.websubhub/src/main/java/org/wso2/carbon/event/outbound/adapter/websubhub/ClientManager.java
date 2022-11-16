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

package org.wso2.carbon.event.outbound.adapter.websubhub;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.wso2.carbon.event.outbound.adapter.websubhub.internal.WebSubHubEventAdapterDataHolder;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;

import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.CONNECTION_POOL_MAX_CONNECTIONS;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.CONNECTION_POOL_MAX_CONNECTIONS_PER_ROUTE;


/**
 * Class to retrieve the HTTP Clients.
 */
public class ClientManager {

    private static final Log LOG = LogFactory.getLog(ClientManager.class);
    private static final Map<Integer, CloseableHttpAsyncClient> clientMap = new HashMap<>();
    private static final int HTTP_CONNECTION_TIMEOUT = 300;
    private static final int HTTP_READ_TIMEOUT = 300;
    private static final int HTTP_CONNECTION_REQUEST_TIMEOUT = 300;
    private static final int DEFAULT_MAX_CONNECTIONS = 20;
    private final PoolingNHttpClientConnectionManager poolingHttpClientConnectionManager;

    /**
     * Creates a client manager.
     *
     * @throws FrameworkException
     */
    public ClientManager() throws FrameworkException {

        poolingHttpClientConnectionManager = createPoolingConnectionManager();
    }

    /**
     * Get HTTP client properly configured with tenant configurations.
     *
     * @param tenantDomain Tenant domain of the service provider.
     * @return HttpClient.
     */
    public CloseableHttpAsyncClient getClient(String tenantDomain) throws FrameworkException, IOException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        CloseableHttpAsyncClient client = clientMap.get(tenantId);
        if (client == null) {
            RequestConfig config = createRequestConfig();
            HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClients.custom().setDefaultRequestConfig(config);
            addSslContext(httpClientBuilder, tenantDomain);
            httpClientBuilder.setConnectionManager(poolingHttpClientConnectionManager);
            client = httpClientBuilder.build();
            client.start();
            clientMap.put(tenantId, client);
        }
        return client;
    }

    private RequestConfig createRequestConfig() {

        return RequestConfig.custom()
                .setConnectTimeout(HTTP_CONNECTION_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_CONNECTION_REQUEST_TIMEOUT)
                .setSocketTimeout(HTTP_READ_TIMEOUT)
                .setRedirectsEnabled(false)
                .setRelativeRedirectsAllowed(false)
                .build();
    }

    private PoolingNHttpClientConnectionManager createPoolingConnectionManager() throws FrameworkException {

        String maxConnectionsString = IdentityUtil.getProperty(CONNECTION_POOL_MAX_CONNECTIONS);
        String maxConnectionsPerRouteString = IdentityUtil.getProperty(CONNECTION_POOL_MAX_CONNECTIONS_PER_ROUTE);
        int maxConnections = DEFAULT_MAX_CONNECTIONS;
        int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS;

        if (StringUtils.isNotEmpty(maxConnectionsString)) {
            try {
                maxConnections = Integer.parseInt(maxConnectionsString);
            } catch (NumberFormatException e) {
                // Default value is used.
                LOG.error("Error while converting MaxConnection " + maxConnections + " to integer. So proceed with " +
                        "default value ", e);
            }
        }

        if (StringUtils.isNotEmpty(maxConnectionsPerRouteString)) {
            try {
                maxConnectionsPerRoute = Integer.parseInt(maxConnectionsPerRouteString);
            } catch (NumberFormatException e) {
                // Default value is used.
                LOG.error("Error while converting MaxConnectionsPerRoute " + maxConnectionsPerRoute + " to integer. " +
                        "So proceed with default value ", e);
            }
        }

        ConnectingIOReactor ioReactor;
        try {
            ioReactor = new DefaultConnectingIOReactor();
        } catch (IOReactorException e) {
            throw new FrameworkException("Error while creating ConnectingIOReactor", e);
        }
        PoolingNHttpClientConnectionManager poolingHttpClientConnectionManager = new
                PoolingNHttpClientConnectionManager(ioReactor);
        // Increase max total connection to 20.
        poolingHttpClientConnectionManager.setMaxTotal(maxConnections);
        // Increase default max connection per route to 20.
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        return poolingHttpClientConnectionManager;
    }

    /**
     * Close the client.
     *
     * @param tenantId Tenant ID.
     * @throws IOException
     */
    public void closeClient(int tenantId) throws IOException {

        CloseableHttpAsyncClient client = clientMap.get(tenantId);
        if (client != null) {
            clientMap.remove(tenantId);
            client.close();
        }
    }

    private void addSslContext(HttpAsyncClientBuilder builder, String tenantDomain) throws IOException {

        try {
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(WebSubHubEventAdapterDataHolder.getInstance().getTrustStore())
                    .build();
            X509HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER;
            builder.setSSLContext(sslContext);
            builder.setHostnameVerifier(hostnameVerifier);

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            LOG.error("Error while creating ssl context for WebSubHub endpoint invocation in tenant domain: " +
                    tenantDomain, e);
            throw new IOException("Error while creating ssl context for WebSubHub endpoint invocation in tenant " +
                    "domain: " + tenantDomain, e);
        }
    }
}
