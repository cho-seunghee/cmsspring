package com.boot.cms.service.sample;

import com.boot.cms.entity.sample.MybaitsClassMethodEntity;
import com.boot.cms.mapper.sample.MybaitsClassMethodMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MybaitsClassMethodService {
    @Autowired
    private MybaitsClassMethodMapper mapper;

    public MybaitsClassMethodEntity findById(String id) {
        System.out.println("Service: Testing findById(" + id + "):");
        MybaitsClassMethodEntity entity = mapper.findById(id);
        System.out.println("Service: Result: " + entity);
        return entity;
    }
}
