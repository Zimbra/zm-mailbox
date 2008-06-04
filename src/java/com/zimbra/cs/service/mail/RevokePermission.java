package com.zimbra.cs.service.mail;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.accesscontrol.PermUtil;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.soap.ZimbraSoapContext;

public class RevokePermission extends GrantPermission {
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        for (Element eACE : request.listElements(MailConstants.E_ACE)) {
            ZimbraACE ace = handleACE(eACE, zsc);
            aces.add(ace);
        }

        Set<ZimbraACE> revoked = PermUtil.revokeAccess(account, aces);
        Element response = zsc.createElement(MailConstants.REVOKE_PERMISSION_RESPONSE);
        if (aces != null) {
            for (ZimbraACE ace : revoked)
                ToXML.encodeACE(response, ace);
        }
        return response;
    }
    
}
