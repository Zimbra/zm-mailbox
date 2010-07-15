/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;

public abstract class TargetIterator{
    protected Provisioning mProv;
    protected TargetType mCurTargetType;
    protected Entry mTarget;
    protected boolean mCheckedSelf;
    protected boolean mNoMore;
    
    Entry next() throws ServiceException {
        if (mNoMore)
            return null;
            
        Entry grantedOn = null;
            
        if (!mCheckedSelf) {
            mCurTargetType = TargetType.global;
            mCheckedSelf = true;
            grantedOn = mTarget;
                
        } else if (mCurTargetType == TargetType.global) {
            mNoMore = true;
            grantedOn = mProv.getGlobalGrant();
        }
                
        return grantedOn;
    }

    
    private TargetIterator(Provisioning prov, TargetType initialTargetType, Entry target) {
        mProv = prov;
        mCurTargetType = initialTargetType;
        mTarget = target;
    }
    
    static TargetIterator getTargetIeterator(Provisioning prov, Entry target, boolean expandGroups) throws ServiceException {
        /*
         * tested in the order of how often acl are checked on each target types in normal 
         * server operation pattern.
         * 
         * CalendarResource is tested before account beore it is a subclass of Account.
         * Could remove testing for CalendarResource here because it would be the same 
         * as Account.  Leave it for now.
         *  
         */
        TargetIterator iter = null;
        
        if (target instanceof CalendarResource)
            iter = new TargetIterator.AccountTargetIterator(prov, target, expandGroups);
        else if (target instanceof Account)
            iter =  new TargetIterator.AccountTargetIterator(prov, target, expandGroups);
        else if (target instanceof Domain)
            iter =  new TargetIterator.DomainTargetIterator(prov, target);
        else if (target instanceof Cos)
            iter =  new TargetIterator.CosTargetIterator(prov, target);
        else if (target instanceof DistributionList) {
            // This path is called from AccessManager.canDo, the target object can be a 
            // DistributionList obtained from prov.get(DistributionListBy).  
            // We require one from prov.getAclGroup(DistributionListBy) here, 
            // call getAclGroup if it is not yet an ACL group.
            if (!((DistributionList)target).isAclGroup())
                target = prov.getAclGroup(DistributionListBy.id, ((DistributionList)target).getId());
            iter =  new TargetIterator.DistributionListTargetIterator(prov, target, expandGroups);
        } else if (target instanceof Server)
            iter =  new TargetIterator.ServerTargetIterator(prov, target);
        else if (target instanceof Config)
            iter =  new TargetIterator.ConfigTargetIterator(prov, target);
        else if (target instanceof GlobalGrant)
            iter =  new TargetIterator.GlobalGrantTargetIterator(prov, target);
        else if (target instanceof Zimlet)
            iter =  new TargetIterator.ZimletTargetIterator(prov, target);
        else if (target instanceof XMPPComponent)
            iter =  new TargetIterator.XMPPComponentTargetIterator(prov, target);
        else
            throw ServiceException.FAILURE("internal error", null);
        
        /*
         * consume the first target, which is the perspective target itself, because RightChecker.canDo 
         * checks the perspective target separately first, then go through the target chain.
         * 
         * For now we leave code in TargetIterator to return the perspective target as the first item.  Will
         * remove it and calling to the next() here if returning the perspective target is not useful at all.
         * 
         */
        iter.next();
        return iter;
    }
    
    public static class AccountTargetIterator extends TargetIterator {
        private boolean mExpandGroups;
        private AclGroups mGroups = null;
        private int mIdxInGroups = 0;
        
        AccountTargetIterator(Provisioning prov, Entry target, boolean expandGroups) {
            super(prov, TargetType.account, target);
            mExpandGroups = expandGroups;
        }
        
