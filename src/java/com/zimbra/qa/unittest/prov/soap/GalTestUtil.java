package com.zimbra.qa.unittest.prov.soap;

import org.junit.Assert;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.qa.unittest.TestUtil;

public class GalTestUtil {
    static final String GAL_SYNC_ACCOUNT_NAME = "galsync";

    public static enum GSAType {
        zimbra,
        external,
        both
    }

    public static void disableGalSyncAccount(Provisioning prov, String domainName) 
    throws Exception {
        Domain domain = prov.get(Key.DomainBy.name, domainName);
        
        String[] galSyncAcctIds = domain.getGalAccountId();
        for (String galSyncAcctId : galSyncAcctIds) {
            prov.deleteAccount(galSyncAcctId);
        }
        
        domain.unsetGalAccountId();
    }

    public static void enableGalSyncAccount(Provisioning prov, String domainName) 
    throws Exception {
        GalTestUtil.enableGalSyncAccount(prov, domainName, GSAType.zimbra);
    }

    public static void enableGalSyncAccount(Provisioning prov, String domainName, GSAType type) 
    throws Exception {
        Domain domain = prov.get(Key.DomainBy.name, domainName);
        String[] galSyncAcctIds = domain.getGalAccountId();
        if (galSyncAcctIds.length > 0) {
            // already enabled
            return;
        } else {
            GalTestUtil.createAndSyncGalSyncAccount(
                    TestUtil.getAddress(GAL_SYNC_ACCOUNT_NAME, domainName), 
                    domainName, type);
        }
    
    }

    static void createAndSyncGalSyncAccount(String galSyncAcctName, String domainName, GSAType type) 
    throws Exception {
        String dataSourceName;
        String dataSourceType;
        String folderName;
        if (type == GSAType.zimbra || type == GSAType.both) {
            dataSourceName = "zimbra";
            dataSourceType = "zimbra";
            folderName = "zimbra-gal-contacts";
        } else {
            dataSourceName = "external";
            dataSourceType = "ldap";
            folderName = "external-gal-contacts";
        }
        
        
        SoapTransport transport = TestUtil.getAdminSoapTransport();
        
        //
        // create gal sync account and data sources, then force sync
        //
        String gsaZimbraId = GalTestUtil.createGalSyncAccountOrDataSource(
                transport, galSyncAcctName, domainName, 
                dataSourceName, dataSourceType, folderName);
        
        GalTestUtil.syncGASDataSource(transport, gsaZimbraId, dataSourceName);
        
        if (type == GSAType.both) {
            dataSourceName = "external";
            dataSourceType = "ldap";
            folderName = "external-gal-contacts";
            GalTestUtil.createGalSyncAccountOrDataSource(
                    transport, galSyncAcctName, domainName, 
                    dataSourceName, dataSourceType, folderName);
            GalTestUtil.syncGASDataSource(transport, gsaZimbraId, dataSourceName);
        }
        
        //
        // index the gal sync account (otherwise the first search will fail)
        //
        Element eReIndex = Element.create(transport.getRequestProtocol(), 
                AdminConstants.REINDEX_REQUEST);
        eReIndex.addAttribute(AdminConstants.A_ACTION, "start");
        Element eMbox = eReIndex.addElement(AdminConstants.E_MAILBOX);
        eMbox.addAttribute(AdminConstants.A_ID, gsaZimbraId);
        transport.invoke(eReIndex);
        
        // wait for the reindex to finish
        Thread.sleep(2000);
    }

    static String createGalSyncAccountOrDataSource(SoapTransport transport,
            String galSyncAcctName, String domainName, 
            String dataSourceName, String dataSourceType, String folderName) 
    throws Exception {
        
        Element eCreateReq = Element.create(transport.getRequestProtocol(), 
                AdminConstants.CREATE_GAL_SYNC_ACCOUNT_REQUEST);
        
        eCreateReq.addAttribute(AdminConstants.E_NAME, dataSourceName);
        eCreateReq.addAttribute(AdminConstants.E_DOMAIN, domainName);
        eCreateReq.addAttribute(AdminConstants.A_TYPE, dataSourceType);
        eCreateReq.addAttribute(AdminConstants.E_FOLDER, folderName);
        
        Element eAccount = eCreateReq.addElement(AdminConstants.E_ACCOUNT);
        eAccount.addAttribute(AdminConstants.A_BY, AccountBy.name.name());
        eAccount.setText(galSyncAcctName);
        
        Element response = transport.invoke(eCreateReq);
        
        eAccount = response.getElement(AdminConstants.E_ACCOUNT);
        String name = eAccount.getAttribute(AccountConstants.A_NAME);
        String id = eAccount.getAttribute(AccountConstants.A_ID);
        Assert.assertEquals(galSyncAcctName, name);
        
        return id;
    }

    static void syncGASDataSource(SoapTransport transport, String gsaZimbraId, String dataSourceName) 
    throws Exception {
        Element eSyncReq = Element.create(transport.getRequestProtocol(), 
                AdminConstants.SYNC_GAL_ACCOUNT_REQUEST);
        
        Element eAccount = eSyncReq.addElement(AdminConstants.E_ACCOUNT);
        eAccount.addAttribute(AccountConstants.A_ID, gsaZimbraId);
        
        Element eDataSource = eAccount.addElement(AdminConstants.E_DATASOURCE);
        eDataSource.addAttribute(AdminConstants.A_RESET, "TRUE");
        eDataSource.addAttribute(AdminConstants.A_BY, AccountBy.name.name());
        eDataSource.setText(dataSourceName);
        
        transport.invoke(eSyncReq);
    }


}
