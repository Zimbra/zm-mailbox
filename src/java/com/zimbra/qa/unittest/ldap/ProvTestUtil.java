package com.zimbra.qa.unittest.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CalendarResourceBy;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapHelper;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.admin.type.DataSourceType;

public class ProvTestUtil {
    private LdapProv prov;
    
    public ProvTestUtil() throws Exception {
        prov = LdapProv.getInst();
    }
    
    public LdapProv getProv() {
        return prov;
    }
    
    public Domain createDomain(String domainName) throws Exception {
        return createDomain(domainName, null);
    }
    
    public Domain createDomain(String domainName, Map<String, Object> attrs) 
    throws Exception {
        if (attrs == null) {
            attrs = new HashMap<String, Object>();
        }
        
        Domain domain = prov.get(Key.DomainBy.name, domainName);
        assertNull(domain);
        domain = prov.createDomain(domainName, attrs);
        assertNotNull(domain);
        
        prov.flushCache(CacheEntryType.domain, null);
        domain = prov.get(Key.DomainBy.name, domainName);
        assertNotNull(domain);
        assertEquals(IDNUtil.toAsciiDomainName(domainName).toLowerCase(), 
                domain.getName().toLowerCase());
        
        return domain;
    }
    
    public void deleteDomain(Domain domain) throws Exception {
        String domainId = domain.getId();
        prov.deleteDomain(domainId);
        domain = prov.get(Key.DomainBy.id, domainId);
        assertNull(domain);
    }
    
    public Account createAccount(String localPart, Domain domain) throws Exception {
        return createAccount(localPart, domain, null);
    }
    
    public Account createAccount(String localPart, Domain domain, Map<String, Object> attrs)
    throws Exception {
        String acctName = TestUtil.getAddress(localPart, domain.getName());
        prov.flushCache(CacheEntryType.account, null);
        Account acct = prov.get(AccountBy.name, acctName);
        assertNull(acct);
                
        acct = prov.createAccount(acctName, "test123", attrs);
        assertNotNull(acct);
        
        prov.flushCache(CacheEntryType.account, null);
        acct = prov.get(AccountBy.name, acctName);
        assertNotNull(acct);
        assertEquals(acctName.toLowerCase(), acct.getName().toLowerCase());
        
        return acct;
    }
    
    public void deleteAccount(Account acct) throws Exception {
        String acctId = acct.getId();
        prov.deleteAccount(acctId);
        prov.flushCache(CacheEntryType.account, null);
        acct = prov.get(AccountBy.id, acctId);
        assertNull(acct);
    }
    
    public CalendarResource createCalendarResource(String localPart, Domain domain, 
            Map<String, Object> attrs)
    throws Exception {
        if (attrs == null) {
            attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_displayName, localPart);
            attrs.put(Provisioning.A_zimbraCalResType, Provisioning.CalResType.Equipment.name());
        }
        
        String crName = TestUtil.getAddress(localPart, domain.getName());
        prov.flushCache(CacheEntryType.account, null);
        CalendarResource cr = prov.get(CalendarResourceBy.name, crName);
        assertNull(cr);
                
        cr = prov.createCalendarResource(crName, "test123", attrs);
        assertNotNull(cr);
        
        prov.flushCache(CacheEntryType.account, null);
        cr = prov.get(CalendarResourceBy.name, crName);
        assertNotNull(cr);
        assertEquals(crName.toLowerCase(), cr.getName().toLowerCase());
        
