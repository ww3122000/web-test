package com.ksc.s3.util;

import java.util.Date;

import com.ksc.s3.domain.User;

public class UserUtil {
    
    /**
     * 获取一用户
     * @return
     */
    public static User generateUserInstance(){
        User user = new User();
        user.setId(1L);
        user.setAge(10);
        user.setEmail("wangwei14@126.com");
        user.setHome("北京市");
        user.setName("wangwei");
        user.setPasswd("123456");
        user.setLastModifyTime(new Date(System.currentTimeMillis()));
        return user;
    }

}
