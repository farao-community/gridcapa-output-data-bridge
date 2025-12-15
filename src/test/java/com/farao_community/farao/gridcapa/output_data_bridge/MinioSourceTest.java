package com.farao_community.farao.gridcapa.output_data_bridge;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("ftp")
class MinioSourceTest {

    @Autowired
    private MinioSource source;

    @Test
    void generateS3InboundStreamingMessageSource() throws URISyntaxException {
        Assertions.assertNotNull(source.s3InboundStreamingMessageSource());
    }

    @Test
    void generateFromMinioFlow() {
        Assertions.assertNotNull(source.fromMinioFlow());
    }
}