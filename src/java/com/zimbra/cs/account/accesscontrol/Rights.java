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
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.cs.account.accesscontrol.generated.AdminRights;
import com.zimbra.cs.account.accesscontrol.generated.UserRights;

/**
 * 
 * bridging class so we don't have to include the "generated" classes 
 * at callsites, because that's kind of ugly.
 *
 */
public abstract class Rights {

    public static class Admin extends AdminRights {
    }
    
    public static class User extends UserRights {
    }
}
