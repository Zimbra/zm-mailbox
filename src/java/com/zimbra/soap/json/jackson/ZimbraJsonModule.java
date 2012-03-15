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
package com.zimbra.soap.json.jackson;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.module.SimpleModule;

/**
 * Module that augments basic module to handle differences between
 * Zimbra-style JSON and standard Jackson-style JSON.
 */
public class ZimbraJsonModule extends SimpleModule {
    private final static Version VERSION = new Version(0, 1, 0, null);

    public ZimbraJsonModule() {
        super("ZimbraJsonModule", VERSION);
    }

    @Override
    public void setupModule(SetupContext context) {
        // Need to modify BeanSerializer that is used
        context.addBeanSerializerModifier(new ZimbraBeanSerializerModifier());
    }
}
