/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.formatter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.UserServletContext;

/**
 * Interface for classes which need to known when a formatter is running
 * For example if a background process needs to suspend while a particular formatting routine occurs
 */
public interface FormatListener {
    /**
     * Called when the format callback begins
     */
    public void formatCallbackStarted(UserServletContext context) throws ServiceException;

    /**
     * Called when the format callback completes
     */
    public void formatCallbackEnded(UserServletContext context) throws ServiceException;

    /**
     * Called when the save callback begins
     */
    public void saveCallbackStarted(UserServletContext context) throws ServiceException;
    
    /**
     * Called when the save callback completes
     */
    public void saveCallbackEnded(UserServletContext context) throws ServiceException;
}
