package com.hyperchip.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;


@EnableConfigServer
@SpringBootApplication
public class HyperchipConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HyperchipConfigServerApplication.class, args);
    }
}
