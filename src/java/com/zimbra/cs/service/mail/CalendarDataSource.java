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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/**
 * 
 */
package com.zimbra.cs.service.mail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.mail.internet.ContentType;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.property.Method;

/**
 * @author tim
 * 
 * Very simple class which wraps an iCal4j Calendar object in a javamail DataSource 
 *
 */
class CalendarDataSource implements DataSource
{
    private Calendar iCal;
    private String mUid;
    private String mMethod;
    private String mAttachName; // NULL if we want a text/calendar part, or set if we want an attached file

    public CalendarDataSource(Calendar iCal, String uid, String attachmentName) {
        this.iCal = iCal;
        mUid = uid;
        mAttachName = attachmentName;
        if (mAttachName == null || mAttachName.equals("")) {
            mAttachName = "meeting.ics";
        }
        
        Method method = (Method)(iCal.getProperties().getProperty(Property.METHOD));
        
        mMethod = method.getValue();
    }

    public String getContentType() {
        ContentType ct = new ContentType();
        ct.setParameter("charset", "US-ASCII");
        
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
        try { 
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            CalendarOutputter calOut = new CalendarOutputter();
            
            calOut.output(iCal, buf);
            
            ByteArrayInputStream in = new ByteArrayInputStream(buf.toByteArray());
            return in;
        } catch (ValidationException e) {
            IOException ioe = new IOException("CalendarDataSource.getInputStream");
            ioe.initCause(e);
            throw ioe;
        }
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