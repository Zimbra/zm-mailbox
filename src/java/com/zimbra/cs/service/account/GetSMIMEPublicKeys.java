package com.zimbra.cs.service.account;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ldap.LdapSMIMEConfig;
import com.zimbra.soap.ZimbraSoapContext;

public class GetSMIMEPublicKeys extends AccountDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        Element eEmail = request.getElement(AccountConstants.E_EMAIL);
        String email = eEmail.getText();
        
        List<String> certs = LdapSMIMEConfig.lookupPublicKeys(account, email);
        
        Element response = zsc.createElement(AccountConstants.GET_SMIME_PUBLIC_KEYS_RESPONSE);
        
        for (String cert : certs) {
            response.addElement(AccountConstants.E_CERT).setText(cert);
        }
        
        return response;
    }
}
