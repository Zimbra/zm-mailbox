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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.StringUtil;

public abstract class CalendarUser {

    protected static final String FN_ADDRESS  = "a";
    protected static final String FN_CN       = "cn";
    protected static final String FN_SENTBY   = "sentby";
    protected static final String FN_DIR      = "dir";
    protected static final String FN_LANGUAGE = "lang";

    private String mAddress;
    private String mCn;
    private String mSentBy;
    private String mDir;
    private String mLanguage;

    public String getAddress() { return mAddress; }
    public void setAddress(String a) { mAddress = getMailToAddress(a); }

    public boolean hasCn() { return !StringUtil.isNullOrEmpty(mCn); }
    public String getCn() { return mCn; }
    public void setCn(String cn) { mCn = cn; }

    public boolean hasSentBy() { return !StringUtil.isNullOrEmpty(mSentBy); }
    public String getSentBy() { return mSentBy; }
    public void setSentBy(String sb) { mSentBy = getMailToAddress(sb); }

    public boolean hasDir() { return !StringUtil.isNullOrEmpty(mDir); }
    public String getDir() { return mDir; }
    public void setDir(String d) { mDir = d; }

    public boolean hasLanguage() { return !StringUtil.isNullOrEmpty(mLanguage); }
    public String getLanguage() { return mLanguage; }
    public void setLanguage(String lang) { mLanguage = lang; }

    
    public CalendarUser(String address,
                           String cn,
                           String sentBy,
                           String dir,
                           String language) {
        setAddress(address);
        setCn(cn);
        setSentBy(sentBy);
        setDir(dir);
        setLanguage(language);
    }

    public CalendarUser(ZProperty prop) {
        this(prop.getValue(),
             prop.paramVal(ICalTok.CN, null),
             prop.paramVal(ICalTok.SENT_BY, null),
             prop.paramVal(ICalTok.DIR, null),
             prop.paramVal(ICalTok.LANGUAGE, null));
    }

    public CalendarUser(Metadata meta) {
        this(meta.get(FN_ADDRESS, null),
             meta.get(FN_CN, null),
             meta.get(FN_SENTBY, null),
             meta.get(FN_DIR, null),
             meta.get(FN_LANGUAGE, null));
    }

    Metadata encodeMetadata() {
        Metadata meta = new Metadata();
        meta.put(FN_ADDRESS, mAddress);
        meta.put(FN_CN, mCn);
        meta.put(FN_SENTBY, mSentBy);
        meta.put(FN_DIR, mDir);
        meta.put(FN_LANGUAGE, mLanguage);
        return meta;
    }

    public Address getFriendlyAddress() throws MailServiceException {
        InternetAddress addr;
        try {
            if (hasCn())
                addr = new InternetAddress(getAddress(),
                                           getCn(),
                                           Mime.P_CHARSET_UTF8);
            else
                addr = new InternetAddress(getAddress());
            return addr;
        } catch (UnsupportedEncodingException e) {
            throw MailServiceException.ADDRESS_PARSE_ERROR(e);
        } catch (AddressException e) {
            throw MailServiceException.ADDRESS_PARSE_ERROR(e);
        }
    }

    public URI getURI() throws ServiceException {
        try {
            return new URI("MAILTO", mAddress, null);
        } catch(URISyntaxException e) {
            throw ServiceException.FAILURE("Could not create URI for address "+mAddress, e);
        }
    }

    protected abstract ICalTok getPropertyName();

    protected void setProperty(ZProperty prop) throws ServiceException {
        if (hasCn())
            prop.addParameter(new ZParameter(ICalTok.CN, getCn()));
        if (hasSentBy())
            prop.addParameter(new ZParameter(ICalTok.SENT_BY, "MAILTO:" + getSentBy()));
        if (hasDir())
            prop.addParameter(new ZParameter(ICalTok.DIR, getDir()));
        if (hasLanguage())
            prop.addParameter(new ZParameter(ICalTok.LANGUAGE, getLanguage()));
    }

    public ZProperty toProperty() throws ServiceException {
        ZProperty prop = new ZProperty(getPropertyName(), "MAILTO:" + getAddress());
        setProperty(prop);
        return prop;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        addToStringBuilder(sb);
        return sb.toString();
    }

    /**
     * Returns the string after "mailto:" if a mailto address.  Returns the
     * entire string otherwise.
     * @param address
     * @return
     */
    protected String getMailToAddress(String address) {
        if (address != null) {
            if (address.toLowerCase().startsWith("mailto:"))
                address = address.substring(7);  // 7 = len("mailto:")
            if (address.length() > 0)
                return address;
        }
        return null;
    }

    protected StringBuilder addToStringBuilder(StringBuilder sb) {
        if (hasCn()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("CN=\"").append(getCn()).append('"');
        }
        if (hasSentBy()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("SENT-BY=\"MAILTO:").append(getSentBy()).append('"');
        }
        if (hasDir()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("DIR=").append(getDir());
        }
        if (hasLanguage()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("LANGUAGE=").append(getLanguage()).append(";");
        }
        if (sb.length() > 0) sb.append(':');
        sb.append("MAILTO:").append(getAddress());
        return sb;
    }
}
