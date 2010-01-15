/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
