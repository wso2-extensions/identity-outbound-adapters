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

package org.wso2.identity.outbound.adapter.websubhub.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterClientException;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterException;
import org.wso2.identity.outbound.adapter.websubhub.exception.WebSubAdapterServerException;
import org.wso2.identity.outbound.adapter.websubhub.model.EventPayload;
import org.wso2.identity.outbound.adapter.websubhub.model.SecurityEventTokenPayload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimMetadataUtils.CORRELATION_ID_MDC;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.AUDIENCE_BASE_URL;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.DEREGISTER;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ERROR_TOPIC_DEREG_FAILURE_ACTIVE_SUBS;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.HUB_ACTIVE_SUBS;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.HUB_MODE;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.HUB_REASON;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.RESPONSE_FOR_SUCCESSFUL_OPERATION;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.URL_KEY_VALUE_SEPARATOR;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.URL_PARAM_SEPARATOR;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.URL_SEPARATOR;

/**
 * Unit tests for {@link WebSubHubAdapterUtil}.
 */
public class WebSubHubAdapterUtilTest {

    private static final String WEBSUB_HUB_BASE_URL = "https://test.com/websub/hub";
    private static final String TEST_OPERATION_SUBSCRIBE = "register";
    private static final String TEST_OPERATION_UNSUB = "deregister";
    private static final String TEST_EVENT = "urn:ietf:params:testEvent";
    private static final String TEST_TOPIC = "TEST-TOPIC";
    private static final String TEST_ORG_NAME = "test-org";
    private static final String TEST_PROPERTY = "test-property";
    private static final int TEST_ORG_ID = 999;
    private static final String INVALID_RESPONSE = "INVALID_RESPONSE";
    private static final String HUB_MODE_DENIED = HUB_MODE + "=" + "denied";

    private MockedStatic<HttpClientBuilder> mockStaticHttpClientBuilder;

    private enum ResponseStatus {

        STATUS_NOT_200, NULL_ENTITY, NON_SUCCESS_OPERATION, FORBIDDEN_TOPIC_DEREG_FAILURE, FORBIDDEN,
        REG_CONFLICT, DEREG_NOT_FOUND
    }

    @BeforeClass
    public void setup() {

        mockStaticHttpClientBuilder = mockStatic(HttpClientBuilder.class);
    }

    @AfterClass
    public void tearDown() {

        mockStaticHttpClientBuilder.close();
    }

    private static EventPayload getEventPayload(int orgId, String orgName, String testProperty, String ref) {

        if (testProperty == null) {
            return null;
        }

        EventPayload testEvenPayload;
        testEvenPayload = new TestEventPayload(testProperty);
        testEvenPayload.setOrganizationId(orgId);
        testEvenPayload.setOrganizationName(orgName);
        testEvenPayload.setRef(ref);
        return testEvenPayload;
    }

    @DataProvider(name = "securityEventDataProvider")
    public Object[][] provideSecurityEventData() {

        return new Object[][]{
                // orgId, orgName, eventUri, topic, testProperty
                {TEST_ORG_ID, TEST_ORG_NAME, TEST_EVENT, TEST_TOPIC, TEST_PROPERTY}
        };
    }

    @Test(dataProvider = "securityEventDataProvider")
    public void testBuildSecurityEventToken(int orgId, String orgName, String eventUri, String topic,
                                            String testProperty) throws WebSubAdapterClientException {

        String ref = "https://localhost:9443/" + orgName + "/test-event";

        EventPayload testEvenPayload = getEventPayload(orgId, orgName, testProperty, ref);

        SecurityEventTokenPayload securityEventTokenPayload =
                WebSubHubAdapterUtil.buildSecurityEventToken(testEvenPayload, eventUri, topic);

        assertNotNull(securityEventTokenPayload);
        assertEquals(securityEventTokenPayload.getIss(), WebSubHubAdapterConstants.EVENT_ISSUER);
        assertEquals(securityEventTokenPayload.getAud(), AUDIENCE_BASE_URL + orgName + URL_SEPARATOR + topic);

        Map<String, EventPayload> events = securityEventTokenPayload.getEvent();
        assertNotNull(events);

        EventPayload eventPayload = events.get(eventUri);
        assertEquals(eventPayload.getOrganizationId(), orgId);
        assertEquals(eventPayload.getOrganizationName(), orgName);
        assertEquals(eventPayload.getRef(), ref);
        assertNotNull(eventPayload);
        assertTrue(eventPayload instanceof TestEventPayload,
                "Event payload should be of type of: " + TestEventPayload.class.getName() + " but found: " +
                        eventPayload.getClass().getName());

        TestEventPayload testEventPayload = (TestEventPayload) eventPayload;
        Assert.assertEquals(testEventPayload.getTestProperty(), testProperty);
    }

