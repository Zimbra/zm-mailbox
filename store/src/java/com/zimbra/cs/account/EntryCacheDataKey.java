/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
    ACCOUNT_DLS,
    ACCOUNT_DIRECT_DLS,
    ACCOUNT_DYNAMIC_GROUPS,
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
    GROUPEDENTRY_MEMBERSHIP,                 /* excludes entries by custom URL */
    GROUPEDENTRY_MEMBERSHIP_ADMINS_ONLY,     /* excludes entries by custom URL */
    GROUPEDENTRY_EXTERNAL_GROUP_DNS,

    /*
     * domain
     */
    DOMAIN_FOREIGN_NAME_HANDLERS,
    DOMAIN_GROUP_CACHE_FULL_HAD_BEEN_WARNED,
    DOMAIN_AUTO_PROVISION_DATA,
    DOMAIN_DEFAULT_COS,

    /*
     * server
     */
    SERVER_ALWAYSONCLUSTER,

    /*
     * group
     */
    GROUP_MEMBERS;

    // all access of the key name must be through this,
    // not calling name() or toString() directly
    // TODO: add a signature in Entry to take a EntryCacheDataKey for get/set
    public String getKeyName() {
        return name();
    }

}
