/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client;

public class LmcEmailAddress {

    private String type;
    private String emailID;
    private String personalName;
    private String emailAddress;
    private String displayName;
    private String content;
    private String referencedID;

    public void setType(String t) { type = t; }
    public void setEmailID(String e) { emailID = e; }
    public void setReferencedID(String r) { referencedID = r; }
    public void setPersonalName(String p) { personalName = p; }
    public void setEmailAddress(String e) { emailAddress = e; }
    public void setDisplayName(String d) { displayName = d; }
    public void setContent(String c) { content = c; }

    public String getReferencedID() { return referencedID; }
    public String getType() { return type; }
    public String getEmailID() { return emailID; }
    public String getPersonalName() { return personalName; }
    public String getEmailAddress() { return emailAddress; }
    public String getDisplayName() { return displayName; }
    public String getContent() { return content; }

}