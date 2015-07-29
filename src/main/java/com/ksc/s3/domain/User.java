package com.ksc.s3.domain;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 用户实体
 * @author wangwei
 *
 */
@Data
public class User implements Serializable{
    
     /**
     * 
     */
    private static final long serialVersionUID = 8036256245942995853L;

    private long id;
     
     private String name;
     
     private int age;
     
     private String email;
     
     private String home;
     
     private String passwd;
     
     private Date lastModifyTime;

}
