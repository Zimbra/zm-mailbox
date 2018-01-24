/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.client.ZFolder;
import com.zimbra.common.soap.Element;

/**
 * Unit test for {@link ZFolder}.
 *
 * @author ysasaki
 */
public class ZFolderTest {

    @Test
    public void defaultView() throws Exception {
        String xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='appointment'/>";
        ZFolder folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.appointment, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='chat'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.chat, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='contact'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.contact, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='conversation'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.conversation, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='document'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.document, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='message'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.message, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='remote'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.remote, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='search'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.search, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='task'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.task, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='voice'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.voice, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11' view='wiki'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.wiki, folder.getDefaultView());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='1' name='X' ms='1' n='0' l='11'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.unknown, folder.getDefaultView());
        Assert.assertEquals(1, folder.getImapMODSEQ());

        xml = "<folder id='1' rev='1' s='0' i4next='2' i4ms='2' name='X' ms='1' n='0' l='11' view='XXX'/>";
        folder = new ZFolder(Element.parseXML(xml), null, null);
        Assert.assertEquals(ZFolder.View.unknown, folder.getDefaultView());
        Assert.assertEquals(2, folder.getImapMODSEQ());
    }

}
