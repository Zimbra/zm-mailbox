package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.type.AutoProvPrincipalBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class AutoProvAccount extends AdminDocumentHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Element eDomain = request.getElement(AdminConstants.E_DOMAIN);
        DomainBy domainBy = DomainBy.fromString(eDomain.getAttribute(AdminConstants.A_BY));
        String domainValue = eDomain.getText();
        Domain domain = prov.get(domainBy, domainValue);
        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(domainValue);
        }
        
        checkRight(zsc, context, domain, Admin.R_autoProvisionAccount);
        
        Element ePrincipal = request.getElement(AdminConstants.E_PRINCIPAL);
        AutoProvPrincipalBy by = AutoProvPrincipalBy.fromString(ePrincipal.getAttribute(AdminConstants.A_BY));
        String principal = ePrincipal.getText();
        
        Element ePassword = request.getOptionalElement(AdminConstants.E_PASSWORD);
        String password = ePassword == null ? null : ePassword.getText();
        
        Account acct = prov.autoProvAccountManual(domain, by, principal, password);
        if (acct == null) {
            throw ServiceException.FAILURE("unable to auto provision account: " + principal, null);
        }
        
        Element response = zsc.createElement(AdminConstants.AUTO_PROV_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, acct);
        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_autoProvisionAccount);
    }
}
