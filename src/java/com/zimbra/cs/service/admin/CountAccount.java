package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CountAccountResult;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class CountAccount extends AdminDocumentHandler {

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Element d = request.getElement(AdminConstants.E_DOMAIN);
        String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();
        
        Domain domain = prov.get(DomainBy.fromString(key), value);

        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(value);

        checkDomainRight(zsc, domain, Admin.R_countAccount);

        Element response = zsc.createElement(AdminConstants.COUNT_ACCOUNT_RESPONSE);
        CountAccountResult result = Provisioning.getInstance().countAccount(domain);
        toXML(response, result);
        return response;
    }
    
    private void toXML(Element parent, CountAccountResult result) {
        for (CountAccountResult.CountAccountByCos c : result.getCountAccountByCos()) {
            Element eCos = parent.addElement(AdminConstants.E_COS);
            eCos.addAttribute(AdminConstants.A_ID, c.getCosId());
            eCos.addAttribute(AdminConstants.A_NAME, c.getCosName());
            eCos.setText(String.valueOf(c.getCount()));
        }
    }
}
