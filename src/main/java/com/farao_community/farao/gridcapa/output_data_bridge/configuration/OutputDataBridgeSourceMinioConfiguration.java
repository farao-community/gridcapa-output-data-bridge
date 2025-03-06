/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.output_data_bridge.configuration;

public record OutputDataBridgeSourceMinioConfiguration(
        String url,
        String accessKey,
        String secretKey,
        String bucket,
        String baseDirectory,
        String fileListPersistenceFile
) {
    @Override
    public String fileListPersistenceFile() {
        return this.fileListPersistenceFile == null ? "/tmp/gridcapa/minio-metadata-store.properties" : this.fileListPersistenceFile;
    }
}
