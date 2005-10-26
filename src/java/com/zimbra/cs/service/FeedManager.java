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
import javax.mail.internet.MimeMessage;

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
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.Element;

/**
 * @author dkarp
 */
public class FeedManager {

    public static final int MAX_REDIRECTS = 3;

    public synchronized static List retrieveRemoteDatasource(Account acct, String url) throws ServiceException {
        if (url == null || url.equals(""))
            return Collections.EMPTY_LIST;
        String lcurl = url.toLowerCase();
        if (lcurl.startsWith("webcal:"))
            url = "http:" + url.substring(7);
        else if (!lcurl.startsWith("http:") && !lcurl.startsWith("https:"))
            throw ServiceException.FAILURE("url must begin with http: or https:", null);

        HttpClient client = new HttpClient();
        client.setConnectionTimeout(10000);
        client.setTimeout(20000);

        String content = null;
        try {
            int redirects = 0;
            while (redirects < MAX_REDIRECTS) {
                GetMethod get = new GetMethod(url);
                get.setFollowRedirects(true);
                client.executeMethod(get);
                content = get.getResponseBodyAsString();
                Header locationHeader = get.getResponseHeader("location");
                if (locationHeader != null) {
                    url = locationHeader.getValue();
                    content = null;
                    redirects++;
                } else {
                    break;
                }
            }
            
            if (content == null || content.length() == 0)
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
    
    private static List parseRssFeed(String content) throws ServiceException {
        try {
            Element root = Element.parseXML(content);

            String channel = root.getElement("channel").getAttribute("title");
            InternetAddress addrChannel = new InternetAddress("", channel);
            Date now = new Date();

            String rname = root.getName();
            if (rname.equals("rss"))
                root = root.getElement("channel");
            else if (!rname.equals("RDF"))
                throw ServiceException.PARSE_ERROR("unknown top-level rss element name: " + root.getQualifiedName(), null);

            ArrayList pms = new ArrayList();
            for (Iterator it = root.elementIterator("item"); it.hasNext(); ) {
                Element item = (Element) it.next();
                Date date = parseISO8601(item.getAttribute("date", null));
                if (date == null)
                    date = now;
                InternetAddress addr = parseCreator(item.getAttribute("creator", null), addrChannel);
                MimeMessage mm = new MimeMessage(JMSession.getSession());
                mm.setSentDate(date);
                mm.addFrom(new InternetAddress[] {addr});
                mm.setSubject(item.getAttribute("title"));
                mm.setText(item.getAttribute("link") + "\r\n\r\n" + item.getAttribute("description"), "utf-8");
                // more stuff here!
                mm.saveChanges();
                pms.add(new ParsedMessage(mm, date.getTime(), false));
            }
            return pms;
        } catch (org.dom4j.DocumentException e) {
            throw ServiceException.PARSE_ERROR("could not parse rss feed", e);
        } catch (MessagingException e) {
            throw ServiceException.PARSE_ERROR("error wrapping rss item in MimeMessage", e);
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("error encoding rss channel name", e);
        }
    }

    private static InternetAddress parseCreator(String encoded, InternetAddress addrChannel) {
        if (encoded == null || encoded.equals(""))
            return addrChannel;

        // check for a mailto: link
        String lc = encoded.trim().toLowerCase(), address = "", personal = encoded;
        int mailto = lc.indexOf("mailto:");
        if (mailto == -1)
            ;
        else if (mailto == 0 && lc.length() <= 7)
            return addrChannel;
        else if (mailto == 0) {
            personal = null;  address = encoded = encoded.substring(7);
        } else {
            // checking for "...[mailto:...]..." or "...(mailto:...)..."
            char delimit = encoded.charAt(mailto - 1), complement = 0;
            if (delimit == '[')       complement = ']';
            else if (delimit == '(')  complement = ')';
            int closing = encoded.indexOf(complement, mailto + 7);
            if (closing != -1 && closing != mailto + 7) {
                address = encoded.substring(mailto + 7, closing);
                personal = (encoded.substring(0, mailto - 1) + encoded.substring(closing + 1)).trim();
            }
        }

        try { return new InternetAddress(address, personal); } catch (UnsupportedEncodingException e) { }
        try { return new InternetAddress("", encoded); }       catch (UnsupportedEncodingException e) { }
        return addrChannel;
    }

    private static Date parseISO8601(String encoded) {
        if (encoded == null)
            return null;

        // normalize format to "2005-10-19T16:25:38-0800"
        int length = encoded.length();
        if (length == 4)
            encoded += "-01-01T00:00:00-0000";
        else if (length == 7)
            encoded += "-01T00:00:00-0000";
        else if (length == 10)
            encoded += "T00:00:00-0000";
        else if (length < 17)
            return null;
        else if (encoded.charAt(16) != ':')
            encoded = encoded.substring(0, 16) + ":00" + encoded.substring(16);
        else if (length >= 22 && encoded.charAt(19) == '.')
            encoded = encoded.substring(0, 19) + encoded.substring(21);
        // remove the ':' from the timezone, if present
        int colon = encoded.lastIndexOf(':');
        if (colon > 19)
            encoded = encoded.substring(0, colon) + encoded.substring(colon + 1);
        // this format doesn't understand 'Z' as a timezone
        if (encoded.endsWith("Z"))
            encoded = encoded.substring(0, encoded.length() - 1) + "-0000";

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            return sdf.parse(encoded);
        } catch (ParseException e) {
            return null;
        }
    }
}
