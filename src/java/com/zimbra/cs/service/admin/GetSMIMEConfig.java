package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class GetSMIMEConfig extends AdminDocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String configName = null;
        Element eConfig = request.getOptionalElement(AdminConstants.E_CONFIG);
        if (eConfig != null) {
            configName = eConfig.getAttribute(AdminConstants.A_NAME);
        }
        
        AttrRightChecker attrRightChecker;
        Map<String, Map<String, Object>> smimeConfigs;
        
        Element eDomain = request.getOptionalElement(AdminConstants.E_DOMAIN);
        if (eDomain != null) {
            DomainBy domainBy = DomainBy.fromString(eDomain.getAttribute(AdminConstants.A_BY));
            String domainStr = eDomain.getText();
            
            Domain domain = prov.get(domainBy, domainStr);
            if (domain == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(domainStr);
            }
            
            if (domain.isShutdown()) {
                throw ServiceException.PERM_DENIED("can not access domain, domain is in " + domain.getDomainStatusAsString() + " status");
            }
            
            attrRightChecker = checkRight(zsc, context, domain, AdminRight.PR_ALWAYS_ALLOW).getAttrRightChecker(domain);
            smimeConfigs = prov.getDomainSMIMEConfig(domain, configName);
        } else {
            Config config = prov.getConfig();
            
            attrRightChecker = checkRight(zsc, context, config, AdminRight.PR_ALWAYS_ALLOW).getAttrRightChecker(config);
            smimeConfigs = prov.getConfigSMIMEConfig(configName);
        }

        Element response = zsc.createElement(AdminConstants.GET_SMIME_CONFIG_RESPONSE);
        encodeSMIMEConfigs(response, smimeConfigs, attrRightChecker);
        return response;
    }
    
    private void encodeSMIMEConfigs(Element response, Map<String, Map<String, Object>> smimeConfigs, AttrRightChecker attrRightChecker) {
        for (Map.Entry<String, Map<String, Object>> config : smimeConfigs.entrySet()) {
            String configName = config.getKey();
            Map<String, Object> configAttrs = config.getValue();
            Element eConfig = response.addElement(AdminConstants.E_CONFIG);
            eConfig.addAttribute(AdminConstants.A_NAME, configName);
            ToXML.encodeAttrs(eConfig, configAttrs, null, attrRightChecker);
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyDomain.getName(), "domain"));

        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyGlobalConfig.getName(), "global config"));
    }
}
