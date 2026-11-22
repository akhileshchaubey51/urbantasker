package com.urbantasker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UrbanTaskerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrbanTaskerApplication.class, args);
        System.out.println("==================================================");
        System.out.println(" UrbanTasker Full-Stack Service Marketplace Ready ");
        System.out.println(" Access URL: http://localhost:8080                ");
        System.out.println("==================================================");
    }
}
