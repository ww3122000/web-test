package com.ksc.s3.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ksc.s3.domain.User;
import com.ksc.s3.service.UserService;
import com.ksc.s3.util.UserUtil;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 保存用户
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/save")
    @ResponseBody
    public String save(HttpServletRequest request, HttpServletResponse response) {
        String result = "OK";
        try {
            User user = UserUtil.generateUserInstance();
            userService.save(user);
        } catch (Exception e) {
            e.printStackTrace();
            result = "Failed";
        }
        return result;
    }

    @RequestMapping(value = "/byName")
    @ResponseBody
    public User byName(HttpServletRequest request, HttpServletResponse response) {
        User user = null;
        try {
            user = userService.findByName("wangwei");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }
    
    @RequestMapping(value = "/byEmail")
    @ResponseBody
    public User findByEmail(HttpServletRequest request, HttpServletResponse response) {
        User user = null;
        try {
            user = userService.findByEmail("wangwei14@126.com");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }
    
    @RequestMapping(value = "/byId")
    @ResponseBody
    public User byId(HttpServletRequest request, HttpServletResponse response) {
        User user = null;
        try {
            user = userService.findById(1L);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    
    @RequestMapping(value = "/update")
    @ResponseBody
    public String update(HttpServletRequest request, HttpServletResponse response) {
        String result = "OK";
        try {
            User user = UserUtil.generateUserInstance();;
            userService.update(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
