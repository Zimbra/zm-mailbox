/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Collection;

public interface StatsDumperDataSource {

    /**
     * Returns the name of the file that stats are written to.  This is a
     * simple filename with no directory path.  All stats files are currently
     * written to /opt/zimbra/zmstat.
     */
    public String getFilename();
    
    /**
     * Returns the first line logged in a new stats file, or <tt>null</tt>
     * if there is no header.
     */
    public String getHeader();
    
    /**
     * Returns the latest set of data lines and resets counters.
     */
    public Collection<String> getDataLines();
    
    /**
     * Specifies whether a <tt>timestamp</tt> column is prepended to the data
     * returned by <tt>getHeader()</tt> and <tt>getDataLines()</tt>.
     */
    public boolean hasTimestampColumn();
}
