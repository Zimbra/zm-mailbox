/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.mime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.zimbra.common.mime.MimeAddressHeader;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.DateUtil;

public class MimeMessage extends MimePart {
    private static String sHostname = null;
        static {
            try {
                sHostname = InetAddress.getLocalHost().getHostName();
            } catch (Throwable t) { }
            if (sHostname == null) {
                sHostname = "localhost";
            }
        }

    private Properties mProperties;
    private MimePart mBody;

    public MimeMessage(MimeMessage mm) {
        super(mm);
        mProperties = mm.mProperties == null ? null : (Properties) mm.mProperties.clone();
        mBody = mm.mBody == null ? null : mm.mBody.clone().setParent(this);
    }

    public MimeMessage(Properties props) {
        super(new ContentType(ContentType.MESSAGE_RFC822));
        mProperties = props;

        mBody = new MimeBodyPart((ContentType) null);
        setHeader("Message-ID", '<' + UUID.randomUUID().toString() + '@' + sHostname + '>');
        setHeader("MIME-Version", "1.0");
    }

    MimeMessage(MimePart body, Properties props) {
        super(new ContentType(ContentType.MESSAGE_RFC822), null, 0, 0, null);
        mProperties = props;

        setBodyPart(body);
    }

    /** Parses a MIME message from a file.  Only the structure of the message
     *  is stored in memory, but a pointer to the original {@code File} is
     *  retained so that the content is accessible on demand. */
    public MimeMessage(File file) throws IOException {
        this(file, null);
    }

    /** Parses a MIME message from a file.  Only the structure of the message
     *  is stored in memory, but a pointer to the original {@code File} is
     *  retained so that the content is accessible on demand. */
    public MimeMessage(File file, Properties props) throws IOException {
        super(new ContentType(ContentType.MESSAGE_RFC822), null, 0, 0, null);
        mProperties = props;

        parse(new PartSource(file));
    }

    /** Parses a MIME message from a byte array.  The structure of the message
     *  is stored in memory and a pointer to the original byte array is
     *  retained so that the message content can be retrieved. */
    public MimeMessage(byte[] body) {
        this(body, null);
    }

    /** Parses a MIME message from a byte array.  The structure of the message
     *  is stored in memory and a pointer to the original byte array is
     *  retained so that the message content can be retrieved. */
    public MimeMessage(byte[] body, Properties props) {
        super(new ContentType(ContentType.MESSAGE_RFC822), null, 0, 0, null);
        mProperties = props;

        try {
            parse(new PartSource(body));
        } catch (IOException ioe) {
            throw new RuntimeException("completely unexpected IOException while reading from byte array", ioe);
        }
    }

    /** Parses a MIME message from an {@code InputStream}.  The entire stream
     *  is read into a byte array in memory before parsing so that the message
     *  content is accessible.  If you only need the MIME structure, use
     *  {@link #readStructure(InputStream, Properties)} instead. */
    public MimeMessage(InputStream is) throws IOException {
        this(is, null);
    }

    /** Parses a MIME message from an {@code InputStream}.  Unless that stream
     *  implements {@link InputStreamSource}, the entire stream is read into a
     *  byte array in memory before parsing so that the message content is
     *  subsequently accessible.  If you only need the MIME structure, use
     *  {@link #readStructure(InputStream, Properties)} instead. */
    public MimeMessage(InputStream is, Properties props) throws IOException {
        super(new ContentType(ContentType.MESSAGE_RFC822), null, 0, 0, null);
        mProperties = props;

        parse(is instanceof InputStreamSource ? new PartSource((InputStreamSource) is) : new PartSource(ByteUtil.getContent(is, -1)));
    }

    private void parse(PartSource psource) throws IOException {
        MimeParserInputStream mpis = new MimeParserInputStream(psource.getContentStream(0, psource.getLength()));
        ByteUtil.drain(mpis).insertBodyPart(this).attachSource(psource);
    }

    /** Constructor used internally to parse a message/rfc822 attachment. */
    MimeMessage(ContentType ctype, MimePart parent, long start, long body, MimeHeaderBlock headers) {
        super(ctype, parent, start, body, headers);
    }

    /** Reads the MIME structure of a message from an {@code InputStream}.
     *  Does <u>not</u> retain a copy of the message content; if you need the
     *  content accessible after the parse, please use one of the standard
     *  {@code MimeMessage} constructors. */
    public static MimeMessage readStructure(InputStream is, Properties props) throws IOException {
        return ByteUtil.drain(new MimeParserInputStream(is)).getMessage(props);
    }


