package com.github.redisdistributedlock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Resource;
import java.util.Collections;

@SpringBootApplication
public class RedisDistributedLockApplication implements ApplicationRunner {

    @Autowired
    private JedisPool jedisPool;
    @Autowired
    private JedisPoolConfig jedisPoolConfig;


    @Bean
    @ConfigurationProperties("redis")
    public JedisPoolConfig jedisPoolConfig() {
        return new JedisPoolConfig();
    }

    @Bean(destroyMethod = "close")
    public JedisPool jedisPool(@Value("${redis.host}") String host) {
        return new JedisPool(jedisPoolConfig(), host);
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisDistributedLockApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println(lock(jedis, "name", "kim", 10));
            System.out.println(unlock(jedis, "name", "kim"));
        }
    }

    private boolean lock(Jedis jedis, String key, String value, int expireSeconds) {
        String result = jedis.set(key, value, "NX", "EX", expireSeconds);
        if (result.equals("OK")) {
            return true;
        }
        return false;
    }

    private boolean unlock(Jedis jedis, String key, String value) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(luaScript, Collections.singletonList(key), Collections.singletonList(value));
        if (result.equals(1L))
            return true;
        return false;
    }
}