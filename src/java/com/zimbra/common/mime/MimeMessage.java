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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.zimbra.common.util.ByteUtil;

public class MimeMessage extends MimePart {
    private Properties mProperties;
    private MimePart mBody;

    MimeMessage(MimePart body, Properties props) {
        super(new ContentType(ContentType.MESSAGE_RFC822), null, 0, 0, null);
        mProperties = props;

        setBodyPart(body);
    }

    /** Parses a MIME message from a file.  Only the structure of the message
     *  is stored in memory, but a pointer to the original <code>File</code> is
     *  retained so that the content is accessible on demand. */
    public MimeMessage(File file) throws IOException {
        this(file, null);
    }

    /** Parses a MIME message from a file.  Only the structure of the message
     *  is stored in memory, but a pointer to the original <code>File</code> is
     *  retained so that the content is accessible on demand. */
    public MimeMessage(File file, Properties props) throws IOException {
        super(new ContentType(ContentType.MESSAGE_RFC822), null, 0, 0, null);
        mProperties = props;

        setContent(file, false);
        InputStream is = new BufferedInputStream(new FileInputStream(file), 8192);
        try {
            readContent(new ParseState(new PeekAheadInputStream(is)));
        } finally {
            ByteUtil.closeStream(is);
        }
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

        setContent(body, false);
        try {
            readContent(new ParseState(new PeekAheadInputStream(new ByteArrayInputStream(body))));
        } catch (IOException ioe) {
            throw new RuntimeException("completely unexpected IOException while reading from byte array", ioe);
        }
    }

    /** Parses a MIME message from an <code>InputStream</code>.  The entire
     *  stream is read into a byte array in memory before parsing so that the
     *  message content is accessible.  If you only need the MIME structure,
     *  use {@link #readStructure(InputStream, Properties)} instead. */
    public MimeMessage(InputStream is) throws IOException {
        this(is, null);
    }

    /** Parses a MIME message from an <code>InputStream</code>.  The entire
     *  stream is read into a byte array in memory before parsing so that the
     *  message content is accessible.  If you only need the MIME structure,
     *  use {@link #readStructure(InputStream, Properties)} instead. */
    public MimeMessage(InputStream is, Properties props) throws IOException {
        this(ByteUtil.getContent(is, -1), props);
    }

    /** Constructor used internally to parse a message/rfc822 attachment. */
    MimeMessage(ContentType ctype, MimePart parent, long start, long body, MimeHeaderBlock headers) {
        super(ctype, parent, start, body, headers);
    }

    /** Reads the MIME structure of a message from an <code>InputStream</code>.
     *  Does <u>not</u> retain a copy of the message content; if you need the
     *  content accessible after the parse, please use one of the standard
     *  <code>MimeMessage</code> constructors. */
    public static MimeMessage readStructure(InputStream is, Properties props) throws IOException {
        MimeMessage mm = new MimeMessage(new ContentType(ContentType.MESSAGE_RFC822), null, 0, 0, null);
        mm.mProperties = props;

        if (!is.markSupported())
            is = new BufferedInputStream(is, 8192);
        mm.readContent(new ParseState(new PeekAheadInputStream(is)));
        return mm;
    }

    /** Returns the {@link MimePart} that forms the "body" of this message.  For
     *  a <tt>multipart/*<tt> message, this will be a {@link MimeMultipart}, and
     *  in almost all other cases it will be a {@link MimeBodyPart}.
     * @see #setBodyPart(MimePart) */
    public MimePart getBodyPart() {
        return mBody;
    }

    /** Sets the given part as this <code>MimeMessage</code>'s body part.  The
     *  part is removed from its previous parent, and its new parent is set to
     *  this message.  The old body part is detached from this message, and its
     *  message headers (those other than the standard MIME "<tt>Content-*</tt>"
     *  headers) are transferred to the new one.
     * @see #getBodyPart() */
    public void setBodyPart(MimePart body) {
        if (mBody != null) {
            transferMessageHeaders(body);
            mBody.detach();
        }
        body.setParent(this);
        mBody = body;
    }

    @Override void removeChild(MimePart mp) {
        if (mp == mBody) {
            mBody = transferMessageHeaders(new MimeBodyPart(null));
        }
    }

