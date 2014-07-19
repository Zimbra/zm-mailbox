/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.MapUtil;
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
	
	private Map<String, Progress> mProgressMap;

	private static class Progress implements DeployListener {
	    private static class Status {
	        String value;
	        Exception error;
	    }
		private Map<String,Status> mStatus;
		
		public Progress(boolean allServers) throws ServiceException {
			mStatus = new HashMap<String,Status>();
			Provisioning prov = Provisioning.getInstance();
			if (!allServers) {
				changeStatus(prov.getLocalServer().getName(), sPENDING);
				return;
			}
			for (Server s : prov.getAllDeployableZimletServers()) {
			    changeStatus(s.getName(), sPENDING);
            }
		}
		public void markFinished(Server s) {
			changeStatus(s.getName(), sSUCCEEDED);
		}
		public void markFailed(Server s, Exception e) {
			changeStatus(s.getName(), sFAILED);
			mStatus.get(s.getName()).error = e;
		}
		public void changeStatus(String name, String status) {
		    Status s = mStatus.get(name);
		    if (s == null) {
                s = new Status();
                mStatus.put(name, s);
		    }
		    s.value = status;
		}
		public void writeResponse(Element resp) {
			for (Map.Entry<String, Status> entry : mStatus.entrySet()) {
				Element progress = resp.addElement(AdminConstants.E_PROGRESS);
				progress.addAttribute(AdminConstants.A_SERVER, entry.getKey());
				progress.addAttribute(AdminConstants.A_STATUS, entry.getValue().value);
				Exception e = entry.getValue().error;
				if (e != null) {
	                progress.addAttribute(AdminConstants.A_ERROR, e.getMessage());
				}
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
					progress.markFailed(s, e);
			} finally {
				FileUploadServlet.deleteUpload(upload);
			}
		}
	}
	
	public DeployZimlet() {
		// keep past 20 zimlet deployment progresses
		mProgressMap = MapUtil.newLruMap(20);
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
		    List<Server> servers =
		        Provisioning.getInstance().getAllDeployableZimletServers();

		    for (Server server : servers) {
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
		Progress progress = mProgressMap.get(aid);
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