    @Override protected MimeMessage clone() {
        return new MimeMessage(this);
    }

    /** Returns the {@link MimePart} that forms the "body" of this message.  For
     *  a "<tt>multipart/*<tt>" message, this will be a {@link MimeMultipart},
     *  and in almost all other cases it will be a {@link MimeBodyPart}.
     * @see #setBodyPart(MimePart) */
    public MimePart getBodyPart() {
        return mBody;
    }

    /** Sets the given part as this {@code MimeMessage}'s body part.  The part
     *  is removed from its previous parent, and its new parent is set to this
     *  message.  The old body part is detached from this message, and its
     *  message headers (those other than "<tt>Content-Transfer-Encoding</tt>"
     *  and "<tt>Content-Type</tt>") are transferred to the new one.
     * @see #getBodyPart() */
    public MimeMessage setBodyPart(MimePart newBody) {
        if (mBody == newBody) {
            return this;
        }

        if (mBody != null) {
            transferMessageHeaders(newBody);
            mBody.detach();
        }
        newBody.setParent(this);
        mBody = newBody;
        // almost certainly unnecessary due to header transfer, but don't cost nothin'
        markDirty(Dirty.CONTENT);
        return this;
    }

    @Override void removeChild(MimePart mp) {
        if (mp == mBody) {
            mBody = transferMessageHeaders(new MimeBodyPart((ContentType) null));
        }
    }

    private MimePart transferMessageHeaders(MimePart newBody) {
        for (Iterator<MimeHeader> it = mBody.getMimeHeaderBlock().iterator(); it.hasNext(); ) {
            MimeHeader header = it.next();
            String lcname = header.getName().toLowerCase();
            if (!lcname.equals("content-type") && !lcname.equals("content-transfer-encoding")) {
                // FIXME: want to have the new body's old headers at the *end* of the resulting list, not at the beginning
                newBody.addMimeHeader(header);
                it.remove();
            }
        }
        return newBody;
    }

    @Override Properties getProperties() {
        return mProperties != null || getParent() == null ? mProperties : getParent().getProperties();
    }


    @Override public MimePart getSubpart(String part) {
        if (part == null || part.equals("")) {
            return this;
        } else if (mBody == null) {
            return null;
        }

        boolean isMultipart = mBody instanceof MimeMultipart;
        if (part.equalsIgnoreCase("TEXT")) {
            return isMultipart ? mBody : null;
        } else if (isMultipart) {
            return mBody.getSubpart(part);
        }

        int dot = part.indexOf('.');
        if (dot == part.length() - 1 || !"1".equals(dot == -1 ? part : part.substring(0, dot))) {
            return null;
        } else {
            return mBody.getSubpart(dot == -1 ? "" : part.substring(dot + 1));
        }
    }

    /** Does a recursive descent of the message's structure and returns a
     *  mapping of part names to the {@link MimePart}s that comprise it.  The
     *  keys in the {@code Map} are IMAP-style part identifiers.  The
     *  {@code MimeMessage} itself is included in the {@code Map} with part
     *  name <tt>""</tt>. */
    public Map<String, MimePart> listMimeParts() {
        Map<String, MimePart> parts = new LinkedHashMap<String, MimePart>(6);
        parts.put("", this);
        return listMimeParts(parts, "");
    }

    @Override Map<String, MimePart> listMimeParts(Map<String, MimePart> parts, String parentName) {
        boolean isMultipart = mBody instanceof MimeMultipart;
        boolean topLevel = parentName.isEmpty();

        parts.put(parentName + (topLevel ? "" : ".") + (isMultipart ? "TEXT" : "1"), mBody);
        return mBody.listMimeParts(parts, parentName + (isMultipart ? "" : (topLevel ? "" : ".") + "1"));
    }


    public String getHeader(String name) {
        return mBody.getMimeHeader(name);
    }

    public byte[] getRawHeader(String name) {
        return mBody.getRawMimeHeader(name);
    }

    public void setHeader(String name, String value) {
        setHeader(name, value, null);
    }

    public void setHeader(String name, String value, String charset) {
        mBody.setMimeHeader(name, value, charset);
    }

    public void setHeader(String name, MimeHeader header) {
        mBody.setMimeHeader(name, header);
    }

//    public void addHeader(String name, String value) {
//        mBody.addMimeHeader(name, value);
//    }

    public void setAddressHeader(String name, InternetAddress iaddr) {
        setAddressHeader(name, iaddr == null ? null : Arrays.asList(iaddr));
    }

    public void setAddressHeader(String name, List<InternetAddress> iaddrs) {
        setHeader(name, iaddrs == null ? null : new MimeAddressHeader(name, iaddrs));
    }

