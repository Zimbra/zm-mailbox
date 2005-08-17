/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetAllAccounts extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
	public Element handle(Element request, Map context) throws ServiceException {
	    
        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
        Element response = null;

        Element d = request.getOptionalElement(AdminService.E_DOMAIN);
        if (d != null) {
            String key = d.getAttribute(AdminService.A_BY);
            String value = d.getText();
	    
            Domain domain = null;
        
            if (key.equals(BY_NAME)) {
                domain = prov.getDomainByName(value);
            } else if (key.equals(BY_ID)) {
                domain = prov.getDomainById(value);
            } else {
                throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
            }
	    
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(value);
            response = lc.createElement(AdminService.GET_ALL_ACCOUNTS_RESPONSE);
            doDomain(response, domain);

        } else {
            response = lc.createElement(AdminService.GET_ALL_ACCOUNTS_RESPONSE);
            List domains = prov.getAllDomains();
            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
                Domain domain = (Domain) dit.next();
                doDomain(response, domain);                
            }
        }
        return response;        
	}
    
    public static void doDomain(Element e, Domain d) throws ServiceException {
        List accounts = d.getAllAccounts();
        for (Iterator it=accounts.iterator(); it.hasNext(); ) {
            GetAccount.doAccount(e, (Account) it.next());
        }        
    }
}
