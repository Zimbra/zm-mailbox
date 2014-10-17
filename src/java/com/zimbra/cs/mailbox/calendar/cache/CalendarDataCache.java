package com.zimbra.cs.mailbox.calendar.cache;

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.Mailbox;

public interface CalendarDataCache {

    public CalendarData get(Key key) throws ServiceException;

    public void put(Key key, CalendarData value) throws ServiceException;

    public void remove(Key key) throws ServiceException;

    public void remove(Set<Key> keys) throws ServiceException;

    public void remove(Mailbox mbox) throws ServiceException;


    public static class Key extends Pair<String,Integer> {
        public Key(String accountId, Integer folderId) {
            super(accountId, folderId);
        }
        public String getAccountId() {
            return getFirst();
        }
        public Integer getFolderId() {
            return getSecond();
        }
    }
}