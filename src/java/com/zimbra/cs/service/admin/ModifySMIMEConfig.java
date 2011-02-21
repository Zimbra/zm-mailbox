package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.ldap.LdapSMIMEConfig;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifySMIMEConfig extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element eConfig = request.getElement(AdminConstants.E_CONFIG);
        String configName = eConfig.getAttribute(AdminConstants.A_NAME);
        
        String op = eConfig.getAttribute(AdminConstants.A_OP, AdminConstants.OP_MODIFY);
        
        Map<String, Object> attrs = AdminService.getAttrs(eConfig);
        
        if (AdminConstants.OP_REMOVE.equals(op)) {
            if (!attrs.isEmpty()) {
                throw ServiceException.INVALID_REQUEST("attr map must not be preset for remove op", null);
            }
        }
        
        String entryName; // for logging
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
            
            if (AdminConstants.OP_REMOVE.equals(op)) {
                checkRightSetAllSMIMEAttrs(zsc, context, domain);
                prov.removeDomainSMIMEConfig(domain, configName);
            } else {
                checkDomainRight(zsc, domain, attrs);
                prov.modifyDomainSMIMEConfig(domain, configName, attrs);
            }
            
            entryName = domain.getName();
        } else {
            Config config = prov.getConfig();
            
            if (AdminConstants.OP_REMOVE.equals(op)) {
                checkRightSetAllSMIMEAttrs(zsc, context, config);
                prov.removeConfigSMIMEConfig(configName);
            } else {
                checkRight(zsc, context, config, attrs);
                prov.modifyConfigSMIMEConfig(configName, attrs);
            }
            
            entryName = config.getLabel();
        }

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "ModifySMIMEConfig","name", entryName}, attrs));     

        Element response = zsc.createElement(AdminConstants.MODIFY_SMIME_CONFIG_RESPONSE);
        return response;
    }
    
    private void checkRightSetAllSMIMEAttrs(ZimbraSoapContext zsc, Map<String, Object> context, Entry entry) 
    throws ServiceException {
        AdminAccessControl.SetAttrsRight sar = new AdminAccessControl.SetAttrsRight();
        for (String smimeAttr : LdapSMIMEConfig.getAllSMIMEAttributes()) {
            sar.addAttr(smimeAttr);
        }
        checkRight(zsc, context, entry, sar);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyDomain.getName(), "domain"));

        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyGlobalConfig.getName(), "global config"));
    }
    
}
