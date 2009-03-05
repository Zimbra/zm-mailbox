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
     * This method is used by both the account namespace and admin namespace GetShareInfoRequest
     * 
     * @param zsc
     * @param targetAcct
     * @param request
     * @param response
     */
    public static void doGetShareInfo(ZimbraSoapContext zsc, Map<String, Object> context,
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
            
        ShareInfo.MountedFolders mountedFolders = new ShareInfo.MountedFolders(octxt, targetAcct);
        
        ResultFilter resultFilter = new ResultFilterByTarget(granteeId, granteeName);
        ShareInfoVisitor visitor = new ShareInfoVisitor(prov, response, mountedFolders, resultFilter);
        
        if (owner == null) {
            // retrieve from published share info, 
            if (granteeType != 0 && granteeType != ACL.GRANTEE_GROUP)
                throw ServiceException.INVALID_REQUEST("invalid grantee type for retrieving published share info", null);
            ShareInfo.Published.get(prov, targetAcct, granteeType, owner, visitor);
        } else {
            // iterate all folders of the owner
            if (targetAcct.getId().equals(owner.getId()))
                throw ServiceException.INVALID_REQUEST("cannot discover shares on self", null);
            ShareInfo.Discover.discover(octxt, prov, targetAcct, granteeType, owner, visitor);
        }

        visitor.finish();
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
