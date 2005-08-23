/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 8, 2004
 */
package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.mail.Action;


/**
 * @author kchen
 */
public class ActionFlag implements Action {
    private boolean set;
    private int flagId;
    private String name;
    
    /**
     * Constructs a flag action.
     * 
     * @param flag the flag
     */
    public ActionFlag(int flagId, boolean set, String name) {
        setFlag(flagId, set, name);
    }
    
    /**
     * @return Returns the flag.
     */
    public int getFlagId() {
        return flagId;
    }
    
    public boolean isSetFlag() {
        return set;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * @param flag The flag to set.
     */
    public void setFlag(int flagId, boolean set, String name) {
        this.flagId = flagId;
        this.set = set;
        this.name = name;
    }
    
    public String toString() {
        return "ActionFlag, " + (set ? "set" : "reset") + " flag " + name;
    }
}
