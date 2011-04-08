/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
public final class MockHit extends ZimbraHit {
    private int id;
    private int convId;
    private MailItem mailItem;

    public MockHit(ZimbraQueryResultsImpl results, int id, Object sortValue) {
        super(results, null, sortValue);
        this.id = id;
    }

    @Override
    public int getItemId() {
        return id;
    }

    public void setItemId(int value) {
        id = value;
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
    String getName() {
        return (String) sortValue;
    }

}
