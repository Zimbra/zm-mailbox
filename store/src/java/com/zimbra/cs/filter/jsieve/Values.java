/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.exception.FeatureException;

public interface Values {
    /**
     * Method values answers a <code>boolean</code> indicating if parameter
     * <code>string1</code> is gt/ge/lt/le/eq/ne to parameter <code>string2</code> using
     * the comparison rules defind by the implementation.
     *
     * @param operator
     * @param lhs left hand side
     * @param rhs right hand side
     * @return boolean
     */
    public boolean values(String operator, String lhs, String rhs) throws FeatureException;
}
