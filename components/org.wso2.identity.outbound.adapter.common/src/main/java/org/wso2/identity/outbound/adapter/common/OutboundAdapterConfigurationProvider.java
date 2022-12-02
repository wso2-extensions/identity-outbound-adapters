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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.identity.outbound.adapter.common.exception.AdapterConfigurationException;
import org.wso2.identity.outbound.adapter.common.exception.AdapterRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Properties;

import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.wso2.identity.outbound.adapter.common.util.OutboundAdapterConstants.CONFIG_FILE_NAME;

/**
 * Class to build the output adapter configurations.
 */
public class OutboundAdapterConfigurationProvider {

    private static final OutboundAdapterConfigurationProvider instance;

    static {
        try {
            instance = new OutboundAdapterConfigurationProvider();
        } catch (AdapterConfigurationException e) {
            throw new AdapterRuntimeException("Error initializing outbound adapter configuration provider.", e);
        }
    }

    private final Properties adapterProperties;

    private OutboundAdapterConfigurationProvider() throws AdapterConfigurationException {

        adapterProperties = this.loadProperties();
    }

    /**
     * Returns the singleton instance of the {@link OutboundAdapterConfigurationProvider}.
     *
     * @return instance of {@link OutboundAdapterConfigurationProvider}.
     * @throws AdapterConfigurationException when instance is null.
     */
    public static OutboundAdapterConfigurationProvider getInstance() throws AdapterConfigurationException {

        //Adding this as a secondary check, since object should be initialized at the class loading.
        return ofNullable(instance).orElseThrow(
                () -> new AdapterConfigurationException("Outbound adapter configuration instance is not initialized."));
    }

    @SuppressWarnings("PATH_TRAVERSAL_IN")
    private Properties loadProperties() throws AdapterConfigurationException {

        Properties properties = new Properties();

        Path path = Paths.get(IdentityUtil.getIdentityConfigDirPath(), CONFIG_FILE_NAME);

        if (Files.notExists(path)) {
            throw new AdapterConfigurationException(CONFIG_FILE_NAME + " configuration file doesn't exist.");
        }

        FileChannel channel;
        try {
            channel = FileChannel.open(path, StandardOpenOption.READ);

            try (InputStream inputStream = Channels.newInputStream(channel)) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new AdapterConfigurationException("Error while retrieving the configuration file.", e);
        }

        return properties;
    }

    /**
     * Returns the {@link Optional} value of the property provided.
     *
     * @param propertyName name of the config property.
     * @return Optional value of the property provided.
     */
    public Optional<String> getProperty(String propertyName) {

        return isNull(propertyName) ? empty() :
                ofNullable(this.adapterProperties.getProperty(propertyName)).filter(StringUtils::isNotBlank);
    }
}
