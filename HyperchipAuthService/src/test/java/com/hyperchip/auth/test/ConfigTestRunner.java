package com.hyperchip.auth.test;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class ConfigTestRunner implements CommandLineRunner {

    @Autowired
    private Environment env;

    @Override
    public void run(String... args) throws Exception {
        // Print out database config from environment
        System.out.println("====== CONFIG TEST START ======");
        String dbUrl = env.getProperty("spring.datasource.url");
        String dbUser = env.getProperty("spring.datasource.username");
        String dbDriver = env.getProperty("spring.datasource.driver-class-name");

        if (dbUrl != null && dbUser != null && dbDriver != null) {
            System.out.println("Datasource URL: " + dbUrl);
            System.out.println("Datasource Username: " + dbUser);
            System.out.println("Datasource Driver: " + dbDriver);
            System.out.println("Config fetched successfully from Config Server!");
        } else {
            System.out.println("Datasource config NOT found! Config Server may not be connected.");
        }
        System.out.println("====== CONFIG TEST END ======");
    }
}
