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
package com.zimbra.cs.filter;

import org.apache.jsieve.comparators.Comparator;

import com.zimbra.cs.filter.jsieve.Counts;
import com.zimbra.cs.filter.jsieve.Values;
import com.zimbra.cs.filter.jsieve.Equals2;

/**
 * Class ZimbraComparator enhances the jsieve's Comparator to support
 * the RFC 5231: Relational Extension
 */
public interface ZimbraComparator extends Comparator, Values, Counts, Equals2 {

}
