/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;

public enum RightClass {
    ALL,
    ADMIN,
    USER;
    
    public static RightClass fromString(String s) throws ServiceException {
        try {
            return RightClass.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("unknown right class: " + s, e);
        }
    }
    
    public static String allValuesInString(String delimiter) {
        StringBuffer sb = new StringBuffer();
        
        for (RightClass value : RightClass.values()) {
            if (sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(value);
        }
        return sb.toString();
    }
    
}
