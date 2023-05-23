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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.ssl.SSLContexts;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import static java.util.Objects.isNull;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_CREATING_ASYNC_HTTP_CLIENT;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_CREATING_SSL_CONTEXT;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ErrorMessages.ERROR_GETTING_ASYNC_CLIENT;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterUtil.handleServerException;

/**
 * Class to retrieve the HTTP Clients.
 */
public class ClientManager {

    private static final Log LOG = LogFactory.getLog(ClientManager.class);
    private final CloseableHttpAsyncClient httpAsyncClient;

    /**
     * Creates a client manager.
     *
     * @throws WebSubAdapterException on errors while creating the http client.
     */
    public ClientManager() throws WebSubAdapterException {

        PoolingAsyncClientConnectionManager connectionManager;
        try {
            connectionManager = createPoolingConnectionManager();
        } catch (IOException e) {
            throw handleServerException(ERROR_CREATING_ASYNC_HTTP_CLIENT, e);
        }

        httpAsyncClient = HttpAsyncClients.custom()
                .setConnectionManager(connectionManager)
                .setIOReactorConfig(IOReactorConfig.custom().build())
                .setDefaultRequestConfig(RequestConfig.custom().build())
                .build();

        httpAsyncClient.start();
    }

    /**
     * Get HTTP client properly configured with tenant configurations.
     *
     * @return CloseableHttpAsyncClient instance.
     */
    public CloseableHttpAsyncClient getClient() throws WebSubAdapterException {

        if (isNull(httpAsyncClient)) {
            throw handleServerException(ERROR_GETTING_ASYNC_CLIENT, null);
        } else if (!httpAsyncClient.getStatus().equals(IOReactorStatus.ACTIVE)) {
            httpAsyncClient.start();
        }
        return httpAsyncClient;
    }

    private PoolingAsyncClientConnectionManager createPoolingConnectionManager()
            throws IOException, WebSubAdapterException {

        int maxConnections = WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration()
                .getDefaultMaxConnections();
        int maxConnectionsPerRoute = WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration()
                .getDefaultMaxConnectionsPerRoute();

        return PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(createTlsStrategy())
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                .setConnPoolPolicy(PoolReusePolicy.LIFO)
                .setMaxConnTotal(maxConnections)
                .setMaxConnPerRoute(maxConnectionsPerRoute)
                .setDefaultConnectionConfig(createConnectionConfig())
                .setDefaultTlsConfig(createTlsConfig())
                .build();
    }

    private TlsStrategy createTlsStrategy() throws WebSubAdapterException {

        try {
            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(WebSubHubAdapterDataHolder.getInstance().getKeyStore(),
                            WebSubHubAdapterDataHolder.getInstance().getKeyStorePassword().toCharArray())
                    .loadTrustMaterial(WebSubHubAdapterDataHolder.getInstance().getTrustStore(), null)
                    .build();

            return ClientTlsStrategyBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(new DefaultHostnameVerifier())
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | UnrecoverableKeyException e) {
            throw handleServerException(ERROR_CREATING_SSL_CONTEXT, e);
        }
    }

    private ConnectionConfig createConnectionConfig() {

        return ConnectionConfig.custom()
                .setSocketTimeout(WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration()
                        .getHttpReadTimeout(), TimeUnit.MILLISECONDS)
                .setConnectTimeout(WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration()
                        .getHTTPConnectionTimeout(), TimeUnit.MILLISECONDS)
                .build();
    }

    private TlsConfig createTlsConfig() {

        return TlsConfig.custom()
                .setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
                .setHandshakeTimeout(WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration()
                        .getHandshakeTimeout(), TimeUnit.MILLISECONDS)
                .build();
    }
}
