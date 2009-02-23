package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class ShareInfoHandler extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ACCOUNT };
    protected String[] getProxiedAccountElementPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only return accounts a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    protected NamedEntry getTargetEntry(ZimbraSoapContext zsc, Element request, Provisioning prov) throws ServiceException {
        Element eAcct = request.getOptionalElement(AdminConstants.E_ACCOUNT);
        Element eDl = request.getOptionalElement(AdminConstants.E_DL);
        
        if (eAcct != null && eDl != null)
            throw ServiceException.INVALID_REQUEST("only one of " + AdminConstants.E_ACCOUNT + " or " +
                                                   AdminConstants.E_DL + " can be specified", null);
        
        NamedEntry entry = null;
        if (eAcct != null) {
            String key = eAcct.getAttribute(AdminConstants.A_BY);
            String value = eAcct.getText();
    
            Account account = prov.get(AccountBy.fromString(key), value, zsc.getAuthToken());
    
            if (account == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(value);
            
            entry = account;
        } else {
            String key = eDl.getAttribute(AdminConstants.A_BY);
            String value = eDl.getText();
    
            DistributionList dl = prov.get(DistributionListBy.fromString(key), value);
            
            if (dl == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);
            
            entry = dl;
        }
        
        return entry;
    }
    
    protected Account getOwner(ZimbraSoapContext zsc, Element eShare, Provisioning prov, boolean required) throws ServiceException {
        Element eOwner = null;
        if (required)
            eOwner = eShare.getElement(AdminConstants.E_OWNER);
        else
            eOwner = eShare.getOptionalElement(AdminConstants.E_OWNER);
        
        if (eOwner == null)
            return null;
        
        String key = eOwner.getAttribute(AdminConstants.A_BY);
        String value = eOwner.getText();

        Account account = prov.get(AccountBy.fromString(key), value, zsc.getAuthToken());

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);
        
        return account;
    }
    

}
