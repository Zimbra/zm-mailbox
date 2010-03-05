/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.filter;

import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

/**
 * Interface implemented by classes that handle filter rule actions. 
 */
public abstract class FilterHandler {

    public abstract ParsedMessage getParsedMessage() throws ServiceException;
    public abstract MimeMessage getMimeMessage() throws ServiceException;
    
    /**
     * Returns the path to the default folder (usually <tt>Inbox</tt>).
     */
    public abstract String getDefaultFolderPath() throws ServiceException;

    /**
     * Executed before mail filtering begins.
     */
    @SuppressWarnings("unused")
    public void beforeFiltering() throws ServiceException { }
    
    /**
     * Discards the message.  This method will only be called when there
     * are no <tt>fileinto</tt> or <tt>keep</tt> actions.
     */
    @SuppressWarnings("unused")
    public void discard() throws ServiceException { }
    
    /**
     * Files the message into either the default folder or the
     * spam folder.  This method will not be called multiple times
     * for the same message.
     * @return the new message, or <tt>null</tt> if it was a duplicate
     */
    @SuppressWarnings("unused")
    public Message implicitKeep(Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        return null;
    }
    
    /**
     * Files the message into the default folder without taking
     * spam filtering into account.  This method will not be called multiple times
     * for the same message. 
     * @return the new message, or <tt>null</tt> if it was a duplicate
     */
    @SuppressWarnings("unused")
    public Message explicitKeep(Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        return null;
    }
    
    /**
     * Files the message into the given folder.  May be local or remote.
     * This method will not be called multiple times for the same path.
     * @return the new message, or <tt>null</tt> if it was a duplicate
     */
    @SuppressWarnings("unused")
    public ItemId fileInto(String folderPath, Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        return null;
    }
    
    /**
     * Redirects the message to another address.
     */
    @SuppressWarnings("unused")
    public void redirect(String destinationAddress) throws ServiceException, MessagingException { }

    /**
     * Executed after mail filtering ends.
     */
    @SuppressWarnings("unused")
    public void afterFiltering() throws ServiceException { }
}
