/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.output_data_bridge.sinks;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.MessageHandler;

import static com.farao_community.farao.gridcapa.output_data_bridge.MinioSource.FILE_NAME_HEADER;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
@ConditionalOnProperty(prefix = "data-bridge.sinks.sftp", name = "active", havingValue = "true")
public class SftpSink {

    public static final String TO_SFTP_CHANNEL = "toSftpChannel";
    public static final int SFTP_SESSION_CACHE_SIZE = 10;

    @Value("${data-bridge.sinks.sftp.host}")
    private String sftpHost;
    @Value("${data-bridge.sinks.sftp.port}")
    private int sftpPort;
    @Value("${data-bridge.sinks.sftp.username}")
    private String sftpUsername;
    @Value("${data-bridge.sinks.sftp.password}")
    private String sftpPassword;
    @Value("${data-bridge.sinks.sftp.base-directory}")
    private String sftpBaseDirectory;

    private SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
        final DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost(sftpHost);
        factory.setPort(sftpPort);
        factory.setUser(sftpUsername);
        factory.setPassword(sftpPassword);
        factory.setAllowUnknownKeys(true);
        return new CachingSessionFactory<>(factory, SFTP_SESSION_CACHE_SIZE);
    }

    @Bean
    @ServiceActivator(inputChannel = TO_SFTP_CHANNEL)
    public MessageHandler handler() {
        final SftpMessageHandler handler = new SftpMessageHandler(sftpSessionFactory());
        handler.setRemoteDirectoryExpression(new LiteralExpression(sftpBaseDirectory));
        handler.setFileNameGenerator(message -> (String) message.getHeaders().get(FILE_NAME_HEADER));
        return handler;
    }
}
