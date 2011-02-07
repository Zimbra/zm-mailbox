package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.ZAttrProvisioning.CalResType;

import junit.framework.TestCase;

public class TestSearchGal extends TestCase {
    
    private static final String GAL_SYNC_ACCOUNT_NAME = "galsync";
    
    /*
    static void authAdmin(SoapTransport transport, String acctName) throws Exception {
        
        Element request = Element.create(transport.getRequestProtocol(), AdminConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, AccountBy.name.name()).setText(acctName);
        request.addElement(AccountConstants.E_PASSWORD).setText("test123");
        
        Element response = transport.invoke(request);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(authToken);
    }
    */
    
    static void authUser(SoapTransport transport, String acctName) throws Exception {
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, AccountBy.name.name()).setText(acctName);
        request.addElement(AccountConstants.E_PASSWORD).setText("test123");
        
        Element response = transport.invoke(request);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(authToken);
    }
    
    static void disableGalSyncAccount(String domainName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        Domain domain = prov.get(DomainBy.name, domainName);
        
        String[] galSyncAcctIds = domain.getGalAccountId();
        for (String galSyncAcctId : galSyncAcctIds) {
            prov.deleteAccount(galSyncAcctId);
        }
        
        domain.unsetGalAccountId();
    }
    
    static void createCalendarResource(String name, String displayName,
            CalResType type, int capacity, String site) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, displayName);
        attrs.put(Provisioning.A_zimbraCalResType, type.name());
        attrs.put(Provisioning.A_zimbraCalResCapacity, String.valueOf(capacity));
        attrs.put(Provisioning.A_zimbraCalResSite, site);
        
        prov.createCalendarResource(name, "test123", attrs);
    }
  
    static void enableGalSyncAccount(String domainName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        Domain domain = prov.get(DomainBy.name, domainName);
        String[] galSyncAcctIds = domain.getGalAccountId();
        if (galSyncAcctIds.length > 0) {
            // already enabled
            return;
        } else {
            createAndSyncGalSyncAccount("galsync@" + domainName, domainName);
        }

    }
    
    static void createAndSyncGalSyncAccount(String galSyncAcctName, String domainName) throws Exception {
        String dataSourceName = "zimbra";
        
        SoapTransport transport = TestUtil.getAdminSoapTransport();
        
        // create gal sync account
        Element eCreateReq = Element.create(transport.getRequestProtocol(), AdminConstants.CREATE_GAL_SYNC_ACCOUNT_REQUEST);
        
        eCreateReq.addAttribute(AdminConstants.E_NAME, dataSourceName);
        eCreateReq.addAttribute(AdminConstants.E_DOMAIN, domainName);
        eCreateReq.addAttribute(AdminConstants.A_TYPE, "zimbra");
        eCreateReq.addAttribute(AdminConstants.E_FOLDER, "zimbra-gal-contacts");
        
        Element eAccount = eCreateReq.addElement(AdminConstants.E_ACCOUNT);
        eAccount.addAttribute(AdminConstants.A_BY, AccountBy.name.name());
        eAccount.setText(galSyncAcctName);
        
        Element response = transport.invoke(eCreateReq);
        
        eAccount = response.getElement(AdminConstants.E_ACCOUNT);
        String name = eAccount.getAttribute(AccountConstants.A_NAME);
        String id = eAccount.getAttribute(AccountConstants.A_ID);
        TestCase.assertEquals(galSyncAcctName, name);
        
        // sync gal sync account
        Element eSyncReq = Element.create(transport.getRequestProtocol(), AdminConstants.SYNC_GAL_ACCOUNT_REQUEST);
        
        eAccount = eSyncReq.addElement(AdminConstants.E_ACCOUNT);
        eAccount.addAttribute(AccountConstants.A_ID, id);
        
        Element eDataSource = eAccount.addElement(AdminConstants.E_DATASOURCE);
        eDataSource.addAttribute(AdminConstants.A_RESET, "TRUE");
        eDataSource.addAttribute(AdminConstants.A_BY, AccountBy.name.name());
        eDataSource.setText(dataSourceName);
        
        transport.invoke(eSyncReq);
        
        // index the gal sync account (otherwise the first search will fail)
        Element eReIndex = Element.create(transport.getRequestProtocol(), AdminConstants.REINDEX_REQUEST);
        eReIndex.addAttribute(AdminConstants.A_ACTION, "start");
        Element eMbox = eReIndex.addElement(AdminConstants.E_MAILBOX);
        eMbox.addAttribute(AdminConstants.A_ID, id);
        transport.invoke(eReIndex);
        
        // wait for the reindex to finish
        Thread.sleep(2000);
        
    }

}
