/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Threader;
import com.zimbra.cs.object.ObjectHandlerException;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;

/**
 * Instantiates a JavaMail {@link MimeMessage} from a <tt>byte[]</tt> or
 * file on disk.  Converts or mutates the message as necessary by calling
 * registered instances of {@link MimeVisitor}.  Conversion modifies the in-
 * memory message without affecting the raw version.  Mutation modifies the
 * raw version and affects results returned by the <tt>getRawXXX</tt> methods.
 *
 * @since 2004. 6. 30.
 * @author jhahm
 */
public final class ParsedMessage {

    private static final Log LOG = LogFactory.getLog(ParsedMessage.class);
    private static final MailDateFormat FORMAT = new MailDateFormat();
    public static final long DATE_HEADER = -2;
    public static final long DATE_UNKNOWN = -1;

    private MimeMessage mimeMessage;
    private MimeMessage expandedMessage;
    private boolean parsed = false;
    private boolean analyzedBodyParts = false;
    private boolean analyzedNonBodyParts = false;
    private String bodyContent = "";
    private final List<String> filenames = new ArrayList<String>();
    private boolean indexAttachments;
    private int numParseErrors = 0;
    private String defaultCharset;

    /** if TRUE then there was a _temporary_ failure analyzing the message.  We should attempt
     * to re-index this message at a later time */
    private boolean temporaryAnalysisFailure = false;

    private List<MPartInfo> messageParts;
    private String recipients;
    private String fromOrSender;
    private String from;
    private String to;
    private String cc;
    private String sender;

    private ParsedAddress parsedSender;
    private List<ParsedAddress> parsedRecipients;
    private boolean hasAttachments = false;
    private boolean hasTextCalendarPart = false;
    private String fragment = "";
    private boolean encrypted;
    private long dateHeader = -1;
    private long receivedDate = -1;
    private String subject;
    private String normalizedSubject;
    private boolean subjectIsReply;
    private Boolean hasReplyToHeader = null;
    private final List<IndexDocument> luceneDocuments = new ArrayList<IndexDocument>(2);
    private CalendarPartInfo calendarPartInfo;
    private boolean wasMutated;
    private InputStream sharedStream;
    private final Map<Mailbox, Threader> threaders = new HashMap<Mailbox, Threader>();
    private String dataSourceId = null;

    public ParsedMessage(MimeMessage msg, boolean indexAttachments) throws ServiceException {
        this(msg, getZimbraDateHeader(msg), indexAttachments);
    }

    public ParsedMessage(MimeMessage msg, long receivedDate, boolean indexAttachments) throws ServiceException {
        initialize(msg, receivedDate, indexAttachments);
    }

    public ParsedMessage(byte[] rawData, boolean indexAttachments) throws ServiceException {
        this(rawData, null, indexAttachments);
    }

    public ParsedMessage(byte[] rawData, Long receivedDate, boolean indexAttachments) throws ServiceException {
        initialize(rawData, receivedDate, indexAttachments);
    }

    /**
     * Creates a <tt>ParsedMessage</tt> from a file already stored on disk.
     * @param file the file on disk.
     */
    public ParsedMessage(File file, Long receivedDate, boolean indexAttachments) throws ServiceException, IOException {
        initialize(file, receivedDate, indexAttachments);
    }

    public ParsedMessage(Blob blob, Long receivedDate, boolean indexAttachments) throws ServiceException, IOException {
        initialize(blob, receivedDate, indexAttachments);
    }

    public ParsedMessage(ParsedMessageOptions opt) throws ServiceException {
        if (opt.getAttachmentIndexing() == null) {
            throw ServiceException.FAILURE("Options do not specify attachment indexing state.", null);
        }

        if (opt.getMimeMessage() != null) {
            initialize(opt.getMimeMessage(), opt.getReceivedDate(), opt.getAttachmentIndexing());
        } else if (opt.getRawData() != null) {
            initialize(opt.getRawData(), opt.getReceivedDate(), opt.getAttachmentIndexing());
        } else if (opt.getFile() != null) {
            try {
                initialize(opt.getFile(), opt.getReceivedDate(), opt.getAttachmentIndexing());
            } catch (IOException e) {
                throw ServiceException.FAILURE("Unable to initialize ParsedMessage", e);
            }
        } else {
            throw ServiceException.FAILURE("ParsedMessageOptions do not specify message content", null);
        }
    }

    private void initialize(MimeMessage msg, Long receivedDate, boolean indexAttachments) throws ServiceException {
        mimeMessage = msg;
        expandedMessage = msg;
        initialize(receivedDate, indexAttachments);
    }

