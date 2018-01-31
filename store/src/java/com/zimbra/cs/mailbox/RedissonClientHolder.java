package com.zimbra.cs.mailbox;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public final class RedissonClientHolder {
	private final Config config;
	private final RedissonClient redisson;
	private final static String HOST = "redis";
	private final static String PORT = "6379";

	private static class InstanceHolder {
        public static RedissonClientHolder instance = new RedissonClientHolder();
    }

	private RedissonClientHolder() {
		this.config = new Config();
		this.config.useSingleServer().setAddress("redis://" + HOST + ":" + PORT);
		this.redisson = Redisson.create(this.config);
	}

	public static RedissonClientHolder getInstance() {
		return  InstanceHolder.instance;
	}

	public RedissonClient getRedissonClient() {
		return redisson;
	}
}
