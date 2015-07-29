package com.ksc.s3.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ksc.s3.dao.UserDao;
import com.ksc.s3.domain.User;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;
    
    public void save(User user) {
        userDao.save(user);
    }

    public User findById(long id) {
        return userDao.findById(id);
    }

    public User findByName(String name) {
        return userDao.findByName(name);
    }

    public User findByEmail(String email) {
        return userDao.findByEmail(email);
    }

    public void update(User user) {
        userDao.update(user);
    }
}
