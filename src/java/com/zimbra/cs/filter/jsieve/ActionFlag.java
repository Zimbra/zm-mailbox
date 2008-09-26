/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Nov 8, 2004
 */
package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.mail.Action;

public class ActionFlag implements Action {
    private boolean set;
    private int flagId;
    private String name;
    
    public ActionFlag(int flagId, boolean set, String name) {
        setFlag(flagId, set, name);
    }
    
    public int getFlagId() {
        return flagId;
    }
    
    public boolean isSetFlag() {
        return set;
    }
    
    public String getName() {
        return name;
    }
    
    public void setFlag(int flagId, boolean set, String name) {
        this.flagId = flagId;
        this.set = set;
        this.name = name;
    }
    
    public String toString() {
        return "ActionFlag, " + (set ? "set" : "reset") + " flag " + name;
    }
}
