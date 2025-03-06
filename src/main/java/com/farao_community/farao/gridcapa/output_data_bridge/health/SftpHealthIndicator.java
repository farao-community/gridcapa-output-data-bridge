/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)marc
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.output_data_bridge.health;

import com.farao_community.farao.gridcapa.output_data_bridge.configuration.OutputDataBridgeConfiguration;
import com.farao_community.farao.gridcapa.output_data_bridge.configuration.OutputDataBridgeSinkFtpSftpConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.stereotype.Component;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Component
@ConditionalOnProperty(prefix = "data-bridge.sinks.sftp", name = "active", havingValue = "true")
public class SftpHealthIndicator implements HealthIndicator {

    private final OutputDataBridgeSinkFtpSftpConfiguration sftpConfiguration;

    public SftpHealthIndicator(final OutputDataBridgeConfiguration configuration) {
        this.sftpConfiguration = configuration.sinks().sftp();
    }

    @Override
    public Health health() {
        DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
        sftpSessionFactory.setHost(sftpConfiguration.host());
        sftpSessionFactory.setPort(sftpConfiguration.port());
        sftpSessionFactory.setUser(sftpConfiguration.username());
        sftpSessionFactory.setPassword(sftpConfiguration.password());
        sftpSessionFactory.setAllowUnknownKeys(true);
        try (SftpSession session = sftpSessionFactory.getSession()) {
            if (session.test() && session.exists(sftpConfiguration.baseDirectory())) {
                return Health.up().build();
            }
        } catch (Exception e) {
            return Health.down().build();
        }
        return Health.down().build();
    }
}
