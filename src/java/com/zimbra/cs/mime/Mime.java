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
 * Created on Apr 17, 2004
 */
package com.zimbra.cs.mime;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.QCodec;

import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeCompoundHeader;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeHeader;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.JMSession;

/**
 * @author schemers
 */
public class Mime {

    static Log sLog = LogFactory.getLog(Mime.class);

    private static final int MAX_DECODE_BUFFER = 2048;

    private static final Set<String> TRANSFER_ENCODINGS = new HashSet<String>(Arrays.asList(
            MimeConstants.ET_7BIT, MimeConstants.ET_8BIT, MimeConstants.ET_BINARY, MimeConstants.ET_QUOTED_PRINTABLE, MimeConstants.ET_BASE64
    ));

    public static class FixedMimeMessage extends MimeMessage {
        public FixedMimeMessage(Session s)  { super(s); }
        public FixedMimeMessage(Session s, InputStream is) throws MessagingException  { super(s, is); }
        public FixedMimeMessage(MimeMessage mm) throws MessagingException  { super(mm); }

        public Session getSession() { return this.session; }
        public FixedMimeMessage setSession(Session s)  { session = s;  return this; }

        @Override protected void updateHeaders() throws MessagingException {
            String msgid = getMessageID();
            super.updateHeaders();
            if (msgid != null)
                setHeader("Message-ID", msgid);
        }
    }

    private static final Set<String> INLINEABLE_TYPES = new HashSet<String>(Arrays.asList("image/jpeg", "image/png", "image/gif"));

    /**
     * return complete List of MPartInfo objects. 
     * @param mm
     * @return
     * @throws IOException
     * @throws MessagingException
     */
    public static List<MPartInfo> getParts(MimeMessage mm) throws IOException, MessagingException {
        List<MPartInfo> parts = listParts(mm);
        Set<MPartInfo> bodies = getBody(parts, true);
        for (MPartInfo mpi : parts) {
            mpi.mIsFilterableAttachment = isFilterableAttachment(mpi, bodies);
            if (mpi.mIsFilterableAttachment && !mpi.getContentType().equals(MimeConstants.CT_XML_ZIMBRA_SHARE))
                mpi.mIsToplevelAttachment = bodies == null || !bodies.contains(mpi) || !INLINEABLE_TYPES.contains(mpi.mContentType);
        }
        return parts;
    }

    private static List<MPartInfo> listParts(MimePart root) throws MessagingException, IOException {
        List<MPartInfo> parts = new ArrayList<MPartInfo>();

        LinkedList<MPartInfo> queue = new LinkedList<MPartInfo>();
        queue.add(generateMPartInfo(root, null, "", 0));

        while (!queue.isEmpty()) {
            MPartInfo mpart = queue.removeFirst();
            MimePart mp = mpart.getMimePart();
            parts.add(mpart);

            String cts = mpart.mContentType;
            boolean isMultipart = cts.startsWith(MimeConstants.CT_MULTIPART_PREFIX);
            boolean isMessage = !isMultipart && cts.equals(MimeConstants.CT_MESSAGE_RFC822);

            if (isMultipart) {
                // IMAP part numbering is screwy: top-level multipart doesn't get a number
                String prefix = mpart.mPartName.length() > 0 ? (mpart.mPartName + '.') : "";
                if (mp instanceof MimeMessage)
                    mpart.mPartName = prefix + "TEXT";
                Object content = getMultipartContent(mp, cts);
                if (content instanceof MimeMultipart) {
                    MimeMultipart multi = (MimeMultipart) content;
                    mpart.mChildren = new ArrayList<MPartInfo>(multi.getCount());
                    for (int i = 1; i <= multi.getCount(); i++)
                        mpart.mChildren.add(generateMPartInfo((MimePart) multi.getBodyPart(i - 1), mpart, prefix + i, i));
                    queue.addAll(0, mpart.mChildren);
                }
            } else if (isMessage) {
                MimeMessage mm = getMessageContent(mp);
                if (mm != null) {
                    MPartInfo child = generateMPartInfo(mm, mpart, mpart.mPartName, 0);
                    queue.addFirst(child);
                    mpart.mChildren = Arrays.asList(child);
                }
            } else {
                // nothing to do at this stage
            }
        }

        return parts;
    }

    private static MPartInfo generateMPartInfo(MimePart mp, MPartInfo parent, String prefix, int partNum) {
        boolean inDigest = parent != null && parent.mContentType.equals(MimeConstants.CT_MULTIPART_DIGEST);
        String ctdefault = inDigest ? MimeConstants.CT_MESSAGE_RFC822 : MimeConstants.CT_DEFAULT;
        String cts = getContentType(mp, ctdefault);

        String disp = null, filename = getFilename(mp);
        int size = 0;
        try {
            disp = mp.getDisposition();
        } catch (Exception e) { }
        try {
            size = mp.getSize();
        } catch (MessagingException me) { }

        // the top-level part of a non-multipart message is numbered "1"
        boolean isMultipart = cts.startsWith(MimeConstants.CT_MULTIPART_PREFIX);
        if (!isMultipart && mp instanceof MimeMessage)
            prefix = (prefix.length() > 0 ? (prefix + ".") : "") + '1';

        MPartInfo mpart = new MPartInfo();
        mpart.mPart = mp;
        mpart.mParent = parent;
        mpart.mContentType = cts;
        mpart.mPartName = prefix;
        mpart.mPartNum = partNum;
        mpart.mSize = size;
        mpart.mChildren = null;
        mpart.mDisposition = (disp == null ? (inDigest  && cts.equals(MimeConstants.CT_MESSAGE_RFC822) ? Part.ATTACHMENT : "") : disp.toLowerCase());
        mpart.mFilename = (filename == null ? "" : filename.toLowerCase());
        return mpart;
    }

