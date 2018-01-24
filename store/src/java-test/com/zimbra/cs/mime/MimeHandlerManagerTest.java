/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mime;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.handler.TextEnrichedHandler;
import com.zimbra.cs.mime.handler.TextHtmlHandler;
import com.zimbra.cs.mime.handler.UnknownTypeHandler;

/**
 * Unit test for {@link MimeHandlerManager}.
 *
 * @author ysasaki
 */
public class MimeHandlerManagerTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MockProvisioning prov = new MockProvisioning();
        prov.clearMimeHandlers();

        MockMimeTypeInfo mime = new MockMimeTypeInfo();
        mime.setMimeTypes("text/html");
        mime.setFileExtensions("html", "htm");
        mime.setHandlerClass(TextHtmlHandler.class.getName());
        prov.addMimeType("text/html", mime);

        mime = new MockMimeTypeInfo();
        mime.setMimeTypes("text/enriched");
        mime.setFileExtensions("txe");
        mime.setHandlerClass(TextEnrichedHandler.class.getName());
        prov.addMimeType("text/enriched", mime);

        mime = new MockMimeTypeInfo();
        mime.setHandlerClass(UnknownTypeHandler.class.getName());
        prov.addMimeType("all", mime);

        mime = new MockMimeTypeInfo();
        mime.setMimeTypes("not/exist");
        mime.setFileExtensions("NotExist");
        mime.setHandlerClass("com.zimbra.cs.mime.handler.NotExist");
        prov.addMimeType("not/exist", mime);

        Provisioning.setInstance(prov);
    }

    @Test
    public void html() throws Exception {
        MimeHandler handler = MimeHandlerManager.getMimeHandler(
                "text/html", "filename.html");
        Assert.assertEquals(TextHtmlHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "text/html", null);
        Assert.assertEquals(TextHtmlHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "text/html", "filename.bogus");
        Assert.assertEquals(TextHtmlHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                null, "filename.html");
        Assert.assertEquals(TextHtmlHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "bogus/type", "filename.html");
        Assert.assertEquals(TextHtmlHandler.class, handler.getClass());
    }

    @Test
    public void htm() throws Exception {
        MimeHandler handler = MimeHandlerManager.getMimeHandler(
                "text/html", "filename.htm");
        Assert.assertEquals(TextHtmlHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "text/html", null);
        Assert.assertEquals(TextHtmlHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "text/html", "filename.bogus");
        Assert.assertEquals(TextHtmlHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                null, "filename.htm");
        Assert.assertEquals(TextHtmlHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "bogus/type", "filename.htm");
        Assert.assertEquals(TextHtmlHandler.class, handler.getClass());
    }

    @Test
    public void textEnriched() throws Exception {
        MimeHandler handler = MimeHandlerManager.getMimeHandler(
                "text/enriched", "filename.txe");
        Assert.assertEquals(TextEnrichedHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "text/enriched", null);
        Assert.assertEquals(TextEnrichedHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "text/enriched", "filename.bogus");
        Assert.assertEquals(TextEnrichedHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                null, "filename.txe");
        Assert.assertEquals(TextEnrichedHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "bogus/type", "filename.txe");
        Assert.assertEquals(TextEnrichedHandler.class, handler.getClass());
    }

    @Test
    public void applicationOctetStream() throws Exception {
        MimeHandler handler = MimeHandlerManager.getMimeHandler(
                "application/octet-stream", "filename.exe");
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "application/octet-stream", null);
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "application/octet-stream", "filename.bogus");
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                null, "filename.exe");
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(
                "bogus/type", "filename.exe");
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());
    }

    @Test
    public void nil() throws Exception {
        MimeHandler handler = MimeHandlerManager.getMimeHandler(null, null);
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler(null, "filename.bogus");
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler("bogus/type", null);
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());
    }

    @Test
    public void empty() throws Exception {
        MimeHandler handler = MimeHandlerManager.getMimeHandler("", "");
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler("", "filename.bogus");
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());

        handler = MimeHandlerManager.getMimeHandler("bogus/type", "");
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());
    }

    @Test
    public void classNotFound() throws Exception {
        MimeHandler handler = MimeHandlerManager.getMimeHandler(
                "not/exist", null);
        Assert.assertEquals(UnknownTypeHandler.class, handler.getClass());
    }

}
