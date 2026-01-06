package com.farao_community.farao.gridcapa.output_data_bridge.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("nosftp")
class NoSftpHealthIndicatorTest {
    @Autowired
    private ApplicationContext context;

    @Test
    void throwsBecauseInactive() {
        assertThrows(Exception.class, () -> context.getBean(SftpHealthIndicator.class));
    }
}