        return cr;
    }
    
    public Group createGroup(String localPart, Domain domain, 
            Map<String, Object> attrs, boolean dynamic) 
    throws Exception {
        String groupName = TestUtil.getAddress(localPart, domain.getName());
        Group group = prov.getGroup(Key.DistributionListBy.name, groupName);
        assertNull(group);
        
        if (attrs == null) {
            attrs = new HashMap<String, Object>();
        }
                
        group = prov.createGroup(groupName, attrs, dynamic);
        assertNotNull(group);
        
        prov.flushCache(CacheEntryType.group, null);
        group = prov.getGroup(Key.DistributionListBy.name, groupName);
        assertNotNull(group);
        assertEquals(groupName.toLowerCase(), group.getName().toLowerCase());
        
        return group;
    }
    
    public void deleteGroup(Group group) 
    throws Exception {
        String groupId = group.getId();
        prov.deleteGroup(groupId);
        prov.flushCache(CacheEntryType.group, null);
        group = prov.get(Key.DistributionListBy.id, groupId);
        assertNull(group);
    }
    
    public DistributionList createDistributionList(String localPart, 
            Domain domain, Map<String, Object> attrs) throws Exception {
        return (DistributionList) createGroup(localPart, domain, attrs, false);
    }
    
    public void deleteDistributionList(DistributionList dl) 
    throws Exception {
        String dlId = dl.getId();
        prov.deleteDistributionList(dl.getId());
        prov.flushCache(CacheEntryType.group, null);
        dl = prov.get(Key.DistributionListBy.id, dlId);
        assertNull(dl);
    }
    
    public DynamicGroup createDynamicGroup(String localPart, 
            Domain domain, Map<String, Object> attrs) throws Exception {
        return (DynamicGroup) createGroup(localPart, domain, attrs, true);
    }
    
    public void deleteDynamicGroup(DynamicGroup group) throws Exception {
        deleteGroup(group);
    }
    
    public Cos createCos(String cosName) throws Exception {
        return createCos(cosName, null);
    }
    
    public Cos createCos(String cosName, Map<String, Object> attrs) 
    throws Exception {
        Cos cos = prov.get(Key.CosBy.name, cosName);
        assertNull(cos);
        
        if (attrs == null) {
            attrs = new HashMap<String, Object>();
        }
        
        cos = prov.createCos(cosName, attrs);
        assertNotNull(cos);
        
        prov.flushCache(CacheEntryType.cos, null);
        cos = prov.get(Key.CosBy.name, cosName);
        assertNotNull(cos);
        assertEquals(cosName.toLowerCase(), cos.getName().toLowerCase());
        
        return cos;
    }

    public void deleteCos(Cos cos) throws Exception {
        String codId = cos.getId();
        prov.deleteCos(codId);
        prov.flushCache(CacheEntryType.cos, null);
        cos = prov.get(Key.CosBy.id, codId);
        assertNull(cos);
    }
    
    public Server createServer(String serverName, Map<String, Object> attrs) 
    throws Exception {
        if (attrs == null) {
            attrs = new HashMap<String, Object>();
        }
        Server server = prov.get(Key.ServerBy.name, serverName);
        assertNull(server);
        
        server = prov.createServer(serverName, attrs);
        assertNotNull(server);
        
        server = prov.get(Key.ServerBy.name, serverName);
        assertNotNull(server);
        assertEquals(serverName.toLowerCase(), server.getName().toLowerCase());
        
        return server;
    }
    
    public void deleteServer(Server server) throws Exception {
        String serverId = server.getId();
        prov.deleteServer(serverId);
        server = prov.get(Key.ServerBy.id, serverId);
        assertNull(server);
    }
    
    public DataSource createDataSourceRaw(Account acct, String dataSourceName) 
    throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_TRUE);
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "123");
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
        attrs.put(Provisioning.A_zimbraDataSourceHost, "zimbra.com");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "9999");
        return prov.createDataSource(acct, DataSourceType.pop3, dataSourceName, attrs);
    }
    
    public DataSource createDataSource(Account acct, String dataSourceName) 
    throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        DataSource dataSource = prov.get(acct, Key.DataSourceBy.name, dataSourceName);
        assertNull(dataSource);
        
        dataSource = createDataSourceRaw(acct, dataSourceName);
        assertNotNull(dataSource);
        
        prov.flushCache(CacheEntryType.account, null);
        dataSource = prov.get(acct, Key.DataSourceBy.name, dataSourceName);
        assertNotNull(dataSource);
        assertEquals(dataSourceName, dataSource.getName());
        
        return dataSource;
    }

    public void deleteDataSource(Account acct, DataSource dataSource) 
    throws Exception {
        String dataSourceId = dataSource.getId();
        prov.deleteDataSource(acct, dataSourceId);
        prov.flushCache(CacheEntryType.account, null);
        dataSource = prov.get(acct, Key.DataSourceBy.id, dataSourceId);
        assertNull(dataSource);
    }
}