    @DataProvider(name = "errorSecurityEventDataProvider")
    public Object[][] provideErrorSecurityEventData() {

        return new Object[][]{
                // orgId, orgName, eventUri, topic, testProperty
                {TEST_ORG_ID, null, TEST_EVENT, TEST_TOPIC, TEST_PROPERTY},
                {TEST_ORG_ID, TEST_ORG_NAME, null, TEST_TOPIC, TEST_PROPERTY},
                {TEST_ORG_ID, TEST_ORG_NAME, TEST_EVENT, null, TEST_PROPERTY},
                {TEST_ORG_ID, TEST_ORG_NAME, TEST_EVENT, TEST_TOPIC, null}
        };
    }

    @Test(dataProvider = "errorSecurityEventDataProvider", expectedExceptions = WebSubAdapterClientException.class)
    public void testBuildSecurityEventTokenError(int orgId, String orgName, String eventUri, String topic,
                                                 String testProperty) throws WebSubAdapterClientException {

        String ref = "https://localhost:9443/" + orgName + "/test-event";

        EventPayload testEventPayload = getEventPayload(orgId, orgName, testProperty, ref);

        WebSubHubAdapterUtil.buildSecurityEventToken(testEventPayload, eventUri, topic);

        if (orgName == null) {
            Assert.fail("Error expected for null orgName.");
        }

        if (eventUri == null) {
            Assert.fail("Error expected for null event URI.");
        }

        if (topic == null) {
            Assert.fail("Error expected for null topic.");
        }

        if (testEventPayload == null) {
            Assert.fail("Error expected for null event payload.");
        }
    }

    @DataProvider(name = "correlationIdProvider")
    public Object[][] provideCorrelationId() {

        return new Object[][]{
                {UUID.randomUUID().toString()},
                {null}
        };
    }

    @Test(dataProvider = "correlationIdProvider")
    public void testGetCorrelationID(String correlationId) {

        if (correlationId != null) {
            try {
                MDC.put(CORRELATION_ID_MDC, correlationId);
                assertEquals(WebSubHubAdapterUtil.getCorrelationID(), correlationId);
            } finally {
                MDC.remove(CORRELATION_ID_MDC);
            }
        } else {
            assertNotNull(WebSubHubAdapterUtil.getCorrelationID(), "Correlation ID should not be null");
        }
    }

    @DataProvider(name = "makeTopicMgtAPICallDataProvider")
    public Object[][] makeTopicMgtAPICallDataProvider() {

        return new Object[][]{
                // topic, websub hub base URL, operation, responseStatus, expectedException
                {TEST_TOPIC, WEBSUB_HUB_BASE_URL, TEST_OPERATION_SUBSCRIBE, null, null},
                {null, WEBSUB_HUB_BASE_URL, TEST_OPERATION_SUBSCRIBE, null, WebSubAdapterClientException.class},
                {TEST_TOPIC, null, TEST_OPERATION_SUBSCRIBE, null, WebSubAdapterClientException.class},
                {TEST_TOPIC, WEBSUB_HUB_BASE_URL, null, null, WebSubAdapterClientException.class},
                {TEST_TOPIC, WEBSUB_HUB_BASE_URL, TEST_OPERATION_SUBSCRIBE, ResponseStatus.STATUS_NOT_200,
                        WebSubAdapterServerException.class},
                {TEST_TOPIC, WEBSUB_HUB_BASE_URL, TEST_OPERATION_SUBSCRIBE, ResponseStatus.NULL_ENTITY,
                        WebSubAdapterServerException.class},
                {TEST_TOPIC, WEBSUB_HUB_BASE_URL, TEST_OPERATION_SUBSCRIBE, ResponseStatus.NON_SUCCESS_OPERATION,
                        WebSubAdapterServerException.class},
                {TEST_TOPIC, WEBSUB_HUB_BASE_URL, DEREGISTER, ResponseStatus.FORBIDDEN_TOPIC_DEREG_FAILURE,
                        WebSubAdapterClientException.class},
                {TEST_TOPIC, WEBSUB_HUB_BASE_URL, TEST_OPERATION_SUBSCRIBE, ResponseStatus.FORBIDDEN,
                        WebSubAdapterServerException.class},
                {TEST_TOPIC, WEBSUB_HUB_BASE_URL, TEST_OPERATION_SUBSCRIBE, ResponseStatus.REG_CONFLICT, null},
                {TEST_TOPIC, WEBSUB_HUB_BASE_URL, TEST_OPERATION_UNSUB, ResponseStatus.DEREG_NOT_FOUND, null}
        };
    }

