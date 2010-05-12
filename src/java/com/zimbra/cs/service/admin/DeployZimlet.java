/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.zimlet.ZimletFile;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.cs.zimlet.ZimletUtil.DeployListener;
import com.zimbra.soap.ZimbraSoapContext;

public class DeployZimlet extends AdminDocumentHandler {

	public static final String sPENDING = "pending";
	public static final String sSUCCEEDED = "succeeded";
	public static final String sFAILED = "failed";
	
	private LRUMap mProgressMap;

	private static class Progress implements DeployListener {
		private Map<String,String> mStatus;
		
		public Progress(boolean allServers) throws ServiceException {
			mStatus = new HashMap<String,String>();
			Provisioning prov = Provisioning.getInstance();
			if (!allServers) {
				mStatus.put(prov.getLocalServer().getName(), sPENDING);
				return;
			}
			List<Server> servers = prov.getAllServers();
			for (Server s : servers) {
			    boolean hasMailboxService = s.getMultiAttrSet(Provisioning.A_zimbraServiceEnabled).contains("mailbox");
			    if (hasMailboxService)
			        mStatus.put(s.getName(), sPENDING);
            }
		}
		public void markFinished(Server s) {
			mStatus.put(s.getName(), sSUCCEEDED);
		}
		public void markFailed(Server s) {
			mStatus.put(s.getName(), sFAILED);
		}
		public void writeResponse(Element resp) {
			Set<Map.Entry<String,String>> entries = mStatus.entrySet();
			for (Map.Entry<String, String> entry : entries) {
				Element progress = resp.addElement(AdminConstants.E_PROGRESS);
				progress.addAttribute(AdminConstants.A_SERVER, entry.getKey());
				progress.addAttribute(AdminConstants.A_STATUS, entry.getValue());
			}
		}
	}
	
	private static class DeployThread implements Runnable {
		Upload upload;
		Progress progress;
		ZAuthToken auth;
		boolean flushCache;
		public DeployThread(Upload up, Progress pr, ZAuthToken au, boolean flush) {
			upload = up;
			progress = pr;
			auth = au;
			flushCache = flush;
		}
		public void run() {
			Server s = null;
			try {
				s = Provisioning.getInstance().getLocalServer();
				ZimletFile zf = new ZimletFile(upload.getName(), upload.getInputStream());
				ZimletUtil.deployZimlet(zf, progress, auth, flushCache);
			} catch (Exception e) {
				ZimbraLog.zimlet.info("deploy", e);
				if (s != null)
					progress.markFailed(s);
			} finally {
				FileUploadServlet.deleteUpload(upload);
			}
		}
	}
	
	public DeployZimlet() {
		// keep past 20 zimlet deployment progresses
		mProgressMap = new LRUMap(20);
	}
	
	private void deploy(ZimbraSoapContext lc, String aid, ZAuthToken auth, boolean flushCache, boolean synchronous) throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), aid, lc.getAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(aid);

        Progress pr = new Progress((auth != null));
        mProgressMap.put(aid, pr);
        Runnable action = new DeployThread(up, pr, auth, flushCache);
        Thread t = new Thread(action);
        t.start();
        if (synchronous) {
            try {
                t.join(DEPLOY_TIMEOUT);
            } catch (InterruptedException e) {
                ZimbraLog.zimlet.warn("error while deploying Zimlet", e);
            }
        }
	}
	
	private static final long DEPLOY_TIMEOUT = 10000;
	
	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
	    ZimbraSoapContext zsc = getZimbraSoapContext(context);
		String action = request.getAttribute(AdminConstants.A_ACTION).toLowerCase();
		Element content = request.getElement(MailConstants.E_CONTENT);
		String aid = content.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
		boolean flushCache = request.getAttributeBool(AdminConstants.A_FLUSH, false);
        boolean synchronous = request.getAttributeBool(AdminConstants.A_SYNCHRONOUS, false);
		if (action.equals(AdminConstants.A_STATUS)) {
			// just print the status
		} else if (action.equals(AdminConstants.A_DEPLOYALL)) {
		    
		    for (Server server : Provisioning.getInstance().getAllServers()) {
		    	checkRight(zsc, context, server, Admin.R_deployZimlet);
		    }
		        
			deploy(zsc, aid, zsc.getRawAuthToken(), flushCache, synchronous);
			if(flushCache) {
				if (ZimbraLog.misc.isDebugEnabled()) {
					ZimbraLog.misc.debug("DeployZimlet: flushing zimlet cache");
				}				
				checkRight(zsc, context, Provisioning.getInstance().getLocalServer(), Admin.R_flushCache);
				FlushCache.sendFlushRequest(context, "/service", "/zimlet/res/all.js");
			}

		} else if (action.equals(AdminConstants.A_DEPLOYLOCAL)) {
		    
		    Server localServer = Provisioning.getInstance().getLocalServer();
		    checkRight(zsc, context, localServer, Admin.R_deployZimlet);
		    
			deploy(zsc, aid, null, false, synchronous);
			
			if(flushCache) {
				if (ZimbraLog.misc.isDebugEnabled()) {
					ZimbraLog.misc.debug("DeployZimlet: flushing zimlet cache");
				}								
				checkRight(zsc, context, localServer, Admin.R_flushCache);
				FlushCache.sendFlushRequest(context, "/service", "/zimlet/res/all.js");
			}
		} else {
			throw ServiceException.INVALID_REQUEST("invalid action "+action, null);
		}
		Element response = zsc.createElement(AdminConstants.DEPLOY_ZIMLET_RESPONSE);
		Progress progress = (Progress)mProgressMap.get(aid);
		if (progress != null)
			progress.writeResponse(response);
		return response;
	}
	
	@Override
	public void docRights(List<AdminRight> relatedRights, List<String> notes) {
	    relatedRights.add(Admin.R_deployZimlet);
	    
	    notes.add("If deploying on all servers, need the " + Admin.R_deployZimlet.getName() + 
	            " right on all servers or on global grant.  If deploying on local server, need " +
	            "the " + Admin.R_deployZimlet.getName() + " on the local server.");
    }
}
