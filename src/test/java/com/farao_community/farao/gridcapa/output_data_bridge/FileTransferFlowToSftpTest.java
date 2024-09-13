package com.farao_community.farao.gridcapa.output_data_bridge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.support.MessageChannelReference;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
@ActiveProfiles("sftp")
class FileTransferFlowToSftpTest {

    @Autowired
    private FileTransferFlow fileTransferFlow;

    @Test
    void transferFlowFromMinioToSftp() {
        IntegrationFlow integrationFlow = fileTransferFlow.transferFlowFromMinioToSftp();
        assertEquals("(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2})_(?<hour>[0-9]{2})(?<minute>[0-9]{2})_.*.(uct|UCT)", fileTransferFlow.getFileNameRegex());
        MessageChannelReference messageChannelReference = (MessageChannelReference) integrationFlow.getInputChannel();
        assertEquals("fromMinioChannel", messageChannelReference.name());
    }
}
