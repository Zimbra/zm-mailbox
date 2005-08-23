/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 6. 30.
 */
package com.zimbra.cs.mime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.TopLevelMessageHandler;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.object.ObjectHandlerException;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.StringUtil;

/**
 * @author jhahm
 */
public class ParsedMessage {

    private static Log sLog = LogFactory.getLog(ParsedMessage.class);
    private static MailDateFormat sFormat = new MailDateFormat();

    private MimeMessage mMimeMessage;
    private boolean mParsed = false;
    private boolean mAnalyzed = false;
    private boolean mIndexAttachments = true;

    private List /*<MPartInfo>*/ mMessageParts;
    private String mRecipients;
    private String mSender;
    private ParsedAddress mParsedSender;
	private boolean mHasAttachments = false;
	private String mFragment = "";
	private long mDateHeader = -1;
    private long mReceivedDate = -1;
    private long mZimbraDate = -1;
    private String mSubject;
    private String mNormalizedSubject;
	private boolean mSubjectPrefixed;
	private List /*<Document>*/ mLuceneDocuments;
	private Calendar miCalendar;

    // m*Raw* should only be assigned by setRawData so these fields can kept in sync
    private byte[] mRawData;
    private String mRawDigest;
    private boolean mHaveRaw;
    
    public ParsedMessage(MimeMessage msg, boolean indexAttachments) {
        mIndexAttachments = indexAttachments;
        mMimeMessage = msg;
        setReceivedDate(getZimbraDateHeader());
	}

    // When re-indexing, we must force the received-date to be correct
    // and we need to do it here in the constructor, before the Lucene
    // document is initialized
    public ParsedMessage(MimeMessage msg, long receivedDate, boolean indexAttachments) {
        mIndexAttachments = indexAttachments;
        mMimeMessage = msg;
        setReceivedDate(receivedDate);
    }

    public ParsedMessage(byte[] rawData, boolean indexAttachments) throws MessagingException {
        mIndexAttachments = indexAttachments;
        setRawData(rawData);
        ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
        mMimeMessage = new MimeMessage(JMSession.getSession(), bais);
        try {
            bais.close();
        } catch (IOException ioe) {
            // we know ByteArrayInputStream.close() is a no-op
        }
        setReceivedDate(getZimbraDateHeader());
    }

    public ParsedMessage(byte[] rawData, long receivedDate, boolean indexAttachments) throws MessagingException {
        mIndexAttachments = indexAttachments;
        setRawData(rawData);
        ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
        mMimeMessage = new MimeMessage(JMSession.getSession(), bais);
        try {
            bais.close();
        } catch (IOException ioe) {
            // we know ByteArrayInputStream.close() is a no-op
        }
        setReceivedDate(receivedDate);
    }

	private ParsedMessage parse() {
        if (mParsed)
            return this;
        mParsed = true;

		try {
            mMessageParts = Mime.getParts(mMimeMessage);
            mHasAttachments = Mime.hasAttachment(mMessageParts);
        } catch (Exception e) {
            sLog.warn("exception while parsing message; message will not be indexed", e);
            mMessageParts = new ArrayList();
        }
        return this;
    }
    
    public ParsedMessage analyze() throws ServiceException {
        parse();
		try {
    		analyzeMessage();   // figure out everything about this message
        } catch (ServiceException e) {
            throw e;
		} catch (Exception e) {
            sLog.warn("exception while analyzing message; message will be partially indexed", e);
		}
        return this;
	}

	public MimeMessage getMimeMessage() {
		return mMimeMessage;
	}

    private void setRawData(byte[] rawData) {
        mHaveRaw = true;
        mRawData = rawData;
        mRawDigest = ByteUtil.getDigest(rawData);
    }

    // Lazy conversion of MimeMessage to byte[] when needed - this happens only
    // when you have to save a message.
    private void mimeToRaw() throws MessagingException, IOException {
        if (mHaveRaw)
            return;

        int size = mMimeMessage.getSize() + 2048;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
        mMimeMessage.writeTo(baos);
        byte[] rawData = baos.toByteArray();
        baos.close();
        setRawData(rawData);
    }

    public int getRawSize() throws MessagingException, IOException {
        mimeToRaw();
        return mRawData.length;
    }
    
    public byte[] getRawData() throws MessagingException, IOException {
        mimeToRaw();
        return mRawData;
    }

    public String getRawDigest() throws MessagingException, IOException {
        mimeToRaw();
        return mRawDigest;
    }

    public List /*<MPartInfo>*/ getMessageParts() {
        parse();
    	return mMessageParts;
    }

