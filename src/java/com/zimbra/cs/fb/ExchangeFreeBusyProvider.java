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

import java.net.URLEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import org.dom4j.DocumentException;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.httpclient.HttpProxyUtil;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;

public class ExchangeFreeBusyProvider extends FreeBusyProvider {
	
	public static final String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)";
	public static final String FORM_AUTH_PATH = "/exchweb/bin/auth/owaauth.dll";
	public static final int FB_INTERVAL = 30;
	public static final int MULTI_STATUS = 207;
	
	public enum AuthScheme { basic, form };
	
	public static class ServerInfo {
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
				Account acct = Provisioning.getInstance().get(AccountBy.name, emailAddr);
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

	public FreeBusyProvider getInstance() {
		return new ExchangeFreeBusyProvider();
	}
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
	private HashMap<String,ArrayList<Request>> mRequests;
	
	public List<FreeBusy> getResults() {
		ArrayList<FreeBusy> ret = new ArrayList<FreeBusy>();
		for (Map.Entry<String, ArrayList<Request>> entry : mRequests.entrySet()) {
			try {
				ret.addAll(this.getFreeBusyForHost(entry.getKey(), entry.getValue()));
			} catch (IOException e) {
				ZimbraLog.fb.error("error communicating to "+entry.getKey(), e);
				return getEmptyList(entry.getValue());
			}
		}
		return ret;
	}
	
	private static final String EXCHANGE = "EXCHANGE";
	private static final String HEADER_USER_AGENT = "User-Agent";
	private static final String HEADER_TRANSLATE  = "Translate";
	
	public String foreignPrincipalPrefix() {
		return Provisioning.FP_PREFIX_AD;
	}
	
	public String getName() {
		return EXCHANGE;
	}

	public int registerForItemTypes() {
		return MailItem.typeToBitmask(MailItem.TYPE_APPOINTMENT);
	}
	
	public boolean registerForMailboxChanges() {
		if (sRESOLVERS.size() > 1)
			return true;
		Config config = null;
		try {
			config = Provisioning.getInstance().getConfig();
		} catch (ServiceException se) {
			ZimbraLog.fb.warn("cannot fetch config", se);
			return false;
		}
		String url = config.getAttr(Provisioning.A_zimbraFreebusyExchangeURL, null);
		String user = config.getAttr(Provisioning.A_zimbraFreebusyExchangeAuthUsername, null);
		String pass = config.getAttr(Provisioning.A_zimbraFreebusyExchangeAuthPassword, null);
		String scheme = config.getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme, null);
		return (url != null && user != null && pass != null && scheme != null);
	}
	
	public long cachedFreeBusyStartTime() {
		Calendar cal = GregorianCalendar.getInstance();
		try {
			Config config = Provisioning.getInstance().getConfig();
			long dur = config.getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, 0);
			cal.setTimeInMillis(System.currentTimeMillis() - dur);
		} catch (ServiceException se) {
			// set to 1 week ago
			cal.setTimeInMillis(System.currentTimeMillis() - Constants.MILLIS_PER_WEEK);
		}
		// normalize the time to 00:00:00
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		return cal.getTimeInMillis();
	}
	
	public long cachedFreeBusyEndTime() {
		long duration = Constants.MILLIS_PER_MONTH * 2;
		Calendar cal = GregorianCalendar.getInstance();
		try {
			Config config = Provisioning.getInstance().getConfig();
			duration = config.getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedInterval, duration);
		} catch (ServiceException se) {
		}
		cal.setTimeInMillis(cachedFreeBusyStartTime() + duration);
		// normalize the time to 00:00:00
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		return cal.getTimeInMillis();
	}
	
	public boolean handleMailboxChange(String accountId) {
		String email;
		FreeBusy fb;
		try {
			email = getEmailAddress(accountId);
			fb = getFreeBusy(accountId, FreeBusyQuery.CALENDAR_FOLDER_ALL);
		} catch (ServiceException se) {
			ZimbraLog.fb.warn("can't get freebusy for account "+accountId, se);
			// retry the request if it's receivers fault.
			return !se.isReceiversFault();
		}
		if (email == null || fb == null) {
			ZimbraLog.fb.warn("can't get freebusy for account "+accountId);
			return true;  // no retry
		}
		ServerInfo serverInfo = getServerInfo(email);
		if (serverInfo == null || serverInfo.org == null || serverInfo.cn == null) {
			ZimbraLog.fb.warn("no exchange server info for user "+email);
			return true;  // no retry
		}
		ExchangeMessage msg = new ExchangeMessage(serverInfo.org, serverInfo.cn, email);
		String url = serverInfo.url + msg.getUrl();

		HttpMethod method = null;
		
		try {
			ZimbraLog.fb.debug("POST "+url);
			method = msg.createMethod(url, fb);
			method.setRequestHeader(HEADER_TRANSLATE, "f");
			int status = sendRequest(method, serverInfo);
			if (status != MULTI_STATUS) {
			    InputStream resp = method.getResponseBodyAsStream();
			    String respStr = (resp == null ? "" : new String(ByteUtil.readInput(resp, 1024, 1024), "UTF-8"));
				ZimbraLog.fb.error("cannot modify resource at %s : http error %d, buf (%s)", url, status, respStr);
				return false;  // retry
			}
		} catch (IOException ioe) {
			ZimbraLog.fb.error("error commucating to "+serverInfo.url, ioe);
			return false;  // retry
		} finally {
			method.releaseConnection();
		}
		return true;
	}
	
	private int sendRequest(HttpMethod method, ServerInfo info) throws IOException {
		method.setDoAuthentication(true);
		method.setRequestHeader(HEADER_USER_AGENT, USER_AGENT);
		HttpClient client = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
		HttpProxyUtil.configureProxy(client);
		switch (info.scheme) {
		case basic:
			basicAuth(client, info);
			break;
		case form:
			formAuth(client, info);
			break;
		}
		return HttpClientUtil.executeMethod(client, method);
	}
	
	private boolean basicAuth(HttpClient client, ServerInfo info) {
		HttpState state = new HttpState();
		Credentials cred = new UsernamePasswordCredentials(info.authUsername, info.authPassword);
		state.setCredentials(AuthScope.ANY, cred);
		client.setState(state);
		ArrayList<String> authPrefs = new ArrayList<String>();
		authPrefs.add(AuthPolicy.BASIC);
		client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
		return true;
	}
	
	private boolean formAuth(HttpClient client, ServerInfo info) throws IOException {
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
		String url = info.url + FORM_AUTH_PATH;
		PostMethod method = new PostMethod(url);
		ByteArrayRequestEntity re = new ByteArrayRequestEntity(buf.toString().getBytes(), "x-www-form-urlencoded");
		method.setRequestEntity(re);
		HttpState state = new HttpState();
		client.setState(state);
		try {
			int status = HttpClientUtil.executeMethod(client, method);
			if (status >= 400) {
				ZimbraLog.fb.error("form auth to Exchange returned an error: "+status);
				return false;
			}
		} finally {
			method.releaseConnection();
		}
		return true;
	}
	
	ExchangeFreeBusyProvider() {
		mRequests = new HashMap<String,ArrayList<Request>>();
	}
	
	private List<FreeBusy> getEmptyList(ArrayList<Request> req) {
		ArrayList<FreeBusy> ret = new ArrayList<FreeBusy>();
		for (Request r : req)
			ret.add(FreeBusy.emptyFreeBusy(r.email, r.start, r.end));
		return ret;
	}
	
	public List<FreeBusy> getFreeBusyForHost(String host, ArrayList<Request> req) throws IOException {
		Request r = req.get(0);
		ServerInfo serverInfo = (ServerInfo) r.data;
		if (serverInfo == null) {
			ZimbraLog.fb.warn("no exchange server info for user "+r.email);
			return getEmptyList(req);
		}
		String url = constructGetUrl(serverInfo, req);
		ZimbraLog.fb.debug("fetching fb from url="+url);
		HttpMethod method = new GetMethod(url);
		
		
		Element response = null;
		try {
			int status = sendRequest(method, serverInfo);
			if (status != 200)
				return getEmptyList(req);
			if (ZimbraLog.fb.isDebugEnabled()) {
			    Header cl = method.getResponseHeader("Content-Length");
			    int contentLength = 10240;
			    if (cl != null)
			        contentLength = Integer.valueOf(cl.getValue());
				String buf = new String(com.zimbra.common.util.ByteUtil.readInput(method.getResponseBodyAsStream(), contentLength, contentLength), "UTF-8");
				ZimbraLog.fb.debug(buf);
				response = Element.parseXML(buf);
			} else
				response = Element.parseXML(method.getResponseBodyAsStream());
		} catch (DocumentException e) {
			ZimbraLog.fb.warn("error parsing fb response from exchange", e);
			return getEmptyList(req);
		} catch (IOException e) {
			ZimbraLog.fb.warn("error parsing fb response from exchange", e);
			return getEmptyList(req);
		} finally {
			method.releaseConnection();
		}
		ArrayList<FreeBusy> ret = new ArrayList<FreeBusy>();
		for (Request re : req) {
			String fb = getFbString(response, re.email);
			ret.add(new ExchangeUserFreeBusy(fb, re.email, FB_INTERVAL, re.start, re.end));
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
		StringBuilder buf = new StringBuilder(info.url);
		buf.append("/public/?cmd=freebusy");
		buf.append("&start=").append(DateUtil.toISO8601(new Date(req.get(0).start)));
		buf.append("&end=").append(DateUtil.toISO8601(new Date(req.get(0).end)));
		buf.append("&interval=").append(FB_INTERVAL);
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
	
	public static int checkAuth(ServerInfo info, Account requestor) throws ServiceException, IOException {
		ExchangeFreeBusyProvider prov = new ExchangeFreeBusyProvider();
		ArrayList<Request> req = new ArrayList<Request>();
		req.add(new Request(
				requestor, 
				requestor.getName(), 
				prov.cachedFreeBusyStartTime(), 
				prov.cachedFreeBusyEndTime(),
				FreeBusyQuery.CALENDAR_FOLDER_ALL));
		String url = prov.constructGetUrl(info, req);
		
		HttpMethod method = new GetMethod(url);
		try {
		    return prov.sendRequest(method, info);
		} finally {
		    method.releaseConnection();
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
	    	for (int i = 0; i < fb.length(); i++) {
	    		switch (fb.charAt(i)) {
	    		case TENTATIVE:
	    			mList.addInterval(new Interval(start, start + intervalInMillis, IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE));
	    			break;
	    		case BUSY:
	    			mList.addInterval(new Interval(start, start + intervalInMillis, IcalXmlStrMap.FBTYPE_BUSY));
	    			break;
	    		case UNAVAILABLE:
	    			mList.addInterval(new Interval(start, start + intervalInMillis, IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE));
	    			break;
	    		case FREE:
	    		case NODATA:
	    		default:
	    			break;
	    		}
	    		start += intervalInMillis;
	    	}
	    }
	}
}
