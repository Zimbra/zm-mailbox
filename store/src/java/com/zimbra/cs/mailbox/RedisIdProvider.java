package com.zimbra.cs.mailbox;

import java.util.Collections;
import java.util.List;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RScript;
import org.redisson.api.RScript.Mode;
import org.redisson.api.RScript.ReturnType;
import org.redisson.api.RedissonClient;

import com.zimbra.common.util.ZimbraLog;

public class RedisIdProvider extends IdProvider {

    private static final String ITEM_ID_LABEL = "itemId";
    private static final String SEARCH_ID_LABEL = "searchId";
    private static final String CHANGE_ID_LABEL = "changeId";

    private RedissonClient client;

    public RedisIdProvider(Mailbox mailbox) {
        super(mailbox);
    }

    @Override
    protected void init() {
        client = RedissonClientHolder.getInstance().getRedissonClient();
        super.init();
    }

    @Override
    protected SynchronizedId initId(IdType idType) {
        switch(idType) {
        case ITEM_ID:
            return new RedisId(client, ITEM_ID_LABEL);
        case SEARCH_ID:
            return new RedisId(client, SEARCH_ID_LABEL);
        case CHANGE_ID:
            return new RedisId(client, CHANGE_ID_LABEL);
        default:
            return null;
        }
    }

    private class RedisId implements IdProvider.SynchronizedId {
        private RAtomicLong value;
        private String label;
        private String key;

        private RedisId(RedissonClient client, String label) {
            this.label = label;
            this.key = String.format("%s_%s", mbox.getAccountId(), label);
            this.value = client.getAtomicLong(key);
        }

        @Override
        public int value() {
            int idValue = (int) value.get();
            ZimbraLog.mailbox.debug("got last %s=%d from redis for mailbox %s", label, idValue, mbox.getAccountId());
            return idValue;
        }

        @Override
        public void increment(int delta) {
            if (delta > 0) {
                int newValue = (int) value.addAndGet(delta);
                ZimbraLog.mailbox.debug("incremented %s for mailbox %s by %d, new value is %s", label, mbox.getAccountId(), delta, newValue);
            }
        }

        @Override
        public int setIfNotExists(int value) {
            String luaScript =
                    "redis.call('setnx', KEYS[1], ARGV[1]); "
                    + "return redis.call('get', KEYS[1]); ";
            List<Object> keys = Collections.<Object>singletonList(key);
            RScript script = client.getScript();
            int retVal = (int) script.eval(Mode.READ_WRITE, luaScript, ReturnType.INTEGER, keys, value);
            if (retVal == value) {
                ZimbraLog.mailbox.debug("set %s for account %s to %s", label, mbox.getAccountId(), retVal);
            } else {
                ZimbraLog.mailbox.debug("%s for account %s already has value %s", label, mbox.getAccountId(), retVal);
            }
            return retVal;
        }
    }

    public static class Factory implements IdProvider.Factory {

        @Override
        public IdProvider getIdProvider(Mailbox mbox) {
            return new RedisIdProvider(mbox);
        }
    }
}