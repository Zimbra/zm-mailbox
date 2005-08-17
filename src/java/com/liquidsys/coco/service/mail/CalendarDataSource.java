/**
 * 
 */
package com.liquidsys.coco.service.mail;

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
        
        Method method = (Method)(iCal.getProperties().getProperty(Property.METHOD));
        
        mMethod = method.getValue();
    }

    public String getContentType() {
        ContentType ct = new ContentType();
        ct.setParameter("charset", "US-ASCII");
        
        ct.setPrimaryType("text");
        ct.setSubType("calendar");
        ct.setParameter("method", mMethod);
        ct.setParameter("name", "meeting.ics");
        
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