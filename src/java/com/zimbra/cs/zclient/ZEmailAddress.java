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

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.MailServiceException;
import org.json.JSONException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ZEmailAddress implements ToZJSONObject {
    
    public static final String EMAIL_TYPE_BCC = "b";
    public static final String EMAIL_TYPE_CC = "c";
    public static final String EMAIL_TYPE_FROM = "f";
    public static final String EMAIL_TYPE_SENDER = "s";
    public static final String EMAIL_TYPE_TO = "t";
    public static final String EMAIL_TYPE_REPLY_TO = "r";
    

    private String mAddress;
    private String mDisplay;
    private String mPersonal;
    private String mType;

    public ZEmailAddress(String address, String display, String personal, String type) {
        mAddress = address;
        mDisplay = display;
        mPersonal = personal;
        mType = type;
    }

    public ZEmailAddress(Element e) throws ServiceException {
        mAddress = e.getAttribute(MailConstants.A_ADDRESS, null);
        mDisplay = e.getAttribute(MailConstants.A_DISPLAY, null);
        mPersonal = e.getAttribute(MailConstants.A_PERSONAL, null);
        mType = e.getAttribute(MailConstants.A_TYPE, "");
    }

    /**
     * (f)rom, t(o), c(c), (s)ender, (r)eply-to, b(cc). Type is only sent when an individual message message is returned. In the
     * list of conversations, all the email addresseses returned for a conversation are a subset
     * of the participants. In the list of messages in a converstation, the email addressses are
     * the senders. 
     */
    public String getType() {
        return mType;
    }

    /**
     * the comment/name part of an address
     */
    public String getPersonal() {
        return mPersonal;
    }
    
    /**
     * the user@domain part of an address
     */
    public String getAddress() {
        return mAddress;
    }
    
    /**
     * if we have personal, first word in "word1 word2" format, or last word in "word1, word2" format.
     * if no personal, take string before "@" in email-address.
     */
    public String getDisplay() {
        return mDisplay;
    }


    private String quoteAddress(String addr) {
        if (addr.startsWith("<"))
            return addr;
        else
            return "<" + addr +">";
    }

    public String getFullAddressQuoted() {
        if (mPersonal == null) {
            return quoteAddress(mAddress);
        } else {
            String p = mPersonal;
            if (p.indexOf("\"") != -1)
                p = p.replaceAll("\"", "\\\"");
            return "\"" + p + "\" "+ quoteAddress(mAddress);
        }
    }

    public String getFullAddress() {
        try {
            if (mPersonal == null)
                return mAddress;
            else
                return new InternetAddress(mAddress, mPersonal).toUnicodeString();
        } catch (UnsupportedEncodingException e) {
            if (mPersonal == null)
                return mAddress;
            else {
                String p = mPersonal;
                if (p.indexOf("\"") != -1)
                    p = p.replaceAll("\"", "\\\"");
                return p + " "+getAddress();
            }
        }
    }
    
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("address", mAddress);
        jo.put("display", mDisplay);
        jo.put("personal", mPersonal);
        jo.put("type", mType);
        return jo;
    }

    public String toString() {
        return ZJSONObject.toString(this);
    }

    /**
    *
    * @param type type of addresses to create in the returned list.
    * @see com.zimbra.cs.zclient.ZEmailAddress EMAIL_TYPE_TO, etc.
    * @return list of ZEMailAddress obejcts.
    * @throws ServiceException
    */
    public static List<ZEmailAddress> parseAddresses(String line, String type) throws ServiceException {
        try {
            line = line.replace(";", ",");
            InternetAddress[] inetAddrs = InternetAddress.parseHeader(line, false);
            List<ZEmailAddress> result = new ArrayList<ZEmailAddress>(inetAddrs.length);
            for (InternetAddress ia : inetAddrs) {
                result.add(new ZEmailAddress(ia.getAddress(), null, ia.getPersonal(), type));
            }
            return result;
        } catch (AddressException e) {
            throw MailServiceException.ADDRESS_PARSE_ERROR(e);
        }
    }
    
}
