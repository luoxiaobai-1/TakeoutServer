package com.sky.Aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Slf4j
@Component
public class as {
    @Pointcut("@annotation(com.sky.annotation.jkl)")
    public void poincut(){}
    @Before("poincut()")
    public void  get(JoinPoint joinPoint)
    {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        log.error(request.getRequestURI());
        log.error(request.getRemoteAddr());
        joinPoint.getArgs();

    }
    @After("poincut()")
    public void get1 ()
    {
        log.error("aaaaaaa6");
    }

}
