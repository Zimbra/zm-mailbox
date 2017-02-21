/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.List;

import com.zimbra.cs.account.accesscontrol.AdminRight;

public interface AdminRightCheckPoint {

    /**
     * canned notes
     */
    static class Notes {
        // only system admins are allowed
        public static final String SYSTEM_ADMINS_ONLY = "Only system admins are allowed.";

        // no right is needed, any admin can do it.
        public static final String ALLOW_ALL_ADMINS = "Do not need any right, all admins are allowed.";

        // in the end no one should refer to this string
        public static final String TODO = "TDB";

        public static final String GET_ENTRY_1 = "Attributes that are not allowed to be get by the authenticated admin will be returned "
                + "as <a n=\"{attr-name}\" pd=\"1\"/>.";

        public static final String GET_ENTRY = GET_ENTRY_1
                + "To allow an admin to get all attributes, grant the %s right";

        public static final String MODIFY_ENTRY = "All attrs provided in the attribute list have to be settable by the authed admin. "
                + "You can grant the %s right, which allows "
                + "setting all attributes on %s, or grant the set attrs right just "
                + "for the attributes the admin needs to set while creating an entry.";

        public static final String LIST_ENTRY = "If the authenticated admin does not have the corresponding list{Entry} right "
                + "for an entry, the entry is skipped in the getAllXXX/searchXXX/searchDirectoryResponse,  "
                + "no PERM_DENIED exception will be thrown. " + GET_ENTRY_1;

        public static final String ADMIN_LOGIN_AS = "If the target is an account, need the adminLoginAs right; "
                + "If the target is a calendar resource, need the R_adminLoginCalendarResourceAs right.";

    }

    public void docRights(List<AdminRight> relatedRights, List<String> notes);
}
