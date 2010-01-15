/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

/**
 * 
 */
package com.zimbra.cs.mailbox.calendar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.activation.DataSource;
import javax.mail.internet.ContentType;

import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mime.Mime;


/**
 * @author tim
 * 
 * Very simple class which wraps an iCal4j Calendar object in a javamail DataSource 
 *
 */
public class CalendarDataSource implements DataSource
{
    private ZCalendar.ZVCalendar mICal;
    private String mUid;
    private String mMethod;
    private String mAttachName; // NULL if we want a text/calendar part, or set if we want an attached file
    private byte[] mBuf = null;

    public CalendarDataSource(ZCalendar.ZVCalendar iCal, String uid, String attachmentName) {
        mICal = iCal;
        mUid = uid;
        mAttachName = attachmentName;
        if (mAttachName == null || mAttachName.equals("")) {
            mAttachName = "meeting.ics";
        }
        
//        Method method = (Method)(iCal.getProperties().getProperty(Property.METHOD));
//        mMethod = method.getValue();
        mMethod = iCal.getPropVal(ICalTok.METHOD, ICalTok.PUBLISH.toString());
    }

    public String getContentType() {
        ContentType ct = new ContentType();
        ct.setParameter(Mime.P_CHARSET, Mime.P_CHARSET_UTF8);
        
        ct.setPrimaryType("text");
        ct.setSubType("calendar");
        ct.setParameter("method", mMethod);
        if (!mAttachName.toLowerCase().endsWith(".ics")) {
            mAttachName = mAttachName+".ics";
        }
        ct.setParameter("name", mAttachName);
        
        return ct.toString();
    }

    /**
     * Returns the InputStream for this blob. Note that this method 
     * needs a database connection and will obtain/release one
     * automatically if needed, or use the one passed to it from
     * the constructor.
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        synchronized(this) {
            if (mBuf == null) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                OutputStreamWriter wout =
                    new OutputStreamWriter(buf, Mime.P_CHARSET_UTF8);
                mICal.toICalendar(wout);
                wout.flush();
                mBuf = buf.toByteArray();
            }
        }
        ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
        return in;
    }

    /* (non-Javadoc)
     * @see javax.activation.DataSource#getName()
     */
    public String getName() {
        return mUid;
    }

    /* (non-Javadoc)
     * @see javax.activation.DataSource#getOutputStream()
     */
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }
    
}