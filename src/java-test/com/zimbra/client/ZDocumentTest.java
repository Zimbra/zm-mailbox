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

package com.zimbra.client;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;
import com.zimbra.client.ZDocument;
import com.zimbra.common.soap.Element.XMLElement;

public class ZDocumentTest {

    @Test
    public void note() throws Exception {
        String xml =
            "<doc f='' d='1300925565000' rev='2' ms='2' l='0-0-0:16' ver='1' ct='text/plain' id='0-0-0:257' cr='' " +
            "loid='' t='' s='18' md='1300925565000' leb='' name='doc.txt' descEnabled='1' cd='1300925565000'><meta/><fr>This is a document</fr></doc>";
        ZDocument doc = new ZDocument(XMLElement.parseXML(xml));
        Assert.assertEquals(null, Strings.emptyToNull(doc.getFlags()));
        
        xml =
            "<doc f='t' d='1300925565000' rev='3' ms='4' l='0-0-0:16' ver='1' ct='text/plain' id='0-0-0:258' cr='' " +
            "loid='' t='' s='14' md='1300925565000' leb='' name='note.txt' descEnabled='1' cd='1300925565000'><meta/><fr>This is a note</fr></doc>";
        ZDocument note = new ZDocument(XMLElement.parseXML(xml));
        Assert.assertEquals("t", note.getFlags());
    }
}
