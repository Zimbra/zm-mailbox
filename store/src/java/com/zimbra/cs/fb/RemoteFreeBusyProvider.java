/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.HttpProxyUtil;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ServerProxyTarget;
import com.zimbra.soap.ZimbraSoapContext;

public class RemoteFreeBusyProvider extends FreeBusyProvider {

    public RemoteFreeBusyProvider(HttpServletRequest httpReq, ZimbraSoapContext zsc,
                                  long start, long end, String exApptUid) {
        mRemoteAccountMap = new HashMap<String,StringBuilder>();
        mRequestList = new ArrayList<Request>();
        mHttpReq = httpReq;
        mSoapCtxt = zsc;
        mStart = start;
        mEnd = end;
        mExApptUid = exApptUid;
    }

    @Override
    public FreeBusyProvider getInstance() {
        // special case this class as it's used to get free/busy of zimbra accounts
        // on a remote mailbox host, and we can perform some shortcuts i.e.
        // returning the proxied response straight to the main response.
        // plus it requires additional resources such as original
        // HttpServletRequest and ZimbraSoapContext.
        return null;
    }

    @Override
    public void addFreeBusyRequest(Request req) {
        Account account = (Account) req.data;
        if (account == null)
            return;
        String hostname = account.getAttr(Provisioning.A_zimbraMailHost);
        StringBuilder buf = mRemoteAccountMap.get(hostname);
        if (buf == null)
            buf = new StringBuilder(req.email);
        else
            buf.append(",").append(req.email);
        mRemoteAccountMap.put(hostname, buf);
        mRequestList.add(req);
    }
    public void addFreeBusyRequest(Account requestor, Account acct, String id, long start, long end, int folder) {
        Request req = new Request(requestor, id, start, end, folder);
        req.data = acct;
        addFreeBusyRequest(req);
    }