    private void initialize(byte[] rawData, Long receivedDate, boolean indexAttachments) throws ServiceException {
        if (rawData == null || rawData.length == 0) {
            throw ServiceException.FAILURE("Message data cannot be null or empty.", null);
        }
        sharedStream = new SharedByteArrayInputStream(rawData);
        initialize(receivedDate, indexAttachments);
    }

    private void initialize(Blob blob, Long receivedDate, boolean indexAttachments)
            throws IOException, ServiceException {
        sharedStream = StoreManager.getInstance().getContent(blob);
        initialize(receivedDate, indexAttachments);
    }

    private void initialize(File file, Long receivedDate, boolean indexAttachments)
            throws IOException, ServiceException {
        if (file == null) {
            throw new IOException("File cannot be null.");
        }
        if (file.length() == 0) {
            throw new IOException("File " + file.getPath() + " is empty.");
        }

        long size;
        if (FileUtil.isGzipped(file)) {
            size = ByteUtil.getDataLength(new GZIPInputStream(new FileInputStream(file)));
        } else {
            size = file.length();
        }
        sharedStream = new BlobInputStream(file, size);
        initialize(receivedDate, indexAttachments);
    }

    private void initialize(Long receivedDate, boolean indexAttachments) throws ServiceException {
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
    private void init(Long receivedDate, boolean indexAttachments) throws MessagingException, IOException {
        this.indexAttachments = indexAttachments;

        if (mimeMessage == null) {
            if (sharedStream == null) {
                throw new IOException("Content stream has not been initialized.");
            }
            if (!(sharedStream instanceof SharedInputStream)) {
                InputStream in = sharedStream;
                sharedStream = null;
                byte[] content = ByteUtil.getContent(in, 0);
                sharedStream = new SharedByteArrayInputStream(content);
            }

            mimeMessage = expandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), sharedStream);
        }

