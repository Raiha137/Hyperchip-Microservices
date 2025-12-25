package com.hyperchip.user;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Disabled because integration beans (Mail, Eureka, Config) are not needed for build")
class HyperchipUserApplicationTests {

    @Test
    void contextLoads() {
    }
}