    @Override
    public List<FreeBusy> getResults() {
        ArrayList<FreeBusy> fbList = new ArrayList<FreeBusy>();
        for (Request req : mRequestList) {
            HttpRequestBase method = null;
            Account acct = (Account)req.data;
            try {
                StringBuilder targetUrl = new StringBuilder();
                targetUrl.append(UserServlet.getRestUrl(acct));
                targetUrl.append("/Calendar?fmt=ifb");
                targetUrl.append("&start=").append(mStart);
                targetUrl.append("&end=").append(mEnd);
                if (req.folder != FreeBusyQuery.CALENDAR_FOLDER_ALL)
                    targetUrl.append("&").append(UserServlet.QP_FREEBUSY_CALENDAR).append("=").append(req.folder);
                try {
                    if (mExApptUid != null)
                        targetUrl.append("&").append(UserServlet.QP_EXUID).append("=").append(URLEncoder.encode(mExApptUid, "UTF-8"));
                } catch (UnsupportedEncodingException e) {}
                String authToken = null;
                try {
                    if (mSoapCtxt != null)
                        authToken = mSoapCtxt.getAuthToken().getEncoded();
                } catch (AuthTokenException e) {}
                if (authToken != null) {
                    targetUrl.append("&").append(ZimbraServlet.QP_ZAUTHTOKEN).append("=");
                    try {
                        targetUrl.append(URLEncoder.encode(authToken, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {}
                }
                HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
                HttpProxyUtil.configureProxy(clientBuilder);
                method = new HttpGet(targetUrl.toString());
                String fbMsg;
                try {
                    HttpResponse response =  HttpClientUtil.executeMethod(clientBuilder.build(), method);
                    byte[] buf = ByteUtil.getContent(response.getEntity().getContent(), 0);
                    fbMsg = new String(buf, "UTF-8");
                } catch (IOException | HttpException ex) {
                    // ignore this recipient and go on
                    fbMsg = null;
                }
                if (fbMsg != null) {
                    ZVCalendar cal = ZCalendarBuilder.build(fbMsg);
                    for (Iterator<ZComponent> compIter = cal.getComponentIterator(); compIter.hasNext(); ) {
                        ZComponent comp = compIter.next();
                        if (ICalTok.VFREEBUSY.equals(comp.getTok())) {
                            FreeBusy fb = FreeBusy.parse(comp);
                            fbList.add(fb);
                        }
                    }
                }
            } catch (ServiceException e) {
                ZimbraLog.fb.warn("can't get free/busy information for "+req.email, e);
            } finally {
                if (method != null)
                    method.releaseConnection();
            }
        }
        return fbList;
    }

    @Override
    public Set<MailItem.Type> registerForItemTypes() {
        return EnumSet.noneOf(MailItem.Type.class);
    }

    @Override
    public boolean registerForMailboxChanges() {
        return false;
    }

    @Override
    public boolean registerForMailboxChanges(String accountId) {
        return false;
    }

    @Override
    public long cachedFreeBusyStartTime() {
        return 0;
    }

    @Override
    public long cachedFreeBusyEndTime() {
        return 0;
    }

    @Override
    public long cachedFreeBusyStartTime(String accountId) {
        return 0;
    }

    @Override
    public long cachedFreeBusyEndTime(String accountId) {
        return 0;
    }

    @Override
    public boolean handleMailboxChange(String accountId) {
        return true;
    }

    @Override
    public String foreignPrincipalPrefix() {
        return "";
    }

    @Override
    public void addResults(Element response) {
        Provisioning prov = Provisioning.getInstance();
        for (Map.Entry<String, StringBuilder> entry : mRemoteAccountMap.entrySet()) {
            // String server = entry.getKey();
            String paramStr = entry.getValue().toString();
            String[] idStrs = paramStr.split(",");

            try {
                Element req = mSoapCtxt.getRequestProtocol().getFactory().createElement(MailConstants.GET_FREE_BUSY_REQUEST);
                req.addAttribute(MailConstants.A_CAL_START_TIME, mStart);
                req.addAttribute(MailConstants.A_CAL_END_TIME, mEnd);
                req.addAttribute(MailConstants.A_UID, paramStr);

                // hack: use the ID of the first user
                Account acct = prov.get(AccountBy.name, idStrs[0], mSoapCtxt.getAuthToken());
                if (acct == null)
                    acct = prov.get(AccountBy.id, idStrs[0], mSoapCtxt.getAuthToken());
                if (acct != null) {
                    Element remoteResponse = proxyRequest(req, acct.getId(), mSoapCtxt);
                    for (Element thisElt : remoteResponse.listElements())
                        response.addElement(thisElt.detach());
                } else {
                    ZimbraLog.fb.debug("Account " + idStrs[0] + " not found while searching free/busy");
                }
            } catch (SoapFaultException e) {
                ZimbraLog.fb.error("cannot get free/busy for "+idStrs[0], e);
                addFailedAccounts(response, idStrs);
            } catch (ServiceException e) {
                ZimbraLog.fb.error("cannot get free/busy for "+idStrs[0], e);
                addFailedAccounts(response, idStrs);
            }
        }
    }

    private static final String REMOTE = "REMOTE";

    @Override
    public String getName() {
        return REMOTE;
    }
    private Map<String,StringBuilder> mRemoteAccountMap;
    private ArrayList<Request> mRequestList;
    private HttpServletRequest mHttpReq;
    private ZimbraSoapContext mSoapCtxt;
    private long mStart;
    private long mEnd;
    private String mExApptUid;  // UID of appointment to exclude from free/busy search

    private void addFailedAccounts(Element response, String[] idStrs) {
        for (String id : idStrs) {
            ToXML.encodeFreeBusy(response, FreeBusy.nodataFreeBusy(id, mStart, mEnd));
        }
    }

    protected Element proxyRequest(Element request, String acctId, ZimbraSoapContext zsc) throws ServiceException {
        // new context for proxied request has a different "requested account"
        ZimbraSoapContext zscTarget = new ZimbraSoapContext(zsc, acctId);
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(Key.AccountBy.id, acctId);
        Server server = prov.getServer(acct);

        // executing remotely; find out target and proxy there
        ServerProxyTarget proxy = new ServerProxyTarget(server.getId(), zsc.getAuthToken(), mHttpReq);
        Element response = DocumentHandler.proxyWithNotification(request.detach(), proxy, zscTarget, zsc);
        return response;
    }
}
