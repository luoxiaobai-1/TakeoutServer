package com.sky.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//命名规则: 类似于配置文件 则把这个类称之为"配置类"一般Config结尾
@Configuration //标识我是一个配置类(代替之前的xml文件)
public class MybatisPlusConfig {
    //铺垫: xml中通过标签管理对象,将对象交给Spring容器管理. <bean>
    //配置类: 将方法的返回值交给Spring容器管理  @Bean注解.

    /**
     * 关于MP分页规则说明
     *   规则: 需要设定一个拦截器.将分页的Sql进行动态的拼接.
     *  Sql: 规则现在的Sql都支持Sql92标准!!!! 设计理念不同
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MARIADB));
        return interceptor;
    }
}
