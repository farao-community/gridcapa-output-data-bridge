package com.farao_community.farao.gridcapa.output_data_bridge.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.boot.actuate.health.Status.DOWN;

@SpringBootTest
@ActiveProfiles("sftp")
class SftpHealthIndicatorTest {

    @Autowired
    private SftpHealthIndicator ftpHealthIndicator;

    @Test
    void healthDownBecauseNoSession() {
        assertSame(DOWN, ftpHealthIndicator.health().getStatus());
    }
}