/*
 * ***** BEGIN LICENSE BLOCK ***** Zimbra Collaboration Suite Server Copyright
 * (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>. *****
 * END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.HashMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.RecoverAccountRequest;
import com.zimbra.soap.mail.message.RecoverAccountResponse;
import com.zimbra.soap.mail.message.SetRecoveryEmailRequest;
import com.zimbra.soap.mail.message.SetRecoveryEmailResponse;

public interface SendRecoveryCode {
    public RecoverAccountResponse handleRecoverAccountRequest(RecoverAccountRequest request, Account account) throws ServiceException;
    public void sendResetPasswordRecoveryCode() throws ServiceException;
    public void saveResetPasswordRecoveryCode(AuthToken authToken, HashMap<String, Object> prefs) throws ServiceException;
    public SetRecoveryEmailResponse handleSetRecoveryEmailRequest(SetRecoveryEmailRequest request, ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException;
    public void sendSetRecoveryAccountValidationCode(OperationContext octxt) throws ServiceException;
    public void validateSetRecoveryEmailCode(String recoveryAccountVerificationCode, ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException;
    public void saveSetRecoveryAccountValidationCode(AuthToken authToken, HashMap<String, Object> prefs) throws ServiceException;
}
