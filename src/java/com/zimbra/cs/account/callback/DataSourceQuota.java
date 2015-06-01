package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.util.AccountUtil;

public class DataSourceQuota extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry) throws ServiceException {
        // if changing data source quota, make sure it's not higher than total data source quota
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        if (mod.unsetting()) {
            return;
        }
        long newQuota = Long.valueOf(mod.value());
        if (entry instanceof Account) {
            Account account = (Account) entry;
            long accountQuota = AccountUtil.getEffectiveQuota(account);
            if (accountQuota > 0 && newQuota > accountQuota) {
                throw ServiceException.FAILURE("data source quota cannot exceed account quota", null);
            }
            if (attrName.equals(ZAttrProvisioning.A_zimbraDataSourceQuota)) {
                long totalQuota = account.getDataSourceTotalQuota();
                if (newQuota > 0 && totalQuota > 0 && newQuota > totalQuota) {
                    throw ServiceException.FAILURE("cannot set data source quota to be higher than total data source quota", null);
                }
            } else if (attrName.equals(ZAttrProvisioning.A_zimbraDataSourceTotalQuota)) {
                long dataSourceQuota = account.getDataSourceQuota();
                if (newQuota > 0 && dataSourceQuota > 0 && newQuota < dataSourceQuota) {
                    throw ServiceException.FAILURE("cannot set data source total quota to be lower than data source quota", null);
                }
            }
        } else if (entry instanceof Cos) {
            Cos cos = (Cos) entry;
            if (attrName.equals(ZAttrProvisioning.A_zimbraDataSourceQuota)) {
                long totalQuota = cos.getDataSourceTotalQuota();
                if (newQuota > 0 && totalQuota > 0 && newQuota > totalQuota) {
                    throw ServiceException.FAILURE("cannot set data source quota to be higher than total data source quota", null);
                }
            } else if (attrName.equals(ZAttrProvisioning.A_zimbraDataSourceTotalQuota)) {
                long dataSourceQuota = cos.getDataSourceQuota();
                if (newQuota > 0 && dataSourceQuota > 0 && newQuota < dataSourceQuota) {
                    throw ServiceException.FAILURE("cannot set data source total quota to be lower than data source quota", null);
                }
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {}
}