    public List<InternetAddress> getAddressHeader(String name) {
        MimeHeader header = mBody.getMimeHeaderBlock().get(name);
        if (header == null) {
            return null;
        } else if (header instanceof MimeAddressHeader) {
            return ((MimeAddressHeader) header).getAddresses();
        } else {
            return new MimeAddressHeader(header).getAddresses();
        }
    }

    public void setSubject(String subject) {
        setSubject(subject, null);
    }

    public void setSubject(String subject, String charset) {
        setHeader("Subject", subject, charset);
    }

    public String getSubject() {
        return getHeader("Subject");
    }

    public void setSentDate(Date date) {
        setHeader("Date", date == null ? null : DateUtil.toRFC822Date(date));
    }

    public Date getSentDate() {
        return DateUtil.parseRFC2822Date(getHeader("Date"), null);
    }

    @Override ContentType updateContentType(ContentType ctype) {
        if (ctype != null && !ctype.getContentType().equals(ContentType.MESSAGE_RFC822)) {
            throw new UnsupportedOperationException("cannot change a message to text:" + ctype);
        }
        return super.updateContentType(ctype == null ? new ContentType(ContentType.MESSAGE_RFC822) : ctype);
    }


    @Override public long getSize() throws IOException {
        long size = super.getSize();
        if (size == -1 && mBody != null) {
            size = recordSize(mBody.getMimeHeaderBlock().getLength() + mBody.getSize());
        }
        return size;
    }

    @Override public InputStream getInputStream() throws IOException {
        return getParent() != null ? super.getInputStream() : mBody.getInputStream();
    }

    @Override public InputStream getRawContentStream() throws IOException {
        return getParent() != null || isDirty() ? mBody.getInputStream() : super.getRawContentStream();
    }

    public InputStream getRawContentStream(String[] omitHeaders) throws IOException {
        MimeHeaderBlock headers = mBody.getMimeHeaderBlock();
        for (String name : omitHeaders) {
            if (headers.containsHeader(name)) {
                MimeHeaderBlock trimmed = new MimeHeaderBlock(headers, omitHeaders);
                return new VectorInputStream(trimmed.toByteArray(), mBody.getRawContentStream());
            }
        }
        return getRawContentStream();
    }

    public MimeMessage setText(String text) throws IOException {
        return setText(text, null, null, null);
    }

    public MimeMessage setText(String text, String charset, String subtype, ContentTransferEncoding cte) throws IOException {
        if (mBody instanceof MimeBodyPart) {
            ((MimeBodyPart) mBody).setText(text, charset, subtype, cte);
        } else {
            setBodyPart(new MimeBodyPart(new ContentType("text/plain")).setText(text, charset, subtype, cte));
        }
        return this;
    }


