package com.zimbra.cs.mailbox.redis;

import java.util.concurrent.locks.ReadWriteLock;

import org.redisson.RedissonShutdownException;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.RedisResponseTimeoutException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.stats.ActivityTracker;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.RetryUtil.RequestWithRetry;
import com.zimbra.cs.util.RetryUtil.RequestWithRetry.Command;

public abstract class RedissonRetryDecorator<R> {

    protected RequestWithRetry.OnFailureAction onFailure;
    protected RequestWithRetry.ExceptionHandler exceptionHandler;
    protected RedissonRetryClient client;
    protected R redissonObject;
    private RedissonInitializer<R> initializer;
    protected int clientVersion;
    private ActivityTracker tracker = ZimbraPerf.REDIS_TRACKER;
    private ReadWriteLock clientLock;

    public RedissonRetryDecorator(RedissonInitializer<R> initializer, RedissonRetryClient client) {
        this.client = client;
        this.clientVersion = client.getClientVersion();
        this.initializer = initializer;
        this.clientLock = client.getClientLock();
        initialize();

        onFailure = new RequestWithRetry.OnFailureAction() {
            @Override
            public void run() throws Exception {
                reconnect(clientVersion);
            }
        };

        exceptionHandler = new RequestWithRetry.ExceptionHandler() {

            @Override
            public synchronized boolean exceptionMatches(Exception e) {
                boolean isRedisException = false;
                if (e instanceof RedisException && !(e instanceof RedissonShutdownException) && !(e instanceof RedisResponseTimeoutException)) {
                    ZimbraLog.mailbox.debug("caught %s in ExceptionHandler, will attempt to re-initialize redisson client", e.getClass().getName(), e);
                    isRedisException = true;
                } else {
                    ZimbraLog.mailbox.warn("caught %s in ExceptionHandler, but it is not the right exception type", e.getClass().getName(), e);
                }
                return isRedisException;
            }
        };
    }

    protected void initialize() {
        redissonObject = client.runInitializer(initializer);
    }

    private synchronized void reconnect(int clientVersionAtFailure) {
        clientVersion = client.restart(clientVersionAtFailure);
        initialize();
    }

    protected void checkClientVersion() {
        if (clientVersion != client.getClientVersion()) {
            ZimbraLog.mailbox.info("detected old client version (%d < %d), re-initializing %s", clientVersion, client.getClientVersion(), this.getClass().getSimpleName());
            initialize();
            clientVersion = client.getClientVersion();
        }
    }

    protected <T> T runCommand(Command<T> command) {
        long startTime = System.currentTimeMillis();
        Command<T> wrapped = new WrappedCommand<>(command, clientLock, this);
        RequestWithRetry<T> withRetry = new RequestWithRetry<>(wrapped, exceptionHandler, onFailure);
        try {
            T result = withRetry.execute();
            String caller = Thread.currentThread().getStackTrace()[2].getMethodName();
            tracker.addStat(caller, startTime);
            return result;
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

    protected static class WrappedCommand<T> implements Command<T> {

        private Command<T> command;
        private ReadWriteLock lock;
        private RedissonRetryDecorator<?> object;

        public WrappedCommand(Command<T> command, ReadWriteLock lock, RedissonRetryDecorator<?> object) {
            this.command = command;
            this.lock = lock;
            this.object = object;
        }

        @Override
        public T execute() throws Exception {
            lock.readLock().lock();
            object.checkClientVersion();
            try {
                return command.execute();
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
