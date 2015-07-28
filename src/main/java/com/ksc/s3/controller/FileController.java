package com.ksc.s3.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Throwables;

@Slf4j
@Controller
@RequestMapping("/")
public class FileController {
    
    private static byte[] data;
    private static byte[] data2;
    
    static {
        try {
            InputStream inputStream = FileController.class.getResourceAsStream("/data.txt");
            data = new byte[inputStream.available()];
            IOUtils.read(inputStream, data);
            IOUtils.closeQuietly(inputStream);
            
            inputStream = FileController.class.getResourceAsStream("/data2.txt");
            data2 = new byte[inputStream.available()];
            IOUtils.read(inputStream, data2);
            IOUtils.closeQuietly(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @RequestMapping("/")
    @ResponseBody
    public String test(){
    	log.info("hello, world");
    	return "hello, world";
    }
    
    

    @RequestMapping("/download")
    public void download(HttpServletResponse response) {
        response.setCharacterEncoding("utf-8");
        response.setContentType("multipart/form-data");

        response.setHeader("Content-Disposition", "attachment;fileName=" + "data.txt");
        long startTime = System.currentTimeMillis();
        try {
//            Thread.sleep(15 * 1000L);
            int length = 512 * 1024;
            int start = 0;
            OutputStream os = response.getOutputStream();
            while ((start < data.length)) {
                int max = start + length < data.length ? length : data.length - start;
                os.write(data, start, max);
                start += max;
//            Thread.sleep(10L);
                
            }
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        } finally{
            log.info("ThreadName: " + Thread.currentThread().getName() + " elapsed time: " + (System.currentTimeMillis() - startTime));  
        }
    }
    
    @RequestMapping("/download2")
    public void download2(HttpServletResponse response) {
        response.setCharacterEncoding("utf-8");
        response.setContentType("multipart/form-data");
        
        response.setHeader("Content-Disposition", "attachment;fileName=" + "data2.txt");
        long startTime = System.currentTimeMillis();
        try {
//            Thread.sleep(20 * 1000L);
//            int length = 5 * 1024 * 1024;
//            int start = 0;
            OutputStream os = response.getOutputStream();
//            while ((start < data2.length)) {
//                int max = start + length < data2.length ? length : data2.length - start;
//                os.write(data2, start, max);
//                start += max;
//                
//            }
            
            IOUtils.write(data2, os);
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        } finally{
            log.info("ThreadName: " + Thread.currentThread().getName() + " elapsed time: " + (System.currentTimeMillis() - startTime));  
        }
    }

}
