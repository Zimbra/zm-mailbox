package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public abstract class DistributionListDocumentHandler extends AccountDocumentHandler {

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) 
    throws ServiceException {
        try {
            /* TODO
            Group group = getGroup(request, Provisioning.getInstance());
            
            if (!Provisioning.onLocalServer(group)) {
                return proxyRequest(request, context, getServer(group));
            }
            */
            
            return super.proxyIfNecessary(request, context);
        } catch (ServiceException e) {
            // if something went wrong proxying the request, just execute it locally
            if (ServiceException.PROXY_ERROR.equals(e.getCode()))
                return null;
            // but if it's a real error, it's a real error
            throw e;
        }
    }
    
    private Server getServer(Group group) throws ServiceException {
        String hostname = group.getAttr(Provisioning.A_zimbraMailHost);
        if (hostname == null) {
            throw ServiceException.PROXY_ERROR(AccountServiceException.NO_SUCH_SERVER(""), "");
        }
        Server server = Provisioning.getInstance().get(Key.ServerBy.name, hostname);
        if (server == null) {
            throw ServiceException.PROXY_ERROR(AccountServiceException.NO_SUCH_SERVER(hostname), "");
        }
        return server;
    }

    protected boolean isOwner(Account acct, Group group) throws ServiceException {
        return AccessManager.getInstance().canAccessGroup(acct, group);
    }

    protected Group getGroup(Element request, Account acct, Provisioning prov) 
    throws ServiceException {
        Group group = getGroup(request, prov);
        
        if (!isOwner(acct, group)) {
            throw ServiceException.PERM_DENIED(
                    "you do not have sufficient rights to access this distribution list");
        }
        
        return group;
    }

    protected Group getGroup(Element request, Provisioning prov) 
    throws ServiceException {
        Element d = request.getElement(AccountConstants.E_DL);
        String key = d.getAttribute(AccountConstants.A_BY);
        String value = d.getText();
        
        // temporary fix for the caching bug
        // Group group = prov.getGroupBasic(Key.DistributionListBy.fromString(key), value);
        Group group = prov.getGroup(Key.DistributionListBy.fromString(key), value);
        
        if (group == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);
        }
        
        return group;
    }

}