        @Override
        Entry next() throws ServiceException {
            if (mNoMore)
                return null;
            
            Entry grantedOn = null;
            
            if (!mCheckedSelf) {
                if (mExpandGroups)
                    mCurTargetType = TargetType.dl;
                else
                    mCurTargetType = TargetType.domain;
                
                mCheckedSelf = true;
                grantedOn = mTarget;
                
            } else if (mCurTargetType == TargetType.dl) {
                if (mGroups == null) {
                    // LdapProvisioning.getAclGroups will do a LDAP search
                    // if the AclGroups is not computed/cached yet.  
                    // Do not even go there if we are a pseudo object,
                    // just create an empty AclGroups and all our TargetIterator
                    // flow will be the same.
                    if (mTarget instanceof PseudoTarget.PseudoAccount)    
                        mGroups = new AclGroups();
                    else        
                        mGroups =  mProv.getAclGroups((Account)mTarget, false);
                }
                
                if (mIdxInGroups < mGroups.groupIds().size()) {
                    grantedOn = mProv.getAclGroup(DistributionListBy.id, mGroups.groupIds().get(mIdxInGroups));
                    mIdxInGroups++;
                } else {
                    mCurTargetType = TargetType.domain;
                    grantedOn = next();
                }
                
            } else if (mCurTargetType == TargetType.domain) {
                mCurTargetType = TargetType.global;
                
                Domain pseudoDomain = null;
                if (mTarget instanceof PseudoTarget.PseudoAccount)
                    pseudoDomain = ((PseudoTarget.PseudoAccount)mTarget).getPseudoDomain();
                else if (mTarget instanceof PseudoTarget.PseudoCalendarResource)
                    pseudoDomain = ((PseudoTarget.PseudoCalendarResource)mTarget).getPseudoDomain();
                
                if (pseudoDomain != null)
                    grantedOn = next();
                else
                    grantedOn = mProv.getDomain((Account)mTarget);
                
            } else if (mCurTargetType == TargetType.global) {
                mNoMore = true;
                grantedOn = mProv.getGlobalGrant();
            }
                
            return grantedOn;
        }
    }
    
    public static class DistributionListTargetIterator extends TargetIterator {
        private boolean mExpandGroups;
        private AclGroups mGroups = null;
        private int mIdxInGroups = 0;
        
        DistributionListTargetIterator(Provisioning prov, Entry target, boolean expandGroups) {
            super(prov, TargetType.dl, target);
            mExpandGroups = expandGroups;
        }
        
        @Override
        Entry next() throws ServiceException {
            if (mNoMore)
                return null;
            
            Entry grantedOn = null;
            
            if (!mCheckedSelf) {
                if (mExpandGroups)
                    mCurTargetType = TargetType.dl;
                else
                    mCurTargetType = TargetType.domain;
                
                mCheckedSelf = true;
                grantedOn = mTarget;
                
            } else if (mCurTargetType == TargetType.dl) {
                if (mGroups == null) {
                    // LdapProvisioning.getAclGroups will do a LDAP search
                    // if the AclGroups is not computed/cached yet.  
                    // Do not even go there if we are a pseudo object,
                    // just create an empty AclGroups and all our TargetIterator
                    // flow will be the same.
                    if (mTarget instanceof PseudoTarget.PseudoDistributionList)
                        mGroups = new AclGroups();
                    else        
                        mGroups =  mProv.getAclGroups((DistributionList)mTarget, false);
                }
                
                if (mIdxInGroups < mGroups.groupIds().size()) {
                    grantedOn = mProv.getAclGroup(DistributionListBy.id, mGroups.groupIds().get(mIdxInGroups));
                    mIdxInGroups++;
                } else {
                    mCurTargetType = TargetType.domain;
                    grantedOn = next();
                }
                
            } else if (mCurTargetType == TargetType.domain) {
                mCurTargetType = TargetType.global;
                
                Domain pseudoDomain = null;
                if (mTarget instanceof PseudoTarget.PseudoDistributionList)
                    pseudoDomain = ((PseudoTarget.PseudoDistributionList)mTarget).getPseudoDomain();
                
                if (pseudoDomain != null)
                    grantedOn = next();
                else
                    grantedOn = mProv.getDomain((DistributionList)mTarget);
                
            } else if (mCurTargetType == TargetType.global) {
                mNoMore = true;
                grantedOn = mProv.getGlobalGrant();
            }
                
            return grantedOn;
        }
    }
        
    public static class ConfigTargetIterator extends TargetIterator {
        
