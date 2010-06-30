/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.dav.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.DomUtil;
import com.zimbra.cs.dav.DavContext.KnownUserAgent;
import com.zimbra.cs.dav.service.method.*;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.calendar.cache.AccountCtags;
import com.zimbra.cs.mailbox.calendar.cache.AccountKey;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.mailbox.calendar.cache.CtagInfo;
import com.zimbra.cs.mailbox.calendar.cache.CtagResponseCache;
import com.zimbra.cs.mailbox.calendar.cache.CtagResponseCache.CtagResponseCacheKey;
import com.zimbra.cs.mailbox.calendar.cache.CtagResponseCache.CtagResponseCacheValue;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

@SuppressWarnings("serial")
public class DavServlet extends ZimbraServlet {

	public static final String DAV_PATH = "/dav";
	
	private static Map<String, DavMethod> sMethods;
	
	public void init() throws ServletException {
		super.init();
		sMethods = new HashMap<String, DavMethod>();
		addMethod(new Copy());
		addMethod(new Delete());
		addMethod(new Get());
		addMethod(new Head());
		addMethod(new Lock());
		addMethod(new MkCol());
		addMethod(new Move());
		addMethod(new Options());
		addMethod(new Post());
		addMethod(new Put());
		addMethod(new PropFind());
		addMethod(new PropPatch());
		addMethod(new Unlock());
		addMethod(new MkCalendar());
		addMethod(new Report());
		addMethod(new Acl());
	}

	protected void addMethod(DavMethod method) {
		sMethods.put(method.getName(), method);
	}
	
	public static void setAllowHeader(HttpServletResponse resp) {
		Set<String> methods = sMethods.keySet();
		StringBuilder buf = new StringBuilder();
		for (String method : methods) {
			if (buf.length() > 0)
				buf.append(", ");
			buf.append(method);
		}
		DavMethod.setResponseHeader(resp, DavProtocol.HEADER_ALLOW, buf.toString());
	}
	
	enum RequestType { password, authtoken, both, none };
	
