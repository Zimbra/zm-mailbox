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
class CalendarDataSource implements DataSource
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