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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author schemers
 */
public class Mime {
    
    private static Log mLog = LogFactory.getLog(Mime.class);

    // content types
    public static final String CT_TEXT_PLAIN = "text/plain";
    public static final String CT_TEXT_HTML = "text/html";
    public static final String CT_TEXT_CALENDAR = "text/calendar";
    public static final String CT_TEXT_VCARD = "text/x-vcard";
	public static final String CT_MESSAGE_RFC822 = "message/rfc822";
	public static final String CT_APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String CT_MULTIPART = "multipart";
    public static final String CT_MULTIPART_ALTERNATIVE = "multipart/alternative";
	public static final String CT_MULTIPART_MIXED = "multipart/mixed";

	public static final String CT_APPLICATION_MSWORD = "application/msword";
	public static final String CT_APPLICATION_PDF = "application/pdf";
	
	public static final String CT_XML_ZIMBRA_SHARE = "xml/x-zimbra-share";
	
	public static final String CT_APPPLICATION_WILD = "application/*";
   	public static final String CT_IMAGE_WILD = "image/*";
   	public static final String CT_AUDIO_WILD = "audio/*";
   	public static final String CT_VIDEO_WILD = "video/*";
   	public static final String CT_MULTIPART_WILD = "multipart/*";
   	public static final String CT_TEXT_WILD = "text/*";
   	public static final String CT_XML_WILD = "xml/*";
	
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
    public static final String P_CHARSET_UTF8 = "utf-8";
    public static final String P_CHARSET_DEFAULT = "us-ascii";
    
    private static final int MAX_DECODE_BUFFER = 2048;
    
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
		String cts = mp.getContentType();
		if (cts == null)
			cts = CT_DEFAULT;
		else {
			// only use "type/subtype"
			// This is a workaround for messages sent by some broken mailers
			// that generate an invalid content type string which causes
			// JavaMail ParseException.  The broken mailer that necessitated
			// this hack is "X-Mailer: Balsa 2.0.17", which generated
			// Content-Type of "Content-Type:   text/plain; charset=US-ASCII;\r\n"
			// "\tFormat=Flowed   DelSp=Yes\r\n".  Notice it is missing ';' after
			// "Flowed".
			int semicolon = cts.indexOf(';');
			if (semicolon != -1)
				cts = cts.substring(0, semicolon);

			// Some mailers don't specify subtype at all, e.g. "Content-Type: text"
			// Special case "text" to "text/plain".
			if (cts.equals("text"))
				cts = CT_TEXT_PLAIN;
		}
		ContentType ct = null;
		try {
			ct = new ContentType(cts.toLowerCase());
			cts = ct.getPrimaryType() + "/" + ct.getSubType();
		} catch (ParseException e) {
			if (mLog.isInfoEnabled())
				mLog.info("Unrecognized Content-Type " + cts + "; assuming " + CT_DEFAULT);
			ct = new ContentType(CT_DEFAULT);
		}
        boolean isMultipart = ct.match(CT_MULTIPART_WILD); 
        boolean isMessage = !isMultipart && ct.match(CT_MESSAGE_RFC822);

        String disp = null, filename = null;
        try {
            disp = mp.getDisposition();
            filename = Mime.getFilename(mp);
        } catch (ParseException pe) { }

        // the top-level part of a non-multipart message is numbered "1"
        if (!isMultipart && mp instanceof MimeMessage)
            prefix = (prefix.length() > 0 ? (prefix + ".") : "") + '1';

