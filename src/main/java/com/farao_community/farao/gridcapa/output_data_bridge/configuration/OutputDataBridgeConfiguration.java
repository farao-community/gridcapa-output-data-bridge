/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.output_data_bridge.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("data-bridge")
public record OutputDataBridgeConfiguration(
        String fileRegex,
        OutputDataBridgeSources sources,
        OutputDataBridgeSinks sinks,
        List<String> whitelist
) {
    public record OutputDataBridgeSources(OutputDataBridgeSourceMinioConfiguration minio) {
    }

    public record OutputDataBridgeSinks(
            OutputDataBridgeSinkFtpSftpConfiguration ftp,
            OutputDataBridgeSinkFtpSftpConfiguration sftp
    ) {
    }
}
