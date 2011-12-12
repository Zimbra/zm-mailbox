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
package com.zimbra.qa.unittest.prov.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.account.Key;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.qa.unittest.prov.Names;

public class TestLdapProvXMPPComponent extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    private static Server server;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
        server = prov.getLocalServer();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private XMPPComponent createXMPPComponent(String xmppCpntName) throws Exception {
        XMPPComponent xmppCpnt = prov.get(Key.XMPPComponentBy.name, xmppCpntName);
        assertNull(xmppCpnt);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraXMPPComponentCategory, "whatever");
        attrs.put(Provisioning.A_zimbraXMPPComponentClassName, "whatever");
        attrs.put(Provisioning.A_zimbraXMPPComponentType, "whatever");
        xmppCpnt = prov.createXMPPComponent(xmppCpntName, domain, server, attrs);
        assertNotNull(xmppCpnt);
        
        xmppCpnt = prov.get(Key.XMPPComponentBy.name, xmppCpntName);
        assertNotNull(xmppCpnt);
        assertEquals(xmppCpntName.toLowerCase(), xmppCpnt.getName().toLowerCase());
        
        return xmppCpnt;
    }
    
    private void deleteXMPPComponent(XMPPComponent xmppCpnt)  throws Exception {
        String xmppCpntId = xmppCpnt.getId();
        prov.deleteXMPPComponent(xmppCpnt);
        xmppCpnt = prov.get(Key.XMPPComponentBy.id, xmppCpntId); 
        assertNull(xmppCpnt);
    }
    
    @Test
    public void createXMPPComponent() throws Exception {
        String XMPPCPNT_NAME = Names.makeXMPPName(genXMPPName());
        XMPPComponent xmppCpnt = createXMPPComponent(XMPPCPNT_NAME);
        
        deleteXMPPComponent(xmppCpnt);
    }
    
    @Test
    public void createXMPPComponentAlreadyExists() throws Exception {
        String XMPPCPNT_NAME = Names.makeXMPPName(genXMPPName());
        XMPPComponent xmppCpnt = createXMPPComponent(XMPPCPNT_NAME);
        
        boolean caughtException = false;
        try {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraXMPPComponentCategory, "whatever");
            attrs.put(Provisioning.A_zimbraXMPPComponentClassName, "whatever");
            attrs.put(Provisioning.A_zimbraXMPPComponentType, "whatever");
            prov.createXMPPComponent(XMPPCPNT_NAME, domain, server, attrs);
        } catch (AccountServiceException e) {
            if (AccountServiceException.IM_COMPONENT_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteXMPPComponent(xmppCpnt);
    }
    
    @Test 
    public void getXMPPComponent() throws Exception {
        String XMPPCPNT_NAME = Names.makeXMPPName(genXMPPName());
        XMPPComponent xmppCpnt = createXMPPComponent(XMPPCPNT_NAME);
        String xmppCpntId = xmppCpnt.getId();
        
        xmppCpnt = prov.get(Key.XMPPComponentBy.id, xmppCpntId); 
        assertEquals(xmppCpntId, xmppCpnt.getId());
        
        xmppCpnt = prov.get(Key.XMPPComponentBy.name, XMPPCPNT_NAME); 
        assertEquals(xmppCpntId, xmppCpnt.getId());
        
        // not implemented
        // xmppCpnt = prov.get(XMPPComponentBy.serviceHostname, server.getServiceHostname()); 
        // assertEquals(xmppCpntId, xmppCpnt.getId());
        
        deleteXMPPComponent(xmppCpnt);
    }
    
    @Test 
    public void getXMPPComponentNotExist() throws Exception {
        String XMPPCPNT_NAME = Names.makeXMPPName(genXMPPName());
        XMPPComponent xmppCpnt = prov.get(Key.XMPPComponentBy.name, XMPPCPNT_NAME); 
        assertNull(xmppCpnt);
    }
    
    @Test
    public void getAllXMPPComponents() throws Exception {
        String XMPPCPNT_NAME_1 = Names.makeXMPPName(genXMPPName("1"));
        XMPPComponent xmppCpnt1 = createXMPPComponent(XMPPCPNT_NAME_1);
        
        String XMPPCPNT_NAME_2 = Names.makeXMPPName(genXMPPName("2"));
        XMPPComponent xmppCpnt2 = createXMPPComponent(XMPPCPNT_NAME_2);
        
        List<XMPPComponent> allXMPPCpnts = prov.getAllXMPPComponents();
        assertEquals(2, allXMPPCpnts.size());
        
        Set<String> allXMPPCpntIds = new HashSet<String>();
        for (XMPPComponent xmppCpnt : allXMPPCpnts) {
            allXMPPCpntIds.add(xmppCpnt.getId());
        }
        assertTrue(allXMPPCpntIds.contains(xmppCpnt1.getId()));
        assertTrue(allXMPPCpntIds.contains(xmppCpnt2.getId()));
        
        deleteXMPPComponent(xmppCpnt1);
        deleteXMPPComponent(xmppCpnt2);
    }
}
