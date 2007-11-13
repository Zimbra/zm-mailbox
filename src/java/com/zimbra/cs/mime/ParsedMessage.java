/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 6. 30.
 */
package com.zimbra.cs.mime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.util.SharedByteArrayInputStream;
import javax.mail.util.SharedFileInputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.TopLevelMessageHandler;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.object.ObjectHandlerException;
import com.zimbra.cs.util.JMSession;

/**
 * @author jhahm
 */
public class ParsedMessage {

    private static Log sLog = LogFactory.getLog(ParsedMessage.class);
    private static MailDateFormat sFormat = new MailDateFormat();

    private MimeMessage mMimeMessage;
    private MimeMessage mExpandedMessage;
    private boolean mParsed = false;
    private boolean mAnalyzed = false;
    private boolean mIndexAttachments = true;

    private List<MPartInfo> mMessageParts;
    private String mRecipients;
    private String mSender;

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

    private File mRawFile;
    private byte[] mRawData;
    private String mRawDigest;
    private SharedFileInputStream mRawFileInputStream;
    private boolean mWasMutated;

    public ParsedMessage(MimeMessage msg, boolean indexAttachments) {
        this(msg, getZimbraDateHeader(msg), indexAttachments);
    }

    public ParsedMessage(MimeMessage msg, long receivedDate, boolean indexAttachments) {
        mIndexAttachments = indexAttachments;
        mMimeMessage = mExpandedMessage = msg;
        // FIXME: not running mutators yet because of exception throwing!
//      runMimeMutators();
        // must set received-date before Lucene document is initialized
        runMimeConverters();
        setReceivedDate(receivedDate);
    }

    public ParsedMessage(byte[] rawData, boolean indexAttachments) throws MessagingException {
        this(rawData, null, indexAttachments);
    }

