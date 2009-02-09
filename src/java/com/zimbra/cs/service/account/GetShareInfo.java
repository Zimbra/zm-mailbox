package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.soap.ZimbraSoapContext;

public class GetShareInfo  extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        Provisioning prov = Provisioning.getInstance();

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        String granteeType = null;
        Element eGrantee = request.getOptionalElement(AccountConstants.E_GRANTEE);
        if (eGrantee != null) {
            granteeType = eGrantee.getAttribute(AccountConstants.A_TYPE);
        }
        
        Element response = zsc.createElement(AccountConstants.GET_SHARE_INFO_RESPONSE);
        
        Account owner = null;
        Element eOwner = request.getOptionalElement(AccountConstants.E_OWNER);
        if (eOwner != null) {
            AccountBy acctBy = AccountBy.fromString(eOwner.getAttribute(AccountConstants.A_BY));
            String key = eOwner.getText();
            owner = prov.get(acctBy, key);
            
            // to defend against harvest attacks return "no shares" instead of error 
            // when an invalid user name/id is used.
            if (owner == null)
                return response;
        }
            
        ShareInfoVisitor visitor = new ShareInfoVisitor(prov, response);
        ShareInfo.Published.get(account, granteeType, owner, visitor);

        return response;
    }

    public static class ShareInfoVisitor implements ShareInfo.Published.Visitor {
        Provisioning mProv;
        Element mResp;
        
        public ShareInfoVisitor(Provisioning prov,Element resp) {
            mProv = prov;
            mResp = resp;
        }
        
        public void visit(ShareInfo.Published shareInfo) throws ServiceException {
            Element eShare = mResp.addElement(AccountConstants.E_SHARE);
            shareInfo.toXML(eShare);
        }
    }

}
