/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
 * A {@link ZimbraHit} which is being proxied from another server: i.e. we did a SOAP request somewhere else and are now
 * wrapping results we got from request.
 *
 * @since Mar 28, 2005
 * @author tim
 */
public final class ProxiedHit extends ZimbraHit  {
    private long proxiedDate = -1;
    private int proxiedConvId = -1;
    private int proxiedMsgId = -1;
    private String proxiedSubject;
    private ItemId itemId;
    private final Element element;

    public ProxiedHit(ZimbraQueryResultsImpl results, Element elt, Object sortValue) {
        super(results, null, sortValue);
        element = elt;
    }

    @Override
    public ItemId getParsedItemID() throws ServiceException {
        if (itemId == null) {
            itemId = new ItemId(element.getAttribute(MailConstants.A_ID), (String) null);
        }
        return itemId;
    }

    void setParsedItemId(ItemId value) {
        itemId = value;
    }

    @Override
    long getSize() throws ServiceException {
        return (int) element.getAttributeLong(MailConstants.A_SIZE, 0);
    }

    @Override
    long getDate() throws ServiceException {
        if (proxiedDate < 0) {
            proxiedDate = element.getAttributeLong(MailConstants.A_DATE, 0);
            if (proxiedDate == 0) {
                proxiedDate = element.getAttributeLong(MailConstants.A_SORT_FIELD, 0);
            }
        }
        return proxiedDate;
    }

    @Override
    int getConversationId() throws ServiceException {
        if (proxiedConvId <= 0) {
            proxiedConvId = (int) element.getAttributeLong(MailConstants.A_CONV_ID, 0);
        }
        return proxiedConvId;
    }

    @Override
    public MailItem getMailItem() {
        return null;
    }

    @Override
    public int getItemId() throws ServiceException {
        if (proxiedMsgId <= 0) {
            proxiedMsgId = getParsedItemID().getId();
        }
        return proxiedMsgId;
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
        if (proxiedSubject == null) {
            proxiedSubject = element.getAttribute(MailConstants.E_SUBJECT, null);
            if (proxiedSubject == null) {
                proxiedSubject = element.getAttribute(MailConstants.A_SORT_FIELD);
            }
        }
        return proxiedSubject;
    }

    String getFragment() {
        Element frag = element.getOptionalElement(MailConstants.E_FRAG);
        return frag != null ? frag.getText() : "";
    }

    @Override
    String getName() throws ServiceException {
        return element.getAttribute(MailConstants.A_SORT_FIELD);
    }

    @Override
    public String toString() {
        return element.toString();
    }

    public String getServer() {
        return ((ProxiedQueryResults) getResults()).getServer();
    }

    public Element getElement() {
        return element;
    }

    @Override
    boolean isLocal() {
        return false;
    }

}
