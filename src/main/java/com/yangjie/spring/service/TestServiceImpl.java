package com.yangjie.spring.service;

import com.yangjie.spring.annoation.YJService;

@YJService
public class TestServiceImpl implements TestService {
    @Override
    public String hello() {
        return "nihao";
    }
}
