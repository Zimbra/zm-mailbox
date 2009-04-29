/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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
package com.zimbra.cs.service.account;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class GetShareInfo  extends AccountDocumentHandler {

    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {
        
        if (isInternal(request)) {
            // we have been proxied to this server because the specified 
            // "owner account" is homed here.
            // Do not proxy in this case. 
            return null;
        } else {
            // call super class, go the normal route to proxy to 
            // the requested account's home server
            return super.proxyIfNecessary(request, context);  
        }
    }
    
    private boolean isInternal(Element request) throws ServiceException {
        return request.getAttributeBool(AccountConstants.A_INTERNAL, false);
    }
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        Element response = zsc.createElement(AccountConstants.GET_SHARE_INFO_RESPONSE);
        doGetShareInfo(zsc, context, account, request, response);
        
        return response;
    }
    
    /**
     * @param zsc
     * @param targetAcct
     * @param request
     * @param response
     */
    private void doGetShareInfo(ZimbraSoapContext zsc, Map<String, Object> context,
            Account targetAcct, Element request, Element response) throws ServiceException {
        
        Provisioning prov = Provisioning.getInstance();
        
        Element eGrantee = request.getOptionalElement(AccountConstants.E_GRANTEE);
        byte granteeType = getGranteeType(eGrantee);
        String granteeId = eGrantee == null? null : eGrantee.getAttribute(AccountConstants.A_ID, null);
        String granteeName = eGrantee == null? null : eGrantee.getAttribute(AccountConstants.A_NAME, null);
            
        Account owner = null;
        Element eOwner = request.getOptionalElement(AccountConstants.E_OWNER);
        if (eOwner != null) {
            AccountBy acctBy = AccountBy.fromString(eOwner.getAttribute(AccountConstants.A_BY));
            String key = eOwner.getText();
            owner = prov.get(acctBy, key);
            
            // to defend against harvest attacks return "no shares" instead of error 
            // when an invalid user name/id is used.
            if (owner == null)
                return;
        }
        
        OperationContext octxt = getOperationContext(zsc, context);
            
        
        ShareInfo.MountedFolders mountedFolders = null;
        if (!isInternal(request)) {
            // this (new ShareInfo.MountedFolders) should be executed on the requested 
            // account's home server.  
            // If we get here, we should be proxied to the right server naturally by the framework.
            mountedFolders = new ShareInfo.MountedFolders(octxt, targetAcct);
        }
        
        ResultFilter resultFilter = new ResultFilterByTarget(granteeId, granteeName);
        ShareInfoVisitor visitor = new ShareInfoVisitor(prov, response, mountedFolders, resultFilter);
        
        if (owner == null) {
            // retrieve from published share info, 
            if (granteeType != 0 && granteeType != ACL.GRANTEE_GROUP)
                throw ServiceException.INVALID_REQUEST("invalid grantee type for retrieving published share info", null);
            ShareInfo.Published.get(prov, targetAcct, granteeType, owner, visitor);
        } else {
            // iterate all folders of the owner, this should be proxied to the owner account's
            // home server if the owner account does not reside on the same server as the requesting 
            // account.
            
            if (targetAcct.getId().equals(owner.getId()))
                throw ServiceException.INVALID_REQUEST("cannot discover shares on self", null);
            
            if (Provisioning.onLocalServer(owner))
                ShareInfo.Discover.discover(octxt, prov, targetAcct, granteeType, owner, visitor);
            else {
                // issue an GetShareInfoRequest to the home server of the owner, and tell it *not* 
                // to proxy to the requesting account's mailbox server.
                fetchRemoteShareInfo(zsc, context, request, owner.getId(), visitor);
            }
        }

        visitor.finish();
    }
    
    private void fetchRemoteShareInfo(ZimbraSoapContext zsc, Map<String, Object> context, 
            Element request, String ownerId, ShareInfoVisitor visitor)
    throws ServiceException {
       
        /*
         * hack, there is no way to tell the proxying code to set
         * the <targetServer> element in the SOAP context so the 
         * request won't be proxied again back to this server.
         * 
         * mark the proxy request "internal" to indicate to the 
         * handler to:
         * 
         * 1. Do not proxy the request again (normal flow would be to 
         *    proxy to the home server of the requested account, which is this server)
         *    
         * 2. Do not get the mounted info of the requesting account, because 
         *    it won't be able to access the mailbox of the requested account, 
         *    which lives on this server.   
         */
        request.addAttribute(AccountConstants.A_INTERNAL, true);

        Element response = proxyRequest(request, context, getServer(ownerId), zsc);
        for (Element eShare : response.listElements(AccountConstants.E_SHARE)) {
            ShareInfo si = ShareInfo.fromXML(eShare);
            visitor.visit(si);
        }
    }

    public static byte getGranteeType(Element eGrantee) throws ServiceException {
        String granteeType = null;
        if (eGrantee != null)
            granteeType = eGrantee.getAttribute(AccountConstants.A_TYPE, null);
        
        byte gt;
        if (granteeType == null)
            gt = 0;
        else {
            gt = ACL.stringToType(granteeType);
        }
        return gt;
    }
    
    public static interface ResultFilter {
        /*
         * return true if filtered in, false if filtered out
         */
        public boolean check(ShareInfo si);
    }
    
    public static class ResultFilterByTarget implements ResultFilter {
        String mGranteeId;
        String mGranteeName;
        
        public ResultFilterByTarget(String granteeId, String granteeName) {
            mGranteeId = granteeId;
            mGranteeName = granteeName;
        }
        
        public boolean check(ShareInfo si) {
            if (mGranteeId != null && !mGranteeId.equals(si.getGranteeId()))
                return false;
            
            if (mGranteeName != null && !mGranteeName.equals(si.getGranteeName()))
                return false;
            
            return true;
        }
    }
    
    public static class ShareInfoVisitor implements ShareInfo.Visitor {
        Provisioning mProv;
        Element mResp;
        ShareInfo.MountedFolders mMountedFolders;
        ResultFilter mResultFilter;
        
        SortedSet<ShareInfo> mSortedShareInfo = new TreeSet<ShareInfo>(new ShareInfoComparator());
        
        private static class ShareInfoComparator implements Comparator<ShareInfo> {
            public int compare(ShareInfo a, ShareInfo b) {
                int r = a.getFolderPath().compareToIgnoreCase(b.getFolderPath());
                if (r == 0)
                    r = a.getOwnerAcctEmail().compareToIgnoreCase(b.getOwnerAcctEmail());
                if (r == 0)
                    r = a.getGranteeName().compareToIgnoreCase(b.getGranteeName());
                return r;
            }
        }
        
        public ShareInfoVisitor(Provisioning prov, Element resp,
                ShareInfo.MountedFolders mountedFolders, ResultFilter resultFilter) {
            mProv = prov;
            mResp = resp;
            mMountedFolders = mountedFolders;
            mResultFilter = resultFilter;
        }
        
        // sorting and filtering visitor
        // note: if grnteeType is filtered at ShareInfo
        public void visit(ShareInfo shareInfo) throws ServiceException {
            // add the result if there is no filter or the result passes the filter test
            if (mResultFilter == null || mResultFilter.check(shareInfo))
                mSortedShareInfo.add(shareInfo);
        }
        
        public void finish() throws ServiceException {
            for (ShareInfo si : mSortedShareInfo)
                doVisit(si);
        }
        
        // the real visitor
        private void doVisit(ShareInfo shareInfo) throws ServiceException {
            Element eShare = mResp.addElement(AccountConstants.E_SHARE);
            
            // add mountpoint id to XML if the share is already mounted
            Integer mptId = null;
            
            if (mMountedFolders != null) 
                mptId = mMountedFolders.getLocalFolderId(
                    shareInfo.getOwnerAcctId(), shareInfo.getFolderId());
            
            shareInfo.toXML(eShare, mptId);
        }
    }

}
