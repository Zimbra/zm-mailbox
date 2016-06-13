/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.accesscontrol;

public class GranteeFlag {
    
    // allowed for admin rights
    public static final short F_ADMIN = 0x0001;
    
    public static final short F_INDIVIDUAL = 0x0002;
    public static final short F_GROUP      = 0x0004;
    public static final short F_DOMAIN     = 0x0008;
    public static final short F_AUTHUSER   = 0x0010;
    public static final short F_PUBLIC     = 0x0020;
    
    public static final short F_IS_ZIMBRA_ENTRY = 0x0040;
    
    public static final short F_HAS_SECRET = 0x0080;
}