    private static MimeMultipart validateMultipart(MimeMultipart multi, MimePart mp) throws MessagingException, IOException {
        ContentType ctype = new ContentType(mp.getContentType());
        try {
            if (!ctype.containsParameter("generated") && !findStartBoundary(mp, ctype.getParameter("boundary")))
                return new MimeMultipart(new RawContentMultipartDataSource(mp, ctype));
            multi.getCount();
        } catch (ParseException pe) {
            multi = new MimeMultipart(new FixedMultipartDataSource(mp, ctype));
        } catch (MessagingException me) {
            multi = new MimeMultipart(new FixedMultipartDataSource(mp, ctype));
        }
        return multi;
    }

    /** Max length (in bytes) that a MIME multipart preamble can be before
     *  we give up and wrap the whole multipart in a text/plain. */
    private static final int MAX_PREAMBLE_LENGTH = 1024;

    /** Returns whether the given "boundary" string occurs within the first
     *  {@link #MAX_PREAMBLE_LENGTH} bytes of the {@link MimePart}'s content.*/
    private static boolean findStartBoundary(MimePart mp, String boundary) throws IOException {
        InputStream is = null;
        try {
            is = getRawInputStream(mp);
        } catch (MessagingException me) {
            return true;
        }
        final int blength = boundary == null ? 0 : boundary.length();
        int bindex = 0, dashes = 0;
        boolean failed = false;
        try {
            for (int i = 0; i < MAX_PREAMBLE_LENGTH; i++) {
                int c = is.read();
                if (c == -1) {
                    return false;
                } else if (c == '\r' || c == '\n') {
                    if (!failed && (boundary == null ? bindex > 0 : bindex == blength))
                        return true;
                    bindex = dashes = 0;  failed = false;
                } else if (failed) {
                    continue;
                } else if (dashes != 2) {
                    if (c == '-')
                        dashes++;
                    else
                        failed = true;
                } else if (boundary == null) {
                    if (Character.isWhitespace(c))
                        failed = true;
                    bindex++;
                } else {
                    if (bindex >= blength || c != boundary.charAt(bindex++))
                        failed = true;
                }
            }
        } finally {
            ByteUtil.closeStream(is);
        }
        return false;
    }

    static InputStream getRawInputStream(MimePart mp) throws MessagingException {
        if (mp instanceof MimeBodyPart)
            return ((MimeBodyPart) mp).getRawInputStream();
        if (mp instanceof MimeMessage)
            return ((MimeMessage) mp).getRawInputStream();
        return new ByteArrayInputStream(new byte[0]);
    }

    private static class FixedMultipartDataSource implements DataSource {
        private final MimePart mMimePart;
        private final ContentType mContentType;
        FixedMultipartDataSource(MimePart mp, ContentType ctype) {
            mMimePart = mp;
            mContentType = ctype;
        }

        public ContentType getParsedContentType()  { return mContentType; }

        public String getContentType()         { return mContentType.toString(); }
        public String getName()                { return null; }
        public OutputStream getOutputStream()  { throw new UnsupportedOperationException(); }
        public InputStream getInputStream() throws IOException {
            try {
                return getRawInputStream(mMimePart);
            } catch (MessagingException e) {
                IOException ioex = new IOException("failed to get raw input stream for mime part");
                ioex.initCause(e);
                throw ioex;
            }
        }
    }

    private static class RawContentMultipartDataSource extends FixedMultipartDataSource {
        RawContentMultipartDataSource(MimePart mp, ContentType ctype) {
            super(mp, ctype);
        }

        @Override public InputStream getInputStream() throws IOException {
            return new RawContentInputStream(super.getInputStream());
        }

        private class RawContentInputStream extends FilterInputStream {
            private final String mBoundary;
            private byte[] mPrologue;
            private byte[] mEpilogue;
            private int mPrologueIndex = 0, mEpilogueIndex = 0;
            private boolean mInPrologue = true, mInContent = false, mInEpilogue = false;

            RawContentInputStream(InputStream is) {
                super(is);

                String explicitBoundary = getParsedContentType().getParameter("boundary");
                mBoundary = explicitBoundary == null ? "_-_" + UUID.randomUUID().toString() : explicitBoundary;
                byte[] boundary = mBoundary.getBytes();

                mPrologue = new byte[2 + boundary.length + 4];
                mPrologue[0] = mPrologue[1] = '-';
                System.arraycopy(boundary, 0, mPrologue, 2, boundary.length);
                mPrologue[boundary.length + 2] = mPrologue[boundary.length + 4] = '\r';
                mPrologue[boundary.length + 3] = mPrologue[boundary.length + 5] = '\n';

                mEpilogue = new byte[4 + boundary.length + 4];
                mEpilogue[0] = mEpilogue[boundary.length + 6] = '\r';
                mEpilogue[1] = mEpilogue[boundary.length + 7] = '\n';
                mEpilogue[2] = mEpilogue[3] = '-';
                System.arraycopy(boundary, 0, mEpilogue, 4, boundary.length);
                mEpilogue[boundary.length + 4] = mEpilogue[boundary.length + 5] = '-';
            }

            @Override public int available() throws IOException {
                return mPrologue.length - mPrologueIndex + super.available() + mEpilogue.length - mEpilogueIndex;
            }

            @Override public int read() throws IOException {
                int c;
                if (mInPrologue) {
                    c = mPrologue[mPrologueIndex++];
                    if (mPrologueIndex >= mPrologue.length) {
                        mInPrologue = false;  mInContent = true;
                    }
                } else if (mInContent) {
                    c = super.read();
                    if (c == -1) {
                        c = mEpilogue[0];  mEpilogueIndex = 1;
                        mInContent = false;  mInEpilogue = true;
                    }
                } else if (mInEpilogue) {
                    c = mEpilogue[mEpilogueIndex++];
                    if (mEpilogueIndex >= mEpilogue.length) {
                        mInEpilogue = false;
                    }
                } else {
                    c = -1;
                }
                return c;
            }