    @Test(dataProvider = "makeTopicMgtAPICallDataProvider")
    public void testMakeTopicMgtAPICall(String topic, String webSubHubBaseUrl, String operation,
                                        ResponseStatus responseStatus, Class<?> expectedException) throws IOException {

        HttpClientBuilder mockHttpClientBuilder = mock(HttpClientBuilder.class);
        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        StatusLine mockStatusLine = mock(StatusLine.class);

        mockStaticHttpClientBuilder.when(HttpClientBuilder::create).thenReturn(mockHttpClientBuilder);
        when(mockHttpClientBuilder.useSystemProperties()).thenReturn(mockHttpClientBuilder);
        when(mockHttpClientBuilder.build()).thenReturn(mockHttpClient);
        when(mockHttpClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);

        if (responseStatus == ResponseStatus.STATUS_NOT_200) {
            when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        } else if (responseStatus == ResponseStatus.REG_CONFLICT) {
            when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_CONFLICT);
        } else if (responseStatus == ResponseStatus.DEREG_NOT_FOUND) {
            when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
        } else {
            when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        }

        if (responseStatus == ResponseStatus.NULL_ENTITY) {
            when(mockResponse.getEntity()).thenReturn(null);
        } else {
            when(mockResponse.getEntity()).thenReturn(mockEntity);
        }

        if (responseStatus == ResponseStatus.NON_SUCCESS_OPERATION) {
            when(mockEntity.getContent()).thenReturn(
                    new ByteArrayInputStream(INVALID_RESPONSE.getBytes(StandardCharsets.UTF_8)));
        } else {
            when(mockEntity.getContent()).thenReturn(
                    new ByteArrayInputStream(RESPONSE_FOR_SUCCESSFUL_OPERATION.getBytes(StandardCharsets.UTF_8)));
        }

        if (responseStatus == ResponseStatus.FORBIDDEN_TOPIC_DEREG_FAILURE) {
            when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
            String responseContent = HUB_MODE_DENIED + URL_PARAM_SEPARATOR + HUB_REASON + URL_KEY_VALUE_SEPARATOR +
                    String.format(ERROR_TOPIC_DEREG_FAILURE_ACTIVE_SUBS, TEST_TOPIC) + URL_PARAM_SEPARATOR +
                    HUB_ACTIVE_SUBS + URL_KEY_VALUE_SEPARATOR + "subscriber_1,subscriber_2";
            when(mockEntity.getContent()).thenReturn(
                    new ByteArrayInputStream(responseContent.getBytes(StandardCharsets.UTF_8)));
        }

        if (responseStatus == ResponseStatus.FORBIDDEN) {
            when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
            doReturn(new ByteArrayInputStream(HUB_MODE_DENIED.getBytes(StandardCharsets.UTF_8))).when(mockEntity)
                    .getContent();
        }

        try {
            WebSubHubAdapterUtil.makeTopicMgtAPICall(topic, webSubHubBaseUrl, operation);

            if (expectedException == null) {

                verify(mockHttpClientBuilder).useSystemProperties();
                verify(mockHttpClientBuilder).build();
                verify(mockResponse).getStatusLine();
                verify(mockResponse).getEntity();
                verify(mockEntity).getContent();
            } else {
                Assert.fail("Expected an exception of type: " + expectedException.getName());
            }
        } catch (WebSubAdapterException e) {
            if (expectedException == null) {
                Assert.fail("Received exception: " + e.getClass().getName() + " for a successful test case.");
            }
            Assert.assertSame(e.getClass(), expectedException);
        }
    }

    /**
     * Test event payload implementation extended from {@link EventPayload}.
     */
    private static class TestEventPayload extends EventPayload {

        private final String testProperty;

        TestEventPayload(String testProperty) {

            this.testProperty = testProperty;
        }

        public String getTestProperty() {

            return testProperty;
        }
    }
}
