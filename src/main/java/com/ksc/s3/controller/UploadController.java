package com.ksc.s3.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Slf4j
@Controller
@RequestMapping("/")
public class UploadController {

	/**
	 * 文件上传
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	@ResponseBody
	public String upload(HttpServletRequest request,
			HttpServletResponse response) {
		String result = "OK";
		try {
			MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
			MultipartFile myFile = multipartRequest.getFile("file");
			byte[] bytes = myFile.getBytes();
			log.info("upload length: " + bytes.length);
		} catch (Exception e) {
			e.printStackTrace();
			result = "Failed";
		}
		return result;
	}

	@RequestMapping(value = "/uploadObject", method = RequestMethod.POST)
	public String upload(@RequestParam("file") MultipartFile file,
			Map<String, Object> model) {
		String result = "OK";
		log.info("Object name parameter is " + file.getOriginalFilename());
		try {
			log.info(file.getBytes().length + "");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

}
