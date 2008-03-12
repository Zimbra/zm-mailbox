/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
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
		String auth;
		public DeployThread(Upload up, Progress pr, String au) {
			upload = up;
			progress = pr;
			auth = au;
		}
		public void run() {
			Server s = null;
			try {
				s = Provisioning.getInstance().getLocalServer();
				ZimletUtil.deployZimlet(new ZimletFile(upload.getName(), upload.getInputStream()), progress, auth);
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
	
	private void deploy(ZimbraSoapContext lc, String aid, String auth) throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), aid, lc.getAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(aid);

        Progress pr = new Progress((auth != null));
        mProgressMap.put(aid, pr);
        Runnable action = new DeployThread(up, pr, auth);
        new Thread(action).start();
	}
	
	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
		String action = request.getAttribute(AdminConstants.A_ACTION).toLowerCase();
		Element content = request.getElement(MailConstants.E_CONTENT);
		String aid = content.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
		
		if (action.equals(AdminConstants.A_STATUS)) {
			// just print the status
		} else if (action.equals(AdminConstants.A_DEPLOYALL)) {
			deploy(lc, aid, lc.getRawAuthTokenString());
		} else if (action.equals(AdminConstants.A_DEPLOYLOCAL)) {
			deploy(lc, aid, null);
		} else {
			throw ServiceException.INVALID_REQUEST("invalid action "+action, null);
		}
		Element response = lc.createElement(AdminConstants.DEPLOY_ZIMLET_RESPONSE);
		Progress progress = (Progress)mProgressMap.get(aid);
		if (progress != null)
			progress.writeResponse(response);
		return response;
	}
}
