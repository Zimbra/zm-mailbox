/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
