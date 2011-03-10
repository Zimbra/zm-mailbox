/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.ByteArrayDataSource;
import javax.mail.util.SharedByteArrayInputStream;

import com.zimbra.common.localconfig.LC;

public class JavaMailMimeMessage extends MimeMessage implements JavaMailShim {
    static final boolean ZPARSER = LC.javamail_zparser.booleanValue();

    public static boolean usingZimbraParser() {
        return ZPARSER;
    }

    private com.zimbra.common.mime.MimeMessage zmessage;
    private Object jmcontent; // JavaMailMimeMultipart or JavaMailMimeMessage or null

    public JavaMailMimeMessage(com.zimbra.common.mime.MimeMessage mm) {
        super(Session.getInstance(mm.getProperties() == null ? new Properties() : mm.getProperties()));
        zmessage = mm;
    }

    public JavaMailMimeMessage(Session session) {
        super(session);
        if (ZPARSER) {
            Properties props = session == null ? null : session.getProperties();
            zmessage = new com.zimbra.common.mime.MimeMessage(props);
        }
    }

    public JavaMailMimeMessage(Session session, InputStream is) throws MessagingException {
        super(session);
        if (ZPARSER) {
            if (is instanceof SharedInputStream && !(is instanceof com.zimbra.common.mime.MimePart.InputStreamSource)) {
                is = new JavaMailMimeBodyPart.SharedInputStreamSource((SharedInputStream) is);
            }
            try {
                zmessage = new com.zimbra.common.mime.MimeMessage(is, session == null ? null : session.getProperties());
            } catch (IOException ioe) {
                throw new MessagingException("error instantiating MimeMessage", ioe);
            }
        } else {
            modified = false;
            super.parse(is);
        }
        saved = true;
    }

    public JavaMailMimeMessage(MimeMessage mm) throws MessagingException {
        // get the other message's session if we can, since it's not formally exposed
        super(mm instanceof JavaMailMimeMessage ? ((JavaMailMimeMessage) mm).getSession() : Session.getInstance(new Properties()));
        if (ZPARSER) {
            if (mm instanceof JavaMailMimeMessage) {
                zmessage = new com.zimbra.common.mime.MimeMessage(((JavaMailMimeMessage) mm).getZimbraMimeMessage());
            } else {
                zmessage = new com.zimbra.common.mime.MimeMessage(asByteArray(mm), session.getProperties());
            }
        } else {
            modified = false;
            // copied from superclass; no way to call this indirectly
            try {
                SharedByteArrayInputStream bais = new SharedByteArrayInputStream(asByteArray(mm));
                parse(bais);
                bais.close();
            } catch (IOException ioe) {
                // should never happen, but just in case...
                throw new MessagingException("IOException while copying message", ioe);
            }
        }
        saved = true;
    }

    private static byte[] asByteArray(MimeMessage mm) throws MessagingException {
        try {
            int size = mm.getSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(size > 0 ? size : 32);
            mm.writeTo(baos);
            baos.close();
            return baos.toByteArray();
        } catch (IOException ioe) {
            // should never happen, but just in case...
            throw new MessagingException("IOException while copying message", ioe);
        }
    }

    com.zimbra.common.mime.MimeMessage getZimbraMimeMessage() {
        return zmessage;
    }

    public Session getSession() {
        return session;
    }

    public JavaMailMimeMessage setSession(Session session) {
        this.session = session;
        if (ZPARSER) {
            zmessage.setProperties(session == null ? null : session.getProperties());
        }
        return this;
    }

    public String getProperty(String key) {
        return session == null ? null : session.getProperties().getProperty(key);
    }

    public JavaMailMimeMessage setProperty(String key, String value) {
        Properties props = session == null ? null : session.getProperties();
        if (props != null) {
            if (value != null) {
                props.setProperty(key, value);
            } else {
                props.remove(key);
            }
        }
        if (ZPARSER) {
            Properties zprops = zmessage.getProperties();
            if (zprops == null && value != null) {
                zmessage.setProperties(zprops = new Properties());
            }
            if (zprops != props) {
                if (value != null) {
                    zprops.setProperty(key, value);
                } else if (zprops != null) {
                    zprops.remove(key);
                }
            }
        }
        return this;
    }

