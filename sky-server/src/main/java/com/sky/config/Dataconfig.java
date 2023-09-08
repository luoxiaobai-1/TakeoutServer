package com.sky.config;

import com.sky.constant.StatusConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Dataconfig implements CommandLineRunner {
    @Autowired
    RedisCache redisCache;
    public static final String KEY = "SHOP_STATUS";
    @Override
    public void run(String... args) throws Exception {
        log.info("初始化");
        redisCache.setCacheObject(KEY, StatusConstant.ENABLE);

    }
}
