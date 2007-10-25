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
 * Created on Apr 17, 2004
 */
package com.zimbra.cs.mime;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.ParseException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.QCodec;

import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeCompoundHeader;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.cs.util.JMSession;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author schemers
 */
public class Mime {
    
    static Log mLog = LogFactory.getLog(Mime.class);

    // content types
    public static final String CT_TEXT_PLAIN = "text/plain";
    public static final String CT_TEXT_HTML = "text/html";
    public static final String CT_TEXT_ENRICHED = "text/enriched";
    public static final String CT_TEXT_CALENDAR = "text/calendar";
    public static final String CT_TEXT_VCARD = "text/x-vcard";
    public static final String CT_MESSAGE_RFC822 = "message/rfc822";
    public static final String CT_APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String CT_APPLICATION_BINARY = "application/binary";
    public static final String CT_APPLICATION_MSWORD = "application/msword";
    public static final String CT_APPLICATION_PDF = "application/pdf";
    public static final String CT_MULTIPART_ALTERNATIVE = "multipart/alternative";
    public static final String CT_MULTIPART_DIGEST = "multipart/digest";
    public static final String CT_MULTIPART_MIXED = "multipart/mixed";
    public static final String CT_MULTIPART_REPORT = "multipart/report";
    public static final String CT_XML_ZIMBRA_SHARE = "xml/x-zimbra-share";

    public static final String CT_MULTIPART_PREFIX = "multipart/";
    public static final String CT_TEXT_PREFIX = "text/";

    public static final String CT_APPLICATION_WILD = "application/.*";
    public static final String CT_IMAGE_WILD = "image/.*";
    public static final String CT_AUDIO_WILD = "audio/.*";
    public static final String CT_VIDEO_WILD = "video/.*";
    public static final String CT_MULTIPART_WILD = "multipart/.*";
    public static final String CT_TEXT_WILD = "text/.*";
    public static final String CT_XML_WILD = "xml/.*";
    
    public static final String CT_DEFAULT = CT_TEXT_PLAIN;
	
	// encodings
    public static final String ET_7BIT = "7bit";
    public static final String ET_8BIT = "8bit";
    public static final String ET_BINARY = "binary";
    public static final String ET_QUOTED_PRINTABLE = "quoted-printable";
    public static final String ET_BASE64 = "base64";
    public static final String ET_DEFAULT = ET_7BIT;

    // attachment metadata fields
    public static final String MF_TITLE = "title";
    public static final String MF_AUTHOR = "author";
    public static final String MF_KEYWORDS = "keywords";
    public static final String MF_COMPANY = "company";
    public static final String MF_METADATA = "metadata";

    // parameters
    public static final String P_CHARSET = "charset";
    // default value for charset
    public static final String P_CHARSET_ASCII = "us-ascii";
    public static final String P_CHARSET_UTF8 = "utf-8";
    public static final String P_CHARSET_DEFAULT = P_CHARSET_ASCII;

    private static final int MAX_DECODE_BUFFER = 2048;

    private static final Set<String> TRANSFER_ENCODINGS = new HashSet<String>(Arrays.asList(new String[] {
            ET_7BIT, ET_8BIT, ET_BINARY, ET_QUOTED_PRINTABLE, ET_BASE64
    }));

    public static class FixedMimeMessage extends MimeMessage {
        public FixedMimeMessage(Session s)  { super(s); }
        public FixedMimeMessage(Session s, InputStream is) throws MessagingException  { super(s, is); }
        public FixedMimeMessage(MimeMessage mm) throws MessagingException  { super(mm); }

        public void setSession(Session s)  { session = s; }

        @Override protected void updateHeaders() throws MessagingException {
            String msgid = getMessageID();
            super.updateHeaders();
            if (msgid != null)
                setHeader("Message-ID", msgid);
        }
    }

    /**
     * return complete List of MPartInfo objects. 
     * @param mm
     * @return
     * @throws IOException
     * @throws MessagingException
     */
    public static List<MPartInfo> getParts(MimeMessage mm) throws IOException, MessagingException {
        List<MPartInfo> parts = new ArrayList<MPartInfo>();
        if (mm != null)
            handlePart(mm, "", parts, null, 0);
        return parts;
    }
    
