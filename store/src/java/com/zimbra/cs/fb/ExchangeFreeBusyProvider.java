/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2018 Synacor, Inc.
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
package com.zimbra.cs.fb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpHeader;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.httpclient.HttpProxyUtil;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;

public class ExchangeFreeBusyProvider extends FreeBusyProvider {

    public static final String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)";
    //public static final String FORM_AUTH_PATH = "/exchweb/bin/auth/owaauth.dll";  // specified in LC.calendar_exchange_form_auth_url
    public static final int MULTI_STATUS = 207;
    public static final String TYPE_WEBDAV = "webdav";

    public enum AuthScheme { basic, form };

    public static class ServerInfo {
        public boolean enabled;
        public String url;
        public String org;
        public String cn;
        public String authUsername;
        public String authPassword;
        public AuthScheme scheme;
    }
    public static interface ExchangeUserResolver {
        public ServerInfo getServerInfo(String emailAddr);
    }

    private static class BasicUserResolver implements ExchangeUserResolver {
        @Override
        public ServerInfo getServerInfo(String emailAddr) {
            String url = getAttr(Provisioning.A_zimbraFreebusyExchangeURL, emailAddr);
            String user = getAttr(Provisioning.A_zimbraFreebusyExchangeAuthUsername, emailAddr);
            String pass = getAttr(Provisioning.A_zimbraFreebusyExchangeAuthPassword, emailAddr);
            String scheme = getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme, emailAddr);
            if (url == null || user == null || pass == null || scheme == null)
                return null;

            ServerInfo info = new ServerInfo();
            info.url = url;
            info.authUsername = user;
            info.authPassword = pass;
            info.scheme = AuthScheme.valueOf(scheme);
            info.org = getAttr(Provisioning.A_zimbraFreebusyExchangeUserOrg, emailAddr);
            try {
                Account acct = null;
                if (emailAddr != null) {
                    acct = Provisioning.getInstance().get(AccountBy.name, emailAddr);
                }
                if (acct != null) {
                    String fps[] = acct.getMultiAttr(Provisioning.A_zimbraForeignPrincipal);
                    if (fps != null && fps.length > 0) {
                        for (String fp : fps) {
                            if (fp.startsWith(Provisioning.FP_PREFIX_AD)) {
                                int idx = fp.indexOf(':');
                                if (idx != -1) {
                                    info.cn = fp.substring(idx+1);
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (ServiceException se) {
                info.cn = null;
            }
            String exchangeType = getAttr(Provisioning.A_zimbraFreebusyExchangeServerType, emailAddr);
            info.enabled = TYPE_WEBDAV.equals(exchangeType);
            return info;
        }
        // first lookup account/cos, then domain, then globalConfig.
        static String getAttr(String attr, String emailAddr) {
            String val = null;
            if (attr == null)
                return val;
            try {
                Provisioning prov = Provisioning.getInstance();
                if (emailAddr != null) {
                    Account acct = prov.get(AccountBy.name, emailAddr);
                    if (acct != null) {
                        val = acct.getAttr(attr, null);
                        if (val != null)
                            return val;
                        Domain dom = prov.getDomain(acct);
                        if (dom != null)
                            val = dom.getAttr(attr, null);
                        if (val != null)
                            return val;
                    }
                }
                val = prov.getConfig().getAttr(attr, null);
            } catch (ServiceException se) {
                ZimbraLog.fb.error("can't get attr "+attr, se);
            }
            return val;
        }
    }

    private static ArrayList<ExchangeUserResolver> sRESOLVERS;
    static {
        sRESOLVERS = new ArrayList<ExchangeUserResolver>();

        registerResolver(new BasicUserResolver(), 0);
        register(new ExchangeFreeBusyProvider());
    }

    public static void registerResolver(ExchangeUserResolver r, int priority) {
        synchronized (sRESOLVERS) {
            sRESOLVERS.ensureCapacity(priority+1);
            sRESOLVERS.add(priority, r);
        }
    }

    @Override
    public FreeBusyProvider getInstance() {
        return new ExchangeFreeBusyProvider();
    }

    @Override
    public void addFreeBusyRequest(Request req) throws FreeBusyUserNotFoundException {
        ServerInfo info = null;
        for (ExchangeUserResolver resolver : sRESOLVERS) {
            String email = req.email;
            if (req.requestor != null)
                email = req.requestor.getName();
            info = resolver.getServerInfo(email);
            if (info != null)
                break;
        }
        if (info == null)
            throw new FreeBusyUserNotFoundException();
        if (!info.enabled)
            throw new FreeBusyUserNotFoundException();
        addRequest(info, req);
    }

    private void addRequest(ServerInfo info, Request req) {
        ArrayList<Request> r = mRequests.get(info.url);
        if (r == null) {
            r = new ArrayList<Request>();
            mRequests.put(info.url, r);
        }
        req.data = info;
        r.add(req);
    }
    private Map<String,ArrayList<Request>> mRequests;

    @Override
    public List<FreeBusy> getResults() {
        ArrayList<FreeBusy> ret = new ArrayList<FreeBusy>();
        for (Map.Entry<String, ArrayList<Request>> entry : mRequests.entrySet()) {
            try {
                ret.addAll(this.getFreeBusyForHost(entry.getKey(), entry.getValue()));
            } catch (IOException e) {
                ZimbraLog.fb.error("error communicating with "+entry.getKey(), e);
                return getEmptyList(entry.getValue());
            }
        }
        return ret;
    }

    private static final String EXCHANGE = "EXCHANGE";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_TRANSLATE  = "Translate";

    @Override
    public String foreignPrincipalPrefix() {
        return Provisioning.FP_PREFIX_AD;
    }

    @Override
    public String getName() {
        return EXCHANGE;
    }

    @Override
    public Set<MailItem.Type> registerForItemTypes() {
        return EnumSet.of(MailItem.Type.APPOINTMENT);
    }

    @Override
    public boolean registerForMailboxChanges() {
        return registerForMailboxChanges(null);
    }

    @Override
    public boolean registerForMailboxChanges(String accountId) {
        String email = null;
        try {
            Account account = null;
            if (accountId != null)
                account = Provisioning.getInstance().getAccountById(accountId);
            if (account != null)
                email = account.getName();
        } catch (ServiceException se) {
            ZimbraLog.fb.warn("cannot fetch account", se);
        }
        return getServerInfo(email) != null;
    }

    private long getTimeInterval(String attr, String accountId, long defaultValue) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        if (accountId != null) {
            Account acct = prov.get(AccountBy.id, accountId);
            if (acct != null) {
                return acct.getTimeInterval(attr, defaultValue);
            }
        }
        return prov.getConfig().getTimeInterval(attr, defaultValue);
    }

    @Override
    public long cachedFreeBusyStartTime(String accountId) {
        Calendar cal = GregorianCalendar.getInstance();
        int curYear = cal.get(Calendar.YEAR);
        try {
            long dur = getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, accountId, 0);
            cal.setTimeInMillis(System.currentTimeMillis() - dur);
        } catch (ServiceException se) {
            // set to 1 week ago
            cal.setTimeInMillis(System.currentTimeMillis() - Constants.MILLIS_PER_WEEK);
        }
        // normalize the time to 00:00:00
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        if (cal.get(Calendar.YEAR) < curYear) {
            // Exchange accepts FB info for only one calendar year. If the start date falls in the previous year
            // change it to beginning of the current year.
            cal.set(curYear, 0, 1);
        }
        return cal.getTimeInMillis();
    }

    @Override
    public long cachedFreeBusyEndTime(String accountId) {
        long duration = Constants.MILLIS_PER_MONTH * 2;
        Calendar cal = GregorianCalendar.getInstance();
        try {
            duration = getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedInterval, accountId, duration);
        } catch (ServiceException se) {
        }
        cal.setTimeInMillis(cachedFreeBusyStartTime(accountId) + duration);
        // normalize the time to 00:00:00
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }

    @Override
    public long cachedFreeBusyStartTime() {
        return cachedFreeBusyStartTime(null);
    }

    @Override
    public long cachedFreeBusyEndTime() {
        return cachedFreeBusyEndTime(null);
    }

    @Override
    public boolean handleMailboxChange(String accountId) {
        String email = getEmailAddress(accountId);
        ServerInfo serverInfo = getServerInfo(email);
        if (email == null || !serverInfo.enabled) {
            return true;  // no retry
        }
        FreeBusy fb;
        try {
            fb = getFreeBusy(accountId, FreeBusyQuery.CALENDAR_FOLDER_ALL);
        } catch (ServiceException se) {
            ZimbraLog.fb.warn("can't get freebusy for account "+accountId, se);
            // retry the request if it's receivers fault.
            return !se.isReceiversFault();
        }
        if (email == null || fb == null) {
            ZimbraLog.fb.warn("account not found / incorrect / wrong host: "+accountId);
            return true;  // no retry
        }
        if (serverInfo == null || serverInfo.org == null || serverInfo.cn == null) {
            ZimbraLog.fb.warn("no exchange server info for user "+email);
            return true;  // no retry
        }
        ExchangeMessage msg = new ExchangeMessage(serverInfo.org, serverInfo.cn, email);
        String url = serverInfo.url + msg.getUrl();

        HttpRequestBase method = null;
        HttpResponse response = null;
        try {
            ZimbraLog.fb.debug("POST "+url);
            method = msg.createMethod(url, fb);
            method.addHeader(HEADER_TRANSLATE, "f");
            response = sendRequest(method, serverInfo);
            int status = response.getStatusLine().getStatusCode();
            if (status != MULTI_STATUS) {
                InputStream resp = response.getEntity().getContent();
                String respStr = (resp == null ? "" : new String(ByteUtil.readInput(resp, 1024, 1024), "UTF-8"));
                ZimbraLog.fb.error("cannot modify resource at %s : http error %d, buf (%s)", url, status, respStr);
                return false;  // retry
            }
        } catch (IOException | HttpException ioe) {
            ZimbraLog.fb.error("error communicating with "+serverInfo.url, ioe);
            return false;  // retry
        } finally {
            method.releaseConnection();
        }
        return true;
    }

    private HttpResponse sendRequest(HttpRequestBase method, ServerInfo info) throws IOException, HttpException {

        method.addHeader(HEADER_USER_AGENT, USER_AGENT);
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpProxyUtil.configureProxy(clientBuilder);
        switch (info.scheme) {
        case basic:
            basicAuth(clientBuilder, info);
            break;
        case form:
            formAuth(clientBuilder, info);
            break;
        }
        return HttpClientUtil.executeMethod(clientBuilder.build(), method);
    }

    private boolean basicAuth(HttpClientBuilder clientBuilder, ServerInfo info) {

        Credentials cred = new UsernamePasswordCredentials(info.authUsername, info.authPassword);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider .setCredentials(AuthScope.ANY, cred);
        clientBuilder.setDefaultCredentialsProvider(credsProvider);

        Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
            .register(AuthSchemes.BASIC, new BasicSchemeFactory(Consts.UTF_8)).build();
        clientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
        return true;
    }

    private boolean formAuth(HttpClientBuilder clientBuilder, ServerInfo info) throws IOException, HttpException {
        StringBuilder buf = new StringBuilder();
        buf.append("destination=");
        buf.append(URLEncoder.encode(info.url, "UTF-8"));
        buf.append("&username=");
        buf.append(info.authUsername);
        buf.append("&password=");
        buf.append(URLEncoder.encode(info.authPassword, "UTF-8"));
        buf.append("&flags=0");
        buf.append("&SubmitCreds=Log On");
        buf.append("&trusted=0");
        String url = info.url + LC.calendar_exchange_form_auth_url.value();
        HttpPost method = new HttpPost(url);
        ByteArrayEntity re = new ByteArrayEntity(buf.toString().getBytes(), ContentType.create("x-www-form-urlencoded"));
        method.setEntity(re);
        BasicCookieStore state = new BasicCookieStore();
        clientBuilder.setDefaultCookieStore(state);
        HttpResponse response = null;
        try {
            response = HttpClientUtil.executeMethod(clientBuilder.build(), method);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 400) {
                ZimbraLog.fb.error("form auth to Exchange returned an error: "+status);
                return false;
            }
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
        }
        return true;
    }

    ExchangeFreeBusyProvider() {
        mRequests = new HashMap<String,ArrayList<Request>>();
    }

    public List<FreeBusy> getFreeBusyForHost(String host, ArrayList<Request> req) throws IOException {
        ArrayList<FreeBusy> ret = new ArrayList<FreeBusy>();
        int fb_interval = LC.exchange_free_busy_interval_min.intValueWithinRange(5, 1444);
        Request r = req.get(0);
        ServerInfo serverInfo = (ServerInfo) r.data;
        if (serverInfo == null) {
            ZimbraLog.fb.warn("no exchange server info for user "+r.email);
            return ret;
        }
        if (!serverInfo.enabled) {
            return ret;
        }
        String url = constructGetUrl(serverInfo, req);
        ZimbraLog.fb.debug("fetching fb from url="+url);
        HttpRequestBase method = new HttpGet(url);


        Element response = null;
        HttpResponse httpResponse = null;
        try {
            httpResponse = sendRequest(method, serverInfo);
            int status = httpResponse.getStatusLine().getStatusCode();
            if (status != 200)
                return getEmptyList(req);
            if (ZimbraLog.fb.isDebugEnabled()) {
                Header cl = httpResponse.getFirstHeader(HttpHeader.CONTENT_LENGTH.name());
                int contentLength = 10240;
                if (cl != null)
                    contentLength = Integer.valueOf(cl.getValue());
                String buf = new String(com.zimbra.common.util.ByteUtil.readInput(
                    httpResponse.getEntity().getContent(), contentLength, contentLength), "UTF-8");
                ZimbraLog.fb.debug(buf);
                response = Element.parseXML(buf);
            } else
                response = Element.parseXML(httpResponse.getEntity().getContent());
        } catch (XmlParseException e) {
            ZimbraLog.fb.warn("error parsing fb response from exchange", e);
            return getEmptyList(req);
        } catch (IOException | HttpException e) {
            ZimbraLog.fb.warn("error parsing fb response from exchange", e);
            return getEmptyList(req);
        }  finally {
            EntityUtils.consumeQuietly(httpResponse.getEntity());
        }
        for (Request re : req) {
            String fb = getFbString(response, re.email);
            ret.add(new ExchangeUserFreeBusy(fb, re.email, fb_interval, re.start, re.end));
        }
        return ret;
    }

    private String constructGetUrl(ServerInfo info, ArrayList<Request> req) {
        // http://exchange/public/?params..
        //   cmd      = freebusy
        //   start    = [ISO8601date]
        //   end      = [ISO8601date]
        //   interval = [minutes]
        //   u        = SMTP:[emailAddr]
        int fb_interval = LC.exchange_free_busy_interval_min.intValueWithinRange(5, 1444);
        long start = Request.offsetInterval(req.get(0).start, fb_interval);
        StringBuilder buf = new StringBuilder(info.url);
        buf.append("/public/?cmd=freebusy");
        buf.append("&start=").append(DateUtil.toISO8601(new Date(start)));
        buf.append("&end=").append(DateUtil.toISO8601(new Date(req.get(0).end)));
        buf.append("&interval=").append(fb_interval);
        for (Request r : req)
            buf.append("&u=SMTP:").append(r.email);
        return buf.toString();
    }

    private String getFbString(Element fbxml, String emailAddr) {
        String ret = "";
        Element node = fbxml.getOptionalElement("recipients");
        if (node != null) {
            for (Element e : node.listElements("item")) {
                Element email = e.getOptionalElement("email");
                if (email != null && email.getText().trim().equalsIgnoreCase(emailAddr)) {
                    Element fb = e.getOptionalElement("fbdata");
                    if (fb != null) {
                        ret = fb.getText();
                    }
                    break;
                }
            }
        }
        return ret;
    }

    public ServerInfo getServerInfo(String emailAddr) {
        ServerInfo serverInfo = null;
        for (ExchangeUserResolver r : sRESOLVERS) {
            serverInfo = r.getServerInfo(emailAddr);
            if (serverInfo != null)
                break;
        }
        return serverInfo;
    }

    public static HttpResponse checkAuth(ServerInfo info, Account requestor) throws ServiceException, IOException, HttpException {
        ExchangeFreeBusyProvider prov = new ExchangeFreeBusyProvider();
        ArrayList<Request> req = new ArrayList<Request>();
        req.add(new Request(
                requestor,
                requestor.getName(),
                prov.cachedFreeBusyStartTime(),
                prov.cachedFreeBusyEndTime(),
                FreeBusyQuery.CALENDAR_FOLDER_ALL));
        String url = prov.constructGetUrl(info, req);

        HttpRequestBase method = new HttpGet(url);
        HttpResponse response = null;
        try {
            response = prov.sendRequest(method, info);
            return response;
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }


    public static class ExchangeUserFreeBusy extends FreeBusy {
        /*
            <a:response xmlns:a="WM">
                <a:recipients>
                    <a:item>
                        <a:displayname>All Attendees</a:displayname>
                        <a:type>1</a:type>
                        <a:fbdata>000020000000000000000000000000000000000022220000</a:fbdata>
                    </a:item>
                    <a:item>
                        <a:displayname>test user</a:displayname>
                        <a:email type="SMTP">testuser@2k3-ex2k3.local</a:email>
                        <a:type>1</a:type>
                        <a:fbdata>000020000000000000000000000000000000000022220000</a:fbdata>
                    </a:item>
                </a:recipients>
            </a:response>
         */
        private final char FREE        = '0';
        private final char TENTATIVE   = '1';
        private final char BUSY        = '2';
        private final char UNAVAILABLE = '3';
        private final char NODATA      = '4';

        protected ExchangeUserFreeBusy(String fb, String emailAddr, int interval, long start, long end) {
            super(emailAddr, start, end);
            parseInterval(fb, emailAddr, interval, start, end);
        }
        private void parseInterval(String fb, String emailAddr, int interval, long start, long end) {
            long intervalInMillis = interval * 60L * 1000L;
            long maskedStart = Request.offsetInterval(start, interval);
            if (maskedStart < start) {
                end = maskedStart + intervalInMillis;
            } else {
                end = start + intervalInMillis;
            }
            for (int i = 0; i < fb.length(); i++) {
                switch (fb.charAt(i)) {
                case TENTATIVE:
                    mList.addInterval(new Interval(start, end, IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE));
                    break;
                case BUSY:
                    mList.addInterval(new Interval(start, end, IcalXmlStrMap.FBTYPE_BUSY));
                    break;
                case UNAVAILABLE:
                    mList.addInterval(new Interval(start, end, IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE));
                    break;
                case NODATA:
                    mList.addInterval(new Interval(start, end, IcalXmlStrMap.FBTYPE_NODATA));
                    break;
                case FREE:
                default:
                    break;
                }
                start = end;
                end = start + intervalInMillis;
            }
        }
    }
}
