/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.im;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.soap.Element;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.im.IMGetChat;
import com.zimbra.cs.util.JMSession;

/**
 * Hacky temporary class to convert a Chat into a MimeMessage for saving
 *
 */
class ChatWriter {

    private static class ImXmlPartDataSource implements DataSource {

        ImXmlPartDataSource(Element elt)  {
            mElt = elt;
        }

        public String getContentType() {
            return "application/zimbra-im-xml";
        }

        private Element mElt;
        private byte[] mBuf = null;

        public InputStream getInputStream() throws IOException {
            synchronized(this) {
                if (mBuf == null) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    OutputStreamWriter wout =
                        new OutputStreamWriter(buf, Mime.P_CHARSET_UTF8);
                    String text = mElt.toXML().asXML(); 
                    wout.write(text);
                    wout.flush();
                    mBuf = buf.toByteArray();
                }
            }
            ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
            return in;
        }

        public String getName() {
            return "ImXmlPartDataSource";
        }

        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }

    }

    private static class HtmlPartDataSource implements DataSource {

        HtmlPartDataSource(String text)  {
            mText = text;
        }

        public String getContentType() {
            return "text/html; charset=utf-8";
        }

        private String mText;
        private byte[] mBuf = null;

        public InputStream getInputStream() throws IOException {
            synchronized(this) {
                if (mBuf == null) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    OutputStreamWriter wout =
                        new OutputStreamWriter(buf, Mime.P_CHARSET_UTF8);
                    String text = mText;
                    wout.write(text);
                    wout.flush();
                    mBuf = buf.toByteArray();
                }
            }
            ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
            return in;
        }

        public String getName() {
            return "HtmlPartDataSource";
        }

        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }

    }

    private static final String sColors[] = new String[] {
        "#0000FF",
        "#FF0000",
        "#00FF00",
        "#FF00FF",
    };

    static ParsedMessage writeChat(IMChat chat) throws MessagingException {
        MimeMessage mm = new MimeMessage(JMSession.getSession());
        MimeMultipart mmp = new MimeMultipart("alternative");
        mm.setContent(mmp);

        StringBuilder subject = new StringBuilder();
        StringBuilder plainText = new StringBuilder();
        List<Address> addrs = new ArrayList<Address>();
        DateFormat df = new SimpleDateFormat("h:mm a");
        Date highestDate = new Date(0);
        StringBuilder html = new StringBuilder("<html>");
        Integer colorOff = 0; // an index of the # unique users we've seen so far in this im chat
        HashMap<String /*addr*/, String /*colorId*/> colorMap = new HashMap<String, String>();

        for (IMMessage msg : chat.messages()) {
            InternetAddress ia = new InternetAddress(msg.getFrom().getAddr());
            if (!addrs.contains(ia))
                addrs.add(ia);

            String from = msg.getFrom() != null ? msg.getFrom().toString() : "";

            String msgBody = msg.getBody() !=  null ? msg.getBody().getPlainText() : "";

            // strip off a trailing newline, for presentation's sake
            if (msgBody.length() > 0 && msgBody.charAt(msgBody.length()-1) == '\n')
                msgBody = msgBody.substring(0, msgBody.length()-1);

            // append the first few messages into the Subject of the transcript
            if (subject.length() < 40)
                subject.append(msgBody).append("   ");

            plainText.append(new Formatter().format("%s[%s]: %s\n", from, df.format(msg.getDate()), msgBody));

            // date tracking: find the date of the latest message in the conv
            if (msg.getDate().after(highestDate))
                highestDate = msg.getDate();

            String msgBodyHtml = msg.getBody() != null ? msg.getBody().toString() : "";

            // find the color for this user
            if (!colorMap.containsKey(from)) {
                if (colorOff == -1)
                    colorMap.put(from, "#000000");
                else
                    colorMap.put(from, sColors[colorOff++]);

                if (colorOff >= sColors.length)
                    colorOff = -1;
            }
            String colorId = colorMap.get(from);

            html.append(new Formatter().format("<font color=\"%s\"><b>%s</b><i>[%s]</i>: %s</font><br>\n", 
                        colorId, msg.getFrom().toString(), df.format(msg.getDate()), msgBodyHtml));
        }
        html.append("</html>");

        // subject
        int subjLen = Math.min(40, subject.length());
        mm.setSubject(subject.substring(0, subjLen), Mime.P_CHARSET_UTF8);

        // sender list
        Address[] addrArray  = new Address[addrs.size()];
        addrs.toArray(addrArray);
        mm.addFrom(addrArray);

        // date
        mm.setSentDate(highestDate);

        // plain text part
        MimeBodyPart textPart = new MimeBodyPart();
        mmp.addBodyPart(textPart);
        textPart.setText(plainText.toString(), Mime.P_CHARSET_UTF8);

        // html
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(html.toString())));
        mmp.addBodyPart(htmlPart);
        
        // xml part
        Element root = new Element.XMLElement("im");
        IMGetChat.chatToXml(chat, root);
        MimeBodyPart xmlPart = new MimeBodyPart();
        mmp.addBodyPart(xmlPart);
        xmlPart.setDataHandler(new DataHandler(new ImXmlPartDataSource(root)));

        mm.saveChanges(); // don't forget to call this, or bad things will happen!

        ParsedMessage pm  = new ParsedMessage(mm, true);

        return pm; 
    }
}
