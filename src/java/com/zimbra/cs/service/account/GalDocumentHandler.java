package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class GalDocumentHandler extends AccountDocumentHandler {

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {
        try {
            ZimbraSoapContext zsc = getZimbraSoapContext(context);
            
            Provisioning prov = Provisioning.getInstance();
            
            // check whether we need to proxy to the home server of the GAL sync acount
            String[] xpath = getProxiedAccountPath();
            String acctId = (xpath != null ? getXPath(request, xpath) : null);
            if (acctId != null) {
                Account acct = prov.get(AccountBy.id, acctId, zsc.getAuthToken());
                if (acct != null) {
                    if (!Provisioning.onLocalServer(acct))
                        return proxyRequest(request, context, acctId);
                    else
                        return null;
                }
            }
            
            return super.proxyIfNecessary(request, context);
        } catch (ServiceException e) {
            // if something went wrong proxying the request, just execute it locally
            if (ServiceException.PROXY_ERROR.equals(e.getCode()))
                return null;
            // but if it's a real error, it's a real error
            throw e;
        }
    }

    protected String[] getProxiedAccountPath() { 
        return new String[] { AccountConstants.A_GAL_ACCOUNT_ID };
    }
}
