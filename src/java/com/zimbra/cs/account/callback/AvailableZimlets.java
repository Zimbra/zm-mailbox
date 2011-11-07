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
package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.zimlet.ZimletPresence;
import com.zimbra.cs.zimlet.ZimletPresence.Presence;

public class AvailableZimlets extends AttributeCallback {
    
    @Override 
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) {
        
        Object replacing = attrsToModify.get(attrName);
        Object deleting = attrsToModify.get("-" + attrName);
        Object adding = attrsToModify.get("+" + attrName);
        
        ZimletPresence availZimlets;
        if (replacing != null)
            availZimlets = doReplacing(replacing);
        else 
            availZimlets = doDeletingAdding(entry, attrName, deleting, adding);
        
        String[] zimlets = availZimlets.getZimletNamesAsArray();
        String[] newValues = new String[zimlets.length];
        for (int i = 0;  i < zimlets.length; i++) {
            String zimletName = zimlets[i];
            Presence presence = availZimlets.getPresence(zimletName);
            newValues[i] = presence.prefix() + zimletName;
        }       
        
        // do the update using our re-shuffled values
        attrsToModify.remove(attrName);
        attrsToModify.remove("-" + attrName);
        attrsToModify.remove("+" + attrName);
        attrsToModify.put(attrName, newValues);
    }
    
    /*
     * 
     */
    private ZimletPresence doReplacing(Object replacing) {
        ZimletPresence availZimlets = new ZimletPresence();
        
        // dedupe conflicts (e.g. both !foo and +foo are provided) by putting values to the ZimletPresence
        // this will fix dups like !foo, +foo
        if (replacing instanceof String) {
            availZimlets.put((String)replacing);
        } else if (replacing instanceof String[]) {
            for (String v : (String[])replacing)
                availZimlets.put(v);
        }
        
        return availZimlets;
    }
    
    /*
     * To remove a zimlet from zimbraZimletAvailableZimlets/zimbraZimletDomainAvailableZimlets, 
     * the zimlet can be identified by either the {zimlet-name} or matching {prefix}{zimlet-name}.
     * This callback will take care of identifying the zimlet with either way.
     * 
     * e.g. if the current values contain: !foo
     *      to remove it, both of the following will work: 
     *          zmprov mc <cos> -zimbraZimletAvailableZimlets foo
     *          zmprov mc <cos> -zimbraZimletAvailableZimlets !foo
     *      
     *      but sending in a not-matching prefix will not work:
     *          zmprov mc <cos> -zimbraZimletAvailableZimlets +foo
     *      it will be a noop and no error/warn wilkl be thrown 
     *      (just like the regular modify behavior)
     *      
     * To add a zimlet to zimbraZimletAvailableZimlets/zimbraZimletDomainAvailableZimlets, 
     * admin can just pass in the desired {prerix}{zimlet-name} without having to remove the 
     * current {a-diff-prerix}{zimlet-name} if it exists.  This callback will take care of cleaning 
     * up the current value.
     *  
     * e.g. if the current values contain: +bar
     *      to change it to -bar, just send:
     *          zmprov mc <cos> +zimbraZimletAvailableZimlets -bar
     *      (or, of course you can send:
     *          zmprov mc <cos> -zimbraZimletAvailableZimlets +bar +zimbraZimletAvailableZimlets -bar
     *       but that is not necessary)       
     */
    private ZimletPresence doDeletingAdding(Entry entry, String attrName, Object deleting, Object adding) {
        ZimletPresence availZimlets = new ZimletPresence();
        
        // get current values, parse it into our map
        if (entry != null) {
            String[] curValues = entry.getMultiAttr(attrName, false);
            for (String zimletWithPrefix : curValues) {
                availZimlets.put(zimletWithPrefix);
            }
        }
        
        // remove what admin wants to remove
        // ZimletPresence.remove will take care of identifying the correct zimlet 
        if (deleting != null) {
            if (deleting instanceof String)
                availZimlets.remove((String)deleting);
            else {
                for (String v : (String[])deleting)
                    availZimlets.remove(v);
            }
        }
        
        // add what admin wants to add
        // ZimletPresence.put will take care of cleaning up existing same zimlet with diff prefix
        if (adding != null) {
            if (adding instanceof String)
                availZimlets.put((String)adding);
            else if (adding instanceof String[]) {
                for (String v : (String[])adding)
                    availZimlets.put(v);
            }
        }

        return availZimlets;
    }
    
    @Override 
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }

}
