/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.common.mime.shim;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Header;
import javax.mail.MessageAware;
import javax.mail.MessageContext;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.SharedInputStream;

import com.sun.mail.util.ASCIIUtility;
import com.sun.mail.util.PropUtil;
import com.zimbra.common.util.ByteUtil;

public class JavaMailMimeBodyPart extends MimeBodyPart implements JavaMailShim {
    private static final boolean ZPARSER = JavaMailMimeMessage.ZPARSER;

    private com.zimbra.common.mime.MimePart zpart;
    private JavaMailMimeMultipart jmparent;
    private Object jmcontent;  // JavaMailMimeMultipart or JavaMailMimeMessage or null

    JavaMailMimeBodyPart(com.zimbra.common.mime.MimePart mp) {
        this(mp, null);
    }

    JavaMailMimeBodyPart(com.zimbra.common.mime.MimePart mp, JavaMailMimeMultipart parent) {
        this.zpart = mp;
        this.jmparent = parent;
    }

    public JavaMailMimeBodyPart() {
        super();
        this.zpart = new com.zimbra.common.mime.MimeBodyPart(null);
    }

    public JavaMailMimeBodyPart(InputStream is) throws MessagingException {
        super();
        if (ZPARSER) {
            try {
                com.zimbra.common.mime.MimePart.InputStreamSource iss = null;
                byte[] bcontent = null;
                if (is instanceof com.zimbra.common.mime.MimePart.InputStreamSource) {
                    iss = (com.zimbra.common.mime.MimePart.InputStreamSource) is;
                } else if (is instanceof SharedInputStream) {
                    iss = new SharedInputStreamSource((SharedInputStream) is);
                } else {
                    bcontent = ByteUtil.getContent(is, -1);
                    is = new ByteArrayInputStream(bcontent);
                }

                com.zimbra.common.mime.MimeParserInputStream mpis = new com.zimbra.common.mime.MimeParserInputStream(is);
                if (bcontent != null) {
                    mpis.setSource(bcontent);
                } else {
                    mpis.setSource(iss);
                }
                try {
                    JavaMailMimeBodyPart.writeTo(mpis, null);
                    this.zpart = mpis.getPart();
                } finally {
                    ByteUtil.closeStream(mpis);
                }
            } catch (IOException ioe) {
                throw new MessagingException("error parsing mime body part", ioe);
            }
        } else {
            // copied from superclass; no way to call this indirectly
            if (!(is instanceof ByteArrayInputStream) && !(is instanceof BufferedInputStream) && !(is instanceof SharedInputStream)) {
                is = new BufferedInputStream(is);
            }
            
            this.headers = new JavaMailInternetHeaders(is);
            
            if (is instanceof SharedInputStream) {
                SharedInputStream sis = (SharedInputStream) is;
                this.contentStream = sis.newStream(sis.getPosition(), -1);
            } else {
                try {
                    this.content = ASCIIUtility.getBytes(is);
                } catch (IOException ioex) {
                    throw new MessagingException("Error reading input stream", ioex);
                }
            }
        }
    }

    public JavaMailMimeBodyPart(InternetHeaders headers, byte[] content) throws MessagingException {
        super();
        if (ZPARSER) {
            com.zimbra.common.mime.MimeHeaderBlock hblock;
            if (headers instanceof JavaMailInternetHeaders) {
                hblock = ((JavaMailInternetHeaders) headers).getZimbraMimeHeaderBlock();
            } else {
                hblock = new JavaMailInternetHeaders(headers).getZimbraMimeHeaderBlock();
            }

            try {
                InputStream is = new ByteArrayInputStream(content);
                com.zimbra.common.mime.MimeParserInputStream mpis = new com.zimbra.common.mime.MimeParserInputStream(is, hblock);
                JavaMailMimeBodyPart.writeTo(mpis.setSource(content), null);
                this.zpart = mpis.getPart();
            } catch (IOException ioe) {
                throw new MessagingException("error parsing mime body part", ioe);
            }
        } else {
            this.headers = headers;
            this.content = content;
        }
    }

    static class SharedInputStreamSource extends FilterInputStream implements com.zimbra.common.mime.MimePart.InputStreamSource {
        private SharedInputStream stream;
        private long size;

        SharedInputStreamSource(SharedInputStream sis) {
            super((InputStream) sis);
            this.stream = sis;
            try {
                int remaining = ((InputStream) sis).available();
                this.size = remaining <= 0 ? -1 : sis.getPosition() + remaining;
            } catch (IOException e) {
                this.size = -1;
            }
        }

