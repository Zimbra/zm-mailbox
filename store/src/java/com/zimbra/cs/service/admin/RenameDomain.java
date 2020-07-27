package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.RenameDomainRequest;
import com.zimbra.soap.admin.message.RenameDomainResponse;

public class RenameDomain extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (!zsc.getAuthToken().isAdmin()) {
            throw ServiceException.PERM_DENIED("Can not rename domain");
        }
        Provisioning prov = Provisioning.getInstance();
        RenameDomainRequest req = zsc.elementToJaxb(request);
        Domain domain = prov.get(req.getDomain());
        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(req.getDomain().getKey());
        }
        if (domain.isShutdown())
            throw ServiceException.PERM_DENIED("can not access domain, domain is in " + domain.getDomainStatusAsString() + " status");
        LdapProv lp = LdapProvisioning.getInst();
        lp.renameDomain(domain.getId(), req.getName());
        RenameDomainResponse res = new RenameDomainResponse();
        return JaxbUtil.jaxbToElement(res);
    }
}
