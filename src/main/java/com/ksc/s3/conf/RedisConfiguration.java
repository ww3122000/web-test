package com.ksc.s3.conf;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import redis.clients.jedis.JedisPoolConfig;

@Configuration
@EnableCaching
@EnableConfigurationProperties
public class RedisConfiguration {

	@Autowired
    protected MyRedisProperties myRedisProperties;
	
	@Bean
    public RedisConnectionFactory redisConnectionFactory()
            throws UnknownHostException {
	    JedisPoolConfig config = jedisPoolConfig();
        
	    RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
	    sentinelConfig.master(this.myRedisProperties.getSentinel().getMaster());
	    sentinelConfig.setSentinels(createSentinels(this.myRedisProperties.getSentinel().getNodes()));
//	    sentinelConfig.master("mymaster");
//	    sentinelConfig.setSentinels(createSentinels("127.0.0.1:36379,127.0.0.1:26379"));
	    JedisConnectionFactory factory = new JedisConnectionFactory(sentinelConfig, config);
	    
        return applyProperties(factory);
    }
	
	protected final JedisConnectionFactory applyProperties(
            JedisConnectionFactory factory) {
        factory.setHostName(this.myRedisProperties.getHost());
        factory.setPort(this.myRedisProperties.getPort());
        if (this.myRedisProperties.getPassword() != null) {
            factory.setPassword(this.myRedisProperties.getPassword());
        }
        factory.setDatabase(this.myRedisProperties.getDatabase());
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

    private JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        MyRedisProperties.Pool props = this.myRedisProperties.getPool();
        config.setMaxTotal(props.getMaxActive());
        config.setMaxIdle(props.getMaxIdle());
        config.setMinIdle(props.getMinIdle());
        config.setMaxWaitMillis(props.getMaxWait());
        return config;
    }

}