        @Override public InputStream newStream(long start, long end) {
            return stream.newStream(start, end);
        }

        @Override public long getSize() {
            return size;
        }
    }

    com.zimbra.common.mime.MimePart getZimbraMimePart() {
        return zpart;
    }

    @Override public int getSize() throws MessagingException {
        if (ZPARSER) {
            try {
                return (int) zpart.getSize();
            } catch (IOException ioe) {
                throw new MessagingException("error calculating part size", ioe);
            }
        } else {
            return super.getSize();
        }
    }

    @Override public int getLineCount() throws MessagingException {
        if (ZPARSER) {
            return zpart.getLineCount();
        } else {
            return super.getLineCount();
        }
    }

    @Override public String getContentType() throws MessagingException {
        if (ZPARSER) {
            return zpart.getContentType().toString();
        } else {
            return super.getContentType();
        }
    }

    @Override public boolean isMimeType(String mimeType) throws MessagingException {
        if (ZPARSER) {
            String type = zpart.getContentType().getContentType();
            if (mimeType.endsWith("/*")) {
                return type.startsWith(mimeType.substring(0, mimeType.length() - 2).toLowerCase());
            } else {
                return type.equals(mimeType.toLowerCase());
            }
        } else {
            return super.isMimeType(mimeType);
        }
    }

    @Override public String getDisposition() throws MessagingException {
        if (ZPARSER) {
            if (zpart.getRawMimeHeader("Content-Disposition") != null) {
                return zpart.getContentDisposition().getDisposition();
            } else {
                return null;
            }
        } else {
            return super.getDisposition();
        }
    }

    @Override public void setDisposition(String disposition) throws MessagingException {
        if (ZPARSER) {
            boolean hasDisposition = zpart.getRawMimeHeader("Content-Disposition") != null;
            if (hasDisposition && disposition != null) {
                zpart.setMimeHeader("Content-Disposition", zpart.getContentDisposition().setDisposition(disposition));
            } else if (hasDisposition || disposition != null) {
                zpart.setMimeHeader("Content-Disposition", disposition);
            }
        } else {
            super.setDisposition(disposition);
        }
    }

    @Override public String getEncoding() throws MessagingException {
        if (ZPARSER) {
            if (zpart.getRawMimeHeader("Content-Transfer-Encoding") == null) {
                return null;
            } else if (zpart instanceof com.zimbra.common.mime.MimeBodyPart) {
                return ((com.zimbra.common.mime.MimeBodyPart) zpart).getTransferEncoding().toString();
            } else {
                return zpart.getMimeHeader("Content-Transfer-Encoding").trim().toLowerCase();
            }
        } else {
            return super.getEncoding();
        }
    }

    @Override public String getContentID() throws MessagingException {
        if (ZPARSER) {
            return zpart.getMimeHeader("Content-ID");
        } else {
            return super.getContentID();
        }
    }

    @Override public void setContentID(String cid) throws MessagingException {
        if (ZPARSER) {
            zpart.setMimeHeader("Content-ID", cid);
        } else {
            super.setContentID(cid);
        }
    }

    @Override public String getContentMD5() throws MessagingException {
        if (ZPARSER) {
            return zpart.getMimeHeader("Content-MD5");
        } else {
            return super.getContentMD5();
        }
    }

    @Override public void setContentMD5(String md5) throws MessagingException {
        if (ZPARSER) {
            zpart.setMimeHeader("Content-MD5", md5);
        } else {
            super.setContentMD5(md5);
        }
    }

    @Override public String[] getContentLanguage() throws MessagingException {
        if (ZPARSER) {
            // TODO Auto-generated method stub
            return super.getContentLanguage();
        } else {
            return super.getContentLanguage();
        }
    }

    @Override public void setContentLanguage(String[] languages) throws MessagingException {
        if (ZPARSER) {
            if (languages == null) {
                zpart.setMimeHeader("Content-Language", (String) null);
            } else if (languages.length == 0) {
                zpart.setMimeHeader("Content-Language", "");
            } else {
                StringBuilder sb = new StringBuilder(languages[0]);
                for (int i = 1; i < languages.length; i++) {
                    sb.append(", ").append(languages[i]);
                }
                zpart.setMimeHeader("Content-Language", sb.toString());
            }
        } else {
            super.setContentLanguage(languages);
        }
    }