    public static void main(String[] args) throws IOException {
        MimeMessage mm = new MimeMessage(new File(args[0] + File.separator + "toplevel-nested-message"));
        dumpParts(mm);
        ByteUtil.copy(mm.getRawContentStream(new String[] { "x-originalArrivalTime" }), true, System.out, false);
        ByteUtil.copy(mm.getRawContentStream(new String[] { "foo" }), true, System.out, false);
        mm.setHeader("X-Mailer", "Zimbra 5.0 RC2");
        dumpParts(mm);
        ((MimeBodyPart) mm.getSubpart("1.1")).setTransferEncoding(ContentTransferEncoding.BASE64);
        dumpParts(mm);
        mm.getSubpart("1.1").setFilename("boogle");
        ByteUtil.copy(mm.getSubpart("1").getRawContentStream(), true, System.out, false);
        ByteUtil.copy(mm.getInputStream(), true, System.out, false);
        System.out.write(mm.getSubpart("1").getRawContent());
        ByteUtil.copy(mm.getSubpart("1").getInputStream(), true, System.out, false);
        System.out.write(mm.getSubpart("1").getContent());

        mm = new MimeMessage(new File(args[0] + File.separator + "digest-attachment-16771"));
        dumpParts(mm);
        try {
            javax.mail.Session jsession = javax.mail.Session.getInstance(new Properties());
            javax.mail.internet.MimeMessage jmm = new javax.mail.internet.MimeMessage(jsession, new FileInputStream(args[0] + File.separator + "23079"));
            javax.mail.internet.MimeMultipart jmmulti = (javax.mail.internet.MimeMultipart) jmm.getContent();
            javax.mail.internet.MimeBodyPart jmmbp = (javax.mail.internet.MimeBodyPart) jmmulti.getBodyPart(1);
            javax.mail.internet.MimePartDataSource jmmpds = new javax.mail.internet.MimePartDataSource(jmmbp);
            MimePart mp = new MimeBodyPart(new ContentType("text/html")).setContent(jmmpds).setTransferEncoding(ContentTransferEncoding.QUOTED_PRINTABLE);
            ((MimeMultipart) mm.getSubpart("TEXT")).addPart(mp, 1);
        } catch (javax.mail.MessagingException e) {
            e.printStackTrace();
        }
        dumpParts(mm);
        ByteUtil.copy(mm.getInputStream(), true, System.out, false);

        mm = new MimeMessage(new File(args[0] + File.separator + "bad-ctype-params-11946"));
//        dumpParts(mm);
        ((MimeBodyPart) mm.getSubpart("1")).setTransferEncoding(ContentTransferEncoding.SEVEN_BIT);
//        ((MimeBodyPart) mm.getSubpart("1")).setTransferEncoding(ContentTransferEncoding.QUOTED_PRINTABLE);
//        ByteUtil.copy(mm.getSubpart("1").getRawContentStream(), true, System.out, false);
//        ByteUtil.copy(mm.getInputStream(), true, System.out, false);

        mm = new MimeMessage(new File(args[0] + File.separator + "blank-base64-ellen"));
        dumpParts(mm);
//        InputStream in1 = mm.getSubpart("2").getContentStream(), in2 = null;
//        try {
//            javax.mail.Session jsession = javax.mail.Session.getInstance(new Properties());
//            javax.mail.internet.MimeMessage jmm = new javax.mail.internet.MimeMessage(jsession, new FileInputStream(args[0] + File.separator + "blank-base64-ellen"));
//            in2 = Mime.getMimePart(jmm, "2").getInputStream();
//        } catch (javax.mail.MessagingException me) { }
//        int pos = 0, c1 = 0, c2 = 0;
////        while ((c1 = in1.read()) == (c2 = in2.read()) && c1 != -1)
////            pos++;
//        byte[] body1 = ByteUtil.getContent(in1, -1), body2 = ByteUtil.getContent(in2, -1);
//        if (body1.length != body2.length)
//            System.out.println("difference in lengths (new = " + body1.length + ", old = " + body2.length + ")");
//        for (pos = 0; pos < Math.min(body1.length, body2.length) && (c1 = body1[pos]) == (c2 = body2[pos]); pos++)
//            ;
//        if (c1 != c2)
//            System.out.println("divergence at decoded position " + pos + " (new = " + c1 + ", old = " + c2 + ")");
//        InputStream in1 = mm.getSubpart("TEXT").getContentStream(), in2 = null;
//        try {
//            javax.mail.Session jsession = javax.mail.Session.getInstance(new Properties());
//            javax.mail.internet.MimeMessage jmm = new javax.mail.internet.MimeMessage(jsession, new FileInputStream(args[0] + File.separator + "blank-base64-ellen"));
//            java.util.List<MPartInfo> mpis = Mime.getParts(jmm);
//            for (MPartInfo mpi : mpis)
//                if (mpi.mPartName.equals("TEXT"))
//                    in2 = mpi.mPart.getInputStream();
//        } catch (javax.mail.MessagingException me) { }
//        int pos = 0, c1 = 0, c2 = 0;
//        while ((c1 = in1.read()) == (c2 = in2.read()) && c1 != -1)
//            pos++;
//        if (c1 != c2)
//            System.out.println("divergence at decoded position " + pos + " (new = " + c1 + ", old = " + c2 + ")");
//        ByteUtil.copy(mm.getSubpart("4").getInputStream(), true, System.out, false);
        ((MimeMultipart) mm.getBodyPart()).setContentType(((MimeMultipart) mm.getBodyPart()).getContentType().setParameter("boundary", "b*o*u*n*d*a*r*y"));
//        System.out.write(mm.getContent());

        mm = new MimeMessage(new File(args[0] + File.separator + "partial-multipart-5775"));
//        dumpParts(mm);

        mm = new MimeMessage(new File(args[0] + File.separator + "zimbra-accent"));
//        dumpParts(mm);
        ((MimeBodyPart) mm.getSubpart("1")).setContentType(new ContentType("text/plain; format=flowed; charset=iso-8859-2"));
        ((MimeBodyPart) mm.getSubpart("1")).setTransferEncoding(ContentTransferEncoding.BASE64);
        ((MimeMultipart) mm.getBodyPart()).setContentType(((MimeMultipart) mm.getBodyPart()).getContentType().setParameter("boundary", "b*o*u*n*d*a*r*y"));
//        System.out.write(mm.getSubpart("2").getContent());
//        ByteUtil.copy(mm.getSubpart("2").getContentStream(), true, System.out, false);
        System.out.write(mm.getContent());

        MimeBodyPart body = new MimeBodyPart(new ContentType("text/enriched; charset=us-ascii"));
        body.setMimeHeader("Content-Disposition", "attachment; filename=bar.txt");
        body.setText("espionage detected!");
//        ((MimeMultipart) mm.getBodyPart()).addPart(body);
        MimeMultipart multi = new MimeMultipart("mixed");
        multi.addPart(mm.getBodyPart());
        multi.addPart(body);
        mm.setBodyPart(multi);
//        dumpParts(mm);
        System.out.write(mm.getRawContent());

        mm = MimeMessage.readStructure(new FileInputStream(new File(args[0] + File.separator + "report-attachment-6667")), null);
        dumpParts(mm);
//        ByteUtil.copy(mm.getInputStream(), true, System.out, false);

        mm = new MimeMessage(new File(args[0] + File.separator + "report-attachment-6667"));
//        dumpParts(mm);
        ((MimeBodyPart) mm.getSubpart("1")).setTransferEncoding(ContentTransferEncoding.QUOTED_PRINTABLE);
//        System.out.write(mm.getSubpart("2").getRawContent());
//        ByteUtil.copy(mm.getSubpart("2.3").getInputStream(), true, System.out, false);
//        ByteUtil.copy(mm.getInputStream(), true, System.out, false);


//        mm = new MimeMessage(new File(args[0] + File.separator + "brinkster-16512"));
//        dumpParts(mm);
//
//        long time = System.currentTimeMillis();
//        for (int i = 0; i < 10; i++)
//            new MimeMessage(new File(args[0] + File.separator + "brinkster-16512"));
//        System.out.println("NEW: " + (System.currentTimeMillis() - time) / 10 + "ms");
//
//        javax.mail.Session jsession = javax.mail.Session.getInstance(new Properties());
//        javax.mail.internet.MimeMessage jmm = null;
//        time = System.currentTimeMillis();
//        try {
//            for (int i = 0; i < 10; i++) {
//                jmm = new javax.mail.internet.MimeMessage(jsession, new FileInputStream(args[0] + File.separator + "brinkster-16512"));
//                Mime.getParts(jmm);
//            }
//            System.out.println("OLD: " + (System.currentTimeMillis() - time) / 10 + "ms");
//            for (MPartInfo mpi : Mime.getParts(jmm))
//                System.out.println('"' + mpi.mPartName + "\": " + mpi.mContentType + (mpi.mFilename == null ? "" : " [" + mpi.mFilename + "]"));
//        } catch (javax.mail.MessagingException e) {
//            System.out.println("error during JavaMail parse");
//        }

        mm = new MimeMessage((Properties) null);
        mm.setHeader("Subject", "testing \u00e9ncoding");
        ByteUtil.copy(mm.getInputStream(), true, System.out, false);
    }