    private RequestType getAllowedRequestType(HttpServletRequest req) {
    	if (!super.isRequestOnAllowedPort(req))
    		return RequestType.none;
    	Server server = null;
    	try {
    		server = Provisioning.getInstance().getLocalServer();
    	} catch (Exception e) {
    		return RequestType.none;
    	}
    	boolean allowPassword = server.getBooleanAttr(Provisioning.A_zimbraCalendarCalDavClearTextPasswordEnabled, true);
    	int sslPort = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, 443);
    	int mailPort = server.getIntAttr(Provisioning.A_zimbraMailPort, 80);
    	int incomingPort = req.getLocalPort();
    	if (incomingPort == sslPort)
    		return RequestType.both;
    	else if (incomingPort == mailPort && allowPassword)
    		return RequestType.both;
    	else
    		return RequestType.authtoken;
    }
    
	public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ZimbraLog.clearContext();
		addRemoteIpToLoggingContext(req);
		ZimbraLog.addUserAgentToContext(req.getHeader(DavProtocol.HEADER_USER_AGENT));

		RequestType rtype = getAllowedRequestType(req);
		
		if (rtype == RequestType.none) {
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
			return;
		}

		/*
		if (ZimbraLog.dav.isDebugEnabled()) {
			java.util.Enumeration en = req.getHeaderNames();
			while (en.hasMoreElements()) {
				String n = (String)en.nextElement();
				java.util.Enumeration vals = req.getHeaders(n);
				while (vals.hasMoreElements()) {
					String v = (String)vals.nextElement();
		        	ZimbraLog.dav.debug("HEADER "+n+": "+v);
				}
			}
		}
		*/
        Account authUser = null;
		DavContext ctxt;
		try {
            AuthToken at = AuthProvider.getAuthToken(req, false);
            if (at != null && at.isExpired())
                at = null;
            if (at != null && (rtype == RequestType.both || rtype == RequestType.authtoken))
            	authUser = Provisioning.getInstance().get(AccountBy.id, at.getAccountId());
            else if (at == null && (rtype == RequestType.both || rtype == RequestType.password))
    			authUser = basicAuthRequest(req, resp, true);
			if (authUser == null) {
				try {
					resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
				} catch (Exception e) {}
				return;
			}
			ZimbraLog.addToContext(ZimbraLog.C_ANAME, authUser.getName());
			ctxt = new DavContext(req, resp, authUser);
		} catch (AuthTokenException e) {
			ZimbraLog.dav.error("error getting authenticated user", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		} catch (ServiceException e) {
			ZimbraLog.dav.error("error getting authenticated user", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		DavMethod method = sMethods.get(req.getMethod());
		if (method == null) {
			setAllowHeader(resp);
			resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}

        long t0 = System.currentTimeMillis();
        if (ZimbraLog.dav.isDebugEnabled()) {
            try {
                Upload upload = ctxt.getUpload();
                if (upload.getSize() > 0 && upload.getContentType().startsWith("text")) {
                    ZimbraLog.dav.debug("REQUEST:\n"+new String(ByteUtil.readInput(upload.getInputStream(), -1, 2048), "UTF-8"));
                }
            } catch (Exception e) {
                ZimbraLog.dav.debug("ouch", e);
            }
        }

        CacheStates cache = null;
        try {
        	cache = checkCachedResponse(ctxt, authUser);
    		if (!ctxt.isResponseSent() && !isProxyRequest(ctxt, method)) {
		
    			method.checkPrecondition(ctxt);
    			method.handle(ctxt);
    			method.checkPostcondition(ctxt);
    			if (!ctxt.isResponseSent())
    				resp.setStatus(ctxt.getStatus());
    		}
		} catch (DavException e) {
			if (e.getCause() instanceof MailServiceException.NoSuchItemException ||
					e.getStatus() == HttpServletResponse.SC_NOT_FOUND)
				ZimbraLog.dav.info(ctxt.getUri()+" not found");
			else if (e.getStatus() == HttpServletResponse.SC_MOVED_TEMPORARILY ||
					 e.getStatus() == HttpServletResponse.SC_MOVED_PERMANENTLY) 
				ZimbraLog.dav.info("sending redirect");
			
			try {
				if (e.isStatusSet()) {
					resp.setStatus(e.getStatus());
					if (e.hasErrorMessage())
						e.writeErrorMsg(resp.getOutputStream());
	                ZimbraLog.dav.info("sending http error %d because: %s", e.getStatus(), e.getMessage());
	                if (e.getCause() != null)
	                    ZimbraLog.dav.debug("exception: ", e.getCause());
				} else {
					ZimbraLog.dav.error("error handling method "+method.getName(), e);
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			} catch (IllegalStateException ise) {
                ZimbraLog.dav.debug("can't write error msg", ise);
			}
		} catch (ServiceException e) {
			if (e instanceof MailServiceException.NoSuchItemException) {
				ZimbraLog.dav.info(ctxt.getUri()+" not found");
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			ZimbraLog.dav.error("error handling method "+method.getName(), e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			ZimbraLog.dav.error("error handling method "+method.getName(), e);
			try {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} catch (Exception ex) {}
		} finally {
            long t1 = System.currentTimeMillis();
            ZimbraLog.dav.info("DavServlet operation "+method.getName()+" to "+req.getPathInfo()+" (depth: "+ctxt.getDepth().name()+") finished in "+(t1-t0)+"ms");
			cacheCleanUp(ctxt, cache);
		    ctxt.cleanup();
		}
	}
	
	public static String getDavUrl(String user) throws DavException, ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, user);
        if (account == null)
			throw new DavException("unknown user "+user, HttpServletResponse.SC_NOT_FOUND, null);
        return getServiceUrl(account, DAV_PATH);
	}

	@SuppressWarnings("unchecked")
    private boolean isCtagRequest(DavContext ctxt) throws DavException {
	    String httpMethod = ctxt.getRequest().getMethod();
	    if (PropFind.PROPFIND.equalsIgnoreCase(httpMethod)) {
    	    Document doc = ctxt.getRequestMessage();
            Element top = doc.getRootElement();
            if (top == null || !top.getQName().equals(DavElements.E_PROPFIND))
                return false;
            Element prop = top.element(DavElements.E_PROP);
            if (prop == null)
                return false;
            Iterator iter = prop.elementIterator();
            while (iter.hasNext()) {
                prop = (Element) iter.next();
                if (prop.getQName().equals(DavElements.E_GETCTAG))
                    return true;
            }
	    }
        return false;
    }
	
	private static class CacheStates {
        boolean ctagCacheEnabled = MemcachedConnector.isConnected();
        boolean gzipAccepted = false;
        boolean cacheThisCtagResponse = false;
        CtagResponseCacheKey ctagCacheKey = null;
        String acctVerSnapshot = null;
        Map<Integer /* calendar folder id */, String /* ctag */> ctagsSnapshot = null;
        CtagResponseCache ctagResponseCache = null;
	}
	
	private CacheStates checkCachedResponse(DavContext ctxt, Account authUser) throws IOException, DavException, ServiceException {
		CacheStates cache = new CacheStates();
		
        // Are we running with cache enabled, and is this a cachable CalDAV ctag request?
		if (cache.ctagCacheEnabled && isCtagRequest(ctxt)) {
			cache.ctagResponseCache = CalendarCacheManager.getInstance().getCtagResponseCache();
			cache.gzipAccepted = ctxt.isGzipAccepted();
		    String targetUser = ctxt.getUser();
		    Account targetAcct = Provisioning.getInstance().get(AccountBy.name, targetUser);
		    boolean ownAcct = targetAcct != null && targetAcct.getId().equals(authUser.getId());
		    String parentPath = ctxt.getPath();
	        KnownUserAgent knownUA = ctxt.getKnownUserAgent();
	        // Use cache only when requesting own account and User-Agent and path are well-defined.
		    if (ownAcct && knownUA != null && parentPath != null) {
		        AccountKey accountKey = new AccountKey(targetAcct.getId());
		        AccountCtags allCtagsData = CalendarCacheManager.getInstance().getCtags(accountKey);
		        // We can't use cache if it doesn't have data for this user.
		        if (allCtagsData != null) {
		            boolean validRoot = true;
                    int rootFolderId = Mailbox.ID_FOLDER_USER_ROOT;
    		        if (!"/".equals(parentPath)) {
    		            CtagInfo calInfoRoot = allCtagsData.getByPath(parentPath);
    		            if (calInfoRoot != null)
    		                rootFolderId = calInfoRoot.getId();
    		            else
    		                validRoot = false;
    		        }
    		        if (validRoot) {
    		            // Is there a previously cached response?
    		        	cache.ctagCacheKey = new CtagResponseCacheKey(targetAcct.getId(), knownUA.toString(), rootFolderId);
                        CtagResponseCacheValue ctagResponse = cache.ctagResponseCache.get(cache.ctagCacheKey);
                        if (ctagResponse != null) {
                            // Found a cached response.  Let's check if it's stale.
                            // 1. If calendar list has been updated since, cached response is no good.
                            String currentCalListVer = allCtagsData.getVersion();
                            if (currentCalListVer.equals(ctagResponse.getVersion())) {
                                // 2. We have to examine ctags of individual calendars.
                                boolean cacheHit = true;
                                Map<Integer, String> oldCtags = ctagResponse.getCtags();
                                // We're good if ctags from before are unchanged.
                                for (Map.Entry<Integer, String> entry : oldCtags.entrySet()) {
                                    int calFolderId = entry.getKey();
                                    String ctag = entry.getValue();
                                    CtagInfo calInfoCurr = allCtagsData.getById(calFolderId);
                                    if (calInfoCurr == null) {
                                        // Just a sanity check.  The cal list version check should have
                                        // already taken care of added/removed calendars.
                                        cacheHit = false;
                                        break;
                                    }
                                    if (!ctag.equals(calInfoCurr.getCtag())) {
                                        // A calendar has been modified.  Stale!
                                        cacheHit = false;
                                        break;
                                    }
                                }
                		        if (cacheHit) {
                                    ZimbraLog.dav.debug("CTAG REQUEST CACHE HIT");
                		            // All good.  Send cached response.
                                    ctxt.setStatus(DavProtocol.STATUS_MULTI_STATUS);
                		            HttpServletResponse response = ctxt.getResponse();
                		            response.setStatus(ctxt.getStatus());
                		            response.setContentType(DavProtocol.DAV_CONTENT_TYPE);
                                    byte[] respData = ctagResponse.getResponseBody();
                		            response.setContentLength(ctagResponse.getRawLength());

                		            byte[] unzipped = null;
                		            if (ZimbraLog.dav.isDebugEnabled() || (ctagResponse.isGzipped() && !cache.gzipAccepted)) {
                		                if (ctagResponse.isGzipped()) {
                                            ByteArrayInputStream bais = new ByteArrayInputStream(respData);
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            GZIPInputStream gzis = null;
                                            try {
                                                gzis = new GZIPInputStream(bais, respData.length);
                                                ByteUtil.copy(gzis, false, baos, true);
                                            } finally {
                                                ByteUtil.closeStream(gzis);
                                            }
                                            unzipped = baos.toByteArray();
                		                } else {
                		                    unzipped = respData;
                		                }
                		                if (ZimbraLog.dav.isDebugEnabled())
                                            ZimbraLog.dav.debug("RESPONSE:\n" + new String(unzipped, "UTF-8"));
                		            }
                		            if (!ctagResponse.isGzipped()) {
                                        response.getOutputStream().write(respData);
                		            } else {
                		                if (cache.gzipAccepted) {
                                            response.addHeader(DavProtocol.HEADER_CONTENT_ENCODING, DavProtocol.ENCODING_GZIP);
                                            response.getOutputStream().write(respData);
                		                } else {
                		                    assert(unzipped != null);
                                            response.getOutputStream().write(unzipped);
                		                }
                		            }

                		            // Tell the context the response has been sent.
                		            ctxt.responseSent();
                		        }
                            }
                        }

                        if (!ctxt.isResponseSent()) {
                            // Cache miss, or cached response is stale.  We're gonna have to generate the
                            // response the hard way.  Capture a snapshot of current state of calendars
                            // to attach to the response to be cached later.
                        	cache.cacheThisCtagResponse = true;
                        	cache.acctVerSnapshot = allCtagsData.getVersion();
                        	cache.ctagsSnapshot = new HashMap<Integer, String>();
                            Collection<CtagInfo> childCals = allCtagsData.getChildren(rootFolderId);
                            if (rootFolderId != Mailbox.ID_FOLDER_USER_ROOT) {
                                CtagInfo ctagRoot = allCtagsData.getById(rootFolderId);
                                if (ctagRoot != null)
                                	cache.ctagsSnapshot.put(rootFolderId, ctagRoot.getCtag());
                            }
                            for (CtagInfo calInfo : childCals) {
                            	cache.ctagsSnapshot.put(calInfo.getId(), calInfo.getCtag());
                            }
                        }
    		        }
		        }
                if (!ctxt.isResponseSent())
                    ZimbraLog.dav.debug("CTAG REQUEST CACHE MISS");
		    }
		}
		return cache;
	}
	private void cacheCleanUp(DavContext ctxt, CacheStates cache) throws IOException {
	    if (cache.ctagCacheEnabled && cache.cacheThisCtagResponse && ctxt.getStatus() == DavProtocol.STATUS_MULTI_STATUS) {
	        assert(cache.ctagCacheKey != null && cache.acctVerSnapshot != null && !cache.ctagsSnapshot.isEmpty());
	        DavResponse dresp = ctxt.getDavResponse();
            ByteArrayOutputStream baosRaw = null;
            try {
                baosRaw = new ByteArrayOutputStream();
                dresp.writeTo(baosRaw);
            } finally {
                ByteUtil.closeStream(baosRaw);
            }
            byte[] respData = baosRaw.toByteArray();
            int rawLen = respData.length;

            boolean forceGzip = true;
            // Cache gzipped response if client supports it.
            boolean responseGzipped = forceGzip || cache.gzipAccepted;
            if (responseGzipped) {
                ByteArrayOutputStream baosGzipped = new ByteArrayOutputStream();
                GZIPOutputStream gzos = null;
                try {
                    gzos = new GZIPOutputStream(baosGzipped);
                    gzos.write(respData);
                } finally {
                    ByteUtil.closeStream(gzos);
                }
                respData = baosGzipped.toByteArray();
            }

            CtagResponseCacheValue ctagCacheVal = new CtagResponseCacheValue(
	                respData, rawLen, responseGzipped, cache.acctVerSnapshot, cache.ctagsSnapshot);
	        try {
	        	cache.ctagResponseCache.put(cache.ctagCacheKey, ctagCacheVal);
            } catch (ServiceException e) {
                ZimbraLog.dav.warn("Unable to cache ctag response", e);
                // No big deal if we can't cache the response.  Just move on.
            }
	    }
	}
	
	private static String[] PROXY_HEADERS = {
		DavProtocol.HEADER_DAV,
		DavProtocol.HEADER_DEPTH,
		DavProtocol.HEADER_CONTENT_TYPE,
		DavProtocol.HEADER_ETAG,
		DavProtocol.HEADER_IF_MATCH,
        DavProtocol.HEADER_DESTINATION
	};
	
	private boolean isProxyRequest(DavContext ctxt, DavMethod m) throws IOException, DavException, ServiceException {
		Provisioning prov = Provisioning.getInstance();
		ItemId target = null;
		String extraPath = null;
		String requestPath = ctxt.getPath();
		try {
		    if (ctxt.getUser() == null)
		        return false;
		    if (requestPath == null || requestPath.length() < 2)
		        return false;
			Account account = prov.getAccountByName(ctxt.getUser());
			if (account == null)
				return false;
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			Pair<Folder, String> match = mbox.getFolderByPathLongestMatch(ctxt.getOperationContext(), Mailbox.ID_FOLDER_USER_ROOT, requestPath);
			Folder targetFolder = match.getFirst();
			if (!(targetFolder instanceof Mountpoint))
				return false;
			Mountpoint mp = (Mountpoint) targetFolder;
			target = new ItemId(mp.getOwnerId(), mp.getRemoteId());
			extraPath = match.getSecond();
		} catch (ServiceException e) {
			ZimbraLog.dav.debug("can't get path", e);
			return false;
		}
		
		// we don't proxy zero depth PROPFIND, and all PROPPATCH on mountpoints,
		// because the mountpoint object contains WebDAV properties that are
		// private to the user.
		// we also don't proxy DELETE on a mountpoint.
		if (extraPath == null 
			&& (m.getName().equals(PropFind.PROPFIND) && ctxt.getDepth() == DavContext.Depth.zero
				|| m.getName().equals(PropPatch.PROPPATCH)
				|| m.getName().equals(Delete.DELETE)))
			return false;
		
		String prefix = ctxt.getPath();
		if (extraPath != null)
			prefix = prefix.substring(0, prefix.indexOf(extraPath));
		prefix = URLUtil.urlEscape(DAV_PATH + "/" + ctxt.getUser() + prefix);
		
		// make sure the target account exists.
		Account acct = prov.getAccountById(target.getAccountId());
		if (acct == null)
		    return false;
		Server server = prov.getServer(acct);
		if (server == null)
            return false;

		// get the path to the target mail item
        AuthToken authToken = AuthProvider.getAuthToken(ctxt.getAuthAccount());
        ZMailbox.Options zoptions = new ZMailbox.Options(authToken.toZAuthToken(), AccountUtil.getSoapUri(acct));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(target.getAccountId());
        zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
        ZFolder f = zmbx.getFolderById("" + target.toString());
        if (f == null)
            return false;
        String path = f.getPath();
    	String newPrefix = URLUtil.urlEscape(DAV_PATH + "/" + acct.getName() + f.getPath());

        if (ctxt.hasRequestMessage()) {
            // replace the path in <href> of the request with the path to the target mail item.
            Document req = ctxt.getRequestMessage();
            for (Object hrefObj : req.getRootElement().elements(DavElements.E_HREF)) {
            	if (!(hrefObj instanceof Element))
            		continue;
            	Element href = (Element) hrefObj;
            	String v = href.getText();
            	// prefix matching is not as straightforward as we have jetty redirect from /dav to /home/dav.
            	href.setText(newPrefix + "/" + v.substring(v.lastIndexOf('/')+1));
            }
        }
        
        // build proxy request
		String url = getProxyUrl(ctxt.getRequest(), server, DAV_PATH) + URLUtil.urlEscape("/" + acct.getName() + path + "/" + (extraPath == null ? "" : extraPath));
		HttpState state = new HttpState();
        authToken.encode(state, false, server.getAttr(Provisioning.A_zimbraServiceHostname));
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        client.setState(state);
        HttpMethod method = m.toHttpMethod(ctxt, url);
        for (String h : PROXY_HEADERS) {
            String hval = ctxt.getRequest().getHeader(h);
            if (hval != null)
            	method.addRequestHeader(h, hval);
        }
        int statusCode = HttpClientUtil.executeMethod(client, method);
        ctxt.getResponse().setStatus(statusCode);
        ctxt.setStatus(statusCode);
    	InputStream in = method.getResponseBodyAsStream();
        switch (statusCode) {
        case DavProtocol.STATUS_MULTI_STATUS:
        	// rewrite the <href> element in the response to point to local mountpoint.
        	try {
        		Document response = com.zimbra.common.soap.Element.getSAXReader().read(in);
        		Element top = response.getRootElement();
        		for (Object responseObj : top.elements(DavElements.E_RESPONSE)) {
        			if (!(responseObj instanceof Element))
        				continue;
                	Element href = ((Element)responseObj).element(DavElements.E_HREF);
                	String v = href.getText();
                	if (v.startsWith(newPrefix))
                		href.setText(prefix + v.substring(newPrefix.length()+1));
        		}
        		if (ZimbraLog.dav.isDebugEnabled())
        			ZimbraLog.dav.debug("PROXY RESPONSE:\n"+new String(DomUtil.getBytes(response), "UTF-8"));
        		DomUtil.writeDocumentToStream(response, ctxt.getResponse().getOutputStream());
	            ctxt.responseSent();
        	} catch (DocumentException e) {
        		ZimbraLog.dav.warn("proxy request failed", e);
        		return false;
        	}
        	break;
        default:
        	if (in != null)
        		ByteUtil.copy(in, true, ctxt.getResponse().getOutputStream(), false);
        	ctxt.responseSent();
        	break;
        }
		return true;
	}
}
