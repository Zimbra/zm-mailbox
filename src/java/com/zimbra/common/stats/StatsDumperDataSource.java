/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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
     * Returns a set of data lines.
     */
    public Collection<String> getDataLines();
    
    /**
     * Specifies whether a <tt>timestamp</tt> column is prepended to the data
     * returned by <tt>getHeader()</tt> and <tt>getDataLines()</tt>.
     */
    public boolean hasTimestampColumn();
}