    @Override public String getDescription() throws MessagingException {
        if (ZPARSER) {
            return zpart.getMimeHeader("Content-Description");
        } else {
            return super.getDescription();
        }
    }

    @Override public void setDescription(String description) throws MessagingException {
        if (ZPARSER) {
            zpart.setMimeHeader("Content-Description", description);
        } else {
            super.setDescription(description);
        }
    }

    @Override public void setDescription(String description, String charset) throws MessagingException {
        if (ZPARSER) {
            zpart.setMimeHeader("Content-Description", description, charset);
        } else {
            super.setDescription(description, charset);
        }
    }

    private static boolean decodeFileName = PropUtil.getBooleanSystemProperty("mail.mime.decodefilename", false);

    @Override public String getFileName() throws MessagingException {
        if (ZPARSER) {
            String filename = zpart.getFilename();
            if (filename != null && !decodeFileName) {
                filename = com.zimbra.common.mime.MimeHeader.escape(filename, null, false);
            }
            return filename;
        } else {
            return super.getFileName();
        }
    }

    @Override public void setFileName(String filename) throws MessagingException {
        if (ZPARSER) {
            // does the right thing regardless of whether the filename is encoded or not
            zpart.setFilename(filename);
        } else {
            super.setFileName(filename);
        }
    }

    @Override public InputStream getInputStream() throws IOException, MessagingException  {
        if (ZPARSER) {
            return zpart.getContentStream();
        } else {
            return super.getInputStream();
        }
    }

    @Override protected InputStream getContentStream() throws MessagingException {
        if (ZPARSER) {
            try {
                return zpart.getRawContentStream();
            } catch (IOException ioe) {
                throw new MessagingException("error fetching content stream", ioe);
            }
        } else {
            return super.getContentStream();
        }
    }

    @Override public InputStream getRawInputStream() throws MessagingException {
        if (ZPARSER) {
            return getContentStream();
        } else {
            return super.getRawInputStream();
        }
    }

    public static class MimePartDataSource implements DataSource, MessageAware {
        private JavaMailMimeBodyPart jmpart;

        public MimePartDataSource(JavaMailMimeBodyPart part) {
            jmpart = part;
        }

        @Override public InputStream getInputStream() throws IOException {
            return jmpart.getZimbraMimePart().getContentStream();
        }

        @Override public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override public String getContentType() {
            return jmpart.getZimbraMimePart().getContentType().toString();
        }

        @Override public String getName() {
            try {
                return jmpart.getFileName();
            } catch (MessagingException me) {
                return null;
            }
        }

        @Override public MessageContext getMessageContext() {
            return new MessageContext(jmpart);
        }
    }

    @Override public DataHandler getDataHandler() throws MessagingException {
        if (ZPARSER) {
            return new DataHandler(new MimePartDataSource(this));
        } else {
            return super.getDataHandler();
        }
    }

    @Override public Object getContent() throws IOException, MessagingException {
        if (ZPARSER) {
            if (jmcontent != null) {
                return jmcontent;
            } else if (zpart instanceof com.zimbra.common.mime.MimeMessage) {
                return this.jmcontent = new JavaMailMimeMessage((com.zimbra.common.mime.MimeMessage) zpart);
            } else if (zpart instanceof com.zimbra.common.mime.MimeMultipart) {
                return this.jmcontent = new JavaMailMimeMultipart((com.zimbra.common.mime.MimeMultipart) zpart, this);
            } else {
                return getDataHandler().getContent();
            }
        } else {
            return super.getContent();
        }
    }

    private static final String[] NON_TRANSFERRED = new String[] { "Content-Type", "Content-Transfer-Encoding" };

    static void transferHeaders(com.zimbra.common.mime.MimePart from, com.zimbra.common.mime.MimePart to) {
        if (from == null || to == null) {
            return;
        }
        for (com.zimbra.common.mime.MimeHeader header : new com.zimbra.common.mime.MimeHeaderBlock(from.getMimeHeaderBlock(), NON_TRANSFERRED)) {
            to.addMimeHeader(header);
        }
    }

