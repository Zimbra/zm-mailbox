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
package com.zimbra.cs.service.util;

import java.util.Comparator;

import com.zimbra.soap.account.message.HABGroup;

/**
 * @author zimbra
 *
 */
public class SortBySeniorityIndexThenName implements Comparator<HABGroup> {

    @Override
    public int compare(HABGroup a, HABGroup b) {
        if (a == null || b == null) {
            return 0;
        }

        int s1 = a.getSeniorityIndex();
        int s2 = b.getSeniorityIndex();

        if (s1 == 0 && s2 == 0) {
            String name1 = a.getName();
            String name2 = b.getName();

            if (name1 == null && name2== null) {
                return 0;
            } else if (name1 != null && name2 == null) {
                return -1;
            } else if (name1 == null && name2 != null) {
                return 1;
            } else {
                return name1.compareTo(name2);
            }

        } else {
            return new Integer(s2).compareTo(new Integer(s1));
        }

    }
}
