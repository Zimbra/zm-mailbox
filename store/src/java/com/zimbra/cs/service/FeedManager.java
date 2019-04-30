/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
/*
 * Created on Oct 23, 2005
 */

package com.zimbra.cs.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.ParseException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.io.Closeables;
import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.httpclient.HttpProxyUtil;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;

public class FeedManager {

    public static final class SubscriptionData<T> {
        private final List<T> items;
        private String lastGuid;
        private long   lastDate;
        private boolean notModified;

        static SubscriptionData<Object> NOT_MODIFIED() {
            return new SubscriptionData<Object>(new ArrayList<Object>(0), 0, true);
        }

        SubscriptionData() {
            this(new ArrayList<T>(), 0);
        }

        SubscriptionData(List<T> items, long ldate)  {
            this(items, ldate, false);
        }

        SubscriptionData(List<T> items, long lastModifiedDate, boolean notModified)  {
            this.items = items;
            this.lastDate = lastModifiedDate;
            this.notModified = notModified;
        }

        void recordItem(T item, String guid, long date) {
            items.add(item);
            if (date > lastDate) {
                lastGuid = guid;  lastDate = date;
            }
        }

        void recordFeedModifiedDate(long feedModified) {
            if (feedModified > lastDate) {
                lastDate = feedModified;
            }
        }

        public List<T> getItems() {
            return items;
        }

        // returns the guid of the most recently modified item
        public String getMostRecentGuid() {
            return lastGuid;
        }

        // returns the timestamp of the most recently modified item, or the last modified time of the feed itself,
        // whichever is more recent
        public long getLastModifiedDate() {
            return lastDate;
        }

        // returns true if the feed has no change since the last sync (HTTP 304 Not Modified response)
        public boolean isNotModified() {
            return notModified;
        }
    }

    private static String getBrowserTag() {
        String tag = " Zimbra/" + BuildInfo.MAJORVERSION + "." + BuildInfo.MINORVERSION + "." + BuildInfo.MICROVERSION;
        return tag.indexOf("unknown") == -1 ? tag : " Zimbra/8.0";
    }

    public static final int MAX_REDIRECTS = 3;

