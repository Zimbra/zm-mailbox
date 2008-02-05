/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.fb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

import org.dom4j.DocumentException;

import com.zimbra.common.soap.Element;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;

public class ExchangeFreeBusyProvider extends FreeBusyProvider {
	
	public static final String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)";
	public static final int FB_INTERVAL = 30;
	public static final int MULTI_STATUS = 207;
	
	public static class ServerInfo {
		enum Scheme { http, https };
		public Scheme scheme;
		public String hostname;
		public int port;
		public String ou;
		public String authUsername;
		public String authPassword;
	}
	public static interface ExchangeUserResolver {
		public ServerInfo getServerInfo(String emailAddr);
	}
	
	private static ArrayList<ExchangeUserResolver> mResolvers;
	static {
		mResolvers = new ArrayList<ExchangeUserResolver>();
	}

	public static void registerResolver(ExchangeUserResolver r, int priority) {
		mResolvers.ensureCapacity(priority+1);
		mResolvers.add(priority, r);
	}

	public FreeBusyProvider getInstance() {
		return new ExchangeFreeBusyProvider();
	}
	public void addFreeBusyRequest(Request req) throws FreeBusyUserNotFoundException {
		ServerInfo info = null;
		for (ExchangeUserResolver resolver : mResolvers) {
			info = resolver.getServerInfo(req.email);
			if (info != null)
				break;
		}
		if (info == null)
			throw new FreeBusyUserNotFoundException();
		addRequest(info, req);
	}
	
	private void addRequest(ServerInfo info, Request req) {
		ArrayList<Request> r = mRequests.get(info.hostname);
		if (r == null) {
			r = new ArrayList<Request>();
			mRequests.put(info.hostname, r);
		}
		req.data = info;
		r.add(req);
	}
	private HashMap<String,ArrayList<Request>> mRequests;
	
	public Set<FreeBusy> getResults() {
		HashSet<FreeBusy> ret = new HashSet<FreeBusy>();
		for (Map.Entry<String, ArrayList<Request>> entry : mRequests.entrySet())
			ret.addAll(this.getFreeBusyForHost(entry.getKey(), entry.getValue()));
		return ret;
	}
	
	private static final String EXCHANGE = "EXCHANGE";
	
	public String getName() {
		return EXCHANGE;
	}
	
	public boolean canCacheZimbraUserFreeBusy() {
		return mResolvers.size() > 0;
	}
	
	public long cachedFreeBusyStartTime() {
		// beginning of this month
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		return cal.getTimeInMillis();
	}
	
	public long cachedFreeBusyEndTime() {
		// beginning of this month + 2
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		int month = cal.get(Calendar.MONTH) + 2;
		if (month > 11) {
			month -= 12;
			cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
		}
		cal.set(Calendar.MONTH, month);
		return cal.getTimeInMillis();
	}
	
	public boolean propogateFreeBusy(String email, FreeBusy fb) {
		ServerInfo serverInfo = getServerInfo(email);
		if (serverInfo == null) {
			ZimbraLog.misc.warn("no exchange server info for user "+email);
			return true;  // no retry
		}
		ExchangeMessage msg = new ExchangeMessage(serverInfo.ou, email);
		String url = constructUrl(serverInfo) + msg.getUrl();
		Credentials cred = new UsernamePasswordCredentials(serverInfo.authUsername, serverInfo.authPassword);

		HttpMethod method = null;
		try {
			method = msg.createMethod(url, fb);
			int status = sendRequest(method, cred);
			if (status != MULTI_STATUS) {
				ZimbraLog.misc.error("cannot create resource at "+url);
				return false;  // retry
			}
		} catch (IOException ioe) {
			ZimbraLog.misc.error("error commucating to "+serverInfo.hostname, ioe);
			return false;  // retry
		} finally {
			if (method != null)
				method.releaseConnection();
		}
		return true;
	}
	
	private int sendRequest(HttpMethod method, Credentials credential) throws IOException {
		int status = 0;
		method.setDoAuthentication(true);
		method.setRequestHeader("User-Agent", USER_AGENT);
		HttpState state = new HttpState();
		state.setCredentials(AuthScope.ANY, credential);
		HttpClient client = new HttpClient();
		client.setState(state);
		ArrayList<String> authPrefs = new ArrayList<String>();
		authPrefs.add(AuthPolicy.BASIC);
		client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
		try {
			status = client.executeMethod(method);
		} finally {
			method.releaseConnection();
		}
		return status;
	}
	
	ExchangeFreeBusyProvider() {
		mRequests = new HashMap<String,ArrayList<Request>>();
	}
	
	private Set<FreeBusy> getEmptySet(ArrayList<Request> req) {
		HashSet<FreeBusy> ret = new HashSet<FreeBusy>();
		for (Request r : req)
			ret.add(FreeBusy.emptyFreeBusy(r.email, r.start, r.end));
		return ret;
	}
	
	public Set<FreeBusy> getFreeBusyForHost(String host, ArrayList<Request> req) {
		Request r = req.get(0);
		ServerInfo serverInfo = (ServerInfo) r.data;
		if (serverInfo == null) {
			ZimbraLog.misc.warn("no exchange server info for user "+r.email);
			return getEmptySet(req);
		}
		String url = constructGetUrl(serverInfo, req);
		ZimbraLog.misc.debug("fetching fb from url="+url);
		HttpMethod method = new GetMethod(url);
		method.setDoAuthentication(true);
		method.setRequestHeader("User-Agent", USER_AGENT);
		HttpState state = new HttpState();
		state.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(serverInfo.authUsername, serverInfo.authPassword));
		HttpClient client = new HttpClient();
		client.setState(state);
        ArrayList<String> authPrefs = new ArrayList<String>();
        authPrefs.add(AuthPolicy.BASIC);
        client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
		Element response = null;
		try {
			int status = client.executeMethod(method);
			Header cl = method.getResponseHeader("Content-Length");
			if (status != 200 || cl == null)
				return getEmptySet(req);
			response = Element.parseXML(method.getResponseBodyAsStream());
		} catch (DocumentException e) {
			ZimbraLog.misc.warn("error parsing fb response from exchange", e);
			return getEmptySet(req);
		} catch (IOException e) {
			ZimbraLog.misc.warn("error parsing fb response from exchange", e);
			return getEmptySet(req);
		} finally {
			method.releaseConnection();
		}
		HashSet<FreeBusy> ret = new HashSet<FreeBusy>();
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
		StringBuilder buf = new StringBuilder(constructUrl(info));
		buf.append("/public/?cmd=freebusy");
		buf.append("&start=").append(DateUtil.toISO8601(new Date(req.get(0).start)));
		buf.append("&endt=").append(DateUtil.toISO8601(new Date(req.get(0).end)));
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
    			if (email != null && email.getText().equals(emailAddr)) {
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
    
	private String constructUrl(ServerInfo info) {
		StringBuilder buf = new StringBuilder();
		if (info.scheme == ServerInfo.Scheme.http)
			buf.append("http://");
		else
			buf.append("https://");
		buf.append(info.hostname);
		if (info.scheme == ServerInfo.Scheme.http && info.port != 80 ||
			info.scheme == ServerInfo.Scheme.https && info.port != 443) {
			buf.append(":").append(info.port);
		}
		return buf.toString();
	}
	
	public ServerInfo getServerInfo(String emailAddr) {
		ServerInfo serverInfo = null;
		for (ExchangeUserResolver r : mResolvers) {
			serverInfo = r.getServerInfo(emailAddr);
			if (serverInfo != null)
				break;
		}
		return serverInfo;
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
