/*
 * Created on 2004. 5. 27.
 */
package com.zimbra.cs.index;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeHandler;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.TnefExtractor;
import com.zimbra.cs.object.ObjectHandlerException;
import com.zimbra.cs.service.ServiceException;



/**
 * @author jhahm
 *
 * Class that creates a Lucene document for top-level MIME message.
 * All textual data from MIME parts are aggregated into this single
 * Lucene document
 */
public final class TopLevelMessageHandler {

    private static Log mLog = LogFactory.getLog(TopLevelMessageHandler.class);
	
	private static final int STORE = 10;
	private static final int DONT_STORE = 20;
	
    private static final int INDEX = 30;
    private static final int DONT_INDEX = 40;

    private static final int TOKENIZE = 50;
//    private static final int DONT_TOKENIZE = 60;
    
    private MimeMessage  mMessage;
    private List         mMessageParts;
    private Document     mDocument;
    private StringBuffer mContent;
    private String       mBodyContent;
    private boolean      mHasCalendar;
//    private String mDomains;

    public TopLevelMessageHandler(MimeMessage mm, List parts) {
    	mMessage = mm;
    	mMessageParts = parts;
    	mDocument = new Document();
    	mContent = new StringBuffer();
    }

    public void addContent(String text) {
    	addContent(text, false);
    }
    
    public void addContent(String text, boolean isMainBody) {
    	if (mContent.length() > 0)
    		mContent.append(" ");
    	mContent.append(text);
    	
    	if (isMainBody)
    		mBodyContent = text;
    }

    public MimeMessage getMimeMessage() {
    	return mMessage;
    }
    
    public List /*<MPartInfo>*/ getMessageParts() {
    	return mMessageParts;
    }
    
    public void hasCalendarPart(boolean hasCal) {
        mHasCalendar = hasCal;
    }

    public String getFragment() {
        String remainder = (mBodyContent == null ? mContent.toString() : mBodyContent).trim();
        return Fragment.getFragment(remainder, mHasCalendar);
    }

