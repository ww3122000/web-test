package redis;

import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.base.Stopwatch;
import com.ksc.s3.StartUp;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = StartUp.class)
@Slf4j
public class RedisTest {
    
    @Autowired
    private RedisTemplate<?, ?> redisTemplate;
    
    @Value("${spring.redis.pool.max-active}")
    private int maxActive;
    
    @Value("${default.key}")
    private String defualtkey;
    
    @Test
    public void testSetString(){
        System.out.println(maxActive);
        redisTemplate.execute(new RedisCallback<String>(){
            @Override
            public String doInRedis(RedisConnection connection) throws DataAccessException {
                try {
                    connection.set("wangwei".getBytes("UTF-8"), "kingsoft".getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return null;
            }
            
        });
    }
    
    @Test
    public void testGetString(){
        for(int i = 0; i < Integer.MAX_VALUE; i++){
            try {
                Stopwatch watch = Stopwatch.createUnstarted();
                watch.start();
                redisTemplate.execute(new RedisCallback<String>(){
                    @Override
                    public String doInRedis(RedisConnection connection) throws DataAccessException {
                        try {
                            connection.get(defualtkey.getBytes("UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                    
                });
                watch.stop();
                log.info("get success! " + watch.toString());
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                log.info("get failed!");
            }
        }
    }
    
    @Test
    public void testConcurrentOpt() throws InterruptedException{
        long loopCount = 1000;
        int threadNum = 1000;
//        ExecutorService pool = Executors.newFixedThreadPool(threadNum);
        ExecutorService pool = new ThreadPoolExecutor(threadNum, threadNum, 100000, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(threadNum));
        String key = "wangwei";
        String value = "baidu.com";
        
        Stopwatch watch = Stopwatch.createUnstarted();
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(threadNum);
        for(int i = 0; i < threadNum; i++){
            RedisRunnable task = new RedisRunnable(key + "-t-" + i, i + "." + value, redisTemplate, loopCount, startSignal, doneSignal);
            pool.submit(task);
        }
        
        log.info("all thread Ready, go!");
        watch.start();
        startSignal.countDown();
        doneSignal.await();
        watch.stop();
        if(watch.elapsed(TimeUnit.SECONDS) > 0){
            log.info("at maxActive: {}, total opt count: {}, elapsed time: {}, all completed speed: {} 次/s", maxActive, String.valueOf(loopCount * threadNum), watch.toString(), String.valueOf((loopCount * threadNum)/watch.elapsed(TimeUnit.SECONDS)));
        } else {
            log.info("at maxActive: {}, total opt count: {}, elapsed time: {}", maxActive, String.valueOf(loopCount * threadNum), watch.toString());
        }
        pool.shutdown();
    }
    
    @AllArgsConstructor
    class RedisRunnable implements Runnable {
        
        private String key;
        
        private String value;
        
        private RedisTemplate<?, ?> redisTemplate;
        
        private long loopCount;
        
        private CountDownLatch startSignal;
        private CountDownLatch doneSignal;

        
        @Override
        public void run() {
            try {
                startSignal.await();
                Stopwatch watch = Stopwatch.createUnstarted();
                watch.start();
                for(int i = 0; i < loopCount; i++){
                    redisTemplate.execute(new RedisCallback<String>(){
                        @Override
                        public String doInRedis(RedisConnection connection) throws DataAccessException {
                            try {
                                String temp = key + "-order-" + UUID.randomUUID();
//                                String temp = key;
                                connection.set(temp.getBytes("UTF-8"), value.getBytes("UTF-8"));
                                log.debug(temp);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    });
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