	// FIXME: this needs to be more robust and ignore exceptions on parts it can't handle
	// so we get as many as possible
	private static void handlePart(MimePart mp, String prefix, List<MPartInfo> partList, MPartInfo parent, int partNum)
    throws IOException, MessagingException {
		String cts = getContentType(mp);
        boolean isMultipart = cts.startsWith(CT_MULTIPART_PREFIX); 
        boolean isMessage = !isMultipart && cts.equals(CT_MESSAGE_RFC822);
        boolean digestParent = parent != null && parent.mContentType.equalsIgnoreCase(CT_MULTIPART_DIGEST);

        String disp = null, filename = null;
        try {
            disp = mp.getDisposition();
            filename = getFilename(mp);
        } catch (ParseException pe) { }
        int size = 0;
        try {
        	size = mp.getSize();
        } catch (MessagingException me) { }

        // the top-level part of a non-multipart message is numbered "1"
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
        mpart.mDisposition = (disp == null ? (digestParent ? Part.ATTACHMENT : "") : disp.toLowerCase());
        mpart.mFilename = (filename == null ? "" : filename.toLowerCase());
		partList.add(mpart);
		if (parent != null) {
			if (parent.mChildren == null)
				parent.mChildren = new ArrayList<MPartInfo>();
			parent.mChildren.add(mpart);
		}

		// System.out.println("    " + mpart.info);

		if (isMultipart) {
            // IMAP part numbering is screwy: top-level multipart doesn't get a number
			String newPrefix = prefix.length() > 0 ? (prefix + '.') : "";
            if (mp instanceof MimeMessage)
                mpart.mPartName = newPrefix + "TEXT";
			mpart.mContent = getMultipartContent(mp, cts);
			if (mpart.mContent instanceof MimeMultipart)
				handleMultipart((MimeMultipart) mpart.mContent, newPrefix, partList, mpart);
		} else if (isMessage) {
			mpart.mContent = getMessageContent(mp);
			if (mpart.mContent instanceof MimeMessage)
				handlePart((MimeMessage) mpart.mContent, prefix, partList, mpart, 0);
		} else {
			// nothing to do at this stage
		}
	}

