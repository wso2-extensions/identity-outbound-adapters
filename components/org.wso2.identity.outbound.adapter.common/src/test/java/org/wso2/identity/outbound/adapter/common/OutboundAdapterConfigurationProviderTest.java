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

package org.wso2.identity.outbound.adapter.common;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.identity.outbound.adapter.common.exception.AdapterConfigurationException;

import java.nio.file.Paths;
import java.util.Optional;

/**
 * Unit tests for {@link OutboundAdapterConfigurationProvider}.
 */
public class OutboundAdapterConfigurationProviderTest {

    @BeforeMethod
    public void setup() {

        String carbonConfPath = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty("carbon.config.dir.path", carbonConfPath);

    }

    @DataProvider(name = "adapterPropertyProvider")
    public Object[][] provideSecurityEventData() {

        return new Object[][]{
                // propertyKey, propertyValue, isPresent
                {"adapter.websub.enabled", "true", true},
                {"adapter.invalid.key", "true", false}
        };
    }

    @Test(dataProvider = "adapterPropertyProvider")
    public void testGetProperty(String propertyKey, String propertyValue, boolean isPresent) {

        try {
            Optional<String> webSubEnabled =
                    OutboundAdapterConfigurationProvider.getInstance().getProperty(propertyKey);

            if (isPresent) {
                Assert.assertTrue(webSubEnabled.isPresent(), propertyKey + " property should be available.");
                Assert.assertEquals(webSubEnabled.get(), propertyValue);
            } else {
                Assert.assertFalse(webSubEnabled.isPresent(), propertyKey + " property should not be available.");
            }
        } catch (AdapterConfigurationException e) {
            Assert.fail("Error occurred while obtaining adapter configs.", e);
        }
    }
}