    @Override
    protected void parse(InputStream is) throws MessagingException {
        if (ZPARSER) {
            // should only be called from super.MimeMessage() ctors
            throw new UnsupportedOperationException();
        } else {
            super.parse(is);
        }
    }

    private List<com.zimbra.common.mime.InternetAddress> getZimbraAddressList(String name) throws MessagingException {
        return com.zimbra.common.mime.InternetAddress.parseHeader(getHeader(name, ", "));
    }

    private Address[] getAddressList(String name) throws MessagingException {
        return JavaMailInternetAddress.asJavaMailInternetAddresses(getZimbraAddressList(name));
    }

    private void addAddressList(String name, Address[] addresses) throws MessagingException {
        if (addresses != null && addresses.length > 0) {
            List<com.zimbra.common.mime.InternetAddress> existing = getZimbraAddressList(name);
            setAddressList(name, addresses, existing);
        }
    }

    private void setAddress(String name, Address address) {
        com.zimbra.common.mime.InternetAddress iaddr = JavaMailInternetAddress.asZimbraInternetAddress(address);
        zmessage.setAddressHeader(name, iaddr);
    }

    private void setAddressList(String name, Address[] addresses) {
        setAddressList(name, addresses, null);
    }

    private void setAddressList(String name, Address[] addresses, List<com.zimbra.common.mime.InternetAddress> iaddrs) {
        if (addresses != null) {
            if (iaddrs == null) {
                iaddrs = new ArrayList<com.zimbra.common.mime.InternetAddress>(addresses.length);
            }
            for (Address addr : addresses) {
                iaddrs.add(JavaMailInternetAddress.asZimbraInternetAddress(addr));
            }
        }
        zmessage.setAddressHeader(name, iaddrs);
    }

    @Override
    public Address[] getFrom() throws MessagingException {
        if (ZPARSER) {
            Address[] addresses = getAddressList("From");
            return addresses != null ? addresses : getAddressList("Sender");
        } else {
            return super.getFrom();
        }
    }

    @Override
    public void setFrom(Address address) throws MessagingException {
        if (ZPARSER) {
            setAddress("From", address);
        } else {
            super.setFrom(address);
        }
    }

    @Override
    public void setFrom() throws MessagingException {
        if (ZPARSER) {
            setFrom(InternetAddress.getLocalAddress(session));
        } else {
            super.setFrom();
        }
    }

    @Override
    public void addFrom(Address[] addresses) throws MessagingException {
        if (ZPARSER) {
            addAddressList("From", addresses);
        } else {
            super.addFrom(addresses);
        }
    }

    @Override
    public Address getSender() throws MessagingException {
        if (ZPARSER) {
            Address[] senders = getAddressList("Sender");
            return senders == null || senders.length == 0 ? null : senders[0];
        } else {
            return super.getSender();
        }
    }

    @Override
    public void setSender(Address address) throws MessagingException {
        if (ZPARSER) {
            setAddress("Sender", address);
        } else {
            super.setSender(address);
        }
    }

    private static String asZimbraAddressType(Message.RecipientType type) {
        if (type == Message.RecipientType.TO) {
            return "To";
        } else if (type == Message.RecipientType.CC) {
            return "Cc";
        } else if (type == Message.RecipientType.BCC) {
            return "Bcc";
        } else if (type == MimeMessage.RecipientType.NEWSGROUPS) {
            return "Newsgroups";
        } else {
            return null;
        }
    }

    @Override
    public Address[] getRecipients(Message.RecipientType type) throws MessagingException {
        if (ZPARSER) {
            return getAddressList(asZimbraAddressType(type));
        } else {
            return super.getRecipients(type);
        }
    }

    private static List<String> RECIPIENT_HEADERS = Arrays.asList("To", "Cc", "Bcc", "Newsgroups");

