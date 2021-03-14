/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.ldap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.*;

import static org.junit.Assert.*;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CosBy;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.qa.QA.Bug;
import com.zimbra.qa.unittest.prov.Names;
import com.zimbra.soap.admin.type.CacheEntryType;

public class TestLdapProvCos extends LdapTest {

    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private Cos createCos(String cosName) throws Exception {
        return createCos(cosName, null);
    }

    private Cos createCos(String cosName, Map<String, Object> attrs) throws Exception {
        return provUtil.createCos(cosName, attrs);
    }
    
    private void deleteCos(Cos cos) throws Exception {
        provUtil.deleteCos( cos);
    }
    
    private void modifyAttr(Entry entry, String attr, String value) throws Exception {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(attr, value);
        prov.modifyAttrs(entry, attrs);
        assertEquals(value, entry.getAttr(attr, false));
    }
    
    @Test
    public void createCos() throws Exception {
        String COS_NAME = Names.makeCosName(genCosName());
        Cos cos = createCos(COS_NAME);
        deleteCos(cos);
    }
    
    @Test
    public void createCosAlreadyExists() throws Exception {
        String COS_NAME = Names.makeCosName(genCosName());
        Cos cos = createCos(COS_NAME);
                
        boolean caughtException = false;
        try {
            prov.createCos(COS_NAME, new HashMap<String, Object>());
        } catch (AccountServiceException e) {
            if (AccountServiceException.COS_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteCos(cos);
    }
    
    @Test
    public void defaultCos() throws Exception {
        Cos cos = prov.get(Key.CosBy.name, Provisioning.DEFAULT_COS_NAME);
        assertNotNull(cos);
    }
    
    @Test
    public void getCos() throws Exception {
        String COS_NAME = Names.makeCosName(genCosName());
        Cos cos = createCos(COS_NAME);
        String cosId = cos.getId();
        
        prov.flushCache(CacheEntryType.cos, null);
        cos = prov.get(Key.CosBy.id, cosId);
        assertEquals(cosId, cos.getId());
        
        prov.flushCache(CacheEntryType.cos, null);
        cos = prov.get(Key.CosBy.name, COS_NAME);
        assertEquals(cosId, cos.getId());
        
        deleteCos(cos);
    }
    
    @Test
    public void getCosNotExist() throws Exception {
        String COS_NAME = Names.makeCosName(genCosName());
        prov.flushCache(CacheEntryType.cos, null);
        Cos cos = prov.get(Key.CosBy.name, COS_NAME);
        assertNull(cos);
    }
    
    @Test
    public void copyCos() throws Exception {
        String COS_NAME = Names.makeCosName(genCosName());
        Cos defaultCos = prov.get(Key.CosBy.name, Provisioning.DEFAULT_COS_NAME);
        Cos copiedCos = prov.copyCos(defaultCos.getId(), COS_NAME);
        
        Map<String, Object> defaultCosAttrs = defaultCos.getAttrs();
        Map<String, Object> copiedCosAttrs = copiedCos.getAttrs();

        for (Map.Entry<String, Object> attr : defaultCosAttrs.entrySet()) {
            String attrName = attr.getKey();
            Object valueInDefaultCos = attr.getValue();
            
            Object valueInCopiedCos = copiedCosAttrs.get(attrName);
            
            // description is not copied over
            if (!Provisioning.A_description.equals(attrName)) {
                assertNotNull(valueInCopiedCos);
            } else {
                continue;
            }
            
            if (valueInDefaultCos instanceof String) {
                assertTrue(valueInCopiedCos instanceof String);
                if (!Provisioning.A_zimbraId.equals(attrName) &&
                        !Provisioning.A_zimbraCreateTimestamp.equals(attrName) &&
                        !Provisioning.A_zimbraACE.equals(attrName) &&
                        !Provisioning.A_cn.equals(attrName) &&
                        !Provisioning.A_description.equals(attrName)) {
                    assertEquals((String) valueInDefaultCos, (String) valueInCopiedCos);
                } else {
                    if (((String) valueInDefaultCos).equals((String) valueInCopiedCos)) {
                        System.out.println("attr: " + attrName);
                        System.out.println("valueInDefaultCos: " + (String) valueInDefaultCos);
                        System.out.println("valueInCopiedCos: " + (String) valueInCopiedCos);
                        fail();
                    }
                }
            } else if (valueInDefaultCos instanceof String[]) {
                assertTrue(valueInCopiedCos instanceof String[]);
                assertEquals(((String[]) valueInDefaultCos).length, 
                        ((String[]) valueInCopiedCos).length);
            }
        }
        
        /*
         * Attr in defaultCosAttrs but not in copiedCosAttrs: description
         * 
         * zimbraCreateTimestamp will be in the default cos only if upgrade 22033 
         * has been run after reset-the-world - i.r. whether TestLdapUpgrade has been
         * run.
         */
        for (String attr : defaultCosAttrs.keySet()) {
            if (!copiedCosAttrs.containsKey(attr)) {
                System.out.println("Attr in defaultCosAttrs but not in copiedCosAttrs: " + attr);
            }
        }
        for (String attr : copiedCosAttrs.keySet()) {
            if (!defaultCosAttrs.containsKey(attr)) {
                System.out.println("Attr in copiedCosAttrs but not in defaultCosAttrs: " + attr);
            }
        }
        if (defaultCosAttrs.containsKey(Provisioning.A_zimbraCreateTimestamp)) {
            assertEquals(defaultCosAttrs.size() - 1, copiedCosAttrs.size());
        } else {
            assertEquals(defaultCosAttrs.size(), copiedCosAttrs.size());
        }
        assertNull(copiedCosAttrs.get(Provisioning.A_description));
        
        deleteCos(copiedCos);
    }
    
    @Test
    public void getAllCos() throws Exception {
        Cos defaultCos = prov.get(Key.CosBy.name, Provisioning.DEFAULT_COS_NAME);
        
        List<Cos> allCos = prov.getAllCos();
        assertEquals(2, allCos.size());  // default and defaultExternal cos
        assertEquals(defaultCos.getId(), allCos.get(0).getId());
    }
    
    @Test
    public void renameCos() throws Exception {
        String OLD_COS_NAME = Names.makeCosName(genCosName("old"));
        String NEW_COS_NAME = Names.makeCosName(genCosName("new"));
        
        Cos cos = createCos(OLD_COS_NAME);
        String cosId = cos.getId();
        
        prov.renameCos(cos.getId(), NEW_COS_NAME);
        Cos renamedCos = prov.get(Key.CosBy.name, NEW_COS_NAME);
        assertEquals(cosId, renamedCos.getId());
        
        deleteCos(cos);
    }
    
    @Test
    public void renameCosToExisting() throws Exception {
        String OLD_COS_NAME = Names.makeCosName(genCosName("old"));
        String NEW_COS_NAME = Provisioning.DEFAULT_COS_NAME;
        
        Cos cos = createCos(OLD_COS_NAME);
        String cosId = cos.getId();
        
        boolean caughtException = false;
        try {
            prov.renameCos(cos.getId(), NEW_COS_NAME);
        } catch (AccountServiceException e) {
            if (AccountServiceException.COS_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteCos(cos);
    }
    
    @Test
    public void domainCos() throws Exception {
        String ATTR_NAME = Provisioning.A_zimbraMailQuota;
        
        final String COS1_NAME = genCosName("1");
        final String COS2_NAME = genCosName("2");
        final String DOMAIN_DEFAULT_COS_NAME = genCosName("domain-default-cos");
        
        final String COS1_VALUE = "10000";
        final String COS2_VALUE = "20000";
        final String DOMAIN_DEFAULT_COS_VALUE = "30000";
        final String ACCOUNT_VALUE = "40000";
        
        final String SYSTEM_DEFAULT_COS_VALUE = 
                prov.get(CosBy.name, Provisioning.DEFAULT_COS_NAME).getAttr(ATTR_NAME);
        
        Map<String, Object> cos1Attrs = Maps.newHashMap();
        cos1Attrs.put(ATTR_NAME, COS1_VALUE);
        Cos cos1 = createCos(COS1_NAME, cos1Attrs);
        
        Map<String, Object> cos2Attrs = Maps.newHashMap();
        cos2Attrs.put(ATTR_NAME, COS2_VALUE);
        Cos cos2 = createCos(COS2_NAME, cos2Attrs);
        
        Map<String, Object> domainDefaultCosAttrs = Maps.newHashMap();
        domainDefaultCosAttrs.put(ATTR_NAME, DOMAIN_DEFAULT_COS_VALUE);
        Cos domainDefaultCos = createCos(DOMAIN_DEFAULT_COS_NAME, domainDefaultCosAttrs);
        
        modifyAttr(domain, Provisioning.A_zimbraDomainDefaultCOSId, domainDefaultCos.getId());

        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);
        final String ACCT_NAME = acct.getName();
        
        // account should inherit the domain default cos value
        assertEquals(DOMAIN_DEFAULT_COS_VALUE, acct.getAttr(ATTR_NAME));
        
        // modify value on the domain default cos, should get the new value
        String newValue = String.valueOf(Long.valueOf(DOMAIN_DEFAULT_COS_VALUE) - 1);
        modifyAttr(domainDefaultCos, ATTR_NAME, newValue);
        assertEquals(newValue, acct.getAttr(ATTR_NAME));
        
        // modify the value back
        modifyAttr(domainDefaultCos, ATTR_NAME, DOMAIN_DEFAULT_COS_VALUE);
        
        // modify domain default cos, should still be the old cos value
        // known bug. This is fine for now - don't have a use case/bug for this, have to fix if needed
        modifyAttr(domain, Provisioning.A_zimbraDomainDefaultCOSId, cos1.getId());
        assertEquals(DOMAIN_DEFAULT_COS_VALUE, acct.getAttr(ATTR_NAME)); 
        
        // flush cache, re-get acct, we now should get the updated cos value
        prov.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(Key.CacheEntryBy.name, ACCT_NAME)});
        acct = prov.get(AccountBy.name, ACCT_NAME);
        assertEquals(COS1_VALUE, acct.getAttr(ATTR_NAME));
        
        // remove domain default cos, should get the system default cos value,
        // but because of the bug, will still be the old cos value (known bug)
        modifyAttr(domain, Provisioning.A_zimbraDomainDefaultCOSId, null);
        assertEquals(COS1_VALUE, acct.getAttr(ATTR_NAME));
        
        // flush cache, we now should get the updated cos value
        prov.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(Key.CacheEntryBy.name, ACCT_NAME)});
        acct = prov.get(AccountBy.name, ACCT_NAME);
        assertEquals(SYSTEM_DEFAULT_COS_VALUE, acct.getAttr(ATTR_NAME));
        
