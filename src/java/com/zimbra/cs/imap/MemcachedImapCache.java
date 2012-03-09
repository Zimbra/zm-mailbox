/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.common.io.Closeables;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

/**
 * IMAP cache using memcached.
 *
 * @author ysasaki
 */
final class MemcachedImapCache implements ImapSessionManager.Cache {

    private MemcachedMap<ImapMemcachedKey, ImapFolder> map = new MemcachedMap<ImapMemcachedKey, ImapFolder>(
            MemcachedConnector.getClient(), new ImapMemcachedSerializer());

    @Override
    public void put(String key, ImapFolder value) {
        try {
            synchronized (value) {
                map.put(new ImapMemcachedKey(key), value);
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("Failed to store into cache", e);
        }
    }

    @Override
    public ImapFolder get(String key) {
        try {
            return map.get(new ImapMemcachedKey(key));
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("Failed to load from cache", e);
            return null;
        }
    }

    @Override
    public void remove(String key) {
        try {
            map.remove(new ImapMemcachedKey(key));
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("Failed to remove from cache", e);
        }
    }

    @Override
    public void updateAccessTime(String key) {
    }

    private static final class ImapMemcachedKey implements MemcachedKey {
        private final String key;

        ImapMemcachedKey(String key) {
            this.key = key;
        }

        @Override
        public String getKeyPrefix() {
            return MemcachedKeyPrefix.IMAP;
        }

        @Override
        public String getKeyValue() {
            return key;
        }
    }

    private static final class ImapMemcachedSerializer implements MemcachedSerializer<ImapFolder> {
        ImapMemcachedSerializer()  { }

        @Override
        public Object serialize(ImapFolder folder) throws ServiceException {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = null;
            try {
                oout = new ObjectOutputStream(bout);
                oout.writeObject(folder);
            } catch (Exception e) {
                throw ServiceException.FAILURE("Failed to serialize ImapFolder", e);
            } finally {
                Closeables.closeQuietly(oout);
            }
            return bout.toByteArray();
        }

        @Override
        public ImapFolder deserialize(Object obj) throws ServiceException {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new ByteArrayInputStream((byte[]) obj));
                return (ImapFolder) in.readObject();
            } catch (Exception e) {
                throw ServiceException.FAILURE("Failed to deserialize ImapFolder", e);
            } finally {
                Closeables.closeQuietly(in);
            }
        }

    }

}
