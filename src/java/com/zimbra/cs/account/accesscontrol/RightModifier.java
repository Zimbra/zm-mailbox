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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

public enum RightModifier {
    /*
     * the right is specifically denied
     */
    RM_DENY('-', AdminConstants.A_DENY),
    
    /*
     * the same right or part of the right can be delegated(granted) to others
     */
    RM_CAN_DELEGATE('+', AdminConstants.A_CAN_DELEGATE),
    
    /*
     * bug 46602
     * 
     * the grant affects sub domains only
     *   - for domain rights only
     *   - can only be granted on domain targets
     *   - affect sub domains only, *not* the domain on which the right is granted
     *   - does not work with the + modifier.  i.e., grants made with the * modifier cannot
     *     be further granted to other delegated admins by the grantee of the grant.
     *   - can be negated, just like how negation works for grants made without any 
     *     modifier or the delegable modifier
     *
     *   When ACL on an entry is examined, we either look at the:
     *   1) denied + delegable + grants without a modifier
     *      for the "regular" inheritance path
     *   or
     *   2) denied + subDomain
     *      for the sub domain inheritance path, bug 46602
     * 
     */
    RM_SUBDOMAIN('*', AdminConstants.A_SUB_DOMAIN),
    
    /*
     * bug 68820
     * 
     * the grant cannot be inherited by sub-groups on the target side
     *   - can only be associated with dl/account/calresource rights, or combo rights 
     *     containing any dl/account/calresource rights.
     *   - can only be granted on dl targets (it does not make sense to use this modifier 
     *     on any other target types).  
     *   - does not work with the + modifier.  i.e., grants made with the ^ modifier cannot
     *     be further granted to other delegated admins by the grantee of the grant.
     *   - not applicable for target side negation, because grants with this modifier 
     *     are not visible to "sub-target" anyway.  In another words, grants with this 
     *     modifier are automatically negated in all sub-targets.
     *     e.g. subDL is a member of DL
     *          user is a member of subDL
     *          => grants on DL are not visible on subDL and user.
     *     
     *   This modifier disallows the grant to be inherited by sub groups  
     *   - for dl rights, it means the grant only applies to the dl on which the grant
     *     is made, it is not effective on sub-groups
     *   - for account/calresource rights, it means the grant only applies to 
     *     accounts/calresources that are direct member of the dl on which the grant 
     *     is made.
     *
     *     e.g.
     *     DL has members:
     *        user
     *        subDL1
     *
     *     subDL1 has members:
     *        user1
     *        subDL2
     *
     *     subDL2 has members:
     *        user2
     *
     *     grants on DL:
     *         - adminA addDistributionListMember
     *         - adminA modifyAccount
     *         - adminB ^addDistributionListMember
     *         - adminB ^modifyAccount
     *
     *     Then,
     *     adminA can:
     *         - addDistributionListMember on DLs: DL, subDL1, subDL2
     *         - modifyAccount on accounts: user, user1, user2
     * 
     *     adminB can:
     *         - addDistributionListMember on DLs: DL
     *         - modifyAccount on accounts: user
     *            
     */
    RM_DISINHERIT_SUB_GROUPS('^', AdminConstants.A_DISINHERIT_SUB_GROUPS);
    
    // urg, our soap intereface is already published with with the deny attribute, 
    // for backward compatibility, we map the modifier to soap instead of changing soap 
    // to take a modifier attribute
    private String mSoapAttrMapping;
    private char mModifier;
    
    private RightModifier(char modifier, String soapAttrMapping) {
        mModifier = modifier;
        mSoapAttrMapping = soapAttrMapping;
    }
    
    public static RightModifier fromChar(char c) throws ServiceException {
        
        if (RM_DENY.mModifier == c)
            return RM_DENY;
        else if (RM_CAN_DELEGATE.mModifier == c)
            return RM_CAN_DELEGATE;
        else if (RM_DISINHERIT_SUB_GROUPS.mModifier == c)
            return RM_DISINHERIT_SUB_GROUPS;
        else if (RM_SUBDOMAIN.mModifier == c)
            return RM_SUBDOMAIN;
        else
            return null;
    }
    
    public char getModifier() {
        return mModifier;
    }
    
    public String getSoapAttrMapping() {
        return mSoapAttrMapping;
    }
}
