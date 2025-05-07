package com.boot.cms.mapper.sample;

import com.boot.cms.entity.sample.MybaitsXmlMethodEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MybaitsXmlMethodMapper {
    MybaitsXmlMethodEntity findByIdXml(@Param("userId") String userId);
}
