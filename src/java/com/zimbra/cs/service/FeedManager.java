/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
/*
 * Created on Oct 23, 2005
 */

package com.zimbra.cs.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.httpclient.HttpProxyUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;

public class FeedManager {

    public static final class SubscriptionData<T> {
        public final List<T> items;
        public String lastGuid;
        public long   lastDate = -1;
        public long   feedDate = System.currentTimeMillis();

        SubscriptionData()              { items = new ArrayList<T>(); }
        SubscriptionData(List<T> list)  { items = list; }
        SubscriptionData(long fdate)    { items = new ArrayList<T>();  feedDate = fdate; }

        void recordItem(T item, String guid, long date) {
            items.add(item);
            if (date > lastDate)  { lastGuid = guid;  lastDate = date; }
        }
    }

    public static final int MAX_REDIRECTS = 3;
    public static final String HTTP_USER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322) Zimbra/2.0";
    public static final String HTTP_ACCEPT = "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, " +
    		                             "application/vnd.ms-powerpoint, application/vnd.ms-excel, application/msword, */*";

    public static SubscriptionData<?> retrieveRemoteDatasource(Account acct, String url, Folder.SyncData fsd)
    throws ServiceException {
        HttpClient client = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpProxyUtil.configureProxy(client);

        String originalURL = url;

        // cannot set connection timeout because it'll affect all HttpClients associated with the conn mgr.
        // see comments in ZimbraHttpConnectionManager
        // client.setConnectionTimeout(10000); 

        HttpMethodParams params = new HttpMethodParams();
        params.setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, MimeConstants.P_CHARSET_UTF8);
        params.setSoTimeout(60000);

        GetMethod get = null;
        BufferedInputStream content = null;
        try {
            String expectedCharset = MimeConstants.P_CHARSET_UTF8;
            int redirects = 0;
            do {
                if (url == null || url.equals(""))
                    return new SubscriptionData<Object>();
                String lcurl = url.toLowerCase();
                if (lcurl.startsWith("webcal:"))
                    url = "http:" + url.substring(7);
                else if (lcurl.startsWith("feed:"))
                    url = "http:" + url.substring(5);
                else if (!lcurl.startsWith("http:") && !lcurl.startsWith("https:"))
                    throw ServiceException.INVALID_REQUEST("url must begin with http: or https:", null);

                // username and password are encoded in the URL as http://user:pass@host/...
                if (url.indexOf('@') != -1) {
                    HttpURL httpurl = lcurl.startsWith("https:") ? new HttpsURL(url) : new HttpURL(url);
                    if (httpurl.getUser() != null) {
                        String user = httpurl.getUser();
                        if (user.indexOf('%') != -1) {
                            try {
                                user = URLDecoder.decode(httpurl.getUser());
                            } catch (OutOfMemoryError e) {
                                Zimbra.halt("out of memory", e);
                            } catch (Throwable t) { }
                        }
                        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, httpurl.getPassword());
                        client.getParams().setAuthenticationPreemptive(true);
                        client.getState().setCredentials(AuthScope.ANY, creds);
                    }
                }


                try {
                    get = new GetMethod(url);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("out of memory", e);  return null;
                } catch (Throwable t) {
                    throw ServiceException.INVALID_REQUEST("invalid url for feed: " + url, t);
                }
                get.setParams(params);
                get.setFollowRedirects(true);
                get.setDoAuthentication(true);
                get.addRequestHeader("User-Agent", HTTP_USER_AGENT);
                get.addRequestHeader("Accept", HTTP_ACCEPT);
                HttpClientUtil.executeMethod(client, get);

                Header locationHeader = get.getResponseHeader("location");
                if (locationHeader != null) {
                    // update our target URL and loop again to do another HTTP GET
                    url = locationHeader.getValue();
                    get.releaseConnection();
                } else if (get.getStatusCode() != HttpServletResponse.SC_OK) {
                    throw ServiceException.RESOURCE_UNREACHABLE(get.getStatusLine().toString(), null);
                } else {
                    content = new BufferedInputStream(get.getResponseBodyAsStream());
                    expectedCharset = get.getResponseCharSet();
                    break;
                }
            } while (++redirects <= MAX_REDIRECTS);

            if (redirects > MAX_REDIRECTS)
                throw ServiceException.TOO_MANY_PROXIES(originalURL);

            StringBuilder charset = new StringBuilder(expectedCharset);
            switch (getLeadingChar(content, charset)) {
                case -1:
                    throw ServiceException.PARSE_ERROR("empty body in response when fetching remote subscription", null);
                case '<':
                    return parseRssFeed(Element.parseXML(content), fsd);
                case 'B':  case 'b':
                    List<ZVCalendar> icals = ZCalendarBuilder.buildMulti(content, charset.toString());
                    List<Invite> invites = Invite.createFromCalendar(acct, null, icals, true, true, null);
                    // handle missing UIDs on remote calendars by generating them as needed
                    for (Invite inv : invites) {
                    	if (inv.getUid() == null)
                    	    inv.setUid(LdapUtil.generateUUID());
                    }
                    return new SubscriptionData<Invite>(invites);
                default:
                    throw ServiceException.PARSE_ERROR("unrecognized remote content", null);
            }
        } catch (org.dom4j.DocumentException e) {
            throw ServiceException.PARSE_ERROR("could not parse feed", e);
        } catch (HttpException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("HttpException: " + e, e);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("IOException: " + e, e);
        } finally {
            if (get != null)
                get.releaseConnection();
        }
    }

    private static int getLeadingChar(BufferedInputStream is, StringBuilder charset) throws IOException {
        is.mark(128);
        // check for any BOMs that would override the specified charset
        int ch = is.read();
        switch (ch) {
            case 0xEF:
                if (is.read() == 0xBB && is.read() == 0xBF) {
                    is.mark(128);
                    ch = is.read();  charset.setLength(0);  charset.append("utf-8");
                }
                break;
            case 0xFE:
                if (is.read() == 0xFF && is.read() == 0x00) {
                    ch = is.read();  charset.setLength(0);  charset.append("utf-16");
                }
                break;
            case 0xFF:
                if (is.read() == 0xFE) {
                    ch = is.read();  charset.setLength(0);  charset.append("utf-16");
                }
                break;
        }
        // skip up to 120 bytes of leading whitespace
        for (int index = 0; index < 120 && (ch == '\0' || Character.isWhitespace(ch)); index++)
            ch = is.read();
        // reset to the original state and return the first non-whtespace character
        is.reset();
        return ch;
    }

