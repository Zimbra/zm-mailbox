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

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.qa.unittest.prov.Names;
import com.zimbra.soap.admin.type.CacheEntryType;

public class TestLdapProvZimlet extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
    }
    
    private Zimlet createZimlet(String zimletName) throws Exception {
        Zimlet zimlet = prov.getZimlet(zimletName);
        assertNull(zimlet);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraZimletVersion, "1.0");
        zimlet = prov.createZimlet(zimletName, attrs);
        assertNotNull(zimlet);
        
        prov.flushCache(CacheEntryType.zimlet, null);
        zimlet = prov.getZimlet(zimletName);
        assertNotNull(zimlet);
        assertEquals(zimletName.toLowerCase(), zimlet.getName().toLowerCase());
        
        return zimlet;
    }
    
    private void deleteZimlet(Zimlet zimlet) throws Exception {
        String zimletName = zimlet.getName();
        prov.deleteZimlet(zimletName);
        zimlet = prov.getZimlet(zimletName);
        assertNull(zimlet);
    }
    
    @Test
    public void createZimlet() throws Exception {
        String ZIMLET_NAME = Names.makeZimletName(genZimletName());
        Zimlet zimlet = createZimlet(ZIMLET_NAME);
        
        deleteZimlet(zimlet);
    }
    
    @Test
    public void createZimletAlreadyExists() throws Exception {
        String ZIMLET_NAME = Names.makeZimletName(genZimletName());
        Zimlet zimlet = createZimlet(ZIMLET_NAME);
        
        boolean caughtException = false;
        try {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraZimletVersion, "1.0");
            zimlet = prov.createZimlet(ZIMLET_NAME, attrs);
        } catch (AccountServiceException e) {
            if (AccountServiceException.ZIMLET_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteZimlet(zimlet);
    }
    
    @Test
    public void listAllZimlets() throws Exception {
        List<Zimlet> allZimlets = prov.listAllZimlets();
        // assertEquals(11, allZimlets.size());  // not very reliable, it can change in r-t-w
    }
    
    @Test
    public void getZimlet() throws Exception {
        String ZIMLET_NAME = Names.makeZimletName(genZimletName());
        Zimlet zimlet = createZimlet(ZIMLET_NAME);
        
        prov.flushCache(CacheEntryType.zimlet, null);
        zimlet = prov.getZimlet(ZIMLET_NAME);
        assertEquals(ZIMLET_NAME.toLowerCase(), zimlet.getName().toLowerCase());
        
        deleteZimlet(zimlet);
    }
    
    @Test
    public void getZimletNotExist() throws Exception {
        String ZIMLET_NAME = Names.makeZimletName(genZimletName());
        Zimlet zimlet = prov.getZimlet(ZIMLET_NAME);
        assertNull(zimlet);
    }
}
