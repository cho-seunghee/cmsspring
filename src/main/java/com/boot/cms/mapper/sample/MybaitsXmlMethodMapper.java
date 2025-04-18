package com.boot.cms.mapper.sample;

import com.boot.cms.entity.MybaitsClassMethodEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MybaitsXmlMethodMapper {
    MybaitsClassMethodEntity findByIdXml(@Param("userId") String userId);
}
