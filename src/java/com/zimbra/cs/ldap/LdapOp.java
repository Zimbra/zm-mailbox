/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
/**
 *
 */
package com.zimbra.cs.ldap;

public enum LdapOp {
    GET_CONN("get a connection from a connection pool"),
    REL_CONN("release a connection back to the connection pool"),
    OPEN_CONN("open a new connection"),
    CLOSE_CONN("close a connection"),
    CREATE_ENTRY("create an entry"),
    DELETE_ENTRY("delete entry"),
    GET_ENTRY("get entry"),
    GET_SCHEMA("get schema"),
    MODIFY_DN("modify DN"),
    MODIFY_ATTRS("modify attributes"),
    SEARCH("search"),
    TEST_AND_MODIFY_ATTRS("test and modify attributes"),
    SET_PASSWORD("set password"),
    COMPARE("compare");

    private String desc;

    private LdapOp(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}