/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.lang.reflect.Method;
import java.util.Set;

import com.zimbra.common.util.ZimbraLog;

public class VoiceExtensionUtil {

    /*
     * VoiceStore itself is in an extension(voice).  Each voice service provider is 
     * also an extension.  Voice service providers must register their VoiceStore 
     * implementation class with VoiceStore.register().  
     * 
     * This helper method facilitates the class loading complications.
     */
    @SuppressWarnings("unchecked")
    public static void registerVoiceProvider(String extension, String providerName,
            String className, Set<String> applicableAttrs) {
        try {
            Class vsClass = ExtensionUtil.findClass("com.zimbra.cs.voice.VoiceStore");
            Method method = vsClass.getMethod("register", String.class, String.class,
                    String.class, Set.class);
            method.invoke(vsClass, extension, providerName, className, applicableAttrs);
        } catch (Exception e) {
            ZimbraLog.extensions.error("unable to register VoiceStore: extension=" + 
                    extension + ", className=" + className, e);
        }
    }
}