    private com.zimbra.common.mime.MimePart replaceInParent(com.zimbra.common.mime.MimePart mp) throws MessagingException {
        if (zpart == mp) {
            return zpart;
        }

        transferHeaders(zpart, mp);
        // cache the parent because removePart() resets it
        JavaMailMimeMultipart oldparent = jmparent;
        if (oldparent != null) {
            int index = oldparent.getBodyPartIndex(this);
            if (index != -1) {
                oldparent.removePart(index);
            } else {
                index = oldparent.getCount();
            }
            this.zpart = mp;
            oldparent.addBodyPart(this, index);
        } else {
            this.zpart = mp;
        }
        this.jmcontent = null;
        return mp;
    }

    static com.zimbra.common.mime.MimePart parsePart(DataSource ds) throws MessagingException {
        com.zimbra.common.mime.ContentType ctype = new com.zimbra.common.mime.ContentType(ds.getContentType());
        com.zimbra.common.mime.MimeHeaderBlock zheaders = new com.zimbra.common.mime.MimeHeaderBlock(ctype);
        com.zimbra.common.mime.MimeParserInputStream mpis = null;
        try {
            mpis = new com.zimbra.common.mime.MimeParserInputStream(ds.getInputStream(), zheaders);
            writeTo(mpis.setSource(ds), null);
            return mpis.getPart();
        } catch (IOException ioe) {
            throw new MessagingException("error parsing body part data source", ioe);
        } finally {
            ByteUtil.closeStream(mpis);
        }
    }

    public void setDataSource(DataSource ds) throws MessagingException {
        replaceInParent(parsePart(ds));
        setHeader("Content-Type", ds.getContentType());
    }

    @Override public void setDataHandler(DataHandler dh) throws MessagingException {
        if (ZPARSER) {
            setDataSource(dh.getDataSource());
        } else {
            super.setDataHandler(dh);
        }
    }

    @Override public void setContent(Object o, String type) throws MessagingException {
        if (ZPARSER) {
            com.zimbra.common.mime.ContentType ctype = new com.zimbra.common.mime.ContentType(type);
            if (o instanceof JavaMailMimeMultipart) {
                setContent((Multipart) o);
                setHeader("Content-Type", type);
            } else if (o instanceof JavaMailMimeMessage) {
                replaceInParent(((JavaMailMimeMessage) o).getZimbraMimeMessage());
                setHeader("Content-Type", type);
                this.jmcontent = o;
            } else if (o instanceof String && ctype.getPrimaryType().equals("text")) {
                setText((String) o, ctype.getParameter("charset"), ctype.getSubType());
                setHeader("Content-Type", type);
            } else {
                setDataHandler(new DataHandler(o, type));
            }
        } else {
            super.setContent(o, type);
        }
    }

    @Override public void setContent(Multipart mp) throws MessagingException {
        if (ZPARSER) {
            if (mp instanceof JavaMailMimeMultipart) {
                replaceInParent(((JavaMailMimeMultipart) mp).getZimbraMimeMultipart());
                this.jmcontent = mp;
            } else {
                // treat it as any other content
                setContent(mp, mp.getContentType());
            }
        } else {
            super.setContent(mp);
        }
    }

    @Override public void setText(String text) throws MessagingException {
        if (ZPARSER) {
            setText(text, null, null);
        } else {
            super.setText(text);
        }
    }

    @Override public void setText(String text, String charset) throws MessagingException {
        if (ZPARSER) {
            setText(text, charset, null);
        } else {
            super.setText(text, charset);
        }
    }

    @Override public void setText(String text, String charset, String subtype) throws MessagingException {
        if (ZPARSER) {
            if (!(zpart instanceof com.zimbra.common.mime.MimeBodyPart)) {
                // need to switch from multipart or message to body part in enclosing part
                replaceInParent(new com.zimbra.common.mime.MimeBodyPart(null));
            } 
            try {
                ((com.zimbra.common.mime.MimeBodyPart) zpart).setText(text, charset, subtype, null);
            } catch (IOException ioe) {
                throw new MessagingException("error encoding text with charset", ioe);
            }
        } else {
            super.setText(text, charset, subtype);
        }
    }

    @Override public void attachFile(File file) throws IOException, MessagingException {
        if (ZPARSER) {
            com.zimbra.common.mime.ContentType ctype = new com.zimbra.common.mime.ContentType("application/octet-stream");
            com.zimbra.common.mime.MimeBodyPart mp = new com.zimbra.common.mime.MimeBodyPart(ctype);
            replaceInParent(mp.setContent(file).setFilename(file.getName()));
        } else {
            super.attachFile(file);
        }
    }

