/*
 * Created on Apr 1, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.mime.handler;

import java.io.IOException;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.lucene.document.Document;

import com.liquidsys.coco.convert.AttachmentInfo;
import com.liquidsys.coco.convert.ConversionException;
import com.liquidsys.coco.mime.MimeHandler;
import com.liquidsys.coco.mime.MimeHandlerException;
import com.liquidsys.coco.util.JMSession;


/**
 * @author schemers
 *
 *  class that creates a Lucene document from a Java Mail Message
 */
public class MessageRFC822Handler extends MimeHandler {

    private MimeMessage mMessage;
    
    public void init(DataSource source) throws MimeHandlerException {
        super.init(source);
        try {
            mMessage = new MimeMessage(JMSession.getSession(), source.getInputStream());
        } catch (Exception e) {
            throw new MimeHandlerException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeHandler#populate(org.apache.lucene.document.Document)
     */
    public void addFields(Document doc) throws MimeHandlerException {
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeHandler#getContent()
     */
    protected String getContentImpl() throws MimeHandlerException {
        try {
            return mMessage.getSubject();
        } catch (Exception e) {
            throw new MimeHandlerException(e);
        }
    }
    
    /**
     * No need to convert rfc822 messages ever.
     */
    public boolean doConversion() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeHandler#convert(com.liquidsys.coco.convert.AttachmentInfo, java.lang.String)
     */
    public String convert(AttachmentInfo doc, String baseURL) throws IOException, ConversionException {
        throw new IllegalStateException("no need to convert message/rfc822 content");
    }

}