        MPartInfo mpart = new MPartInfo();
		mpart.mPart = mp;
		mpart.mParent = parent;
		mpart.mContentType = ct;
		mpart.mContentTypeString = cts;
		mpart.mPartName = prefix;
		mpart.mPartNum = partNum;
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
				handleMultiPart((MimeMultipart) mpart.mContent, newPrefix, partList, mpart);
		} else if (isMessage) {
			mpart.mContent = getMessageContent(mp);
			if (mpart.mContent instanceof MimeMessage)
				handlePart((MimeMessage) mpart.mContent, prefix, partList, mpart, 0);
		} else {
			// nothing to do at this stage
		}
	}

    private static void handleMultiPart(MimeMultipart mmp, String prefix, List<MPartInfo> partList, MPartInfo parent)
    throws IOException, MessagingException {
        for (int i = 0; i < mmp.getCount(); i++) {
            BodyPart bp = mmp.getBodyPart(i);
            if (!(bp instanceof MimePart))
                continue;
            handlePart((MimePart) bp, prefix + (i + 1), partList, parent, i+1);
        }
    }
    
    /** Returns the MimeMultipart object encapsulating the body of a MIME
     *  part with content-type "message/rfc822".  Use this method instead of
     *  {@link Part#getContent()} to work around JavaMail's fascism about
     *  proper MIME format and failure to support RFC 2184. */
    public static Object getMessageContent(MimePart message822Part) throws IOException, MessagingException {
        Object content = message822Part.getContent();
        if (content instanceof InputStream)
            try {
                // handle unparsed content due to miscapitalization of content-type value
                content = new MimeMessage(null, (InputStream) content);
            } catch (Exception e) {}
            return content;
    }
    
    /** Returns the MimeMultipart object encapsulating the body of a MIME
     *  part with content-type "multipart/*".  Use this method instead of
     *  {@link Part#getContent()} to work around JavaMail's fascism about
     *  proper MIME format and failure to support RFC 2184. */
    public static Object getMultipartContent(MimePart multipartPart, String contentType) throws IOException, MessagingException {
        Object content = multipartPart.getContent();
        if (content instanceof InputStream)
            try {
                // handle unparsed content due to miscapitalization of content-type value
                content = new MimeMultipart(new InputStreamDataSource((InputStream) content, contentType));
            } catch (Exception e) {}
            return content;
    }

    /**
     * Walks the mail object tree depth-first, starting at the specified <code>MimePart</code>.
     * Invokes the various methods in <code>MimeVisitor</code> for each visited node.
     * 
     * @param visitor the instance of <code>MimeVisitor</code> that will traverse
     * the tree
     * @param mp the root node at which to start the traversal
     */
    public static void accept(MimeVisitor visitor, MimePart mp)
    throws IOException, MessagingException {
        if (mp instanceof MimeMessage) {
            visitor.visitMessage((MimeMessage) mp, MimeVisitor.VISIT_BEGIN);
        }
        String cts = mp.getContentType();
        if (cts == null)
            cts = CT_DEFAULT;
        else {
            // only use "type/subtype"
            // This is a workaround for messages sent by some broken mailers
            // that generate an invalid content type string which causes
            // JavaMail ParseException.  The broken mailer that necessitated
            // this hack is "X-Mailer: Balsa 2.0.17", which generated
            // Content-Type of "Content-Type:   text/plain; charset=US-ASCII;\r\n"
            // "\tFormat=Flowed   DelSp=Yes\r\n".  Notice it is missing ';' after
            // "Flowed".
            int semicolon = cts.indexOf(';');
            if (semicolon != -1)
                cts = cts.substring(0, semicolon);

            // Some mailers don't specify subtype at all, e.g. "Content-Type: text"
            // Special case "text" to "text/plain".
            if (cts.equals("text"))
                cts = CT_TEXT_PLAIN;
        }
        ContentType ct = null;
        try {
            ct = new ContentType(cts.toLowerCase());
            cts = ct.getPrimaryType() + "/" + ct.getSubType();
        } catch (ParseException e) {
            if (mLog.isInfoEnabled())
                mLog.info("Unrecognized Content-Type " + cts + "; assuming " + CT_DEFAULT);
            ct = new ContentType(CT_DEFAULT);
        }
        
        boolean isMultipart = ct.match(CT_MULTIPART_WILD);
        boolean isMessage = !isMultipart && ct.match(CT_MESSAGE_RFC822);
        
        if (isMultipart) {
            Object content = getMultipartContent(mp, cts);
            if (content instanceof MimeMultipart) {
                MimeMultipart multi = (MimeMultipart) content;
                visitor.visitMultipart(multi, MimeVisitor.VISIT_BEGIN);
                
                // Make a copy of the parts array and iterate the copy,
                // in case the visitor is adding or removing parts.
                List<BodyPart> parts = new ArrayList<BodyPart>();
                for (int i = 0; i < multi.getCount(); i++)
                    parts.add(multi.getBodyPart(i));
                for (BodyPart bodyPart : parts) {
                    if (bodyPart instanceof MimeBodyPart)
                        accept(visitor, (MimeBodyPart) bodyPart);
                    else
                        mLog.info("Mime.accept(): Unexpected BodyPart subclass: " + bodyPart.getClass().getName());
                }
                visitor.visitMultipart(multi, MimeVisitor.VISIT_END);
            }
        } else if (isMessage) {
            Object content = getMessageContent(mp);
            if (content instanceof MimeMessage) {
                accept(visitor, (MimeMessage) content);
            }
        } else if (mp instanceof MimeBodyPart) {
            visitor.visitBodyPart((MimeBodyPart) mp);
        } else if (!(mp instanceof MimeMessage)) {
            mLog.info("Mime.accept(): Unexpected type: " + mp.getClass().getName() +
                ".  Content-Type='" + cts + "'");
        }

        if (mp instanceof MimeMessage) {
            visitor.visitMessage((MimeMessage) mp, MimeVisitor.VISIT_END);
        }
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
            if (ct.startsWith(CT_MULTIPART + '/')) {
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
	    
	    if (part.getContentType().match(Mime.CT_MULTIPART_WILD))
	        return false;
	    
	    if (part.getContentType().match(Mime.CT_TEXT_WILD)) {
	        if (parent == null || (part.getPartNum() == 1 && parent.getContentType().match(Mime.CT_MESSAGE_RFC822))) {
	            // ignore top-level text/* types
	            return false;
	        }
	        
	        if (parent != null && parent.getContentType().match(Mime.CT_MULTIPART_ALTERNATIVE)) {
	            // ignore body parts with a parent of multipart/alternative
	            return false; 
	        }
	        
	        // ignore if: it is the first body part, and has a multipart/*
	        // parent, and that
	        // multipart's parent is null or message/rfc822
	        if (part.getPartNum() == 1) {
	            if (parent != null && parent.getContentType().match(Mime.CT_MULTIPART_WILD)) {
	                MPartInfo pp = parent.getParent();
	                if (pp == null || pp.getContentType().match(Mime.CT_MESSAGE_RFC822)) { 
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
	             set.add(mpi.getContentTypeString());
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
                return new InternetAddress[] { new InternetAddress(null, header) };
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

    /**
	 * Given a content type (potentially with parameters), canonicalize
	 * stripping off all parameters except charset for text/* parts.
	 * 
	 * TODO: is this really what we want? This function is used by the
	 * BucketBlobStore to determine what value to stick in the db with a blob.
	 * 
	 * @param contentType
	 * @return canonicalized content type
	 */
	public static String normalizeContentType(String contentType) {
	    int index = contentType.indexOf(';');
	    if (index != -1) {
	        String newContentType = null;
	        try {
	            ContentType ct = new ContentType(contentType);
	            if (ct.match(CT_TEXT_WILD)) {
	                String charset = ct.getParameter(P_CHARSET);
	                // if charset is specified, is not the default, and
	                // there is more then one parameter (like format=flowed),
	                // then strip out everything but the charset.
	                if ((charset != null) && 
	                        !charset.equalsIgnoreCase(P_CHARSET_DEFAULT) &&
	                        (ct.getParameterList().size() > 1)) {
	                    ContentType nct = new ContentType();
	                    nct.setPrimaryType(ct.getPrimaryType());
	                    nct.setSubType(ct.getSubType());
	                    nct.setParameter(P_CHARSET, charset);
	                    newContentType = nct.toString();
	                } else {
	                    // it is the default or not present...
	                }
	            }
	        } catch (ParseException e) {
	            // take everything before the ;
	        }
	        if (newContentType == null)
	            newContentType = contentType.substring(0, index);
	        return newContentType;
	    } else {
	        return contentType;
	    }
	}
	 
	 /**
	  * Given a content type (potentially with parameters), return only
	  * the lowercased "type/subtype" part.
	  * @param contentType
	  * @return
	  */
	public static String contentTypeOnly(String contentType) {
	    int index = contentType.indexOf(';');
	    if (index != -1)
	        contentType = contentType.substring(0, index);
	    return contentType.toLowerCase();
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
	public static void decodeText(InputStream input, String contentType, StringBuffer buffer)
		throws IOException 
	{
    	Reader reader = decodeText(input, contentType);
        char [] cbuff = new char[MAX_DECODE_BUFFER];
        int num;
        while ( (num = reader.read(cbuff, 0, cbuff.length)) != -1) {
            buffer.append(cbuff, 0, num);
        }
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
    public static Reader decodeText(InputStream input, String contentType) {
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
        ContentType ct;
        try {
            ct = new ContentType(contentType);
            // we shouldn't get called with anything other then text/*, but if we do...
            if (!ct.match(CT_TEXT_WILD))
                throw new IllegalArgumentException("unsupported content type: " + contentType);            
        } catch (ParseException e1) {
            // TODO: treat as text/plain? run some sanity checks on it?
            ct = new ContentType("text", "plain", null);
        }
   
        String charset = ct.getParameter(Mime.P_CHARSET);
        if (charset == null)
            charset = Mime.P_CHARSET_DEFAULT;
        return charset;
    }

    public static String getFilename(MimePart mp) throws MessagingException {
        String name = mp.getFileName();

        // catch (legal, but uncommon) RFC 2231 encoded filenames
        try {
            String cd = mp.getHeader("Content-Disposition", null);
            if (cd != null && cd.indexOf('*') != -1) {
                // catch things like filename*=UTF-8''%E3%82%BD%E3%83%AB%E3%83%86%E3%82%A3.rtf
                Map<String, String> cdattrs = decodeRFC2231(cd);
                if (cdattrs != null && cdattrs.containsKey("filename"))
                    name = cdattrs.get("filename");
            }
        } catch (MessagingException me) { }

        if (name == null)
            return null;

        // catch (illegal, but common) RFC 2047 encoded-words
        if (name.indexOf("=?") != -1 && name.indexOf("?=") != -1)
            try {
                return MimeUtility.decodeText(name);
            } catch (UnsupportedEncodingException uee) { }

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

    private enum RFC2231State { PARAM, CONTINUED, EXTENDED, EQUALS, CHARSET, LANG, VALUE, QVALUE, SLOP };

    private static class RFC2231Data {
        RFC2231State state = RFC2231State.EQUALS;
        StringBuilder key = null;
        StringBuilder value = new StringBuilder();
        boolean continued = false;
        boolean encoded = false;
        StringBuilder charset = null;

        void setState(RFC2231State newstate) {
            state = newstate;
            if (newstate == RFC2231State.PARAM) {
                key = new StringBuilder();  value = new StringBuilder();
                continued = false;  encoded = false;
            }
        }

        void setContinued()  { continued = true; }
        void setEncoded() {
            encoded = true;
            if (!continued)
                charset = new StringBuilder();
        }

        void addCharsetChar(char c)  { charset.append(c); }
        void addKeyChar(char c)      { key.append(c); }
        void addValueChar(char c)    { value.append(c); }

        void saveToMap(Map<String, String> attrs) {
            if (value == null)
                return;
            String pname = key == null ? null : key.toString().toLowerCase();
            String pvalue = value.toString();
            if ("".equals(pname) && "".equals(pvalue))
                return;
            if (encoded) {
                if (charset.equals(""))
                    charset.append("us-ascii");
                try {
                    pvalue = URLDecoder.decode(pvalue, charset.toString());
                } catch (UnsupportedEncodingException uee) { 
                    System.out.println(uee);
                }
            }
            String existing = continued ? attrs.get(pname) : null;
            attrs.put(pname, existing == null ? pvalue : existing + pvalue);
            key = null;  value = null;
        }
    }

    private static Map<String, String> decodeRFC2231(String header) {
        if (header == null)
            return null;
        header = header.trim();

        RFC2231Data rfc2231 = new RFC2231Data();
        Map<String, String> attrs = new HashMap<String, String>();
        boolean escaped = false;

        for (int i = 0, count = header.length(); i < count; i++) {
            char c = header.charAt(i);
            if (rfc2231.state == RFC2231State.SLOP) {
                if (c == ';' || c == '\n' || c == '\r')
                    rfc2231.setState(RFC2231State.PARAM);
            } else if (c == '\r' || c == '\n') {
                if (!attrs.isEmpty() || rfc2231.value.length() > 0) {
                    rfc2231.saveToMap(attrs);
                    rfc2231.setState(RFC2231State.PARAM);
                }
                // otherwise, it's just folding and we can effectively just ignore the CR/LF
            } else if (rfc2231.state == RFC2231State.PARAM) {
                if (c == '=')
                    rfc2231.setState(RFC2231State.EQUALS);
                else if (c == '*')
                    rfc2231.setState(RFC2231State.EXTENDED);
                else if (c != ' ' && c != '\t')
                    rfc2231.addKeyChar(c);
            } else if (rfc2231.state == RFC2231State.VALUE) {
                if (c != ';' && c != ' ' && c != '\t') {
                    rfc2231.addValueChar(c);
                } else {
                    rfc2231.saveToMap(attrs);
                    rfc2231.setState(c == ';' ? RFC2231State.PARAM : RFC2231State.SLOP);
                }
            } else if (rfc2231.state == RFC2231State.QVALUE) {
                if (!escaped && c == '\\') {
                    escaped = true;
                } else if (escaped || c != '"') {
                    rfc2231.addValueChar(c);  escaped = false;
                } else {
                    rfc2231.saveToMap(attrs);
                    rfc2231.setState(RFC2231State.SLOP);
                }
            } else if (rfc2231.state == RFC2231State.EQUALS) {
                if (c == ';') {
                    rfc2231.saveToMap(attrs);
                    rfc2231.setState(RFC2231State.PARAM);
                } if (c == '"') {
                    escaped = false;
                    rfc2231.setState(RFC2231State.QVALUE);
                } else {
                    rfc2231.addValueChar(c);
                    rfc2231.setState(RFC2231State.VALUE);
                }
            } else if (rfc2231.state == RFC2231State.EXTENDED) {
                if (c >= '0' && c <= '9') {
                    if (c != '0')
                        rfc2231.setContinued();
                    rfc2231.setState(RFC2231State.CONTINUED);
                } else if (c == '=') {
                    rfc2231.setEncoded();
                    rfc2231.setState(rfc2231.continued ? RFC2231State.VALUE : RFC2231State.CHARSET);
                }
            } else if (rfc2231.state == RFC2231State.CONTINUED) {
                if (c == '=')
                    rfc2231.setState(RFC2231State.EQUALS);
                else if (c == '*')
                    rfc2231.setState(RFC2231State.EXTENDED);
                else if (c >= '0' && c <= '9')
                    rfc2231.setContinued();
            } else if (rfc2231.state == RFC2231State.CHARSET) {
                if (c == '\'')
                    rfc2231.setState(RFC2231State.LANG);
                else
                    rfc2231.addCharsetChar(c);
            } else if (rfc2231.state == RFC2231State.LANG) {
                if (c == '\'')
                    rfc2231.setState(RFC2231State.VALUE);
            }
        }

        rfc2231.saveToMap(attrs);
        return attrs;
    }

    public static MPartInfo getBody(List parts, boolean preferHtml) {
     	if (parts.isEmpty())
     		return null;
     	
     	// if top-level has no children, then it is the body
     	MPartInfo top = (MPartInfo) parts.get(0);
     	if (!top.getContentType().match(CT_MULTIPART_WILD))
     		return top.getDisposition().equals(Part.ATTACHMENT) ? null : top;

        return getBodySubpart(top, preferHtml);
    }

    private static MPartInfo getBodySubpart(MPartInfo base, boolean preferHtml) {
        // short-circuit malformed messages
        if (!base.hasChildren())
            return null;

        List<MPartInfo> children;
        if (base.getContentType().match(CT_MULTIPART_ALTERNATIVE))
            children = base.getChildren();
        else {
            // for multipart/mixed (etc.), only the first part is really "body"
            children = new ArrayList<MPartInfo>();
            children.add(base.getChildren().get(0));
        }

        // go through top-level children, stopping at first text part we are interested in
        MPartInfo alternative = null;
        for (MPartInfo p : children) {
            boolean isAttachment = p.getDisposition().equals(Part.ATTACHMENT);
            // the Content-Type we want and the one we'd settle for...
            String wantType = preferHtml ? CT_TEXT_HTML  : CT_TEXT_PLAIN;
            String altType  = preferHtml ? CT_TEXT_PLAIN : CT_TEXT_HTML;

            if (p.getContentType().match(wantType) && !isAttachment) {
                return p;
            } else if (p.getContentType().match(altType) && !isAttachment) {
                if (alternative == null)
                    alternative = p;
            } else if (p.getContentType().match(CT_MULTIPART_WILD)) {
                MPartInfo subpart = getBodySubpart(p, preferHtml);
                if (subpart == null)
                	continue;
                if (subpart.getContentType().match(wantType))
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

        System.out.println(decodeRFC2231("   \n  attachment;\n filename*=UTF-8''%E3%82%BD%E3%83%AB%E3%83%86%E3%82%A3%E3%83%AC%E3%82%A4.rtf\n  \n "));
        System.out.println(decodeRFC2231("application/x-stuff; title*0*=us-ascii'en'This%20is%20even%20more%20; title*1*=%2A%2A%2Afun%2A%2A%2A%20; title*2=\"isn't it!\"\n"));
        System.out.println(decodeRFC2231("multipart/mixed; charset=us-ascii;\n foo=\n  boundary=\"---\" \n"));
        System.out.println(decodeRFC2231("message/external-body; access-type=URL;\n URL*0=\"ftp://\";\n URL*1=\"cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar\"\n"));
        System.out.println(decodeRFC2231("application/x-stuff;\n\ttitle*=us-ascii'en-us'This%20is%20%2A%2A%2Afun%2A%2A%2A"));
    }
}