/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

package com.zimbra.common.gql;

public class GqlConstants {
    // modify search folder spec constants
    public static final String MODIFY_SEARCH_FOLDER_SPEC = "ModifySearchFolderSpec";
    public static final String SEARCH_FOLDER_ID = "searchFolderId";
    public static final String QUERY = "query";
    public static final String SEARCH_TYPES = "searchTypes";
    public static final String SORT_BY = "sortBy";

    // new search folder spec constants
    public static final String NEW_SEARCH_FOLDER_SPEC = "NewSearchFolderSpec";
    public static final String NAME = "name";
    public static final String FLAGS = "flags";
    public static final String COLOR = "color";
    public static final String PARENT_FOLDER_ID = "parentFolderId";
    public static final String RGB = "rgb";

    // search folder constants
    public static final String SEARCH_FOLDER = "SearchFolder";

    // account info constants
    public static final String ACCOUNT_INFO = "AccountInfo";
    public static final String ATTRS = "attrs";
    public static final String SOAP_URL = "soapURL";
    public static final String PUBLIC_URL = "publicURL";
    public static final String CHANGE_PASSWORD_URL = "changePasswordURL";
    public static final String COMMUNITY_URL = "communityURL";
    public static final String ADMIN_URL = "adminURL";
    public static final String BOSH_URL = "boshURL";

    // named value constants
    public static final String NAMED_VALUE = "NamedValue";
    public static final String VALUE = "value";

    // end session constants
    public static final String CLEAR_COOKIES= "clearCookies";
}
