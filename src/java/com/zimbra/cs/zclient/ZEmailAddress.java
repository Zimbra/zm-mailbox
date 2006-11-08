/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ZEmailAddress {
    
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

    static ZEmailAddress getAddress(Element e, Map<String, ZEmailAddress> cache) throws ServiceException {
        ZEmailAddress addr;

        String id = e.getAttribute(MailService.A_ID, null);
        String ref = e.getAttribute(MailService.A_REF, null);
        if (ref != null && cache != null) {
            return cache.get(ref);
        }
        addr = new ZEmailAddress(
                e.getAttribute(MailService.A_ADDRESS, null),
                e.getAttribute(MailService.A_DISPLAY, null),
                e.getAttribute(MailService.A_PERSONAL, null),
                e.getAttribute(MailService.A_TYPE, ""));
        
        if (cache != null && id != null) {
            cache.put(id, addr);
        }
        return addr;
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

    public String getFullAddress() {
        try {
            return new InternetAddress(mAddress, mPersonal == null ? "" : mPersonal).toString();
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
    
    ZSoapSB toString(ZSoapSB sb) {
        sb.beginStruct();
        sb.add("address", mAddress);
        sb.add("display", mDisplay);
        sb.add("personal", mPersonal);
        sb.add("type", mType);
        sb.endStruct();
        return sb;
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
    
    public String toString() {
        return toString(new ZSoapSB()).toString();
    }
}
