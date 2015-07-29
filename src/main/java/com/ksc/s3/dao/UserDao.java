package com.ksc.s3.dao;

import com.ksc.s3.domain.User;

public interface UserDao {
    
    void save(User user);
    
    User findById(long id);
    
    User findByName(String name);
    
    User findByEmail(String email);
    
    void update(User user);

}
