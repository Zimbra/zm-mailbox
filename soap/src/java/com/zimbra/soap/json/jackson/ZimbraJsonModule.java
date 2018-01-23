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
package com.zimbra.soap.json.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Module that augments basic module to handle differences between
 * Zimbra-style JSON and standard Jackson-style JSON.
 */
public class ZimbraJsonModule extends SimpleModule {
    /**
     * 
     */
    private static final long serialVersionUID = -4809416138833975969L;
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
