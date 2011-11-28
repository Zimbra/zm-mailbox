/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.ldap;

import java.util.HashMap;

import org.junit.*;

import com.zimbra.common.account.Key;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.upgrade.LdapUpgrade;

import static org.junit.Assert.*;

public class TestLdapUpgrade extends LdapTest {

    private static LdapProv prov;
    
    @BeforeClass
    public static void init() throws Exception {
        prov = LdapProv.getInst();
    }
    
    private Cos createCos(String cosName, HashMap<String, Object> attrs) throws Exception {
        Cos cos = prov.get(Key.CosBy.name, cosName);
        assertNull(cos);
        
        cos = prov.createCos(cosName, attrs);
        assertNotNull(cos);
        
        prov.flushCache(CacheEntryType.cos, null);
        cos = prov.get(Key.CosBy.name, cosName);
        assertNotNull(cos);
        assertEquals(cosName.toLowerCase(), cos.getName().toLowerCase());
        
        return cos;
    }
    
    private void deleteCos(Cos cos) throws Exception {
        String codId = cos.getId();
        prov.deleteCos(codId);
        prov.flushCache(CacheEntryType.cos, null);
        cos = prov.get(Key.CosBy.id, codId);
        assertNull(cos);
    }
    
    private Cos getFresh(Cos cos) throws Exception {
        prov.flushCache(CacheEntryType.cos, null);
        cos = prov.get(Key.CosBy.id, cos.getId());
        assertNotNull(cos);
        return cos;
    }
    
    private String[] getArgs(String bug) {
        return new String[] {"-b", bug};
    }
    
    /*
    @Test
    public void bug10287() throws Exception {
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderSendEmail, ProvisioningConstants.FALSE);
        Cos cos = createCos("bug10287", attrs);
        
        LdapUpgrade.upgrade(getArgs("10287"));
        
        cos = getFresh(cos);
        assertEquals(ProvisioningConstants.TRUE, cos.getAttr(Provisioning.A_zimbraPrefCalendarReminderSendEmail));
    }
    */
    
    @Test
    public void bug14531() throws Exception {
        LdapUpgrade.upgrade(getArgs("14531"));
    }
    
    @Test
    public void bug18277() throws Exception {
        LdapUpgrade.upgrade(getArgs("18277"));
    }
    
    @Test
    public void bug22033() throws Exception {
        LdapUpgrade.upgrade(getArgs("22033"));
    }
    
    @Test
    public void bug27075() throws Exception {
        String[] args = new String[] {"-b", "27075", "5.0.12"};
        LdapUpgrade.upgrade(args);
    }
    
    @Test  // TODO: verify me, there is test code in the class
    public void bug29978() throws Exception {
        LdapUpgrade.upgrade(getArgs("29978"));
    }
    
    @Test
    public void bug31694() throws Exception {
        LdapUpgrade.upgrade(getArgs("31694"));
    }
    
    @Test
    public void bug32557() throws Exception {
        LdapUpgrade.upgrade(getArgs("32557"));
    }
    
    @Test
    public void bug32719() throws Exception {
        LdapUpgrade.upgrade(getArgs("32719"));
    }

    @Test
    public void bug33814() throws Exception {
        LdapUpgrade.upgrade(getArgs("33814"));
    }
    
    @Test
    public void bug41000() throws Exception {
        LdapUpgrade.upgrade(getArgs("41000"));
    }

    @Test
    public void bug42828() throws Exception {
        LdapUpgrade.upgrade(getArgs("42828"));
    }
    
    @Test
    public void bug42877() throws Exception {
        LdapUpgrade.upgrade(getArgs("42877"));
    }
    
    @Test
    public void bug42896() throws Exception {
        LdapUpgrade.upgrade(getArgs("42896"));
    }
    
    @Test
    public void bug43147() throws Exception {
        LdapUpgrade.upgrade(getArgs("43147"));
    }
    
    @Test
    public void bug43779() throws Exception {
        LdapUpgrade.upgrade(getArgs("43779"));
    }
    
    @Test
    public void bug46297() throws Exception {
        LdapUpgrade.upgrade(getArgs("46297"));
    }

    @Test
    public void bug46883() throws Exception {
        LdapUpgrade.upgrade(getArgs("46883"));
    }

    @Test
    public void bug46961() throws Exception {
        LdapUpgrade.upgrade(getArgs("46961"));
    }

    @Test
    public void bug47934() throws Exception {
        LdapUpgrade.upgrade(getArgs("47934"));
    }

    @Test
    public void bug50258() throws Exception {
        LdapUpgrade.upgrade(getArgs("50258"));
    }
    
    @Test  // TODO: fix impl
    public void bug50458() throws Exception {
        LdapUpgrade.upgrade(getArgs("50458"));
    }

    @Test
    public void bug50465() throws Exception {
        LdapUpgrade.upgrade(getArgs("50465"));
    }
    
    @Test
    public void bug53745() throws Exception {
        LdapUpgrade.upgrade(getArgs("53745"));
    }

    @Test
    public void bug55649() throws Exception {
        LdapUpgrade.upgrade(getArgs("55649"));
    }

    @Test
    public void bug57039() throws Exception {
        LdapUpgrade.upgrade(getArgs("57039"));
    }

    @Test
    public void bug57425() throws Exception {
        LdapUpgrade.upgrade(getArgs("57425"));
    }

    @Test
    public void bug57855() throws Exception {
        LdapUpgrade.upgrade(getArgs("57855"));
    }
    
    @Test
    public void bug58084() throws Exception {
        LdapUpgrade.upgrade(getArgs("58084"));
    }

    @Test
    public void bug58481() throws Exception {
        LdapUpgrade.upgrade(getArgs("58481"));
    }
    
    @Test
    public void bug58514() throws Exception {
        LdapUpgrade.upgrade(getArgs("58514"));
    }

    @Test
    public void bug59720() throws Exception {
        LdapUpgrade.upgrade(getArgs("59720"));
    }

}
