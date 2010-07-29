package com.zimbra.cs.account.accesscontrol;

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;

interface AdminConsoleCapable {

    void getAllEffectiveRights(RightBearer rightBearer, 
            boolean expandSetAttrs, boolean expandGetAttrs,
            AllEffectiveRights result) throws ServiceException;
    
    void getEffectiveRights(RightBearer rightBearer, Entry target, 
            boolean expandSetAttrs, boolean expandGetAttrs,
            RightCommand.EffectiveRights result) throws ServiceException;
    
    /**
     * grant types for the grant search for revoking all rights
     * when a Grantee is to be deleted.
     * 
     * @return
     */
    Set<TargetType> targetTypesForGrantSearch();
}
