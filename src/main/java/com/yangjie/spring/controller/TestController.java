package com.yangjie.spring.controller;

import com.yangjie.spring.annoation.YJAutowired;
import com.yangjie.spring.annoation.YJController;
import com.yangjie.spring.annoation.YJRequestMapping;
import com.yangjie.spring.annoation.YJRequestParam;
import com.yangjie.spring.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@YJController
public class TestController {

    @YJAutowired
    private TestService testService;

    @YJRequestMapping("/hello")
    public String hello(HttpServletRequest req, HttpServletResponse response, @YJRequestParam("name") String name) {
        try {
            response.getWriter().write(testService.hello()+name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return testService.hello();
    }
}
