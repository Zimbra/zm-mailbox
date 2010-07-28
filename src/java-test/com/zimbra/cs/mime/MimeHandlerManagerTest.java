/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.mime;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
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
    public static void init() {
        MockProvisioning mock = new MockProvisioning();
        MockMimeTypeInfo mime = new MockMimeTypeInfo();
        mime.setMimeTypes("text/html");
        mime.setFileExtensions("html", "htm");
        mime.setHandlerClass(TextHtmlHandler.class.getName());
        mock.addMimeType("text/html", mime);

        mime = new MockMimeTypeInfo();
        mime.setMimeTypes("text/enriched");
        mime.setFileExtensions("txe");
        mime.setHandlerClass(TextEnrichedHandler.class.getName());
        mock.addMimeType("text/enriched", mime);

        mime = new MockMimeTypeInfo();
        mime.setHandlerClass(UnknownTypeHandler.class.getName());
        mock.addMimeType("all", mime);

        mime = new MockMimeTypeInfo();
        mime.setMimeTypes("not/exist");
        mime.setFileExtensions("NotExist");
        mime.setHandlerClass("com.zimbra.cs.mime.handler.NotExist");
        mock.addMimeType("not/exist", mime);

        Provisioning.setInstance(mock);
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