        // Run mutators.
        try {
            runMimeMutators();
        } catch (Exception e) {
            wasMutated = false;
            // Original stream has been read, so get a new one.
            mimeMessage = expandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), getRawInputStream());
        }

        if (wasMutated()) {
            // Original data is now invalid.
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mimeMessage.writeTo(buffer);
            byte[] content = buffer.toByteArray();
            ByteUtil.closeStream(sharedStream);
            sharedStream = new SharedByteArrayInputStream(content);
            mimeMessage = expandedMessage = null;
            mimeMessage = expandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), sharedStream);
        }

        ExpandMimeMessage expand = new ExpandMimeMessage(mimeMessage);
        try {
            expand.expand();
            expandedMessage = expand.getExpanded();
        } catch (Exception e) {
            // roll back if necessary
            expandedMessage = mimeMessage;
            LOG.warn("exception while converting message; message will be analyzed unconverted", e);
        }

        // must set received-date before Lucene document is initialized
        if (receivedDate == null) {
            receivedDate = getZimbraDateHeader(mimeMessage);
        }
        setReceivedDate(receivedDate);
    }

    public boolean wasMutated() {
        return wasMutated;
    }

    public ParsedMessage setDefaultCharset(String charset) {
        defaultCharset = charset;
        if (mimeMessage instanceof ZMimeMessage) {
            ((ZMimeMessage) mimeMessage).setProperty("mail.mime.charset", charset);
        }
        if (expandedMessage != mimeMessage && expandedMessage instanceof ZMimeMessage) {
            ((ZMimeMessage) expandedMessage).setProperty("mail.mime.charset", charset);
        }
        subject = normalizedSubject = null;
        return this;
    }

    /** Applies all registered on-the-fly MIME converters to a copy of the
     *  encapsulated message (leaving the original message intact), then
     *  generates the list of message parts.
     *
     * @return the ParsedMessage itself
     * @throws ServiceException
     * @see #runMimeConverters() */
    private ParsedMessage parse() {
        if (parsed) {
            return this;
        }
        parsed = true;

        try {
            messageParts = Mime.getParts(expandedMessage);
            hasAttachments = Mime.hasAttachment(messageParts);
            hasTextCalendarPart = Mime.hasTextCalenndar(messageParts);
        } catch (Exception e) {
            ZimbraLog.index.warn("exception while parsing message; message will not be indexed", e);
            messageParts = new ArrayList<MPartInfo>();
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
        wasMutated = false;
        for (Class<? extends MimeVisitor> vclass : MimeVisitor.getMutators()) {
            try {
                wasMutated |= vclass.newInstance().accept(mimeMessage);
                if (mimeMessage != expandedMessage) {
                    ((MimeVisitor) vclass.newInstance()).accept(expandedMessage);
                }
            } catch (MessagingException e) {
                throw e;
            } catch (Exception e) {
                ZimbraLog.misc.warn("exception ignored running mutator; skipping", e);
            }
        }
    }

    private static final Set<String> ENCRYPTED_PART_TYPES = ImmutableSet.of(
            MimeConstants.CT_APPLICATION_SMIME, MimeConstants.CT_APPLICATION_PGP, MimeConstants.CT_MULTIPART_ENCRYPTED
    );

    /**
     * Analyze and extract text from all the "body" (non-attachment) parts of the message.
     * This step is required to properly generate the message fragment.
     */
    private void analyzeBodyParts() throws ServiceException {
        if (analyzedBodyParts) {
            return;
        }

        analyzedBodyParts = true;
        if (DebugConfig.disableMessageAnalysis) {
            return;
        }

        parse();

        try {
            Set<MPartInfo> mpiBodies = Mime.getBody(messageParts, false);

            // extract text from the "body" parts
            StringBuilder body = new StringBuilder();
            for (MPartInfo mpi : messageParts) {
                if (mpiBodies.contains(mpi)) {
                    String toplevelText = analyzePart(true, mpi);
                    if (toplevelText.length() > 0) {
                        appendToContent(body, toplevelText);
                    }
                }
                if (ENCRYPTED_PART_TYPES.contains(mpi.mContentType)) {
                    encrypted = true;
                }
            }

            // calculate the fragment -- requires body content
            bodyContent = body.toString().trim();
            fragment = Fragment.getFragment(bodyContent, hasTextCalendarPart);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("exception while analyzing message; message will be partially indexed", e);
        }
    }

    /**
     * Analyze and extract text from all attachments parts of the message
     */
    private void analyzeNonBodyParts() throws ServiceException {
        if (analyzedNonBodyParts) {
            return;
        }

        analyzedNonBodyParts = true;
        if (DebugConfig.disableMessageAnalysis) {
            return;
        }

        analyzeBodyParts();

        try {
            Set<MPartInfo> mpiBodies = Mime.getBody(messageParts, false);

            // extract text from the "non-body" parts
            StringBuilder fullContent = new StringBuilder(bodyContent);
            {
                for (MPartInfo mpi : messageParts) {
                    boolean isMainBody = mpiBodies.contains(mpi);
                    if (!isMainBody) {
                        String toplevelText = analyzePart(isMainBody, mpi);
                        if (toplevelText.length() > 0) {
                            appendToContent(fullContent, toplevelText);
                        }
                    }
                }
            }

            // requires FULL content (all parts)
            luceneDocuments.add(getMainBodyLuceneDocument(fullContent));

            // we're done with the body content (saved from analyzeBodyParts()) now
            bodyContent = "";

            if (numParseErrors > 0) {
                LOG.warn("Message had analysis errors in %d parts (Message-Id: %s, Subject: %s)",
                        numParseErrors, getMessageID(), getSubject());
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("exception while analyzing message; message will be partially indexed", e);
        }
    }

    /**
     * Extract all indexable text from this message.  This API should *only* be called if you are
     * sure you're going to add the message to the index.  The only callsites of this API should be
     * Mailbox and test utilities.  Don't call it unless you absolutely are sure you need to do so.
     */
    public void analyzeFully() throws ServiceException {
        analyzeNonBodyParts();
    }

    /**
     * Returns the {@link MimeMessage}.  Affected by both conversion and mutation.
     */
    public MimeMessage getMimeMessage() {
        return expandedMessage;
    }

    /**
     * Returns the original {@link MimeMessage}.  Affected by mutation but not conversion.
     */
    public MimeMessage getOriginalMessage() {
        return mimeMessage;
    }

    /**
     * Returns the raw MIME data.  Affected by mutation but not conversion.
     */
    public byte[] getRawData() throws IOException {
        return ByteUtil.getContent(getRawInputStream(), 1024);
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
    public InputStream getRawInputStream() throws IOException {
        if (sharedStream != null) {
            return ((SharedInputStream) sharedStream).newStream(0, -1);
        } else {
            return Mime.getInputStream(mimeMessage);
        }
    }

    public boolean isAttachmentIndexingEnabled() {
        return indexAttachments;
    }

    public List<MPartInfo> getMessageParts() {
        parse();
        return messageParts;
    }

    public boolean hasAttachments() {
        parse();
        return hasAttachments;
    }

    /**
     * Returns the {@link BlobInputStream} if this message is being streamed from a file, otherwise returns {@code null}.
     */
    public BlobInputStream getBlobInputStream() {
        if (sharedStream instanceof BlobInputStream) {
            return (BlobInputStream) sharedStream;
        }
        return null;
    }

    public boolean isStreamedFromDisk() {
        return getBlobInputStream() != null;
    }

    public int getPriorityBitmask() {
        MimeMessage mm = getMimeMessage();

        try {
            String xprio = mm.getHeader("X-Priority", null);
            if (xprio != null) {
                xprio = xprio.trim();
                if (xprio.startsWith("1") || xprio.startsWith("2")) {
                    return Flag.BITMASK_HIGH_PRIORITY;
                } else if (xprio.startsWith("3")) {
                    return 0;
                } else if (xprio.startsWith("4") || xprio.startsWith("5")) {
                    return Flag.BITMASK_LOW_PRIORITY;
                }
            }
        } catch (MessagingException e) {
        }

        try {
            String impt = mm.getHeader("Importance", null);
            if (impt != null) {
                impt = impt.trim().toLowerCase();
                if (impt.startsWith("high")) {
                    return Flag.BITMASK_HIGH_PRIORITY;
                } else if (impt.startsWith("normal")) {
                    return 0;
                } else if (impt.startsWith("low")) {
                    return Flag.BITMASK_LOW_PRIORITY;
                }
            }
        } catch (MessagingException e) {
        }

        return 0;
    }

    public boolean isList(String envSenderString) {
        MimeMessage mm = getMimeMessage();

        try {
            if (mm.getHeader("List-ID") != null) {
                return true;
            }
        } catch (MessagingException e) {
        }

        try {
            if ("list".equalsIgnoreCase(mm.getHeader("Precedence", null))) {
                return true;
            }
        } catch (MessagingException e) {
        }

        if (envSenderString != null) {
            try {
                // NB: 'strict' being 'true' causes <> to except
                InternetAddress envSender = new JavaMailInternetAddress(envSenderString, true);
                if (envSender.getAddress() != null) {
                    String[] envSenderAddrParts = EmailUtil.getLocalPartAndDomain(envSender.getAddress());
                    if (envSenderAddrParts != null) {
                        String sender = envSenderAddrParts[0].toLowerCase();
                        if (sender.startsWith("owner-") || sender.endsWith("-owner") ||
                                sender.indexOf("-request") != -1 || sender.equals("mailer-daemon") ||
                                sender.equals("majordomo") || sender.equals("listserv")) {
                            return true;
                        }
                    }
                }
            } catch (AddressException e) {
            }
        }

        return false;
    }

    private boolean isInReplyTo() {
        if (hasReplyToHeader != null) {
            return hasReplyToHeader;
        }
        String[] replyTo;
        try {
            replyTo = getMimeMessage().getHeader("In-Reply-To");
            hasReplyToHeader = replyTo != null && replyTo.length > 0 && replyTo[0].length() > 0;
            return hasReplyToHeader;
        } catch (MessagingException e) {
            LOG.warn("messaging exception getting In-Reply-To header", e);
            return false;
        }
    }

    public boolean isReply() {
        normalizeSubject();
        return subjectIsReply || isInReplyTo();
    }

    public String getSubject() {
        normalizeSubject();
        return subject;
    }

    public String getNormalizedSubject() {
        normalizeSubject();
        return normalizedSubject;
    }

    public String getFragment(Locale lc) {
        try {
            analyzeBodyParts();
        } catch (ServiceException e) {
            LOG.warn("Message analysis failed when getting fragment; fragment is: %s", fragment, e);
        }

        if (encrypted && fragment.isEmpty()) {
            return Strings.nullToEmpty(L10nUtil.getMessage(L10nUtil.MsgKey.encryptedMessageFragment, lc));
        } else {
            return fragment;
        }
    }

    /** Returns the message ID, or <tt>null</tt> if the message id cannot be
     *  determined. */
    public String getMessageID() {
        return Mime.getMessageID(getMimeMessage());
    }

    /** Returns all message-ids referenced by this message's headers.  This
     *  includes those in the message's {@code Message-ID} header, its {@code
     *  In-Reply-To} header, and its {@code References} header.  The enclosing
     *  angle brackets and any embedded comments and quoted-strings are
     *  stripped from the message-ids.
     * @return a non-{@code null}, mutable {@code Set} containing the message's
     *         references. */
    public Set<String> getAllReferences() {
        MimeMessage mm = getMimeMessage();

        Set<String> refs = new HashSet<String>();
        refs.addAll(Mime.getReferences(mm, "Message-ID"));
        refs.addAll(Mime.getReferences(mm, "In-Reply-To"));
        refs.addAll(Mime.getReferences(mm, "References"));
        refs.addAll(Mime.getReferences(mm, "Resent-Message-ID"));

        refs.remove(null);
        refs.remove("");
        return refs;
    }

    /**
     * Returns a comma-separated list of {@code To} addresses.
     */
    public String getRecipients() {
        if (recipients == null) {
            try {
                recipients = getMimeMessage().getHeader("To", ", ");
            } catch (MessagingException e) {
                recipients = "";
            }
        }
        return recipients;
    }

    /**
     * Returns the {@code To} addresses.
     */
    public List<ParsedAddress> getParsedRecipients() {
        if (parsedRecipients == null) {
            List<com.zimbra.common.mime.InternetAddress> addrs = com.zimbra.common.mime.InternetAddress.parseHeader(
                    getRecipients());
            if (addrs != null) {
                parsedRecipients = new ArrayList<ParsedAddress>(addrs.size());
                for (com.zimbra.common.mime.InternetAddress addr : addrs) {
                    parsedRecipients.add(new ParsedAddress(addr).parse());
                }
            } else {
                parsedRecipients = Collections.emptyList();
            }
        }
        return parsedRecipients;
    }

    /** Returns the value of the <tt>From</tt> header.  If not available,
     *  returns the value of the <tt>Sender</tt> header.  Returns an empty
     *  {@code String} if neither header is available. */
    public String getSender() {
        if (fromOrSender == null) {
            fromOrSender = Mime.getSender(getMimeMessage());
        }
        return fromOrSender;
    }

    private String getFrom() {
        if (from != null) {
            return from;
        }

        String from = null;
        try {
            from = getMimeMessage().getHeader("From", null);
        } catch (MessagingException ignore) {
        }
        if (from == null) {
            try {
                from = getMimeMessage().getHeader("Sender", null);
            } catch (MessagingException ignore) {
            }
        }
        this.from = from;
        return from;
    }

    private String getTo() {
        if (to != null) {
            return to;
        }

        String to = null;
        try {
            to = getMimeMessage().getHeader("To", ",");
        } catch (MessagingException ignore) {
        }
        this.to = to;
        return to;
    }

    private String getCc() {
        if (cc != null) {
            return cc;
        }

        String cc = null;
        try {
            cc = getMimeMessage().getHeader("Cc", ",");
        } catch (MessagingException ignore) {
        }
        this.cc = cc;
        return cc;
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
                if (froms != null && froms.length > 0 && froms[0] instanceof InternetAddress) {
                    return ((InternetAddress) froms[0]).getAddress();
                }
                Address sender = getMimeMessage().getSender();
                if (sender instanceof InternetAddress) {
                    return ((InternetAddress) sender).getAddress();
                }
            } else {
                // Sender header first, then From
                Address sender = getMimeMessage().getSender();
                if (sender instanceof InternetAddress) {
                    return ((InternetAddress) sender).getAddress();
                }
                Address[] froms = getMimeMessage().getFrom();
                if (froms != null && froms.length > 0 && froms[0] instanceof InternetAddress) {
                    return ((InternetAddress) froms[0]).getAddress();
                }
            }
        } catch (MessagingException e) {
        }

        return null;
    }

    public ParsedAddress getParsedSender() {
        if (parsedSender == null) {
            parsedSender = new ParsedAddress(getSender()).parse();
        }
        return parsedSender;
    }

    public String getReplyTo() {
        String replyTo = null;
        try {
            replyTo = getMimeMessage().getHeader("Reply-To", null);
            if (replyTo == null || replyTo.trim().isEmpty()) {
                return null;
            }
        } catch (Exception e) {
        }

        String sender = getSender();
        if (sender != null && sender.equals(replyTo)) {
            return null;
        }
        return replyTo;
    }

    public long getDateHeader() {
        if (dateHeader != -1) {
            return dateHeader;
        }
        dateHeader = getReceivedDate();
        try {
            Date date = getMimeMessage().getSentDate();
            if (date != null) {
                // prevent negative dates, which Lucene can't deal with
                dateHeader = Math.max(date.getTime(), 0L);
            }
        } catch (MessagingException e) {
        }

        return dateHeader;
    }

    public ParsedMessage setReceivedDate(long date) {
        // round to nearest second...
        if (date == DATE_HEADER) {
            receivedDate = getDateHeader();
        } else if (date != DATE_UNKNOWN) {
            receivedDate = (Math.max(0, date) / 1000) * 1000;
        }
        return this;
    }

    public long getReceivedDate() {
        if (receivedDate == -1) {
            receivedDate = System.currentTimeMillis();
        }
        assert receivedDate >= 0 : receivedDate;
        return receivedDate;
    }

    private static long getZimbraDateHeader(MimeMessage mm) {
        String zimbraHeader = null;
        try {
            zimbraHeader = mm.getHeader("X-Zimbra-Received", null);
            if (zimbraHeader == null || zimbraHeader.trim().isEmpty()) {
                return -1;
            }
        } catch (MessagingException mex) {
            return -1;
        }

        Date zimbraDate = null;
        synchronized (FORMAT) {
            try {
                zimbraDate = FORMAT.parse(zimbraHeader);
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
            LOG.warn("message analysis failed when getting lucene documents");
        }
        return luceneDocuments;
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
            if (hasTextCalendarPart) {
                analyzeFully();
            }
        } catch (ServiceException e) {
            // the calendar info should still be parsed
            LOG.warn("Message analysis failed when getting calendar info");
        }
        return calendarPartInfo;
    }

    /**
     * @return TRUE if there was a _temporary_ failure detected while analyzing the message.  In
     *         the case of a temporary failure, the message should be flagged and indexing re-tried
     *         at some point in the future
     */
    public boolean hasTemporaryAnalysisFailure() throws ServiceException {
        analyzeFully();
        return temporaryAnalysisFailure;
    }

    private IndexDocument getMainBodyLuceneDocument(StringBuilder fullContent)
            throws MessagingException, ServiceException {

        IndexDocument doc = new IndexDocument();
        doc.addMimeType("message/rfc822");
        doc.addPartName(LuceneFields.L_PARTNAME_TOP);
        doc.addFrom(getFrom());
        doc.addTo(getTo());
        doc.addCc(getCc());
        try {
            doc.addEnvFrom(getMimeMessage().getHeader("X-Envelope-From", ","));
        } catch (MessagingException ignore) {
        }
        try {
            doc.addEnvTo(getMimeMessage().getHeader("X-Envelope-To", ","));
        } catch (MessagingException ignore) {
        }

        String msgId = Strings.nullToEmpty(Mime.getHeader(getMimeMessage(), "message-id"));
        if (msgId.length() > 0) {
            if (msgId.charAt(0) == '<') {
                msgId = msgId.substring(1);
            }
            if (msgId.charAt(msgId.length() - 1) == '>') {
                msgId = msgId.substring(0, msgId.length() - 1);
            }
            if (msgId.length() > 0) {
                doc.addMessageId(msgId);
            }
        }

        // iterate all the message headers, add them to the structured-field data in the index
        MimeMessage mm = getMimeMessage();
        List<Part> parts = new ArrayList<Part>();
        parts.add(mm);
        try {
            if (mm.getContent() instanceof ZMimeMultipart) {
                ZMimeMultipart content = (ZMimeMultipart) mm.getContent();
                int numParts = content.getCount();
                for (int i = 0; i < numParts; i++) {
                    parts.add(content.getBodyPart(i));
                }
            }
        } catch (IOException ignore) {}
        for (Part part: parts) {
            Enumeration<?> en = part.getAllHeaders();
            while (en.hasMoreElements()) {
                Header h = (Header) en.nextElement();
                String key = h.getName().trim();
                String value = h.getValue();
                if (value != null) {
                    value = MimeUtility.unfold(value).trim();
                } else {
                    value = "";
                }
                if (key.length() > 0) {
                    String val;
                    if (value.length() == 0) {
                        // low-level tokenizer can't deal with blank header value, so we'll index
                        // some dummy value just so the header appears in the index.
                        // Users can query for the existence of the header with a query
                        // like #headername:*
                    	val = "_blank_";
                    } else {
                    	val = value;
                    }
                    doc.addField(String.format("%s:%s", key, val));
                    Integer intVal = Ints.tryParse(val);
                    if (intVal != null) {
                        //numeric values get indexed in a separate field
                        doc.addIntHeader(key.toLowerCase(), intVal);
                    }
                }
            }
        }
        String subject = getSubject();
        doc.addSubject(subject);

        doc.addContent(fullContent.toString());


        // Get the list of attachment content types from this message and any TNEF attachments
        Set<String> attachmentTypes = Mime.getAttachmentTypeList(messageParts);
        if (attachmentTypes.isEmpty()) {
            doc.addAttachments(LuceneFields.L_ATTACHMENT_NONE);
        } else {
            for (String attachment: attachmentTypes) {
                doc.addAttachments(attachment);
                doc.addAttachments(LuceneFields.L_ATTACHMENT_ANY);
            }
        }
        return doc;
    }

    /**
     * For every attachment, many of the lucene indexed fields from the top level
     * message are also indexed as part of the attachment: this is done so that the
     * attachment will show up if you do things like "type:pdf and from:foo"
     *
     * "this" --> top level doc
     * @param doc sub-document of top level
     */
    private IndexDocument setLuceneHeadersFromContainer(IndexDocument doc) {
        doc.addFrom(getFrom());
        doc.addTo(getTo());
        doc.addCc(getCc());

        String subject = getNormalizedSubject();
        if (!Strings.isNullOrEmpty(subject)) {
            doc.addSubject(subject);
        }

        return doc;
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
        calendarPartInfo = new CalendarPartInfo();
        calendarPartInfo.cal = cal;
        calendarPartInfo.method = cal.getMethod();
        calendarPartInfo.wasForwarded = false;
        MPartInfo parent = mpi;
        while ((parent = parent.getParent()) != null) {
            if (parent.isMessage()) {
                calendarPartInfo.wasForwarded = true;
            }
        }
    }

    /**
     * @return Extracted toplevel text (any text that should go into the toplevel indexed document)
     */
    private String analyzePart(boolean isMainBody, MPartInfo mpi) throws MessagingException, ServiceException {

        boolean ignoreCalendar;
        if (calendarPartInfo == null) {
            ignoreCalendar = isBouncedCalendar(mpi);
        } else {
            ignoreCalendar = true;
        }
        String methodParam = (new ContentType(mpi.getMimePart().getContentType())).getParameter("method");
        if (methodParam == null && !LC.calendar_allow_invite_without_method.booleanValue()) {
            ignoreCalendar = true;
        }
        String toRet = "";
        try {
            // ignore multipart "container" parts
            if (mpi.isMultipart()) {
                return toRet;
            }
            String ctype = mpi.getContentType();
            MimeHandler handler = MimeHandlerManager.getMimeHandler(ctype, mpi.getFilename());
            assert(handler != null);
            handler.setDefaultCharset(defaultCharset);

            Mime.repairTransferEncoding(mpi.getMimePart());

            if (handler.isIndexingEnabled()) {
                handler.init(mpi.getMimePart().getDataHandler().getDataSource());
                handler.setPartName(mpi.getPartName());
                handler.setFilename(mpi.getFilename());
                handler.setSize(mpi.getSize());

                // remember the first iCalendar attachment
                if (!ignoreCalendar && calendarPartInfo == null) {
                    ZVCalendar cal = handler.getICalendar();
                    if (cal != null) {
                        setCalendarPartInfo(mpi, cal);
                    }
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
                if ((isMainBody && (!handler.runsExternally() || indexAttachments)) ||
                            (indexAttachments && !DebugConfig.disableIndexingAttachmentsTogether)) {
                    toRet = handler.getContent();
                }

                if (indexAttachments && !DebugConfig.disableIndexingAttachmentsSeparately) {
                    // Each non-text MIME part is also indexed as a separate
                    // Lucene document.  This is necessary so that we can tell the
                    // client what parts match if a search matched a particular
                    // part.
                    IndexDocument doc = new IndexDocument(handler.getDocument());

                    String filename = handler.getFilename();
                    if (!Strings.isNullOrEmpty(filename)) {
                        filenames.add(filename);
                    }
                    doc.addSortSize(mpi.getMimePart().getSize());
                    luceneDocuments.add(setLuceneHeadersFromContainer(doc));
                }
            }

            // make sure we've got the text/calendar handler installed
            if (!ignoreCalendar && calendarPartInfo == null && ctype.equals(MimeConstants.CT_TEXT_CALENDAR)) {
                if (handler.isIndexingEnabled()) {
                    ZimbraLog.index.warn("TextCalendarHandler not correctly installed");
                }
                InputStream is = null;
                try {
                    String charset = mpi.getContentTypeParameter(MimeConstants.P_CHARSET);
                    if (charset == null || charset.trim().isEmpty()) {
                        charset = MimeConstants.P_CHARSET_DEFAULT;
                    }
                    is = mpi.getMimePart().getInputStream();
                    ZVCalendar cal = ZCalendarBuilder.build(is, charset);
                    if (cal != null) {
                        setCalendarPartInfo(mpi, cal);
                    }
                } catch (IOException ioe) {
                    ZimbraLog.index.warn("error reading text/calendar mime part", ioe);
                } finally {
                    ByteUtil.closeStream(is);
                }
            }
        } catch (MimeHandlerException e) {
            handleParseError(mpi, e);
        } catch (ObjectHandlerException e) {
            handleParseError(mpi, e);
        }
        return toRet;
    }

    /**
     * Log the error and index minimum information.
     *
     * @param mpi MIME info
     * @param error error to handle
     */
    private void handleParseError(MPartInfo mpi, Throwable error) {
        numParseErrors++;

        LOG.warn("Unable to parse part=%s filename=%s content-type=%s message-id=%s",
                mpi.getPartName(), mpi.getFilename(), mpi.getContentType(), getMessageID(), error);
        if (ConversionException.isTemporaryCauseOf(error)) {
            temporaryAnalysisFailure = true;
        }

        if (!Strings.isNullOrEmpty(mpi.getFilename())) {
            filenames.add(mpi.getFilename());
        }

        IndexDocument doc = new IndexDocument();
        doc.addMimeType(mpi.getContentType());
        doc.addPartName(mpi.getPartName());
        doc.addFilename(mpi.getFilename());
        try {
            doc.addSortSize(mpi.getMimePart().getSize());
        } catch (MessagingException ignore) {
        }
        luceneDocuments.add(setLuceneHeadersFromContainer(doc));
    }

    private static final void appendToContent(StringBuilder sb, String s) {
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(s);
    }

    // default set of complex subject prefix strings to ignore when normalizing
    private static final Set<String> SYSTEM_PREFIXES = Sets.newHashSet("accept:", "accepted:", "decline:", "declined:",
            "tentative:", "cancelled:", "new time proposed:", "read-receipt:", "share created:", "share accepted:");
    static {
        // installed locale-specific complex subject prefix strings to ignore when normalizing
        for (String localized : L10nUtil.getMessagesAllLocales(L10nUtil.MsgKey.calendarSubjectCancelled,
                L10nUtil.MsgKey.calendarReplySubjectAccept, L10nUtil.MsgKey.calendarReplySubjectDecline,
                L10nUtil.MsgKey.calendarReplySubjectTentative, L10nUtil.MsgKey.shareNotifSubject)) {
            SYSTEM_PREFIXES.add(localized.trim().toLowerCase() + ":");
        }
    }

    private static final int MAX_PREFIX_LENGTH = 3;

    @VisibleForTesting
    static Pair<String, Boolean> trimPrefixes(String subject) {
        if (Strings.isNullOrEmpty(subject)) {
            return new Pair<String, Boolean>("", false);
        }
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
                        if ((c == '(' || c == '[') && i > 0 && paren == -1) {
                            paren = i;
                        } else if ((c == ')' || c == ']') && paren != -1) {
                            matched &= i > paren + 1 && i == prefix.length() - 2;
                        } else if (!Character.isLetter(c)) {
                            matched &= c >= '0' && c <= '9' && paren != -1;
                        } else if (i >= MAX_PREFIX_LENGTH || paren != -1) {
                            matched = false;
                        }
                    }
                }
                if (matched) {
                    if (braced && subject.endsWith("]")) {
                        subject = subject.substring(colon + 1, subject.length() - 1);
                    } else {
                        subject = subject.substring(colon + 1);
                    }
                    trimmed = true;
                    continue;
                }
            }

            // trim mailing list prefixes (e.g. "[rev-dandom]")
            if (LC.conversation_ignore_maillist_prefix.booleanValue()) {
                int bclose;
                if (braced && (bclose = subject.indexOf(']')) > 0 && subject.lastIndexOf('[', bclose) == 0) {
                    String remainder = subject.substring(bclose + 1).trim();
                    if (remainder.length() > 0) {
                        subject = remainder;
                        continue;
                    }
                }
            }

            return new Pair<String, Boolean>(subject, trimmed);
        }
    }

    @VisibleForTesting
    static String compressWhitespace(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = value.length(), last = -1; i < len; i++) {
            char c = value.charAt(i);
            if (c <= ' ') {
                c = ' ';
                if (c == last) {
                    continue;
                }
            }
            sb.append((char) (last = c));
        }
        return sb.toString();
    }

    private void normalizeSubject() {
        if (normalizedSubject != null) {
            return;
        }
        try {
            normalizedSubject = subject = StringUtil.stripControlCharacters(Mime.getSubject(getMimeMessage()));
        } catch (MessagingException e) {
        }

        if (subject == null) {
            normalizedSubject = subject = "";
            subjectIsReply = false;
        } else {
            Pair<String, Boolean> normalized = trimPrefixes(subject);
            normalizedSubject = compressWhitespace(normalized.getFirst());
            subjectIsReply = normalized.getSecond();
            normalizedSubject = DbMailItem.normalize(normalizedSubject, DbMailItem.MAX_SUBJECT_LENGTH);
        }
    }

    public static String normalize(String subject) {
        String trimmed = compressWhitespace(trimPrefixes(StringUtil.stripControlCharacters(subject)).getFirst());
        return DbMailItem.normalize(trimmed, DbMailItem.MAX_SUBJECT_LENGTH);
    }

    public static boolean isReply(String subject) {
        return trimPrefixes(subject).getSecond();
    }

    /**
     * {@link Threader} is cached per mailbox as {@link ParsedMessage} is shared by multiple mailboxes in shared
     * delivery.
     */
    public Threader getThreader(Mailbox mbox) throws ServiceException {
        Threader threader = threaders.get(mbox);
        if (threader == null) {
            threader = new Threader(mbox, this);
            threaders.put(mbox, threader);
        }
        return threader;
    }

    public void setDataSourceId(String dsId) {
        dataSourceId = dsId;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void updateMimeMessage () throws IOException, MessagingException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);
        byte[] content = buffer.toByteArray();
        ByteUtil.closeStream(sharedStream);
        sharedStream = new SharedByteArrayInputStream(content);
        mimeMessage = expandedMessage = null;
        mimeMessage = expandedMessage = new Mime.FixedMimeMessage(JMSession.getSession(), sharedStream);
    }
}
