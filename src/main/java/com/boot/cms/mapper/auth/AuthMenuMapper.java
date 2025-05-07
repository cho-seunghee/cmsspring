package com.boot.cms.mapper.auth;

import com.boot.cms.entity.auth.AuthMenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthMenuMapper {
    List<AuthMenuEntity> findByMenu(@Param("userId") String userId);
}
