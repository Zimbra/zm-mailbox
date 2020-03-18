/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.store.external;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.MailboxBlob;

public class MockValidationFailingStore extends SimpleStoreManager {

    private boolean failOnValidate = false;

    @Override
    public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId,
            long revision, String locator, boolean validate)
            throws ServiceException {
        if (!validate || !failOnValidate) {
            return super.getMailboxBlob(mbox, itemId, revision, locator, false);
        } else {
            return null;
        }
    }

    public void setFailOnValidate(boolean failOnValidate) {
        this.failOnValidate = failOnValidate;
    }
}
