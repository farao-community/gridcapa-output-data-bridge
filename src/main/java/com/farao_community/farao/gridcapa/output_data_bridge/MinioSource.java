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
import com.farao_community.farao.gridcapa.output_data_bridge.configuration.OutputDataBridgeConfiguration;
import com.farao_community.farao.gridcapa.output_data_bridge.configuration.OutputDataBridgeSourceMinioConfiguration;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.apache.commons.io.FilenameUtils;
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
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

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

    private final OutputDataBridgeSourceMinioConfiguration configuration;
    private final List<String> whitelist;

    public MinioSource(final OutputDataBridgeConfiguration configuration) {
        this.configuration = configuration.sources().minio();
        this.whitelist = configuration.whitelist();
    }

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
        AWSCredentials credentials = new BasicAWSCredentials(configuration.accessKey(), configuration.secretKey());
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride(AWS_CLIENT_SIGNER_TYPE);

        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(configuration.url(), Regions.US_EAST_1.name()))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }

    private ConcurrentMetadataStore createMetadataStoreForFilePersistence() {
        Path persistenceFilePath = Path.of(configuration.fileListPersistenceFile());
        PropertiesPersistingMetadataStore filePersistenceMetadataStore = new PropertiesPersistingMetadataStore();
        filePersistenceMetadataStore.setBaseDirectory(persistenceFilePath.getParent().toString());
        filePersistenceMetadataStore.setFileName(persistenceFilePath.getFileName().toString());
        filePersistenceMetadataStore.afterPropertiesSet();
        return filePersistenceMetadataStore;
    }

    private S3PersistentAcceptOnceFileListFilter createFilePersistenceFilter() {
        ConcurrentMetadataStore metadataStore = createMetadataStoreForFilePersistence();
        S3PersistentAcceptOnceFileListFilter s3PersistentAcceptOnceFileListFilter = new S3PersistentAcceptOnceFileListFilter(metadataStore, FROM_MINIO_CHANNEL);
        s3PersistentAcceptOnceFileListFilter.setFlushOnUpdate(true);
        return s3PersistentAcceptOnceFileListFilter;
    }

    @Bean
    @InboundChannelAdapter(value = MINIO_CHANNEL, poller = @Poller(fixedDelay = "${data-bridge.sources.minio.polling-delay-in-ms}"))
    public MessageSource<InputStream> s3InboundStreamingMessageSource() {
        S3StreamingMessageSource messageSource = new S3StreamingMessageSource(template());
        messageSource.setRemoteDirectory(configuration.bucket() + "/" + configuration.baseDirectory());
        messageSource.setFilter(createFilePersistenceFilter());
        return messageSource;
    }

    @Bean
    public IntegrationFlow fromMinioFlow(MinioAdapter minioAdapter) {
        return IntegrationFlow.from("fromMinioDirectChannel")
                .transform(Message.class, this::addFileNameHeader)
                .transform(new CustomStreamTransformer(minioAdapter, whitelist))
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
