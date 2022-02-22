/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.output_data_bridge;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.aws.inbound.S3StreamingMessageSource;
import org.springframework.integration.aws.support.S3RemoteFileTemplate;
import org.springframework.integration.aws.support.filters.S3PersistentAcceptOnceFileListFilter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.io.File;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
public class MinioSource {
    public static final String AWS_CLIENT_SIGNER_TYPE = "AWSS3V4SignerType";
    public static final String FROM_MINIO_CHANNEL = "fromMinioChannel";
    public static final String FILE_NAME_HEADER = "gridcapa_file_name";
    public static final String MINIO_CHANNEL = "minioChannel";

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    @Value("${data-bridge.sources.minio.url}")
    private String url;
    @Value("${data-bridge.sources.minio.access-key}")
    private String accessKey;
    @Value("${data-bridge.sources.minio.secret-key}")
    private String secretKey;
    @Value("${data-bridge.sources.minio.bucket}")
    private String bucket;
    @Value("${data-bridge.sources.minio.base-directory}")
    private String baseDirectory;

    @Bean
    public PollableChannel minioChannel() {
        return new QueueChannel();
    }

    @Bean
    @BridgeFrom(value = MINIO_CHANNEL, poller = @Poller(fixedDelay = "${data-bridge.sources.minio.polling-delay-in-ms}", maxMessagesPerPoll = "10"))
    public SubscribableChannel fromMinioDirectChannel() {
        return new DirectChannel();
    }

    private AmazonS3 amazonS3() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride(AWS_CLIENT_SIGNER_TYPE);

        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(url, Regions.US_EAST_1.name()))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }

    @Bean
    @InboundChannelAdapter(value = MINIO_CHANNEL, poller = @Poller(fixedDelay = "${data-bridge.sources.minio.polling-delay-in-ms}"))
    public MessageSource s3InboundStreamingMessageSource() {
        S3StreamingMessageSource messageSource = new S3StreamingMessageSource(template());
        messageSource.setRemoteDirectory(bucket + "/" + baseDirectory);
        messageSource.setFilter(new S3PersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), FROM_MINIO_CHANNEL));
        return messageSource;
    }

    @Bean
    public IntegrationFlow fromMinioFlow() {
        return IntegrationFlows.from("fromMinioDirectChannel")
                .transform(new StreamTransformer())
                .transform(Message.class, this::addFileNameHeader)
                .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"Integration of file \" + headers." + FILE_NAME_HEADER))
                .channel(FROM_MINIO_CHANNEL)
                .get();
    }

    private Message<File> addFileNameHeader(Message<File> message) {
        String filename = FilenameUtils.getName((String) message.getHeaders().get("file_remoteFile"));
        return MessageBuilder.fromMessage(message)
                .setHeader(FILE_NAME_HEADER, filename)
                .build();
    }

    private S3RemoteFileTemplate template() {
        return new S3RemoteFileTemplate(MinioConnectionFix.getSessionFactory(amazonS3()));
    }
}
