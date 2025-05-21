package com.boot.cms.controller.sample;

import com.boot.cms.entity.sample.MybaitsClassMethodEntity;
import com.boot.cms.entity.sample.MybaitsXmlMethodEntity;
import com.boot.cms.mapper.sample.MybaitsXmlMethodMapper;
import com.boot.cms.service.sample.MybaitsClassMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/sample")
@RequiredArgsConstructor
@Tag(name = "Sample MyBatis", description = "Endpoints for sample MyBatis queries")
public class MybaitsClassMethodController {
    private final MybaitsClassMethodService service;

    private final MybaitsXmlMethodMapper mybaitsXmlMethodMapper;

    // http://localhost:8080/api/sample/find/admin
    // 자바코드에서 sql 구현 시
    @Operation(summary = "Find user by ID (Class-based)", description = "Retrieves user data by ID using class-based MyBatis query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = MybaitsClassMethodEntity.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/find/{id}")
    public MybaitsClassMethodEntity findById(@PathVariable String id) {
        return service.findById(id);
    }

    // http://localhost:8080/api/sample/findByIdXml?userId=admin
    // xm에서 sql 구현 시
    @Operation(summary = "Find user by ID (XML-based)", description = "Retrieves user data by ID using XML-based MyBatis query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = MybaitsXmlMethodEntity.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/findByIdXml")
    public MybaitsXmlMethodEntity findByIdXml(@RequestParam("userId") String userId) {
        return mybaitsXmlMethodMapper.findByIdXml(userId);
    }
}
