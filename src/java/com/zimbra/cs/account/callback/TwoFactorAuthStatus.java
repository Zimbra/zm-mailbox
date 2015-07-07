package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class TwoFactorAuthStatus extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry) throws ServiceException {
        boolean setting = ((String) attrValue).equalsIgnoreCase("true");
        if (entry instanceof Account) {
            Account account = (Account) entry;
            if (attrName.equals(Provisioning.A_zimbraFeatureTwoFactorAuthRequired) && !account.isFeatureTwoFactorAuthAvailable()
                    && setting) {
                throw ServiceException.FAILURE("cannot make two-factor auth required because is not available on this account", null);
            } else if (attrName.equals(Provisioning.A_zimbraFeatureTwoFactorAuthAvailable) && account.isFeatureTwoFactorAuthRequired()
                    && !setting) {
                throw ServiceException.FAILURE("cannot make two-factor auth unavailable because it is currently required on the account", null);
            } else if (attrName.equals(Provisioning.A_zimbraFeatureTwoFactorAuthAvailable) && account.isTwoFactorAuthEnabled()
                    && !setting) {
                throw ServiceException.FAILURE("cannot make two-factor auth unavailable because it is currently enabled on the account", null);
            } else if (attrName.equals(Provisioning.A_zimbraTwoFactorAuthEnabled) && !account.isFeatureTwoFactorAuthAvailable() &&
                    setting) {
                throw ServiceException.FAILURE("cannot enable two-factor auth because it is not available on this account", null);
            }
        } else if (entry instanceof Cos) {
            Cos cos = (Cos) entry;
            if (attrName.equals(Provisioning.A_zimbraFeatureTwoFactorAuthRequired) && !cos.isFeatureTwoFactorAuthAvailable()
                    && setting) {
                throw ServiceException.FAILURE("cannot make two-factor auth required because it is not available on this COS", null);
            } else if (attrName.equals(Provisioning.A_zimbraFeatureTwoFactorAuthAvailable) && cos.isFeatureTwoFactorAuthRequired()
                    && !setting) {
                throw ServiceException.FAILURE("cannot make two-factor auth unavailable because it is currently required on the COS", null);
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {}

}
