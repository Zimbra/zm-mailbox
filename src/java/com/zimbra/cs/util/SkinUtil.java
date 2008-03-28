/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ClassLoaderUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Cos;

public class SkinUtil {

    private static String[] sSkins = null;

    // returns all installed skins 
    private synchronized static String[] getAllInstalledSkinsSorted() throws ServiceException {
        if (sSkins == null) {
            sSkins = loadSkins();
        }
        return sSkins;
    }
    
    public synchronized static void flushSkinCache() throws ServiceException {
        sSkins = null;
    }
    
    private static String[] loadSkins() throws ServiceException {
		ZimbraLog.webclient.debug("Loading skins..." );
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
            for (File f: files) {
				String fname = f.getName();
				if (!fname.startsWith("_") && new File(f, "manifest.xml").exists()) {
					skins.add(fname);
				}
			}
        }
        
        String[] sortedSkins = skins.toArray(new String[skins.size()]);
        Arrays.sort(sortedSkins);

		ZimbraLog.webclient.debug("Skin loading complete." );        
        return sortedSkins;
    }
    
    public static String[] getSkins(Account acct) throws ServiceException {
        String[] installedSkins = getAllInstalledSkinsSorted();
        Set<String> allowedSkins = getAvailableSkins(acct);
    
        String[] availSkins = null;
        if (allowedSkins.size() == 0)
            availSkins = installedSkins;
        else {
            List<String> skins = new ArrayList<String>();
            // take intersection of the two, loop thru installedSkins because it is sorted
            for (String skin : installedSkins) {
                if (allowedSkins.contains(skin))
                    skins.add(skin);
            }
            availSkins = skins.toArray(new String[skins.size()]);
        }
        return availSkins;
    }
    
    private static Set<String> getAvailableSkins(Account acct) throws ServiceException {
        
        // 1) if set on account/cos, use it
        Set<String> skins = acct.getMultiAttrSet(Provisioning.A_zimbraAvailableSkin);
        if (skins.size() > 0)
            return skins;
        
        // 2) if set on Domain, use it
        Domain domain = Provisioning.getInstance().getDomain(acct);
        if (domain == null)
        	return skins;
        return domain.getMultiAttrSet(Provisioning.A_zimbraAvailableSkin);
    }

	public static String chooseSkin(Account acct, String requestedSkin) throws ServiceException {
		String[] installedSkins = getAllInstalledSkinsSorted();

		// If the requested skin is installed and allowed, return it.
		Set<String> allowedSkins = getAvailableSkins(acct);
		if (checkSkin(requestedSkin, installedSkins, allowedSkins)) {
			ZimbraLog.webclient.debug("Loading requested skin "+requestedSkin );
			return requestedSkin;
		}

		// If the account/cos's pref skin is installed and allowed, return it.
		String prefSkin = acct.getAttr(Provisioning.A_zimbraPrefSkin);
		if (checkSkin(prefSkin, installedSkins, allowedSkins)) {
			ZimbraLog.webclient.debug("Loading account skin "+prefSkin );
			return prefSkin;
		}

		// Nothing in ldap has a valid skin. Since beach seens to be our most stable skin, try it.
		String usuallyAvailableSkin = "beach";
		if (prefSkin != usuallyAvailableSkin) {
			if (checkSkin(usuallyAvailableSkin, installedSkins, allowedSkins)) {
				ZimbraLog.webclient.debug("Loading default skin "+usuallyAvailableSkin );
				return usuallyAvailableSkin;
			}
		}

		// Return some installed skin.
		if (installedSkins.length > 0) {
			ZimbraLog.webclient.debug("Returning first known skin "+installedSkins[0] );
			return installedSkins[0];
		}

		// Didn't find an acceptable skin. Return null and hope the client doesn't need it.
		return null;
	}
        
	private static boolean checkSkin(String requestedSkin, String[] installedSkins, Set<String> allowedSkins) {
		if (requestedSkin != null && requestedSkin.length() > 0) {
			for (String skin : installedSkins) {
				if (requestedSkin.equals(skin)) {
					if (allowedSkins.size() == 0 || allowedSkins.contains(skin)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
