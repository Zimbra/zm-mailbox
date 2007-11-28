/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Aug 31, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class GetFolder extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_FOLDER, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { MailConstants.E_FOLDER };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return false; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    static final String DEFAULT_FOLDER_ID = "" + Mailbox.ID_FOLDER_USER_ROOT;

	public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		Mailbox mbox = getRequestedMailbox(zsc);
		OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

		String parentId = DEFAULT_FOLDER_ID;
		Element eFolder = request.getOptionalElement(MailConstants.E_FOLDER);
		if (eFolder != null) {
            String path = eFolder.getAttribute(MailConstants.A_PATH, null);
            if (path != null)
                parentId = mbox.getFolderByPath(octxt, path).getId() + "";
            else
                parentId = eFolder.getAttribute(MailConstants.A_FOLDER, DEFAULT_FOLDER_ID);
        }
		ItemId iid = new ItemId(parentId, zsc);

        boolean visible = request.getAttributeBool(MailConstants.A_VISIBLE, false);
		
        FolderNode rootnode = getFolderTree(octxt, mbox, iid, visible);

		Element response = zsc.createElement(MailConstants.GET_FOLDER_RESPONSE);
        if (rootnode != null) {
    		Element folderRoot = encodeFolderNode(ifmt, octxt, response, rootnode);
    		if (rootnode.mFolder != null && rootnode.mFolder instanceof Mountpoint)
    			handleMountpoint(request, context, iid, (Mountpoint) rootnode.mFolder, folderRoot);			
        }

		return response;
	}

	public static Element encodeFolderNode(ItemIdFormatter ifmt, OperationContext octxt, Element parent, FolderNode node) {
		Element eFolder;
        if (node.mFolder != null)
            eFolder = ToXML.encodeFolder(parent, ifmt, octxt, node.mFolder);
        else
            eFolder = parent.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_ID, node.mId).addAttribute(MailConstants.A_NAME, node.mName);

		for (FolderNode subNode : node.mSubfolders)
			encodeFolderNode(ifmt, octxt, eFolder, subNode);

		return eFolder;
	}

//	public static Element handleFolder(Mailbox mbox, Folder folder, Element response, ZimbraSoapContext lc, OperationContext octxt)
//    throws ServiceException {
//		Element respFolder = ToXML.encodeFolder(response, lc, folder);
//
//        for (Folder subfolder : folder.getSubfolders(octxt))
//        	if (subfolder != null)
//       		handleFolder(mbox, subfolder, respFolder, lc, octxt);
//       return respFolder;
//	}

//	public static Element handleFolder(Mailbox mbox, Folder folder, Element response, ZimbraSoapContext lc, OperationContext octxt)
//	throws ServiceException {
//		Element respFolder = ToXML.encodeFolder(response, lc, folder);
//		
//		List subfolders = folder.getSubfolders(octxt);
//		if (subfolders != null)
//			for (Iterator it = subfolders.iterator(); it.hasNext(); ) {
//				Folder subfolder = (Folder) it.next();
//				if (subfolder != null)
//					handleFolder(mbox, subfolder, respFolder, lc, octxt);
//			}
//		return respFolder;
//	}
	
	private void handleMountpoint(Element request, Map<String, Object> context, ItemId iidLocal, Mountpoint mpt, Element eRoot)
	throws ServiceException, SoapFaultException {
		ItemId iidRemote = new ItemId(mpt.getOwnerId(), mpt.getRemoteId());
		Element proxied = proxyRequest(request, context, iidLocal, iidRemote);
        // return the children of the remote folder as children of the mountpoint
		proxied = proxied.getOptionalElement(MailConstants.E_FOLDER);
		if (proxied != null) {
			eRoot.addAttribute(MailConstants.A_REST_URL, proxied.getAttribute(MailConstants.A_REST_URL, null));
			eRoot.addAttribute(MailConstants.A_RIGHTS, proxied.getAttribute(MailConstants.A_RIGHTS, null));
            for (Element eRemote : proxied.listElements()) {
				// skip the <acl> element, if any
				if (!eRemote.getName().equals(MailConstants.E_ACL))
					eRoot.addElement(eRemote.detach());
			}
		}
	}
    
    public static class FolderNode {
        public int mId;
        public String mName;
        public Folder mFolder;
        public boolean mVisible;
        public List<FolderNode> mSubfolders = new ArrayList<FolderNode>();
    }
    
	public static FolderNode getFolderTree(OperationContext octxt, Mailbox mbox, ItemId iid, boolean returnAllVisibleFolders) throws ServiceException {
	    synchronized (mbox) {
            // get the root node...
	        int folderId = iid != null ? iid.getId() : Mailbox.ID_FOLDER_USER_ROOT;
            Folder folder = mbox.getFolderById(returnAllVisibleFolders ? null : octxt, folderId);

            // for each subNode...
            Set<Folder> visibleFolders = mbox.getVisibleFolders(octxt);
            return handleFolder(folder, visibleFolders, returnAllVisibleFolders);
        }
	}
    
	private static FolderNode handleFolder(Folder folder, Set<Folder> visible, boolean returnAllVisibleFolders) throws ServiceException {
	    boolean isVisible = visible == null || visible.remove(folder);
        if (!isVisible && !returnAllVisibleFolders)
            return null;

	    // short-circuit if we know that this won't be in the output
        List<Folder> subfolders = folder.getSubfolders(null);
        if (!isVisible && subfolders.isEmpty())
            return null;

        FolderNode node = new FolderNode();
        node.mId = folder.getId();
        node.mName = node.mId == Mailbox.ID_FOLDER_ROOT ? null : folder.getName();
        node.mFolder = isVisible ? folder : null;

        // if this was the last visible folder overall, no need to look at children
        if (isVisible && visible != null && visible.isEmpty())
            return node;

        // write the subfolders' data to the response
        for (Folder subfolder : subfolders) {
            FolderNode child = handleFolder(subfolder, visible, returnAllVisibleFolders);
            if (child != null) {
                node.mSubfolders.add(child);
                isVisible = true;
            }
        }

        return isVisible ? node : null;
    }
    
}
