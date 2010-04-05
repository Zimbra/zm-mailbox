/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.zimlet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

public class ZimletPresence {

    public static enum Presence {
        mandatory('!'),
        enabled('+'),
        disabled('-');
        
        private char mPrefix;
        Presence(char prefix) {
            mPrefix = prefix;
        }
        
        public static Presence fromString(String presence) throws ServiceException {
            try {
                return Presence.valueOf(presence);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown zimlet presense: " + presence, e);
            }
        }
        
        public static Presence fromPrefix(char prefix) {
            if (prefix == mandatory.mPrefix) {
                return Presence.mandatory;
            } else if (prefix == enabled.mPrefix) {
                return Presence.enabled;
            } else if (prefix == disabled.mPrefix) {
                return Presence.disabled;
            } else
                return null; // leave it to the caller to decide that action
        }
        
        public char prefix() {
            return mPrefix;
        }
    }
    
    /*
     * Map of zimlet name -> Presence
     * 
     * Key of the map is zimlet name *without* prefix.
     * 
     * A later put of the same zimlet name will overwrite previously put 
     * zimlet with different prefix.
     * 
     * e.g. if current map contains: "zimlet_foo" => mandatory
     *      then a put("-zimlet_foo");
     *      will overwrite the previous presence.
     *      after this put, getPresence("zimlet_foo") will return Presence.disabled.
     */
    Map<String, Presence> mZimlets = new HashMap<String, Presence>();
    
    public void put(String zimletWithPrefix) {
        
        if (zimletWithPrefix.length() == 0)
            return;
        
        char prefix = zimletWithPrefix.charAt(0);
        Presence presence = Presence.fromPrefix(prefix);
        String zimletName;
        
        if (presence != null) {
            if (zimletWithPrefix.length() <= 1)
                return;
            zimletName = zimletWithPrefix.substring(1);
        } else {
            presence = Presence.enabled;
            zimletName = zimletWithPrefix;
        }
        
        put(zimletName, presence);
    }
    
    /*
     * Will remove an entry in the map if:
     * - zimletNameOrWithPrefix matches the current presence
     * - zimletNameOrWithPrefix does not contain prefix
     * 
     * e.g. if current map contains: "zimlet_foo" => mandatory
     *      then either
     *      remove("!zimlet_foo")
     *      or
     *      remove("zimlet_foo")
     *      will remove the entry
     *      
     * Will not remove an entry if zimletNameOrWithPrefix 
     * contains a non-matching prefix.
     *      
     * e.g. if current map contains: "zimlet_foo" => mandatory
     *      then 
     *      remove("+zimlet_foo")
     *      or
     *      remove("-zimlet_foo")  
     *      will *not* remove the entry.  
     */
    public void remove(String zimletNameOrWithPrefix) {
        Presence presence;
        String zimletName;
        
        char prefix = zimletNameOrWithPrefix.charAt(0);
        if (prefix == Presence.mandatory.mPrefix) {
            presence = Presence.mandatory;
            zimletName = zimletNameOrWithPrefix.substring(1);
        } else if (prefix == Presence.enabled.mPrefix) {
            presence = Presence.enabled;
            zimletName = zimletNameOrWithPrefix.substring(1);
        } else if (prefix == Presence.disabled.mPrefix) {
            presence = Presence.disabled;
            zimletName = zimletNameOrWithPrefix.substring(1);
        } else {
            presence = null; // just remove without checking for matching presence
            zimletName = zimletNameOrWithPrefix;
        }
        
        if (presence == null) {
            // just remove without checking for matching presence
            mZimlets.remove(zimletName);
        } else {
            // remove if presence matches
            Presence p = getPresence(zimletName);
            if (p == presence)
                mZimlets.remove(zimletName);
        }
    }
    
    public void put(String zimletName, Presence presence) {
        mZimlets.put(zimletName, presence);
    }
    
    // note: zimletName is just the name, without prefix
    public Presence getPresence(String zimletName) {
        return mZimlets.get(zimletName);
    }
    
    public String[] getZimletNamesAsArray() {
        return mZimlets.keySet().toArray(new String[0]);
    }
    
    public Set<String> getZimletNames() {
        return mZimlets.keySet();
    }
}