    private MimePart transferMessageHeaders(MimePart newBody) {
        for (Iterator<MimeHeader> it = mBody.getMimeHeaderBlock().iterator(); it.hasNext(); ) {
            MimeHeader header = it.next();
            if (!header.getName().toLowerCase().startsWith("content-")) {
                // FIXME: want to have the new body's old headers at the *end* of the resulting list, not at the beginning
                newBody.addMimeHeader(header.getName(), header);
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
     *  keys in the <code>Map</code> are IMAP-style part identifiers.  The
     *  <code>MimeMessage</code> itself is included in the <code>Map</code>
     *  with part name <tt>""</tt>. */
    public Map<String, MimePart> listMimeParts() {
        Map<String, MimePart> parts = new LinkedHashMap<String, MimePart>(6);
        parts.put("", this);
        return listMimeParts(parts, "");
    }

    @Override Map<String, MimePart> listMimeParts(Map<String, MimePart> parts, String prefix) {
        boolean isMultipart = mBody instanceof MimeMultipart;
        boolean topLevel = prefix.equals("");

        parts.put(prefix + (topLevel ? "" : ".") + (isMultipart ? "TEXT" : "1"), mBody);
        return mBody.listMimeParts(parts, prefix + (isMultipart ? "" : (topLevel ? "" : ".") + "1"));
    }


    public String getHeader(String name) {
        return mBody.getMimeHeader(name);
    }

    public byte[] getRawHeader(String name) {
        return mBody.getRawMimeHeader(name);
    }

    public void setHeader(String name, String value) {
        mBody.setMimeHeader(name, value);
    }

//    public void addHeader(String name, String value) {
//        mBody.addMimeHeader(name, value);
//    }

    @Override public void setContentType(ContentType ctype) {
        if (ctype == null) {
            ctype = new ContentType(ContentType.MESSAGE_RFC822);
        } else if (!ctype.getValue().equals(ContentType.MESSAGE_RFC822)) {
            throw new UnsupportedOperationException("cannot change a message to another type");
        }
        super.setContentType(ctype);
    }

    @Override void checkContentType(ContentType ctype) {
        if (ctype == null || !ctype.getValue().equals(ContentType.MESSAGE_RFC822)) {
            throw new UnsupportedOperationException("cannot change a message to text");
        }
    }


    @Override public InputStream getInputStream() throws IOException {
        return getParent() != null ? super.getInputStream() : mBody.getInputStream();
    }

    @Override public InputStream getRawContentStream() throws IOException {
        return getParent() != null || isDirty() ? mBody.getInputStream() : super.getRawContentStream();
    }


    @Override MimePart readContent(ParseState pstate) throws IOException {
        mBody = MimePart.parse(pstate, this, ContentType.TEXT_PLAIN);
        recordEndpoint(mBody.getEndOffset());
        return this;
    }


    public static void main(String[] args) throws FileNotFoundException, IOException {
        MimeMessage mm = new MimeMessage(new File("C:\\Temp\\mail\\24250"));
//        dumpParts(mm);
        mm.setHeader("X-Mailer", "Zimbra 5.0 RC2");
//        ((MimeBodyPart) mm.getSubpart("1.1")).setTransferEncoding(ContentTransferEncoding.BASE64);
//        ByteUtil.copy(mm.getSubpart("1").getRawContentStream(), true, System.out, false);
//        ByteUtil.copy(mm.getInputStream(), true, System.out, false);
//        System.out.write(mm.getSubpart("1").getRawContent());
//        ByteUtil.copy(mm.getSubpart("1").getInputStream(), true, System.out, false);
//        System.out.write(mm.getSubpart("1").getContent());

        mm = new MimeMessage(new File("C:\\Temp\\mail\\digest-attachment-16771"));
//        dumpParts(mm);

        mm = new MimeMessage(new File("C:\\Temp\\mail\\bad-ctype-params-11946"));
//        dumpParts(mm);
        ((MimeBodyPart) mm.getSubpart("1")).setTransferEncoding(ContentTransferEncoding.SEVEN_BIT);
//        ((MimeBodyPart) mm.getSubpart("1")).setTransferEncoding(ContentTransferEncoding.QUOTED_PRINTABLE);
//        ByteUtil.copy(mm.getSubpart("1").getRawContentStream(), true, System.out, false);
//        ByteUtil.copy(mm.getInputStream(), true, System.out, false);

        mm = new MimeMessage(new File("C:\\Temp\\blank-base64-ellen"));
        dumpParts(mm);
//        InputStream in1 = mm.getSubpart("2").getContentStream(), in2 = null;
//        try {
//            javax.mail.Session jsession = javax.mail.Session.getInstance(new Properties());
//            javax.mail.internet.MimeMessage jmm = new javax.mail.internet.MimeMessage(jsession, new FileInputStream("C:\\Temp\\blank-base64-ellen"));
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
//            javax.mail.internet.MimeMessage jmm = new javax.mail.internet.MimeMessage(jsession, new FileInputStream("C:\\Temp\\blank-base64-ellen"));
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

        mm = new MimeMessage(new File("C:\\Temp\\mail\\partial-multipart-5775"));
//        dumpParts(mm);

        mm = new MimeMessage(new File("C:\\Temp\\mail\\zimbra-accent"));
//        dumpParts(mm);
        ((MimeBodyPart) mm.getSubpart("1")).setContentType(new ContentType("text/plain; format=flowed; charset=iso-8859-2"));
        ((MimeBodyPart) mm.getSubpart("1")).setTransferEncoding(ContentTransferEncoding.BASE64);
        ((MimeMultipart) mm.getBodyPart()).setContentType(((MimeMultipart) mm.getBodyPart()).getContentType().setParameter("boundary", "b*o*u*n*d*a*r*y"));
//        System.out.write(mm.getSubpart("2").getContent());
//        ByteUtil.copy(mm.getSubpart("2").getContentStream(), true, System.out, false);
//        System.out.write(mm.getContent());

        MimeBodyPart body = new MimeBodyPart(new ContentType("text/enriched; charset=us-ascii"));
        body.setMimeHeader("Content-Disposition", "attachment; filename=bar.txt");
        body.setText("espionage detected!");
//        ((MimeMultipart) mm.getBodyPart()).addPart(body);
        MimeMultipart multi = new MimeMultipart("mixed");
        multi.addPart(mm.getBodyPart());
        multi.addPart(body);
        mm.setBodyPart(multi);
//        dumpParts(mm);
//        System.out.write(mm.getRawContent());

        mm = MimeMessage.readStructure(new FileInputStream(new File("C:\\Temp\\mail\\report-attachment-6667")), null);
//        dumpParts(mm);
//        ByteUtil.copy(mm.getInputStream(), true, System.out, false);

        mm = new MimeMessage(new File("C:\\Temp\\mail\\report-attachment-6667"));
//        dumpParts(mm);
        ((MimeBodyPart) mm.getSubpart("1")).setTransferEncoding(ContentTransferEncoding.QUOTED_PRINTABLE);
//        System.out.write(mm.getSubpart("2").getRawContent());
//        ByteUtil.copy(mm.getSubpart("2.3").getInputStream(), true, System.out, false);
//        ByteUtil.copy(mm.getInputStream(), true, System.out, false);


//        mm = new MimeMessage(new File("C:\\Temp\\mail\\brinkster-16512"));
//        dumpParts(mm);
//
//        long time = System.currentTimeMillis();
//        for (int i = 0; i < 10; i++)
//            new MimeMessage(new File("C:\\Temp\\mail\\brinkster-16512"));
//        System.out.println("NEW: " + (System.currentTimeMillis() - time) / 10 + "ms");
//
//        javax.mail.Session jsession = javax.mail.Session.getInstance(new Properties());
//        javax.mail.internet.MimeMessage jmm = null;
//        time = System.currentTimeMillis();
//        try {
//            for (int i = 0; i < 10; i++) {
//                jmm = new javax.mail.internet.MimeMessage(jsession, new FileInputStream("C:\\Temp\\mail\\brinkster-16512"));
//                Mime.getParts(jmm);
//            }
//            System.out.println("OLD: " + (System.currentTimeMillis() - time) / 10 + "ms");
//            for (MPartInfo mpi : Mime.getParts(jmm))
//                System.out.println('"' + mpi.mPartName + "\": " + mpi.mContentType + (mpi.mFilename == null ? "" : " [" + mpi.mFilename + "]"));
//        } catch (javax.mail.MessagingException e) {
//            System.out.println("error during JavaMail parse");
//        }
    }

    static void dumpParts(MimeMessage mm) {
        for (Map.Entry<String, MimePart> mpi : mm.listMimeParts().entrySet()) {
            MimePart part = mpi.getValue();
            String filename = part.getFilename() == null ? "" : " [" + part.getFilename() + "]";
            String desc = part.getMimeHeader("Content-Description") == null ? "" : " {" + part.getMimeHeader("Content-Description") + "}";
            String lines = part.getLineCount() < 0 ? "" : ", " + part.getLineCount() + " lines";
            System.out.println('"' + mpi.getKey() + "\": " + part.getContentType().getValue() + " (" + part.getSize() + " bytes" + lines + ")" + filename + desc);
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
