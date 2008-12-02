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
import com.zimbra.common.util.Pair;
import com.zimbra.soap.ZimbraSoapContext;

public class GetFolder extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_FOLDER, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { MailConstants.E_FOLDER };
    @Override protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    @Override protected boolean checkMountpointProxy(Element request)  { return false; }
    @Override protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

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
        boolean traverse = request.getAttributeBool(MailConstants.A_TRAVERSE, false);

        FolderNode rootnode = getFolderTree(octxt, mbox, iid, visible);
        // if the requested folder was a mountpoint, always resolve it
        traverse |= rootnode != null && rootnode.mFolder instanceof Mountpoint;

		Element response = zsc.createElement(MailConstants.GET_FOLDER_RESPONSE);
        if (rootnode != null) {
            List<Pair<Element, Mountpoint>> mptinfo = traverse ? new ArrayList<Pair<Element, Mountpoint>>(3) : null;
    		encodeFolderNode(ifmt, octxt, response, rootnode, true, mptinfo);

    		if (mptinfo != null && !mptinfo.isEmpty()) {
    		    for (Pair<Element, Mountpoint> mount : mptinfo)
    		        handleMountpoint(mount.getSecond(), mount.getFirst(), zsc, context);
    		}
        }

		return response;
	}
	
	public static Element encodeFolderNode(ItemIdFormatter ifmt, OperationContext octxt, Element parent, FolderNode node) {
	    return encodeFolderNode(ifmt, octxt, parent, node, false, null);
	}

	private static Element encodeFolderNode(ItemIdFormatter ifmt, OperationContext octxt, Element parent, FolderNode node,
	                                        boolean exposeAclAccessKey, List<Pair<Element, Mountpoint>> mptinfo) {
		Element eFolder;
        if (node.mFolder != null)
            eFolder = ToXML.encodeFolder(parent, ifmt, octxt, node.mFolder, exposeAclAccessKey);
        else
            eFolder = parent.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_ID, ifmt.formatItemId(node.mId)).addAttribute(MailConstants.A_NAME, node.mName);

        if (mptinfo != null && node.mFolder instanceof Mountpoint)
            mptinfo.add(new Pair<Element, Mountpoint>(eFolder, (Mountpoint) node.mFolder));

		for (FolderNode subNode : node.mSubfolders)
			encodeFolderNode(ifmt, octxt, eFolder, subNode, exposeAclAccessKey, mptinfo);

		return eFolder;
	}

    private void handleMountpoint(Mountpoint mpt, Element eMountpoint, ZimbraSoapContext zsc, Map<String, Object> context)
	throws ServiceException, SoapFaultException {
        // no mountpoints pointing to the same account!
        if (mpt.getAccount().getId().equalsIgnoreCase(mpt.getOwnerId()))
            return;

        ItemId iidLocal = new ItemId(mpt);
        ItemId iidRemote = new ItemId(mpt.getOwnerId(), mpt.getRemoteId());
        Element request = zsc.createElement(MailConstants.GET_FOLDER_REQUEST);
        request.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_FOLDER, iidRemote.toString());

        Element proxied = proxyRequest(request, context, iidLocal, iidRemote);
        // return the children of the remote folder as children of the mountpoint
        Element target = proxied.getOptionalElement(MailConstants.E_FOLDER);
        if (target != null)
            transferMountpointAttributes(eMountpoint, target);
    }

    public static void transferMountpointAttributes(Element eMountpoint, Element eTarget) {
        if (eMountpoint == null || eTarget == null)
            return;

        // transfer folder counts to the serialized mountpoint from the serialized target folder
        eMountpoint.addAttribute(MailConstants.A_UNREAD, eTarget.getAttribute(MailConstants.A_UNREAD, null));
        eMountpoint.addAttribute(MailConstants.A_NUM, eTarget.getAttribute(MailConstants.A_NUM, null));
        eMountpoint.addAttribute(MailConstants.A_SIZE, eTarget.getAttribute(MailConstants.A_SIZE, null));
        eMountpoint.addAttribute(MailConstants.A_URL, eTarget.getAttribute(MailConstants.A_URL, null));
        eMountpoint.addAttribute(MailConstants.A_RIGHTS, eTarget.getAttribute(MailConstants.A_RIGHTS, null));
        if (eTarget.getAttribute(MailConstants.A_FLAGS, "").indexOf("u") != -1)
            eMountpoint.addAttribute(MailConstants.A_FLAGS, "u" + eMountpoint.getAttribute(MailConstants.A_FLAGS, "").replace("u", ""));

        // transfer ACL and child folders to the serialized mountpoint from the serialized remote folder
        for (Element child : eTarget.listElements()) {
            if (child.getName().equals(MailConstants.E_ACL))
                eMountpoint.addUniqueElement(child.clone());
            else
                eMountpoint.addElement(child.clone());
        }
    }
    
    public static class FolderNode {
        public int mId;
        public String mName;
        public Folder mFolder;
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
