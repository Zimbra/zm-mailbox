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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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