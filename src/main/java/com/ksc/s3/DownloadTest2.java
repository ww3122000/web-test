package com.ksc.s3;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Throwables;
import com.ksc.s3.util.HttpUtil;

@Slf4j
public class DownloadTest2 {

    public static void main(String[] args) throws InterruptedException {
        if (args.length >= 2) {
            int threadNum = Integer.valueOf(args[0]);
            int looopNumber = Integer.valueOf(args[1]);
            
            ExecutorService pool = Executors.newFixedThreadPool(threadNum);
            String url = null;
            if(args.length >= 3){
                url = args[2];
            } else {
                url = "http://127.0.0.1:20020/download";
            }
            for (int i = 0; i < threadNum; i++) {
                pool.submit(new DownloadCallable(looopNumber, url));
            }
            pool.shutdown();
            while (!pool.isTerminated()) {
                Thread.sleep(1 * 1000L);
            }
        } else {
            System.out.println("error you must input threadNum and loopNum, such as: java -jar *.jar $threadNum $looopNumber");
        }

    }
    
    static class DownloadCallable implements Callable{
        
        private int looopNumber;
        private String url;

        public DownloadCallable(int looopNumber, String url) {
            super();
            this.looopNumber = looopNumber;
            this.url = url;
        }

        public Object call() throws Exception {
            for(int i = 0; i < looopNumber; i++){
                HttpUtil httpUtil = HttpUtil.getInstance();
                long start = System.currentTimeMillis();
                try {
                    httpUtil.doGetRetString(url);
//                    httpUtil.doGetAndDiscard(url);
//                    httpUtil.doGetAndDiscard("http://127.0.0.1:20020/download");
                    log.info("elapsed time: " + (System.currentTimeMillis() - start));
                } catch (Exception e) {
                    log.error(Throwables.getStackTraceAsString(e));
                }
            }
            log.info("stoped, ThreadName: " + Thread.currentThread().getName());
            return null;
        }
        
    }

}
