/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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

package com.zimbra.cs.extension;

import com.zimbra.common.service.ServiceException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionDispatcherServletTest {

    @Mock
    private ZimbraExtension mockExtension1;
    @Mock
    private ZimbraExtension mockExtension2;
    @Mock
    private ZimbraExtension mockExtension3;

    @Before
    public void setup() {
        Mockito.when(mockExtension1.getName()).thenReturn("mockExtension1");
        Mockito.when(mockExtension2.getName()).thenReturn("mockExtension2");
        Mockito.when(mockExtension3.getName()).thenReturn("mockExtension3");
    }

    @Test
    public void testRegister() throws ServiceException {
        ExtensionDispatcherServlet.register(mockExtension1, getExtensionHttpHandler("handlerPath1"));
        ExtensionDispatcherServlet.register(mockExtension2, getExtensionHttpHandler("handlerPath2"));
        ExtensionDispatcherServlet.register(mockExtension3, getExtensionHttpHandler("handlerPath3"));
        Assert.assertNotNull(ExtensionDispatcherServlet.getHandler("handlerPath1"));
        Assert.assertNotNull(ExtensionDispatcherServlet.getHandler("handlerPath2"));
        Assert.assertNotNull(ExtensionDispatcherServlet.getHandler("handlerPath3"));
    }

    @Test(expected = ServiceException.class)
    public void testDuplicatePathRegister() throws ServiceException {
        ExtensionDispatcherServlet.register(mockExtension1, getExtensionHttpHandler("handlerPath1"));
        ExtensionDispatcherServlet.register(mockExtension2, getExtensionHttpHandler("handlerPath1"));
    }

    @Test
    public void testUnregister() throws ServiceException {
        ExtensionDispatcherServlet.register(mockExtension1, getExtensionHttpHandler("handlerPath1"));
        ExtensionDispatcherServlet.register(mockExtension2, getExtensionHttpHandler("handlerPath2"));
        ExtensionDispatcherServlet.register(mockExtension3, getExtensionHttpHandler("handlerPath3"));
        Assert.assertNotNull(ExtensionDispatcherServlet.getHandler("handlerPath1"));
        Assert.assertNotNull(ExtensionDispatcherServlet.getHandler("handlerPath2"));
        Assert.assertNotNull(ExtensionDispatcherServlet.getHandler("handlerPath3"));

        ExtensionDispatcherServlet.unregister(mockExtension1);
        Assert.assertNull(ExtensionDispatcherServlet.getHandler("handlerPath1"));
        ExtensionDispatcherServlet.unregister(mockExtension2);
        Assert.assertNull(ExtensionDispatcherServlet.getHandler("handlerPath2"));
    }

    @After
    public void tearDown() throws IllegalAccessException, NoSuchFieldException {
        Field fieldSHandlers = ExtensionDispatcherServlet.class.getDeclaredField("sHandlers");
        fieldSHandlers.setAccessible(true);
        fieldSHandlers.set(null, Collections.synchronizedMap(new HashMap()));

        Field fieldExtensionHandlers = ExtensionDispatcherServlet.class.getDeclaredField("EXTENSION_HANDLERS");
        fieldExtensionHandlers.setAccessible(true);
        fieldExtensionHandlers.set(null, new ConcurrentHashMap<>());
    }

    private ExtensionHttpHandler getExtensionHttpHandler(String handlerPath) {
        return new ExtensionHttpHandler() {
            @Override
            public String getPath() {
                return handlerPath;
            }
        };
    }
}