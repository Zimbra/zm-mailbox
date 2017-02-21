/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.client.ToZJSONObject;
import com.zimbra.client.ZEmailAddress;
import com.zimbra.client.ZJSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ZConversationSummaryEvent implements ToZJSONObject {

    protected Element mConvEl;

    public ZConversationSummaryEvent(Element e) throws ServiceException {
        mConvEl = e;
    }

    /**
     * @return conversation id
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public String getId() throws ServiceException {
        return mConvEl.getAttribute(MailConstants.A_ID);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new flags or default value if flags didn't change
     */
    public String getFlags(String defaultValue) {
        return mConvEl.getAttribute(MailConstants.A_FLAGS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new tags or default value if tags didn't change
     */
    public String getTagIds(String defaultValue) {
        return mConvEl.getAttribute(MailConstants.A_TAGS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new message count, or defaultValue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public int getMessageCount(int defaultValue) throws ServiceException {
        return (int) mConvEl.getAttributeLong(MailConstants.A_NUM, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new subject or defaultValue if unchanged
     */
    public String getSubject(String defaultValue) {
        Element sub = mConvEl.getOptionalElement(MailConstants.E_SUBJECT);
        return sub == null ? defaultValue : sub.getText();
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new fragment or defaultValue if unchanged
     */
    public String getFragment(String defaultValue) {
        Element frag = mConvEl.getOptionalElement(MailConstants.E_FRAG);
        return frag == null ? defaultValue : frag.getText();
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new date or defaultValue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public long getDate(long defaultValue) throws ServiceException {
        return mConvEl.getAttributeLong(MailConstants.A_DATE, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return grants or defaultValue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public List<ZEmailAddress> getRecipients(List<ZEmailAddress> defaultValue) throws ServiceException {
        List<ZEmailAddress> result  = null;
        for (Element emailEl: mConvEl.listElements(MailConstants.E_EMAIL)) {
            if (result == null) result = new ArrayList<ZEmailAddress>();
            result.add(new ZEmailAddress(emailEl));
        }
        return result == null ? defaultValue : result;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        try {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("id", getId());
            if (getFlags(null) != null) zjo.put("flags", getFlags(null));
            if (getTagIds(null) != null) zjo.put("tags", getTagIds(null));
            if (getSubject(null) != null) zjo.put("subject", getSubject(null));
            if (getFragment(null) != null) zjo.put("fragment", getFragment(null));
            if (getMessageCount(-1) != -1) zjo.put("messageCount", getMessageCount(-1));
            if (getDate(-1) != -1) zjo.put("date", getDate(-1));
            List<ZEmailAddress> addrs = getRecipients(null);
            if (addrs != null) zjo.put("recipients", addrs);
            return zjo;
        } catch (ServiceException se) {
            throw new JSONException(se);
        }
    }

    public String toString() {
        try {
            return String.format("[ZConversationSummaryEvent %s]", getId());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