    public static final String BROWSER_TAG = getBrowserTag();
    public static final String HTTP_USER_AGENT = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.1.7)" + BROWSER_TAG;
    public static final String HTTP_ACCEPT = "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, " +
                                             "application/vnd.ms-powerpoint, application/vnd.ms-excel, application/msword, */*";

    public static class RemoteDataInfo {
        public final int statusCode;
        public final int redirects;
        public BufferedInputStream content;
        public final String expectedCharset;
        public final long lastModified;
        private HttpGet getMethod = null;

        public RemoteDataInfo(int statusCode, int redirects,
                BufferedInputStream content, String expectedCharset, long lastModified) {
            this.statusCode = statusCode;
            this.redirects = redirects;
            this.content = content;
            this.expectedCharset = expectedCharset;
            this.lastModified = lastModified;
        }
        public HttpGet getGetMethod() {
            return getMethod;
        }
        public void setGetMethod(HttpGet getMethod) {
            this.getMethod = getMethod;
        }
        public void cleanup() {
            Closeables.closeQuietly(content);
            content = null;
            if (getMethod != null) {
                getMethod.releaseConnection();
                getMethod = null;
            }
        }
    }

    /**
     * Determines if a target address falls within the specified subnet.<br>
     * If the prefix has no bit-length, determines direct match with target address.
     * @param targetAddress The address in question
     * @param prefix CIDR notation (first ip and number of relevant bits), or single ip - no wildcards
     * @return True if the address matches or is within subnet range
     */
    protected static boolean isAddressInRange(InetAddress targetAddress, String prefix) {
        ZimbraLog.misc.debug("checking if ip: %s is in range of: %s", targetAddress, prefix);
        // split first ip from bit length
        String [] firstIpAndLength = prefix.split("/");
        // the first ip in the subnet
        InetAddress firstIp;
        // the number of relevant bits in the entire address
        int bitLength;
        try {
            firstIp = InetAddress.getByName(firstIpAndLength[0]);
            // compare direct if no bit length
            if (firstIpAndLength.length < 2) {
                return targetAddress.getHostAddress().equals(firstIp.getHostAddress());
            }
            bitLength = Integer.parseInt(firstIpAndLength[1]);
        } catch (UnknownHostException | NumberFormatException e) {
            ZimbraLog.misc.error("ignoring unparsable ip address prefix: %s", prefix);
            ZimbraLog.misc.debug(e);
            return false;
        }

        // don't compare across ipv4 vs ipv6
        if (!targetAddress.getClass().equals(firstIp.getClass())) {
            ZimbraLog.misc.debug("cannot compare across ipv4 and ipv6 address. target: %s, first ip: %s",
                targetAddress, firstIp);
            return false;
        }

        // determine number of relevant bytes to compare
        // e.g. /116 -> 116/8=14.5 -> 14 -> remaining bits handled below
        // e.g. /30 -> 30/8=3.75 -> 4 -> remaining bits handled below
        // e.g. /24 -> 24/8=3 -> 3
        int maskLength = bitLength / Byte.SIZE;

        // mask on and compare #maskLength bytes we care about
        byte mask = (byte) 0xFF;
        byte [] targetBytes = targetAddress.getAddress();
        byte [] subBytes = firstIp.getAddress();
        for (int i = 0; i < maskLength; i++) {
            if ((targetBytes[i] & mask) != (subBytes[i] & mask)) {
                return false;
            }
        }

        // the number of relevant bits in the last byte of the address
        int doCareLength = bitLength % Byte.SIZE;

        // last byte is only relevant for non-multiples of 8
        // last byte has all bits on except the don't cares specified by bit length
        // e.g. /30 -> 30%8=6 -> 8-6=2 -> last 2 bits are off
        // e.g. /29 -> 29%8=5 -> 8-5=3 -> last 3 bits are off
        if (doCareLength != 0) {
            // set on all bits
            byte lastByteMask = (byte) 0xFF;
            // set off the lowest bits remaining from a full byte
            int dontCareLength = Byte.SIZE - doCareLength;
            lastByteMask <<= dontCareLength;
            return (targetBytes[maskLength] & lastByteMask) == (subBytes[maskLength] & lastByteMask);
        }

        return true;
    }

    /**
     * Returns true if target address is link-local, loopback, or blacklisted.
     * @param url The target
     * @return True if address is blocked for feed manager
     */
    protected static boolean isBlockedFeedAddress(URIBuilder url) {
        String blacklistString = LC.zimbra_feed_manager_blacklist.value();
        List<String> blacklist = new ArrayList<String>();
        if (!StringUtil.isNullOrEmpty(blacklistString)) {
            blacklist.addAll(Arrays.asList(blacklistString.split(",")));
        }
        try {
            InetAddress targetAddress = InetAddress.getByName(url.getHost());
            return targetAddress.isAnyLocalAddress()
                || targetAddress.isLinkLocalAddress()
                || targetAddress.isLoopbackAddress()
                || blacklist.stream()
                    .anyMatch(ip -> isAddressInRange(targetAddress, ip));
        } catch (UnknownHostException e) {
            ZimbraLog.misc.warn("unable to identify feed manager target url host: %s", url);
        }
        return false;
    }

    private static RemoteDataInfo retrieveRemoteData(String url, Folder.SyncData fsd)
    throws ServiceException, HttpException, IOException {
        assert !Strings.isNullOrEmpty(url);

        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpProxyUtil.configureProxy(clientBuilder);

        // cannot set connection timeout because it'll affect all HttpClients associated with the conn mgr.
        // see comments in ZimbraHttpConnectionManager
        // client.setConnectionTimeout(10000);

        SocketConfig config = SocketConfig.custom().setSoTimeout(60000).build();
        clientBuilder.setDefaultSocketConfig(config);

        ConnectionConfig connConfig = ConnectionConfig.custom().setCharset(Charset.forName(MimeConstants.P_CHARSET_UTF8)).build();
        clientBuilder.setDefaultConnectionConfig(connConfig);

        HttpGet get = null;
        BufferedInputStream content = null;
        long lastModified = 0;
        String expectedCharset = MimeConstants.P_CHARSET_UTF8;
        int redirects = 0;
        int statusCode = HttpServletResponse.SC_NOT_FOUND;
        try {
            do {
                String lcurl = url.toLowerCase();
                if (lcurl.startsWith("webcal:")) {
                    url = "http:" + url.substring(7);
                } else if (lcurl.startsWith("feed:")) {
                    url = "http:" + url.substring(5);
                } else if (!lcurl.startsWith("http:") && !lcurl.startsWith("https:")) {
                    throw ServiceException.INVALID_REQUEST("url must begin with http: or https:", null);
                }

                // Add AuthCache to the execution context
                HttpClientContext context = HttpClientContext.create();
                URIBuilder httpurl;
                try {
                    httpurl = new  URIBuilder(url);
                } catch (URISyntaxException e1) {
                    throw ServiceException.INVALID_REQUEST("invalid url for feed: " + url, e1);
                }

                // validate target address (also handles followed `location` header addresses)
                if (isBlockedFeedAddress(httpurl)) {
                    throw ServiceException.INVALID_REQUEST(String.format("invalid url for feed: %s", url), null);
                }
                // username and password are encoded in the URL as http://user:pass@host/...
                if (url.indexOf('@') != -1) {
                    if (httpurl.getUserInfo() != null) {
                        String user = httpurl.getUserInfo();
                        if (user.indexOf('%') != -1) {
                            try {
                                user = URLDecoder.decode(user, "UTF-8");
                            } catch (OutOfMemoryError e) {
                                Zimbra.halt("out of memory", e);
                            } catch (Throwable t) { }
                        }
                        int index = user.indexOf(':');
                        String userName = user.substring(0,  index);
                        String password = user.substring(index + 1);


                        CredentialsProvider provider = new BasicCredentialsProvider();
                        UsernamePasswordCredentials credentials
                         = new UsernamePasswordCredentials(userName, password);
                        provider.setCredentials(AuthScope.ANY, credentials);
                        clientBuilder.setDefaultCredentialsProvider(provider);
                        clientBuilder.setDefaultCredentialsProvider(provider);

                       // Create AuthCache instance
                        AuthCache authCache = new BasicAuthCache();
                        // Generate BASIC scheme object and add it to the local auth cache
                        BasicScheme basicAuth = new BasicScheme();
                        authCache.put(new HttpHost(httpurl.getHost()), basicAuth);

                        // Add AuthCache to the execution context
                        context.setCredentialsProvider(provider);
                        context.setAuthCache(authCache);

                    }
                }

                try {
                    get = new HttpGet(url);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("out of memory", e);  return null;
                } catch (Throwable t) {
                    ZimbraLog.misc.warnQuietly(String.format("invalid url for feed: %s", url), t);
                    throw ServiceException.INVALID_REQUEST("invalid url for feed: " + url, null);
                }

                DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
                clientBuilder.setRedirectStrategy(redirectStrategy);

                get.addHeader("User-Agent", HTTP_USER_AGENT);
                get.addHeader("Accept", HTTP_ACCEPT);
                if (fsd != null && fsd.getLastSyncDate() > 0) {
                    String lastSyncAt = DateUtils.formatDate(new Date(fsd.getLastSyncDate()));
                    get.addHeader("If-Modified-Since", lastSyncAt);
                }

                HttpClient client = clientBuilder.build();
                HttpResponse response = HttpClientUtil.executeMethod(client, get, context);

                Header locationHeader = response.getFirstHeader("location");
                if (locationHeader != null) {
                    // update our target URL and loop again to do another HTTP GET
                    url = locationHeader.getValue();
                    get.releaseConnection();
                } else {
                    statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpServletResponse.SC_OK) {
                        Header contentEncoding = response.getFirstHeader("Content-Encoding");
                        InputStream respInputStream = response.getEntity().getContent();
                        if (contentEncoding != null) {
                            if (contentEncoding.getValue().indexOf("gzip") != -1) {
                                respInputStream = new GZIPInputStream(respInputStream);
                            }
                        }
                        content = new BufferedInputStream(respInputStream);
                        org.apache.http.entity.ContentType contentType =  org.apache.http.entity.ContentType.getOrDefault(response.getEntity());
                        if (contentType != null && contentType.getCharset() != null) {
                            expectedCharset = contentType.getCharset().name();
                        }

                        Header lastModHdr = response.getFirstHeader("Last-Modified");
                        if (lastModHdr == null) {
                            lastModHdr = response.getFirstHeader("Date");
                        }
                        if (lastModHdr != null) {
                            Date d = DateUtils.parseDate(lastModHdr.getValue());
                            lastModified = d.getTime();
                        } else {
                            lastModified = System.currentTimeMillis();
                        }
                    } else if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
                        ZimbraLog.misc.debug("Remote data at " + url + " not modified since last sync");
                        return new RemoteDataInfo(statusCode, redirects, null, expectedCharset, lastModified);
                    } else {
                        throw ServiceException.RESOURCE_UNREACHABLE(response.getStatusLine().toString(), null);
                    }
                    break;
                }
            } while (++redirects <= MAX_REDIRECTS);
        } catch (ServiceException ex) {
            if (get != null) {
                get.releaseConnection();
            }
            throw ex;
        } catch (HttpException ex) {
            if (get != null) {
                get.releaseConnection();
            }
            throw ex;
        } catch (IOException ex) {
            if (get != null) {
                get.releaseConnection();
            }
            throw ex;
        }
        RemoteDataInfo rdi = new RemoteDataInfo(statusCode, redirects, content, expectedCharset, lastModified);
        rdi.setGetMethod(get);
        return rdi;
    }

    @VisibleForTesting
    protected static SubscriptionData<?> retrieveRemoteDatasource(Account acct, RemoteDataInfo rdi, Folder.SyncData fsd)
    throws ServiceException, IOException {
        StringBuilder charset = new StringBuilder(rdi.expectedCharset);
        switch (getLeadingChar(rdi.content, charset)) {
            case -1:
                throw ServiceException.PARSE_ERROR("empty body in response when fetching remote subscription", null);
            case '<':
                return parseRssFeed(Element.parseXML(rdi.content), fsd, rdi.lastModified);
            case 'B':  case 'b':
                List<ZVCalendar> icals = ZCalendarBuilder.buildMulti(rdi.content, charset.toString());
                List<Invite> invites = Invite.createFromCalendar(acct, null, icals, true, true, null);
                // handle missing UIDs on remote calendars by generating them as needed
                for (Invite inv : invites) {
                    if (inv.getUid() == null) {
                        inv.setUid(LdapUtil.generateUUID());
                    }
                }
                return new SubscriptionData<Invite>(invites, rdi.lastModified);
            default:
                throw ServiceException.PARSE_ERROR("unrecognized remote content", null);
        }
    }

    public static SubscriptionData<?> retrieveRemoteDatasource(Account acct, String url, Folder.SyncData fsd)
    throws ServiceException {
        assert !Strings.isNullOrEmpty(url);
        RemoteDataInfo rdi = null;
        try {
            rdi = retrieveRemoteData(url, fsd);

            if (rdi.statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
                return SubscriptionData.NOT_MODIFIED();
            }
            if (rdi.redirects > MAX_REDIRECTS) {
                throw ServiceException.TOO_MANY_PROXIES(url);
            }
            return retrieveRemoteDatasource(acct, rdi, fsd);
        } catch (HttpException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("HttpException: " + e, e);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("IOException: " + e, e);
        } finally {
            if (rdi != null) {
                rdi.cleanup();
            }
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
        private final String url, title, cthdr;

        Enclosure(String url, String title, String ctype) {
            this.url = url;
            this.title = title;
            this.cthdr = ctype;
        }

        String getLocation() {
            return url;
        }

        String getDescription() {
            return title;
        }

        String getContentType() {
            ContentType ctype = new ContentType(cthdr == null ? "text/plain" : cthdr).cleanup();
            try {
                ctype.setParameter("name", FileUtil.trimFilename(URLDecoder.decode(url, "utf-8")));
            } catch (UnsupportedEncodingException e) {
                ctype.setParameter("name", FileUtil.trimFilename(url));
            }
            return ctype.toString();
        }
    }

    private static SubscriptionData<ParsedMessage> parseRssFeed(Element root, Folder.SyncData fsd, long lastModified)
    throws ServiceException {
        try {
            String rname = root.getName();
            if (rname.equals("feed")) {
                return parseAtomFeed(root, fsd, lastModified);
            }

            Element channel = root.getElement("channel");
            String hrefChannel = channel.getAttribute("link");
            String subjChannel = channel.getAttribute("title");
            InternetAddress addrChannel = new JavaMailInternetAddress("", subjChannel, "utf-8");
            Date dateChannel = DateUtil.parseRFC2822Date(channel.getAttribute("lastBuildDate", null), new Date());

            List<Enclosure> enclosures = new ArrayList<Enclosure>(3);
            SubscriptionData<ParsedMessage> sdata = new SubscriptionData<ParsedMessage>();

            if (rname.equals("rss")) {
                root = channel;
            } else if (!rname.equals("RDF")) {
                throw ServiceException.PARSE_ERROR("unknown top-level rss element name: " + root.getQualifiedName(), null);
            }

            for (Element item : root.listElements("item")) {
                // get the item's date
                Date date = DateUtil.parseRFC2822Date(item.getAttribute("pubDate", null), null);
                if (date == null) {
                    date = DateUtil.parseISO8601Date(item.getAttribute("date", null), dateChannel);
                }

                // construct an address for the author
                InternetAddress addr = addrChannel;
                try {
                    addr = parseAuthor(item.getAttribute("author"));
                } catch (Exception e) {
                    addr = parseDublinCreator(stripXML(item.getAttribute("creator", null)), addr);
                }

                // get the item's title and link, defaulting to the channel attributes
                String title = stripXML(item.getAttribute("title", subjChannel));
                String href = item.getAttribute("link", hrefChannel);
                String guid = item.getAttribute("guid", href);

                // make sure we haven't already seen this item
                if (fsd != null && fsd.alreadySeen(guid == hrefChannel ? null : guid, date == dateChannel ? null : date))
                    continue;

                // handle the enclosure (associated media link), if any
                enclosures.clear();
                Element enc = item.getOptionalElement("enclosure");
                if (enc != null) {
                    enclosures.add(new Enclosure(enc.getAttribute("url", null), null, enc.getAttribute("type", null)));
                }

                // get the feed item's content and guess at its type
                String text = item.getAttribute("encoded", null);
                boolean html = text != null;
                if (text == null) {
                    text = item.getAttribute("description", null);
                }
                if (text == null) {
                    text = item.getAttribute("abstract", null);
                }
                if (text == null && title != subjChannel) {
                    text = "";
                }
                if (text == null)
                    continue;
                html |= text.indexOf("</") != -1 || text.indexOf("/>") != -1 || text.indexOf("<p>") != -1;

                ParsedMessage pm = generateMessage(title, text, href, html, addr, date, enclosures);
                sdata.recordItem(pm, guid, date.getTime());
            }
            sdata.recordFeedModifiedDate(lastModified);
            return sdata;
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("error encoding rss channel name", e);
        }
    }

    private static SubscriptionData<ParsedMessage> parseAtomFeed(Element feed, Folder.SyncData fsd, long lastModified)
    throws ServiceException {
        try {
            // get defaults from the <feed> element
            InternetAddress addrFeed = parseAtomAuthor(feed.getOptionalElement("author"), null);
            if (addrFeed == null) {
                addrFeed = new JavaMailInternetAddress("", stripXML(feed.getAttribute("title")), "utf-8");
            }
            Date dateFeed = DateUtil.parseISO8601Date(feed.getAttribute("updated", null), new Date());
            List<Enclosure> enclosures = new ArrayList<Enclosure>();
            SubscriptionData<ParsedMessage> sdata = new SubscriptionData<ParsedMessage>();

            for (Element item : feed.listElements("entry")) {
                // get the item's date
                Date date = DateUtil.parseISO8601Date(item.getAttribute("updated", null), null);
                if (date == null) {
                    date = DateUtil.parseISO8601Date(item.getAttribute("modified", null), dateFeed);
                }

                // construct an address for the author
                InternetAddress addr = parseAtomAuthor(item.getOptionalElement("author"), addrFeed);

                // get the item's title (may be html or xhtml)
                Element tblock = item.getElement("title");
                String type = tblock.getAttribute("type", "text").trim().toLowerCase();
                String title = tblock.getText();
                if (type.equals("html") || type.equals("xhtml") || type.equals("text/html") || type.equals("application/xhtml+xml")) {
                    title = stripXML(title);
                }

                // find the item's link and any enclosures (associated media links)
                enclosures.clear();
                String href = "";
                for (Element link : item.listElements("link")) {
                    String relation = link.getAttribute("rel", "alternate");
                    if (relation.equals("alternate")) {
                        href = link.getAttribute("href");
                    } else if (relation.equals("enclosure")) {
                        enclosures.add(new Enclosure(link.getAttribute("href", null), link.getAttribute("title", null), link.getAttribute("type", null)));
                    }
                }
                String guid = item.getAttribute("id", href);

                // make sure we haven't already seen this item
                if (fsd != null && fsd.alreadySeen(guid == null || guid.equals("") ? null : guid, date == dateFeed ? null : date))
                    continue;

                // get the content/summary and markup
                Element content = item.getOptionalElement("content");
                if (content == null) {
                    content = item.getOptionalElement("summary");
                }
                if (content == null)
                    continue;

                type = content.getAttribute("type", "text").trim().toLowerCase();
                boolean html = false;
                if (type.equals("html") || type.equals("xhtml") || type.equals("text/html") || type.equals("application/xhtml+xml")) {
                    html = true;
                } else if (!type.equals("text") && !type.equals("text/plain")) {
                    throw ServiceException.PARSE_ERROR("unsupported atom entry content type: " + type, null);
                }

                String text = content.getText();
                if (Strings.isNullOrEmpty(text)) {
                    Element div = content.getElement("div");
                    if (div != null) {
                        /*
                         * Assume it is this variant:
                         * http://tools.ietf.org/html/rfc4287#section-4.1.3
                         *   atomInlineXHTMLContent = element atom:content { atomCommonAttributes,
                         *           attribute type { "xhtml" }, xhtmlDiv }
                         */
                        text = div.getText();
                    }
                }
                ParsedMessage pm = generateMessage(title, text, href, html, addr, date, enclosures);
                sdata.recordItem(pm, guid, date.getTime());
            }
            sdata.recordFeedModifiedDate(lastModified);
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
        StringBuilder content = new StringBuilder();
        if (html) {
            content.append(HTML_HEADER).append(text).append("<p>").append(href).append("</p>").append(HTML_FOOTER);
        } else {
            content.append(text).append("\r\n\r\n").append(href);
        }
        return generateMessage(title, content.toString(), ctype, addr, date, attach);
    }

    private static ParsedMessage generateMessage(String title, String content, String ctype,
            InternetAddress addr, Date date, List<Enclosure> attach)
    throws ServiceException {
        // cull out invalid enclosures
        if (attach != null) {
            for (Iterator<Enclosure> it = attach.iterator(); it.hasNext(); ) {
                if (it.next().getLocation() == null) {
                    it.remove();
                }
            }
        }
        boolean hasAttachments = attach != null && !attach.isEmpty();

        // clean up whitespace in the title
        if (title != null) {
            title = title.replaceAll("\\s+", " ");
        }

        // create the MIME message and wrap it
        try {
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
            MimePart body = hasAttachments ? new ZMimeBodyPart() : (MimePart) mm;
            body.setText(content, "utf-8");
            body.setHeader("Content-Type", ctype);

            if (hasAttachments) {
                // encode each enclosure as an attachment with Content-Location set
                MimeMultipart mmp = new ZMimeMultipart("mixed");
                mmp.addBodyPart((BodyPart) body);
                for (Enclosure enc : attach) {
                    MimeBodyPart part = new ZMimeBodyPart();
                    part.setText("");
                    part.addHeader("Content-Location", enc.getLocation());
                    part.addHeader("Content-Type", enc.getContentType());
                    if (enc.getDescription() != null) {
                        part.addHeader("Content-Description", enc.getDescription());
                    }
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
        if (creator == null || creator.equals("")) {
            return addrChannel;
        }

        // check for a mailto: link
        String lc = creator.trim().toLowerCase(), address = "", personal = creator;
        int mailto = lc.indexOf("mailto:");
        if (mailto == 0 && lc.length() <= 7) {
            return addrChannel;
        } else if (mailto == 0) {
            personal = null;
            address = creator = creator.substring(7);
        } else if (mailto != -1) {
            // checking for "...[mailto:...]..." or "...(mailto:...)..."
            char delimit = creator.charAt(mailto - 1), complement = 0;
            if (delimit == '[') {
                complement = ']';
            } else if (delimit == '(') {
                complement = ')';
            }
            int closing = creator.indexOf(complement, mailto + 7);
            if (closing != -1 && closing != mailto + 7) {
                address = creator.substring(mailto + 7, closing);
                personal = (creator.substring(0, mailto - 1) + creator.substring(closing + 1)).trim();
            }
        }

        try {
            return new JavaMailInternetAddress(address, personal, "utf-8");
        } catch (UnsupportedEncodingException e) { }
        try {
            return new JavaMailInternetAddress("", creator, "utf-8");
        } catch (UnsupportedEncodingException e) { }
        return addrChannel;
    }

    private static InternetAddress parseAuthor(String author) throws IOException, ParseException {
        if (author != null && author.indexOf('@') == -1) {
            return new JavaMailInternetAddress("", stripXML(author), "utf-8");
        } else {
            return new JavaMailInternetAddress(author);
        }
    }

    private static InternetAddress parseAtomAuthor(Element author, InternetAddress addrChannel) {
        if (author == null) {
            return addrChannel;
        }

        String address  = stripXML(author.getAttribute("email", ""));
        String personal = stripXML(author.getAttribute("name", ""));
        if (personal.equals("") && address.equals("")) {
            return addrChannel;
        }

        try {
            return new JavaMailInternetAddress(address, personal, "utf-8");
        } catch (UnsupportedEncodingException e) { }
        try {
            return new JavaMailInternetAddress("", address + personal, "utf-8");
        } catch (UnsupportedEncodingException e) { }
        return addrChannel;
    }

    private static class UnescapedContent extends org.xml.sax.helpers.DefaultHandler {
        private final StringBuilder str = new StringBuilder();

        UnescapedContent()  { }

        @Override
        public void startDocument() {
            str.setLength(0);
        }

        @Override
        public void characters(char[] ch, int offset, int length) {
            str.append(ch, offset, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
            if (str.length() > 0) {
                String name = localName.toUpperCase();
                if (name.equals("P") || name.equals("BR") || name.equals("HR")) {
                    if (!Character.isWhitespace(str.charAt(str.length() - 1))) {
                        str.append(" ");
                    }
                }
            }
        }

        @Override
        public String toString() {
            return str.toString();
        }
    }

    @VisibleForTesting
    static final String stripXML(String title) {
        if (title == null) {
            return "";
        } else if (title.indexOf('<') == -1 && title.indexOf('&') == -1) {
            return title;
        }

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
