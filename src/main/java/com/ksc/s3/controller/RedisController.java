package com.ksc.s3.controller;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.redis.RedisProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import redis.clients.jedis.JedisPoolConfig;

import com.google.common.base.Stopwatch;

@Slf4j
@Controller
@RequestMapping("/redis")
public class RedisController {

    private RedisTemplate<?, ?> redisTemplate;
    
    @Autowired
    protected RedisProperties redisProperties;
    
    @Value("${default.value}")
    private String defualtValue = "aaaaaaaaaaaaaaaaaaaa";
    
    @Value("${default.key}")
    private String defualtkey = "aaaaaaaaaaaaaaaaaaaa";
    
    @Value("${test.read}")
    private boolean testRead = false;
    
    
    private RedisConnectionFactory getRedisConnectionFactory(int maxActive, int maxIdle, int minIdle, int maxWait){
        JedisPoolConfig config = jedisPoolConfig(maxActive,  maxIdle,  minIdle,  maxWait);
        
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
        sentinelConfig.master(this.redisProperties.getSentinel().getMaster());
        sentinelConfig.setSentinels(createSentinels(this.redisProperties.getSentinel().getNodes()));
        JedisConnectionFactory factory = new JedisConnectionFactory(sentinelConfig, config);
        return applyProperties(factory);
    }
    
    protected final JedisConnectionFactory applyProperties(
            JedisConnectionFactory factory) {
        factory.setHostName(this.redisProperties.getHost());
        factory.setPort(this.redisProperties.getPort());
        if (this.redisProperties.getPassword() != null) {
            factory.setPassword(this.redisProperties.getPassword());
        }
        factory.setDatabase(this.redisProperties.getDatabase());
        factory.afterPropertiesSet();
        return factory;
    }

    
    private List<RedisNode> createSentinels(String nodes) {
        List<RedisNode> sentinels = new ArrayList<RedisNode>();
        for (String node : StringUtils.commaDelimitedListToStringArray(nodes)) {
            try {
                String[] parts = StringUtils.split(node, ":");
                Assert.state(parts.length == 2, "Must be defined as 'host:port'");
                sentinels.add(new RedisNode(parts[0], Integer.valueOf(parts[1])));
            }
            catch (RuntimeException ex) {
                throw new IllegalStateException("Invalid redis sentinel "
                        + "property '" + node + "'", ex);
            }
        }
        return sentinels;
    }
    
    /**
     * spring.redis.pool.max-idle=50
     * spring.redis.pool.min-idle=50
     * spring.redis.pool.max-active=500
     * spring.redis.pool.max-wait=-1
     * @return
     */
    private JedisPoolConfig jedisPoolConfig(int maxActive, int maxIdle, int minIdle, int maxWait) {
        JedisPoolConfig config = new JedisPoolConfig();
        RedisProperties.Pool props = this.redisProperties.getPool();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxWaitMillis(maxWait);
        
//        config.setMaxTotal(props.getMaxActive());
//        config.setMaxIdle(props.getMaxIdle());
//        config.setMinIdle(props.getMinIdle());
//        config.setMaxWaitMillis(props.getMaxWait());
        return config;
    }
    

    /**
     * 保存用户
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/run")
    @ResponseBody
    public String save(HttpServletRequest request, HttpServletResponse response, 
            @RequestParam(value = "maxActive", required = false, defaultValue = "0") int maxActive, 
            @RequestParam(value = "maxIdle", required = false, defaultValue = "0") int maxIdle, 
            @RequestParam(value = "minIdle", required = false, defaultValue = "0") int minIdle, 
            @RequestParam(value = "maxWait", required = false, defaultValue = "-1") int maxWait) {
        
        RedisProperties.Pool props = this.redisProperties.getPool();
        if(maxActive <= 0){
            maxActive = props.getMaxActive();
        }
        
        if(maxIdle <= 0){
            maxIdle = props.getMaxIdle();
        }
        
        if(minIdle <= 0){
            minIdle = props.getMinIdle();
        } 
        
        if(maxWait < 0){
            maxWait = props.getMaxWait();
        }
        
        redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(getRedisConnectionFactory(maxActive,  maxIdle,  minIdle,  maxWait));
        redisTemplate.afterPropertiesSet();
        String result = "OK";
        AtomicInteger failedCount = new AtomicInteger(0);
        try {
            long loopCount = 1000;
            int threadNum = 1000;
//            ExecutorService pool = Executors.newFixedThreadPool(threadNum);
            ExecutorService pool = new ThreadPoolExecutor(threadNum, threadNum, 100000, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(threadNum));
            String key = "wangwei";
            String value = defualtValue;
            
            Stopwatch watch = Stopwatch.createUnstarted();
            CountDownLatch startSignal = new CountDownLatch(1);
            CountDownLatch doneSignal = new CountDownLatch(threadNum);
            for(int i = 0; i < threadNum; i++){
                RedisRunnable task = new RedisRunnable(key + "-t-" + i, i + "." + value, redisTemplate, loopCount, startSignal, doneSignal, failedCount);
                pool.submit(task);
            }
            
            log.info("all thread Ready, go!");
            watch.start();
            startSignal.countDown();
            doneSignal.await();
            watch.stop();
            if (watch.elapsed(TimeUnit.SECONDS) > 0) {
                log.info("at maxActive: {}, maxIdle: {}, minIdle: {}, maxWait: {}, total opt count: {}, failedCount: {}, elapsed time: {}, all completed speed: {} 次/s", maxActive, maxIdle, minIdle,
                        maxWait, String.valueOf(loopCount * threadNum), failedCount, watch.toString(), String.valueOf((loopCount * threadNum - failedCount.get()) / watch.elapsed(TimeUnit.SECONDS)));
            } else {
                log.info("at maxActive: {}, maxIdle: {}, minIdle: {}, maxWait: {}, total opt count: {}, failedCount: {}, elapsed time: {}", maxActive, maxIdle, minIdle, maxWait,
                        String.valueOf(loopCount * threadNum), failedCount, watch.toString());
            }
            pool.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            result = "Failed";
        }
        return result;
    }
    
    @AllArgsConstructor
    class RedisRunnable implements Runnable {
        
        private String key;
        
        private String value;
        
        private RedisTemplate<?, ?> redisTemplate;
        
        private long loopCount;
        
        private CountDownLatch startSignal;
        private CountDownLatch doneSignal;
        private AtomicInteger failedCount;

        
        @Override
        public void run() {
            try {
                startSignal.await();
                Stopwatch watch = Stopwatch.createUnstarted();
                watch.start();
                for(int i = 0; i < loopCount; i++){
                    try {
                        redisTemplate.execute(new RedisCallback<String>(){
                            @Override
                            public String doInRedis(RedisConnection connection) throws DataAccessException {
                                try {
                                    if(testRead){
                                        connection.get(defualtkey.getBytes("UTF-8"));
                                    } else {
                                        String temp = key + "-order-" + UUID.randomUUID();
//                                      String temp = key;
                                        connection.set(temp.getBytes("UTF-8"), value.getBytes("UTF-8"));
                                        log.debug(temp);
                                    }
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        });
                    } catch (Exception e) {
                        failedCount.incrementAndGet();
                        e.printStackTrace();
                    }
                }
                watch.stop();
                log.info(Thread.currentThread().getName() + "completed, elapsed time:" + watch.toString());
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally {
                doneSignal.countDown();
            }
        }
    }
}
