/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

/**
 * Interface of the match type :is for the comparator i;ascii-numeric.
 * For other comparator's :is, see {@link org.apache.jsieve.comparators.Equals#equals(String, String)}
 */
public interface Equals2 {
    public boolean equals2(String string1, String string2) throws FeatureException;
}
