/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.filter;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

/**
 * Interface implemented by classes that handle filter rule actions. 
 */
public interface FilterHandler {

    public ParsedMessage getParsedMessage();
    public MimeMessage getMimeMessage();
    
    /**
     * Discards the message.
     */
    public void discard();
    
    /**
     * Files the message into either the default folder or the
     * spam folder.
     * @return the new message, or <tt>null</tt> if it was a duplicate
     */
    public Message implicitKeep(int flagBitmask, String tags) throws ServiceException;
    
    /**
     * Files the message into the default folder without taking
     * spam filtering into account. 
     * @return the new message, or <tt>null</tt> if it was a duplicate
     */
    public Message explicitKeep(int flagBitmask, String tags) throws ServiceException;
    
    /**
     * Files the message into the given folder.  May be local or remote.
     * @return the new message, or <tt>null</tt> if it was a duplicate
     */
    public ItemId fileInto(String folderPath, int flagBitmask, String tags) throws ServiceException;
    
    /**
     * Redirects the message to another address.
     */
    public void redirect(String destinationAddress) throws ServiceException, MessagingException;

    /**
     * Returns the path to the default folder (usually <tt>Inbox</tt>).
     */
    public String getDefaultFolderPath() throws ServiceException;
}