	public boolean hasAttachments() {
        parse();
		return mHasAttachments;
	}

	public boolean isReply() {
        normalizeSubject();
		return mSubjectPrefixed;
	}

    public String getSubject() {
        normalizeSubject();
        return mSubject;
    }

    public String getNormalizedSubject() {
        normalizeSubject();
        return mNormalizedSubject;
    }

	public String getFragment() {
        try {
            analyze();
        } catch (ServiceException e) {
            sLog.warn("Message analysis failed when getting fragment; fragment is: " + mFragment);
        }
		return mFragment;
	}

    public String getMessageID() {
        try {
            String msgid = mMimeMessage.getMessageID();
            return ("".equals(msgid) ? null : msgid);
        } catch (MessagingException me) {
            return null;
        }
    }

    public String getRecipients() {
        if (mRecipients == null) {
            String recipients = null;
            try {
                recipients = mMimeMessage.getHeader("To", ", ");
            } catch (MessagingException e) { }
            if (recipients == null)
                recipients = "";
            try {
                mRecipients = MimeUtility.decodeText(recipients);
            } catch (UnsupportedEncodingException e1) {
                mRecipients = recipients;
            }
        }
        return mRecipients;
    }

    public String getSender() {
        if (mSender == null) {
            String sender = null;
            try {
                sender = mMimeMessage.getHeader("From", null);
            } catch (MessagingException e) { }
            if (sender == null)
                try {
                    sender = mMimeMessage.getHeader("Sender", null);
                } catch (MessagingException e2) { }
            if (sender == null)
                sender = "";
            try {
                mSender = MimeUtility.decodeText(sender);
            } catch (UnsupportedEncodingException e1) {
                mSender = sender;
            }
        }
        return mSender;
    }

    public ParsedAddress getParsedSender() {
        if (mParsedSender == null)
            mParsedSender = new ParsedAddress(getSender()).parse();
        return mParsedSender;
    }

    public String getReplyTo() {
        String sender = getSender();
        String replyTo = null;
        try {
            replyTo = mMimeMessage.getHeader("Reply-To", null);
            if (replyTo == null || replyTo.trim().equals(""))
                return null;
            replyTo = MimeUtility.decodeText(replyTo);
        } catch (Exception e) { }
        if (sender != null && sender.equals(replyTo))
            return null;
        return replyTo;
    }

    public long getDateHeader() {
		if (mDateHeader != -1)
			return mDateHeader;
		mDateHeader = getReceivedDate();
		try {
		    Date dateHeader = mMimeMessage.getSentDate();
		    if (dateHeader != null) {
                // prevent negative dates, which Lucene can't deal with
		        mDateHeader = Math.max(dateHeader.getTime(), 0);
            }
		} catch (MessagingException e) { }

        return mDateHeader;
	}

    private void setReceivedDate(long date) {
        if (date == -1)
            date = System.currentTimeMillis();
        date = Math.max(0, date);
        // round to nearest second...
        mReceivedDate = (date / 1000) * 1000;
    }
    public long getReceivedDate() {
        if (mReceivedDate == -1)
            sLog.error("Received date not set for ParsedMessage!");
        assert(mReceivedDate != -1);  
        return mReceivedDate;
    }

    public long getZimbraDateHeader() {
        if (mZimbraDate != -1)
            return mZimbraDate;

        String zimbraHeader = null;
        try {
            zimbraHeader = mMimeMessage.getHeader("X-Zimbra-Received", null);
            if (zimbraHeader == null || zimbraHeader.trim().equals(""))
                return -1;
        } catch (MessagingException mex) {
            return -1;
        }

        Date zimbraDate = null;
        synchronized (sFormat) {
            try {
                zimbraDate = sFormat.parse(zimbraHeader);
            } catch (ParseException e) {
                return -1;
            }
        }
        return (zimbraDate == null ? -1 : zimbraDate.getTime());
    }

    public List /*<Document>*/ getLuceneDocuments() {
        try {
            analyze();
        } catch (ServiceException e) {
            sLog.warn("Message analysis failed when getting lucene documents");
        }
		return mLuceneDocuments;
	}

    /**
     * Returns the iCalendar object that is parsed from the
     * text/calendar mime part if it exists in this message. 
     * The presence of this mime part indicates that this message 
     * carries an appointment.
     * 
     * @return the iCalendar object or null if this is an ordinary message
     */
    public Calendar getiCalendar() {
        try {
            analyze();
        } catch (ServiceException e) {
            // the calendar info should still be parsed 
            sLog.warn("Message analysis failed when getting calendar info");
        }
        return miCalendar;
    }
    
