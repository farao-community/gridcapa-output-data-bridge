/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.output_data_bridge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import static com.farao_community.farao.gridcapa.output_data_bridge.MinioSource.FILE_NAME_HEADER;
import static com.farao_community.farao.gridcapa.output_data_bridge.MinioSource.FROM_MINIO_CHANNEL;
import static com.farao_community.farao.gridcapa.output_data_bridge.sinks.FtpSink.TO_FTP_CHANNEL;
import static com.farao_community.farao.gridcapa.output_data_bridge.sinks.SftpSink.TO_SFTP_CHANNEL;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Component
public class FileTransferFlow {
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    @Value("${data-bridge.file-regex}")
    private String fileNameRegex;

    @Bean
    public MessageChannel toSftpChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel toFtpChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public IntegrationFlow transferFlowFromMinioToSftp() {
        return generateFileTransferFlow(FROM_MINIO_CHANNEL, TO_SFTP_CHANNEL, fileNameRegex);
    }

    @Bean
    public IntegrationFlow transferFlowFromMinioToFtp() {
        return generateFileTransferFlow(FROM_MINIO_CHANNEL, TO_FTP_CHANNEL, fileNameRegex);
    }

    IntegrationFlow generateFileTransferFlow(String fromChannel, String toChannel, String fileNamePattern) {
        return IntegrationFlows.from(fromChannel)
                .filter(Message.class, message -> fileNameMatches(message, fileNamePattern))
                .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"File \" + headers." + FILE_NAME_HEADER + " + \" matches expected pattern, transferred to FTP\""))
                .channel(toChannel)
                .get();
    }

    private boolean fileNameMatches(Message<?> message, String fileNamePattern) {
        String filename = (String) message.getHeaders().get(FILE_NAME_HEADER);
        if (filename == null) {
            throw new DataBridgeException(String.format("Message %s does not contain gridcapa file name header. Please verify the endpoint implementation", message.toString()));
        }
        return filename.matches(fileNamePattern);
    }

    public String getFileNameRegex() {
        return fileNameRegex;
    }
}
