package com.example.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringbootApplication {
    public static void main(String[] args) {

        System.err.println("项目运行成功");
        SpringApplication.run(SpringbootApplication.class, args);
    }

}
