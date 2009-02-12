package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyShareInfo extends ShareInfoHandler {

    private static final String[] OWNER_ACCOUNT_PATH = new String[] { AdminConstants.E_SHARE, AdminConstants.E_OWNER};
    protected String[] getProxiedAccountElementPath()  { return OWNER_ACCOUNT_PATH; }
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        Provisioning prov = Provisioning.getInstance();
        
        // entry to modify the share info for 
        NamedEntry publishingOnEntry = getTargetEntry(zsc, request, prov);
        
        // TODO, check permission
        
        Element eShare = request.getElement(AdminConstants.E_SHARE);
        ShareInfo.Publishing.Action action = ShareInfo.Publishing.Action.fromString(eShare.getAttribute(AdminConstants.A_ACTION));
            
        String ownerAcctId = getOwner(zsc, eShare, prov, true).getId();
            
        Element eFolder = eShare.getElement(AdminConstants.E_FOLDER);
        String folderPath = eFolder.getAttribute(AdminConstants.A_PATH, null);
        String folderId = eFolder.getAttribute(AdminConstants.A_FOLDER, null);
        String folderIdOrPath = eFolder.getAttribute(AdminConstants.A_PATH_OR_ID, null);
            
        ShareInfo.Publishing si = null;
            
        if (folderPath != null) {
            ensureOtherFolderDescriptorsAreNotPresent(folderId, folderIdOrPath);
            si = new ShareInfo.Publishing(action, ownerAcctId, folderPath, Boolean.FALSE);
        } else if (folderId != null) {
            ensureOtherFolderDescriptorsAreNotPresent(folderPath, folderIdOrPath);
            si = new ShareInfo.Publishing(action, ownerAcctId, folderId, Boolean.TRUE);
        } else if (folderIdOrPath != null) {
            ensureOtherFolderDescriptorsAreNotPresent(folderPath, folderId);
            si = new ShareInfo.Publishing(action, ownerAcctId, folderIdOrPath, null);
        } else
            throw ServiceException.INVALID_REQUEST("missing folder descriptor", null);

        if (si.validateAndDiscoverGrants(octxt, publishingOnEntry))
            si.persist(prov, publishingOnEntry);
        
        Element response = zsc.createElement(AdminConstants.MODIFY_SHARE_INFO_RESPONSE);
        return response;
    }
    
    
    private void ensureOtherFolderDescriptorsAreNotPresent(String other1, String other2) throws ServiceException {
        if (other1 != null || other2 != null)
            throw ServiceException.INVALID_REQUEST("can only specify one of " + 
                                                   AdminConstants.A_PATH + " or " +
                                                   AdminConstants.A_FOLDER + 
                                                   AdminConstants.A_PATH_OR_ID, null);
    }
}
