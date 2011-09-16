/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 VMware, Inc.
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
package com.zimbra.cs.account;

// TODO: move all cache data keys here.
//       currently they are scattered everywhere
//       also, change to signature of set/getCacheDat to enum and remove the getKeyName call


public enum EntryCacheDataKey {
    
    /*
     * all
     */
    PERMISSION,
    
    
    /*
     * account
     */
    ACCOUNT_COS,
    ACCOUNT_IS_GAL_SYNC_ACCOUNT,
    ACCOUNT_EMAIL_FIELDS,
    ACCOUNT_VALIDITY_VALUE_HIGHEST_RELOAD,
    
    /*
     * MailTarget
     */
    MAILTARGET_DOMAIN_ID,
    
    /*
     * grouped entries(entries that can be in groups): account, cr, dl
     */
    GROUPEDENTRY_DIRECT_GROUPIDS,
    
    /*
     * domain
     */
    DOMAIN_FOREIGN_NAME_HANDLERS,
    DOMAIN_GROUP_CACHE_FULL_HAD_BEEN_WARNED;
    
    // all access of the key name must be through this, 
    // not calling name() or toString() directly
    // TODO: add a signature in Entry to take a EntryCacheDataKey for get/set
    public String getKeyName() {
        return name();
    }

}
