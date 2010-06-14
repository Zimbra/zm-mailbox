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

/*
 * Created on May 29, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMountpoint;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;

public class ItemAction extends MailDocumentHandler {

	protected static final String[] OPERATION_PATH = new String[] { MailConstants.E_ACTION, MailConstants.A_OPERATION };
	protected static final String[] TARGET_ITEM_PATH = new String[] { MailConstants.E_ACTION, MailConstants.A_ID };
	
	
	public static final String OP_TAG         = "tag";
	public static final String OP_FLAG        = "flag";
	public static final String OP_READ        = "read";
	public static final String OP_COLOR       = "color";
	public static final String OP_HARD_DELETE = "delete";
    public static final String OP_MOVE        = "move";
    public static final String OP_COPY        = "copy";
    public static final String OP_SPAM        = "spam";
    public static final String OP_TRASH       = "trash";
    public static final String OP_RENAME      = "rename";
    public static final String OP_UPDATE      = "update";

    @Override public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element action = request.getElement(MailConstants.E_ACTION);
        String operation = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();

        String successes = handleCommon(context, request, operation, MailItem.TYPE_UNKNOWN);

        Element response = zsc.createElement(MailConstants.ITEM_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailConstants.E_ACTION);
        act.addAttribute(MailConstants.A_ID, successes);
        act.addAttribute(MailConstants.A_OPERATION, operation);
        return response;
    }

    protected String handleCommon(Map<String,Object> context, Element request, String opAttr, byte type) throws ServiceException {
        Element action = request.getElement(MailConstants.E_ACTION);
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        SoapProtocol responseProto = zsc.getResponseProtocol();

        // determine the requested operation
        String opStr;
        boolean flagValue = true;
        if (opAttr.length() > 1 && opAttr.startsWith("!")) {
            flagValue = false;
            opStr = opAttr.substring(1);
        } else {
            opStr = opAttr;
        }

        // figure out which items are local and which ones are remote, and proxy accordingly
        List<Integer> local = new ArrayList<Integer>();
        Map<String, StringBuffer> remote = new HashMap<String, StringBuffer>();
        partitionItems(zsc, action.getAttribute(MailConstants.A_ID), local, remote);
        if (remote.isEmpty() && local.isEmpty())
            return "";

        // for moves/copies, make sure that we're going to receive notifications from the target folder
        Account remoteNotify = forceRemoteSession(zsc, context, octxt, opStr, action);

        // handle referenced items living on other servers
        StringBuffer successes = proxyRemoteItems(action, remote, request, context);

        // handle referenced items living on this server
        if (!local.isEmpty()) {
        	String constraint = action.getAttribute(MailConstants.A_TARGET_CONSTRAINT, null);
        	TargetConstraint tcon = TargetConstraint.parseConstraint(mbox, constraint);
        	
        	String localResults;
        	
        	// set additional parameters (depends on op type)
        	if (opStr.equals(OP_TAG)) {
        		int tagId = (int) action.getAttributeLong(MailConstants.A_TAG);
        		localResults = ItemActionHelper.TAG(octxt, mbox, responseProto, local, type, flagValue, tcon, tagId).getResult();
        	} else if (opStr.equals(OP_FLAG)) {
        		localResults = ItemActionHelper.FLAG(octxt, mbox, responseProto, local, type, flagValue, tcon).getResult();
        	} else if (opStr.equals(OP_READ)) {
        		localResults = ItemActionHelper.READ(octxt, mbox, responseProto, local, type, flagValue, tcon).getResult();
        	} else if (opStr.equals(OP_COLOR)) {
        	    MailItem.Color color = getColor(action);
        		localResults = ItemActionHelper.COLOR(octxt, mbox, responseProto, local, type, tcon, color).getResult();
        	} else if (opStr.equals(OP_HARD_DELETE)) {
        		localResults = ItemActionHelper.HARD_DELETE(octxt, mbox, responseProto, local, type, tcon).getResult();
            } else if (opStr.equals(OP_MOVE)) {
                ItemId iidFolder = new ItemId(action.getAttribute(MailConstants.A_FOLDER), zsc);
                localResults = ItemActionHelper.MOVE(octxt, mbox, responseProto, local, type, tcon, iidFolder).getResult();
            } else if (opStr.equals(OP_COPY)) {
                ItemId iidFolder = new ItemId(action.getAttribute(MailConstants.A_FOLDER), zsc);
                localResults = ItemActionHelper.COPY(octxt, mbox, responseProto, local, type, tcon, iidFolder).getResult();
        	} else if (opStr.equals(OP_SPAM)) {
        		String defaultFolder = (flagValue ? Mailbox.ID_FOLDER_SPAM : Mailbox.ID_FOLDER_INBOX) + "";
                ItemId iidFolder = new ItemId(action.getAttribute(MailConstants.A_FOLDER, defaultFolder), zsc);
        		localResults = ItemActionHelper.SPAM(octxt, mbox, responseProto, local, type, flagValue, tcon, iidFolder).getResult();
            } else if (opStr.equals(OP_TRASH)) {
                localResults = ItemActionHelper.TRASH(octxt, mbox, responseProto, local, type, tcon).getResult();
            } else if (opStr.equals(OP_RENAME)) {
                String name = action.getAttribute(MailConstants.A_NAME);
                ItemId iidFolder = new ItemId(action.getAttribute(MailConstants.A_FOLDER, "-1"), zsc);
                localResults = ItemActionHelper.RENAME(octxt, mbox, responseProto, local, type, tcon, name, iidFolder).getResult();
        	} else if (opStr.equals(OP_UPDATE)) {
                String folderId = action.getAttribute(MailConstants.A_FOLDER, null);
                ItemId iidFolder = new ItemId(folderId == null ? "-1" : folderId, zsc);
        		if (!iidFolder.belongsTo(mbox))
        			throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
                else if (folderId != null && iidFolder.getId() <= 0)
                    throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
                String name  = action.getAttribute(MailConstants.A_NAME, null);
        		String flags = action.getAttribute(MailConstants.A_FLAGS, null);
        		String tags  = action.getAttribute(MailConstants.A_TAGS, null);
        		MailItem.Color color = getColor(action);
        		localResults = ItemActionHelper.UPDATE(octxt, mbox, responseProto, local, type, tcon, name, iidFolder, flags, 
        					tags, color).getResult();
        	} else {
        		throw ServiceException.INVALID_REQUEST("unknown operation: " + opStr, null);
        	}
        	successes.append(successes.length() > 0 ? "," : "").append(localResults);
        }

        // for moves/copies, make sure that we received notifications from the target folder
        if (remoteNotify != null)
            proxyRequest(zsc.createElement(MailConstants.NO_OP_REQUEST), context, remoteNotify.getId());

        return successes.toString();
    }

    public static MailItem.Color getColor(Element action) throws ServiceException {
        String rgb = action.getAttribute(MailConstants.A_RGB, null);
        byte c = (byte) action.getAttributeLong(MailConstants.A_COLOR, -1);
        if (rgb == null && c < 0)
            return new MailItem.Color(-1);  // it will default to ORANGE
        if (rgb == null)
            return new MailItem.Color(c);
        else
            return new MailItem.Color(rgb);
    }
    
    private Account forceRemoteSession(ZimbraSoapContext zsc, Map<String, Object> context, OperationContext octxt, String op, Element action)
    throws ServiceException {
        // only proxying notification from the user's home-server master session
        if (!zsc.isNotificationEnabled())
            return null;
        Session session = (Session) context.get(SoapEngine.ZIMBRA_SESSION);
        if (session instanceof SoapSession.DelegateSession)
            session = ((SoapSession.DelegateSession) session).getParentSession();
        if (!(session instanceof SoapSession) || session.getMailbox() == null)
            return null;
        SoapSession ss = (SoapSession) session;

        // only have to worry about operations where things can get created in other mailboxes (regular notification works for all other cases)
        if (!op.equals(OP_MOVE) && !op.equals(OP_COPY) && !op.equals(OP_SPAM) && !op.equals(OP_RENAME) && !op.equals(OP_UPDATE))
            return null;
        String folderStr = action.getAttribute(MailConstants.A_FOLDER, null);
        if (folderStr == null)
            return null;

        // recursively dereference mountpoints to find ultimate target folder
        ItemId iidFolder = new ItemId(folderStr, zsc), iidRequested = iidFolder;
        Account owner = null;
        int hopCount = 0;
        ZAuthToken zat = null;
        while (hopCount < ZimbraSoapContext.MAX_HOP_COUNT) {
            owner = Provisioning.getInstance().getAccountById(iidFolder.getAccountId());
            if (Provisioning.onLocalServer(owner)) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(owner);
                Folder folder = mbox.getFolderById(octxt, iidFolder.getId());
                if (!(folder instanceof Mountpoint))
                    break;
                iidFolder = ((Mountpoint) folder).getTarget();
            } else {
                if (zat == null) {
                    AuthToken at = zsc.getAuthToken();
                    String pxyAuthToken = at.getProxyAuthToken();
                    zat = pxyAuthToken == null ? at.toZAuthToken() : new ZAuthToken(pxyAuthToken);
                }
                ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(owner));
                zoptions.setNoSession(true);
                zoptions.setTargetAccount(owner.getId());
                zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
                ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
                ZFolder zfolder = zmbx.getFolderById(iidFolder.toString(zsc.getAuthtokenAccountId()));
                if (!(zfolder instanceof ZMountpoint))
                    break;
                iidFolder = new ItemId(((ZMountpoint) zfolder).getCanonicalRemoteId(), zsc.getAuthtokenAccountId());
            }
            hopCount++;
        }
        if (hopCount == ZimbraSoapContext.MAX_HOP_COUNT)
            throw MailServiceException.TOO_MANY_HOPS(iidRequested);

        // avoid dereferencing the mountpoint again later on
        action.addAttribute(MailConstants.A_FOLDER, iidFolder.toString());

        // fault in a session to listen in on the target folder's mailbox
        if (iidFolder.belongsTo(session.getAuthenticatedAccountId())) {
            return null;
        } else if (iidFolder.isLocal()) {
            ss.getDelegateSession(iidFolder.getAccountId());
            return null;
        } else {
            try {
                proxyRequest(zsc.createElement(MailConstants.NO_OP_REQUEST), context, owner.getId());
                return owner;
            } catch (ServiceException e) {
                return null;
            }
        }
    }

    static void partitionItems(ZimbraSoapContext zsc, String ids, List<Integer> local, Map<String, StringBuffer> remote)
    throws ServiceException {
        Account acct = getRequestedAccount(zsc);
        String targets[] = ids.split(",");
        for (int i = 0; i < targets.length; i++) {
            ItemId iid = new ItemId(targets[i], zsc);
            if (iid.belongsTo(acct)) {
                local.add(iid.getId());
            } else {
                StringBuffer sb = remote.get(iid.getAccountId());
                if (sb == null)
                    remote.put(iid.getAccountId(), new StringBuffer(iid.toString()));
                else
                    sb.append(',').append(iid.toString());
            }
        }
    }

    protected StringBuffer proxyRemoteItems(Element action, Map<String, StringBuffer> remote, Element request, Map<String, Object> context)
    throws ServiceException {
        String folderStr = action.getAttribute(MailConstants.A_FOLDER, null);
        if (folderStr != null) {
            // fully qualify the folder ID (if any) in order for proxying to work
            ItemId iidFolder = new ItemId(folderStr, getZimbraSoapContext(context));
            action.addAttribute(MailConstants.A_FOLDER, iidFolder.toString());
        }

        StringBuffer successes = new StringBuffer();
        for (Map.Entry<String, StringBuffer> entry : remote.entrySet()) {
            // update the <action> element to reference the subset of target items belonging to this user...
            String itemIds = entry.getValue().toString();
            action.addAttribute(MailConstants.A_ID, itemIds);
            // ... proxy to the target items' owner's server...
            String accountId = entry.getKey();
            Element response = proxyRequest(request, context, accountId);
            // ... and try to extract the list of items affected by the operation
            try {
                String completed = response.getElement(MailConstants.E_ACTION).getAttribute(MailConstants.A_ID);
                successes.append(completed.length() > 0 && successes.length() > 0 ? "," : "").append(completed);
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("could not extract ItemAction successes from proxied response", e);
            }
        }

        return successes;
    }
}
