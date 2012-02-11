/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
 * Created on Aug 31, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.OperationContextData;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

public class GetFolder extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_FOLDER, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { MailConstants.E_FOLDER };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return false;
    }

    @Override
    protected String[] getResponseItemPath() {
        return RESPONSE_ITEM_PATH;
    }

    static final String DEFAULT_FOLDER_ID = Integer.toString(Mailbox.ID_FOLDER_USER_ROOT);

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        ItemId iid;
        Element eFolder = request.getOptionalElement(MailConstants.E_FOLDER);
        if (eFolder != null) {
            String uuid = eFolder.getAttribute(MailConstants.A_UUID, null);
            if (uuid != null) {
                Folder folder = mbox.getFolderByUuid(octxt,  uuid);
                iid = new ItemId(folder);
            } else {
                iid = new ItemId(eFolder.getAttribute(MailConstants.A_FOLDER, DEFAULT_FOLDER_ID), zsc);
            }

            String path = eFolder.getAttribute(MailConstants.A_PATH, null);
            if (path != null) {
                Pair<Folder, String> resolved = mbox.getFolderByPathLongestMatch(octxt, iid.getId(), path);
                Folder folder = resolved.getFirst();
                String overflow = resolved.getSecond();

                if (overflow == null) {
                    iid = new ItemId(folder);
                } else if (folder instanceof Mountpoint) {
                    // path crosses a mountpoint; update request and proxy to target mailbox
                    ItemId iidTarget = ((Mountpoint) folder).getTarget();
                    eFolder.addAttribute(MailConstants.A_FOLDER, iidTarget.toString()).addAttribute(MailConstants.A_PATH, overflow);
                    return proxyRequest(request, context, new ItemId(folder), iidTarget);
                } else {
                    throw MailServiceException.NO_SUCH_FOLDER(path);
                }
            }
        } else {
            iid = new ItemId(DEFAULT_FOLDER_ID, zsc);
        }

        int depth = (int) request.getAttributeLong(MailConstants.A_FOLDER_DEPTH, -1);
        boolean traverse = request.getAttributeBool(MailConstants.A_TRAVERSE, false);
        boolean visible = request.getAttributeBool(MailConstants.A_VISIBLE, false);
        boolean needGranteeName = request.getAttributeBool(MailConstants.A_NEED_GRANTEE_NAME, true);

        String v = request.getAttribute(MailConstants.A_DEFAULT_VIEW, null);
        MailItem.Type view = v == null ? null : MailItem.Type.of(v);

        FolderNode rootnode = filterByView(mbox.getFolderTree(octxt, iid, visible), view);

        Element response = zsc.createElement(MailConstants.GET_FOLDER_RESPONSE);
        if (rootnode != null) {
            if (needGranteeName) {
                OperationContextData.addGranteeNames(octxt, rootnode);
            } else {
                OperationContextData.setNeedGranteeName(octxt, false);
            }

            List<ExpandableMountpoint> mounts = Lists.newArrayList();
            Element folderRoot = encodeFolderNode(rootnode, response, ifmt, octxt, true, depth, view, traverse, mounts);

            if (rootnode.mFolder != null && rootnode.mFolder instanceof Mountpoint) {
                mounts.add(new ExpandableMountpoint(folderRoot, (Mountpoint) rootnode.mFolder, depth));
            }
            for (ExpandableMountpoint empt : mounts) {
                expandMountpoint(empt, request, context);
            }
        }

        return response;
    }

    public static Element encodeFolderNode(FolderNode node, Element parent, ItemIdFormatter ifmt, OperationContext octxt)
    throws ServiceException {
        return encodeFolderNode(node, parent, ifmt, octxt, false, -1, null, false, null);
    }

    private static Element encodeFolderNode(FolderNode node, Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            boolean exposeAclAccessKey, int depth, MailItem.Type view, boolean traverse, List<ExpandableMountpoint> mounts)
    throws ServiceException {
        Element eFolder;
        Folder folder = node.mFolder;
        if (folder != null) {
            eFolder = ToXML.encodeFolder(parent, ifmt, octxt, folder, ToXML.NOTIFY_FIELDS, exposeAclAccessKey);
            // if requested, fetch contents of mountpoints
            if (traverse && mounts != null && folder instanceof Mountpoint && folder.getMailbox().hasFullAccess(octxt)) {
                mounts.add(new ExpandableMountpoint(eFolder, (Mountpoint) folder, depth));
            }
        } else {
            eFolder = parent.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_ID, ifmt.formatItemId(node.mId)).addAttribute(MailConstants.A_NAME, node.mName);
        }

        if (depth == 0) {
            return eFolder;
        }

        int remainingDepth = depth > 0 ? depth - 1 : depth;
        for (FolderNode subNode : node.mSubfolders) {
            encodeFolderNode(subNode, eFolder, ifmt, octxt, exposeAclAccessKey, remainingDepth, view, traverse, mounts);
        }

        return eFolder;
    }

    private static class ExpandableMountpoint {
        final Element elt;
        final Mountpoint mpt;
        final int depth;

        ExpandableMountpoint(Element elt, Mountpoint mpt, int depth) {
            this.elt = elt;  this.mpt = mpt;  this.depth = depth;
        }

        @Override
        public String toString() {
            return mpt.getName() + " -> " + mpt.getTarget() + (depth >= 0 ? " [depth " + depth + "] ": "");
        }
    }

    private void expandMountpoint(ExpandableMountpoint empt, Element origRequest, Map<String, Object> context)
    throws ServiceException {
        // kinda hacky -- we're reusing the original request for its protocol, toplevel attrs, etc.
        Element request = origRequest.clone();

        if (empt.depth >= 0) {
            request.addAttribute(MailConstants.A_FOLDER_DEPTH, empt.depth);
        }
        // expand only one level of mountpoints!
        request.addAttribute(MailConstants.A_TRAVERSE, false);

        // gotta nuke the old <folder> element, lest it interfere with our proxying...
        Element eFolder = request.getOptionalElement(MailConstants.E_FOLDER);
        if (eFolder != null) {
            eFolder.detach();
        }
        ItemId iidRemote = empt.mpt.getTarget();
        request.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_FOLDER, iidRemote.toString());

        try {
            Element proxied = proxyRequest(request, context, new ItemId(empt.mpt), iidRemote);
            // return the children of the remote folder as children of the mountpoint
            proxied = proxied.getOptionalElement(MailConstants.E_FOLDER);
            if (proxied != null) {
                ToXML.transferMountpointContents(empt.elt, proxied); //args: to,from
            }
        } catch (SoapFaultException sfe) {
            empt.elt.addAttribute(MailConstants.A_BROKEN, true);
        }
    }

    private FolderNode filterByView(FolderNode node, MailItem.Type viewFilter) {
        if (viewFilter == null || viewFilter == MailItem.Type.UNKNOWN) {
            return node;
        }

        // remove subfolders if there's no view match there
        if (!node.mSubfolders.isEmpty()) {
            for (Iterator<FolderNode> it = node.mSubfolders.iterator(); it.hasNext(); ) {
                if (filterByView(it.next(), viewFilter) == null) {
                    it.remove();
                }
            }
        }

        // mark folder invisible if it doesn't match the requested view
        if (node.mFolder != null && node.mId != Mailbox.ID_FOLDER_TRASH && node.mFolder.getDefaultView() != viewFilter) {
            node.mFolder = null;
        }

        // if neither this folder nor any of its subfolders match, it's out
        return node.mFolder == null && node.mSubfolders.isEmpty() ? null : node;
    }
}