    private static void handleMultipart(MimeMultipart multi, String prefix, List<MPartInfo> partList, MPartInfo parent)
    throws IOException, MessagingException {
        for (int i = 0; i < multi.getCount(); i++) {
            BodyPart bp = multi.getBodyPart(i);
            if (!(bp instanceof MimePart))
                continue;
            handlePart((MimePart) bp, prefix + (i + 1), partList, parent, i+1);
        }
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
    public static Object getMessageContent(MimePart message822Part) throws IOException, MessagingException {
        Object content = message822Part.getContent();
        if (content instanceof InputStream) {
            try {
                // handle unparsed content due to miscapitalization of content-type value
                content = new FixedMimeMessage(JMSession.getSession(), (InputStream) content);
            } catch (Exception e) {
            } finally {
                ByteUtil.closeStream((InputStream) content);
            }
        }
        return content;
    }

    /** Returns the MimeMultipart object encapsulating the body of a MIME
     *  part with content-type "multipart/*".  Use this method instead of
     *  {@link Part#getContent()} to work around JavaMail's fascism about
     *  proper MIME format and failure to support RFC 2184. */
    public static Object getMultipartContent(MimePart multipartPart, String contentType) throws IOException, MessagingException {
        Object content = multipartPart.getContent();
        if (content instanceof InputStream) {
        	MimeMultipart mmp = null;
            try {
                // handle unparsed content due to miscapitalization of content-type value
                mmp = new MimeMultipart(new InputStreamDataSource((InputStream) content, contentType));
            } catch (Exception e) {
            } finally {
                ByteUtil.closeStream((InputStream) content);
            }
            if (mmp != null)
            	content = mmp;
        }
        if (content instanceof MimeMultipart)
            content = validateMultipart((MimeMultipart) content, multipartPart);
        return content;
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

        String[] subpart = part.split("\\.");
        for (int i = 0; i < subpart.length; i++) {
            int index = Integer.parseInt(subpart[i]);
            if (index <= 0)
                return null;
            // the content-type determines the expected substructure
            String ct = mp.getContentType().toLowerCase();
            if (ct == null)
                return null;
            if (ct.startsWith(CT_MULTIPART_PREFIX)) {
                Object content = getMultipartContent(mp, ct);
                if (content instanceof MimeMultipart && ((MimeMultipart) content).getCount() >= index) {
                    BodyPart bp = ((MimeMultipart) content).getBodyPart(index - 1);
                    if (bp instanceof MimePart) {
                        mp = (MimePart) bp;
                        continue;
                    }
                }
            } else if (mp instanceof MimeMessage && index == 1 && i == subpart.length - 1) {
                // the top-level part of a non-multipart message is numbered "1"
                break;
            } else if (ct.startsWith(CT_MESSAGE_RFC822)) {
                Object content = getMessageContent(mp);
                if (content instanceof MimeMessage) {
                    if (mp instanceof MimeMessage) {
                        // the top-level part of a non-multipart message is numbered "1"
                        if (index != 1)
                            return null;
                    } else
                    	i--;
                    mp = (MimePart) content;
                    continue;
                }
            }
            return null;
        }
        return mp;
    }

	/**
	 * Returns true if we consider this to be an attachment for the sake of "filtering" by attachments.
	 * i.e., if someone searchs for messages with attachment types of "text/plain", we probably wouldn't want
	 * every multipart/mixed message showing up, since 99% of them will have a first body part of text/plain.
	 * 
	 * @param part
	 * @return
	 */
	 static boolean isFilterableAttachment(MPartInfo part) {
	    MPartInfo parent = part.getParent();
	    
	    if (part.getContentType().startsWith(CT_MULTIPART_PREFIX))
	        return false;
	    
	    if (part.getContentType().startsWith(CT_TEXT_PREFIX)) {
	        if (parent == null || (part.getPartNum() == 1 && parent.getContentType().equals(CT_MESSAGE_RFC822))) {
	            // ignore top-level text/* types
	            return false;
	        }
	        
	        if (parent != null && parent.getContentType().equals(CT_MULTIPART_ALTERNATIVE)) {
	            // ignore body parts with a parent of multipart/alternative
	            return false; 
	        }
	        
	        // ignore if: it is the first body part, and has a multipart/*
	        // parent, and that
	        // multipart's parent is null or message/rfc822
	        if (part.getPartNum() == 1) {
	            if (parent != null && parent.getContentType().startsWith(CT_MULTIPART_PREFIX)) {
	                MPartInfo pp = parent.getParent();
	                if (pp == null || pp.getContentType().equals(CT_MESSAGE_RFC822)) { 
	                    return false; 
	                }
	            }
	        }
	    }
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
         for (MPartInfo mpi : parts)
	         if (mpi.isFilterableAttachment())
	             set.add(mpi.getContentType());
	     return set;
	 }
	 
	/** Returns true if any of the given message parts qualify as "attachments"
     *  for the purpose of displaying the little paperclip icon in the web UI.
	 *  Note that Zimbra folder sharing notifications are expressly *not*
     *  considered attachments for this purpose. */
	public static boolean hasAttachment(List<MPartInfo> parts) {
        for (MPartInfo mpi : parts)
	        if (mpi.isFilterableAttachment() && !mpi.getContentType().equalsIgnoreCase(CT_XML_ZIMBRA_SHARE))
	            return true;
	    return false;
	}

    private static final InternetAddress[] NO_ADDRESSES = new InternetAddress[0];

    public static InternetAddress[] parseAddressHeader(MimeMessage mm, String headerName) {
        String header = null;
        try {
            header = mm.getHeader(headerName, ",");
            if (header == null || header.trim().equals(""))
                return NO_ADDRESSES;
            header = header.trim();
            return InternetAddress.parseHeader(header, false);
        } catch (AddressException e) {
            if (header == null)
                return NO_ADDRESSES;
            try {
                return new InternetAddress[] { new InternetAddress(null, header, P_CHARSET_UTF8) };
            } catch (UnsupportedEncodingException e1) {
                return NO_ADDRESSES;
            }
        } catch (MessagingException e) {
            return NO_ADDRESSES;
        }
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
    public static final String getContentType(Part part) {
        try {
            return getContentType(part.getContentType());
        } catch (MessagingException e) {
            ZimbraLog.extensions.warn("could not fetch part's content-type; defaulting to " + CT_DEFAULT, e);
            return CT_DEFAULT;
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
	 
    /** Returns a reader that decodes the specified <code>InputStream</code>.
     *  <code>contentType</code> must of type "text/*".  If a valid charset
     *  parameter is present in the Content-Type string, it is used as the
     *  charset for decoding the text.  If not, we fall back to the user's
     *  default charset preference.  If both of those options fail, the
     *  platform default is used.
     * 
     * @param input  The InputStream to decode.
     * @param contentType  The Content-Type of the stream, which must be "text/*".
     * @parame defaultCharset  The user's default charset preference */
    public static Reader getTextReader(InputStream input, String contentType, String defaultCharset) {
    	String charset = getCharset(contentType);
        if (charset != null) {
            try {
                return new InputStreamReader(input, charset);
            } catch (UnsupportedEncodingException e) { }
        }

        // if we're here, either there was no explicit charset on the part or it was invalid, so try the user's personal default charset
        if (defaultCharset != null && !defaultCharset.trim().equals("")) {
            try {
                return new InputStreamReader(input, defaultCharset);
            } catch (UnsupportedEncodingException e) { }
        }

        // if we're here, the user's default charset was also either unspecified or unavailable, so go with the JVM's default charset
        return new InputStreamReader(input);
    }

    public static String getCharset(String contentType) {
        String charset = new ContentType(contentType).getParameter(P_CHARSET);
        if (charset == null || charset.trim().equals(""))
            charset = null;
        return charset;
    }

    public static String encodeFilename(String filename) {
        try {
            // JavaMail doesn't use RFC 2231 encoding, and we're not going to, either...
            if (!StringUtil.isAsciiString(filename))
                return new QCodec().encode(filename, P_CHARSET_UTF8);
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
        if (name == null)
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
            if (mpi.getContentType().startsWith(CT_TEXT_PREFIX))
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
     	if (!top.getContentType().startsWith(CT_MULTIPART_PREFIX)) {
            if (!top.getDisposition().equals(Part.ATTACHMENT))
                (bodies = new HashSet<MPartInfo>(1)).add(top);
        } else {
            bodies = getBodySubparts(top, preferHtml);
        }

        if (bodies == null)
            bodies = Collections.emptySet();
        return bodies;
    }

    private static Set<String> TEXT_ALTERNATES = new HashSet<String>(Arrays.asList(new String[] { CT_TEXT_ENRICHED, CT_TEXT_HTML } ));
    private static Set<String> HTML_ALTERNATES = new HashSet<String>(Arrays.asList(new String[] { CT_TEXT_ENRICHED, CT_TEXT_PLAIN } ));

    private static Set<MPartInfo> getBodySubparts(MPartInfo base, boolean preferHtml) {
        // short-circuit malformed messages and message subparts
        String ctype = base.getContentType();
        if (!base.hasChildren() || ctype.equals(CT_MESSAGE_RFC822))
            return null;

        List<MPartInfo> children;
        if (ctype.equals(CT_MULTIPART_ALTERNATIVE))
            return getAlternativeBodySubpart(base.getChildren(), preferHtml);
        else if (ctype.equals(CT_MULTIPART_MIXED))
            children = base.getChildren();
        else
            (children = new ArrayList<MPartInfo>(1)).add(base.getChildren().get(0));

        Set<MPartInfo> bodies = null;
        for (MPartInfo mpi : children) {
            String childType = mpi.getContentType();
            if (childType.startsWith(CT_MULTIPART_PREFIX)) {
                Set<MPartInfo> found = getBodySubparts(mpi, preferHtml);
                if (found != null) {
                    if (bodies == null)  bodies = new LinkedHashSet<MPartInfo>(found.size());
                    bodies.addAll(found);
                }
            } else if (!mpi.getDisposition().equals(Part.ATTACHMENT) && !childType.equalsIgnoreCase(CT_MESSAGE_RFC822)) {
                if (bodies == null)  bodies = new LinkedHashSet<MPartInfo>(1);
                bodies.add(mpi);
            }
        }

        return bodies;
    }

    private static Set<MPartInfo> getAlternativeBodySubpart(List<MPartInfo> children, boolean preferHtml) {
        // go through top-level children, stopping at first text part we are interested in
        Set<MPartInfo> body;
        MPartInfo alternative = null;
        for (MPartInfo mpi : children) {
            boolean isAttachment = mpi.getDisposition().equals(Part.ATTACHMENT);
            // the Content-Type we want and the one we'd settle for...
            String wantType = preferHtml ? CT_TEXT_HTML  : CT_TEXT_PLAIN;
            Set<String> altTypes = preferHtml ? HTML_ALTERNATES : TEXT_ALTERNATES;

            String ctype = mpi.getContentType();
            if (!isAttachment && ctype.equals(wantType)) {
                (body = new LinkedHashSet<MPartInfo>(1)).add(mpi);
                return body;
            } else if (!isAttachment && altTypes.contains(ctype)) {
                alternative = mpi;
            } else if (ctype.startsWith(CT_MULTIPART_PREFIX)) {
                if ((body = getBodySubparts(mpi, preferHtml)) != null)
                    return body;
            }
        }

        if (alternative == null)
            return null;
        (body = new LinkedHashSet<MPartInfo>(1)).add(alternative);
        return body;
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
}