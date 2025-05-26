package com.boot.cms.controller.sample;

import com.boot.cms.entity.sample.MybaitsClassMethodEntity;
import com.boot.cms.entity.sample.MybaitsXmlMethodEntity;
import com.boot.cms.mapper.sample.MybaitsXmlMethodMapper;
import com.boot.cms.service.sample.MybaitsClassMethodService;
import com.boot.cms.util.CommonApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/sample")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Sample MyBatis", description = "Endpoints for sample MyBatis queries")
public class MybaitsClassMethodController {
    private final MybaitsClassMethodService service;

    private final MybaitsXmlMethodMapper mybaitsXmlMethodMapper;

    @CommonApiResponses
    @GetMapping("/find/{id}")
    public MybaitsClassMethodEntity findById(@PathVariable String id) {
        return service.findById(id);
    }

    @CommonApiResponses
    @GetMapping("/findByIdXml")
    public MybaitsXmlMethodEntity findByIdXml(
            @RequestParam("userId") String userId) {
        return mybaitsXmlMethodMapper.findByIdXml(userId);
    }
}