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

package com.zimbra.cs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ClassLoaderUtil;

public class SkinUtil {

    
    private static String[] sSkins;

    // returns all installed skins 
    public synchronized static String[] getAllSkinsSorted() throws ServiceException {
        if (sSkins == null) {
            sSkins = loadSkins();
        }
        return sSkins;
    }
    
    private static String findSkin(ClassLoader classLoader, File dir) throws ServiceException {
        String skinName = null;
        
        String relResName = "./" + dir.getName() + "/" + "skin.properties";
        
        URL url = classLoader.getResource(relResName);
        
        if (url != null) {
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(new File(url.getFile())));
                skinName = props.getProperty("SkinName");
            } catch (IOException e) {
                // no such property
            }
        }
        
        return skinName;
    }
    
    private static String[] loadSkins() throws ServiceException {
        String skinsDir = LC.skins_directory.value();
        ClassLoader classLoader = ClassLoaderUtil.getClassLoaderByDirectory(skinsDir);
        if (classLoader == null)
            throw ServiceException.FAILURE("unable to get class loader for directory " + 
                                           skinsDir + " configured in localconfig key " + LC.skins_directory.key(), 
                                           null);
        
        List<String> skins = new ArrayList<String>();
        
        File dir = new File(skinsDir);
        File[] files = dir.listFiles();
        
        if (files != null)  {
            for (File f : files) {
                if (f.isDirectory()) {
                    String skin = findSkin(classLoader, f);
                    if (skin != null)
                        skins.add(skin);
                }
            }
        }
        
        String[] sortedSkins = skins.toArray(new String[skins.size()]);
        Arrays.sort(sortedSkins);
        
        return sortedSkins;
    }
        
    
    
}