    static void dumpParts(MimeMessage mm) throws IOException {
        for (Map.Entry<String, MimePart> mpi : mm.listMimeParts().entrySet()) {
            MimePart part = mpi.getValue();
            String size = part.getSize() < 0 ? "unknown size" : part.getSize() + " bytes";
            String lines = part.getLineCount() < 0 ? "" : ", " + part.getLineCount() + " lines";
            String filename = part.getFilename() == null ? "" : " [" + part.getFilename() + "]";
            String desc = part.getMimeHeader("Content-Description") == null ? "" : " {" + part.getMimeHeader("Content-Description") + "}";
            System.out.println('"' + mpi.getKey() + "\": " + part.getContentType().getContentType() + " (" + size + lines + ")" + filename + desc);
            if (mm.getSubpart(mpi.getKey()) != mpi.getValue()) {
                System.out.println("  MISMATCH!");
            }
            if (part instanceof MimeMultipart) {
                MimeBodyPart extra;
                if ((extra = ((MimeMultipart) part).getPreamble()) != null) {
                    System.out.println("  preamble: " + extra.getLineCount() + " line(s)");
                }
                if ((extra = ((MimeMultipart) part).getEpilogue()) != null) {
                    System.out.println("  epilogue: " + extra.getLineCount() + " line(s)");
                }
            }
//            if (mpi.getValue().getContentType().getValue().equals("text/plain")) {
//                try { System.out.println(new String(part.getRawContent())); } catch (IOException ioe) {}
//            }
        }
    }
}
