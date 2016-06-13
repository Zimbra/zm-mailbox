/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.calendar;

import java.io.File;

import net.fortuna.ical4j.util.Base64;

import com.google.common.base.Strings;
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
    private String fileName;

    protected Attach(String uri, String contentType) {
        mUri = uri;
        mContentType = contentType;
    }

    protected Attach(String binaryB64Data) {
        mBinaryB64Data = binaryB64Data;
    }

    protected Attach(byte[] binaryB64Data, String contentType) {
        mBinaryB64Data = new String(binaryB64Data);
        mContentType = contentType;
    }

    public static Attach fromUnencodedAndContentType(byte[] rawBytes, String contentType) {
        net.fortuna.ical4j.model.property.Attach ical4jAttach = new net.fortuna.ical4j.model.property.Attach(rawBytes);
        String encoded = ical4jAttach.getValue();
        return fromEncodedAndContentType(encoded, contentType);
    }

    public static Attach fromEncodedAndContentType(byte[] binaryB64Data, String contentType) {
        return new Attach(binaryB64Data, contentType);
    }

    public static Attach fromEncodedAndContentType(String binaryB64Data, String contentType) {
        Attach attach = new Attach(binaryB64Data);
        attach.setContentType(contentType);
        return attach;
    }

    public static Attach fromUriAndContentType(String uri, String contentType) {
        return new Attach(uri, contentType);
    }

    public String getUri() {
        return mUri;
    }

    public void setContentType(String contentType) {
        mContentType = contentType;
    }

    public String getContentType() {
        return mContentType;
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName - the basename of this filename will be associated with the attachment
     */
    public void setFileName(String fileName) {
        if (Strings.isNullOrEmpty(fileName)) {
            this.fileName = null;
        }
        File file = new File(fileName);
        this.fileName = file.getName();
    }

    public String getBinaryB64Data() {
        return mBinaryB64Data;
    }

    public byte[] getDecodedData() {
        if (null == mBinaryB64Data) {
            return null;
        }
        return Base64.decode(mBinaryB64Data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mUri != null) {
            sb.append("uri=").append(mUri);
        } else {
            sb.append("binaryBase64=").append(mBinaryB64Data);
        }
        if (mContentType != null) {
            sb.append(", ct=").append(mContentType);
        }
        if (fileName != null) {
            sb.append(", fn=").append(fileName);
        }
        return sb.toString();
    }

    public Element toXml(Element parent) {
        Element attachElem = parent.addNonUniqueElement(MailConstants.E_CAL_ATTACH);
        if (mUri != null) {
            attachElem.addAttribute(MailConstants.A_CAL_ATTACH_URI, mUri);
        } else {
            attachElem.setText(mBinaryB64Data);
        }
        if (mContentType != null) {
            attachElem.addAttribute(MailConstants.A_CAL_ATTACH_CONTENT_TYPE, mContentType);
        }
        return attachElem;
    }

    public static Attach parse(Element element) {
        String ct = element.getAttribute(MailConstants.A_CAL_ATTACH_CONTENT_TYPE, null);
        String uri = element.getAttribute(MailConstants.A_CAL_ATTACH_URI, null);
        if (uri != null) {
            return Attach.fromUriAndContentType(uri, ct);
        } else {
            String binB64 = element.getTextTrim();
            return Attach.fromEncodedAndContentType(binB64, ct);
        }
    }

    public ZProperty toZProperty() {
        ZProperty prop;
        if (mUri != null) {
            prop = new ZProperty(ICalTok.ATTACH, mUri);
        } else {
            prop = new ZProperty(ICalTok.ATTACH, mBinaryB64Data);
            prop.addParameter(new ZParameter(ICalTok.VALUE, "BINARY"));
            prop.addParameter(new ZParameter(ICalTok.ENCODING, "BASE64"));
        }
        if (mContentType != null) {
            prop.addParameter(new ZParameter(ICalTok.FMTTYPE, mContentType));
        }
        if (fileName != null) {
            /* We recognise X-FILENAME incoming as well from Microsoft documentation but CalDAV clients are more
             * likely to use X-APPLE-FILENAME
             */
            prop.addParameter(new ZParameter(ICalTok.X_APPLE_FILENAME, fileName));
        }
        return prop;
    }

    public static Attach parse(ZProperty prop) {
        String value = prop.getValue();
        String ct = null;
        ZParameter fmttype = prop.getParameter(ICalTok.FMTTYPE);
        if (fmttype != null) {
            ct = fmttype.getValue();
        }
        ZParameter valueType = prop.getParameter(ICalTok.VALUE);
        Attach attach = null;
        if ((valueType != null) && (valueType.getValue().equals("BINARY"))) {
            attach = Attach.fromEncodedAndContentType(value, ct);
            ZParameter fn = prop.getParameter(ICalTok.X_FILENAME);
            if (fn == null) {
                fn = prop.getParameter(ICalTok.X_APPLE_FILENAME);
            }
            if (fn != null) {
                attach.setFileName(fn.getValue());
            }
        } else {
            // URI
            attach = Attach.fromUriAndContentType(value, ct);
        }
        return attach;
    }
}