//    private static org.dom4j.QName QN_CONTENT_ENCODED = org.dom4j.QName.get("encoded", "content", "http://purl.org/rss/1.0/modules/content/");

    private static final class Enclosure {
        private String mUrl, mTitle, mCtype;
        Enclosure(String url, String title, String ctype) {
            mUrl = url;  mTitle = title;  mCtype = ctype;
        }
        String getLocation()     { return mUrl; }
        String getDescription()  { return mTitle; }
        String getContentType() {
            ContentType ctype = new ContentType(mCtype == null ? "text/plain" : mCtype).cleanup();
            try {
                ctype.setParameter("name", FileUtil.trimFilename(URLDecoder.decode(mUrl, "utf-8")));
            } catch (UnsupportedEncodingException e) {
                ctype.setParameter("name", FileUtil.trimFilename(mUrl));
            }
            return ctype.toString();
        }
    }

    private static SubscriptionData<ParsedMessage> parseRssFeed(Element root, Folder.SyncData fsd) throws ServiceException {
        try {
            String rname = root.getName();
            if (rname.equals("feed"))
                return parseAtomFeed(root, fsd);

            Element channel = root.getElement("channel");
            String hrefChannel = channel.getAttribute("link");
            String subjChannel = channel.getAttribute("title");
            InternetAddress addrChannel = new InternetAddress("", subjChannel, "utf-8");
            Date dateChannel = DateUtil.parseRFC2822Date(channel.getAttribute("lastBuildDate", null), new Date());

            List<Enclosure> enclosures = new ArrayList<Enclosure>(3);
            SubscriptionData<ParsedMessage> sdata = new SubscriptionData<ParsedMessage>(dateChannel.getTime());

            if (rname.equals("rss"))
                root = channel;
            else if (!rname.equals("RDF"))
                throw ServiceException.PARSE_ERROR("unknown top-level rss element name: " + root.getQualifiedName(), null);

            for (Element item : root.listElements("item")) {
                // get the item's date
                Date date = DateUtil.parseRFC2822Date(item.getAttribute("pubDate", null), null);
                if (date == null)
                    date = DateUtil.parseISO8601Date(item.getAttribute("date", null), dateChannel);
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
                String guid = item.getAttribute("guid", href);
                // make sure we haven't already seen this item
                if (fsd != null && fsd.alreadySeen(guid == hrefChannel ? null : guid, date == dateChannel ? null : date))
                    continue;
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
                    text = item.getAttribute("abstract", null);
                if (text == null && title != subjChannel)
                    text = "";
                if (text == null)
                    continue;
                html |= text.indexOf("</") != -1 || text.indexOf("/>") != -1 || text.indexOf("<p>") != -1;

                ParsedMessage pm = generateMessage(title, text, href, html, addr, date, enclosures);
                sdata.recordItem(pm, guid, date.getTime());
            }
            return sdata;
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("error encoding rss channel name", e);
        }
    }

    private static SubscriptionData<ParsedMessage> parseAtomFeed(Element feed, Folder.SyncData fsd) throws ServiceException {
        try {
            // get defaults from the <feed> element
            InternetAddress addrFeed = parseAtomAuthor(feed.getOptionalElement("author"), null);
            if (addrFeed == null)
                addrFeed = new InternetAddress("", feed.getAttribute("title"), "utf-8");
            Date dateFeed = DateUtil.parseISO8601Date(feed.getAttribute("updated", null), new Date());
            List<Enclosure> enclosures = new ArrayList<Enclosure>();
            SubscriptionData<ParsedMessage> sdata = new SubscriptionData<ParsedMessage>(dateFeed.getTime());

            for (Element item : feed.listElements("entry")) {
                // get the item's date
                Date date = DateUtil.parseISO8601Date(item.getAttribute("updated", null), null);
                if (date == null)
                    date = DateUtil.parseISO8601Date(item.getAttribute("modified", null), dateFeed);
                // construct an address for the author
                InternetAddress addr = parseAtomAuthor(item.getOptionalElement("author"), addrFeed);
                // get the item's title (may be html or xhtml)
                Element tblock = item.getElement("title");
                String type = tblock.getAttribute("type", "text").trim().toLowerCase();
                String title = tblock.getText();
                if (type.equals("html") || type.equals("xhtml") || type.equals("text/html") || type.equals("application/xhtml+xml"))
                    title = parseTitle(title);
                // find the item's link and any enclosures (associated media links)
                enclosures.clear();
                String href = "";
                for (Element link : item.listElements("link")) {
                    String relation = link.getAttribute("rel", "alternate");
                    if (relation.equals("alternate"))
                        href = link.getAttribute("href");
                    else if (relation.equals("enclosure"))
                        enclosures.add(new Enclosure(link.getAttribute("href", null), link.getAttribute("title", null), link.getAttribute("type", null)));
                }
                String guid = item.getAttribute("id", href);
                // make sure we haven't already seen this item
                if (fsd != null && fsd.alreadySeen(guid == null || guid.equals("") ? null : guid, date == dateFeed ? null : date))
                    continue;
                // get the content/summary and markup
                Element content = item.getOptionalElement("content");
                if (content == null)
                    content = item.getOptionalElement("summary");
                if (content == null)
                    continue;
                type = content.getAttribute("type", "text").trim().toLowerCase();
                boolean html = false;
                if (type.equals("html") || type.equals("xhtml") || type.equals("text/html") || type.equals("application/xhtml+xml"))
                    html = true;
                else if (!type.equals("text") && !type.equals("text/plain"))
                    throw ServiceException.PARSE_ERROR("unsupported atom entry content type: " + type, null);

                ParsedMessage pm = generateMessage(title, content.getText(), href, html, addr, date, enclosures);
                sdata.recordItem(pm, guid, date.getTime());
            }
            return sdata;
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("error encoding atom feed name", e);
        }
    }

    private static final String HTML_HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n" +
                                              "<HTML><HEAD><META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=utf-8\"></HEAD><BODY>";
    private static final String HTML_FOOTER = "</BODY></HTML>";

    private static ParsedMessage generateMessage(String title, String text, String href, boolean html,
                                                 InternetAddress addr, Date date, List<Enclosure> attach)
    throws ServiceException {
        String ctype = html ? "text/html; charset=\"utf-8\"" : "text/plain; charset=\"utf-8\"";
        String content = html ? HTML_HEADER + text + "<p>" + href + HTML_FOOTER : text + "\r\n\r\n" + href;
        // cull out invalid enclosures
        if (attach != null) {
            for (Iterator<Enclosure> it = attach.iterator(); it.hasNext(); )
                if (it.next().getLocation() == null)
                    it.remove();
        }
        boolean hasAttachments = attach != null && !attach.isEmpty();

        // create the MIME message and wrap it
        try {
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
            MimePart body = (hasAttachments ? new MimeBodyPart() : (MimePart) mm);
            body.setText(content, "utf-8");
            body.setHeader("Content-Type", ctype);

            if (hasAttachments) {
                // encode each enclosure as an attachment with Content-Location set
                MimeMultipart mmp = new MimeMultipart("mixed");
                mmp.addBodyPart((BodyPart) body);
                for (Enclosure enc : attach) {
                    MimeBodyPart part = new MimeBodyPart();
                    part.setText("");
                    part.addHeader("Content-Location", enc.getLocation());
                    part.addHeader("Content-Type", enc.getContentType());
                    if (enc.getDescription() != null)
                        part.addHeader("Content-Description", enc.getDescription());
                    part.addHeader("Content-Disposition", "attachment");
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
        if (mailto == 0 && lc.length() <= 7) {
            return addrChannel;
        } else if (mailto == 0) {
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

    private static class UnescapedContent extends org.xml.sax.helpers.DefaultHandler {
        private StringBuffer str = new StringBuffer();
        private boolean continued;

        UnescapedContent()  { }

        @Override public void startDocument() {
            str.setLength(0);  continued = false;
        }
        @Override public void characters(char[] ch, int offset, int length) {
            if (!continued && str.length() > 0)
                str.append(' ');
            str.append(ch, offset, length);
            continued = true;
        }
        @Override public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
            continued = localName.toUpperCase().equals("A");
        }
        @Override public void endElement(String uri, String localName, String qName) {
            continued = localName.toUpperCase().equals("A");
        }
        @Override public String toString() {
            return str.toString();
        }
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
