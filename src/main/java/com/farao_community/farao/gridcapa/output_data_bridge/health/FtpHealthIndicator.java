/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)marc
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.output_data_bridge.health;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;
import org.springframework.stereotype.Component;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Component
@ConditionalOnProperty(prefix = "data-bridge.sinks.ftp", name = "active", havingValue = "true")
public class FtpHealthIndicator implements HealthIndicator {

    @Value("${data-bridge.sinks.ftp.host}")
    private String ftpHost;
    @Value("${data-bridge.sinks.ftp.port}")
    private int ftpPort;
    @Value("${data-bridge.sinks.ftp.username}")
    private String ftpUsername;
    @Value("${data-bridge.sinks.ftp.password}")
    private String ftpPassword;
    @Value("${data-bridge.sinks.ftp.base-directory}")
    private String ftpBaseDirectory;

    @Override
    public Health health() {
        DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
        ftpSessionFactory.setHost(ftpHost);
        ftpSessionFactory.setPort(ftpPort);
        ftpSessionFactory.setUsername(ftpUsername);
        ftpSessionFactory.setPassword(ftpPassword);
        ftpSessionFactory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
        try (FtpSession session = ftpSessionFactory.getSession()){
            if (session.test() && session.exists(ftpBaseDirectory)) {
                return Health.up().build();
            }
        } catch (Exception e) {
            return Health.down().build();
        }
        return Health.down().build();
    }
}
