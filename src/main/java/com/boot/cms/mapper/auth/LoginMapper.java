package com.boot.cms.mapper.auth;

import com.boot.cms.entity.auth.LoginEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginMapper {
    @Select("""
        SELECT a.EMPNO, a.EMPNM, a.EMPPWD, IFNULL(b.AUTHID, '') AUTH, a.ORGCD ,c.Name AS ORGNM, IFNULL(a.PWDCHGYN, '') PWDCHGYN
        FROM tb_userinfo a
        LEFT JOIN tb_userauthgroup b ON a.EMPNO = b.EMPNO
        LEFT JOIN tb_ktnorg c ON a.ORGCD = c.Code
        WHERE a.EMPNO = #{empNo}
        AND a.EMPPWD = #{empPw}
    """)
    LoginEntity loginCheck(@Param("empNo") String empNo, @Param("empPw") String empPw);
}
