package com.zyc.microgoods;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
//不使用数据库
//@SpringBootApplication(scanBasePackages = "com.zyc",exclude = DataSourceAutoConfiguration.class)

@SpringBootApplication(scanBasePackages = "com.zyc")
@EnableCaching //启用缓存
public class MicroGoodsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroGoodsApplication.class, args);
    }

}
