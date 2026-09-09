package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import java.util.Map;

public class MobileNotificationPayload extends AttributeCallback {

    /**
     * Restricting callback only at Domain-level.
     *
     * @param entry the LDAP entry being modified
     * @return a lowercase label identifying the entry level (e.g. "domain", "cos", "account", "server")
     */
    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) throws ServiceException {
        if (!(entry instanceof Config)) {
            String level = resolveEntryLevel(entry);
            throw ServiceException.PERM_DENIED(
                    String.format("'%s' cannot be configured at the %s level. "
                            + "Please use global config instead.", attrName, level));
        }
    }

    private String resolveEntryLevel(Entry entry) {
        if (entry instanceof Domain) {
            return "domain";
        } else if (entry instanceof Cos) {
            return "cos (class of service)";
        } else if (entry instanceof Account) {
            return "account";
        }
        return entry.getClass().getSimpleName().toLowerCase();
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        // No post-modify actions required
    }
}
