package com.zimbra.cs.service.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAdminConsoleUIComp extends AdminDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element resp = zsc.createElement(AdminConstants.GET_ADMIN_CONSOLE_UI_COMP_RESPONSE);
        
        Set<String> added = new HashSet<String>();
        
        Account acct = getAuthenticatedAccount(zsc);
        addValues(acct, resp, added);
        
        Provisioning prov = Provisioning.getInstance();
        AclGroups aclGroups = prov.getAclGroups(acct, true);
        for (String groupId : aclGroups.groupIds()) {
            DistributionList dl = prov.get(DistributionListBy.id, groupId);
            addValues(dl, resp, added);
        }
        
        return resp;
    }
    
    private void addValues(NamedEntry entry, Element resp, Set<String> added) {
        Set<String> values = entry.getMultiAttrSet(Provisioning.A_zimbraAdminConsoleUIComponents);
        for (String value: values) {
            if (!added.contains(value)) {
                resp.addElement(AdminConstants.E_A).setText(value);
                added.add(value);
            }
        }
    }
    
    @Override
    protected void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(sDocRightNotesAllowAllAdmins);
    }
    
}
