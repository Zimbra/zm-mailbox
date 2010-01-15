/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007 Zimbra, Inc.
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
