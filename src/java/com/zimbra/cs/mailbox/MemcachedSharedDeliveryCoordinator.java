package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

import javax.annotation.PostConstruct;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.StringBasedMemcachedKey;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class MemcachedSharedDeliveryCoordinator implements SharedDeliveryCoordinator {
    static final int WAIT_MS = 3000;
    @Autowired protected ZimbraMemcachedClient memcachedClient;
    protected MemcachedMap<StringBasedMemcachedKey, State> stateByAccountIdLookup;

    /** Constructor */
    public MemcachedSharedDeliveryCoordinator() {
    }

    @PostConstruct
    public void init() {
        stateByAccountIdLookup = new MemcachedMap<>(memcachedClient, new Serializer(), false);
    }

    protected StringBasedMemcachedKey key(Mailbox mbox) {
        return new StringBasedMemcachedKey(MemcachedKeyPrefix.MBOX_SHARED_DELIVERY_COORD, mbox.getAccountId());
    }

    protected State getState(Mailbox mbox) throws ServiceException {
        return stateByAccountIdLookup.get(key(mbox));
    }

    protected State getOrCreateState(Mailbox mbox) throws ServiceException {
        State state = getState(mbox);
        if (state == null) {
            state = new State();
            stateByAccountIdLookup.put(key(mbox), state);
        }
        return state;
    }

    @Override
    public boolean beginSharedDelivery(Mailbox mbox) throws ServiceException {
        State state = getOrCreateState(mbox);
        assert(state.numDelivs >= 0);
        if (state.allow) {
            state.numDelivs++;
            stateByAccountIdLookup.put(key(mbox), state);
            if (ZimbraLog.mailbox.isDebugEnabled()) {
                ZimbraLog.mailbox.debug("# of shared deliv incr to " + state.numDelivs +
                            " for mailbox " + mbox.getId());
            }
            return true;
        } else {
            // If request for other ops is pending on this mailbox, don't allow
            // any more shared deliveries from starting.
            return false;
        }
    }

    @Override
    public void endSharedDelivery(Mailbox mbox) throws ServiceException {
        State state = getOrCreateState(mbox);

        if (state != null && state.numDelivs > 0) {
            // This is the normal flow
            state.numDelivs--;
            stateByAccountIdLookup.put(key(mbox), state);
            if (ZimbraLog.mailbox.isDebugEnabled()) {
                ZimbraLog.mailbox.debug("# of shared deliv decr to " + state.numDelivs +
                            " for mailbox " + mbox.getId());
            }
        } else {
            ZimbraLog.mailbox.warn("Shared delivery state lost by memcached for mailbox " + mbox.getId());
        }

    }

    @Override
    public boolean isSharedDeliveryAllowed(Mailbox mbox) throws ServiceException {
        State state = getState(mbox);
        return state == null || state.allow;
    }

    @Override
    public void setSharedDeliveryAllowed(Mailbox mbox, boolean allow) throws ServiceException {
        State state = getOrCreateState(mbox);

        // If someone is changing allow from true to false, wait for completion of active shared deliveries
        // before allowing the change
        if (allow == false && state.allow && state.numDelivs > 0) {
            ZimbraLog.mailbox.debug("Waiting for completion of active shared delivery before allowing disabling; mailbox={}", mbox.getId());
            waitUntilSharedDeliveryCompletes(mbox);
        }

        state.allow = allow;
        stateByAccountIdLookup.put(key(mbox), state);
    }

    @Override
    public void waitUntilSharedDeliveryCompletes(Mailbox mbox) throws ServiceException {
        State state = getOrCreateState(mbox);
        while (state.numDelivs > 0) {
            try {
                synchronized (this) {
                    wait(WAIT_MS);
                }
                ZimbraLog.misc.info("wake up from wait for completion of shared delivery; mailbox=" + mbox.getId() +
                            " # of shared deliv=" + state.numDelivs);
            } catch (InterruptedException e) {}
            state = getOrCreateState(mbox);
        }
    }

    @Override
    public boolean isSharedDeliveryComplete(Mailbox mbox) throws ServiceException {
        State state = getState(mbox);
        return state == null || state.numDelivs < 1;
    }


    public static class State {
        public int numDelivs = 0;
        public boolean allow = true;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("numDelivs", numDelivs)
                    .add("allow", allow)
                    .toString();
        }
    }


    private static class Serializer implements MemcachedSerializer<State> {
        private ObjectMapper mapper = new ObjectMapper();

        Serializer() {}

        @Override
        public Object serialize(State value) throws ServiceException {
            try {
                return mapper.writer().writeValueAsString(value);
            } catch (Exception e) {
                throw ServiceException.PARSE_ERROR("failed serializing state for cache", e);
            }
        }

        @Override
        public State deserialize(Object obj) throws ServiceException {
            try {
                return mapper.readValue(obj.toString(), State.class);
            } catch (Exception e) {
                throw ServiceException.PARSE_ERROR("failed deserializing state from cache", e);
            }
        }
    }
}
