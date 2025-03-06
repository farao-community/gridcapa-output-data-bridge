/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.output_data_bridge;

import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class CustomStreamTransformer extends AbstractTransformer {

    private final MinioAdapter minioAdapter;
    private final List<String> whitelist;

    public CustomStreamTransformer(final MinioAdapter minioAdapter, final List<String> whitelist) {
        this.minioAdapter = minioAdapter;
        this.whitelist = whitelist;
    }

    public InputStream openUrlStream(final String urlString) {
        try {
            if (whitelist.stream().noneMatch(urlString::startsWith)) {
                throw new DataBridgeException(String.format("URL '%s' is not part of application's whitelisted url's.", urlString));
            }
            final URL url = new URI(urlString).toURL();
            return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            throw new DataBridgeException(String.format("Exception occurred while retrieving file content from %s", urlString), e);
        }
    }

    @Override
    protected Object doTransform(Message<?> message) {
        final String fileRemoteFile = (String) message.getHeaders().get("file_remoteFile");
        final String preSignedUrl = minioAdapter.generatePreSignedUrl(fileRemoteFile);

        try (InputStream stream = openUrlStream(preSignedUrl)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileCopyUtils.copy(stream, baos);

            Closeable closeableResource = StaticMessageHeaderAccessor.getCloseableResource(message);
            if (closeableResource != null) {
                closeableResource.close();
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
