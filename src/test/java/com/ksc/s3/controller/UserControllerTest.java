package com.ksc.s3.controller;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksc.s3.StartUp;

@WebAppConfiguration
@TransactionConfiguration(defaultRollback = true)
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = StartUp.class)
public class UserControllerTest {
    
    /**
     * 日志打印
     */
    protected Logger logger = LoggerFactory.getLogger(getClass());
    
    protected ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }
    
    @Before
    public void setup() {
//        DomainController domainController = wac.getBean("domainController", DomainController.class);
//        mockMvc = MockMvcBuilders.standaloneSetup(domainController).build();
        
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSave() throws Exception {
        String url = "/user/save";
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post(url);
        MvcResult result = mockMvc.perform(requestBuilder).andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
    }

}
