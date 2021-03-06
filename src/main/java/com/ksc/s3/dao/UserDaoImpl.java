package com.ksc.s3.dao;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Repository;

import com.ksc.s3.domain.User;
import com.ksc.s3.util.UserUtil;

@Repository
@Slf4j
public class UserDaoImpl implements UserDao {

    @Caching(put = {  
            @CachePut(value = "user", key = "#result.id", condition = "#result != null"),  
            @CachePut(value = "user", key = "#result.name", condition = "#result != null"),
            @CachePut(value = "user", key = "#result.email", condition = "#result != null")
    })
    @Override
    public User save(User user) {
        log.info("dao save...");
        return user;
    }

    @Cacheable(value = "user", key = "#id")
    @Override
    public User findById(long id) {
        User user = UserUtil.generateUserInstance();
        log.info("dao findById...");
        return user;
    }

    @Cacheable(value = "user", key = "#name")
    @Override
    public User findByName(String name) {
        User user = UserUtil.generateUserInstance();
        log.info("dao findByName...");
        return user;
    }

    @Cacheable(value = "user", key = "#email")
    @Override
    public User findByEmail(String email) {
        User user = UserUtil.generateUserInstance();
        log.info("dao findByEmail...");
        return user;
    }

    @Caching(evict = {
            @CacheEvict(value = "user", key = "#user.id"),
            @CacheEvict(value = "user", key = "#user.name"),
            @CacheEvict(value = "user", key = "#user.email")
    })
    @Override
    public void update(User user) {
        log.info("dao update...");
    }
}
