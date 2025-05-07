package com.boot.cms.controller.sample;

import com.boot.cms.entity.sample.MybaitsClassMethodEntity;
import com.boot.cms.entity.sample.MybaitsXmlMethodEntity;
import com.boot.cms.mapper.sample.MybaitsXmlMethodMapper;
import com.boot.cms.service.sample.MybaitsClassMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/sample")
@RequiredArgsConstructor
public class MybaitsClassMethodController {
    private final MybaitsClassMethodService service;

    private final MybaitsXmlMethodMapper mybaitsXmlMethodMapper;

    // http://localhost:8080/api/sample/find/admin
    // 자바코드에서 sql 구현 시
    @GetMapping("/find/{id}")
    public MybaitsClassMethodEntity findById(@PathVariable String id) {
        return service.findById(id);
    }

    // http://localhost:8080/api/sample/findByIdXml?userId=admin
    // xm에서 sql 구현 시
    @GetMapping("/findByIdXml")
    public MybaitsXmlMethodEntity findByIdXml(@RequestParam("userId") String userId) {
        return mybaitsXmlMethodMapper.findByIdXml(userId);
    }
}
