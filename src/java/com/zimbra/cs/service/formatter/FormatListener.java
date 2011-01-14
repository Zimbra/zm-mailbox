/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
