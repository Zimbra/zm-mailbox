package com.zimbra.cs.mailbox.redis;

import org.redisson.RedissonShutdownException;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.RedisTimeoutException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.RetryUtil.RequestWithRetry;
import com.zimbra.cs.util.RetryUtil.RequestWithRetry.Command;

public abstract class RedissonRetryDecorator<R> {

    protected RequestWithRetry.OnFailureAction onFailure;
    protected RequestWithRetry.ExceptionHandler exceptionHandler;
    protected RedissonRetryClient client;
    protected R redissonObject;
    private RedissonInitializer<R> initializer;
    protected int clientVersion;

    public RedissonRetryDecorator(RedissonInitializer<R> initializer, RedissonRetryClient client) {
        this.client = client;
        this.clientVersion = client.getClientVersion();
        this.initializer = initializer;
        initialize();

        onFailure = new RequestWithRetry.OnFailureAction() {
            @Override
            public void run() throws Exception {
                ZimbraLog.mailbox.info("detected redis failure (version %d), will re-initialize redisson client", clientVersion);
                reconnect(clientVersion);
            }
        };

        exceptionHandler = new RequestWithRetry.ExceptionHandler() {

            @Override
            public synchronized boolean exceptionMatches(Exception e) {
                boolean isRedisException = false;
                if (e instanceof RedisException && !(e instanceof RedissonShutdownException) && !(e instanceof RedisTimeoutException)) {
                    ZimbraLog.mailbox.warn("caught %s in ExceptionHandler, will attempt to re-initialize redisson client", e.getClass().getName(), e);
                    isRedisException = true;
                } else {
                    ZimbraLog.mailbox.warn("caught %s in ExceptionHandler, but it is not the right exception type", e.getClass().getName(), e);
                }
                return isRedisException;
            }
        };
    }

    private void initialize() {
        redissonObject = client.runInitializer(initializer);
    }

    private synchronized void reconnect(int clientVersionAtFailure) {
        if (clientVersionAtFailure < client.getClientVersion()) {
            //another thread already re-initialized the redisson client
            return;
        }
        client.restart();
        clientVersion = client.getClientVersion();
        initialize();
    }

    protected void checkClientVersion() {
        if (clientVersion != client.getClientVersion()) {
            ZimbraLog.mailbox.debug("detected old client version (%d < %d), re-initializing decorated object", clientVersion, client.getClientVersion());
            initialize();
            clientVersion = client.getClientVersion();
        }
    }

    protected <T> T runCommand(Command<T> command) {
        checkClientVersion();
        RequestWithRetry<T> withRetry = new RequestWithRetry<>(command, exceptionHandler, onFailure);
        try {
            return withRetry.execute();
        } catch (ServiceException e) {
            if (e.getCause() instanceof RedisException) {
                //re-throw underlying exception, since RedisException is unchecked
                throw (RedisException) e.getCause();
            } else {
                //wrap in a redis exception, since we need an unchecked exception
                throw new RedisException("non-redis exception encountered running a redis request with retry", e.getCause());
            }
        }
    }

    protected interface RedissonInitializer<R> {
        public R init(RedissonClient client);
    }
}
