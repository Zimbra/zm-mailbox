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
 * Created on Apr 17, 2004
 */
package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.ParseException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.QCodec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	public static final String CT_APPLICATION_MSWORD = "application/msword";
	public static final String CT_APPLICATION_PDF = "application/pdf";
    public static final String CT_MULTIPART_ALTERNATIVE = "multipart/alternative";
	public static final String CT_MULTIPART_MIXED = "multipart/mixed";
	public static final String CT_XML_ZIMBRA_SHARE = "xml/x-zimbra-share";

    public static final String CT_MULTIPART_PREFIX = "multipart/";

	public static final String CT_APPPLICATION_WILD = "application/.*";
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

        protected void updateHeaders() throws MessagingException {
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
        mpart.mDisposition = (disp == null ? "" : disp.toLowerCase());
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

    private static MimeMultipart validateMultipart(MimeMultipart multi, MimePart mp) throws MessagingException {
        try {
             multi.getCount();
        } catch (ParseException pe) {
            FixedMultipartDataSource ds = new FixedMultipartDataSource(mp.getDataHandler().getDataSource());
            multi = new MimeMultipart(ds);
        }
        return multi;
    }

    private static class FixedMultipartDataSource implements DataSource {
        private DataSource mDataSource;
        FixedMultipartDataSource(DataSource ds) { mDataSource = ds; }

        public String getContentType()                          { return new MimeCompoundHeader.ContentType(mDataSource.getContentType()).toString(); }
        public String getName()                                 { return mDataSource.getName(); }
        public InputStream getInputStream() throws IOException  { return mDataSource.getInputStream(); }
        public OutputStream getOutputStream()                   { throw new UnsupportedOperationException(); }
    }

    /** Returns the MimeMessage object encapsulating a MIME part with
     *  content-type "message/rfc822".  Use this method instead of
     *  {@link Part#getContent()} to work around JavaMail's fascism about
     *  proper MIME format and failure to support RFC 2184. */
    public static Object getMessageContent(MimePart message822Part) throws IOException, MessagingException {
        Object content = message822Part.getContent();
        if (content instanceof InputStream)
            try {
                // handle unparsed content due to miscapitalization of content-type value
                content = new FixedMimeMessage(JMSession.getSession(), (InputStream) content);
            } catch (Exception e) {}
        return content;
    }

    /** Returns the MimeMultipart object encapsulating the body of a MIME
     *  part with content-type "multipart/*".  Use this method instead of
     *  {@link Part#getContent()} to work around JavaMail's fascism about
     *  proper MIME format and failure to support RFC 2184. */
    public static Object getMultipartContent(MimePart multipartPart, String contentType) throws IOException, MessagingException {
        Object content = multipartPart.getContent();
        if (content instanceof InputStream) {
            try {
                // handle unparsed content due to miscapitalization of content-type value
                content = new MimeMultipart(new InputStreamDataSource((InputStream) content, contentType));
            } catch (Exception e) {}
        }
        if (content instanceof MimeMultipart)
            content = validateMultipart((MimeMultipart) content, multipartPart);
        return content;
    }

    /** Returns a String containing the text content of the MimePart.  If the
     *  part's specified charset is unknown, defaults to the system's default
     *  charset.  Use this method instead of {@link Part#getContent()} to work
     *  around JavaMail's fascism about proper MIME format and failure to
     *  support RFC 2184. 
     * @throws MessagingException 
     * @throws IOException */
    public static String getStringContent(MimePart textPart) throws IOException, MessagingException {
        repairTransferEncoding(textPart);
        return decodeText(textPart.getInputStream(), textPart.getContentType());
    }

    public static void repairTransferEncoding(MimePart mp) throws MessagingException {
        String cte = mp.getHeader("Content-Transfer-Encoding", null);
        if (cte != null && !TRANSFER_ENCODINGS.contains(cte.toLowerCase()))
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
	    
	    if (part.getContentType().matches(CT_TEXT_WILD)) {
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
	 
	/**
	 * returns true if there are any attachments in the list of parts
	 * 
	 * @param parts
	 * @return
	 */
	public static boolean hasAttachment(List<MPartInfo> parts) {
        for (MPartInfo mpi : parts)
	        if (mpi.isFilterableAttachment())
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
        return new MimeCompoundHeader.ContentType(cthdr).getValue().trim();
    }

	/**
	 * decode the specified InputStream into the supplied StringBuffer.
	 * contentType must of type "text/*". If a charset parameter is present,
	 * it is used as the charset for decoding the text, otherwise us-ascii is.
	 * 
	 * @param input InputStream to decode
	 * @param contentType The Content-Type of the stream, which must be "text/*"
	 * @param buffer
	 * @throws IOException
	 */
	public static String decodeText(InputStream input, String contentType) throws IOException {
        StringBuilder buffer = new StringBuilder();
    	Reader reader = getTextReader(input, contentType);
        char [] cbuff = new char[MAX_DECODE_BUFFER];
        int num;
        while ( (num = reader.read(cbuff, 0, cbuff.length)) != -1) {
            buffer.append(cbuff, 0, num);
        }
        return buffer.toString();
	}
	 
    /**
      * Returns a reader that will decode the specified InputStream.
      * contentType must of type "text/*". If a charset parameter is present,
      * it is used as the charset for decoding the text, otherwise us-ascii is.
      * 
      * @param input InputStream to decode
      * @param contentType The Content-Type of the stream, which must be "text/*"
      * @throws IOException
      */
    public static Reader getTextReader(InputStream input, String contentType) {
    	String charset = getCharset(contentType);
        Reader reader = null;
        if (charset != null) { 
            try {
                reader = new InputStreamReader(input, charset);
            } catch (UnsupportedEncodingException e){
                // use default encoding?
            }
        }

        if (reader == null)
            reader = new InputStreamReader(input);
        return reader;
    }

    public static String getCharset(String contentType) {
        String charset = new MimeCompoundHeader.ContentType(contentType).getParameter(P_CHARSET);
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

    public static MPartInfo getBody(List parts, boolean preferHtml) {
     	if (parts.isEmpty())
     		return null;
     	
     	// if top-level has no children, then it is the body
     	MPartInfo top = (MPartInfo) parts.get(0);
     	if (!top.getContentType().startsWith(CT_MULTIPART_PREFIX))
     		return top.getDisposition().equals(Part.ATTACHMENT) ? null : top;

        return getBodySubpart(top, preferHtml);
    }

    private static Set<String> TEXT_ALTERNATES = new HashSet<String>(Arrays.asList(new String[] { CT_TEXT_ENRICHED, CT_TEXT_HTML } ));
    private static Set<String> HTML_ALTERNATES = new HashSet<String>(Arrays.asList(new String[] { CT_TEXT_ENRICHED, CT_TEXT_PLAIN } ));

    private static MPartInfo getBodySubpart(MPartInfo base, boolean preferHtml) {
        // short-circuit malformed messages
        if (!base.hasChildren())
            return null;

        List<MPartInfo> children;
        if (base.getContentType().equals(CT_MULTIPART_ALTERNATIVE)) {
            children = base.getChildren();
        } else {
            // for multipart/mixed (etc.), only the first part is really "body"
            children = new ArrayList<MPartInfo>();
            children.add(base.getChildren().get(0));
        }

        // go through top-level children, stopping at first text part we are interested in
        MPartInfo alternative = null;
        for (MPartInfo mpi : children) {
            boolean isAttachment = mpi.getDisposition().equals(Part.ATTACHMENT);
            // the Content-Type we want and the one we'd settle for...
            String wantType = preferHtml ? CT_TEXT_HTML  : CT_TEXT_PLAIN;
            Set<String> altTypes = preferHtml ? HTML_ALTERNATES : TEXT_ALTERNATES;

            String ctype = mpi.getContentType();
            if (!isAttachment && ctype.equals(wantType)) {
                return mpi;
            } else if (!isAttachment && altTypes.contains(ctype)) {
                alternative = mpi;
            } else if (ctype.startsWith(CT_MULTIPART_PREFIX)) {
                MPartInfo subpart = getBodySubpart(mpi, preferHtml);
                if (subpart == null)
                	continue;
                if (subpart.getContentType().equals(wantType))
                    return subpart;
                if (alternative == null)
                    alternative = subpart;
            }
        }
        return alternative;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        String s = URLDecoder.decode("Zimbra%20&#26085;&#26412;&#35486;&#21270;&#12398;&#32771;&#24942;&#28857;.txt", "utf-8");
        System.out.println(s);
        System.out.println(expandNumericCharacterReferences("Zimbra%20&#26085;&#26412;&#35486;&#21270;&#12398;&#32771;&#24942;&#28857;.txt&#x40;&;&#;&#x;&#&#3876;&#55"));
    }
}