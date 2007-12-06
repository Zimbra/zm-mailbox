/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 28, 2005
 */
package com.zimbra.cs.index;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.mailbox.MailItem;


/**
 * @author tim
 *
 * A ZimbraHit which is being proxied from another server: ie we did a SOAP request
 * somewhere else and are now wrapping results we got from request.
 */
public class ProxiedHit extends ZimbraHit 
{
    protected long mProxiedDate = -1;
    protected int mProxiedConvId = -1;
    protected int mProxiedMsgId = -1;
    protected byte mProxiedItemType = -1;
    protected String mProxiedSubject = null;
    protected String mProxiedName = null;
    protected ItemId itemID = null;

    protected Element mElement;

    @Override public ItemId getParsedItemID() throws ServiceException {
        if (itemID == null)
            itemID = new ItemId(mElement.getAttribute(MailConstants.A_ID), (String) null);
        return itemID;
    }

    public ProxiedHit(ProxiedQueryResults results, Element elt) {
        super(results, null, 0.0f);
        mElement = elt;
    }

    long getSize() throws ServiceException {
        return (int) mElement.getAttributeLong(MailConstants.A_SIZE, 0);
    }

    long getDate() throws ServiceException {
        if (mProxiedDate < 0) {
            mProxiedDate = mElement.getAttributeLong(MailConstants.A_DATE, 0);
            if (mProxiedDate == 0) {
                mProxiedDate = mElement.getAttributeLong(MailConstants.A_SORT_FIELD, 0);
            }
        }
        return mProxiedDate;
    }

    int getConversationId() throws ServiceException {
        if (mProxiedConvId <= 0) {
            mProxiedConvId = (int) mElement.getAttributeLong(MailConstants.A_CONV_ID, 0);
        }
        return mProxiedConvId;
    }

    public MailItem getMailItem() { return null; }

    public int getItemId() throws ServiceException {
        if (mProxiedMsgId <= 0) {
            ItemId id = getParsedItemID();
            mProxiedMsgId = id.getId();
        }
        return mProxiedMsgId;
    }

    byte getItemType() throws ServiceException {
        if (mProxiedItemType <= 0) {
            mProxiedItemType = (byte) mElement.getAttributeLong(MailConstants.A_ITEM_TYPE);
        }
        return mProxiedItemType;
    }

    void setItem(MailItem item) {
        assert(false); // can't preload a proxied hit!
    }

    boolean itemIsLoaded() {
        return true;
    }


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

    String getName() throws ServiceException {
        if (mProxiedName == null) {
            mProxiedName = mElement.getAttribute(MailConstants.A_SORT_FIELD);
        }
        return mProxiedName;
    }

    public String toString() {
        return mElement.toString();
    }

    public String getServer() {
        ProxiedQueryResults res = (ProxiedQueryResults) getResults();
        return res.getServer();
    }

    public Element getElement() { 
        return mElement;
    }
    
    boolean isLocal() {
        return false;
    }
    

}
