package com.boot.cms.mapper.sample;

import com.boot.cms.entity.sample.MybaitsClassMethodEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MybaitsClassMethodMapper {
    @Select("SELECT userid, usernm FROM tb_userinfo_exam WHERE userid = #{userId}")
    MybaitsClassMethodEntity findById(@Param("userId") String userId);
}
