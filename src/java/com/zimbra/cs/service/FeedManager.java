/*
 * Created on Oct 23, 2005
 */
package com.zimbra.cs.service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMessage;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dom4j.QName;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.Element;

/**
 * @author dkarp
 */
public class FeedManager {

    public static final int MAX_REDIRECTS = 3;
    public static final String HTTP_USER_AGENT = "User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)";

    public static List retrieveRemoteDatasource(Account acct, String url) throws ServiceException {
        HttpClient client = new HttpClient();
        client.setConnectionTimeout(10000);
        client.setTimeout(20000);

        String content = null;
        try {
            int redirects = 0;
            do {
                if (url == null || url.equals(""))
                    return Collections.EMPTY_LIST;
                String lcurl = url.toLowerCase();
                if (lcurl.startsWith("webcal:"))
                    url = "http:" + url.substring(7);
                else if (!lcurl.startsWith("http:") && !lcurl.startsWith("https:"))
                    throw ServiceException.FAILURE("url must begin with http: or https:", null);

                GetMethod get = new GetMethod(url);
                get.setFollowRedirects(true);
                get.addRequestHeader("User-Agent", HTTP_USER_AGENT);
                client.executeMethod(get);

                Header locationHeader = get.getResponseHeader("location");
                if (locationHeader == null) {
                    content = get.getResponseBodyAsString();
                    break;
                }
                url = locationHeader.getValue();
            } while (++redirects <= MAX_REDIRECTS);

            if (redirects > MAX_REDIRECTS)
                throw ServiceException.TOO_MANY_HOPS();
            else if (content == null || content.length() == 0)
                throw ServiceException.FAILURE("empty body in response when fetching remote subscription", null);

            if (content.charAt(0) == '<')
                return parseRssFeed(content);
            else if (content.charAt(0) == 'B') {
                Reader reader = new StringReader(content);
                Calendar ical = new CalendarBuilder().build(reader);
                return Invite.createFromICalendar(acct, null, ical, false); 
            } else
                throw ServiceException.PARSE_ERROR("unrecognized remote content :" + content.substring(0, Math.min(100, content.length())), null);
        } catch (HttpException e) {
            throw ServiceException.FAILURE("HttpException: " + e, e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException: " + e, e);
        } catch (ParserException e) {
            throw ServiceException.FAILURE("error parsing raw iCalendar data", e);
        }
    }

    private static final String HTML_HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">\n" +
                                              "<HTML><HEAD><META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\"><HEAD/><BODY>";
    private static final String HTML_FOOTER = "</BODY></HTML>";
    
    private static List parseRssFeed(String content) throws ServiceException {
        try {
            Element root = parseXML(content);
            String rname = root.getName();
            if (rname.equals("feed"))
                return parseAtomFeed(root);

            Element channel = root.getElement("channel");
            String hrefChannel = channel.getAttribute("link");
            String subjChannel = channel.getAttribute("title");
            InternetAddress addrChannel = new InternetAddress("", subjChannel);
            Date dateChannel = parseRFC2822Date(channel.getAttribute("lastBuildDate", null), new Date());

            if (rname.equals("rss"))
                root = root.getElement("channel");
            else if (!rname.equals("RDF"))
                throw ServiceException.PARSE_ERROR("unknown top-level rss element name: " + root.getQualifiedName(), null);

            ArrayList pms = new ArrayList();
            for (Iterator it = root.elementIterator("item"); it.hasNext(); ) {
                Element item = (Element) it.next();
                Date date = parseRFC2822Date(item.getAttribute("pubDate", null), null);
                if (date == null)
                    date = parseISO8601Date(item.getAttribute("date", null), dateChannel);
                InternetAddress addr = addrChannel;
                try {
                    addr = new InternetAddress(item.getAttribute("author"));
                } catch (Exception e) {
                    addr = parseDublinCreator(item.getAttribute("creator", null), addr);
                }
                String href = item.getAttribute("link", hrefChannel);
                String text = item.getAttribute("description", "").trim(), ctype;
                if (text.indexOf("</") != -1 || text.indexOf("/>") != -1) {
                    ctype = "text/html; charset=\"utf-8\"";  text = HTML_HEADER + text + "<p>" + href + HTML_FOOTER;
                } else {
                    ctype = "text/plain; charset=\"utf-8\"";  text += "\r\n\r\n" + href;
                }

                MimeMessage mm = new MimeMessage(JMSession.getSession());
                mm.setSentDate(date);
                mm.addFrom(new InternetAddress[] {addr});
                mm.setSubject(item.getAttribute("title", subjChannel));
                mm.setText(text, "utf-8");
                mm.setHeader("Content-Type", ctype);
                // more stuff here!
                mm.saveChanges();
                pms.add(new ParsedMessage(mm, date.getTime(), false));
            }
            return pms;
        } catch (org.dom4j.DocumentException e) {
            throw ServiceException.PARSE_ERROR("could not parse feed", e);
        } catch (MessagingException e) {
            throw ServiceException.PARSE_ERROR("error wrapping rss item in MimeMessage", e);
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("error encoding rss channel name", e);
        }
    }

    private static List parseAtomFeed(Element feed) throws ServiceException {
        try {
            // get defaults from the <feed> element
            InternetAddress addrFeed = parseAtomAuthor(feed.getOptionalElement("author"), null);
            if (addrFeed == null)
                addrFeed = new InternetAddress("", feed.getAttribute("title"));
            Date dateFeed = parseISO8601Date(feed.getAttribute("updated", null), new Date());

            ArrayList pms = new ArrayList();
            for (Iterator it = feed.elementIterator("entry"); it.hasNext(); ) {
                Element item = (Element) it.next();
                Date date = parseISO8601Date(item.getAttribute("updated", null), null);
                if (date == null)
                    date = parseISO8601Date(item.getAttribute("modified", null), dateFeed);
                InternetAddress addr = parseAtomAuthor(item.getOptionalElement("author"), addrFeed);
                // figure out the url from the mess of link elements
                String href = "";
                for (Iterator itlink = item.elementIterator("link"); itlink.hasNext(); ) {
                    Element link = (Element) itlink.next();
                    if (link.getAttribute("rel", "alternate").equals("alternate")) {
                        href = link.getAttribute("href");  break;
                    }
                }
                String title = item.getAttribute("title");
                // get the content/summary
                Element content = item.getOptionalElement("content");
                if (content == null)
                    content = item.getOptionalElement("summary");
                if (content == null)
                    continue;
                String text, ctype, type = content.getAttribute("type", "text").trim().toLowerCase();
                if (type.equals("text") || type.equals("text/plain")) {
                    ctype = "text/plain; charset=\"utf-8\"";  text = content.getText() + "\r\n\r\n" + href;
                } else if (type.equals("html") || type.equals("xhtml") || type.equals("text/html") || type.equals("application/xhtml+xml")) {
                    ctype = "text/html; charset=\"utf-8\"";  text = HTML_HEADER + content.getText() + "<p>" + href + HTML_FOOTER;
                } else
                    throw ServiceException.PARSE_ERROR("unsupported content type: " + type, null);

                MimeMessage mm = new MimeMessage(JMSession.getSession());
                mm.setSentDate(date);
                mm.addFrom(new InternetAddress[] {addr});
                mm.setSubject(title);
                mm.setText(text, "utf-8");
                mm.setHeader("Content-Type", ctype);
                // more stuff here!
                mm.saveChanges();
                pms.add(new ParsedMessage(mm, date.getTime(), false));
            }
            return pms;
        } catch (MessagingException e) {
            throw ServiceException.PARSE_ERROR("error wrapping atom item in MimeMessage", e);
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("error encoding atom feed name", e);
        }
    }

    private static InternetAddress parseDublinCreator(String creator, InternetAddress addrChannel) {
        if (creator == null || creator.equals(""))
            return addrChannel;

        // check for a mailto: link
        String lc = creator.trim().toLowerCase(), address = "", personal = creator;
        int mailto = lc.indexOf("mailto:");
        if (mailto == -1)
            ;
        else if (mailto == 0 && lc.length() <= 7)
            return addrChannel;
        else if (mailto == 0) {
            personal = null;  address = creator = creator.substring(7);
        } else {
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

        try { return new InternetAddress(address, personal); } catch (UnsupportedEncodingException e) { }
        try { return new InternetAddress("", creator); }       catch (UnsupportedEncodingException e) { }
        return addrChannel;
    }

    private static InternetAddress parseAtomAuthor(Element author, InternetAddress addrChannel) {
        if (author == null)
            return addrChannel;

        String address  = author.getAttribute("email", "");
        String personal = author.getAttribute("name", "");
        if (personal.equals("") && address.equals(""))
            return addrChannel;

        try { return new InternetAddress(address, personal); }      catch (UnsupportedEncodingException e) { }
        try { return new InternetAddress("", address + personal); } catch (UnsupportedEncodingException e) { }
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

    private static final QName XHTML_DIV = QName.get("div", "http://www.w3.org/1999/xhtml");

    private static Element parseXML(String xml) throws org.dom4j.DocumentException {
        return parseXML(org.dom4j.DocumentHelper.parseText(xml).getRootElement());
    }
    private static Element parseXML(org.dom4j.Element d4root) {
        Element elt = Element.XMLElement.mFactory.createElement(d4root.getQName());
        for (Iterator it = d4root.attributeIterator(); it.hasNext(); ) {
            org.dom4j.Attribute d4attr = (org.dom4j.Attribute) it.next();
            elt.addAttribute(d4attr.getQualifiedName(), d4attr.getValue());
        }
        String content = d4root.getText();
        if (content != null && !content.trim().equals(""))
            elt.setText(content);
        for (Iterator it = d4root.elementIterator(); it.hasNext(); ) {
            org.dom4j.Element d4elt = (org.dom4j.Element) it.next();
            if (d4elt.getQName().equals(XHTML_DIV))
                elt.setText(d4elt.asXML());
            else
                elt.addElement(parseXML(d4elt));
        }
        return elt;
    }
}