    @Override
    public Address[] getAllRecipients() throws MessagingException {
        if (ZPARSER) {
            boolean foundAny = false;
            List<com.zimbra.common.mime.InternetAddress> iaddrs = new ArrayList<com.zimbra.common.mime.InternetAddress>();
            for (String name : RECIPIENT_HEADERS) {
                List<com.zimbra.common.mime.InternetAddress> subaddrs = getZimbraAddressList(name);
                if (subaddrs != null) {
                    iaddrs.addAll(subaddrs);
                    foundAny = true;
                }
            }
            return foundAny ? JavaMailInternetAddress.asJavaMailInternetAddresses(iaddrs) : null;
        } else {
            return super.getAllRecipients();
        }
    }

    @Override
    public void setRecipient(Message.RecipientType type, Address address) throws MessagingException {
        if (ZPARSER) {
            setAddress(asZimbraAddressType(type), address);
        } else {
            super.setRecipient(type, address);
        }
    }

    @Override
    public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        if (ZPARSER) {
            setAddressList(asZimbraAddressType(type), addresses);
        } else {
            super.setRecipients(type, addresses);
        }
    }

    @Override
    public void setRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        if (ZPARSER) {
            String name = asZimbraAddressType(type);
            zmessage.setAddressHeader(name, com.zimbra.common.mime.InternetAddress.parseHeader(addresses));
        } else {
            super.setRecipients(type, addresses);
        }
    }

    @Override
    public void addRecipient(Message.RecipientType type, Address address) throws MessagingException {
        if (ZPARSER) {
            addRecipients(type, new Address[] { address });
        } else {
            super.addRecipient(type, address);
        }
    }

    @Override
    public void addRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        if (ZPARSER) {
            addAddressList(asZimbraAddressType(type), addresses);
        } else {
            super.addRecipients(type, addresses);
        }
    }

    @Override
    public void addRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        if (ZPARSER) {
            String name = asZimbraAddressType(type);
            List<com.zimbra.common.mime.InternetAddress> iaddrs = zmessage.getAddressHeader(name);
            if (iaddrs == null) {
                iaddrs = new ArrayList<com.zimbra.common.mime.InternetAddress>(5);
            }
            iaddrs.addAll(com.zimbra.common.mime.InternetAddress.parseHeader(addresses));
            zmessage.setAddressHeader(name, iaddrs);
        } else {
            super.addRecipients(type, addresses);
        }
    }

    @Override
    public Address[] getReplyTo() throws MessagingException {
        if (ZPARSER) {
            Address[] addresses = getAddressList("Reply-To");
            return addresses != null ? addresses : getAddressList("From");
        } else {
            return super.getReplyTo();
        }
    }

    @Override
    public void setReplyTo(Address[] addresses) throws MessagingException {
        if (ZPARSER) {
            setAddressList("Reply-To", addresses);
        } else {
            super.setReplyTo(addresses);
        }
    }

    @Override
    public String getSubject() throws MessagingException {
        if (ZPARSER) {
            return zmessage.getSubject();
        } else {
            return super.getSubject();
        }
    }

    @Override
    public void setSubject(String subject) throws MessagingException {
        if (ZPARSER) {
            zmessage.setSubject(subject);
        } else {
            super.setSubject(subject);
        }
    }

    @Override
    public void setSubject(String subject, String charset) throws MessagingException {
        if (ZPARSER) {
            zmessage.setSubject(subject, charset);
        } else {
            super.setSubject(subject, charset);
        }
    }

    @Override
    public Date getSentDate() throws MessagingException {
        if (ZPARSER) {
            return zmessage.getSentDate();
        } else {
            return super.getSentDate();
        }
    }

    @Override
    public void setSentDate(Date d) throws MessagingException {
        if (ZPARSER) {
            zmessage.setSentDate(d);
        } else {
            super.setSentDate(d);
        }
    }

    @Override
    public Date getReceivedDate() throws MessagingException {
        if (ZPARSER) {
            return null;
        } else {
            return super.getReceivedDate();
        }
    }

    private JavaMailMimeBodyPart bodyDelegate() {
        return new JavaMailMimeBodyPart(zmessage.getBodyPart());
    }

    @Override
    public int getSize() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getSize();
        } else {
            return super.getSize();
        }
    }

    @Override
    public int getLineCount() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getLineCount();
        } else {
            return super.getLineCount();
        }
    }

    @Override
    public String getContentType() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getContentType().toString();
        } else {
            return super.getContentType();
        }
    }

    @Override
    public boolean isMimeType(String mimeType) throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().isMimeType(mimeType);
        } else {
            return super.isMimeType(mimeType);
        }
    }

    @Override
    public String getDisposition() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getDisposition();
        } else {
            return super.getDisposition();
        }
    }

    @Override
    public void setDisposition(String disposition) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().setDisposition(disposition);
        } else {
            super.setDisposition(disposition);
        }
    }

    @Override
    public String getEncoding() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getEncoding();
        } else {
            return super.getEncoding();
        }
    }

    @Override
    public String getContentID() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getContentID();
        } else {
            return super.getContentID();
        }
    }

    @Override
    public void setContentID(String cid) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().setContentID(cid);
        } else {
            super.setContentID(cid);
        }
    }

    @Override
    public String getContentMD5() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getContentMD5();
        } else {
            return super.getContentMD5();
        }
    }

    @Override
    public void setContentMD5(String md5) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().setContentMD5(md5);
        } else {
            super.setContentMD5(md5);
        }
    }

    @Override
    public String getDescription() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getDescription();
        } else {
            return super.getDescription();
        }
    }

    @Override
    public void setDescription(String description) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().setDescription(description);
        } else {
            super.setDescription(description);
        }
    }

    @Override
    public void setDescription(String description, String charset) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().setDescription(description, charset);
        } else {
            super.setDescription(description, charset);
        }
    }

    @Override
    public String[] getContentLanguage() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getContentLanguage();
        } else {
            return super.getContentLanguage();
        }
    }

    @Override
    public void setContentLanguage(String[] languages) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().setContentLanguage(languages);
        } else {
            super.setContentLanguage(languages);
        }
    }

    @Override
    public String getMessageID() throws MessagingException {
        if (ZPARSER) {
            return zmessage.getHeader("Message-ID");
        } else {
            return super.getMessageID();
        }
    }

    @Override
    public String getFileName() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getFileName();
        } else {
            return super.getFileName();
        }
    }

    @Override
    public void setFileName(String filename) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().setFileName(filename);
        } else {
            super.setFileName(filename);
        }
    }

    /** Returns an InputStream consisting of the entire message, as would
     *  be written in a call to {@link #writeTo(OutputStream)}. */
    public InputStream getMessageStream() throws IOException {
        if (ZPARSER) {
            return zmessage.getRawContentStream();
        } else {
            // warning: this goes via a PipedInputStream and a separate thread
            return new DataHandler(this, "message/rfc822").getInputStream();
        }
    }

    @Override
    public InputStream getInputStream() throws MessagingException, IOException {
        if (ZPARSER) {
            return bodyDelegate().getInputStream();
        } else {
            return super.getInputStream();
        }
    }

    @Override
    protected InputStream getContentStream() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getContentStream();
        } else {
            return super.getContentStream();
        }
    }

    @Override
    public InputStream getRawInputStream() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getRawInputStream();
        } else {
            return super.getRawInputStream();
        }
    }

    @Override
    public synchronized DataHandler getDataHandler() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getDataHandler();
        } else {
            return super.getDataHandler();
        }
    }

    @Override
    public Object getContent() throws IOException, MessagingException {
        if (ZPARSER) {
            com.zimbra.common.mime.MimePart body = zmessage.getBodyPart();
            if (jmcontent != null) {
                return jmcontent;
            } else if (body instanceof com.zimbra.common.mime.MimeMessage) {
                return jmcontent = new JavaMailMimeMessage((com.zimbra.common.mime.MimeMessage) body);
            } else if (body instanceof com.zimbra.common.mime.MimeMultipart) {
                return jmcontent = new JavaMailMimeMultipart((com.zimbra.common.mime.MimeMultipart) body, this);
            } else {
                return getDataHandler().getContent();
            }
        } else {
            return super.getContent();
        }
    }

    private void setDataSource(DataSource ds) throws MessagingException {
        com.zimbra.common.mime.MimePart body = JavaMailMimeBodyPart.parsePart(ds);
        zmessage.setBodyPart(body);
        if (!(body instanceof com.zimbra.common.mime.MimeMultipart) && ds.getName() != null && !ds.getName().trim().isEmpty()) {
            body.setFilename(ds.getName());
        }
        jmcontent = null;
    }

    @Override
    public synchronized void setDataHandler(DataHandler dh) throws MessagingException {
        if (ZPARSER) {
            setDataSource(dh.getDataSource());
        } else {
            super.setDataHandler(dh);
        }
    }

    @Override
    public void setContent(Object o, String type) throws MessagingException {
        if (ZPARSER) {
            com.zimbra.common.mime.ContentType ctype = new com.zimbra.common.mime.ContentType(type);
            if (o instanceof JavaMailMimeMultipart) {
                setContent((Multipart) o);
                setHeader("Content-Type", type);
            } else if (o instanceof JavaMailMimeMessage) {
                zmessage.setBodyPart(((JavaMailMimeMessage) o).getZimbraMimeMessage());
                setHeader("Content-Type", type);
                jmcontent = o;
            } else if (o instanceof String) {
                if (ctype.getPrimaryType().equals("text")) {
                    setText((String) o, ctype.getParameter("charset"), ctype.getSubType());
                    setHeader("Content-Type", type);
                } else {
                    try {
                        setDataSource(new ByteArrayDataSource((String) o, type));
                    } catch (IOException ioe) {
                        throw new MessagingException("error setting string content", ioe);
                    }
                }
            } else {
                setDataHandler(new DataHandler(o, type));
            }
        } else {
            super.setContent(o, type);
        }
    }

    @Override
    public void setContent(Multipart mp) throws MessagingException {
        if (ZPARSER) {
            if (mp instanceof JavaMailMimeMultipart) {
                zmessage.setBodyPart(((JavaMailMimeMultipart) mp).getZimbraMimeMultipart());
                mp.setParent(this);
                jmcontent = mp;
            } else {
                // treat it as any other data source
                setContent(mp, mp.getContentType());
            }
        } else {
            super.setContent(mp);
        }
    }

    @Override
    public void setText(String text) throws MessagingException {
        if (ZPARSER) {
            setText(text, null, null);
        } else {
            super.setText(text);
        }
    }

    @Override
    public void setText(String text, String charset) throws MessagingException {
        if (ZPARSER) {
            setText(text, charset, null);
        } else {
            super.setText(text, charset);
        }
    }

    @Override
    public void setText(String text, String charset, String subtype) throws MessagingException {
        if (ZPARSER) {
            com.zimbra.common.mime.MimePart mp = zmessage.getBodyPart();
            if (!(mp instanceof com.zimbra.common.mime.MimeBodyPart)) {
                zmessage.setBodyPart(mp = new com.zimbra.common.mime.MimeBodyPart(null));
            }
            try {
                ((com.zimbra.common.mime.MimeBodyPart) mp).setText(text, charset, subtype, null);
                jmcontent = null;
            } catch (IOException ioe) {
                throw new MessagingException("error encoding text with charset", ioe);
            }
        } else {
            super.setText(text, charset, subtype);
        }
    }

    @Override
    public Message reply(boolean replyToAll) throws MessagingException {
        // let the superclass handle this oddball case
        return super.reply(replyToAll);
    }

    @Override
    public void writeTo(OutputStream os) throws IOException, MessagingException {
        if (ZPARSER) {
            if (!saved) {
                saveChanges();
            }
            bodyDelegate().writeTo(os);
        } else {
            super.writeTo(os);
        }
    }

    @Override
    public void writeTo(OutputStream os, String[] ignoreList) throws IOException, MessagingException {
        if (ZPARSER) {
            JavaMailMimeBodyPart.writeTo(zmessage.getRawContentStream(ignoreList), os);
        } else {
            super.writeTo(os, ignoreList);
        }
    }

    @Override
    public String[] getHeader(String name) throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getHeader(name);
        } else {
            return super.getHeader(name);
        }
    }

    @Override
    public String getHeader(String name, String delimiter) throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getHeader(name, delimiter);
        } else {
            return super.getHeader(name, delimiter);
        }
    }

    @Override
    public void setHeader(String name, String value) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().setHeader(name, value);
        } else {
            super.setHeader(name, value);
        }
    }

    @Override
    public void addHeader(String name, String value) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().addHeader(name, value);
        } else {
            super.addHeader(name, value);
        }
    }

    @Override
    public void removeHeader(String name) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().removeHeader(name);
        } else {
            super.removeHeader(name);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<Header> getAllHeaders() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getAllHeaders();
        } else {
            return super.getAllHeaders();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<Header> getMatchingHeaders(String[] names) throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getMatchingHeaders(names);
        } else {
            return super.getMatchingHeaders(names);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<Header> getNonMatchingHeaders(String[] names) throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getNonMatchingHeaders(names);
        } else {
            return super.getNonMatchingHeaders(names);
        }
    }

    @Override
    public void addHeaderLine(String line) throws MessagingException {
        if (ZPARSER) {
            bodyDelegate().addHeaderLine(line);
        } else {
            super.addHeaderLine(line);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getAllHeaderLines() throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getAllHeaderLines();
        } else {
            return super.getAllHeaderLines();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getMatchingHeaderLines(String[] names) throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getMatchingHeaderLines(names);
        } else {
            return super.getMatchingHeaderLines(names);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getNonMatchingHeaderLines(String[] names) throws MessagingException {
        if (ZPARSER) {
            return bodyDelegate().getNonMatchingHeaderLines(names);
        } else {
            return super.getNonMatchingHeaderLines(names);
        }
    }

    @Override
    public synchronized Flags getFlags() throws MessagingException {
        // let the superclass handle all the Flag-related stuff
        return super.getFlags();
    }

    @Override
    public synchronized boolean isSet(Flags.Flag flag) throws MessagingException {
        // let the superclass handle all the Flag-related stuff
        return super.isSet(flag);
    }

    @Override
    public synchronized void setFlags(Flags flag, boolean set) throws MessagingException {
        // let the superclass handle all the Flag-related stuff
        super.setFlags(flag, set);
    }

    @Override
    public void saveChanges() throws MessagingException {
        if (ZPARSER) {
            // this is default JavaMail behavior, although not what our library does
            saved = true;
            updateHeaders();
        } else {
            super.saveChanges();
        }
    }

    @Override
    protected void updateMessageID() throws MessagingException {
        // to preserve functionality, use the superclass' method to update the Message-ID header
        super.updateMessageID();
    }

    @Override
    protected void updateHeaders() throws MessagingException {
        if (ZPARSER) {
            // would love to delegate this to the superclass, but that involves a lot of unused internals
            com.zimbra.common.mime.MimePart mp = zmessage.getBodyPart();
            if (mp instanceof com.zimbra.common.mime.MimeMultipart) {
                if (jmcontent == null) {
                    jmcontent = new JavaMailMimeMultipart((com.zimbra.common.mime.MimeMultipart) mp);
                }
                ((JavaMailMimeMultipart) jmcontent).updateHeaders();
            }
            setHeader("MIME-Version", "1.0");
            updateMessageID();
        } else {
            super.updateHeaders();
        }
    }

    @Override
    protected InternetHeaders createInternetHeaders(InputStream is) throws MessagingException {
        return new JavaMailInternetHeaders(is);
    }

    @Override
    protected MimeMessage createMimeMessage(Session s) {
        return new JavaMailMimeMessage(s);
    }

    @Override
    public String toString() {
        if (ZPARSER) {
            return zmessage.toString();
        } else {
            return super.toString();
        }
    }
}
