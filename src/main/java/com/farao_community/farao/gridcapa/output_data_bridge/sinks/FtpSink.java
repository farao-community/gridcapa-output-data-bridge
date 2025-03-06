/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.output_data_bridge.sinks;

import com.farao_community.farao.gridcapa.output_data_bridge.configuration.OutputDataBridgeConfiguration;
import com.farao_community.farao.gridcapa.output_data_bridge.configuration.OutputDataBridgeSinkFtpSftpConfiguration;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.ftp.outbound.FtpMessageHandler;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.messaging.MessageHandler;

import static com.farao_community.farao.gridcapa.output_data_bridge.MinioSource.FILE_NAME_HEADER;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
@ConditionalOnProperty(prefix = "data-bridge.sinks.ftp", name = "active", havingValue = "true")
public class FtpSink {
    public static final String TO_FTP_CHANNEL = "toFtpChannel";
    private final OutputDataBridgeSinkFtpSftpConfiguration ftpConfiguration;

    public FtpSink(final OutputDataBridgeConfiguration configuration) {
        this.ftpConfiguration = configuration.sinks().ftp();
    }

    private DefaultFtpSessionFactory ftpSessionFactory() {
        DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
        ftpSessionFactory.setHost(ftpConfiguration.host());
        ftpSessionFactory.setPort(ftpConfiguration.port());
        ftpSessionFactory.setUsername(ftpConfiguration.username());
        ftpSessionFactory.setPassword(ftpConfiguration.password());
        ftpSessionFactory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
        return ftpSessionFactory;
    }

    @Bean
    @ServiceActivator(inputChannel = TO_FTP_CHANNEL)
    public MessageHandler handler() {
        FtpMessageHandler handler = new FtpMessageHandler(ftpSessionFactory());
        handler.setAutoCreateDirectory(true);
        handler.setRemoteDirectoryExpression(new LiteralExpression(ftpConfiguration.baseDirectory()));
        handler.setFileNameGenerator(message -> (String) message.getHeaders().get(FILE_NAME_HEADER));
        return handler;
    }
}
