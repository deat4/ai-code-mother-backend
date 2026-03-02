package com.zkf.aicodemother;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class AiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeMotherApplication.class, args);
    }

}
