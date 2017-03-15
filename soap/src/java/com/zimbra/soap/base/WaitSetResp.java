/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Zimbra, Inc.
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

package com.zimbra.soap.base;

import java.util.List;

import com.zimbra.soap.type.AccountIdAndFolderIds;
import com.zimbra.soap.type.IdAndType;

public interface WaitSetResp {
    public void setCanceled(Boolean canceled);
    public void setSeqNo(String seqNo);
    public void setSignalledAccounts(Iterable <AccountIdAndFolderIds> signalledAccounts);
    public WaitSetResp addSignalledAccount(AccountIdAndFolderIds signalledAccount);
    public void setErrors(Iterable <IdAndType> errors);
    public WaitSetResp addError(IdAndType error);
    public String getWaitSetId();
    public Boolean getCanceled();
    public String getSeqNo();
    public List<AccountIdAndFolderIds> getSignalledAccounts();
    public List<IdAndType> getErrors();
    public void setWaitSetId(String waitSetId);
}
