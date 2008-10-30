/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.zclient.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.ToZJSONObject;
import com.zimbra.cs.zclient.ZEmailAddress;
import com.zimbra.cs.zclient.ZItem;
import com.zimbra.cs.zclient.ZJSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ZCreateMessageEvent implements ZCreateItemEvent, ToZJSONObject {

    protected Element mMessageEl;

    public ZCreateMessageEvent(Element e) throws ServiceException {
        mMessageEl = e;
    }

    /**
     * @return id
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public String getId() throws ServiceException {
        return mMessageEl.getAttribute(MailConstants.A_ID);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new flags or default value if flags didn't change
     */
    public String getFlags(String defaultValue) {
        return mMessageEl.getAttribute(MailConstants.A_FLAGS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new tags or default value if tags didn't change
     */
    public String getTagIds(String defaultValue) {
        return mMessageEl.getAttribute(MailConstants.A_TAGS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new message count, or defaultValue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public int getSize(int defaultValue) throws ServiceException {
        return (int) mMessageEl.getAttributeLong(MailConstants.A_SIZE, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new folder id or defaultValue if unchanged
     */
    public String getFolderId(String defaultValue) {
        return mMessageEl.getAttribute(MailConstants.A_FOLDER, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new conv id or defaultValue if unchanged
     */
    public String getConversationId(String defaultValue) {
        return mMessageEl.getAttribute(MailConstants.A_CONV_ID, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new subject or defaultValue if unchanged
     */
    public String getSubject(String defaultValue) {
        return mMessageEl.getAttribute(MailConstants.E_SUBJECT, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new fragment or defaultValue if unchanged
     */
    public String getFragment(String defaultValue) {
        return mMessageEl.getAttribute(MailConstants.E_FRAG, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new date or defaultValue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public long getReceivedDate(long defaultValue) throws ServiceException {
        return mMessageEl.getAttributeLong(MailConstants.A_DATE, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new date or defaultValue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public long getSentDate(long defaultValue) throws ServiceException {
        return mMessageEl.getAttributeLong(MailConstants.A_SENT_DATE, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return grants or defaultValue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public List<ZEmailAddress> getEmailAddresses(List<ZEmailAddress> defaultValue) throws ServiceException {
        List<ZEmailAddress> result  = null;
        for (Element emailEl: mMessageEl.listElements(MailConstants.E_EMAIL)) {
            if (result == null) result = new ArrayList<ZEmailAddress>();
            result.add(new ZEmailAddress(emailEl));
        }
        return result == null ? defaultValue : result;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        try {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("id", getId());
            if (getFolderId(null) != null) zjo.put("folderId", getFolderId(null));
            if (getConversationId(null) != null) zjo.put("conversationId", getConversationId(null));
            if (getFlags(null) != null) zjo.put("flags", getFlags(null));
            if (getTagIds(null) != null) zjo.put("tags", getTagIds(null));
            if (getSubject(null) != null) zjo.put("subject", getSubject(null));
            if (getFragment(null) != null) zjo.put("fragment", getFragment(null));
            if (getSize(-1) != -1) zjo.put("size", getSize(-1));
            if (getReceivedDate(-1) != -1) zjo.put("receivedDate", getReceivedDate(-1));
            if (getSentDate(-1) != -1) zjo.put("sentDate", getSentDate(-1));
            List<ZEmailAddress> addrs = getEmailAddresses(null);
            if (addrs != null) zjo.put("addresses", addrs);
            return zjo;
        } catch (ServiceException se) {
            throw new JSONException(se);
        }
    }

    public String toString() {
        try {
            return String.format("[ZCreateMessageEvent %s]", getId());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public ZItem getItem() throws ServiceException {
        return null;
    }
}
