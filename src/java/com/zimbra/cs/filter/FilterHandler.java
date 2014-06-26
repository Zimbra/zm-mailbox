/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.filter;

import java.util.Collection;
import java.util.List;

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
interface FilterHandler {

    Message getMessage() throws ServiceException;

    ParsedMessage getParsedMessage() throws ServiceException;

    MimeMessage getMimeMessage() throws ServiceException;

    int getMessageSize();

    /**
     * Returns the path to the default folder (usually <tt>Inbox</tt>).
     */
    String getDefaultFolderPath() throws ServiceException;

    /**
     * Executed before mail filtering begins.
     */
    void beforeFiltering() throws ServiceException;

    /**
     * Discards the message.  This method will only be called when there
     * are no <tt>fileinto</tt> or <tt>keep</tt> actions.
     */
    void discard() throws ServiceException;

    /**
     * Files the message into either the default folder or the
     * spam folder.  This method will not be called multiple times
     * for the same message.
     * @return the new message, or <tt>null</tt> if it was a duplicate
     */
    Message implicitKeep(Collection<ActionFlag> flagActions, String[] tags) throws ServiceException;

    /**
     * Files the message into the default folder without taking
     * spam filtering into account.  This method will not be called multiple times
     * for the same message.
     * @return the new message, or <tt>null</tt> if it was a duplicate
     */
    Message explicitKeep(Collection<ActionFlag> flagActions, String[] tags) throws ServiceException;

    /**
     * Files the message into the given folder.  May be local or remote.
     * This method will not be called multiple times for the same path.
     * @return the new message, or <tt>null</tt> if it was a duplicate
     */
    ItemId fileInto(String folderPath, Collection<ActionFlag> flagActions, String[] tags) throws ServiceException;

    /**
     * Redirects the message to another address.
     */
    void redirect(String destinationAddress) throws ServiceException;

    /**
     * Replies to the message.
     */
    void reply(String bodyTemplate) throws ServiceException, MessagingException;

    /**
     * Executed after mail filtering ends.
     */
    void afterFiltering() throws ServiceException;

    /**
     * Sends an email notification.
     */
    abstract void notify(String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes,
            List<String> origHeaders) throws ServiceException, MessagingException;
}
