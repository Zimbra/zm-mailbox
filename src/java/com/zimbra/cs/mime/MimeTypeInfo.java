/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Apr 14, 2005
 *
 */
package com.zimbra.cs.mime;

import java.util.Set;

public interface MimeTypeInfo {
    
    /**
     * Returns the associated MIME types.  The MIME type can be a regular expression.
     */
    public String[] getMimeTypes();
    
    /**
     * Gets the name of the extension where the handler class is defined.
     * If it is part of the core, return null.
     * @return
     */
    public String getExtension();
    
    /**
     * Gets the name of the handler class. If no package is specified, 
     * com.zimbra.cs.mime.handler is assumed. 
     * @return
     */
    public String getHandlerClass();
    
    /**
     * Whether the content is to be indexed for this mime type.
     * @return
     */
    public boolean isIndexingEnabled();
    
    /**
     * Gets the description of the mime type
     * @return
     */
    public String getDescription();
    
    /**
     * Returns the <tt>Set</tt> of file extensions.  Extensions are returned
     * in lower case.
     */
    public Set<String> getFileExtensions();

    /**
     * Gets the priority.  In the case where multiple <tt>MimeTypeInfo</tt>s
     * match a search, the one with the highest priority wins.
     */
    public int getPriority();
}
