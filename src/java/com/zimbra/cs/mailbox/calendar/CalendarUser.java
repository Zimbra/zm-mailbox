/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailbox.calendar;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.mime.MimeConstants;

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
    private List<ZParameter> mXParams = new ArrayList<ZParameter>();

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
    
    private final String blankIfNullSpaceAfterIfNot(String str) {
        if (str == null)
            return "";
        else
            return str+" ";
    }
    
    /**
     * @return all the data in this concatenated, for easy indexing
     */
    public String getIndexString() {
        StringBuilder s = new StringBuilder();
        s.append(blankIfNullSpaceAfterIfNot(getCn()));
        s.append(blankIfNullSpaceAfterIfNot(getAddress()));
        s.append(blankIfNullSpaceAfterIfNot(getSentBy()));
        s.append(blankIfNullSpaceAfterIfNot(getDir()));
        return s.toString().trim();
    }
    
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

    public CalendarUser(String address,
                        String cn,
                        String sentBy,
                        String dir,
                        String language,
                        List<ZParameter> xparams) {
        this(address, cn, sentBy, dir, language);
        if (xparams != null)
            mXParams = xparams;
    }

    public CalendarUser(ZProperty prop) {
        this(prop.getValue(),
             prop.paramVal(ICalTok.CN, null),
             prop.paramVal(ICalTok.SENT_BY, null),
             prop.paramVal(ICalTok.DIR, null),
             prop.paramVal(ICalTok.LANGUAGE, null));

        for (Iterator<ZParameter> paramIter = prop.parameterIterator(); paramIter.hasNext(); ) {
            ZParameter param = paramIter.next();
            if (param.getToken() == null) {
                String name = param.getName();
                if (name.startsWith("X-") || name.startsWith("x-"))
                    addXParam(param);
            }
        }
    }

    public CalendarUser(Metadata meta) throws ServiceException {
        this(meta.get(FN_ADDRESS, null),
             meta.get(FN_CN, null),
             meta.get(FN_SENTBY, null),
             meta.get(FN_DIR, null),
             meta.get(FN_LANGUAGE, null));

        List<ZParameter> xparams = Util.decodeXParamsFromMetadata(meta);
        if (xparams != null) {
            for (ZParameter xparam : xparams) {
                mXParams.add(xparam);
            }
        }
    }

    protected CalendarUser(CalendarUser other) {
        mAddress = other.mAddress;
        mCn = other.mCn;
        mSentBy = other.mSentBy;
        mDir = other.mDir;
        mLanguage = other.mLanguage;
        mXParams.addAll(other.mXParams);
    }

    public Metadata encodeMetadata() {
        Metadata meta = new Metadata();
        meta.put(FN_ADDRESS, mAddress);
        meta.put(FN_CN, mCn);
        meta.put(FN_SENTBY, mSentBy);
        meta.put(FN_DIR, mDir);
        meta.put(FN_LANGUAGE, mLanguage);

        if (mXParams.size() > 0)
            Util.encodeXParamsAsMetadata(meta, xparamsIterator());

        return meta;
    }

    public InternetAddress getFriendlyAddress() throws MailServiceException {
        InternetAddress addr;
        try {
            String address = getAddress();
            if (address == null || address.length() < 1)
                throw MailServiceException.ADDRESS_PARSE_ERROR("No address value", null);
            if (hasCn())
                addr = new InternetAddress(address,
                                           getCn(),
                                           MimeConstants.P_CHARSET_UTF8);
            else
                addr = new InternetAddress(address);
            return addr;
        } catch (UnsupportedEncodingException e) {
            throw MailServiceException.ADDRESS_PARSE_ERROR(e);
        } catch (AddressException e) {
            throw MailServiceException.ADDRESS_PARSE_ERROR(e);
        }
    }

    /**
     * Reply-to address is either the address of the user, or the sent-by
     * address if it is set.
     * @return
     * @throws MailServiceException
     */
    public InternetAddress getReplyAddress() throws MailServiceException {
        InternetAddress addr;
        try {
            if (hasSentBy()) {
                addr = new InternetAddress(getSentBy());
            } else {
                addr = getFriendlyAddress();
            }
            return addr;
        } catch (AddressException e) {
            throw MailServiceException.ADDRESS_PARSE_ERROR(e);
        }
    }

    public URI getURI() throws ServiceException {
        try {
            return new URI("mailto", mAddress, null);
        } catch(URISyntaxException e) {
            throw ServiceException.FAILURE("Could not create URI for address "+mAddress, e);
        }
    }

    protected abstract ICalTok getPropertyName();

    protected void setProperty(ZProperty prop) throws ServiceException {
        if (hasCn())
            prop.addParameter(new ZParameter(ICalTok.CN, getCn()));
        if (hasSentBy())
            prop.addParameter(new ZParameter(ICalTok.SENT_BY, "mailto:" + getSentBy()));
        if (hasDir())
            prop.addParameter(new ZParameter(ICalTok.DIR, getDir()));
        if (hasLanguage())
            prop.addParameter(new ZParameter(ICalTok.LANGUAGE, getLanguage()));
    }

    public ZProperty toProperty() throws ServiceException {
    	String addr = getAddress();
    	if (addr != null && addr.indexOf(':') < 0)
    		addr = "mailto:" + addr;
        ZProperty prop = new ZProperty(getPropertyName(), addr);
        setProperty(prop);
        // x-param
        for (ZParameter xparam : mXParams) {
            prop.addParameter(xparam);
        }
        return prop;
    }

    public List<ZParameter> getXParams() { return mXParams; }
    public Iterator<ZParameter> xparamsIterator() { return mXParams.iterator(); }
    public void addXParam(ZParameter param) {
        mXParams.add(param);
    }
    public ZParameter getXParam(String xparamName) {
        for (ZParameter param : mXParams) {
            if (param.getName().equalsIgnoreCase(xparamName))
                return param;
        }
        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        addToStringBuilder(sb);
        for (ZParameter xparam : mXParams) {
            sb.append(", ").append(xparam.toString());
        }
        return sb.toString();
    }

    /**
     * Returns the string after "mailto:" if a mailto address.  Returns the
     * entire string otherwise.
     * @param address
     * @return
     */
    protected static String getMailToAddress(String address) {
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
            sb.append("SENT-BY=\"mailto:").append(getSentBy()).append('"');
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
        sb.append("mailto:").append(getAddress());
        return sb;
    }

    protected static <T> boolean sameValues(T val1, T val2) {
        if (val1 != null)
            return val1.equals(val2);
        else
            return val2 == null;
    }

    public boolean equals(Object o) {
    	if (o == this) return true;
    	if (!(o instanceof CalendarUser)) return false;
    	CalendarUser other = (CalendarUser) o;
        if (!sameValues(mAddress, other.mAddress))
        	return false;
        if (!sameValues(mCn, other.mCn))
        	return false;
        if (!sameValues(mSentBy, other.mSentBy))
        	return false;
        if (!sameValues(mDir, other.mDir))
        	return false;
        if (!sameValues(mLanguage, other.mLanguage))
        	return false;
        if (!sameValues(mXParams, other.mXParams))
        	return false;
        return true;
    }
}
