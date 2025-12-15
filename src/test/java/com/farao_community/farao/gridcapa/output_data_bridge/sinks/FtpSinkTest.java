package com.farao_community.farao.gridcapa.output_data_bridge.sinks;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("ftp")
class FtpSinkTest {

    @Autowired
    private FtpSink sink;

    @Test
    void handlerIsNotNull() {
        Assertions.assertNotNull(sink.handler());
    }
}
