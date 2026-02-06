package com.zyyyys.culinarywhispers;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.zyyyys.culinarywhispers.module.*.mapper")
public class CulinaryWhispersApplication {

    public static void main(String[] args) {
        SpringApplication.run(CulinaryWhispersApplication.class, args);
    }

}
