/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.extension;

import com.zimbra.common.service.ServiceException;

/**
 * Zimbra extension. An extension to the Zimbra server is packaged as a jar
 * file with its manifest containing the header:
 * <p>
 * <code>
 *   Zimbra-Extension-Class: <i>name of implementation class of this interface</i>
 * </code>
 * <p>
 * The extension is deployed by dropping the jar file into the
 * <i>zimbra_home</i>/lib/ext/<i>ext</i> directory. It is loaded upon server startup.
 */
public interface ZimbraExtension {

    /**
     * Defines a name for the extension. It must be an identifier.
     *
     * @return extension name
     */
    String getName();

    /**
     * Initializes the extension. Called when the extension is loaded.
     *
     * @throws ExtnsionException voluntarily resign from the registration
     * @throws ServiceException error
     */
    void init() throws ExtensionException, ServiceException;

    /**
     * Terminates the extension. Called when the server is shut down or this
     * extension is unregistered.
     */
    void destroy();
}
