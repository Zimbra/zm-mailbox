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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

public interface ZGrant {
    
    /**
     *  some combination of (r)ead, (w)rite, (i)nsert, (d)elete, (a)dminister, workflow action (x)
     */
    public String getRights();
    
    /**
     * the type of grantee: "usr", "grp", "dom" (domain), "cos", 
     * "all" (all authenticated users), "pub" (public authenticated and unauthenticated access), 
     * "guest" (non-Zimbra email address and password)
     */
    public String getGranteeType();

    /***
     * the display name (*not* the zimbra id) of the principal being granted rights;
     * optional if {grantee-type} is "all"
     */
    public String getGranteeName();
    
    /**
     * whether rights granted on this folder are also granted on all subfolders
     */
    public boolean getInherit();
    
    /**
     *  optional argument.  password when {grantee-type} is "guest"
     */
    public String getArgs();

}
