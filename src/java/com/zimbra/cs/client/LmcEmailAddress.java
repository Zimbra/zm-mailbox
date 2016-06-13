/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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