/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.index;

import com.zimbra.cs.mailbox.MailItem;

/**
 * Mock implementation of {@link ZimbraHit} for testing.
 *
 * @author ysasaki
 */
public class MockHit extends ZimbraHit {
    private int id;
    private long date;
    private long size;
    private int convId;
    private MailItem mailItem;
    private String subject;
    private String name;

    public MockHit(int id, String name) {
        super(null, null, 0);
        this.id = id;
        this.name = name;
    }

    @Override
    public int getItemId() {
        return id;
    }

    public void setItemId(int value) {
        id = value;
    }

    @Override
    long getDate() {
        return date;
    }

    public void setDate(long value) {
        date = value;
    }

    @Override
    long getSize() {
        return size;
    }

    public void setSize(long value) {
        size = value;
    }

    @Override
    int getConversationId() {
        return convId;
    }

    void setConversationId(int value) {
        convId = value;
    }

    @Override
    public MailItem getMailItem() {
        return mailItem;
    }

    @Override
    void setItem(MailItem value) {
        mailItem = value;
    }

    @Override
    boolean itemIsLoaded() {
        return mailItem != null;
    }

    @Override
    String getSubject() {
        return subject;
    }

    void setSubject(String value) {
        subject = value;
    }

    @Override
    String getName() {
        return name;
    }

    void setName(String value) {
        name = value;
    }

}
