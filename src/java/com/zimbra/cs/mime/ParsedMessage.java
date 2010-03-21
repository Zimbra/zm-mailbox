/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 6. 30.
 */
package com.zimbra.cs.mime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;

import com.zimbra.cs.store.Blob;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CalculatorStream;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
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
    private List<String> mFilenames = new ArrayList<String>();
    private boolean mIndexAttachments;
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
    private boolean mSubjectIsReply;
    private List<IndexDocument> mZDocuments = new ArrayList<IndexDocument>();
    private CalendarPartInfo mCalendarPartInfo;

    private String mRawDigest;
    private boolean mWasMutated;
    private Integer mRawSize;
    private InputStream mSharedStream;
    
    public static final long DATE_HEADER = -2;
    public static final long DATE_UNKNOWN = -1;

    public ParsedMessage(MimeMessage msg, boolean indexAttachments)
    throws ServiceException {
        this(msg, getZimbraDateHeader(msg), indexAttachments);
    }

    public ParsedMessage(MimeMessage msg, long receivedDate, boolean indexAttachments)
    throws ServiceException {
        initialize(msg, receivedDate, indexAttachments, null, null);
    }

    public ParsedMessage(byte[] rawData, boolean indexAttachments)
    throws ServiceException {
        this(rawData, null, indexAttachments);
    }

    public ParsedMessage(byte[] rawData, Long receivedDate, boolean indexAttachments)
    throws ServiceException {
        initialize(rawData, receivedDate, indexAttachments, null);
    }
    
    /**
     * Creates a <tt>ParsedMessage</tt> from a file already stored on disk.
     * @param file the file on disk.
     */
    public ParsedMessage(File file, Long receivedDate, boolean indexAttachments)
    throws ServiceException, IOException {
        initialize(file, receivedDate, indexAttachments, null, null);
    }

    public ParsedMessage(Blob blob, Long receivedDate, boolean indexAttachments)
        throws ServiceException, IOException {
        initialize(blob.getFile(), receivedDate, indexAttachments, (int) blob.getRawSize(), blob.getDigest());    
    }
    
    public ParsedMessage(ParsedMessageOptions opt)
    throws ServiceException {
        if (opt.getAttachmentIndexing() == null) {
            throw ServiceException.FAILURE("Options do not specify attachment indexing state.", null);
        }
        if (opt.getMimeMessage() != null) {
            initialize(opt.getMimeMessage(), opt.getReceivedDate(), opt.getAttachmentIndexing(), opt.getSize(), opt.getDigest());
        } else if (opt.getRawData() != null) {
            initialize(opt.getRawData(), opt.getReceivedDate(), opt.getAttachmentIndexing(), opt.getDigest());
        } else if (opt.getFile() != null) {
            try {
                initialize(opt.getFile(), opt.getReceivedDate(), opt.getAttachmentIndexing(), opt.getSize(), opt.getDigest());
            } catch (IOException e) {
                throw ServiceException.FAILURE("Unable to initialize ParsedMessage", e);
            }
        } else {
            throw ServiceException.FAILURE("ParsedMessageOptions do not specify message content", null);
        }
    }
    
    private void initialize(MimeMessage msg, Long receivedDate, boolean indexAttachments, Integer rawSize, String rawDigest)
    throws ServiceException {
        mMimeMessage = msg;
        mExpandedMessage = msg;
        mRawSize = rawSize;
        mRawDigest = rawDigest;
        initialize(receivedDate, indexAttachments);
    }
    
    private void initialize(byte[] rawData, Long receivedDate, boolean indexAttachments, String rawDigest)
    throws ServiceException {
        if (rawData == null || rawData.length == 0) {
            throw ServiceException.FAILURE("Message data cannot be null or empty.", null);
        }
        mSharedStream = new SharedByteArrayInputStream(rawData);
        mRawSize = rawData.length;
        mRawDigest = rawDigest;
        initialize(receivedDate, indexAttachments);
    }
    
    private void initialize(File file, Long receivedDate, boolean indexAttachments, Integer rawSize, String rawDigest)
    throws IOException, ServiceException {
        if (file == null) {
            throw new IOException("File cannot be null.");
        }
        if (file.length() == 0) {
            throw new IOException("File " + file.getPath() + " is empty.");
        }

        if (rawSize != null) {
            mRawSize = rawSize;
        } else if (!FileUtil.isGzipped(file)) {
            mRawSize = (int) file.length();
        }
        mSharedStream = new BlobInputStream(file, mRawSize);
        mRawDigest = rawDigest;
        initialize(receivedDate, indexAttachments);
    }
    
    private void initialize(Long receivedDate, boolean indexAttachments)
        throws ServiceException {
        try {
            init(receivedDate, indexAttachments);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Unable to initialize ParsedMessage", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to initialize ParsedMessage", e);
        }
    }
    
    /**
     * Runs MIME mutators and converters, initializes {@link #mMimeMessage}, {@link #mExpandedMessage},
     * {@link #mFileInputStream} and {@link #mReceivedDate} based on message content.
     */
    private void init(Long receivedDate, boolean indexAttachments)
    throws MessagingException, IOException {
        mIndexAttachments = indexAttachments;
        
        if (mMimeMessage == null) {
            if (mSharedStream == null) {
                throw new IOException("Content stream has not been initialized.");
            }
            if (!(mSharedStream instanceof SharedInputStream)) {
                InputStream in = mSharedStream;
                mSharedStream = null;
                CalculatorStream calc = new CalculatorStream(in);
                byte[] content = ByteUtil.getContent(calc, 0);
                mSharedStream = new SharedByteArrayInputStream(content);
                mRawSize = (int) calc.getSize();
                mRawDigest = calc.getDigest();
            }

            mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), mSharedStream);
        }
        
        // Run mutators.
        try {
            runMimeMutators();
        } catch (Exception e) {
            mWasMutated = false;
            // Original stream has been read, so get a new one.
            mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), getRawInputStream());
        }

        if (wasMutated()) {
            // Original data is now invalid.
            mRawDigest = null;
            mRawSize = null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mMimeMessage.writeTo(buffer);
            byte[] content = buffer.toByteArray();
            ByteUtil.closeStream(mSharedStream);
            mSharedStream = new SharedByteArrayInputStream(content); 
            mMimeMessage = mExpandedMessage = null;
            mMimeMessage = mExpandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), mSharedStream);
        }
        
        ExpandMimeMessage expand = new ExpandMimeMessage(mMimeMessage);
        try {
            expand.expand();
            mExpandedMessage = expand.getExpanded();
        } catch (Exception e) {
            // roll back if necessary
            mExpandedMessage = mMimeMessage;
            sLog.warn("exception while converting message; message will be analyzed unconverted", e);
        }

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
     * Allows the caller to set the digest of the raw message.  This avoids
     * rereading the file in the case where the caller has already read it
     * once and calculated the digest.
     */
    public void setRawDigest(String digest) {
        mRawDigest = digest;
    }

    /**
     * Allows the caller to set the size of the raw message.  This avoids
     * rereading the possibly compressed file in the case where the caller has
     * already determined it
     * 
     */
    public void setRawSize(Integer size) {
        mRawSize = size;
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
                for (MPartInfo mpi : mMessageParts) {
                    boolean isMainBody = mpiBodies.contains(mpi);
                    if (isMainBody) {
                        String toplevelText = analyzePart(isMainBody, mpi);
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
                for (MPartInfo mpi : mMessageParts) {
                    boolean isMainBody = mpiBodies.contains(mpi);
                    if (!isMainBody) {
                        String toplevelText = analyzePart(isMainBody, mpi);
                        if (toplevelText.length() > 0)
                            appendToContent(fullContent, toplevelText);
                    }
                }
            }
            
            // requires FULL content (all parts)
            mZDocuments.add(setLuceneHeadersFromContainer(getMainBodyLuceneDocument(fullContent)));

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
        return mExpandedMessage;
    }
    
    /**
     * Returns the original <tt>MimeMessage</tt>.  Affected by mutation but not
     * conversion.
     */
    public MimeMessage getOriginalMessage() {
        return mMimeMessage;
    }

    /**
     * Returns the size of the raw MIME message.  Affected by mutation but
     * not conversion.
     */
    public int getRawSize() throws IOException, ServiceException {
        if (mRawSize == null) {
            initializeDigestAndSize();
        }
        return mRawSize;
    }

    /**
     * Returns the raw MIME data.  Affected by mutation but
     * not conversion.
     */
    public byte[] getRawData() throws IOException {
        int sizeHint = (mRawSize == null ? 0 : mRawSize);
        return ByteUtil.getContent(getRawInputStream(), sizeHint);
    }

    /**
     * Returns the SHA1 digest of the raw MIME message data, encoded as base64.  Affected by mutation but
     * not conversion.
     */
    public String getRawDigest() throws IOException, ServiceException {
        if (mRawDigest == null) {
            initializeDigestAndSize();
        }
        return mRawDigest;
    }
    
    private void initializeDigestAndSize() throws IOException, ServiceException {
        if (mRawDigest != null && mRawSize != null) {
            return;
        }
        int size = 0;
        InputStream in = null;
        MessageDigest digest;

        // Initialize streams and digest calculator.
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("Unable to calculate digest", e);
        }

        try {
            in = getRawInputStream();
            DigestInputStream digestStream = new DigestInputStream(in, digest);
            int numRead = -1;
            byte[] buf = new byte[1024];
            while ((numRead = digestStream.read(buf)) >= 0) {
                size += numRead;
            }
            mRawSize = size;
            mRawDigest = ByteUtil.encodeFSSafeBase64(digest.digest());
        } finally {
            ByteUtil.closeStream(in);
        }
    }
    
    /**
     * Returns a stream to the raw MIME message.  Affected by mutation but
     * not conversion.<p>
     * 
     * <b>Important</b>: when the content comes from a <tt>MimeMessage</tt>,
     * JavaMail requires us to start a {@link MimeMessageOutputThread} to
     * serve up the content.  As a result, all calls to this method must
     * be wrapped in a try/finally with a call to {@link ByteUtil#closeStream(InputStream)}
     * in the finally block.  This guarantees that the stream is drained and
     * the <tt>MimeMessageOutputThread</tt> exits.
     */
    public InputStream getRawInputStream()
    throws IOException {
        if (mSharedStream != null) {
            return ((SharedInputStream) mSharedStream).newStream(0, -1);
        } else {
            return Mime.getInputStream(mMimeMessage);
        }
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
    
    /**
     * Returns the <tt>BlobInputStream</tt> if this message is being streamed
     * from a file, otherwise returns <tt>null</tt>.
     */
    public BlobInputStream getBlobInputStream() {
        if (mSharedStream instanceof BlobInputStream) {
            return (BlobInputStream) mSharedStream;
        }
        return null;
    }
    
    public boolean isStreamedFromDisk() {
        return (getBlobInputStream() != null);
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
        return mSubjectIsReply;
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

    /**
     * Returns the message ID, or <tt>null</tt> if the message id cannot be determined.
     */
    public String getMessageID() {
        return Mime.getMessageID(getMimeMessage());
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
     * Returns the decoded value of the <tt>From</tt> header.  If not available,
     * returns the value of the <tt>Sender</tt> header.  Returns an empty
     * <tt>String</tt> if neither header is available.
     */
    public String getSender() {
        if (mSender == null) {
            mSender = Mime.getSender(getMimeMessage());
        }
        return mSender;
    }

    /**
     * Returns the email address of the first <tt>From</tt> or
     * <tt>Sender</tt> header.
     */
    public String getSenderEmail() {
        return getSenderEmail(true);
    }

    /**
     * If fromFirst is true, returns the email address of the first <tt>From</tt>
     * or <tt>Sender</tt> header.  If fromFirst is false, returns the email address
     * of the first <tt>Sender</tt> or <tt>From</tt> header.
     */
    public String getSenderEmail(boolean fromFirst) {
        try {
            if (fromFirst) {
                // From header first, then Sender
                Address[] froms = getMimeMessage().getFrom();
                if (froms != null && froms.length > 0 && froms[0] instanceof InternetAddress)
                    return ((InternetAddress) froms[0]).getAddress();
                Address sender = getMimeMessage().getSender();
                if (sender instanceof InternetAddress)
                    return ((InternetAddress) sender).getAddress();
            } else {
                // Sender header first, then From
                Address sender = getMimeMessage().getSender();
                if (sender instanceof InternetAddress)
                    return ((InternetAddress) sender).getAddress();
                Address[] froms = getMimeMessage().getFrom();
                if (froms != null && froms.length > 0 && froms[0] instanceof InternetAddress)
                    return ((InternetAddress) froms[0]).getAddress();
            }
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

    public void setReceivedDate(long date) {
        // round to nearest second...
        if (date == DATE_HEADER)
            mReceivedDate = getDateHeader();
        else if (date != DATE_UNKNOWN)
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

    public List<IndexDocument> getLuceneDocuments() {
        try {
            analyzeFully();
        } catch (ServiceException e) {
            sLog.warn("message analysis failed when getting lucene documents");
        }
        return mZDocuments;
    }

    /**
     * Returns CalenarPartInfo object containing a ZVCalendar representing an iCalendar
     * part and some additional information about the calendar part such as whether
     * the part was found inside a forwarded rfc822 message and if the part had
     * "method" parameter indicating it was an actual invite email as opposed to
     * a regular email that happens to carry an ics attachment.
     */
    public CalendarPartInfo getCalendarPartInfo() {
        try {
            parse();
            if (mHasTextCalendarPart) {
                analyzeFully();
            }
        } catch (ServiceException e) {
            // the calendar info should still be parsed 
            sLog.warn("Message analysis failed when getting calendar info");
        }
        return mCalendarPartInfo;
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
    
    private IndexDocument getMainBodyLuceneDocument(StringBuilder fullContent)
    throws MessagingException, ServiceException {
        Document document = new Document();
        IndexDocument zd = new IndexDocument(document);
        
        document.add(new Field(LuceneFields.L_MIMETYPE, "message/rfc822", Field.Store.YES, Field.Index.TOKENIZED));
        document.add(new Field(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_TOP, Field.Store.YES, Field.Index.UN_TOKENIZED));

        String toValue = setHeaderAsLuceneField(document, "to", LuceneFields.L_H_TO, Field.Store.NO, Field.Index.TOKENIZED);
        String ccValue = setHeaderAsLuceneField(document, "cc", LuceneFields.L_H_CC, Field.Store.NO, Field.Index.TOKENIZED);

        setHeaderAsLuceneField(document, "x-envelope-from", LuceneFields.L_H_X_ENV_FROM, Field.Store.NO, Field.Index.TOKENIZED);
        setHeaderAsLuceneField(document, "x-envelope-to", LuceneFields.L_H_X_ENV_TO, Field.Store.NO, Field.Index.TOKENIZED);

        String msgId = Mime.getHeader(getMimeMessage(), "message-id");
        if (msgId == null) {
            msgId = "";
        }
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
            @SuppressWarnings("unchecked")
            Enumeration<Header> en = getMimeMessage().getAllHeaders();
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
        String subject = getSubject();
        String sortFrom = getParsedSender().getSortString();
        if (sortFrom == null)
            sortFrom = "";
        else if (sortFrom.length() > DbMailItem.MAX_SENDER_LENGTH)
            sortFrom = sortFrom.substring(0, DbMailItem.MAX_SENDER_LENGTH);

        document.add(new Field(LuceneFields.L_H_FROM, from, Field.Store.NO, Field.Index.TOKENIZED));
        document.add(new Field(LuceneFields.L_H_SUBJECT, subject, Field.Store.NO, Field.Index.TOKENIZED));

        
        // add subject and from to main content for better searching
        StringBuilder contentPrepend = new StringBuilder(subject);

        // Bug 583: add all of the TOKENIZED versions of the email addresses to our CONTENT field...
        appendToContent(contentPrepend, ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_FROM, from));
        appendToContent(contentPrepend, ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_TO, toValue));
        appendToContent(contentPrepend, ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_CC, ccValue));
        
        // bug 33461: add filenames to our CONTENT field
        for (String fn : mFilenames) {
            appendToContent(contentPrepend, ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_FILENAME, fn));
            appendToContent(contentPrepend, fn); // also add the non-tokenized form, so full-filename searches match
        }

        String text = contentPrepend.toString()+" "+fullContent.toString();

        document.add(new Field(LuceneFields.L_CONTENT, text, Field.Store.NO, Field.Index.TOKENIZED));

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

        return zd;
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
    private IndexDocument setLuceneHeadersFromContainer(IndexDocument zd) throws MessagingException {
        org.apache.lucene.document.Document d = (org.apache.lucene.document.Document)(zd.getWrappedDocument());
        
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
        
        return zd;
    }

    private static boolean isBouncedCalendar(MPartInfo mpi) {
        if (MimeConstants.CT_TEXT_CALENDAR.equals(mpi.getContentType())) {
            MPartInfo parent = mpi;
            while ((parent = parent.getParent()) != null) {
                String ct = parent.getContentType();
                if (MimeConstants.CT_MULTIPART_REPORT.equals(ct))  // Assume multipart/report == bounced message.
                    return true;
            }
        }
        return false;
    }

    public static class CalendarPartInfo {
        public ZVCalendar cal;
        public ICalTok method;
        public boolean wasForwarded;
    }

    private void setCalendarPartInfo(MPartInfo mpi, ZVCalendar cal) {
        mCalendarPartInfo = new CalendarPartInfo();
        mCalendarPartInfo.cal = cal;
        mCalendarPartInfo.method = cal.getMethod();

        mCalendarPartInfo.wasForwarded = false;
        MPartInfo parent = mpi;
        while ((parent = parent.getParent()) != null) {
            String ct = parent.getContentType();
            if (MimeConstants.CT_MESSAGE_RFC822.equals(ct))
                mCalendarPartInfo.wasForwarded = true;
        }
    }

    /**
     * @return Extracted toplevel text (any text that should go into the toplevel indexed document)
     */
    private String analyzePart(boolean isMainBody, MPartInfo mpi)
    throws MessagingException, ServiceException {

        boolean ignoreCalendar;
        if (mCalendarPartInfo == null)
            ignoreCalendar = isBouncedCalendar(mpi);
        else
            ignoreCalendar = true;

        String methodParam = (new ContentType(mpi.getMimePart().getContentType())).getParameter("method");
        if (methodParam == null && !LC.calendar_allow_invite_without_method.booleanValue())
            ignoreCalendar = true;

        String toRet = "";
        try {
            String ctype = mpi.getContentType();
            // ignore multipart "container" parts
            if (ctype.startsWith(MimeConstants.CT_MULTIPART_PREFIX))
                return toRet;
            
            MimeHandler handler = MimeHandlerManager.getMimeHandler(ctype, mpi.getFilename());
            assert(handler != null);
            
            Mime.repairTransferEncoding(mpi.getMimePart());
            
            if (handler.isIndexingEnabled()) {
                handler.init(mpi.getMimePart().getDataHandler().getDataSource());
                handler.setPartName(mpi.getPartName());
                handler.setFilename(mpi.getFilename());
                handler.setSize(mpi.getSize());

                // remember the first iCalendar attachment
                if (!ignoreCalendar && mCalendarPartInfo == null) {
                    ZVCalendar cal = handler.getICalendar();
                    if (cal != null)
                        setCalendarPartInfo(mpi, cal);
                }

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
                    IndexDocument zd = new IndexDocument(handler.getDocument());
                    
                    String fn = handler.getFilename();
                    if (fn != null && fn.length() > 0) {
                        mFilenames.add(fn);
                    }
                    
                    if (zd!= null) {
                        org.apache.lucene.document.Document doc = (org.apache.lucene.document.Document)(zd.getWrappedDocument()); 
                        int partSize = mpi.getMimePart().getSize();
                        doc.add(new Field(LuceneFields.L_SIZE, Integer.toString(partSize), Field.Store.YES, Field.Index.NO));
                        mZDocuments.add(setLuceneHeadersFromContainer(zd));
                    }
                }
            }

            // make sure we've got the text/calendar handler installed
            if (!ignoreCalendar && mCalendarPartInfo == null && ctype.equals(MimeConstants.CT_TEXT_CALENDAR)) {
                if (handler.isIndexingEnabled())
                    ZimbraLog.index.warn("TextCalendarHandler not correctly installed");

                InputStream is = null;
                try {
                    String charset = mpi.getContentTypeParameter(MimeConstants.P_CHARSET);
                    if (charset == null || charset.trim().equals(""))
                        charset = MimeConstants.P_CHARSET_DEFAULT;

                    is = mpi.getMimePart().getInputStream();
                    ZVCalendar cal = ZCalendarBuilder.build(is, charset);
                    if (cal != null)
                        setCalendarPartInfo(mpi, cal);
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
    private static final Set<String> SYSTEM_PREFIXES = new HashSet<String>(Arrays.asList(
        "accept:", "accepted:", "decline:", "declined:",
        "tentative:", "cancelled:", "new time proposed:",
        "read-receipt:", "share created:", "share accepted:"
    ));

    private static final int MAX_PREFIX_LENGTH = 3;

    private static Pair<String, Boolean> trimPrefixes(String subject) {
        if (subject == null || subject.length() == 0)
            return new Pair<String, Boolean>("", false);

        boolean trimmed = false;
        while (true) {
            subject = subject.trim();
            if (subject.length() == 0)
                return new Pair<String, Boolean>(subject, trimmed);

            // first, strip off any "(fwd)" at the end
            int tstart = subject.length() - 5;
            char c;
            if (tstart >= 0 && subject.charAt(tstart) == '(' &&
                    ((c = subject.charAt(tstart + 1)) == 'f' || c == 'F') &&
                    ((c = subject.charAt(tstart + 2)) == 'w' || c == 'W') &&
                    ((c = subject.charAt(tstart + 3)) == 'd' || c == 'D') &&
                    subject.charAt(tstart + 4) == ')') {
                subject = subject.substring(0, subject.length() - 5).trim();
                trimmed = true;
                continue;
            }

            // find the first ':' in the subject
            boolean braced = subject.charAt(0) == '[';
            int colon = subject.indexOf(':');
            if (colon > (braced ? 1 : 0)) {
                // figure out if it's either a known calendar response prefix or a 1-3 letter prefix
                String prefix = subject.substring(braced ? 1 : 0, colon + 1);
                boolean matched = true;
                if (!SYSTEM_PREFIXES.contains(prefix.toLowerCase())) {
                    // make sure to catch "re(2):" and "fwd[5]:" as well...
                    int paren = -1;
                    for (int i = 0; matched && i < prefix.length() - 1; i++) {
                        c = prefix.charAt(i);
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
                if (matched) {
                    if (braced && subject.endsWith("]"))
                        subject = subject.substring(colon + 1, subject.length() - 1);
                    else
                        subject = subject.substring(colon + 1);
                    trimmed = true;
                    continue;
                }
            }

            // trim mailing list prefixes (e.g. "[rev-dandom]")
            int bclose;
            if (braced && (bclose = subject.indexOf(']')) > 0 && subject.lastIndexOf('[', bclose) == 0) {
                String remainder = subject.substring(bclose + 1).trim();
                if (remainder.length() > 0) {
                    subject = remainder;
                    continue;
                }
            }

            return new Pair<String, Boolean>(subject, trimmed);
        }
    }

    private static String compressWhitespace(String value) {
        if (value == null || value.equals(""))
            return value;

        StringBuilder sb = new StringBuilder();
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

        try {
            mNormalizedSubject = mSubject = StringUtil.stripControlCharacters(Mime.getSubject(getMimeMessage()));
        } catch (MessagingException e) { }

        if (mSubject == null) {
            mNormalizedSubject = mSubject = "";
            mSubjectIsReply = false;
        } else {
            Pair<String, Boolean> normalized = trimPrefixes(mSubject);
            mNormalizedSubject = compressWhitespace(normalized.getFirst());
            mSubjectIsReply = normalized.getSecond();
            if (mNormalizedSubject.length() > DbMailItem.MAX_SUBJECT_LENGTH)
                mNormalizedSubject = mNormalizedSubject.substring(0, DbMailItem.MAX_SUBJECT_LENGTH).trim();
        }
    }

    public static String normalize(String subject) {
        subject = compressWhitespace(trimPrefixes(StringUtil.stripControlCharacters(subject)).getFirst());
        if (subject != null && subject.length() > DbMailItem.MAX_SUBJECT_LENGTH)
            subject = subject.substring(0, DbMailItem.MAX_SUBJECT_LENGTH).trim();
        return subject;
    }

    public static boolean isReply(String subject) {
        return trimPrefixes(subject).getSecond();
    }


    private static void testNormalization(String[] test) {
        String raw = test[0], normalized = test[1], description = test[3];
        boolean isReply = Boolean.parseBoolean(test[2]);

        Pair<String, Boolean> result = trimPrefixes(StringUtil.stripControlCharacters(raw));
        String actual = compressWhitespace(result.getFirst());
        if (!normalized.equals(actual) || isReply != result.getSecond()) {
            System.out.println("failed test: " + description);
            System.out.println("  raw:      {" + raw + '}');
            System.out.println("  expected: |" + normalized + "| (" + (isReply ? "" : "un") + "trimmed)");
            System.out.println("  actual:   |" + actual + "| (" + (result.getSecond() ? "" : "un") + "trimmed)");
        }

        if (!normalized.equals(normalize(raw)))
            System.out.println("error in normalize() for {" + raw + '}');
    }

    public static void main(String[] args) {
        String[][] tests = new String[][] {
            { "foo", "foo", "false", "normal subject" },
            { " foo", "foo", "false", "leading whitespace" },
            { "foo\t", "foo", "false", "trailing whitespace" },
            { "  foo\t", "foo", "false", "leading and trailing whitespace" },
            { "foo  bar", "foo bar", "false", "compressing whitespace" },
            { null, "", "false", "missing subject" },
            { "", "", "false", "blank subject" },
            { "  \t ", "", "false", "nothing but whitespace" },
            { "[bar] foo", "foo", "false", "mlist prefix" },
            { "[foo]", "[foo]", "false", "only a mlist prefix" },
            { "[bar[] foo", "[bar[] foo", "false", "broken mlist prefix" },
            { "[bar][baz][foo]", "[foo]", "false", "keep only the last mlist prefix" },
            { "re: foo", "foo", "true", "re: prefix" },
            { "re:foo", "foo", "true", "no space after re: prefix" },
            { "  re: foo", "foo", "true", "re: prefix with leading whitespace" },
            { "re: [fwd: [fwd: re: [fwd: babylon]]]", "babylon", "true", "re and [fwd" },
            { "Ad: Re: Ad: Re: Ad: x", "x", "true", "alternative prefixes" },
            { "[foo] Fwd: [bar] Re: fw: b (fWd)  (fwd)", "b", "true", "mlist prefixes, std prefixes, mixed-case fwd trailers" },
            { "[foo] Fwd: [bar] Re: d fw: b (fWd)  (fwd)", "d fw: b", "true", "character mixed in with prefixes, mixed-case fwd trailers" },
            { "Fwd: [Imap-protocol] Re: so long, and thanks for all the fish!", "so long, and thanks for all the fish!", "true", "intermixed prefixes" },
        };

        for (String[] test : tests)
            testNormalization(test);
    }
}
