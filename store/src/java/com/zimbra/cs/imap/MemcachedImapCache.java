/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.common.io.Closeables;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.cs.io.SecureObjectInputStream;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

/**
 * IMAP cache using memcached.
 *
 * @author ysasaki
 */
final class MemcachedImapCache implements ImapSessionManager.Cache<String, ImapFolder> {

    private final MemcachedMap<ImapMemcachedKey, ImapFolder> map = new MemcachedMap<ImapMemcachedKey, ImapFolder>(
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
        private Set<String> validClassNames;

        public ImapMemcachedSerializer() {
            this.validClassNames = new HashSet<String>();
            this.validClassNames.add(ImapFolder.class.getName());
            this.validClassNames.add(ItemIdentifier.class.getName());
            this.validClassNames.add(ImapMessage.class.getName());
            this.validClassNames.add(ImapFlagCache.class.getName());
            this.validClassNames.add(MailItem.Type.class.getName());
        }

        @Override
        public Object serialize(ImapFolder folder) throws ServiceException {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try (ObjectOutputStream oout = new ObjectOutputStream(bout)) {
                oout.writeObject(folder);
            } catch (Exception e) {
                throw ServiceException.FAILURE("Failed to serialize ImapFolder", e);
            }
            return bout.toByteArray();
        }

        @Override
        public ImapFolder deserialize(Object obj) throws ServiceException {
            ObjectInputStream in = null;

            try {
                in = new SecureObjectInputStream(new ByteArrayInputStream((byte[]) obj), this.validClassNames);
                return (ImapFolder) in.readObject();
            } catch (Exception e) {
                throw ServiceException.FAILURE("Failed to deserialize ImapFolder", e);
            } finally {
                Closeables.closeQuietly(in);
            }
        }

    }

}
