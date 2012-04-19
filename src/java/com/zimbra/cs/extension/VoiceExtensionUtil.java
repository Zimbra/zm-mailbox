/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
