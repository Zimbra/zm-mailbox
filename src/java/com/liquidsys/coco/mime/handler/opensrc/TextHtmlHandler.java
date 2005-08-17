/*
 * Created on Apr 1, 2004
 *
 */
package com.liquidsys.coco.mime.handler.opensrc;

import java.io.IOException;
import java.io.Reader;

import javax.activation.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.demo.html.HTMLParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.liquidsys.coco.convert.AttachmentInfo;
import com.liquidsys.coco.convert.ConversionException;
import com.liquidsys.coco.mime.Mime;
import com.liquidsys.coco.mime.MimeHandler;
import com.liquidsys.coco.mime.MimeHandlerException;

/**
 * @author schemers
 *
 *  class that creates a Lucene document from a Java Mail Message
 */
public class TextHtmlHandler extends MimeHandler {
    
    private static Log mLog = LogFactory.getLog(TextHtmlHandler.class);

    private static final int MAX_DECODE_BUFFER = 2048;
    
    private String mContent;
    private HTMLParser mParser;
        
    private void copy(Reader reader, StringBuffer buffer)
    	throws IOException 
    {
        char [] cbuff = new char[MAX_DECODE_BUFFER];
        int num;
        while ( (num = reader.read(cbuff, 0, cbuff.length)) != -1) {
	         buffer.append(cbuff, 0, num);
        }
    }

    public void init(DataSource source) throws MimeHandlerException {
        super.init(source);
        try {
            StringBuffer buffer = new StringBuffer();
            Reader reader = Mime.decodeText(source.getInputStream(), source.getContentType());
            mParser = new HTMLParser(reader);
        } catch (Exception e) {
            throw new MimeHandlerException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeHandler#populate(org.apache.lucene.document.Document)
     */
    public void addFields(Document doc) throws MimeHandlerException {
        try {
            // Add the summary as an UnIndexed field, so that it is stored and returned
            // with hit documents for display.
            doc.add(Field.UnIndexed("summary", mParser.getSummary()));
            // Add the title as a separate Text field, so that it can be searched
            // separately.
            doc.add(Field.Text("title", mParser.getTitle()));
        } catch (IOException e) {
            throw new MimeHandlerException(e);
        } catch (InterruptedException e) {
            throw new MimeHandlerException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeHandler#getContent()
     */
    protected String getContentImpl() throws MimeHandlerException {
        if (mContent == null) {
            StringBuffer sb = new StringBuffer();
            try {
                copy(mParser.getReader(), sb);
            } catch (IOException e) {
                throw new MimeHandlerException(e);
            }
            mContent = sb.toString();            
        }
        return mContent;
    }
    
    /**
     * No need to convert text/html document ever.
     */
    public boolean doConversion() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.mime.MimeHandler#convert(com.liquidsys.coco.convert.AttachmentInfo, java.lang.String)
     */
    public String convert(AttachmentInfo doc, String baseURL) throws IOException, ConversionException {
        throw new UnsupportedOperationException();
    }
            
}