    static void writeTo(InputStream is, OutputStream os) throws IOException {
        byte buffer[] = new byte[8192];
        try {
            while (true) {
                int numRead = is.read(buffer);
                if (numRead < 0) {
                    break;
                } else if (os != null) {
                    os.write(buffer, 0, numRead);
                }
            }
        } finally {
            is.close();
        }
    }

    @Override public void writeTo(OutputStream os) throws IOException, MessagingException  {
        if (ZPARSER) {
            writeTo(zpart.getInputStream(), os);
        } else {
            super.writeTo(os);
        }
    }

    private JavaMailInternetHeaders headerDelegate() {
        return new JavaMailInternetHeaders(zpart.getMimeHeaderBlock(), zpart.getDefaultCharset());
    }

    @Override public String[] getHeader(String name) throws MessagingException {
        if (ZPARSER) {
            return headerDelegate().getHeader(name);
        } else {
            return super.getHeader(name);
        }
    }

    @Override public String getHeader(String name, String delimiter) throws MessagingException {
        if (ZPARSER) {
            return headerDelegate().getHeader(name, delimiter);
        } else {
            return super.getHeader(name, delimiter);
        }
    }

    @Override public void setHeader(String name, String value) throws MessagingException {
        if (ZPARSER) {
            headerDelegate().setHeader(name, value);
            if (name.equalsIgnoreCase("Content-Disposition")) {
                String filename = zpart.getContentDisposition().getParameter("filename");
                if (filename != null) {
                    zpart.setContentType(zpart.getContentType().setParameter("name", filename));
                }
            }
        } else {
            super.setHeader(name, value);
        }
    }

    @Override public void addHeader(String name, String value) throws MessagingException {
        if (ZPARSER) {
            headerDelegate().addHeader(name, value);
        } else {
            super.addHeader(name, value);
        }
    }

    @Override public void removeHeader(String name) throws MessagingException {
        if (ZPARSER) {
            headerDelegate().removeHeader(name);
        } else {
            super.removeHeader(name);
        }
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<Header> getAllHeaders() throws MessagingException {
        if (ZPARSER) {
            return headerDelegate().getAllHeaders();
        } else {
            return super.getAllHeaders();
        }
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<Header> getMatchingHeaders(String[] names) throws MessagingException {
        if (ZPARSER) {
            return headerDelegate().getMatchingHeaders(names);
        } else {
            return super.getMatchingHeaders(names);
        }
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<Header> getNonMatchingHeaders(String[] names) throws MessagingException {
        if (ZPARSER) {
            return headerDelegate().getNonMatchingHeaders(names);
        } else {
            return super.getNonMatchingHeaders(names);
        }
    }

    @Override public void addHeaderLine(String line) throws MessagingException {
        if (ZPARSER) {
            headerDelegate().addHeaderLine(line);
        } else {
            super.addHeaderLine(line);
        }
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<String> getAllHeaderLines() throws MessagingException {
        if (ZPARSER) {
            return headerDelegate().getAllHeaderLines();
        } else {
            return super.getAllHeaderLines();
        }
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<String> getMatchingHeaderLines(String[] names) throws MessagingException {
        if (ZPARSER) {
            return headerDelegate().getMatchingHeaderLines(names);
        } else {
            return super.getMatchingHeaderLines(names);
        }
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<String> getNonMatchingHeaderLines(String[] names) throws MessagingException {
        if (ZPARSER) {
            return headerDelegate().getNonMatchingHeaderLines(names);
        } else {
            return super.getNonMatchingHeaderLines(names);
        }
    }

    @Override protected void updateHeaders() throws MessagingException {
        if (ZPARSER) {
            if (zpart instanceof com.zimbra.common.mime.MimeMultipart) {
                if (jmcontent == null) {
                    jmcontent = new JavaMailMimeMultipart((com.zimbra.common.mime.MimeMultipart) zpart);
                }
                ((JavaMailMimeMultipart) jmcontent).updateHeaders();
            }
        } else {
            super.updateHeaders();
        }
    }

    void setParent(JavaMailMimeMultipart jmmulti) {
        this.jmparent = jmmulti;
    }

    @Override public Multipart getParent() {
        if (ZPARSER) {
            return jmparent;
        } else {
            return super.getParent();
        }
    }

    @Override public String toString() {
        if (ZPARSER) {
            return zpart.toString();
        } else {
            return super.toString();
        }
    }
}
