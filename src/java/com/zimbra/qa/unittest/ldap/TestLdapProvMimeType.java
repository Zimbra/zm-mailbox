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

import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.mime.handler.UnknownTypeHandler;

public class TestLdapProvMimeType extends LdapTest {
    private static Provisioning prov;
    
    @BeforeClass
    public static void init() throws Exception {
        prov = new LdapProvTestUtil().getProv();
    }
    
    @Test
    public void getMimeTypes() throws Exception {
        String MIME_TYPE = "all";
        List<MimeTypeInfo> mimeTypes = prov.getMimeTypes(MIME_TYPE);
        assertEquals(1, mimeTypes.size());
        assertEquals(UnknownTypeHandler.class.getSimpleName(), mimeTypes.get(0).getHandlerClass());
    }
    
    @Test
    public void getAllMimeTypes() throws Exception {
        List<MimeTypeInfo> allMimeType = prov.getAllMimeTypes();
        assertEquals(6, allMimeType.size()); // mime types installed by r-t-w
    }
    
}
