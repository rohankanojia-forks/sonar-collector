/*
 * Copyright 2017-2022 Steinar Bang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package no.priv.bang.sonar.collector.webhook;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.osgi.service.log.LogService;

import no.priv.bang.osgi.service.mocks.logservice.MockLogService;

class SonarCollectorConfigurationTest {

    /**
     * Test the default value for the JDBC connection: PostgreSQL on localhost, no username or password.
     * (localhost PostgreSQL uses the username of the connecting process to grant access).
     */
    @Test
    void testGetMetricKeys() {
        MockLogService logservice = new MockLogService();
        SonarCollectorConfiguration configuration = new SonarCollectorConfiguration();
        configuration.loadProperties(logservice);

        assertEquals(10, configuration.getMetricKeys().length);
    }

    /**
     * Corner case test: test robustness for setting a null configuration object
     *
     * The behaviour should be the same as the default values and there should
     * be no exceptions thrown.
     */
    @Test
    void testGetMetricKeysNullConfig() {
        MockLogService logservice = new MockLogService();
        SonarCollectorConfiguration configuration = new SonarCollectorConfiguration();
        configuration.loadProperties(logservice);

        configuration.setConfig(null);

        assertEquals(10, configuration.getMetricKeys().length);
    }

    /**
     * Test getting all values from injected config.
     *
     * The behaviour should be the same as the default values and there should
     * be no exceptions thrown.
     */
    @Test
    void testGetMetricKeysInjectedConfig() {
        Properties originalProperties = (Properties) System.getProperties().clone();
        try {
            MockLogService logservice = new MockLogService();
            SonarCollectorConfiguration configuration = new SonarCollectorConfiguration();
            configuration.loadProperties(logservice);
            Map<String, Object> injectedConfig = new HashMap<>();
            injectedConfig.put(SonarCollectorConfiguration.SONAR_MEASURES_COMPONENTS_METRIC_KEYS, "lines,bugs,new_bugs,vulnerabilities,new_vulnerabilities,code_smells");

            // Set a system property that should not be picked up, since the injected config is picked first
            System.setProperty(SonarCollectorConfiguration.SONAR_MEASURES_COMPONENTS_METRIC_KEYS, "notametric");

            configuration.setConfig(injectedConfig);

            assertEquals(6, configuration.getMetricKeys().length);
        } finally {
            // Restore the original system properties
            System.setProperties(originalProperties);
        }
    }

    /**
     * Test getting all values from system properties.
     *
     * The behaviour should be the same as the default values and there should
     * be no exceptions thrown.
     */
    @Test
    void testGetMetricKeysFromSystemProperties() {
        Properties originalProperties = (Properties) System.getProperties().clone();
        try {
            MockLogService logservice = new MockLogService();
            SonarCollectorConfiguration configuration = new SonarCollectorConfiguration();
            configuration.loadProperties(logservice);
            Map<String, Object> injectedConfig = new HashMap<>();
            configuration.setConfig(injectedConfig);

            System.setProperty(SonarCollectorConfiguration.SONAR_MEASURES_COMPONENTS_METRIC_KEYS, "lines,bugs,new_bugs,vulnerabilities,new_vulnerabilities,code_smells,new_code_smells,coverage");

            assertEquals(8, configuration.getMetricKeys().length);
        } finally {
            // Restore the original system properties
            System.setProperties(originalProperties);
        }
    }

    @Test
    void testSonarApiUserTokenConfigNotSet() {
        SonarCollectorConfiguration configuration = new SonarCollectorConfiguration();
        assertFalse(configuration.hasSonarApiUserToken());
    }

    @Test
    void testHasSonarApiUserToken() {
        SonarCollectorConfiguration configuration = new SonarCollectorConfiguration();
        String usertoken = "squ_3869fbac07cc388306804e35fb72ca7c4baff275";
        var config = new HashMap<String, Object>();
        config.put(SonarCollectorConfiguration.SONAR_USER_TOKEN, usertoken);
        configuration.setConfig(config);
        assertTrue(configuration.hasSonarApiUserToken());
        assertEquals(usertoken, configuration.getSonarApiUserToken());
    }

    static class SonarCollectorConfigurationWithApplicationPropertiesThrowingIOException extends SonarCollectorConfiguration {

        SonarCollectorConfigurationWithApplicationPropertiesThrowingIOException(LogService logservice) {
            super();
        }

        @Override
        protected InputStream getApplicationProperties() {
            InputStream inputstream = mock(InputStream.class);
            try {
                when(inputstream.read(any(byte[].class))).thenThrow(IOException.class);
            } catch (IOException e) {
                /* Won't actually throw an IOException here */
            }
            return inputstream;
        }

    }

    /**
     * Corner case test: test what happens when the application.properties resource stream throws
     * an IOException.
     *
     * @throws IOException
     */
    @Test
    void testGetApplicationPropertiesThrowsIOException() throws IOException {
        MockLogService logservice = new MockLogService();

        // Verify that there are no log messages before the configuration property class is created
        assertEquals(0, logservice.getLogmessages().size());

        SonarCollectorConfiguration configuration = new SonarCollectorConfigurationWithApplicationPropertiesThrowingIOException(logservice);
        configuration.loadProperties(logservice);

        // Verify that a single log message had been logged
        assertEquals(1, logservice.getLogmessages().size());

        // All properties will be null
        String[] metrickeys = configuration.getMetricKeys();
        assertEquals(0, metrickeys.length);
    }

}
