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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.util.SharedByteArrayInputStream;

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
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.object.ObjectHandlerException;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.util.JMSession;

/**
 * Instantiates a JavaMail <tt>MimeMessage</tt> from a <tt>byte[]</tt> or
 * file on disk.  Converts or mutates the message as necessary by calling
 * registered instances of {@link MimeVisitor}.  Conversion modifies the in-
 * memory message without affecting the raw version.  Mutation modifies the
 * raw version and affects results returned by the <tt>getRawXXX</tt> methods.
 * @author jhahm
 */
public class ParsedMessage {

    private static Log sLog = LogFactory.getLog(ParsedMessage.class);
    private static MailDateFormat sFormat = new MailDateFormat();

    private MimeMessage mMimeMessage;
    private MimeMessage mExpandedMessage;
    private boolean mParsed = false;
    private boolean mAnalyzedBodyParts = false;
    private boolean mAnalyzedNonBodyParts = false;
    private String mBodyContent = "";
    private final boolean mIndexAttachments;
    private int mNumParseErrors = 0;

    /** if TRUE then there was a _temporary_ failure analyzing the message.  We should attempt
     * to re-index this message at a later time */
    private boolean mTemporaryAnalysisFailure = false;

    private List<MPartInfo> mMessageParts;
    private String mRecipients;
    private String mSender;

    private ParsedAddress mParsedSender;
    private boolean mHasAttachments = false;
    private boolean mHasTextCalendarPart = false;
    private String mFragment = "";
    private long mDateHeader = -1;
    private long mReceivedDate = -1;
    private String mSubject;
    private String mNormalizedSubject;
    private boolean mSubjectPrefixed;
    private List<Document> mLuceneDocuments = new ArrayList<Document>();
    private ZVCalendar mCalendar;

    private File mRawFile;
    private byte[] mRawData;
    private String mRawDigest;
    private BlobInputStream mRawFileInputStream;
    private boolean mWasMutated;

    public ParsedMessage(MimeMessage msg, boolean indexAttachments) {
        this(msg, getZimbraDateHeader(msg), indexAttachments);
    }

    public ParsedMessage(MimeMessage msg, long receivedDate, boolean indexAttachments) {
        mIndexAttachments = indexAttachments;
        mMimeMessage = mExpandedMessage = msg;

        if (MimeVisitor.anyMutatorsRegistered()) {
            MimeMessage backup = null;
            try {
                backup = new Mime.FixedMimeMessage(msg);
                runMimeMutators();
            } catch (MessagingException e) {
                ZimbraLog.extensions.warn("Error applying message mutator.  Reverting to original MIME message.", e);
                if (backup != null)
                    mMimeMessage = mExpandedMessage = backup;
            }
        }
        runMimeConverters();
        // must set received-date before Lucene document is initialized
        setReceivedDate(receivedDate);
    }

    public ParsedMessage(byte[] rawData, boolean indexAttachments) throws MessagingException {
        this(rawData, null, indexAttachments);
    }

