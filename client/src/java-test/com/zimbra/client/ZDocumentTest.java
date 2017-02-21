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

package com.zimbra.client;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;
import com.zimbra.client.ZDocument;
import com.zimbra.common.soap.Element;

public class ZDocumentTest {

    @Test
    public void note() throws Exception {
        String xml =
            "<doc f='' d='1300925565000' rev='2' ms='2' l='0-0-0:16' ver='1' ct='text/plain' id='0-0-0:257' cr='' " +
            "loid='' t='' s='18' md='1300925565000' leb='' name='doc.txt' descEnabled='1' cd='1300925565000'><meta/><fr>This is a document</fr></doc>";
        ZDocument doc = new ZDocument(Element.parseXML(xml));
        Assert.assertEquals(null, Strings.emptyToNull(doc.getFlags()));
        
        xml =
            "<doc f='t' d='1300925565000' rev='3' ms='4' l='0-0-0:16' ver='1' ct='text/plain' id='0-0-0:258' cr='' " +
            "loid='' t='' s='14' md='1300925565000' leb='' name='note.txt' descEnabled='1' cd='1300925565000'><meta/><fr>This is a note</fr></doc>";
        ZDocument note = new ZDocument(Element.parseXML(xml));
        Assert.assertEquals("t", note.getFlags());
    }
}