    public Document getDocument(ParsedMessage pm)
    throws MessagingException, ServiceException {
        
        mDocument.add(Field.Text(LuceneFields.L_MIMETYPE, "message/rfc822"));
        mDocument.add(Field.Keyword(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_TOP));
        
        String toValue = setHeaderAsField("to", LuceneFields.L_H_TO, DONT_STORE, INDEX, TOKENIZE);
        String ccValue = setHeaderAsField("cc", LuceneFields.L_H_CC, DONT_STORE, INDEX, TOKENIZE);
//      String subject = setHeaderAsField("subject", LuceneFields.L_H_SUBJECT, DONT_STORE, INDEX, TOKENIZE);
        
        String subject = pm.getNormalizedSubject();
        String sortFrom = pm.getParsedSender().getSortString();
        String from = pm.getSender();
        //                                                                          store, index, tokenize
        mDocument.add(new Field(LuceneFields.L_H_FROM, from,                        false, true, true));
        mDocument.add(new Field(LuceneFields.L_H_SUBJECT, subject,                  false, true, true));
        mDocument.add(new Field(LuceneFields.L_SORT_SUBJECT, subject.toUpperCase(), true, true, false));
        mDocument.add(new Field(LuceneFields.L_SORT_NAME, sortFrom.toUpperCase(),   false, true, false));
        
        // add subject and from to main content for better searching
        addContent(subject);
        
        // Bug 583: add all of the TOKENIZED versions of the email addresses to our CONTENT field...
        addContent(ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_FROM, from));
        addContent(ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_TO, toValue));
        addContent(ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_CC, ccValue));
        
        String text = mContent.toString();
        
        mDocument.add(Field.UnStored(LuceneFields.L_CONTENT, text));
        
        String sizeStr = Integer.toString(mMessage.getSize());
        mDocument.add(Field.Text(LuceneFields.L_SIZE, sizeStr));
        
        try {
            MimeHandler.getObjects(text, mDocument);
        } catch (ObjectHandlerException e) {
            String msgid = pm.getMessageID();
            String sbj = pm.getMimeMessage().getSubject();
            mLog.warn("Unable to recognize searchable objects in message (Message-ID: " +
                msgid + ", Subject: " + sbj + ")", e);
        }
        
        // Get the list of attachment content types from this message and any
        // TNEF attachments
        Set contentTypes = Mime.getAttachmentList(mMessageParts);
        TnefExtractor tnef = new TnefExtractor();
        try {
            Mime.accept(tnef, pm.getMimeMessage());
            MimeMessage[] msgs = tnef.getTnefsAsMime();
            for (int i = 0; i < msgs.length; i++) {
                mLog.debug("Adding embedded content types from TNEF attachment");
                MimeMessage msg = msgs[i];
                List parts = Mime.getParts(msg);
                Set moreTypes = Mime.getAttachmentList(parts);
                contentTypes.addAll(moreTypes);
            }
        } catch (Exception e) {
            mLog.info("Unable to determine embedded TNEF content types", e);
        }
        
        // Assemble a comma-separated list of attachment content types
        StringBuffer buf = new StringBuffer();
        Iterator i = contentTypes.iterator();
        while (i.hasNext()) {
            String contentType = (String) i.next();
            if (buf.length() > 0) {
                buf.append(',');
            }
            buf.append(contentType);
        }
        
        String attachments = buf.toString();
        if (attachments.equals(""))
            attachments = LuceneFields.L_ATTACHMENT_NONE;
        else
            attachments = attachments + "," + LuceneFields.L_ATTACHMENT_ANY;
        mDocument.add(Field.UnStored(LuceneFields.L_ATTACHMENTS, attachments));
        
        long date = pm.getReceivedDate();
        
        String dateString = DateField.timeToString(date);
        if (dateString != null) {
            try {
                mDocument.add(Field.Text(LuceneFields.L_DATE, dateString));
            } catch(Exception e) {
                // parse error on date or 
                // date before/after lucene's valid date range.  ignore it.
            }
        } else {
            throw new MessagingException("Couldn't get valid date for message");
        }
        
        return mDocument;
    }
    
	private String setHeaderAsField(String headerName, String fieldName,
			int stored, int indexed, int tokenized)
	throws MessagingException  {
		return setHeaderAsField(mDocument, headerName, fieldName, stored, indexed, tokenized);
	}	
	
	private String setHeaderAsField(Document d, String headerName, String fieldName,
			int stored, int indexed, int tokenized)
	throws MessagingException  {
	    assert((stored == STORE) || (stored == DONT_STORE));
	    assert((indexed == INDEX) || (stored == DONT_INDEX));
	    
		String value = mMessage.getHeader(headerName, null);
		
		if (value == null || value.length() == 0)
			return "";
		try {
			value = MimeUtility.decodeText(value);
		} catch (UnsupportedEncodingException e) { }
        d.add(new Field(fieldName, value, stored == STORE, indexed == INDEX, tokenized == TOKENIZE));
		return value;
	}
	
	/**
	 * For every attachment, many of the lucene indexed fields from the top level
	 * message are also indexed as part of the attachment: this is done so that the
	 * attachment will show up if you do things like "type:pdf and from:foo"
	 * 
	 * "this" --> top level doc
	 * @param d subdocument of top level 
	 */
	public void setLuceneHeadersFromContainer(Document d, ParsedMessage pm) throws MessagingException {
	    setHeaderAsField(d, "to", LuceneFields.L_H_TO, DONT_STORE, INDEX, TOKENIZE);
		setHeaderAsField(d, "cc", LuceneFields.L_H_CC, DONT_STORE, INDEX, TOKENIZE);
        
        String subject = pm.getNormalizedSubject();
        String sortFrom = pm.getParsedSender().getSortString();
        String from = pm.getSender();
        d.add(new Field(LuceneFields.L_H_FROM, from, false, true, true));
        d.add(new Field(LuceneFields.L_H_SUBJECT, subject, false, true, true));
        d.add(new Field(LuceneFields.L_SORT_SUBJECT, subject.toUpperCase(), true, true, false));
        d.add(new Field(LuceneFields.L_SORT_NAME, sortFrom.toUpperCase(), false, true, false));
        
		/* If the document already has a date set (e.g. the word doc we parsed had a modified date or something) 
		 * then we'll just use that date -- otherwise, we'll use the date from our container's document
		 */
		if (null == d.getField(LuceneFields.L_DATE)) {
		    String containerDate = mDocument.get(LuceneFields.L_DATE);
		    d.add(Field.Text(LuceneFields.L_DATE, containerDate));
		}
	}
}
