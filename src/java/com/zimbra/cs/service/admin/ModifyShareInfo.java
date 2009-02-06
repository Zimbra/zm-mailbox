package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyShareInfo extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ACCOUNT };
    protected String[] getProxiedAccountElementPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only return accounts a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        Provisioning prov = Provisioning.getInstance();
        
        // entry to modify the share info for 
        NamedEntry publishingOnEntry = getPublishingOnEntry(zsc, request, prov);
        
        // TODO, check permission
        
        List<ShareInfo> shareInfo = new ArrayList<ShareInfo>();
        for (Element eShare : request.listElements(AdminConstants.E_SHARE)) {
            ShareInfo.Action action = ShareInfo.Action.fromString(eShare.getAttribute(AdminConstants.A_ACTION));
            
            String ownerAcctId = getOwner(zsc, eShare, prov).getId();
            
            String desc = null;
            // desc is required for add, ignored for remove
            /*
            if (action == ShareInfo.Action.add)
                desc = eShare.getElement(AdminConstants.E_DESC).getText();
            */
            
            Element eFolder = eShare.getElement(AdminConstants.E_FOLDER);
            String folderPath = eFolder.getAttribute(AdminConstants.A_PATH, null);
            String folderId = eFolder.getAttribute(AdminConstants.A_FOLDER, null);
            String folderIdOrPath = eFolder.getAttribute(AdminConstants.A_PATH_OR_ID, null);
            
            ShareInfo si = null;
            
            if (folderPath != null) {
                ensureOtherFolderDescriptorsAreNotPresent(folderId, folderIdOrPath);
                si = new ShareInfo(action, ownerAcctId, folderPath, Boolean.FALSE, desc);
            } else if (folderId != null) {
                ensureOtherFolderDescriptorsAreNotPresent(folderPath, folderIdOrPath);
                si = new ShareInfo(action, ownerAcctId, folderId, Boolean.TRUE, desc);
            } else if (folderIdOrPath != null) {
                ensureOtherFolderDescriptorsAreNotPresent(folderPath, folderId);
                si = new ShareInfo(action, ownerAcctId, folderIdOrPath, null, desc);
            } else
                throw ServiceException.INVALID_REQUEST("missing folder descriptor", null);

            if (si.validateAndDiscoverGrants(octxt, publishingOnEntry))
                shareInfo.add(si);
        }
        
        ShareInfo.persist(prov, publishingOnEntry, shareInfo);
        
        Element response = zsc.createElement(AdminConstants.MODIFY_ACCOUNT_RESPONSE);
        return response;
    }
    
    private NamedEntry getPublishingOnEntry(ZimbraSoapContext zsc, Element request, Provisioning prov) throws ServiceException {
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
            
            if (!canAccessAccount(zsc, account))
                throw ServiceException.PERM_DENIED("can not access account");
            
            entry = account;
        } else {
            String key = eDl.getAttribute(AdminConstants.A_BY);
            String value = eDl.getText();
    
            DistributionList dl = prov.get(DistributionListBy.fromString(key), value);
            
            if (dl == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);
    
            if (!canAccessEmail(zsc, dl.getName()))
                throw ServiceException.PERM_DENIED("can not access dl");
            
            entry = dl;
        }
        
        return entry;
    }
    
    private Account getOwner(ZimbraSoapContext zsc, Element eShare, Provisioning prov) throws ServiceException {
        Element eOwner = eShare.getElement(AdminConstants.E_OWNER);
        String key = eOwner.getAttribute(AdminConstants.A_BY);
        String value = eOwner.getText();

        Account account = prov.get(AccountBy.fromString(key), value, zsc.getAuthToken());

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);
        
        return account;
    }
    
    private void ensureOtherFolderDescriptorsAreNotPresent(String other1, String other2) throws ServiceException {
        if (other1 != null || other2 != null)
            throw ServiceException.INVALID_REQUEST("can only specify one of " + 
                                                   AdminConstants.A_PATH + " or " +
                                                   AdminConstants.A_FOLDER + 
                                                   AdminConstants.A_PATH_OR_ID, null);
    }
}
