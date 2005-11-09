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
 * Created on Oct 23, 2005
 */
package com.zimbra.cs.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.ParseMimeMessage;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.Element;

/**
 * @author dkarp
 */
public class FeedManager {

    public static final int MAX_REDIRECTS = 3;
    public static final String HTTP_USER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322) Zimbra/2.0";
    public static final String HTTP_ACCEPT = "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/vnd.ms-powerpoint, application/vnd.ms-excel, application/msword, */*";

    public static List retrieveRemoteDatasource(Account acct, String url) throws ServiceException {
        HttpClient client = new HttpClient();
        client.setConnectionTimeout(10000);
        client.setTimeout(20000);

        InputStream content = null;
        try {
            int redirects = 0;
            do {
                if (url == null || url.equals(""))
                    return Collections.EMPTY_LIST;
                String lcurl = url.toLowerCase();
                if (lcurl.startsWith("webcal:") || lcurl.startsWith("feed:"))
                    url = "http:" + url.substring(7);
                else if (!lcurl.startsWith("http:") && !lcurl.startsWith("https:"))
                    throw ServiceException.FAILURE("url must begin with http: or https:", null);

                GetMethod get = new GetMethod(url);
                get.setFollowRedirects(true);
                get.addRequestHeader("User-Agent", HTTP_USER_AGENT);
                get.addRequestHeader("Accept", HTTP_ACCEPT);
                client.executeMethod(get);

                Header locationHeader = get.getResponseHeader("location");
                if (locationHeader == null) {
                    content = new BufferedInputStream(get.getResponseBodyAsStream());
                    break;
                }
                url = locationHeader.getValue();
            } while (++redirects <= MAX_REDIRECTS);

            if (redirects > MAX_REDIRECTS)
                throw ServiceException.TOO_MANY_HOPS();

            content.mark(10);  int ch = content.read();  content.reset();
            if (ch == -1)
                throw ServiceException.FAILURE("empty body in response when fetching remote subscription", null);
            else if (ch == '<') {
                return parseRssFeed(Element.parseXML(content));
            } else if (ch == 'B') {
                Reader reader = new InputStreamReader(content);
                Calendar ical = new CalendarBuilder().build(reader);
                return Invite.createFromICalendar(acct, null, ical, false); 
            } else
                throw ServiceException.PARSE_ERROR("unrecognized remote content", null);
        } catch (org.dom4j.DocumentException e) {
            throw ServiceException.PARSE_ERROR("could not parse feed", e);
        } catch (HttpException e) {
            throw ServiceException.FAILURE("HttpException: " + e, e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException: " + e, e);
        } catch (ParserException e) {
            throw ServiceException.FAILURE("error parsing raw iCalendar data", e);
        }
    }

    private static org.dom4j.QName QN_CONTENT_ENCODED = org.dom4j.QName.get("encoded", "content", "http://purl.org/rss/1.0/modules/content/");

    private static final class Enclosure {
        private String mUrl, mTitle, mCtype;
        Enclosure(String url, String title, String ctype) {
            mUrl = url;  mTitle = title;  mCtype = ctype;
        }
        String getLocation()     { return mUrl; }
        String getDescription()  { return mTitle; }
        String getContentType() {
            ContentType ctype;
            try {
                ctype = new ContentType(mCtype == null ? "text/plain" : mCtype);
            } catch (javax.mail.internet.ParseException e) {
                ctype = new ContentType("text", "plain", null);
            }
            ctype.setParameter("name", ParseMimeMessage.trimFilename(URLDecoder.decode(mUrl)));
            return ctype.toString();
        }
    }

    private static List parseRssFeed(Element root) throws ServiceException {
        try {
            String rname = root.getName();
            if (rname.equals("feed"))
                return parseAtomFeed(root);

            Element channel = root.getElement("channel");
            String hrefChannel = channel.getAttribute("link");
            String subjChannel = channel.getAttribute("title");
            InternetAddress addrChannel = new InternetAddress("", subjChannel, "utf-8");
            Date dateChannel = parseRFC2822Date(channel.getAttribute("lastBuildDate", null), new Date());
            List enclosures = new ArrayList();

            if (rname.equals("rss"))
                root = root.getElement("channel");
            else if (!rname.equals("RDF"))
                throw ServiceException.PARSE_ERROR("unknown top-level rss element name: " + root.getQualifiedName(), null);

            ArrayList pms = new ArrayList();
            for (Iterator it = root.elementIterator("item"); it.hasNext(); ) {
                Element item = (Element) it.next();
                // get the item's date
                Date date = parseRFC2822Date(item.getAttribute("pubDate", null), null);
                if (date == null)
                    date = parseISO8601Date(item.getAttribute("date", null), dateChannel);
                // construct an address for the author
                InternetAddress addr = addrChannel;
                try {
                    addr = new InternetAddress(item.getAttribute("author"));
                } catch (Exception e) {
                    addr = parseDublinCreator(item.getAttribute("creator", null), addr);
                }
                // get the item's title and link, defaulting to the channel attributes
                String title = parseTitle(item.getAttribute("title", subjChannel));
                String href = item.getAttribute("link", hrefChannel);
                // handle the enclosure (associated media link), if any
                enclosures.clear();
                Element enc = item.getOptionalElement("enclosure");
                if (enc != null)
                    enclosures.add(new Enclosure(enc.getAttribute("url", null), null, enc.getAttribute("type", null)));
                // get the feed item's content and guess at its type
                String text = item.getAttribute("encoded", null);
                boolean html = text != null;
                if (text == null)
                    text = item.getAttribute("description", null);
                if (text == null)
                    text = item.getAttribute("abstract", "");
                html |= text.indexOf("</") != -1 || text.indexOf("/>") != -1 || text.indexOf("<p>") != -1;

                pms.add(generateMessage(title, text, href, html, addr, date, enclosures));
            }
            return pms;
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("error encoding rss channel name", e);
        }
    }

    private static List parseAtomFeed(Element feed) throws ServiceException {
        try {
            // get defaults from the <feed> element
            InternetAddress addrFeed = parseAtomAuthor(feed.getOptionalElement("author"), null);
            if (addrFeed == null)
                addrFeed = new InternetAddress("", feed.getAttribute("title"), "utf-8");
            Date dateFeed = parseISO8601Date(feed.getAttribute("updated", null), new Date());
            List enclosures = new ArrayList();

            ArrayList pms = new ArrayList();
            for (Iterator it = feed.elementIterator("entry"); it.hasNext(); ) {
                Element item = (Element) it.next();
                // get the item's date
                Date date = parseISO8601Date(item.getAttribute("updated", null), null);
                if (date == null)
                    date = parseISO8601Date(item.getAttribute("modified", null), dateFeed);
                // construct an address for the author
                InternetAddress addr = parseAtomAuthor(item.getOptionalElement("author"), addrFeed);
                // get the item's title (may be html or xhtml)
                String title = parseTitle(item.getElement("title").getText());
                // find the item's link and any enclosures (associated media links)
                enclosures.clear();
                String href = "";
                for (Iterator itlink = item.elementIterator("link"); itlink.hasNext(); ) {
                    Element link = (Element) itlink.next();
                    String relation = link.getAttribute("rel", "alternate");
                    if (relation.equals("alternate"))
                        href = link.getAttribute("href");
                    else if (relation.equals("enclosure"))
                        enclosures.add(new Enclosure(link.getAttribute("href", null), link.getAttribute("title", null), link.getAttribute("type", null)));
                }
                // get the content/summary and markup
                Element content = item.getOptionalElement("content");
                if (content == null)
                    content = item.getOptionalElement("summary");
                if (content == null)
                    continue;
                String type = content.getAttribute("type", "text").trim().toLowerCase();
                boolean html = false;
                if (type.equals("html") || type.equals("xhtml") || type.equals("text/html") || type.equals("application/xhtml+xml"))
                    html = true;
                else if (!type.equals("text") && !type.equals("text/plain"))
                    throw ServiceException.PARSE_ERROR("unsupported atom entry content type: " + type, null);

                pms.add(generateMessage(title, content.getText(), href, html, addr, date, enclosures));
            }
            return pms;
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("error encoding atom feed name", e);
        }
    }

    private static final String HTML_HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n" +
                                              "<HTML><HEAD><META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=utf-8\"></HEAD><BODY>";
    private static final String HTML_FOOTER = "</BODY></HTML>";

    private static ParsedMessage generateMessage(String title, String text, String href, boolean html, InternetAddress addr, Date date, List attach)
    throws ServiceException {
        String ctype = html ? "text/html; charset=\"utf-8\"" : "text/plain; charset=\"utf-8\"";
        String content = html ? HTML_HEADER + text + "<p>" + href + HTML_FOOTER : text + "\r\n\r\n" + href;
        // cull out invalid enclosures
        if (attach != null)
            for (Iterator it = attach.iterator(); it.hasNext(); )
                if (((Enclosure) it.next()).getLocation() == null)
                    it.remove();
        boolean hasAttachments = attach != null && attach.size() > 0;

        // create the MIME message and wrap it
        try {
            MimeMessage mm = new MimeMessage(JMSession.getSession());
            MimePart body = (hasAttachments ? new MimeBodyPart() : (MimePart) mm);
            body.setText(content, "utf-8");
            body.setHeader("Content-Type", ctype);

            if (hasAttachments) {
                // encode each enclosure as an attachment with Content-Location set
                MimeMultipart mmp = new MimeMultipart("mixed");
                mmp.addBodyPart((BodyPart) body);
                for (Iterator it = attach.iterator(); it.hasNext(); ) {
                    Enclosure enc = (Enclosure) it.next();
                    MimeBodyPart part = new MimeBodyPart();
                    part.setText("");
                    part.addHeader("Content-Location", enc.getLocation());
                    part.addHeader("Content-Type", enc.getContentType());
                    if (enc.getDescription() != null)
                        part.addHeader("Content-Description", enc.getDescription());
                    mmp.addBodyPart(part);
                }
                mm.setContent(mmp);
            }

            mm.setSentDate(date);
            mm.addFrom(new InternetAddress[] {addr});
            mm.setSubject(title, "utf-8");
            // more stuff here!
            mm.saveChanges();
            return new ParsedMessage(mm, date.getTime(), false);
        } catch (MessagingException e) {
            throw ServiceException.PARSE_ERROR("error wrapping feed item in MimeMessage", e);
        }
    }

    private static InternetAddress parseDublinCreator(String creator, InternetAddress addrChannel) {
        if (creator == null || creator.equals(""))
            return addrChannel;

        // check for a mailto: link
        String lc = creator.trim().toLowerCase(), address = "", personal = creator;
        int mailto = lc.indexOf("mailto:");
        if (mailto == 0 && lc.length() <= 7)
            return addrChannel;
        else if (mailto == 0) {
            personal = null;  address = creator = creator.substring(7);
        } else if (mailto != -1) {
            // checking for "...[mailto:...]..." or "...(mailto:...)..."
            char delimit = creator.charAt(mailto - 1), complement = 0;
            if (delimit == '[')       complement = ']';
            else if (delimit == '(')  complement = ')';
            int closing = creator.indexOf(complement, mailto + 7);
            if (closing != -1 && closing != mailto + 7) {
                address = creator.substring(mailto + 7, closing);
                personal = (creator.substring(0, mailto - 1) + creator.substring(closing + 1)).trim();
            }
        }

        try { return new InternetAddress(address, personal, "utf-8"); } catch (UnsupportedEncodingException e) { }
        try { return new InternetAddress("", creator, "utf-8"); }       catch (UnsupportedEncodingException e) { }
        return addrChannel;
    }

    private static InternetAddress parseAtomAuthor(Element author, InternetAddress addrChannel) {
        if (author == null)
            return addrChannel;

        String address  = author.getAttribute("email", "");
        String personal = author.getAttribute("name", "");
        if (personal.equals("") && address.equals(""))
            return addrChannel;

        try { return new InternetAddress(address, personal, "utf-8"); }      catch (UnsupportedEncodingException e) { }
        try { return new InternetAddress("", address + personal, "utf-8"); } catch (UnsupportedEncodingException e) { }
        return addrChannel;
    }

    private static Date parseRFC2822Date(String encoded, Date fallback) {
        if (encoded == null)
            return fallback;
        try {
            return new MailDateFormat().parse(encoded);
        } catch (ParseException e) {
            return fallback;
        }
    }

    private static Date parseISO8601Date(String encoded, Date fallback) {
        if (encoded == null)
            return fallback;

        // normalize format to "2005-10-19T16:25:38-0800"
        int length = encoded.length();
        if (length == 4)
            encoded += "-01-01T00:00:00-0000";
        else if (length == 7)
            encoded += "-01T00:00:00-0000";
        else if (length == 10)
            encoded += "T00:00:00-0000";
        else if (length < 17)
            return fallback;
        else if (encoded.charAt(16) != ':')
            encoded = encoded.substring(0, 16) + ":00" + encoded.substring(16);
        else if (length >= 22 && encoded.charAt(19) == '.')
            encoded = encoded.substring(0, 19) + encoded.substring(21);

        // timezone cleanup: this format understands '-0800', not '-08:00'
        int colon = encoded.lastIndexOf(':');
        if (colon > 19)
            encoded = encoded.substring(0, colon) + encoded.substring(colon + 1);
        // timezone cleanup: this format doesn't understand 'Z' or default timezones
        if (encoded.length() == 19)
            encoded += "-0000";
        else if (encoded.endsWith("Z"))
            encoded = encoded.substring(0, encoded.length() - 1) + "-0000";

        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(encoded);
        } catch (ParseException e) {
            return fallback;
        }
    }

    private static class UnescapedContent extends org.xml.sax.helpers.DefaultHandler {
        private StringBuffer str = new StringBuffer();
        
        public void startDocument() { str.setLength(0); }
        public void characters(char[] ch, int offset, int length) {
            if (str.length() > 0)
                str.append(' ');
            str.append(ch, offset, length);
        }
        public String toString() { return str.toString(); }
    }

    private static final String parseTitle(String title) {
        if (title == null)
            return "";
        else if (title.indexOf('<') == -1 && title.indexOf('&') == -1)
            return title;
        org.xml.sax.XMLReader parser = new org.cyberneko.html.parsers.SAXParser();
        org.xml.sax.ContentHandler handler = new UnescapedContent();
        parser.setContentHandler(handler);
        try {
            parser.parse(new org.xml.sax.InputSource(new StringReader(title)));
            return handler.toString();
        } catch (Exception e) {
            return title;
        }
    }
}
