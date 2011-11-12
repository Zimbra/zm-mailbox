/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.calendar;

import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

/**
 * iCalendar ATTACH property
 */
public class Attach {

    private String mContentType;
    private String mUri;
    private String mBinaryB64Data;

    public Attach(String uri, String contentType) {
        mUri = uri;
        mContentType = contentType;
    }

    public Attach(String binaryB64Data) {
        mBinaryB64Data = binaryB64Data;
    }
    
    public String getUri() {
        return mUri;
    }
    
    public String getContentType() {
        return mContentType;
    }
    
    public String getBinaryB64Data() {
        return mBinaryB64Data;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mUri != null) {
            sb.append("uri=").append(mUri);
            if (mContentType != null)
                sb.append(", ct=").append(mContentType);
        } else
            sb.append("binaryBase64=").append(mBinaryB64Data);
        return sb.toString();
    }

    public Element toXml(Element parent) {
        Element attachElem = parent.addElement(MailConstants.E_CAL_ATTACH);
        if (mUri != null) {
            attachElem.addAttribute(MailConstants.A_CAL_ATTACH_URI, mUri);
            if (mContentType != null) {
                attachElem.addAttribute(MailConstants.A_CAL_ATTACH_CONTENT_TYPE, mContentType);
            }
        } else {
            attachElem.setText(mBinaryB64Data);
        }
        return attachElem;
    }

    public static Attach parse(Element element) {
        String uri = element.getAttribute(MailConstants.A_CAL_ATTACH_URI, null);
        if (uri != null) {
            String ct = element.getAttribute(MailConstants.A_CAL_ATTACH_CONTENT_TYPE, null);
            return new Attach(uri, ct);
        } else {
            String binB64 = element.getTextTrim();
            return new Attach(binB64);
        }
    }

    public ZProperty toZProperty() {
        if (mUri != null) {
            ZProperty prop = new ZProperty(ICalTok.ATTACH, mUri);
            if (mContentType != null)
                prop.addParameter(new ZParameter(ICalTok.FMTTYPE, mContentType));
            return prop;
        } else {
            ZProperty prop = new ZProperty(ICalTok.ATTACH, mBinaryB64Data);
            prop.addParameter(new ZParameter(ICalTok.VALUE, "BINARY"));
            prop.addParameter(new ZParameter(ICalTok.ENCODING, "BASE64"));
            return prop;
        }
    }

    public static Attach parse(ZProperty prop) {
        String value = prop.getValue();
        ZParameter valueType = prop.getParameter(ICalTok.VALUE);
        if (valueType != null) {
            if (valueType.getValue().equals("BINARY"))
                return new Attach(value);
        }

        // URI
        String ct = null;
        ZParameter fmttype = prop.getParameter(ICalTok.FMTTYPE);
        if (fmttype != null)
            ct = fmttype.getValue();
        return new Attach(value, ct);
    }
}
