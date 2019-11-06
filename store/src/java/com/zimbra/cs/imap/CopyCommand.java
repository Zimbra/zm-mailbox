/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.imap;

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;

public class CopyCommand extends ImapCommand {

    private ImapPath destPath;
    private String sequenceSet;
    private  Set<String> processedList;

    public CopyCommand(String sequenceSet, ImapPath destPath) {
        super();
        this.destPath = destPath;
        this.sequenceSet = sequenceSet;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sequenceSet == null) ? 0 : sequenceSet.hashCode());
        result = prime * result + ((destPath == null) ? 0 : destPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CopyCommand other = (CopyCommand) obj;
        if (sequenceSet == null) {
            if (other.sequenceSet != null) {
                return false;
            }
        } else if (!sequenceSet.equals(other.sequenceSet)) {
            ZimbraLog.imap.info("IMAP::: Seq 1 " + sequenceSet + "\n" + "other: " + other.sequenceSet);
            return false;
        }
        if (destPath == null) {
            if (other.destPath != null) {
                return false;
            }
        } else if (!destPath.equals(other.destPath)) {
            ZimbraLog.imap.info("Dest path " + destPath + " 2: " +other.destPath );
            return false;
        }
        return true;
    }

    public boolean isCopyToTrash() {
        try {
            return ((Folder) this.destPath.getFolder()).getFolderId() == Mailbox.ID_FOLDER_TRASH;
        } catch (ServiceException e) {
            return false;
        }
    }

    public void setProcessedList(Set<String> processedList) {
        this.processedList = processedList;
    }

    @Override
    protected boolean isDuplicate(ImapCommand command) {
        return this.getClass().equals(command.getClass()) && this.hasSameParams(command);
    }

    @Override
    protected boolean throttle(ImapCommand previousCommand) {
        ZimbraLog.imap.info("IMAP::: Checking copy command for throttle. :" + processedList);

        try {
            if (((Folder) this.destPath.getFolder()).getFolderId() == Mailbox.ID_FOLDER_TRASH) {
                CopyCommand c = this;
                if (processedList != null) {
                    ZimbraLog.imap.info("List processed" + processedList.contains(c.sequenceSet));
                    ZimbraLog.imap.info("IMAP:::Copy is for trash so checking in processed list.");
                     return processedList.contains(c.sequenceSet);
                }
                return false;
            } else {
                ZimbraLog.imap.info("IMAP:::Copy is not for trash.");
                return false;
            }
        } catch (ServiceException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CopyCommand [");
        if (destPath != null) {
            builder.append("destPath=");
            builder.append(destPath);
            builder.append(", ");
        }
        if (sequenceSet != null) {
            builder.append("sequenceSet=");
            builder.append(sequenceSet);
        }
        builder.append("]");
        return builder.toString();
    }
}
