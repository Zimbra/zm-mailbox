/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;

public class AllowedAttrs implements AccessManager.AttrRightChecker {
    
    private static final Log sLog = LogFactory.getLog(AllowedAttrs.class);
    
    public enum Result {
        ALLOW_ALL,
        DENY_ALL,
        ALLOW_SOME;
    }
    
    private Result mResult;
    private Set<String> mAllowSome;

    public static final AllowedAttrs ALLOW_ALL_ATTRS() {
        return new AllowedAttrs(AllowedAttrs.Result.ALLOW_ALL, null);
    }
    
    public static final AllowedAttrs DENY_ALL_ATTRS() {
        return new AllowedAttrs(AllowedAttrs.Result.DENY_ALL, null);
    }
    
    public static AllowedAttrs ALLOW_SOME_ATTRS(Set<String> allowSome) {
        return new AllowedAttrs(AllowedAttrs.Result.ALLOW_SOME, allowSome);
    }
    
    private AllowedAttrs(Result result, Set<String> allowSome) {
        mResult = result;
        mAllowSome = allowSome;
    }
    
    public Result getResult() {
        return mResult;
    }
    
    public Set<String> getAllowed() {
        return mAllowSome;
    }
    
    public boolean allowAttr(String attrName) {
        if (mResult == AllowedAttrs.Result.ALLOW_ALL)
            return true;
        else if (mResult == AllowedAttrs.Result.DENY_ALL)
            return false;
        else {
            String actualAttrName = getActualAttrName(attrName);
            return getAllowed().contains(actualAttrName);
        }
    }
    
    boolean canAccessAttrs(Set<String> attrsNeeded, Entry target) throws ServiceException {
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("canAccessAttrs attrsAllowed: " + dump());
            
            StringBuilder sb = new StringBuilder();
            if (attrsNeeded == null)
                sb.append("<all attributes>");
            else {
                for (String a : attrsNeeded)
                    sb.append(a + " ");
            }
            sLog.debug("canAccessAttrs attrsNeeded: " + sb.toString());
        }
        
        if (mResult == AllowedAttrs.Result.ALLOW_ALL)
            return true;
        else if (mResult == AllowedAttrs.Result.DENY_ALL)
            return false;
        
        //
        // allow some
        //
        
        // need all, nope
        if (attrsNeeded == null)
            return false;
        
        // see if all needed are allowed
        Set<String> allowed = getAllowed();
        for (String attr : attrsNeeded) {
            String attrName = getActualAttrName(attr);
            if (!allowed.contains(attrName)) {
                /*
                 * throw instead of return false, so it is easier for users to 
                 * figure out the denied reason.  All callsite of this method 
                 * can handle either a false return value or a PERM_DENIED  
                 * ServiceException and react properly.
                 */
                // return false;
                throw ServiceException.PERM_DENIED("cannot access attribute " + attrName +
                        " on " + TargetType.getTargetType(target) + " target " +
                        target.getLabel());
            }
        }
        return true;
    }
    
    /**
     * Returns if setting attrs to the specified values is allowed.
     * 
     * This method DOES check for constraints.
     * 
     * @param attrsAllowed result from canAccessAttrs
     * @param attrsNeeded attrs needed to be set.  Cannot be null, must specify which attrs/values to set
     * @return
     */
    boolean canSetAttrs(Grantee grantee, Entry target, Map<String, Object> attrsNeeded) throws ServiceException {
        
        if (attrsNeeded == null)
            throw ServiceException.FAILURE("internal error", null);
        
        if (mResult == AllowedAttrs.Result.DENY_ALL)
            return false;
        
        Entry constraintEntry = AttributeConstraint.getConstraintEntry(target);
        Map<String, AttributeConstraint> constraints = (constraintEntry==null)?null:
            AttributeConstraint.getConstraint(constraintEntry);
        boolean hasConstraints = (constraints != null && !constraints.isEmpty());
        
        if (hasConstraints) {
            // see if the grantee can set zimbraConstraint on the constraint entry
            // if so, the grantee can set attrs to any value (not restricted by the constraints)
            AllowedAttrs allowedAttrsOnConstraintEntry = 
                CheckAttrRight.accessibleAttrs(grantee, constraintEntry, AdminRight.PR_SET_ATTRS, false);
            
            if (allowedAttrsOnConstraintEntry.getResult() == AllowedAttrs.Result.ALLOW_ALL ||
                (allowedAttrsOnConstraintEntry.getResult() == AllowedAttrs.Result.ALLOW_SOME &&
                 allowedAttrsOnConstraintEntry.getAllowed().contains(Provisioning.A_zimbraConstraint)))
                hasConstraints = false;
        }
        
        boolean allowAll = (mResult == AllowedAttrs.Result.ALLOW_ALL);
        Set<String> allowed = getAllowed();
        
        for (Map.Entry<String, Object> attr : attrsNeeded.entrySet()) {
            String attrName = getActualAttrName(attr.getKey());
            
            if (!allowAll && !allowed.contains(attrName)) {
                /*
                 * throw instead of return false, so it is easier for users to 
                 * figure out the denied reason.  All callsite of this method 
                 * can handle either a false return value or a PERM_DENIED  
                 * ServiceException and react properly.
                 */
                // return false;
                throw ServiceException.PERM_DENIED("cannot access attribute " + attrName +
                        " on " + TargetType.getTargetType(target) + " target " +
                        target.getLabel());
            }
            
            if (hasConstraints) {
                if (AttributeConstraint.violateConstraint(constraints, attrName, attr.getValue()))
                    return false;
            }
        }
        return true;
    }
    
    private String getActualAttrName(String attr) {
        if (attr.charAt(0) == '+' || attr.charAt(0) == '-')
            return attr.substring(1);
        else
            return attr;
    }
    
    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("result = " + mResult + " ");
        
        if (mResult == Result.ALLOW_SOME) {
            sb.append("allowed = (");
            for (String a : mAllowSome)
                sb.append(a + " ");
            sb.append(")");
        }
        
        return sb.toString();
    }
}
