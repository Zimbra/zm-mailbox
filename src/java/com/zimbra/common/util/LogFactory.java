/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Factory that creates and manages instances of {@link Log}.
 * 
 * @author bburtin
 */
public class LogFactory {

    private static Map<Class, Log> sLogsByClass = new HashMap<Class, Log>();
    private static Map<String, Log> sLogsByName = new HashMap<String, Log>();
    
    public static Log getLog(Class clazz) {
        Log log = null;
        
        synchronized (sLogsByClass) {
            log = sLogsByClass.get(clazz);
            if (log == null) {
                Logger log4jLogger = Logger.getLogger(clazz);
                log = new Log(log4jLogger);
                sLogsByClass.put(clazz, log);
            }
        }
        
        return log;
    }
    
    public static Log getLog(String name) {
        Log log = null;
        
        synchronized (sLogsByName) {
            log = sLogsByName.get(name);
            if (log == null) {
                Logger log4jLogger = Logger.getLogger(name);
                log = new Log(log4jLogger);
                sLogsByName.put(name, log);
            }
        }
        
        return log;
    }
    
}
