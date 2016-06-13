/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.stats;

import java.util.List;

/**
 * Defines an interface to an object that keeps track of
 * one or more statistics.
 */
public interface Accumulator {

    /**
     * Returns stat names.  The size of the <code>List</code> must match the size of the
     * <code>List</code> returned by {@link #getData()}.
     */
    public List<String> getNames();
    
    /**
     * Returns stat values.  The size of the <code>List</code> must match the size of the
     * <code>List</code> returned by {@link #getNames()}.
     */
    public List<Object> getData();
    
    /**
     * Resets the values tracked by this <code>Accumulator</code>.
     */
    public void reset();
}
