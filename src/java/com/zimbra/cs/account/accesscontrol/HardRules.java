package com.zimbra.cs.account.accesscontrol;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.service.ServiceException.Argument;
import com.zimbra.common.service.ServiceException.InternalArgument;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class HardRules {

    private static Set<String> ALWAYS_FORBIDDEN_ATTRS;
    
    static {
        Set<String> forbiddenAttr = new HashSet<String>();
        forbiddenAttr.add(Provisioning.A_zimbraIsAdminAccount.toLowerCase());
        
        ALWAYS_FORBIDDEN_ATTRS = Collections.unmodifiableSet(forbiddenAttr);
    }
    
    public static enum HardRule {
        NOT_EFFECTIVE_DELEGATED_ADMIN_ACCOUNT,
        DELEGATED_ADMIN_CANNOT_ACCESS_GLOBAL_ADMIN;
        
        public static HardRule ruleVolated(ServiceException e) {
            if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                List<Argument> args = e.getArgs();
                if (args != null) {
                    for (Argument arg : args) {
                        String name = arg.getName();
                        if (name != null) {
                            try {
                                return HardRule.valueOf(name);
                            } catch (IllegalArgumentException iae) {
                            }
                        }
                    }
                }
            }
            return null;
        }
        
        private static Argument getExceptionArgument(HardRule rule) {
            return new InternalArgument(rule.name(), "VIOLATED", 
                    ServiceException.Argument.Type.STR);
        }
    }
    
    /**
     * strict rules for each and every ACL checking calls.
     * 
     * Currently, the only check is if the authed account is a system admin.
     * 
     * @param authedAcct
     * @param target
     * @return
     * @throws ServiceException
     */
    public static Boolean checkHardRules(Account authedAcct, boolean asAdmin, Entry target, Right right) throws ServiceException {
        if (AccessControlUtil.isGlobalAdmin(authedAcct, asAdmin)) {
            return Boolean.TRUE;
        } else {
            boolean isAdminRight = (right == null || !right.isUserRight());
            
            // We are checking an admin right
            if (isAdminRight) {
                // 1. ensure the authed account must be a delegated admin
                if (!AccessControlUtil.isDelegatedAdmin(authedAcct, asAdmin)) {
                    throw ServiceException.PERM_DENIED("not an eligible admin account",
                            HardRule.getExceptionArgument(HardRule.NOT_EFFECTIVE_DELEGATED_ADMIN_ACCOUNT));
                }
                
                // 2. don't allow a delegated-only admin to access a global admin's account,
                //    no matter how much rights it has.
                if (target instanceof Account) {
                    if (AccessControlUtil.isGlobalAdmin((Account)target, true)) {
                        throw ServiceException.PERM_DENIED(
                                "delegated admin is not allowed to access a global admin's account",
                                HardRule.getExceptionArgument(HardRule.DELEGATED_ADMIN_CANNOT_ACCESS_GLOBAL_ADMIN));
                    }
                }
            }
        }
        
        // hard rules are not applicable
        return null;
    }
    
    public static void checkForbiddenAttr(String attrName) throws ServiceException {
        if (isForbiddenAttr(attrName))
            throw ServiceException.PERM_DENIED("delegated admin is not allowed to modify " + attrName);
    }
    
    public static boolean isForbiddenAttr(String attrName) {
        return ALWAYS_FORBIDDEN_ATTRS.contains(attrName.toLowerCase());
    }

}
