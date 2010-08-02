/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.mailbox.MailItem;


/**
 * A {@link ZimbraHit} which is being proxied from another server: i.e. we did
 * a SOAP request somewhere else and are now wrapping results we got from request.
 *
 * @since Mar 28, 2005
 * @author tim
 */
public class ProxiedHit extends ZimbraHit  {
    protected long mProxiedDate = -1;
    protected int mProxiedConvId = -1;
    protected int mProxiedMsgId = -1;
    protected byte mProxiedItemType = -1;
    protected String mProxiedSubject = null;
    protected String mProxiedName = null;
    protected ItemId itemID = null;

    protected Element mElement;

    public ProxiedHit(ProxiedQueryResults results, Element elt) {
        super(results, null, 0.0f);
        mElement = elt;
    }

    @Override
    public ItemId getParsedItemID() throws ServiceException {
        if (itemID == null) {
            itemID = new ItemId(mElement.getAttribute(MailConstants.A_ID),
                    (String) null);
        }
        return itemID;
    }

    @Override
    long getSize() throws ServiceException {
        return (int) mElement.getAttributeLong(MailConstants.A_SIZE, 0);
    }

    @Override
    long getDate() throws ServiceException {
        if (mProxiedDate < 0) {
            mProxiedDate = mElement.getAttributeLong(MailConstants.A_DATE, 0);
            if (mProxiedDate == 0) {
                mProxiedDate = mElement.getAttributeLong(
                        MailConstants.A_SORT_FIELD, 0);
            }
        }
        return mProxiedDate;
    }

    @Override
    int getConversationId() throws ServiceException {
        if (mProxiedConvId <= 0) {
            mProxiedConvId = (int) mElement.getAttributeLong(
                    MailConstants.A_CONV_ID, 0);
        }
        return mProxiedConvId;
    }

    @Override
    public MailItem getMailItem() {
        return null;
    }

    @Override
    public int getItemId() throws ServiceException {
        if (mProxiedMsgId <= 0) {
            ItemId id = getParsedItemID();
            mProxiedMsgId = id.getId();
        }
        return mProxiedMsgId;
    }

    byte getItemType() throws ServiceException {
        if (mProxiedItemType <= 0) {
            mProxiedItemType = (byte) mElement.getAttributeLong(
                    MailConstants.A_ITEM_TYPE);
        }
        return mProxiedItemType;
    }

    @Override
    void setItem(MailItem item) {
        assert(false); // can't preload a proxied hit!
    }

    @Override
    boolean itemIsLoaded() {
        return true;
    }

    @Override
    String getSubject() throws ServiceException {
        if (mProxiedSubject == null) {
            mProxiedSubject = mElement.getAttribute(MailConstants.E_SUBJECT, null);
            if (mProxiedSubject == null) {
                mProxiedSubject = mElement.getAttribute(MailConstants.A_SORT_FIELD);
            }
        }
        return mProxiedSubject;
    }

    String getFragment() {
        Element frag = mElement.getOptionalElement(MailConstants.E_FRAG);
        if (frag != null) {
            return frag.getText();
        }
        return "";
    }

    @Override
    String getName() throws ServiceException {
        if (mProxiedName == null) {
            mProxiedName = mElement.getAttribute(MailConstants.A_SORT_FIELD);
        }
        return mProxiedName;
    }

    @Override
    public String toString() {
        return mElement.toString();
    }

    public String getServer() {
        return ((ProxiedQueryResults) getResults()).getServer();
    }

    public Element getElement() {
        return mElement;
    }

    @Override
    boolean isLocal() {
        return false;
    }

}
