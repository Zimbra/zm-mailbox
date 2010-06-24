/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.fb;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

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
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.httpclient.HttpProxyUtil;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ProxyTarget;
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
	
	public FreeBusyProvider getInstance() {
		// special case this class as it's used to get free/busy of zimbra accounts
		// on a remote mailbox host, and we can perform some shortcuts i.e.
		// returning the proxied response straight to the main response.
		// plus it requires additional resources such as original
		// HttpServletRequest and ZimbraSoapContext.
		return null;
	}
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
	public List<FreeBusy> getResults() {
		ArrayList<FreeBusy> fb = new ArrayList<FreeBusy>();
        for (Request req : mRequestList) {
            HttpMethod method = null;
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
                    targetUrl.append("&").append(UserServlet.QP_ZAUTHTOKEN).append("=");
                    try {
                        targetUrl.append(URLEncoder.encode(authToken, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {}
                }
                HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
                HttpProxyUtil.configureProxy(client);
                method = new GetMethod(targetUrl.toString());
                String fbMsg;
                try {
                    HttpClientUtil.executeMethod(client, method);
                    byte[] buf = ByteUtil.getContent(method.getResponseBodyAsStream(), 0);
                    fbMsg = new String(buf, "UTF-8");
                } catch (IOException ex) {
                    // ignore this recipient and go on
                    fbMsg = null;
                }
                if (fbMsg != null)
                	fb.add(new FreeBusyString(req.email, mStart, mEnd, fbMsg));
            } catch (ServiceException e) {
            	ZimbraLog.fb.warn("can't get free/busy information for "+req.email, e);
            } finally {
                if (method != null)
                    method.releaseConnection();
            }
        }
		return fb;
	}
	
	public int registerForItemTypes() {
		return 0;
	}
	public boolean registerForMailboxChanges() {
		return false;
	}
	public long cachedFreeBusyStartTime() {
		return 0;
	}
	public long cachedFreeBusyEndTime() {
		return 0;
	}

	public boolean handleMailboxChange(String accountId) {
		return true;
	}

	public String foreignPrincipalPrefix() {
		return "";
	}
	
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
        Account acct = prov.get(Provisioning.AccountBy.id, acctId);
        Server server = prov.getServer(acct);

        // executing remotely; find out target and proxy there
        ProxyTarget proxy = new ProxyTarget(server.getId(), zsc.getAuthToken(), mHttpReq);
        Element response = DocumentHandler.proxyWithNotification(request.detach(), proxy, zscTarget, zsc);
        return response;
    }
    
    private static class FreeBusyString extends FreeBusy {
    	private String mFbStr;
    	public FreeBusyString(String name, long start, long end, String fbStr) {
    		super(name, start, end);
    		mFbStr = fbStr;
    	}
        public String toVCalendar(Method m, String organizer, String attendee, String url) {
        	String ret = mFbStr;
        	String org = "ORGANIZER:"+organizer;
        	if (attendee != null) {
        		if (ret.indexOf("ATTENDEE") > 0)
                    ret = ret.replaceAll("ATTENDEE:.*", "ATTENDEE:"+attendee);
        		else
        			org += "\nATTENDEE:"+attendee;
        	}
        	if (url != null && ret.indexOf("URL") < 0) {
        		if (ret.indexOf("URL") > 0)
                    ret = ret.replaceAll("URL:.*", "URL:"+url);
        		else
        			org += "\nURL:"+url;
        	}
            ret = ret.replaceAll("METHOD:PUBLISH", "METHOD:"+m.toString());
            ret = ret.replaceAll("ORGANIZER:.*", org);
        	return ret;
        }
    }
}
