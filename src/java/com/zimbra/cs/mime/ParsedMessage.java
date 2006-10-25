//depot/main/ZimbraServer/src/java/com/zimbra/cs/mime/ParsedMessage.java#34 - edit change 25120 (ktext)
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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.*;

import javax.mail.MessagingException;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.TopLevelMessageHandler;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.object.ObjectHandlerException;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author jhahm
 */
public class ParsedMessage {

    private static Log sLog = LogFactory.getLog(ParsedMessage.class);
    private static MailDateFormat sFormat = new MailDateFormat();

    private MimeMessage mMimeMessage;
    private MimeMessage mExpandedMessage;
    private boolean mMutatorsRun = false;
    private boolean mParsed = false;
    private boolean mAnalyzed = false;
    private boolean mIndexAttachments = true;

    private List<MPartInfo> mMessageParts;
    private String mRecipients;
    private String mSender;
    
    private String mEnvFrom; // x-env-from
    private String mEnvTo; // x-env-to
    
    private ParsedAddress mParsedSender;
	private boolean mHasAttachments = false;
	private String mFragment = "";
	private long mDateHeader = -1;
    private long mReceivedDate = -1;
    private String mSubject;
    private String mNormalizedSubject;
	private boolean mSubjectPrefixed;
	private List<Document> mLuceneDocuments;
	private ZVCalendar miCalendar;

    // m*Raw* should only be assigned by setRawData so these fields can kept in sync
    private byte[] mRawData;
    private String mRawDigest;
    private boolean mHaveRaw;


    public ParsedMessage(MimeMessage msg, boolean indexAttachments) {
        this(msg, getZimbraDateHeader(msg), indexAttachments);
	}

    public ParsedMessage(MimeMessage msg, long receivedDate, boolean indexAttachments) {
        mIndexAttachments = indexAttachments;
        mMimeMessage = mExpandedMessage = msg;
        // FIXME: not running mutators yet because of exception throwing!
//        runMimeMutators();
        // must set received-date before Lucene document is initialized
        setReceivedDate(receivedDate);
    }

    public ParsedMessage(byte[] rawData, boolean indexAttachments) throws MessagingException {
        mIndexAttachments = indexAttachments;
        parseRawData(rawData);

        // if there are no mutators, the raw data is valid and storeable
        try {
            if (runMimeMutators())
                setRawData(rawData);
        } catch (MessagingException e) {
            // mutator threw an exception, so go back to the raw and use it verbatim
            ZimbraLog.extensions.warn("error applying message mutator; using raw instead", e);
            parseRawData(rawData);
            setRawData(rawData);
        }
        // must set received-date before Lucene document is initialized
        setReceivedDate(getZimbraDateHeader(mMimeMessage));
    }

    public ParsedMessage(byte[] rawData, long receivedDate, boolean indexAttachments) throws MessagingException {
        mIndexAttachments = indexAttachments;
        parseRawData(rawData);

        // if there are no mutators, the raw data is valid and storeable
        try {
            if (runMimeMutators())
                setRawData(rawData);
        } catch (MessagingException e) {
            // mutator threw an exception, so go back to the raw and use it verbatim
            ZimbraLog.extensions.warn("error applying message mutator; using raw instead", e);
            parseRawData(rawData);
            setRawData(rawData);
        }
        // must set received-date before Lucene document is initialized
        setReceivedDate(receivedDate);
    }