	private void analyzeMessage()
	throws MessagingException, ServiceException {
        if (mAnalyzed)
            return;
        mAnalyzed = true;
        
        mLuceneDocuments = new ArrayList();

        if (DebugConfig.disableMessageAnalysis) {
			// Note this also suppresses fragment support in conversation
			// feature.  (see getFragment() call at end of this method)
			return;
		}

		MPartInfo mpiBody = Mime.getBody(mMessageParts, false);

		TopLevelMessageHandler allTextHandler =
            new TopLevelMessageHandler(mMimeMessage, mMessageParts);

        int numParseErrors = 0;
        ServiceException conversionError = null;
        
		for (Iterator it = mMessageParts.iterator(); it.hasNext(); ) {
            MPartInfo mpi = (MPartInfo) it.next();
            try {
                analyzePart(mpi, mpiBody, allTextHandler);
            } catch (MimeHandlerException e) {
                numParseErrors++;
                String pn = mpi.getPartName();
                String ct = mpi.getContentType().getBaseType();
                String msgid = getMessageID();
                sLog.warn("Parse error on MIME part " + pn +
                          " (" + ct + ", Message-ID: " + msgid + ")", e);
                if (ConversionException.isTemporaryCauseOf(e) && conversionError == null) {
                    conversionError = ServiceException.FAILURE("failed to analyze part", e.getCause());
                }
            } catch (ObjectHandlerException e) {
                numParseErrors++;
                String pn = mpi.getPartName();
                String ct = mpi.getContentType().getBaseType();
                String msgid = getMessageID();
                sLog.warn("Parse error on MIME part " + pn +
                          " (" + ct + ", Message-ID: " + msgid + ")", e);
            }
		}
        if (numParseErrors > 0) {
            String msgid = getMessageID();
            String sbj = mMimeMessage.getSubject();
            sLog.warn("Message had parse errors in " + numParseErrors +
                      " parts (Message-Id: " + msgid + ", Subject: " + sbj + ")");
        }
        mLuceneDocuments.add(allTextHandler.getDocument(this));

    	for (Iterator it = mLuceneDocuments.iterator(); it.hasNext(); ) {
    		Document doc = (Document) it.next();
    		if (doc != null) {
    			// foreach doc we are adding, add domains of message
    			//msgHandler.setEmailDomainsField(doc);
    			allTextHandler.setLuceneHeadersFromContainer(doc, this);
    		}
    	}

		mFragment = allTextHandler.getFragment();
        
        // this is the first conversion error we encountered when analyzing all the parts
        // raise it at the end so that we have any calendar info parsed 
        if (conversionError != null) {
            throw conversionError;
        }
	}

    private void analyzePart(MPartInfo mpi,
                             MPartInfo mpiBody,
                             TopLevelMessageHandler allTextHandler)
    throws MimeHandlerException,
           ObjectHandlerException,
           MessagingException,
           ServiceException {
        
        String contentTypeString = mpi.getContentTypeString();
        // ignore multipart "container" parts
        if (mpi.getContentType().match(Mime.CT_MULTIPART_WILD)) {
            return;
        }

        if (parseAppointmentInfo(mpi.getMimePart(), contentTypeString))
            allTextHandler.hasCalendarPart(true);

        MimeHandler handler = MimeHandler.getMimeHandler(contentTypeString);
        assert(handler != null);

        boolean isTextType =
            mpi.getContentType().match(Mime.CT_TEXT_WILD);
        boolean isMsgRfc822Type =
            contentTypeString.equalsIgnoreCase(Mime.CT_MESSAGE_RFC822);

        if (handler.isIndexingEnabled()) {

            handler.init(mpi.getMimePart().getDataHandler().getDataSource());
            handler.setPartName(mpi.getPartName());
            handler.setFilename(mpi.getFilename());
            try {
                handler.setMessageDigest(this.getRawDigest());
            } catch (MessagingException e) {
                throw new MimeHandlerException("unable to get message digest", e);
            } catch (IOException e) {
                throw new MimeHandlerException("unable to get message digest", e);
            }

            if (mpi == mpiBody ||
                (mIndexAttachments && !DebugConfig.disableIndexingAttachmentsTogether)) {
                // add ALL TEXT from EVERY PART to the toplevel body content.
                // This is necessary for queries with multiple words -- where
                // one word is in the body and one is in a sub-attachment.
                allTextHandler.addContent(handler.getContent(), mpi == mpiBody);
            }

            if (mIndexAttachments && !DebugConfig.disableIndexingAttachmentsSeparately &&
                !isTextType && !isMsgRfc822Type) {
                // Each non-text MIME part is also indexed as a separate
                // Lucene document.  This is necessary so that we can tell the
                // client what parts match if a search matched a particular
                // part.
                Document doc = handler.getDocument();

                if (doc != null) {
                    int partSize = mpi.mPart.getSize();
                    doc.add(Field.Text(LuceneFields.L_SIZE, Integer.toString(partSize)));
                    mLuceneDocuments.add(doc);
                }
            }
        }
    }

