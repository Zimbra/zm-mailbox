package com.zimbra.cs.service.account;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

public class DistributionListAction extends AccountDocumentHandler {
    
    private static enum DLAction {
        delete,
        modify,
        rename,
        addAlias,
        removeAlias,
        addOwner,
        removeOwner,
        addMembers,
        removeMembers;
        
        private static DLAction fromString(String str) throws ServiceException {
            try {
                return DLAction.valueOf(str);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid op: " + str, e);
            }
        }
    };
    
    public Element handle(Element request, Map<String, Object> context) 
    throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Group group = GetDistributionList.getGroup(request, zsc, prov);
        
        Element eAction = request.getElement(AccountConstants.E_ACTION);
        DLAction op = DLAction.fromString(eAction.getAttribute(AccountConstants.A_OP));
        Account acct = getRequestedAccount(zsc);
        
        DLActionHandler handler = null;
        switch (op) {
            case delete:
                handler = new DeleteHandler(request, group, acct);
                break;
            case modify:
                handler = new RenameHandler(request, group, acct);
                break;
            case rename:
                break;
            case addAlias:
                break;
            case removeAlias:
                break;
            case addOwner:
                break;
            case removeOwner:
                break;
            case addMembers:
                break;
            case removeMembers:
                break;
            default:
                throw ServiceException.FAILURE("unsupported op:" + op.name(), null);
        }
        
        handler.handle();
        
        Element response = zsc.createElement(AccountConstants.DISTRIBUTION_LIST_ACTION_RESPONSE);

        return response;
    }
    
    
    private static abstract class DLActionHandler {
        protected Element request;
        protected Group group;
        protected Account requestedAcct;
        protected Provisioning prov;
        
        protected DLActionHandler(Element request, Group group, Account requestedAcct) {
            this.request = request;
            this.group = group;
            this.requestedAcct = requestedAcct;
            this.prov = Provisioning.getInstance();
        }
        
        abstract void handle() throws ServiceException;
        abstract DLAction getAction();
    }
    
    private static class DeleteHandler extends DLActionHandler {

        protected DeleteHandler(Element request, Group group, Account requestedAcct) {
            super(request, group, requestedAcct);
        }

        @Override
        DLAction getAction() {
            return DLAction.delete;
        }
        
        @Override
        void handle() throws ServiceException {
            prov.deleteGroup(group.getId());
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "id", group.getId()}));
        }

    }
    
    private static class RenameHandler extends DLActionHandler {

        protected RenameHandler(Element request, Group group, Account requestedAcct) {
            super(request, group, requestedAcct);
        }
        
        @Override
        DLAction getAction() {
            return DLAction.rename;
        }

        @Override
        void handle() throws ServiceException {
            Element eNewName = request.getElement(AccountConstants.E_NEW_NAME);
            String newName = eNewName.getText();
            
            String oldName = group.getName();
            prov.renameGroup(group.getId(), newName);

            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", oldName, "newName", newName})); 
        }
    }
}
