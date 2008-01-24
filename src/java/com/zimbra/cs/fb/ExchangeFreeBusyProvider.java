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
import java.util.Date;
import java.util.List;

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

public class ExchangeFreeBusyProvider {
	
	public static final String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)";
	public static final int FB_INTERVAL = 30;
	
	public static class ServerInfo {
		enum Scheme { http, https };
		public Scheme scheme;
		public String hostname;
		public int port;
		public String authUsername;
		public String authPassword;
	}
	public static interface ExchangeUserResolver {
		public ServerInfo getServerInfo(String emailAddr);
	}
	
	private static List<ExchangeUserResolver> mResolvers;
	
	static {
		mResolvers = new ArrayList<ExchangeUserResolver>();
	}

	public static void registerResolver(ExchangeUserResolver r, int priority) {
		mResolvers.set(priority, r);
	}
	public FreeBusy getFreeBusy(String emailAddr, long start, long end) {
		ServerInfo serverInfo = null;
		for (ExchangeUserResolver r : mResolvers) {
			serverInfo = r.getServerInfo(emailAddr);
			if (serverInfo != null)
				break;
		}
		if (serverInfo == null) {
			ZimbraLog.misc.warn("no exchange server info for user "+emailAddr);
			return LocalFreeBusyProvider.createDummyFreeBusy(start, end);
		}
		String url = constructUrl(serverInfo, emailAddr, start, end);
		ZimbraLog.misc.debug("fetching fb from url="+url);
		HttpMethod method = new GetMethod(url);
		method.setDoAuthentication(true);
		method.setRequestHeader("User-Agent", USER_AGENT);
		HttpState state = new HttpState();
		state.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(serverInfo.authUsername, serverInfo.authPassword));
		HttpClient client = new HttpClient();
		client.setState(state);
		Element response = null;
		try {
			int status = client.executeMethod(method);
			Header cl = method.getResponseHeader("Content-Length");
			if (status != 200 || cl == null)
				return LocalFreeBusyProvider.createDummyFreeBusy(start, end);
			response = Element.parseXML(method.getResponseBodyAsStream());
		} catch (DocumentException e) {
			ZimbraLog.misc.warn("error parsing fb response from exchange", e);
			return LocalFreeBusyProvider.createDummyFreeBusy(start, end);
		} catch (IOException e) {
			ZimbraLog.misc.warn("error parsing fb response from exchange", e);
			return LocalFreeBusyProvider.createDummyFreeBusy(start, end);
		}
		return new ExchangeUserFreeBusy(response, emailAddr, FB_INTERVAL, start, end);
	}
	
	private String constructUrl(ServerInfo info, String emailAddr, long start, long end) {
		// http://exchange/public/?params..
		//   cmd      = freebusy
		//   start    = [ISO8601date]
		//   end      = [ISO8601date]
		//   interval = [minutes]
		//   u        = SMTP:[emailAddr]
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
		buf.append("/public/?cmd=freebusy");
		buf.append("&start=").append(DateUtil.toISO8601(new Date(start)));
		buf.append("&endt=").append(DateUtil.toISO8601(new Date(end)));
		buf.append("&interval=").append(FB_INTERVAL);
		buf.append("&u=SMTP:").append(emailAddr);
		return buf.toString();
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
        
	    protected ExchangeUserFreeBusy(Element fbxml, String emailAddr, int interval, long start, long end) {
	    	super(start, end);
	    	parseInterval(fbxml, emailAddr, interval, start, end);
	    }
	    private void parseInterval(Element fbxml, String emailAddr, int interval, long start, long end) {
	    	long intervalInMillis = interval * 30L * 60L * 1000L;
	    	String fb = getFbString(fbxml, emailAddr);
	    	for (int i = 0; i < fb.length(); i++) {
	    		start += intervalInMillis;
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
	    	}
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
	}
}
