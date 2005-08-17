  /*
 * Created on Apr 1, 2004
 *
 */
package com.zimbra.cs.mime.handler;

import java.io.IOException;

import javax.activation.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;

/**
 * @author schemers
 *
 *  class that creates a Lucene document from a Java Mail Message
 */
public class TextPlainHandler extends MimeHandler {
    
    private static Log mLog = LogFactory.getLog(TextPlainHandler.class);
    private String mContent;

    public void addFields(Document doc) throws MimeHandlerException {
        // we add no type-specific fields to the doc
    }

    protected String getContentImpl() throws MimeHandlerException {
        if (mContent == null) {
            StringBuffer buffer = new StringBuffer();
            DataSource source = getDataSource();
            try {
                Mime.decodeText(source.getInputStream(), source.getContentType(), buffer);
            } catch (IOException e) {
                throw new MimeHandlerException(e);
            }
            mContent = buffer.toString();
        }
        return mContent;
    }
    
    /**
     * No need to convert plain text document ever.
     */
    public boolean doConversion() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#convert(com.zimbra.cs.convert.AttachmentInfo, java.lang.String)
     */
    public String convert(AttachmentInfo doc, String baseURL) throws IOException, ConversionException {
        throw new IllegalStateException("No need to convert plain text");
    }

}
