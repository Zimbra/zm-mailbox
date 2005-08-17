/*
 * Created on Apr 1, 2004
 *
 */
package com.liquidsys.coco.mime.handler.opensrc;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.textmining.text.extraction.WordExtractor;

import com.liquidsys.coco.convert.AttachmentInfo;
import com.liquidsys.coco.convert.ConversionException;
import com.liquidsys.coco.mime.MimeHandler;
import com.liquidsys.coco.mime.MimeHandlerException;



/**
 * @author schemers
 *
 *  class that creates a Lucene document from a Java Mail Message
 */
public class MSWordHandler extends MimeHandler {
    
    private static Log mLog = LogFactory.getLog(MSWordHandler.class);
    private String mContent;
    
    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeHandler#populate(org.apache.lucene.document.Document)
     */
    public void addFields(Document doc) throws MimeHandlerException {
        // TODO this is where we'd add attributes 
        // like 'Author', 'Keywords', etc.
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeHandler#getContent()
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
     * @see com.liquidsys.coco.mime.MimeHandler#convert(com.liquidsys.coco.convert.AttachmentInfo, java.lang.String)
     */
    public String convert(AttachmentInfo doc, String baseURL) throws IOException, ConversionException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeHandler#doConversion()
     */
    public boolean doConversion() {
        return false;
    }

}