        ConfigTargetIterator(Provisioning prov, Entry target) {
            super(prov, TargetType.account, target);
        }
    }
    
    public static class CosTargetIterator extends TargetIterator {
        
        CosTargetIterator(Provisioning prov, Entry target) {
            super(prov, TargetType.account, target);
        }
    }
    
    public static class DomainTargetIterator extends TargetIterator {
        private int mCurSuperDomain;
        private List<Domain> mSuperDomains;
        
        DomainTargetIterator(Provisioning prov, Entry target) throws ServiceException {
            super(prov, TargetType.account, target);
            
            mCurSuperDomain = 0;
            mSuperDomains = new ArrayList<Domain>();
            
            Domain domain = (Domain)target;
            String domainName = domain.getName();
            
            int nextDot = 0;
            while ((nextDot = domainName.indexOf('.')) != -1) { // if nextDot is -l we've reached the top
                domainName = domainName.substring(nextDot+1);
                Domain parentDomain = mProv.getDomain(Provisioning.DomainBy.name, domainName, true); // check negative cache
                if (parentDomain != null)
                    mSuperDomains.add(parentDomain);
            }
        }
        
        @Override
        Entry next() throws ServiceException {
            if (mNoMore)
                return null;
            
            Entry grantedOn = null;
            
            if (!mCheckedSelf) {
                mCheckedSelf = true;
                grantedOn = mTarget;
            } else {
                if (mCurSuperDomain < mSuperDomains.size()) {
                    grantedOn = mSuperDomains.get(mCurSuperDomain);
                    mCurSuperDomain++;
                } else {
                    mNoMore = true;
                    grantedOn = mProv.getGlobalGrant();
                }
            }
            
            return grantedOn;
        }
    }

    public static class ServerTargetIterator extends TargetIterator {
        
        ServerTargetIterator(Provisioning prov, Entry target) {
            super(prov, TargetType.account, target);
        }
    }
    
    public static class XMPPComponentTargetIterator extends TargetIterator {
        
        XMPPComponentTargetIterator(Provisioning prov, Entry target) {
            super(prov, TargetType.account, target);
        }
    }
    
    public static class ZimletTargetIterator extends TargetIterator {
        
        ZimletTargetIterator(Provisioning prov, Entry target) {
            super(prov, TargetType.account, target);
        }
    }
    
    public static class GlobalGrantTargetIterator extends TargetIterator {
        
        GlobalGrantTargetIterator(Provisioning prov, Entry target) {
            super(prov, TargetType.account, target);
        }
        
        @Override
        Entry next() throws ServiceException {
            if (mNoMore)
                return null;
                
            Entry grantedOn = null;
                
            if (!mCheckedSelf) {
                mCheckedSelf = true;
                mNoMore = true;
                grantedOn = mTarget;
            }
                    
            return grantedOn;
        }
    }
    
    public static void main(String[] args) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        // sub1.sub2.sub3.sub4.sub5.top
        
        Domain sub1 = prov.createDomain("sub1.sub2.sub3.sub4.sub5.top", new HashMap<String, Object>());
        Domain sub3 = prov.createDomain("sub3.sub4.sub5.top", new HashMap<String, Object>());
        Domain sub5 = prov.createDomain("sub5.top", new HashMap<String, Object>());
        
        TargetIterator targetIter;
        Entry grantedOn;
        
        System.out.println("Testing " + sub1.getName());
        targetIter = getTargetIeterator(prov, sub1, false);
        while ((grantedOn = targetIter.next()) != null) {
            System.out.println(grantedOn.getLabel());
        }
        
        System.out.println();
        
        System.out.println("Testing " + sub3.getName());
        targetIter = getTargetIeterator(prov, sub3, false);
        while ((grantedOn = targetIter.next()) != null) {
            System.out.println(grantedOn.getLabel());
        }
        
        System.out.println();
        
        System.out.println("Testing " + sub5.getName());
        targetIter = getTargetIeterator(prov, sub5, false);
        while ((grantedOn = targetIter.next()) != null) {
            System.out.println(grantedOn.getLabel());
        }
    }
}
