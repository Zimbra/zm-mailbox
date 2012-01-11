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

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.html.BrowserDefang;
import com.zimbra.cs.html.DefangFactory;
import com.zimbra.cs.html.HtmlEntityMapper;
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

        @Override
        public String getContentType() {
            return "application/zimbra-im-xml";
        }

        private final Element mElt;
        private byte[] mBuf = null;

        @Override
        public InputStream getInputStream() throws IOException {
            synchronized(this) {
                if (mBuf == null) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    OutputStreamWriter wout =
                        new OutputStreamWriter(buf, MimeConstants.P_CHARSET_UTF8);
                    String text = mElt.toXML().asXML();

                    // convert unicode chars to HTML entities.  This is purely to make the "show original"
                    // easier to deal with by keeping the saved chat entrely in the 7-bit ascii space
                    text = HtmlEntityMapper.unicodeToHtmlEntity(text);

                    wout.write(text);
                    wout.flush();
                    mBuf = buf.toByteArray();
                }
            }
            ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
            return in;
        }

        @Override
        public String getName() {
            return "ImXmlPartDataSource";
        }

        @Override
        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }

    }

    private static class HtmlPartDataSource implements DataSource {

        HtmlPartDataSource(String text)  {
            mText = text;
        }

        @Override
        public String getContentType() {
            return "text/html; charset=utf-8";
        }

        private final String mText;
        private byte[] mBuf = null;

        @Override
        public InputStream getInputStream() throws IOException {
            synchronized(this) {
                if (mBuf == null) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    OutputStreamWriter wout =
                        new OutputStreamWriter(buf, MimeConstants.P_CHARSET_UTF8);
                    String text = mText;
                    wout.write(text);
                    wout.flush();
                    mBuf = buf.toByteArray();
                }
            }
            ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
            return in;
        }

        @Override
        public String getName() {
            return "HtmlPartDataSource";
        }

        @Override
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

    private static final String TAB_STR = "&nbsp;&nbsp;&nbsp;&nbsp;";

    static ParsedMessage writeChat(IMChat chat) throws MessagingException, ServiceException {
        MimeMessage mm = new ZMimeMessage(JMSession.getSession());
        MimeMultipart mmp = new ZMimeMultipart("alternative");

        StringBuilder subject = new StringBuilder();
        StringBuilder plainText = new StringBuilder();
        List<Address> addrs = new ArrayList<Address>();
        DateFormat df = new SimpleDateFormat("h:mm a");
        Date highestDate = new Date(0);
        StringBuilder html = new StringBuilder("<html>");
        Integer colorOff = 0; // an index of the # unique users we've seen so far in this im chat
        HashMap<String /*addr*/, String /*colorId*/> colorMap = new HashMap<String, String>();

        for (IMMessage msg : chat.messages()) {
            InternetAddress ia = new JavaMailInternetAddress(msg.getFrom().getAddr());
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

            String msgBodyHtml = "";
            IMMessage.TextPart body = msg.getBody();
            if (body != null) {
                if (body.hasXHTML()) {
                    msgBodyHtml = body.getXHTMLAsString();
                } else {
                    msgBodyHtml = StringUtil.escapeHtml(body.getPlainText());
                }
            }

            // conert embedded unicode entities to their HTML equivalent
            msgBodyHtml = HtmlEntityMapper.unicodeToHtmlEntity(msgBodyHtml);

            // defang it
            try {
                BrowserDefang defanger = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML);
                msgBodyHtml = defanger.defang(msgBodyHtml, true);
            } catch (IOException ex) {
                ZimbraLog.im.warn("Unable to htmldefang text: "+msgBodyHtml);
                msgBodyHtml = "defang_error";
            }

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
        mm.setSubject(subject.substring(0, subjLen), MimeConstants.P_CHARSET_UTF8);

        // sender list
        Address[] addrArray  = new Address[addrs.size()];
        addrs.toArray(addrArray);
        mm.addFrom(addrArray);

        // date
        mm.setSentDate(highestDate);

        // plain text part
        MimeBodyPart textPart = new ZMimeBodyPart();
        mmp.addBodyPart(textPart);
        textPart.setText(plainText.toString(), MimeConstants.P_CHARSET_UTF8);

        // html
        MimeBodyPart htmlPart = new ZMimeBodyPart();
        htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(html.toString())));
        mmp.addBodyPart(htmlPart);

        // xml part
        Element root = new Element.XMLElement("im");
        IMGetChat.chatToXml(chat, root);
        MimeBodyPart xmlPart = new ZMimeBodyPart();
        xmlPart.setDataHandler(new DataHandler(new ImXmlPartDataSource(root)));
        mmp.addBodyPart(xmlPart);

        mm.setContent(mmp);
        mm.saveChanges(); // don't forget to call this, or bad things will happen!

        ParsedMessage pm  = new ParsedMessage(mm, true);

        return pm;
    }
}
