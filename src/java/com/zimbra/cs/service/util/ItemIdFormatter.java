/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.util;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.ZimbraSoapContext;

public class ItemIdFormatter {
    private boolean mUnqualifiedItemIds;
    private String mAuthenticatedId;
    private String mDefaultId;

    public ItemIdFormatter() {
        this((String) null, (String) null, false);
    }

    public ItemIdFormatter(String authId) {
        this (authId, authId, false);
    }

    public ItemIdFormatter(boolean noqualify) {
        this((String) null, (String) null, noqualify);
    }

    public ItemIdFormatter(ZimbraSoapContext zsc) {
        this(zsc.getAuthtokenAccountId(), zsc.getRequestedAccountId(),
                zsc.wantsUnqualifiedIds());
    }

    public ItemIdFormatter(Account authAcct, Account defaultAcct, boolean noqualify) {
        this(authAcct == null ? null : authAcct.getId(),
                defaultAcct == null ? null : defaultAcct.getId(), noqualify);
    }

    public ItemIdFormatter(Account authAcct, Mailbox mbox, boolean noqualify) {
        this(authAcct == null ? null : authAcct.getId(),
                mbox.getAccountId(), noqualify);
    }

    public ItemIdFormatter(String authId, Mailbox mbox, boolean noqualify) {
        this(authId, mbox.getAccountId(), noqualify);
    }

    public ItemIdFormatter(String authId, String defaultId, boolean noqualify) {
        mAuthenticatedId = authId;
        mDefaultId = defaultId == null ? authId : defaultId;
        mUnqualifiedItemIds = noqualify;
    }

    public String getAuthenticatedId() {
        return mAuthenticatedId;
    }

    public String getDefaultAccountId() {
        return mDefaultId;
    }

    /**
     * Formats the {@link MailItem}'s ID into a <code>String</code> that's
     * addressable by the request's originator.  In other words, if the owner
     * of the item matches the auth token's principal, you just get a bare
     * ID.  But if the owners don't match, you get a formatted ID that refers
     * to the correct <code>Mailbox</code> as well as the item in question.
     *
     * @param item  The item whose ID we want to encode.
     * @see ItemId
     */
    public String formatItemId(MailItem item) {
        return mUnqualifiedItemIds ? formatItemId(item.getId()) :
            new ItemId(item).toString(this);
    }

    /**
     * Formats the ({@link MailItem}'s ID, subpart ID) pair into a
     * <code>String</code> that's addressable by the request's originator.
     * In other words, if the owner of the item matches the auth token's
     * principal, you just get a bare ID.  But if the owners don't match,
     * you get a formatted ID that refers to the correct <code>Mailbox</code>
     * as well as the item in question.
     *
     * @param item   The item whose ID we want to encode.
     * @param subId  The subpart's ID.
     * @see ItemId
     */
    public String formatItemId(MailItem item, int subId) {
        return mUnqualifiedItemIds ? formatItemId(item.getId(), subId) :
            new ItemId(item, subId).toString(this);
    }

    /**
     * Formats the item ID in the requested <code>Mailbox</code> into a
     * <code>String</code> that's addressable by the request's originator.
     * In other words, if the owner of the <code>Mailbox</code> matches the
     * auth token's principal, you just get a bare ID.  But if the owners
     * don't match, you get a formatted ID that refers to the correct
     * <code>Mailbox</code> as well as the item in question.
     *
     * @param itemId  The item's (local) ID.
     * @see ItemId
     */
    public String formatItemId(int itemId) {
        return new ItemId(mUnqualifiedItemIds ? null : mDefaultId,
                itemId).toString(this);
    }

    /**
     * Formats the (item ID, subpart ID) pair in the requested account's
     * <code>Mailbox</code> into a <code>String</code> that's addressable
     * by the request's originator.  In other words, if the owner of the
     * <code>Mailbox</code> matches the auth token's principal, you just
     * get a bare ID.  But if the owners don't match, you get a formatted
     * ID that refers to the correct <code>Mailbox</code> as well as the
     * item in question.
     *
     * @param itemId  The item's (local) ID.
     * @param subId   The subpart's ID.
     * @see ItemId
     */
    public String formatItemId(int itemId, int subId) {
        return new ItemId(mUnqualifiedItemIds ? null : mDefaultId,
                itemId, subId).toString(this);
    }

    /**
     * Formats the item ID into a <code>String</code> that's addressable by
     * the request's originator.  In other words, if the owner of the item
     * ID matches the auth token's principal, you just get a bare ID.  But if
     * the owners don't match, you get a formatted ID that refers to the
     * correct <code>Mailbox</code> as well as the item in question.
     *
     * @param iid  The item's account, item, and subpart IDs.
     * @see ItemId
     */
    public String formatItemId(ItemId iid) {
        if (iid == null)
            return null;
        return mUnqualifiedItemIds ?
                formatItemId(iid.getId(), iid.getSubpartId()) :
                    iid.toString(this);
    }
}
