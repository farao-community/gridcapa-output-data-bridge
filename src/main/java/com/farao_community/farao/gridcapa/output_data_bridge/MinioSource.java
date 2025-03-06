/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.output_data_bridge;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.aws.inbound.S3StreamingMessageSource;
import org.springframework.integration.aws.support.S3RemoteFileTemplate;
import org.springframework.integration.aws.support.filters.S3PersistentAcceptOnceFileListFilter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.transformer.StreamTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

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
    @Value("${data-bridge.sources.minio.file-list-persistence-file:/tmp/gridcapa/minio-metadata-store.properties}")
    private String fileListPersistenceFile;

    @Bean
    public PollableChannel minioChannel() {
        return new QueueChannel();
    }



    private S3Client amazonS3() throws URISyntaxException {
        AwsCredentials credentials = AwsBasicCredentials.builder().accessKeyId(accessKey).secretAccessKey(secretKey).build();

        return S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(new URI(url))
                .overrideConfiguration(c -> c.putAdvancedOption(SdkAdvancedClientOption.SIGNER, AwsS3V4Signer.create()))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    private ConcurrentMetadataStore createMetadataStoreForFilePersistence() {
        Path persistenceFilePath = Path.of(fileListPersistenceFile);
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
    public MessageSource<InputStream> s3InboundStreamingMessageSource() throws URISyntaxException {
        S3StreamingMessageSource messageSource = new S3StreamingMessageSource(template());
        messageSource.setRemoteDirectory(bucket + "/" + baseDirectory);
        messageSource.setFilter(createFilePersistenceFilter());
        return messageSource;
    }

    @Bean
    public IntegrationFlow fromMinioFlow() {
        return IntegrationFlow.from("minioChannel")
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

    private S3RemoteFileTemplate template() throws URISyntaxException {
        return new S3RemoteFileTemplate(MinioConnectionFix.getSessionFactory(amazonS3()));
    }
}
