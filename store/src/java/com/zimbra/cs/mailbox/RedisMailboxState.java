package com.zimbra.cs.mailbox;

import java.util.Collections;
import java.util.Set;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;

public class RedisMailboxState extends MailboxState {

    private RedissonClient client;
    private RMap<String, Object> redisHash;

    public RedisMailboxState(MailboxData data) {
        super(data);
    }

    @Override
    protected void init() {
        client = RedissonClientHolder.getInstance().getRedissonClient();
        redisHash = client.getMap(data.accountId);
        super.init();
    }
    @Override
    protected SynchronizedField<?> initField(MailboxField field) {
        switch(field.getType()) {
        case BOOL:
            return new RedisField<Boolean>(field);
        case INT:
            return new RedisField<Integer>(field);
        case LONG:
            return new RedisField<Long>(field);
        case SET:
            return new RedisField<Set<String>>(field);
        case STRING:
        default:
            return new RedisField<String>(field);
        }
    }

    private class RedisField<T> implements MailboxState.SynchronizedField<T> {

        private String hashKey;
        private MailboxField field;

        public RedisField(MailboxField field) {
            this.field = field;
            this.hashKey = field.name();
        }

        @Override
        public T value() {
            T val = (T) redisHash.get(hashKey);
            ZimbraLog.mailbox.debug("got %s=%s from redis for mailbox %s", hashKey, val, data.accountId);
            return val;
        }

        @Override
        public void set(T val) {
            redisHash.put(hashKey, val);
            ZimbraLog.mailbox.debug("set %s=%s for mailbox %s", hashKey, val, data.accountId);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T setIfNotExists(T value) {
            T prevValue = (T) redisHash.putIfAbsent(hashKey, value);
            if (prevValue == null) {
                ZimbraLog.mailbox.debug("set %s=%s for account %s", hashKey, value, data.accountId);
                return value;
            } else {
                ZimbraLog.mailbox.debug("%s already set to %s for account %s", hashKey, prevValue, data.accountId);
                return prevValue;
            }
        }
    }

    public static class Factory implements MailboxState.Factory {

        @Override
        public MailboxState getMailboxState(MailboxData data) {
            return new RedisMailboxState(data);
        }

    }
}