    private void parseRawData(byte[] rawData) throws MessagingException {
        ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
        mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), bais);
        try {
            bais.close();
        } catch (IOException ioe) {
            // we know ByteArrayInputStream.close() is a no-op
        }
    }


    /** Applies all registered on-the-fly MIME converters to a copy of the
     *  encapsulated message (leaving the original message intact), then
     *  generates the list of message parts.
     *  
     * @return the ParsedMessage itself
     * @throws ServiceException 
     * @see #runMimeConverters() */
	private ParsedMessage parse() throws ServiceException {
        if (mParsed)
            return this;
        mParsed = true;

        // apply any mutators that affect the on-disk representation of the message
        try {
            runMimeMutators();
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("error applying message mutator", e);
        }
        // do an on-the-fly temporary expansion of the raw message (uudecode, tnef-decode, etc.)
        runMimeConverters();
		try {
            mMessageParts = Mime.getParts(mExpandedMessage);
            mHasAttachments = Mime.hasAttachment(mMessageParts);
        } catch (Exception e) {
            sLog.warn("exception while parsing message; message will not be indexed", e);
            mMessageParts = new ArrayList<MPartInfo>();
        }
        return this;
    }

    /** Applies all registered MIME mutators to the encapsulated message.
     *  The message is not forked, so both {@link #mMimeMessage} and
     *  {@link #mExpandedMessage} are affected by the changes.
     *  
     * @return <code>true</code> if the encapsulated message is unchanged,
     *         <code>false</code> if a mutator altered the content
     * @see MimeVisitor#registerMutator(Class) */
    private boolean runMimeMutators() throws MessagingException {
        if (mMutatorsRun)
            return true;
        boolean rawInvalid = false;
        for (Class vclass : MimeVisitor.getMutators())
            try {
                rawInvalid |= ((MimeVisitor) vclass.newInstance()).accept(mMimeMessage);
                if (mMimeMessage != mExpandedMessage)
                    ((MimeVisitor) vclass.newInstance()).accept(mExpandedMessage);
            } catch (MessagingException e) {
                throw e;
            } catch (Exception e) {
                ZimbraLog.misc.warn("exception ignored running mutator; skipping", e);
            }
        mMutatorsRun = true;
        return !rawInvalid;
    }

    /** Applies all registered on-the-fly MIME converters to a copy of the
     *  encapsulated message, forking it from the original MimeMessage if any
     *  changes are made.  Thus {@link #mMimeMessage} should always contain
     *  the original message content, while {@link #mExpandedMessage} may
     *  point to a version altered by the converters.
     *   
     * @return <code>true</code> if the encapsulated message is unchanged,
     *         <code>false</code> if a converter forked and altered it
     * @see MimeVisitor#registerConverter(Class) */
    private boolean runMimeConverters() {
        // this callback copies the MimeMessage if a converter would want 
        //   to make a change, but doesn't alter the original MimeMessage
        MimeVisitor.ModificationCallback forkCallback = new MimeVisitor.ModificationCallback() {
            public boolean onModification() {
                try { forkMimeMessage(); } catch (Exception e) { }  return false;
            } };

        try {
            // first, find out if *any* of the converters would be triggered (but don't change the message)
            for (Class vclass : MimeVisitor.getConverters()) {
                if (mExpandedMessage == mMimeMessage)
                    ((MimeVisitor) vclass.newInstance()).setCallback(forkCallback).accept(mMimeMessage);
                // if there are attachments to be expanded, expand them in the MimeMessage *copy*
                if (mExpandedMessage != mMimeMessage)
                    ((MimeVisitor) vclass.newInstance()).accept(mExpandedMessage);
            }
        } catch (Exception e) {
            // roll back if necessary
            mExpandedMessage = mMimeMessage;
            sLog.warn("exception while converting message; message will be analyzed unconverted", e);
        }

        return mExpandedMessage == mMimeMessage;
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

    void forkMimeMessage() throws MessagingException {
        mExpandedMessage = new Mime.FixedMimeMessage(mMimeMessage);
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

        // mutate the message *before* generating its byte[] representation
        runMimeMutators();

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

    public List<MPartInfo> getMessageParts() throws ServiceException {
        parse();
    	return mMessageParts;
    }

	public boolean hasAttachments() throws ServiceException {
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

    private static long getZimbraDateHeader(MimeMessage mm) {
        String zimbraHeader = null;
        try {
            zimbraHeader = mm.getHeader("X-Zimbra-Received", null);
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

    public List<Document> getLuceneDocuments() {
        try {
            analyze();
        } catch (ServiceException e) {
            sLog.warn("message analysis failed when getting lucene documents");
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
    public ZVCalendar getiCalendar() {
        try {
            analyze();
        } catch (ServiceException e) {
            // the calendar info should still be parsed 
            sLog.warn("Message analysis failed when getting calendar info");
        }
        return miCalendar;
    }
    
	private void analyzeMessage() throws MessagingException, ServiceException {
        if (mAnalyzed)
            return;
        mAnalyzed = true;

        mLuceneDocuments = new ArrayList<Document>();

        if (DebugConfig.disableMessageAnalysis) {
			// Note this also suppresses fragment support in conversation
			// feature.  (see getFragment() call at end of this method)
			return;
		}

		MPartInfo mpiBody = Mime.getBody(mMessageParts, false);

		TopLevelMessageHandler allTextHandler = new TopLevelMessageHandler(mMessageParts);

        int numParseErrors = 0;
        ServiceException conversionError = null;

        for (MPartInfo mpi : mMessageParts) {
            try {
                analyzePart(mpi, mpiBody, allTextHandler);
            } catch (MimeHandlerException e) {
                numParseErrors++;
                String pn = mpi.getPartName();
                String ctype = mpi.getContentType();
                String msgid = getMessageID();
                sLog.warn("Parse error on MIME part " + pn +
                          " (" + ctype + ", Message-ID: " + msgid + ")", e);
                if (ConversionException.isTemporaryCauseOf(e) && conversionError == null) {
                    conversionError = ServiceException.FAILURE("failed to analyze part", e.getCause());
                }
            } catch (ObjectHandlerException e) {
                numParseErrors++;
                String pn = mpi.getPartName();
                String ct = mpi.getContentType();
                String msgid = getMessageID();
                sLog.warn("Parse error on MIME part " + pn +
                          " (" + ct + ", Message-ID: " + msgid + ")", e);
            }
		}
        if (miCalendar != null)
            allTextHandler.hasCalendarPart(true);
        if (numParseErrors > 0) {
            String msgid = getMessageID();
            String sbj = getSubject();
            sLog.warn("Message had parse errors in " + numParseErrors +
                      " parts (Message-Id: " + msgid + ", Subject: " + sbj + ")");
        }
        mLuceneDocuments.add(allTextHandler.getDocument(this));

        for (Document doc : mLuceneDocuments) {
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

    private void analyzePart(MPartInfo mpi, MPartInfo mpiBody, TopLevelMessageHandler allTextHandler)
    throws MimeHandlerException, ObjectHandlerException, MessagingException, ServiceException {
        String ctype = mpi.getContentType();
        // ignore multipart "container" parts
        if (ctype.startsWith(Mime.CT_MULTIPART_PREFIX))
            return;

        MimeHandler handler = MimeHandler.getMimeHandler(ctype);
        assert(handler != null);

        if (handler.isIndexingEnabled()) {
            handler.init(mpi.getMimePart().getDataHandler().getDataSource());
            handler.setPartName(mpi.getPartName());
            handler.setFilename(mpi.getFilename());
            try {
                handler.setMessageDigest(getRawDigest());
            } catch (MessagingException e) {
                throw new MimeHandlerException("unable to get message digest", e);
            } catch (IOException e) {
                throw new MimeHandlerException("unable to get message digest", e);
            }

            // remember the first iCalendar attachment
            if (miCalendar == null)
                miCalendar = handler.getICalendar();

            if (mpi == mpiBody ||
                    (mIndexAttachments && !DebugConfig.disableIndexingAttachmentsTogether)) {
                // add ALL TEXT from EVERY PART to the toplevel body content.
                // This is necessary for queries with multiple words -- where
                // one word is in the body and one is in a sub-attachment.
                //
                // If attachment indexing is disabled, then we only add the main body and
                // text parts...
                allTextHandler.addContent(handler.getContent(), mpi == mpiBody);
            }

            if (mIndexAttachments && !DebugConfig.disableIndexingAttachmentsSeparately) {
                // Each non-text MIME part is also indexed as a separate
                // Lucene document.  This is necessary so that we can tell the
                // client what parts match if a search matched a particular
                // part.
                Document doc = handler.getDocument();
                if (doc != null) {
                    int partSize = mpi.getMimePart().getSize();
                    doc.add(Field.Text(LuceneFields.L_SIZE, Integer.toString(partSize)));
                    mLuceneDocuments.add(doc);
                }
            }
        }

        // make sure we've got the text/calendar handler installed
        if (miCalendar == null && ctype.equals(Mime.CT_TEXT_CALENDAR)) {
            if (handler.isIndexingEnabled())
                ZimbraLog.index.warn("TextCalendarHandler not correctly installed");
            try {
                String charset = mpi.getContentTypeParameter(Mime.P_CHARSET);
                if (charset == null || charset.trim().equals(""))
                    charset = Mime.P_CHARSET_DEFAULT;
                Reader reader = new InputStreamReader(mpi.getMimePart().getInputStream(), charset);
                miCalendar = ZCalendarBuilder.build(reader);
            } catch (IOException ioe) {
                ZimbraLog.index.warn("error reading text/calendar mime part", ioe);
            }
        }
    }

    // these *should* be taken from a properties file
    private static final Set<String> CALENDAR_PREFIXES = new HashSet<String>(Arrays.asList(new String[] {
            "Accept:", "Accepted:", "Decline:", "Declined:", "Tentative:", "Cancelled:", "CANCELLED:", "New Time Proposed:"
    }));
    private static final String FWD_TRAILER = "(fwd)";

    private static final int MAX_PREFIX_LENGTH = 3;

	private static String trimPrefixes(String subject) {
		while (true) {
			int length = subject.length();
            // first, strip off any "(fwd)" at the end
            while (subject.endsWith(FWD_TRAILER)) {
                subject = subject.substring(0, length - FWD_TRAILER.length()).trim();
                length = subject.length();
            }
            if (length == 0)
                return subject;

            // find the first ':' in the subject
    		boolean braced = subject.charAt(0) == '[';
            int colon = subject.indexOf(':');
            if (colon <= (braced ? 1 : 0))
                return subject;

            // figure out if it's either a known calendar response prefix or a 1-3 letter prefix
            String prefix = subject.substring(braced ? 1 : 0, colon + 1);
            boolean matched = true;
            if (CALENDAR_PREFIXES.contains(prefix))
                matched = true;
            else {
                // make sure to catch "re(2):" and "fwd[5]:" as well...
                int paren = -1;
            	for (int i = 0; matched && i < prefix.length() - 1; i++) {
                    char c = prefix.charAt(i);
                    if ((c == '(' || c == '[') && i > 0 && paren == -1)
                        paren = i;
                    else if ((c == ')' || c == ']') && paren != -1)
                        matched &= i > paren + 1 && i == prefix.length() - 2;
                    else if (!Character.isLetter(c))
                    	matched &= c >= '0' && c <= '9' && paren != -1;
                    else if (i >= MAX_PREFIX_LENGTH || paren != -1)
                        matched = false;
            	}
            }

			if (!matched)
				return subject;
			if (braced && subject.endsWith("]"))
				subject = subject.substring(colon + 1, length - 1).trim();
			else
				subject = subject.substring(colon + 1).trim();
		}
	}

    private static String compressWhitespace(String value) {
        if (value == null || value.equals(""))
            return value;
        StringBuffer sb = new StringBuffer();
        for (int i = 0, len = value.length(), last = -1; i < len; i++) {
            char c = value.charAt(i);
            if (c <= ' ') {
                c = ' ';
                if (c == last)
                    continue;
            }
            sb.append((char) (last = c));
        }
        return sb.toString();
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
			String originalSubject = mNormalizedSubject = StringUtil.stripControlCharacters(mNormalizedSubject).trim();
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

            mNormalizedSubject = compressWhitespace(mNormalizedSubject);
		}
	}

    public static String normalize(String subject) {
        if (subject != null) {
            subject = trimPrefixes(StringUtil.stripControlCharacters(subject).trim());
            if (subject.startsWith("[")) {
                int endBracket = subject.indexOf(']');
                if (endBracket != -1 && subject.length() > endBracket + 1)
                    subject = subject.substring(0, endBracket + 1) + ' ' + trimPrefixes(subject.substring(endBracket + 1).trim());
            }
        }
        return compressWhitespace(subject);
    }
}