            @Override public int read(byte[] b, int off, int len) throws IOException {
                if (b == null)
                    throw new NullPointerException();
                else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0))
                    throw new IndexOutOfBoundsException();
                else if (len == 0)
                    return 0;
                else if (!mInPrologue && !mInContent && !mInEpilogue)
                    return -1;

                int remaining = len;
                if (mInPrologue) {
                    int prologue = Math.min(remaining, mPrologue.length - mPrologueIndex);
                    System.arraycopy(mPrologue, mPrologueIndex, b, off, prologue);
                    mPrologueIndex += prologue;
                    if (mPrologueIndex >= mPrologue.length) {
                        mInPrologue = false;  mInContent = true;
                    }
                    remaining -= prologue;  off += prologue;
                }
                if (remaining == 0)
                    return len;
                if (mInContent) {
                    int content = super.read(b, off, remaining);
                    if (content == -1) {
                        mInContent = false;  mInEpilogue = true;
                    } else {
                        remaining -= content;  off += content;
                    }
                }
                if (remaining == 0)
                    return len;
                if (mInEpilogue) {
                    int epilogue = Math.min(remaining, mEpilogue.length - mEpilogueIndex);
                    System.arraycopy(mEpilogue, mEpilogueIndex, b, off, epilogue);
                    mEpilogueIndex += epilogue;
                    if (mEpilogueIndex >= mEpilogue.length) {
                        mInEpilogue = false;
                    }
                    remaining -= epilogue;  off += epilogue;
                }
                return len - remaining;
            }
        }
    }

    /** Returns the MimeMessage object encapsulating a MIME part with
     *  content-type "message/rfc822".  Use this method instead of
     *  {@link Part#getContent()} to work around JavaMail's fascism about
     *  proper MIME format and failure to support RFC 2184. */
    public static MimeMessage getMessageContent(MimePart message822Part) throws IOException, MessagingException {
        String ctype = getContentType(message822Part);
        if (MimeConstants.CT_MESSAGE_RFC822.equals(ctype)) {
            // JavaMail will only return a correct MimeMessage if the Content-Type header was set correctly
            Object content = message822Part.getContent();
            if (content instanceof MimeMessage)
                return (MimeMessage) content;
        }

        InputStream is = null;
        try {
            // handle unparsed content due to multipart/digest or miscapitalization of content-type value
            return new FixedMimeMessage(JMSession.getSession(), is = message822Part.getInputStream());
        } catch (Exception e) {
        } finally {
            ByteUtil.closeStream(is);
        }
        return null;
    }

    /** Returns the MimeMultipart object encapsulating the body of a MIME
     *  part with content-type "multipart/*".  Use this method instead of
     *  {@link Part#getContent()} to work around JavaMail's fascism about
     *  proper MIME format and failure to support RFC 2184. */
    public static MimeMultipart getMultipartContent(MimePart multipartPart, String contentType) throws IOException, MessagingException {
        Object content = multipartPart.getContent();
        MimeMultipart mmp = null;
        if (content instanceof MimeMultipart) {
            mmp = (MimeMultipart) content;
        } else if (content instanceof InputStream) {
            try {
                // handle unparsed content due to miscapitalization of content-type value
                mmp = new MimeMultipart(new InputStreamDataSource((InputStream) content, contentType));
            } catch (Exception e) {
            } finally {
                ByteUtil.closeStream((InputStream) content);
            }
        }
        if (mmp == null)
            return null;
        return validateMultipart(mmp, multipartPart);
    }

    /** Returns a String containing the text content of the MimePart.  If the
     *  part's specified charset is unknown, defaults first to the user's
     *  preferred charset and then to the to the system's default charset.
     *  Use this method instead of {@link Part#getContent()} to work around
     *  JavaMail's fascism about proper MIME format and failure to support
     *  RFC 2184. */
    public static String getStringContent(MimePart textPart, String defaultCharset) throws IOException, MessagingException {
        repairTransferEncoding(textPart);
        return decodeText(textPart.getInputStream(), textPart.getContentType(), defaultCharset);
    }
    
    /** Returns a <tt>Reader</tt> for the text content of the <tt>MimePart</tt>.  If the
     *  part's specified charset is unknown, defaults first to the user's
     *  preferred charset and then to the to the system's default charset.
     *  Use this method instead of {@link Part#getContent()} to work around
     *  JavaMail's fascism about proper MIME format and failure to support
     *  RFC 2184. */
    public static Reader getContentAsReader(MimePart textPart, String defaultCharset) throws IOException, MessagingException {
        repairTransferEncoding(textPart);
        return getTextReader(textPart.getInputStream(), textPart.getContentType(), defaultCharset);
    }

    public static void repairTransferEncoding(MimePart mp) throws MessagingException {
        String cte = mp.getHeader("Content-Transfer-Encoding", null);
        if (cte != null && !TRANSFER_ENCODINGS.contains(cte.toLowerCase().trim()))
            mp.removeHeader("Content-Transfer-Encoding");
    }

    private static final class InputStreamDataSource implements DataSource {
        private InputStream is;
        private String type;

        InputStreamDataSource(InputStream stream, String contentType) {
            is   = stream;
            type = contentType;
        }

        public String getContentType() { return type; }
        public String getName()        { return null; }

        public InputStream getInputStream()   { return is; }
        public OutputStream getOutputStream() { return null; }
    }

    public static MimePart getMimePart(MimePart mp, String part) throws IOException, MessagingException {
        if (mp == null)
            return null;
        if (part == null || part.trim().equals(""))
            return mp;
        part = part.trim();

        boolean digestParent = false;
        String[] subpart = part.split("\\.");
        for (int i = 0; i < subpart.length; i++) {
            int index = Integer.parseInt(subpart[i]);
            if (index <= 0)
                return null;
            // the content-type determines the expected substructure
            String ct = getContentType(mp, digestParent ? MimeConstants.CT_MESSAGE_RFC822 : MimeConstants.CT_DEFAULT);
            if (ct == null)
                return null;
            digestParent = ct.equals(MimeConstants.CT_MULTIPART_DIGEST);

            if (ct.startsWith(MimeConstants.CT_MULTIPART_PREFIX)) {
                MimeMultipart mmp = getMultipartContent(mp, ct);
                if (mmp != null && mmp.getCount() >= index) {
                    BodyPart bp = mmp.getBodyPart(index - 1);
                    if (bp instanceof MimePart) {
                        mp = (MimePart) bp;
                        continue;
                    }
                }
            } else if (mp instanceof MimeMessage && index == 1 && i == subpart.length - 1) {
                // the top-level part of a non-multipart message is numbered "1"
                break;
            } else if (ct.equals(MimeConstants.CT_MESSAGE_RFC822)) {
                MimeMessage content = getMessageContent(mp);
                if (content != null) {
                    if (mp instanceof MimeMessage) {
                        // the top-level part of a non-multipart message is numbered "1"
                        if (index != 1)
                            return null;
                    } else {
                    	i--;
                    }
                    mp = content;
                    continue;
                }
            }
            return null;
        }
        return mp;
    }

    /**
     * Returns true if we consider this to be an attachment for the sake of "filtering" by attachments.
     * i.e., if someone searches for messages with attachment types of "text/plain", we probably wouldn't want
     * every multipart/mixed message showing up, since 99% of them will have a first body part of text/plain.
     * 
     * Note: Zimbra folder sharing notifications are not considered attachments for this purpose.
     * 
     * @param mpi
     * @return
     */
     private static boolean isFilterableAttachment(MPartInfo mpi, Set<MPartInfo> bodies) {
        MPartInfo parent = mpi.getParent();
        String ctype = mpi.getContentType();

        // multiparts are never attachments
        if (ctype.startsWith(MimeConstants.CT_MULTIPART_PREFIX))
            return false;

        if (ctype.startsWith(MimeConstants.CT_TEXT_PREFIX)) {
            // ignore top-level text/* types
            if (parent == null || (mpi.getPartNum() == 1 && parent.getContentType().equals(MimeConstants.CT_MESSAGE_RFC822)))
                return false;

            // inlined text parts are not filterable attachments
            if (bodies != null && bodies.contains(mpi))
                return false;

            // ignore body parts with a parent of multipart/alternative
            if (parent != null && parent.getContentType().equals(MimeConstants.CT_MULTIPART_ALTERNATIVE))
                return false;

            // ignore if: it is the first body part, and has a multipart/* parent, and that
            //   multipart's parent is null or message/rfc822
            if (mpi.getPartNum() == 1) {
                if (parent != null && parent.getContentType().startsWith(MimeConstants.CT_MULTIPART_PREFIX)) {
                    MPartInfo pp = parent.getParent();
                    if (pp == null || pp.getContentType().equals(MimeConstants.CT_MESSAGE_RFC822))
                        return false;
                }
            }
        }
        
        // Zimbra folder sharing notifications are not considered attachments.
        if (ctype.equals(MimeConstants.CT_XML_ZIMBRA_SHARE))
            return false;
        
        return true;
     }

     /**
      * Given a list of <code>MPartInfo</code>s (as returned from {@link #getParts}),
      * returns a <code>Set</code> of unique content-type strings, or an
      * empty set if there are no attachments.
      */
     public static Set<String> getAttachmentList(List<MPartInfo> parts) {
         // get a set of all the content types 
         HashSet<String> set = new HashSet<String>();
         for (MPartInfo mpi : parts) {
             if (mpi.isFilterableAttachment())
                 set.add(mpi.getContentType());
         }
         return set;
     }
 
     /** Returns true if any of the given message parts qualify as top-level
      *  "attachments" for the purpose of displaying the little paperclip icon
      *  in the web UI.  Note that Zimbra folder sharing notifications are
      *  expressly *not* considered attachments for this purpose. */
     public static boolean hasAttachment(List<MPartInfo> parts) {
         for (MPartInfo mpi : parts) {
             if (mpi.mIsToplevelAttachment)
                 return true;
         }
         return false;
     }
	
    /** Returns true if any of the given message parts has a content-type
     *  of text/calendar */
    public static boolean hasTextCalenndar(List<MPartInfo> parts) {
        for (MPartInfo mpi : parts) {
            if (MimeConstants.CT_TEXT_CALENDAR.equals(mpi.getContentType()))
                return true;
        }
        return false;
    }

    private static final InternetAddress[] NO_ADDRESSES = new InternetAddress[0];

    public static InternetAddress[] parseAddressHeader(String header) {
        return parseAddressHeader(header, true);
    }

    public static InternetAddress[] parseAddressHeader(MimeMessage mm, String headerName) {
        return parseAddressHeader(mm, headerName, true);
    }

    public static InternetAddress[] parseAddressHeader(MimeMessage mm, String headerName, boolean expandGroups) {
        try {
            return parseAddressHeader(mm.getHeader(headerName, ","), expandGroups);
        } catch (MessagingException e) {
            return NO_ADDRESSES;
        }
    }

    public static InternetAddress[] parseAddressHeader(String header, boolean expandGroups) {
        if (header == null || header.trim().equals(""))
            return NO_ADDRESSES;
        header = header.trim();

        InternetAddress[] addresses;
        try {
            addresses = InternetAddress.parseHeader(header, false);
        } catch (AddressException e) {
            try {
                return new InternetAddress[] { new InternetAddress(null, header, MimeConstants.P_CHARSET_UTF8) };
            } catch (UnsupportedEncodingException e1) {
                return NO_ADDRESSES;
            }
        }

        if (!expandGroups)
            return addresses;
        boolean hasGroups = false;
        for (InternetAddress addr : addresses) {
            if (addr.isGroup()) {
                hasGroups = true;  break;
            }
        }
        if (!hasGroups)
            return addresses;

        // if we're here, we need to expand at least one group...
        List<InternetAddress> expanded = new ArrayList<InternetAddress>();
        for (InternetAddress addr : addresses) {
            if (!addr.isGroup()) {
                expanded.add(addr);
            } else {
                try {
                    InternetAddress[] members = addr.getGroup(false);
                    if (members == null)
                        expanded.add(addr);
                    else
                        for (InternetAddress member : members)
                            expanded.add(member);
                } catch (AddressException e) {
                    expanded.add(addr);
                }
            }
        }
        return expanded.toArray(new InternetAddress[expanded.size()]);
    }

    static RecipientType[] sRcptTypes = new RecipientType[] {
        RecipientType.TO, RecipientType.CC, RecipientType.BCC
    };

    /**
     * Remove all email addresses in rcpts from To/Cc/Bcc headers of a
     * MimeMessage.
     * @param mm
     * @param rcpts
     * @throws MessagingException
     */
    public static void removeRecipients(MimeMessage mm, String[] rcpts)
    throws MessagingException {
        for (RecipientType rcptType : sRcptTypes) {
            Address[] addrs = mm.getRecipients(rcptType);
            if (addrs == null) continue;
            ArrayList<InternetAddress> list = new ArrayList<InternetAddress>(addrs.length);
            for (int j = 0; j < addrs.length; j++) {
                InternetAddress inetAddr = (InternetAddress) addrs[j];
                String addr = inetAddr.getAddress();
                boolean match = false;
                for (int k = 0; k < rcpts.length; k++)
                    if (addr.equalsIgnoreCase(rcpts[k]))
                        match = true;
                if (!match)
                    list.add(inetAddr);
            }
            if (list.size() < addrs.length) {
                InternetAddress[] newRcpts = new InternetAddress[list.size()];
                list.toArray(newRcpts);
                mm.setRecipients(rcptType, newRcpts);
            }
        }
    }

    /** Determines the "primary/subtype" part of a Multipart's Content-Type
     *  header.  Uses a permissive, RFC2231-capable parser, and defaults
     *  when appropriate. */
    public static final String getContentType(Multipart multi) {
        return getContentType(multi.getContentType());
    }

    /** Determines the "primary/subtype" part of a Part's Content-Type
     *  header.  Uses a permissive, RFC2231-capable parser, and defaults
     *  when appropriate. */
    public static final String getContentType(MimePart mp) {
        return getContentType(mp, MimeConstants.CT_DEFAULT);
    }

    /** Determines the "primary/subtype" part of a Part's Content-Type
     *  header.  Uses a permissive, RFC2231-capable parser, and defaults
     *  as indicated. */
    public static final String getContentType(MimePart mp, String ctdefault) {
        try {
            String cthdr = mp.getHeader("Content-Type", null);
            if (cthdr == null || cthdr.trim().equals(""))
                return ctdefault;
            return getContentType(cthdr);
        } catch (MessagingException e) {
            ZimbraLog.extensions.warn("could not fetch part's content-type; defaulting to " + MimeConstants.CT_DEFAULT, e);
            return MimeConstants.CT_DEFAULT;
        }
    }

    /** Determines the "primary/subtype" part of a Content-Type header
     *  string.  Uses a permissive, RFC2231-capable parser, and defaults
     *  when appropriate. */
    public static final String getContentType(String cthdr) {
        return new ContentType(cthdr).getValue().trim();
    }

    /** Reads the specified <code>InputStream</code> into a <code>String</code>.
     *  <code>contentType</code> must of type "text/*". If a valid charset
     *  parameter is present in the Content-Type string, it is used as the
     *  charset for decoding the text.  If not, we fall back to the user's
     *  default charset preference.  If both of those options fail, the
     *  platform default is used.
     * 
     * @param input  The InputStream to decode.
     * @param contentType  The Content-Type of the stream, which must be "text/*".
     * @parame defaultCharset  The user's default charset preference */
    public static String decodeText(InputStream input, String contentType, String defaultCharset) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try {
            Reader reader = getTextReader(input, contentType, defaultCharset);
            char[] cbuff = new char[MAX_DECODE_BUFFER];
            int num;
            while ( (num = reader.read(cbuff, 0, cbuff.length)) != -1)
                buffer.append(cbuff, 0, num);
        } finally {
            ByteUtil.closeStream(input);
        }
        return buffer.toString();
    }

    private static boolean SUPPORTS_CP1252 = Charset.isSupported(MimeConstants.P_CHARSET_CP1252);
    private static boolean SUPPORTS_GBK = Charset.isSupported(MimeConstants.P_CHARSET_GBK);

    private static final boolean DEFAULT_CP1252 = SUPPORTS_CP1252 && Charset.defaultCharset().name().equals(MimeConstants.P_CHARSET_LATIN1);
    private static final boolean DEFAULT_GBK = SUPPORTS_GBK && Charset.defaultCharset().name().equals(MimeConstants.P_CHARSET_EUC_CN);

    /** Returns a reader that decodes the specified <code>InputStream</code>.
     *  <code>contentType</code> must of type "text/*".  If a valid charset
     *  parameter is present in the Content-Type string, it is used as the
     *  charset for decoding the text.  If not, we fall back to the user's
     *  default charset preference.  If both of those options fail, the
     *  platform default is used.
     * 
     * @param input  The InputStream to decode.
     * @param contentType  The stream's Content-Type, which must be "text/*".
     * @param defaultCharset  The user's default charset preference */
    public static Reader getTextReader(InputStream input, String contentType, String defaultCharset) {
        Reader reader = null;

    	String charset = getCharset(contentType);
        if (charset != null) {
            charset = charset.toLowerCase();
            // windows-1252 is a superset of iso-8859-1 and they're often confused, so use cp1252 in its place
            if (SUPPORTS_CP1252 && charset.equals(MimeConstants.P_CHARSET_LATIN1))
                reader = getReader(input, MimeConstants.P_CHARSET_CP1252);
            else if (SUPPORTS_GBK && (charset.equals(MimeConstants.P_CHARSET_GB2312) || charset.equals(MimeConstants.P_CHARSET_EUC_CN)))
                reader = getReader(input, MimeConstants.P_CHARSET_GBK);
            if (reader == null)
                reader = getReader(input, charset);
        }

        // if either there was no explicit charset on the part or it was invalid, try the user's personal default charset
        if (reader == null && defaultCharset != null && !defaultCharset.trim().equals("")) {
            defaultCharset = defaultCharset.toLowerCase();
            // windows-1252 is a superset of iso-8859-1 and they're often confused, so use cp1252 in its place
            if (SUPPORTS_CP1252 && defaultCharset.equals(MimeConstants.P_CHARSET_LATIN1))
                reader = getReader(input, MimeConstants.P_CHARSET_CP1252);
            else if (SUPPORTS_GBK && (defaultCharset.equals(MimeConstants.P_CHARSET_GB2312) || defaultCharset.equals(MimeConstants.P_CHARSET_EUC_CN)))
                reader = getReader(input, MimeConstants.P_CHARSET_GBK);
            if (reader == null)
                reader = getReader(input, defaultCharset);
        }

        // if the user's default charset was also either unspecified or unavailable, go with the JVM's default charset
        if (reader == null && DEFAULT_CP1252)
            reader = getReader(input, MimeConstants.P_CHARSET_CP1252);
        else if (reader == null && DEFAULT_GBK)
            reader = getReader(input, MimeConstants.P_CHARSET_GBK);
        if (reader == null)
            reader = new InputStreamReader(input);

        return reader;
    }

    /** Returns a <code>Reader</code> for the <code>InputStream</code> using
     *  the supplied <tt>charset</tt> to decode.  Returns <tt>null</tt> if
     *  that charset is not available. */
    private static Reader getReader(InputStream is, String charset) {
        try {
            return new InputStreamReader(is, charset);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String getCharset(String contentType) {
        String charset = new ContentType(contentType).getParameter(MimeConstants.P_CHARSET);
        if (charset == null || charset.trim().equals(""))
            charset = null;
        return charset;
    }

    public static String encodeFilename(String filename) {
        try {
            // JavaMail doesn't use RFC 2231 encoding, and we're not going to, either...
            if (!StringUtil.isAsciiString(filename))
                return new QCodec().encode(filename, MimeConstants.P_CHARSET_UTF8);
        } catch (EncoderException ee) { }
        return filename;
    }

    public static String getFilename(MimePart mp) {
        String name = null;

        // first, check the Content-Disposition header for the "filename" parameter
        try {
            String cdisp = mp.getHeader("Content-Disposition", null);
            if (cdisp != null) {
                // will also catch (legal, but uncommon) RFC 2231 encoded filenames
                //   (things like filename*=UTF-8''%E3%82%BD%E3%83%AB%E3%83%86%E3%82%A3.rtf)
                MimeCompoundHeader mhdr = new MimeCompoundHeader(cdisp);
                if (mhdr.containsParameter("filename"))
                    name = mhdr.getParameter("filename");
            }
        } catch (MessagingException me) { }

        // if we didn't find anything, check the Content-Type header for the "name" parameter
        if (name == null) {
            try {
                String ctype = mp.getHeader("Content-Type", null);
                if (ctype != null) {
                    // will also catch (legal, but uncommon) RFC 2231 encoded filenames
                    //   (things like name*=UTF-8''%E3%82%BD%E3%83%AB%E3%83%86%E3%82%A3.rtf)
                    MimeCompoundHeader mhdr = new MimeCompoundHeader(ctype);
                    if (mhdr.containsParameter("name"))
                        name = mhdr.getParameter("name");
                }
            } catch (MessagingException me) { }
        }

        if (name == null)
            return null;

        // catch (illegal, but less common) character entities
        if (name.indexOf("&#") != -1 && name.indexOf(';') != -1)
            return expandNumericCharacterReferences(name);

        return name;
    }

    public static String expandNumericCharacterReferences(String raw) {
        if (raw == null)
            return null;

        int start = -1;
        boolean hex = false;
        int calc = 0;

        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = raw.length(); i < len; i++) {
            char c = raw.charAt(i);
            if (start != -1) {
                if (c >= '0' && c <= '9') {
                    calc = calc * (hex ? 16 : 10) + c - '0';
                } else if (hex && c >= 'a' && c <= 'f') {
                    calc = calc * 16 + 10 + c - 'a';
                } else if (hex && c >= 'A' && c <= 'F') {
                    calc = calc * 16 + 10 + c - 'A';
                } else if (c == ';' && i > start + (hex ? 4 : 3)) {
                    sb.append((char) calc);  start = -1;
                } else {
                    sb.append(raw.substring(start, i--));  start = -1;
                }
            } else if (c == '&' && i < len - 3 && raw.charAt(i + 1) == '#') {
                hex = raw.charAt(i + 2) == 'x' || raw.charAt(i + 2) == 'X';
                start = i;
                i += hex ? 2 : 1;
                calc = 0;
            } else {
                sb.append(c);
            }
        }
        if (start != -1)
            sb.append(raw.substring(start));

        return sb.toString();
    }


    public static MPartInfo getTextBody(List<MPartInfo> parts, boolean preferHtml) {
        for (MPartInfo mpi : getBody(parts, preferHtml)) {
            if (mpi.getContentType().startsWith(MimeConstants.CT_TEXT_PREFIX))
                return mpi;
        }
        return null;
    }

    public static Set<MPartInfo> getBody(List<MPartInfo> parts, boolean preferHtml) {
     	if (parts.isEmpty())
     		return Collections.emptySet();

        Set<MPartInfo> bodies = null;

     	// if top-level has no children, then it is the body
     	MPartInfo top = parts.get(0);
     	if (!top.getContentType().startsWith(MimeConstants.CT_MULTIPART_PREFIX)) {
            if (!top.getDisposition().equals(Part.ATTACHMENT))
                (bodies = new HashSet<MPartInfo>(1)).add(top);
        } else {
            bodies = getBodySubparts(top, preferHtml);
        }

        if (bodies == null)
            bodies = Collections.emptySet();
        return bodies;
    }

    /**
     * Returns the decoded and unfolded value for the given header name.  If
     * multiple headers with the same name exist, returns the first one.
     * If the header does not exist, returns <tt>null</tt>. 
     */
    public static String getHeader(MimeMessage msg, String headerName) {
        try {
            String value = msg.getHeader(headerName, null);
            if (value == null || value.length() == 0)
                return null;
            try {
                value = MimeUtility.decodeText(value);
            } catch (UnsupportedEncodingException e) { }

            value = MimeUtility.unfold(value);
            return value;
        } catch (MessagingException e) {
            sLog.debug("Unable to get header '%s'", headerName, e);
            return null;
        }
    }
    
    private static final String[] NO_HEADERS = new String[0];
    
    /**
     * Returns the decoded and unfolded values for the given header name,
     * or an empty array if no headers with the given name exist.
     */
    public static String[] getHeaders(MimeMessage msg, String headerName) {
        try {
            String[] values = msg.getHeader(headerName);
            if (values == null || values.length == 0)
                return NO_HEADERS;

            for (int i = 0; i < values.length; i++) {
                try {
                    values[i] = MimeUtility.decodeText(values[i]);
                } catch (UnsupportedEncodingException e) {
                    // values[i] would contain the undecoded value, fine
                }
                values[i] = MimeUtility.unfold(values[i]);
            }

            return values;
        } catch (MessagingException e) {
            sLog.debug("Unable to get headers named '%s'", headerName, e);
            return NO_HEADERS;
        }
    }

    /**
     * Returns the value of the <tt>Message-ID</tt> header, or <tt>null</tt>
     * if the header does not exist or has an empty value.
     */
    public static String getMessageID(MimeMessage mm) {
        try {
            String msgid = mm.getMessageID();
            return ("".equals(msgid) ? null : msgid);
        } catch (MessagingException e) {
            return null;
        }
    }

    /**
     * Returns the decoded value of the <tt>Subject</tt> header, or
     * <tt>null</tt> if the header does not exist.
     */
    public static String getSubject(MimeMessage mm) throws MessagingException {
        String subject = mm.getHeader("Subject", null);
        return subject == null ? null : MimeHeader.decode(subject);
    }

    /**
     * Returns the decoded value of the <tt>From</tt> header.  If not available,
     * returns the value of the <tt>Sender</tt> header.  Returns an empty
     * <tt>String</tt> if neither header is available.
     */
    public static String getSender(MimeMessage msg) {
        String sender = null;
        try {
            sender = msg.getHeader("From", null);
        } catch (MessagingException e) {}
        if (sender == null) {
            try {
                sender = msg.getHeader("Sender", null);
            } catch (MessagingException e) {}
        }
        if (sender == null)
            sender = "";
        String decoded;
        try {
            decoded = MimeUtility.decodeText(sender);
        } catch (UnsupportedEncodingException e) {
            return sender;
        }
        return decoded;
    }
    
    private static Set<String> TEXT_ALTERNATES = new HashSet<String>(Arrays.asList(MimeConstants.CT_TEXT_ENRICHED, MimeConstants.CT_TEXT_HTML));
    private static Set<String> HTML_ALTERNATES = new HashSet<String>(Arrays.asList(MimeConstants.CT_TEXT_ENRICHED, MimeConstants.CT_TEXT_PLAIN));

    private static Set<String> KNOWN_MULTIPART_TYPES = new HashSet<String>(Arrays.asList(
            MimeConstants.CT_MULTIPART_ALTERNATIVE, MimeConstants.CT_MULTIPART_DIGEST, MimeConstants.CT_MULTIPART_MIXED, MimeConstants.CT_MULTIPART_REPORT,
            MimeConstants.CT_MULTIPART_RELATED, MimeConstants.CT_MULTIPART_SIGNED, MimeConstants.CT_MULTIPART_ENCRYPTED
    ));

    private static Set<MPartInfo> getBodySubparts(MPartInfo base, boolean preferHtml) {
        // short-circuit malformed messages and message subparts
        String ctype = base.getContentType();
        if (!base.hasChildren() || ctype.equals(MimeConstants.CT_MESSAGE_RFC822))
            return null;

        List<MPartInfo> children;
        if (ctype.equals(MimeConstants.CT_MULTIPART_ALTERNATIVE))
            return getAlternativeBodySubpart(base.getChildren(), preferHtml);
        else if (ctype.equals(MimeConstants.CT_MULTIPART_RELATED))
            return getRelatedBodySubpart(base.getChildren(), preferHtml, base.getContentTypeParameter("start"));
        else if (ctype.equals(MimeConstants.CT_MULTIPART_MIXED) || !KNOWN_MULTIPART_TYPES.contains(ctype))
            children = base.getChildren();
        else
            children = Arrays.asList(base.getChildren().get(0));

        Set<MPartInfo> bodies = null;
        for (MPartInfo mpi : children) {
            String childType = mpi.getContentType();
            if (childType.startsWith(MimeConstants.CT_MULTIPART_PREFIX)) {
                Set<MPartInfo> found = getBodySubparts(mpi, preferHtml);
                if (found != null) {
                    if (bodies == null)
                        bodies = new LinkedHashSet<MPartInfo>(found.size());
                    bodies.addAll(found);
                }
            } else if (!mpi.getDisposition().equals(Part.ATTACHMENT) && !childType.equalsIgnoreCase(MimeConstants.CT_MESSAGE_RFC822)) {
                if (bodies == null)
                    bodies = new LinkedHashSet<MPartInfo>(1);
                bodies.add(mpi);
            }
        }

        return bodies;
    }

    private static <T> Set<T> setContaining(T mpi) {
        Set<T> body = new LinkedHashSet<T>(1);
        body.add(mpi);
        return body;
    }

    private static Set<MPartInfo> getAlternativeBodySubpart(List<MPartInfo> children, boolean preferHtml) {
        // go through top-level children, stopping at first text part we are interested in
        MPartInfo alternative = null;
        for (MPartInfo mpi : children) {
            boolean isAttachment = mpi.getDisposition().equals(Part.ATTACHMENT);
            // the Content-Type we want and the one we'd settle for...
            String wantType = preferHtml ? MimeConstants.CT_TEXT_HTML  : MimeConstants.CT_TEXT_PLAIN;
            Set<String> altTypes = preferHtml ? HTML_ALTERNATES : TEXT_ALTERNATES;

            String ctype = mpi.getContentType();
            if (!isAttachment && ctype.equals(wantType)) {
                return setContaining(mpi);
            } else if (!isAttachment && altTypes.contains(ctype)) {
                if (alternative == null || !alternative.getContentType().equalsIgnoreCase(ctype))
                    alternative = mpi;
            } else if (ctype.startsWith(MimeConstants.CT_MULTIPART_PREFIX)) {
                Set<MPartInfo> body;
                if ((body = getBodySubparts(mpi, preferHtml)) != null)
                    return body;
            }
        }

        if (alternative == null)
            return null;
        return setContaining(alternative);
    }

    private static Set<MPartInfo> getRelatedBodySubpart(List<MPartInfo> children, boolean preferHtml, String parentCID) {
        // if the multipart/related part had a "parent" param, that names the body subpart by Content-ID
        if (parentCID != null) {
            for (MPartInfo mpi : children) {
                if (!parentCID.equals(mpi.getContentID()))
                    continue;

                if (mpi.getContentType().startsWith(MimeConstants.CT_MULTIPART_PREFIX))
                    return getBodySubparts(mpi, preferHtml);
                else
                    return setContaining(mpi);
            }
        }

        // return the first text subpart, or, if none exists, the first subpart, period
        MPartInfo first = null;
        for (MPartInfo mpi : children) {
            String ctype = mpi.getContentType();
            if (ctype.startsWith(MimeConstants.CT_TEXT_PREFIX)) {
                return setContaining(mpi);
            } else if (ctype.startsWith(MimeConstants.CT_MULTIPART_PREFIX)) {
                return getBodySubparts(mpi, preferHtml);
            } else if (first == null) {
                first = mpi;
            }
        }

        // falling through to here means there was no "parent" CID match and no text part
        if (first == null)
            return null;
        return setContaining(first);
    }

    public static void main(String[] args) throws MessagingException, IOException {
        String s = URLDecoder.decode("Zimbra%20&#26085;&#26412;&#35486;&#21270;&#12398;&#32771;&#24942;&#28857;.txt", "utf-8");
        System.out.println(s);
        System.out.println(expandNumericCharacterReferences("Zimbra%20&#26085;&#26412;&#35486;&#21270;&#12398;&#32771;&#24942;&#28857;.txt&#x40;&;&#;&#x;&#&#3876;&#55"));

        MimeMessage mm = new FixedMimeMessage(JMSession.getSession(), new java.io.FileInputStream("C:\\Temp\\mail\\24245"));
        InputStream is = new RawContentMultipartDataSource(mm, new ContentType(mm.getContentType())).getInputStream();
        int num;  byte buf[] = new byte[1024];
        while ((num = is.read(buf)) != -1)
            System.out.write(buf, 0, num);
    }

    /**
     * Returns an <tt>InputStream</tt> to the content of a <tt>MimeMessage</tt>
     * by starting a thread that serves up its content to a <tt>PipedOutputStream</tt>.
     * This workaround is necessary because JavaMail does not provide <tt>InputStream</tt>
     * access to the content.
     */
    public static InputStream getInputStream(MimeMessage msg)
    throws IOException {
        // Nasty hack because JavaMail doesn't provide an InputStream accessor
        // to the entire RFC 822 content of a MimeMessage.  Start a thread that
        // serves up the content of the MimeMessage via PipedOutputStream.
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        Thread thread = new Thread(new MimeMessageOutputThread(msg, out));
        thread.setName("MimeMessageThread");
        thread.start();
        return in;
    }
    
    /**
     * Returns the size of this <tt>MimePart</tt>'s content.  If the content
     * is encoded, returns the size of the decoded content.
     */
    public static int getSize(MimePart part)
    throws MessagingException, IOException {
        int size = part.getSize();
        if (size > 0) {
            if ("base64".equalsIgnoreCase(part.getEncoding())) {
                // MimePart.getSize() returns the encoded size.
                size = (int) ((size * 0.75) - (size / 76));
            }
        } else {
            size = (int) ByteUtil.getDataLength(part.getInputStream());
        }
        return size;
    }
}