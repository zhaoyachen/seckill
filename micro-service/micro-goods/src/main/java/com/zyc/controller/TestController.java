package com.zyc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/goods/test")
@Slf4j
public class TestController {

    /**
     *  @CrossOrigin 解决跨域问题
     * @return
     */
    @CrossOrigin
    @RequestMapping("/test1")
    @Cacheable(cacheNames = "seckill",key = "'times' + #time")
    public String test1(String time) {
        System.out.println("查询数据库");
        log.info("查询");
        return "test1";
    }

    @CrossOrigin
    @RequestMapping("/test2")
    public String test2() {
        log.info("查询");
        return "test2";
    }
}