    /**
     * @param mimePart
     * @param contentTypeString
     * @throws MessagingException
     */
    private boolean parseAppointmentInfo(MimePart mimePart, String contentTypeString) throws MessagingException {
        if (contentTypeString.indexOf(Mime.CT_TEXT_CALENDAR) != -1) {
            try {
                CalendarBuilder calBuilder = new CalendarBuilder();
                
//                {
//                    InputStream is = mimePart.getInputStream();
//                    for (int i = is.read(); i != -1; i = is.read()) {
//                        char ch = (char)i;
//                        System.out.print(ch);
//                    }
//                    System.out.println();
//                }

                InputStream is = mimePart.getInputStream();
                
                miCalendar = calBuilder.build(is);
                // XXX: should more drastic measure be taken when iCal data cannot be retrieved?
                // e.g., bounce the mail back to the sender
            } catch (IOException e) {
                sLog.warn("error reading text/calendar mime part", e);
            } catch (ParserException e) {
                sLog.warn("error parsing text/calendar mime part", e);
                sLog.warn("\tcaused by: ", e.getCause());
            }
            return true;
        }
        return false;
    }

    private static final String FWD_TRAILER = "(fwd)";

	private static String trimPrefixes(String subject) {
		boolean matched, braced;
		while (true) {
			int i, length = subject.length();
            if (subject.endsWith(FWD_TRAILER)) {
                subject = subject.substring(0, length - FWD_TRAILER.length()).trim();
                length = subject.length();
            }
			int limit = (length > 4 ? 4 : length);
			for (matched = braced = false, i = 0; i < limit && !matched; i++) {
				char c = subject.charAt(i);
				if (c == ':') {
					matched = (i > 0);
					break;
				} else if (i == 0 && c == '[') {
					braced = true;
					limit = (length > 5 ? 5 : length);
				} else if (!Character.isLetter(c))
					break;
			}
			if (!matched)
				return subject;
			if (braced && subject.endsWith("]"))
				subject = subject.substring(i + 1, length - 1).trim();
			else
				subject = subject.substring(i + 1).trim();
		}
	}

	private void normalizeSubject() {
        if (mNormalizedSubject != null)
            return;
		mSubjectPrefixed = false;
		try {
			mNormalizedSubject = mSubject = mMimeMessage.getSubject();
		} catch (MessagingException e) { }
		if (mSubject == null)
			mNormalizedSubject = mSubject = "";
		else {
			String originalSubject = mNormalizedSubject = StringUtil.stripControlCharacters(mNormalizedSubject.trim());
			mNormalizedSubject = trimPrefixes(mNormalizedSubject);
			if (mNormalizedSubject != originalSubject)
				mSubjectPrefixed = true;
			
			// handle mailing list prefixes like "[xmlbeans-dev] Re: foo"
			if (mNormalizedSubject.startsWith("[")) {
				int endBracket = mNormalizedSubject.indexOf(']');
				if (endBracket != -1 && mNormalizedSubject.length() > endBracket + 1) {
					String realSubject = originalSubject = mNormalizedSubject.substring(endBracket + 1).trim();
					realSubject = trimPrefixes(realSubject);
					if (realSubject != originalSubject)
						mSubjectPrefixed = true;
					mNormalizedSubject = mNormalizedSubject.substring(0, endBracket + 1) + ' ' + realSubject;
				}
			}
		}
	}
    
    public static String normalize(String subject) {
        if (subject != null) {
            subject = trimPrefixes(StringUtil.stripControlCharacters(subject.trim()));
            if (subject.startsWith("[")) {
                int endBracket = subject.indexOf(']');
                if (endBracket != -1 && subject.length() > endBracket + 1)
                    subject = subject.substring(0, endBracket + 1) + ' ' + trimPrefixes(subject.substring(endBracket + 1).trim());
            }
        }
        return subject;
    }
}
