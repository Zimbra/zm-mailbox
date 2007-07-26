package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyDomainStatus extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminConstants.E_ID);
        String status = request.getAttribute(AdminConstants.E_STATUS);
        
        Domain domain = prov.get(DomainBy.id, id);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(id);
        
        // pass in true to checkImmutable
        prov.modifyDomainStatus(domain, status);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "ModifyDomainStatus","name", domain.getName()}));     

        Element response = lc.createElement(AdminConstants.MODIFY_DOMAIN_STATUS_RESPONSE);
        Element eDomain = response.addElement(AdminConstants.E_DOMAIN);
        eDomain.addAttribute(AdminConstants.A_NAME, domain.getName());
        eDomain.addAttribute(AdminConstants.A_ID, domain.getId());
        Element eStatus = eDomain.addElement(AdminConstants.E_STATUS);
        eStatus.setText(domain.getDomainStatus());
        
        return response;
    }

}
