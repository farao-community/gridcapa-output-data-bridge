package com.farao_community.farao.gridcapa.output_data_bridge.sinks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("sftp")
class SftpSinkTest {

    @Autowired
    private SftpSink sink;

    @Test
    void handlerIsNotNull() {
        assertNotNull(sink.handler());
    }
}
