/*
 * Created on Apr 1, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.mime.handler.opensrc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.activation.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDDocumentInformation;
import org.pdfbox.util.PDFTextStripper;

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
/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PDFHandler extends MimeHandler {
    
    private static Log mLog = LogFactory.getLog(PDFHandler.class);
    
    private Map mAttributes = new HashMap();
    private String mContent;
    
    public static void addText(Document d, String name, String value) {
        if (value != null)
            d.add(Field.Text(name, value));
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#init(javax.activation.DataSource)
     */
    public void init(DataSource source) throws MimeHandlerException {
        super.init(source);
        try {
            PDDocument pdf = null;
            try {
                org.pdfbox.pdfparser.PDFParser parser;
                parser = new org.pdfbox.pdfparser.PDFParser(source.getInputStream());
                parser.parse();
                pdf = parser.getPDDocument();
                PDFTextStripper stripper = new PDFTextStripper();
                mContent = stripper.getText(pdf);
                
                PDDocumentInformation info = pdf.getDocumentInformation();
                // all of these might be overkill
                StringBuffer metaBuf = new StringBuffer();
                mAttributes.put(Mime.MF_AUTHOR, info.getAuthor());
                mAttributes.put(Mime.MF_KEYWORDS, info.getKeywords());
                mAttributes.put(Mime.MF_TITLE, info.getTitle() + " " + info.getSubject());
                metaBuf.append(info.getAuthor()).append(" ");
                metaBuf.append(info.getCreator()).append(" ");
                metaBuf.append(info.getKeywords()).append(" ");
                metaBuf.append(info.getProducer()).append(" ");
                metaBuf.append(info.getSubject()).append(" ");
                metaBuf.append(info.getTitle()).append(" ");
            
                // do we need these?
                metaBuf.append(info.getCreationDate()).append(" ");
                metaBuf.append(info.getModificationDate());
                mAttributes.put(Mime.MF_METADATA, metaBuf.toString());
            } finally {
                if (pdf != null) { 
                    try {
                        pdf.close();
                    } catch (IOException e) {
                        if (mLog.isWarnEnabled()) {
                            mLog.warn("PDFParser close exception", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
    	    throw new MimeHandlerException(e);
    	}
    }

    public void addFields(Document doc) throws MimeHandlerException {
        for (Iterator it=mAttributes.entrySet().iterator(); it.hasNext();) {
            Map.Entry me = (Entry) it.next();
            addText(doc, (String)me.getKey(), (String)me.getValue());
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mime.MimeHandler#getContent()
     */
    protected String getContentImpl() throws MimeHandlerException {
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
