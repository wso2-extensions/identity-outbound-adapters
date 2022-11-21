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

package org.wso2.carbon.event.outbound.adapter.websubhub.util;

import org.slf4j.MDC;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.event.outbound.adapter.websubhub.model.EventPayload;
import org.wso2.carbon.event.outbound.adapter.websubhub.model.SecurityEventTokenPayload;

import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.AUDIENCE_BASE_URL;
import static org.wso2.carbon.event.outbound.adapter.websubhub.util.WebSubHubEventAdapterConstants.URL_SEPARATOR;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimMetadataUtils.CORRELATION_ID_MDC;

/**
 * Unit tests for {@link WebSubHubEventAdapterUtil}.
 */
public class WebSubHubEventAdapterUtilTest {

    @Test
    public void testBuildSecurityEventToken() {

        int orgId = 999;
        String orgName = "test-org";
        String eventUri = "urn:ietf:params:testEvent";
        String topic = "TEST-TOPIC";
        String testProperty = "test-property";
        String ref = "https://localhot:9443/" + orgName + "/test-event";

        EventPayload testEvenPayload = new TestEventPayload(testProperty);
        testEvenPayload.setOrganizationId(orgId);
        testEvenPayload.setOrganizationName(orgName);
        testEvenPayload.setRef(ref);

        SecurityEventTokenPayload securityEventTokenPayload =
                WebSubHubEventAdapterUtil.buildSecurityEventToken(testEvenPayload, eventUri, topic);

        assertNotNull(securityEventTokenPayload);
        assertEquals(securityEventTokenPayload.getIss(), WebSubHubEventAdapterConstants.EVENT_ISSUER);
        assertEquals(securityEventTokenPayload.getAud(),
                AUDIENCE_BASE_URL + orgName + URL_SEPARATOR + topic);

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
                assertEquals(WebSubHubEventAdapterUtil.getCorrelationID(), correlationId);
            } finally {
                MDC.remove(CORRELATION_ID_MDC);
            }
        } else {
            assertNotNull(WebSubHubEventAdapterUtil.getCorrelationID(), "Correlation ID should not be null");
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

