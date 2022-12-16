package org.wso2.identity.outbound.adapter.websubhub.config;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.identity.outbound.adapter.common.OutboundAdapterConfigurationProvider;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterException;

import java.nio.file.Paths;
import java.util.Optional;

import static java.util.Objects.isNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;
import static org.wso2.identity.outbound.adapter.websubhub.config.WebSubAdapterConfiguration.ADAPTER_ENABLED_CONFIG;
import static org.wso2.identity.outbound.adapter.websubhub.config.WebSubAdapterConfiguration.ADAPTER_HUB_URL_CONFIG;
import static org.wso2.identity.outbound.adapter.websubhub.config.WebSubAdapterConfiguration.ADPATER_HTTP_CLIENT_MAX_CONNECTIONS;
import static org.wso2.identity.outbound.adapter.websubhub.config.WebSubAdapterConfiguration.ADPATER_HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE;

public class WebSubAdapterConfigurationTest {

    @BeforeMethod
    public void setup() {

        String carbonConfPath = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty("carbon.config.dir.path", carbonConfPath);
    }

    @DataProvider(name = "websubAdapterPropertyProvider")
    public String[][] provideSecurityEventData() {

        return new String[][]{
                // enabled, baseUrl, max_conn, max_conn_per_route
                {null, null, null, null},
                {"true", "http://localhost:9090/hub", "50", null},
                {"true", null, "50", null},
                {"false", "http://localhost:9090/hub", null, "25"},
                {"false", null, "20", "25"}
        };
    }

    @Test(dataProvider = "websubAdapterPropertyProvider")
    public void testWebSubAdapterConfiguration(String... param) throws WebSubAdapterException {

        OutboundAdapterConfigurationProvider provider = Mockito.mock(OutboundAdapterConfigurationProvider.class);
        Mockito.when(provider.getProperty(ADAPTER_ENABLED_CONFIG)).thenReturn(Optional.ofNullable(param[0]));
        Mockito.when(provider.getProperty(ADAPTER_HUB_URL_CONFIG)).thenReturn(Optional.ofNullable(param[1]));
        Mockito.when(provider.getProperty(ADPATER_HTTP_CLIENT_MAX_CONNECTIONS))
                .thenReturn(Optional.ofNullable(param[2]));
        Mockito.when(provider.getProperty(ADPATER_HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE))
                .thenReturn(Optional.ofNullable(param[3]));

        if (Boolean.parseBoolean(param[0]) && isNull(param[1])) {
            assertThrows(WebSubAdapterException.class, () -> new WebSubAdapterConfiguration(provider));
            return;
        }

        WebSubAdapterConfiguration configuration = new WebSubAdapterConfiguration(provider);

        assertSame(configuration.isAdapterEnabled(), Boolean.parseBoolean(param[0]));
        assertEquals(configuration.getWebSubHubBaseUrl(), Boolean.parseBoolean(param[0]) ? param[1] : null);
        assertSame(configuration.getHttpClientMaxConnections(),
                Optional.ofNullable(param[2]).map(Integer::parseInt).orElse(20));
        assertSame(configuration.getHttpClientMaxConnectionsPerRoute(),
                Optional.ofNullable(param[3]).map(Integer::parseInt).orElse(20));
    }
}
