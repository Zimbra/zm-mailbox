/*
 * Created on Apr 1, 2004
 *
 */
package com.zimbra.cs.mime.handler.opensrc;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.textmining.text.extraction.WordExtractor;

import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.MimeHandlerException;



/**
 * @author schemers
 *
 *  class that creates a Lucene document from a Java Mail Message
 */
public class MSWordHandler extends MimeHandler {
    
    private static Log mLog = LogFactory.getLog(MSWordHandler.class);
    private String mContent;
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#populate(org.apache.lucene.document.Document)
     */
    public void addFields(Document doc) throws MimeHandlerException {
        // TODO this is where we'd add attributes 
        // like 'Author', 'Keywords', etc.
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#getContent()
     */
    protected String getContentImpl() throws MimeHandlerException {
        if (mContent == null) {
            WordExtractor extractor = new WordExtractor();
            try {
                mContent = extractor.extractText(getDataSource().getInputStream());
            } catch (Exception e) {
                throw new MimeHandlerException(e);
            }	        
        }
        return mContent;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#convert(com.zimbra.cs.convert.AttachmentInfo, java.lang.String)
     */
    public String convert(AttachmentInfo doc, String baseURL) throws IOException, ConversionException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#doConversion()
     */
    public boolean doConversion() {
        return false;
    }

}