        provUtil.deleteAccount(acct);
    }
    
    @Test
    public void acctCos() throws Exception {
        String ATTR_NAME = Provisioning.A_zimbraMailQuota;
        
        final String COS1_NAME = genCosName("1");
        final String COS2_NAME = genCosName("2");
        final String DOMAIN_DEFAULT_COS_NAME = genCosName("domain-default-cos");
        
        final String COS1_VALUE = "10000";
        final String COS2_VALUE = "20000";
        final String DOMAIN_DEFAULT_COS_VALUE = "30000";
        final String ACCOUNT_VALUE = "40000";
    
        Map<String, Object> cos1Attrs = Maps.newHashMap();
        cos1Attrs.put(ATTR_NAME, COS1_VALUE);
        Cos cos1 = createCos(COS1_NAME, cos1Attrs);
        
        Map<String, Object> cos2Attrs = Maps.newHashMap();
        cos2Attrs.put(ATTR_NAME, COS2_VALUE);
        Cos cos2 = createCos(COS2_NAME, cos2Attrs);
        
        Map<String, Object> domainDefaultCosAttrs = Maps.newHashMap();
        domainDefaultCosAttrs.put(ATTR_NAME, DOMAIN_DEFAULT_COS_VALUE);
        Cos domainDefaultCos = createCos(DOMAIN_DEFAULT_COS_NAME, domainDefaultCosAttrs);
        
        modifyAttr(domain, Provisioning.A_zimbraDomainDefaultCOSId, domainDefaultCos.getId());
        
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);
        final String ACCT_NAME = acct.getName();
        
        // set cos on account
        modifyAttr(acct, Provisioning.A_zimbraCOSId, cos1.getId());
        assertEquals(COS1_VALUE, acct.getAttr(ATTR_NAME));
        
        // modify cos on account
        modifyAttr(acct, Provisioning.A_zimbraCOSId, cos2.getId());
        assertEquals(COS2_VALUE, acct.getAttr(ATTR_NAME));
        
        // remove cos from account, should get the domain default cos
        modifyAttr(acct, Provisioning.A_zimbraCOSId, null);
        assertEquals(DOMAIN_DEFAULT_COS_VALUE, acct.getAttr(ATTR_NAME));
        
        // set cos on account again
        modifyAttr(acct, Provisioning.A_zimbraCOSId, cos1.getId());
        assertEquals(COS1_VALUE, acct.getAttr(ATTR_NAME));
        
        // set the attr directly on account
        modifyAttr(acct, ATTR_NAME, ACCOUNT_VALUE);
        assertEquals(ACCOUNT_VALUE, acct.getAttr(ATTR_NAME));
        
        // remove the account value, should get cos value
        modifyAttr(acct, ATTR_NAME, null);
        assertEquals(COS1_VALUE, acct.getAttr(ATTR_NAME));
    }

    @Test
    @Bug(bug=67716)
    public void bug67716() throws Exception {
        // case does match the case declared in zimbra-attrs.xml
        String ATTR_REAL_ANME = Provisioning.A_zimbraMailQuota;
        String ATTR_LOWERCASE_NAME = ATTR_REAL_ANME.toLowerCase();
        String ATTR_VALUE = "12345";
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(ATTR_LOWERCASE_NAME, ATTR_VALUE);
        Cos cos = createCos(genCosName(), attrs);
        assertEquals(ATTR_VALUE, cos.getAttr(ATTR_REAL_ANME));
        assertEquals(ATTR_VALUE, cos.getAttr(ATTR_LOWERCASE_NAME));
    }
}