/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;

/**
 * @since Jun 13, 2004
 */
public interface SharedDeliveryCoordinator {

    /**
     * Puts mailbox in shared delivery mode.  A shared delivery is delivery of
     * a message to multiple recipients.  Conflicting op on mailbox is disallowed
     * while mailbox is in shared delivery mode.  (See bug 2187)
     * Conversely, a shared delivery may not start on a mailbox that is
     * currently being operated on or when there is a pending op request.
     * For example, thread A puts mailbox in shared delivery mode.  Thread B
     * then tries to backup the mailbox.  Backup cannot start until thread A is
     * done, but mailbox is immediately put into backup-pending mode.
     * Thread C then tries to do another shared delivery on the mailbox, but
     * is not allowed to do so because of thread B's pending backup request.
     * A thread that calls this method must call endSharedDelivery() after
     * delivering the message.
     * @return true if shared delivery may begin; false if shared delivery may
     *         not begin because of a pending backup request
     */
    public boolean beginSharedDelivery(Mailbox mbox) throws ServiceException;

    /**
     * @see #beginSharedDelivery()
     */
    public void endSharedDelivery(Mailbox mbox) throws ServiceException;

    /** Returns whether shared delivery is allowed. Defaults to true if it was never explicitly set. */
    public boolean isSharedDeliveryAllowed(Mailbox mbox) throws ServiceException;

    /**
     * Turns shared delivery on/off.  If turning off, waits until the op can begin,
     * i.e. until all currently ongoing shared deliveries finish.  A thread
     * turning shared delivery off must turn it on at the end of the operation, otherwise
     * no further shared deliveries are possible to the mailbox.
     */
    public void setSharedDeliveryAllowed(Mailbox mbox, boolean allow) throws ServiceException;

    /**
     * Wait until shared delivery is completed on this mailbox.  Other conflicting ops may begin when
     * there is no shared delivery in progress.  Call setSharedDeliveryAllowed(false)
     * before calling this method.
     *
     */
    public void waitUntilSharedDeliveryCompletes(Mailbox mbox) throws ServiceException;

    /**
     * Tests whether shared delivery is completed on this mailbox.  Other conflicting ops may begin when
     * there is no shared delivery in progress.
     */
    public boolean isSharedDeliveryComplete(Mailbox mbox) throws ServiceException;
}