    public ParsedMessage(byte[] rawData, Long receivedDate, boolean indexAttachments) throws MessagingException {
        mIndexAttachments = indexAttachments;
        InputStream is = new SharedByteArrayInputStream(rawData);
        mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), is);

        try {
            runMimeMutators();
            if (!wasMutated()) {
                // Not mutated, so raw data is valid and storeable.
                mRawData = rawData;
            }
        } catch (MessagingException e) {
            // mutator threw an exception, so go back to the raw and use it verbatim
            ZimbraLog.extensions.warn(
                "Error applying message mutator.  Reverting to original MIME message.", e);
            mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), is);
            mRawData = rawData;
        }
        runMimeConverters();
        // must set received-date before Lucene document is initialized
        if (receivedDate != null) {
            setReceivedDate(receivedDate);
        }
    }
    
    /**
     * Creates a <tt>ParsedMessage</tt> from a file already stored on disk.
     * @param file the file on disk.  Cannot be compressed.
     */
    public ParsedMessage(File file, Long receivedDate, boolean indexAttachments)
    throws MessagingException, IOException {
        mIndexAttachments = indexAttachments;
        SharedFileInputStream in = new SharedFileInputStream(file);
        mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), in);
        
        try {
            // Maintain reference to the file only if the content was not mutated.
            runMimeMutators();
            if (wasMutated()) {
                // Load data into memory.  This allows access to the message
                // data after close() has been called.
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                mMimeMessage.writeTo(out);
                mRawData = out.toByteArray();
                mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(
                    JMSession.getSession(), new SharedByteArrayInputStream(mRawData));
                in.close();
            } else {
                mRawFile = file;
                mRawFileInputStream = in;
            }
        } catch (Exception e) {
            ZimbraLog.extensions.warn(
                "Error applying message mutator.  Reverting to original MIME message.", e);
            mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), in);
        }

        runMimeConverters();
        
        // must set received-date before Lucene document is initialized
        if (receivedDate == null) {
            receivedDate = getZimbraDateHeader(mMimeMessage); 
        }
        setReceivedDate(receivedDate);
    }

    public boolean wasMutated() {
        return mWasMutated;
    }
    
    /**
     * Allows the caller to set the digest.  This avoids rereading the file,
     * in the case where the caller has already read it once and calculated
     * the digest.
     */
    public void setRawDigest(String digest) {
        mRawDigest = digest;
    }

    /** Applies all registered on-the-fly MIME converters to a copy of the
     *  encapsulated message (leaving the original message intact), then
     *  generates the list of message parts.
     *  
     * @return the ParsedMessage itself
     * @throws ServiceException 
     * @see #runMimeConverters() */
    private ParsedMessage parse() {
        if (mParsed)
            return this;
        mParsed = true;

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
     * @return <tt>true</tt> if a mutator altered the content or
     *   <tt>false</tt> if the encapsulated message is unchanged
     *         
     * @see MimeVisitor#registerMutator */
    private void runMimeMutators() throws MessagingException {
        mWasMutated = false;
        for (Class<? extends MimeVisitor> vclass : MimeVisitor.getMutators()) {
            try {
                mWasMutated |= vclass.newInstance().accept(mMimeMessage);
                if (mMimeMessage != mExpandedMessage)
                    ((MimeVisitor) vclass.newInstance()).accept(mExpandedMessage);
            } catch (MessagingException e) {
                throw e;
            } catch (Exception e) {
                ZimbraLog.misc.warn("exception ignored running mutator; skipping", e);
            }
        }
    }

    /** Applies all registered on-the-fly MIME converters to a copy of the
     *  encapsulated message, forking it from the original MimeMessage if any
     *  changes are made.  Thus {@link #mMimeMessage} should always contain
     *  the original message content, while {@link #mExpandedMessage} may
     *  point to a version altered by the converters.
     *   
     * @return <code>true</code> if a converter forked and altered the encapsulated
     * message, <code>false</code> if the encapsulated message is unchanged.
     *         
     * @see MimeVisitor#registerConverter */
    private boolean runMimeConverters() {
        // this callback copies the MimeMessage if a converter would want 
        //   to make a change, but doesn't alter the original MimeMessage
        MimeVisitor.ModificationCallback forkCallback = new MimeVisitor.ModificationCallback() {
            public boolean onModification() {
                try { forkMimeMessage(); } catch (Exception e) { }  return false;
            } };

        try {
            // first, find out if *any* of the converters would be triggered (but don't change the message)
            for (Class<? extends MimeVisitor> vclass : MimeVisitor.getConverters()) {
                if (mExpandedMessage == mMimeMessage)
                    vclass.newInstance().setCallback(forkCallback).accept(mMimeMessage);
                // if there are attachments to be expanded, expand them in the MimeMessage *copy*
                if (mExpandedMessage != mMimeMessage)
                    vclass.newInstance().accept(mExpandedMessage);
            }
        } catch (Exception e) {
            // roll back if necessary
            mExpandedMessage = mMimeMessage;
            sLog.warn("exception while converting message; message will be analyzed unconverted", e);
        }

        return mExpandedMessage != mMimeMessage;
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
        if (mMimeMessage != null) {
            return mMimeMessage;
        }
        // Reference to MimeMessage was dropped by close().
        try {
            InputStream in;
            if (mRawData != null) {
                // Constructed from a byte array, or constructed from a file and then mutated.
                in = new SharedByteArrayInputStream(mRawData);
            } else if (mRawFile != null) {
                // Constructed from a file and not mutated.
                in = new SharedFileInputStream(mRawFile);
            } else {
                assert(false);
                ZimbraLog.mailbox.warn("Data not available for MimeMessage.  Returning null.");
                return null;
            }
            mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), in);
            runMimeConverters();
        } catch (IOException e) {
            ZimbraLog.mailbox.warn("Unable to create MimeMessage.", e);
        } catch (MessagingException e) {
            ZimbraLog.mailbox.warn("Unable to create MimeMessage.", e);
        }
        return mMimeMessage;
    }

    void forkMimeMessage() throws MessagingException {
        mExpandedMessage = new Mime.FixedMimeMessage(mMimeMessage);
    }

    /**
     * Returns the size of the original MIME message.
     */
    public int getRawSize() throws IOException, MessagingException {
        if (mRawData != null) {
            return mRawData.length;
        }
        if (mRawFile != null) {
            return (int) mRawFile.length();
        }
        if (mMimeMessage != null) {
            int size = mMimeMessage.getSize();
            if (size < 0) {
                return getRawData().length;
            }
        }
        ZimbraLog.mailbox.warn("%s.getRawSize(): Unable to get MIME message data..",
            ParsedMessage.class.getSimpleName());
        return 0;
    }

    /**
     * Returns the original MIME message data.
     */
    public byte[] getRawData() throws IOException, MessagingException {
        if (mRawData != null) {
            return mRawData;
        }
        if (mRawFile != null) {
            mRawData = ByteUtil.getContent(mRawFile);
            return mRawData;
        }
        if (mMimeMessage != null) {
            // Note that this will result in two copies of the message data: one
            // referenced by the MimeMessage and one referenced by mRawData.
            int size = mMimeMessage.getSize();
            ByteArrayOutputStream baos;
            if (size > 0) {
                baos = new ByteArrayOutputStream(size);
            } else {
                baos = new ByteArrayOutputStream();
            }
            mMimeMessage.writeTo(baos);
            mRawData = baos.toByteArray();
            return mRawData;
        }
        ZimbraLog.mailbox.warn("%s.getRawData(): Unable to get MIME message data.",
            ParsedMessage.class.getSimpleName());
        return null;
    }

    /**
     * Returns the SHA1 digest of the original MIME message data, encoded as base64.
     */
    public String getRawDigest() throws IOException, MessagingException {
        if (mRawDigest != null) {
            return mRawDigest;
        }
        if (mRawData != null) {
            mRawDigest = ByteUtil.getSHA1Digest(mRawData, true);
        } else if (mRawFile != null) {
            InputStream in = new FileInputStream(mRawFile);
            mRawDigest = ByteUtil.getSHA1Digest(in, true);
            in.close();
        } else if (mMimeMessage != null) {
            mRawData = getByteArray(mMimeMessage);
            mRawDigest = ByteUtil.getSHA1Digest(mRawData, true);
        } else {
            ZimbraLog.mailbox.warn("%s.getRawDigest(): Cannot calculate digest because message data is not available.",
                ParsedMessage.class.getSimpleName());
        }
        return mRawDigest;
    }
    
    public InputStream getInputStream() throws ServiceException {
        if (mRawData != null) {
            return new ByteArrayInputStream(mRawData);
        }
        try {
            if (mRawFile != null) {
                return new FileInputStream(mRawFile);
            }
            if (mMimeMessage != null) {
                mRawData = getByteArray(mMimeMessage);
                return new ByteArrayInputStream(mRawData);
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to get InputStream.", e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Unable to get InputStream.", e);
        }
        
        ZimbraLog.mailbox.warn("%s.getInputStream(): Unable to get input stream because message data is not available.",
            ParsedMessage.class.getSimpleName());
        return null;
    }
    
    private byte[] getByteArray(MimeMessage mm)
    throws IOException, MessagingException {
        // Note: MimeMessage.getContentStream() will throw an exception if
        // the MimeMessage was constructed programmatically, as opposed to from
        // a blob.  We must therefore instantiate the blob in order to calculate the
        // digest.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mMimeMessage.writeTo(out);
        return out.toByteArray();
    }

    public boolean isAttachmentIndexingEnabled() {
        return mIndexAttachments;
    }

    public List<MPartInfo> getMessageParts() {
        parse();
        return mMessageParts;
    }

    public boolean hasAttachments() {
        parse();
        return mHasAttachments;
    }

    public int getPriorityBitmask() {
        parse();

        try {
            String xprio = getMimeMessage().getHeader("X-Priority", null);
            if (xprio != null) {
                xprio = xprio.trim();
                if (xprio.startsWith("1") || xprio.startsWith("2"))
                    return Flag.BITMASK_HIGH_PRIORITY;
                else if (xprio.startsWith("3"))
                    return 0;
                else if (xprio.startsWith("4") || xprio.startsWith("5"))
                    return Flag.BITMASK_LOW_PRIORITY;
            }
        } catch (MessagingException me) { }

        try {
            String impt = getMimeMessage().getHeader("Importance", null);
            if (impt != null) {
                impt = impt.trim().toLowerCase();
                if (impt.startsWith("high"))
                    return Flag.BITMASK_HIGH_PRIORITY;
                else if (impt.startsWith("normal"))
                    return 0;
                else if (impt.startsWith("low"))
                    return Flag.BITMASK_LOW_PRIORITY;
            }
        } catch (MessagingException me) { }

        return 0;
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
            String msgid = getMimeMessage().getMessageID();
            return ("".equals(msgid) ? null : msgid);
        } catch (MessagingException me) {
            return null;
        }
    }

    /**
     * Returns the decoded header for this header name, only the first header is returned.
     */
    public String getHeader(String headerName) {
        try {
            String value = getMimeMessage().getHeader(headerName, null);
            if (value == null || value.length() == 0)
                return "";
            try {
                value = MimeUtility.decodeText(value);
            } catch (UnsupportedEncodingException e) { }

            return value;
        } catch (MessagingException e) {
            return "";
        }
    }
    
    /**
     * Returns the decoded headers for this header name, all headers for this header name are returned.
     */
    public String[] getHeaders(String headerName) {
        try {
            String value = getMimeMessage().getHeader(headerName, ",");
            if (value == null || value.length() == 0)
                return null;
            
            String[] values = value.split(",");
            for (int i=0; i<values.length; i++) {
                try {
                    values[i] = MimeUtility.decodeText(values[i]);
                } catch (UnsupportedEncodingException e) {
                    // values[i] would contain the undecoded value, fine
                }
            }

            return values;
        } catch (MessagingException e) {
            return null;
        }
    }
    
    
    public String getRecipients() {
        if (mRecipients == null) {
            String recipients = null;
            try {
                recipients = getMimeMessage().getHeader("To", ", ");
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
                sender = getMimeMessage().getHeader("From", null);
            } catch (MessagingException e) {}
            if (sender == null) {
                try {
                    sender = getMimeMessage().getHeader("Sender", null);
                } catch (MessagingException e) {}
            }
            if (sender == null)
                sender = "";
            try {
                mSender = MimeUtility.decodeText(sender);
            } catch (UnsupportedEncodingException e) {
                mSender = sender;
            }
        }
        return mSender;
    }

    public String getSenderEmail() {
        try {
            Address[] froms = getMimeMessage().getFrom();
            if (froms != null && froms.length > 0 && froms[0] instanceof InternetAddress)
                return ((InternetAddress) froms[0]).getAddress();
            Address sender = getMimeMessage().getSender();
            if (sender instanceof InternetAddress)
                return ((InternetAddress) sender).getAddress();
        } catch (MessagingException e) {}
        return null;
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
            replyTo = getMimeMessage().getHeader("Reply-To", null);
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
            Date dateHeader = getMimeMessage().getSentDate();
            if (dateHeader != null) {
                // prevent negative dates, which Lucene can't deal with
                mDateHeader = Math.max(dateHeader.getTime(), 0);
            }
        } catch (MessagingException e) { }

        return mDateHeader;
    }

    private void setReceivedDate(long date) {
        // round to nearest second...
        if (date != -1)
            mReceivedDate = (Math.max(0, date) / 1000) * 1000;
    }
    public long getReceivedDate() {
        if (mReceivedDate == -1)
            mReceivedDate = System.currentTimeMillis();
        assert(mReceivedDate >= 0);  
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

        Set<MPartInfo> mpiBodies = Mime.getBody(mMessageParts, false);

        TopLevelMessageHandler allTextHandler = new TopLevelMessageHandler(mMessageParts);

        int numParseErrors = 0;
        ServiceException conversionError = null;
        String reportRoot = null;

        for (MPartInfo mpi : mMessageParts) {
            // text/calendar parts under a multipart/report aren't considered real calendar invites
            String partName = mpi.mPartName;
            if (reportRoot != null && !partName.startsWith(reportRoot))
                reportRoot = null;
            if (reportRoot == null && mpi.getContentType().equals(Mime.CT_MULTIPART_REPORT))
                reportRoot = partName.endsWith("TEXT") ? partName.substring(0, partName.length() - 4) : partName + ".";

            try {
                analyzePart(mpi, mpiBodies, allTextHandler, reportRoot != null);
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
        if (miCalendar != null) {
            allTextHandler.hasCalendarPart(true);
        }
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
        if (conversionError != null)
            throw conversionError;
    }

    private void analyzePart(MPartInfo mpi, Set<MPartInfo> mpiBodies, TopLevelMessageHandler allTextHandler, boolean ignoreCalendar)
    throws MimeHandlerException, ObjectHandlerException, MessagingException, ServiceException {
        String ctype = mpi.getContentType();
        // ignore multipart "container" parts
        if (ctype.startsWith(Mime.CT_MULTIPART_PREFIX))
            return;

        MimeHandler handler = MimeHandlerManager.getMimeHandler(ctype, mpi.getFilename());
        assert(handler != null);

        Mime.repairTransferEncoding(mpi.getMimePart());

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
            if (!ignoreCalendar && miCalendar == null)
                miCalendar = handler.getICalendar();

            boolean isMainBody = mpiBodies.contains(mpi);
            boolean autoInclude = isMainBody && (!handler.runsExternally() || mIndexAttachments);
            if (autoInclude || (mIndexAttachments && !DebugConfig.disableIndexingAttachmentsTogether)) {
                // add ALL TEXT from EVERY PART to the toplevel body content.
                // This is necessary for queries with multiple words -- where
                // one word is in the body and one is in a sub-attachment.
                //
                // If attachment indexing is disabled, then we only add the main body and
                // text parts...
                allTextHandler.addContent(handler.getContent(), isMainBody);
            }

            if (mIndexAttachments && !DebugConfig.disableIndexingAttachmentsSeparately) {
                // Each non-text MIME part is also indexed as a separate
                // Lucene document.  This is necessary so that we can tell the
                // client what parts match if a search matched a particular
                // part.
                Document doc = handler.getDocument();
                if (doc != null) {
                    int partSize = mpi.getMimePart().getSize();
                    doc.add(new Field(LuceneFields.L_SIZE, Integer.toString(partSize), Field.Store.YES, Field.Index.NO));
                    mLuceneDocuments.add(doc);
                }
            }
        }

        // make sure we've got the text/calendar handler installed
        if (!ignoreCalendar && miCalendar == null && ctype.equals(Mime.CT_TEXT_CALENDAR)) {
            if (handler.isIndexingEnabled())
                ZimbraLog.index.warn("TextCalendarHandler not correctly installed");

            InputStream is = null;
            try {
                String charset = mpi.getContentTypeParameter(Mime.P_CHARSET);
                if (charset == null || charset.trim().equals(""))
                    charset = Mime.P_CHARSET_DEFAULT;

                Reader reader = new InputStreamReader(is = mpi.getMimePart().getInputStream(), charset);
                miCalendar = ZCalendarBuilder.build(reader);
            } catch (IOException ioe) {
                ZimbraLog.index.warn("error reading text/calendar mime part", ioe);
            } finally {
                ByteUtil.closeStream(is);
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
            mNormalizedSubject = mSubject = getMimeMessage().getSubject();
        } catch (MessagingException e) { }

        if (mSubject == null) {
            mNormalizedSubject = mSubject = "";
        } else {
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
            if (mNormalizedSubject.length() > DbMailItem.MAX_SUBJECT_LENGTH)
                mNormalizedSubject = mNormalizedSubject.substring(0, DbMailItem.MAX_SUBJECT_LENGTH).trim();
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
        subject = compressWhitespace(subject);
        if (subject != null && subject.length() > DbMailItem.MAX_SUBJECT_LENGTH)
            subject = subject.substring(0, DbMailItem.MAX_SUBJECT_LENGTH).trim();

        return subject;
    }
    
    /**
     * If this <tt>ParsedMessage</tt> references a file on disk, closes
     * the file and drops references to the encapsulated <tt>MimeMessage</tt>.
     */
    public void close()
    throws IOException {
        if (mRawFile != null) {
            mMimeMessage = null;
            mExpandedMessage = null;
            if (mRawFileInputStream != null) {
                mRawFileInputStream.close();
                mRawFileInputStream = null;
            }
        }
    }
    
    /**
     * Tells this <tt>ParsedMessage</tt> to get its data from a new file.
     * Only applies to <tt>ParsedMessage</tt>s that were instantiated from
     * a file and not mutated.
     */
    public void fileMoved(File newFile)
    throws IOException {
        if (mRawFile != null) {
            mMimeMessage = null;
            mExpandedMessage = null;
            if (mRawFileInputStream != null) {
                mRawFileInputStream.close();
                mRawFileInputStream = null;
            }
        }
        mRawFile = newFile;
    }
}
