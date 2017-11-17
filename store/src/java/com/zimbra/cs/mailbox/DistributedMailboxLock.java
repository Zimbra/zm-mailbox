package com.zimbra.cs.mailbox;

import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class DistributedMailboxLock {

    private Config config;
    private RedissonClient redisson;
    private RedissonRedLock lock;


    public DistributedMailboxLock(){
        config = new Config();
        config.useSingleServer()
                .setAddress("192.168.99.100:6379");

        redisson = Redisson.create(config);

    }

    public RedissonClient getRedissonInstance(){

        return redisson;
    }


}
