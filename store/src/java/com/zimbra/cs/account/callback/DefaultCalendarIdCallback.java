/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.acl.FolderACL;

public class DefaultCalendarIdCallback extends AttributeCallback {

   @SuppressWarnings("rawtypes")
   @Override
   public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry)
           throws ServiceException {
       // validate new value 1st
       if (attrValue == null) {
           throw ServiceException.INVALID_REQUEST("Invalid value received for " + attrName, null);
       }
       Integer value = 0;
       try {
           if (attrValue instanceof String[]) {
               String[] arr = (String[]) attrValue;
               if (arr.length < 1) {
                   throw ServiceException.INVALID_REQUEST("Invalid value received for " + attrName, null);
               }
               String temp = arr[0];
               if (StringUtil.isNullOrEmpty(temp)) {
                   throw ServiceException.INVALID_REQUEST("Invalid value received for " + attrName, null);
               }
               value = Integer.valueOf(temp);
           } else if (attrValue instanceof String) {
               String temp = (String) attrValue;
               if (StringUtil.isNullOrEmpty(temp)) {
                   throw ServiceException.INVALID_REQUEST("Invalid value received for " + attrName, null);
               }
               value = Integer.valueOf(temp);
           } else {
               throw ServiceException.INVALID_REQUEST("Invalid value received for " + attrName, null);
           }
       } catch (NumberFormatException nfe) {
           throw ServiceException.INVALID_REQUEST("Value for " + attrName + " must be valid integer", null);
       }
       if (value  == 0) {
           throw ServiceException.INVALID_REQUEST("Invalid value received for " + attrName, null);
       }

       if (entry instanceof Account) {
           Account account = (Account) entry;
           Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
           OperationContext octxt = new OperationContext(mbox);
           Folder folder = mbox.getFolderById(octxt, value);
           // validate if the folder exist or not
           if (folder == null) {
               throw ServiceException.NOT_FOUND("Folder not found for id " + value + ". Please provide valid folder id.", null);
           }
           // check if folder is calendar or not
           if (folder.getDefaultView() != MailItem.Type.APPOINTMENT) {
               throw ServiceException.INVALID_REQUEST("Folder must be a calendar folder.", null);
           }
           // check for permissions if it's shared calendar
           if (folder.getType() == MailItem.Type.MOUNTPOINT) {
              Mountpoint mp = mbox.getMountpointById(octxt, folder.getId());
              String ownerId = mp.getOwnerId();
              int ownerItemId = mp.getRemoteId();
              FolderACL facl = new FolderACL(octxt, ownerId, ownerItemId);
              boolean writeAccess = facl.canAccess(ACL.RIGHT_WRITE);
              if (!writeAccess) {
                  throw ServiceException.PERM_DENIED(account.getMail() + " do not have enough permissions on " + folder.getName() + " to set default.");
              }
           }
       } else if (entry instanceof Cos) {
           throw ServiceException.INVALID_REQUEST("Changing value for " + attrName + " on COS is not allowed.", null);
       } else if (entry != null) {
           throw ServiceException.INVALID_REQUEST("Invalid entry received.", null);
       }
   }

   @Override
   public void postModify(CallbackContext context, String attrName, Entry entry) {
       // do nothing
   }
}
