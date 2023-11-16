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
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.JWEUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.type.Channel;

public abstract class ChannelProvider {
    private static Map<String, ChannelProvider> providers = new HashMap<String, ChannelProvider>();

    static {
        try {
            registerChannelProvider(Channel.EMAIL.toString(), new EmailChannel());
        } catch (ServiceException e) {
            ZimbraLog.passwordreset.warn("Channel registration failed for %s", Channel.EMAIL.toString());
        }
    }

    public static void registerChannelProvider(String channel, ChannelProvider provider) throws ServiceException {
        if (StringUtil.isNullOrEmpty(channel) || provider == null) {
            ZimbraLog.passwordreset.error("Channel or channel provider is invalid");
            throw ServiceException.FAILURE("Channel and channel provider must be provided", null);
        }
        if (providers.get(channel) != null) {
            ZimbraLog.passwordreset.warn("Channel provider %s already registered for %s, so not adding new provider",
                    providers.get(channel).getClass().getName(), channel);
        } else {
            providers.put(channel, provider);
            ZimbraLog.passwordreset.info("Channel provider %s registered for %s", providers.get(channel).getClass().getName(),
                    channel);
        }
    }

    public static ChannelProvider getProviderForChannel(String channel) {
        if (StringUtil.isNullOrEmpty(channel)) {
            return null;
        }
        return providers.get(channel);
    }

    public static ChannelProvider getProviderForChannel(Channel channel) {
        if (channel == null) {
            return null;
        }
        return providers.get(channel.toString());
    }

    // RecoverAccount API methods
    public Map<String, String> getResetPasswordRecoveryCodeMap(Account account) throws ServiceException {
        String encoded = account.getResetPasswordRecoveryCode();
        return JWEUtil.getDecodedJWE(encoded);
    }
    public abstract String getRecoveryAccount(Account account) throws ServiceException;
    public abstract void sendAndStoreResetPasswordRecoveryCode(ZimbraSoapContext zsc, Account account,
            Map<String, String> recoveryCodeMap) throws ServiceException;

    // SetRecoveryAccount API methods
    public Map<String, String> getSetRecoveryAccountCodeMap(Account account) throws ServiceException {
        Map<String, String> recoveryCodeMap = null;
        String encoded = account.getRecoveryAccountVerificationData();
        recoveryCodeMap = JWEUtil.getDecodedJWE(encoded);
        return recoveryCodeMap;
    }
    public abstract void validateSetRecoveryAccountCode(String recoveryAccountVerificationCode, Account account,
            Mailbox mbox, ZimbraSoapContext zsc) throws ServiceException;
    public abstract void sendAndStoreSetRecoveryAccountCode(Account account, Mailbox mbox,
            Map<String, String> recoveryCodeMap, ZimbraSoapContext zsc, OperationContext octxt,
            HashMap<String, Object> prefs) throws ServiceException;
    public abstract void sendAndStoreTwoFactorAuthAccountCode(Account account, Mailbox mbox,
            Map<String, String> recoveryCodeMap, ZimbraSoapContext zsc, OperationContext octxt,
            HashMap<String, Object> prefs) throws ServiceException;
}