    public ParsedMessage(byte[] rawData, Long receivedDate, boolean indexAttachments) throws MessagingException {
        if (rawData == null || rawData.length == 0)
            throw new MessagingException("Message data cannot be null or empty.");

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
            ZimbraLog.extensions.warn("Error applying message mutator.  Reverting to original MIME message.", e);
            mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(rawData));
            mRawData = rawData;
        }
        runMimeConverters();
        // must set received-date before Lucene document is initialized
        if (receivedDate == null)
            receivedDate = getZimbraDateHeader(mMimeMessage);
        setReceivedDate(receivedDate);
    }
    
    /**
     * Creates a <tt>ParsedMessage</tt> from a file already stored on disk.
     * @param file the file on disk.  Cannot be compressed.
     */
    public ParsedMessage(File file, Long receivedDate, boolean indexAttachments)
    throws MessagingException, IOException {
        if (file == null) {
            throw new IOException("File cannot be null.");
        }
        if (file.length() == 0) {
            throw new IOException("File " + file.getPath() + " is empty.");
        }
        mIndexAttachments = indexAttachments;
        BlobInputStream in = new BlobInputStream(file);
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
     * Returns <tt>true</tt> if this message is being streamed from
     * disk and does not require MIME expansion.
     */
    public boolean isStreamedFromDisk() {
        return (mRawFile != null && (mMimeMessage == mExpandedMessage));
    }
    
    /**
     * Allows the caller to set the digest of the raw message.  This avoids
     * rereading the file, in the case where the caller has already read it
     * once and calculated the digest.
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
            mHasTextCalendarPart = Mime.hasTextCalenndar(mMessageParts);
        } catch (Exception e) {
            ZimbraLog.index.warn("exception while parsing message; message will not be indexed", e);
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
    
    /**
     * Copies the <tt>MimeMessage</tt> if a converter would want 
     * to make a change, but doesn't alter the original MimeMessage.
     */
    private class ForkMimeMessage implements MimeVisitor.ModificationCallback {
        private boolean mForked = false;

        public boolean onModification() {
            if (mForked)
                return false;

            try {
                mForked = true;
                mExpandedMessage = new Mime.FixedMimeMessage(mMimeMessage);
            } catch (Exception e) {
                sLog.warn("Unable to fork MimeMessage.", e);
            }
            return false;
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
        try {
            MimeVisitor.ModificationCallback forkCallback = new ForkMimeMessage();
            
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

    /**
     * Analyze and extract text from all the "body" (non-attachment) parts of the message.
     * This step is required to properly generate the message fragment.
     * 
     * @throws ServiceException
     */
    private void analyzeBodyParts() throws ServiceException {
        if (mAnalyzedBodyParts)
            return;
        mAnalyzedBodyParts = true;
        if (DebugConfig.disableMessageAnalysis)
            return;

        parse();
        
        try {
            Set<MPartInfo> mpiBodies = Mime.getBody(mMessageParts, false);

            // extract text from the "body" parts
            StringBuilder bodyContent = new StringBuilder();
            {
                String reportRoot = null;
                for (MPartInfo mpi : mMessageParts) {
                    // text/calendar parts under a multipart/report aren't considered real calendar invites
                    String partName = mpi.mPartName;
                    if (reportRoot != null && !mpi.mPartName.startsWith(reportRoot))
                        reportRoot = null;
                    if (reportRoot == null && mpi.getContentType().equals(Mime.CT_MULTIPART_REPORT)) {
                        reportRoot = mpi.mPartName.endsWith("TEXT") ? mpi.mPartName.substring(0, partName.length() - 4) : mpi.mPartName + ".";
                    }
                    boolean isMainBody = mpiBodies.contains(mpi);
                    if (isMainBody) {
                        String toplevelText = analyzePart(isMainBody, mpi, reportRoot != null);
                        if (toplevelText.length() > 0)
                            appendToContent(bodyContent, toplevelText);
                    }
                }
                // Calculate the fragment -- requires body content
                mBodyContent = bodyContent.toString().trim();
                mFragment = Fragment.getFragment(mBodyContent, mHasTextCalendarPart);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            sLog.warn("exception while analyzing message; message will be partially indexed", e);
        }
    }
    
    /**
     * Analyze and extract text from all attachments parts of the message 
     *   
     * @throws ServiceException
     */
    private void analyzeNonBodyParts() throws ServiceException {
        if (mAnalyzedNonBodyParts)
            return;
        mAnalyzedNonBodyParts = true;
        if (DebugConfig.disableMessageAnalysis)
            return;
        
        analyzeBodyParts();
        
        try {
            Set<MPartInfo> mpiBodies = Mime.getBody(mMessageParts, false);

            // extract text from the "non-body" parts
            StringBuilder fullContent = new StringBuilder(mBodyContent);
            {
                String reportRoot = null;
                for (MPartInfo mpi : mMessageParts) {
                    // text/calendar parts under a multipart/report aren't considered real calendar invites
                    String partName = mpi.mPartName;
                    if (reportRoot != null && !mpi.mPartName.startsWith(reportRoot))
                        reportRoot = null;
                    if (reportRoot == null && mpi.getContentType().equals(Mime.CT_MULTIPART_REPORT)) {
                        reportRoot = mpi.mPartName.endsWith("TEXT") ? mpi.mPartName.substring(0, partName.length() - 4) : mpi.mPartName + ".";
                    }
                    boolean isMainBody = mpiBodies.contains(mpi);
                    if (!isMainBody) {
                        String toplevelText = analyzePart(isMainBody, mpi, reportRoot != null);
                        if (toplevelText.length() > 0)
                            appendToContent(fullContent, toplevelText);
                    }
                }
            }
            
            // requires FULL content (all parts)
            mLuceneDocuments.add(setLuceneHeadersFromContainer(getMainBodyLuceneDocument(mBodyContent, fullContent)));

            // we're done with the body content (saved from analyzeBodyParts()) now
            mBodyContent = "";
            
            if (mNumParseErrors > 0 && sLog.isWarnEnabled()) {
                String msgid = getMessageID();
                String sbj = getSubject();
                sLog.warn("Message had analysis errors in " + mNumParseErrors +
                          " parts (Message-Id: " + msgid + ", Subject: " + sbj + ")");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            sLog.warn("exception while analyzing message; message will be partially indexed", e);
        }
    }
    
    /**
     * Extract all indexable text from this message.  This API should *only* be called if you are 
     * sure you're going to add the message to the index.  The only callsites of this API should be
     * Mailbox and test utilities.  Don't call it unless you absolutely are sure you need to do so.
     * 
     * @throws ServiceException
     */
    public void analyzeFully() throws ServiceException {
        analyzeNonBodyParts();
    }
    
    /**
     * Returns the <tt>MimeMessage</tt>.  Affected by both conversion and mutation.  
     */
    public MimeMessage getMimeMessage() {
        if (mExpandedMessage != null) {
            return mExpandedMessage;
        }
        // Reference to MimeMessage was dropped by close().
        try {
            InputStream in;
            if (mRawData != null) {
                // Constructed from a byte array, or constructed from a file and then mutated.
                in = new SharedByteArrayInputStream(mRawData);
            } else if (mRawFile != null) {
                // Constructed from a file and not mutated.
                in = new BlobInputStream(mRawFile);
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
        return mExpandedMessage;
    }

    /**
     * Returns the size of the raw MIME message.  Affected by mutation but
     * not conversion.
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
            } else {
                return size;
            }
        }
        ZimbraLog.mailbox.warn("%s.getRawSize(): Unable to get MIME message data..",
            ParsedMessage.class.getSimpleName());
        return 0;
    }

    /**
     * Returns the raw MIME data.  Affected by mutation but
     * not conversion.
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
     * Returns the SHA1 digest of the raw MIME message data, encoded as base64.  Affected by mutation but
     * not conversion.
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
    
    /**
     * Returns a stream to the raw MIME message.  Affected by mutation but
     * not conversion.
     */
    public InputStream getRawInputStream() throws ServiceException {
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
            analyzeBodyParts();
        } catch (ServiceException e) {
            sLog.warn("Message analysis failed when getting fragment; fragment is: " + mFragment, e);
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
            String[] values = getMimeMessage().getHeader(headerName);
            if (values == null || values.length == 0)
                return null;
            
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

    /**
     * Returns the value of the <tt>From</tt> header.  If not available,
     * returns the value of the <tt>Sender</tt> header.  Returns an empty
     * <tt>String</tt> if neither header is available.
     */
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

    /**
     * Returns the email address of the first <tt>From</tt> or
     * <tt>Sender</tt> header.
     */
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
            analyzeFully();
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
            parse();
            if (mHasTextCalendarPart) {
                analyzeFully();
            }
        } catch (ServiceException e) {
            // the calendar info should still be parsed 
            sLog.warn("Message analysis failed when getting calendar info");
        }
        return mCalendar;
    }
    
    /**
     * @return TRUE if there was a _temporary_ failure detected while analyzing the message.  In
     *         the case of a temporary failure, the message should be flagged and indexing re-tried
     *         at some point in the future
     */
    public boolean hasTemporaryAnalysisFailure() throws ServiceException {
        analyzeFully();
        return this.mTemporaryAnalysisFailure; 
    }
    
    private Document getMainBodyLuceneDocument(String bodyContent, StringBuilder fullContent)
    throws MessagingException, ServiceException {
        Document document = new Document();
        
        document.add(new Field(LuceneFields.L_MIMETYPE, "message/rfc822", Field.Store.YES, Field.Index.TOKENIZED));
        document.add(new Field(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_TOP, Field.Store.YES, Field.Index.UN_TOKENIZED));

        String toValue = setHeaderAsLuceneField(document, "to", LuceneFields.L_H_TO, Field.Store.NO, Field.Index.TOKENIZED);
        String ccValue = setHeaderAsLuceneField(document, "cc", LuceneFields.L_H_CC, Field.Store.NO, Field.Index.TOKENIZED);

        setHeaderAsLuceneField(document, "x-envelope-from", LuceneFields.L_H_X_ENV_FROM, Field.Store.NO, Field.Index.TOKENIZED);
        setHeaderAsLuceneField(document, "x-envelope-to", LuceneFields.L_H_X_ENV_TO, Field.Store.NO, Field.Index.TOKENIZED);

        String msgId = getHeader("message-id");
        if (msgId.length() > 0) {
            if (msgId.charAt(0) == '<')
                msgId = msgId.substring(1);

            if (msgId.charAt(msgId.length()-1) == '>')
                msgId = msgId.substring(0, msgId.length()-1);

            if (msgId.length() > 0)
                document.add(new Field(LuceneFields.L_H_MESSAGE_ID, msgId, Field.Store.NO, Field.Index.UN_TOKENIZED));
        }
        
        {
            // iterate all the message headers, add them to the structured-field data in the index
            StringBuilder fieldText = new StringBuilder();
//            Enumeration<String> en = (Enumeration<String>)(getMimeMessage().getAllHeaderLines());
            Enumeration<Header> en = (Enumeration<Header>)(getMimeMessage().getAllHeaders());
            while (en.hasMoreElements()) {
                Header h = en.nextElement();
                String key = h.getName().trim();
                String value = h.getValue();
                if (value != null) {
                    value = MimeUtility.unfold(value).trim();
                } else {
                    value = "";
                }
//                ZimbraLog.mailbox.info("HEADER: "+key+": "+value);
                if (key.length() > 0) {
//                    if (s.length() > 0) {
//                  fieldText.append(s).append('\n');
                    if (value.length() == 0) {
                        // low-level tokenizer can't deal with blank header value, so we'll index
                        // some dummy value just so the header appears in the index.
                        // Users can query for the existence of the header with a query
                        // like #headername:*
                        fieldText.append(key).append(':').append("_blank_").append('\n');
                    } else {
                        fieldText.append(key).append(':').append(value).append('\n');
                    }
                }
            }
            if (fieldText.length() > 0) {
                /* add key:value pairs to the structured FIELD lucene field */
//                ZimbraLog.index.info("FIELD text is:\n"+fieldText.toString());
                document.add(new Field(LuceneFields.L_FIELD, fieldText.toString(), Field.Store.NO, Field.Index.TOKENIZED));
            }                
        }

        String from = getSender();
        String subject = getNormalizedSubject();
        String sortFrom = getParsedSender().getSortString();
        if (sortFrom == null)
            sortFrom = "";
        else if (sortFrom.length() > DbMailItem.MAX_SENDER_LENGTH)
            sortFrom = sortFrom.substring(0, DbMailItem.MAX_SENDER_LENGTH);

        document.add(new Field(LuceneFields.L_H_FROM, from, Field.Store.NO, Field.Index.TOKENIZED));
        document.add(new Field(LuceneFields.L_H_SUBJECT, subject, Field.Store.NO, Field.Index.TOKENIZED));

        // add subject and from to main content for better searching
        appendToContent(fullContent, subject);

        // Bug 583: add all of the TOKENIZED versions of the email addresses to our CONTENT field...
        appendToContent(fullContent, ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_FROM, from));
        appendToContent(fullContent, ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_TO, toValue));
        appendToContent(fullContent, ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_CC, ccValue));

        String text = fullContent.toString();

        document.add(new Field(LuceneFields.L_CONTENT, text, Field.Store.NO, Field.Index.TOKENIZED));

        String sizeStr = Integer.toString(getMimeMessage().getSize());
        document.add(new Field(LuceneFields.L_SIZE, sizeStr, Field.Store.YES, Field.Index.NO));

        try {
            MimeHandler.getObjects(text, document);
        } catch (ObjectHandlerException e) {
            String msgid = getMessageID();
            String sbj = getSubject();
            sLog.warn("Unable to recognize searchable objects in message (Message-ID: " +
                msgid + ", Subject: " + sbj + ")", e);
        }

        // Get the list of attachment content types from this message and any
        // TNEF attachments
        Set<String> contentTypes = Mime.getAttachmentList(mMessageParts);

        // Assemble a comma-separated list of attachment content types
        StringBuilder buf = new StringBuilder();
        for (String contentType : contentTypes) {
            if (buf.length() > 0)
                buf.append(',');
            buf.append(contentType);
        }

        String attachments = buf.toString();
        if (attachments.equals(""))
            attachments = LuceneFields.L_ATTACHMENT_NONE;
        else
            attachments = attachments + "," + LuceneFields.L_ATTACHMENT_ANY;
        document.add(new Field(LuceneFields.L_ATTACHMENTS, attachments, Field.Store.NO, Field.Index.TOKENIZED));

        return document;
    }

    private String setHeaderAsLuceneField(Document d, String headerName,
        String fieldName, Field.Store stored, Field.Index indexed) throws MessagingException  {
        String value = getMimeMessage().getHeader(headerName, null);

        if (value == null || value.length() == 0)
            return "";
        try {
            value = MimeUtility.decodeText(value);
        } catch (UnsupportedEncodingException e) { }
        d.add(new Field(fieldName, value, stored, indexed));
        d.add(new Field(fieldName, value, stored, indexed));
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
    private Document setLuceneHeadersFromContainer(Document d) throws MessagingException {
        setHeaderAsLuceneField(d, "to", LuceneFields.L_H_TO, Field.Store.NO, Field.Index.TOKENIZED);
        setHeaderAsLuceneField(d, "cc", LuceneFields.L_H_CC, Field.Store.NO, Field.Index.TOKENIZED);

        String subject = getNormalizedSubject();
        String sortFrom = getParsedSender().getSortString();
        if (sortFrom != null && sortFrom.length() > DbMailItem.MAX_SENDER_LENGTH)
            sortFrom = sortFrom.substring(0, DbMailItem.MAX_SENDER_LENGTH);
        String from = getSender();

        if (from != null)
            d.add(new Field(LuceneFields.L_H_FROM, from, Field.Store.NO, Field.Index.TOKENIZED));
        
        if (subject != null) 
            d.add(new Field(LuceneFields.L_H_SUBJECT, subject, Field.Store.NO, Field.Index.TOKENIZED));
        
        return d;
    }
    
    
    
    /**
     * @return Extracted toplevel text (any text that should go into the toplevel indexed document)
     */
    private String analyzePart(boolean isMainBody, MPartInfo mpi, boolean ignoreCalendar)
    throws MessagingException, ServiceException {
        
        String toRet = "";
        try {
            String ctype = mpi.getContentType();
            // ignore multipart "container" parts
            if (ctype.startsWith(Mime.CT_MULTIPART_PREFIX))
                return toRet;
            
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
                if (!ignoreCalendar && mCalendar == null)
                    mCalendar = handler.getICalendar();

                // In some cases we want to add ALL TEXT from EVERY PART to the toplevel 
                // body content. This is necessary for queries with multiple words -- where
                // one word is in the body and one is in a sub-attachment.
                //
                // We don't always want to do this, for example if attachment indexing is disabled
                // and this is an attachment handler, we don't want to add this text to the toplevel
                // document.  
                //
                // We index this content in the toplevel if it is:
                //     - the 'main body' and a local mime handler
                //     - the 'main body' and IndexAttachments was set in the constructor
                //     - IndexAttachments was set and !disableIndexingAttachmentsTogether 
                if ((isMainBody && (!handler.runsExternally() || mIndexAttachments)) ||
                            (mIndexAttachments && !DebugConfig.disableIndexingAttachmentsTogether)) {
                    toRet = handler.getContent();
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
                        mLuceneDocuments.add(setLuceneHeadersFromContainer(doc));
                    }
                }
            }

            // make sure we've got the text/calendar handler installed
            if (!ignoreCalendar && mCalendar == null && ctype.equals(Mime.CT_TEXT_CALENDAR)) {
                if (handler.isIndexingEnabled())
                    ZimbraLog.index.warn("TextCalendarHandler not correctly installed");

                InputStream is = null;
                try {
                    String charset = mpi.getContentTypeParameter(Mime.P_CHARSET);
                    if (charset == null || charset.trim().equals(""))
                        charset = Mime.P_CHARSET_DEFAULT;

                    Reader reader = new InputStreamReader(is = mpi.getMimePart().getInputStream(), charset);
                    mCalendar = ZCalendarBuilder.build(reader);
                } catch (IOException ioe) {
                    ZimbraLog.index.warn("error reading text/calendar mime part", ioe);
                } finally {
                    ByteUtil.closeStream(is);
                }
            }
        } catch (MimeHandlerException e) {
            mNumParseErrors++;
            String pn = mpi.getPartName();
            String ctype = mpi.getContentType();
            String msgid = getMessageID();
            sLog.warn("Unable to parse part %s (%s, %s) of message with Message-ID %s.",
                pn, mpi.getFilename(), ctype, msgid, e);
            if (ConversionException.isTemporaryCauseOf(e)) {
                mTemporaryAnalysisFailure = true;
            }
            sLog.warn("Attachment will not be indexed.");
        } catch (ObjectHandlerException e) {
            mNumParseErrors++;
            String pn = mpi.getPartName();
            String ct = mpi.getContentType();
            String msgid = getMessageID();
            sLog.warn("Unable to parse part %s (%s, %s) of message with Message-ID %s." +
                "  Object will not be indexed.",
                pn, mpi.getFilename(), ct, msgid, e);
        }
        return toRet;
    }
    
    private static final void appendToContent(StringBuilder sb, String s) {
        if (sb.length() > 0)
            sb.append(' ');
        sb.append(s);
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
     * the file descriptor.
     */
    public void closeFile()
    throws IOException {
        if (mRawFileInputStream != null) {
            mRawFileInputStream.closeFile();
        }
    }
    
    /**
     * Tells this <tt>ParsedMessage</tt> to get its data from a new file.
     * Only applies to <tt>ParsedMessage</tt>s that were instantiated from
     * a file and not mutated.
     */
    public void fileMoved(File newFile)
    throws IOException {
        if (mRawFileInputStream != null) {
            mRawFileInputStream.fileMoved(newFile);
        }
        mRawFile = newFile;
    }
}
