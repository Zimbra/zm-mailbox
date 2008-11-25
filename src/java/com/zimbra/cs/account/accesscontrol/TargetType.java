package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.MemberOf;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.Provisioning.XMPPComponentBy;
import com.zimbra.cs.account.Provisioning.ZimletBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;


public enum TargetType {
    account(true,           AttributeClass.account),
    resource(true,          AttributeClass.calendarResource),
    distributionlist(true,  AttributeClass.distributionList),
    domain(true,            AttributeClass.domain),
    cos(true,               AttributeClass.cos),
    server(true,            AttributeClass.server),
    xmppcomponent(true,     AttributeClass.xmppComponent),
    zimlet(true,            AttributeClass.zimletEntry),
    config(false,           AttributeClass.globalConfig),
    global(false,           AttributeClass.aclTarget);
    
    private boolean mNeedsTargetIdentity;
    private AttributeClass mAttrClass;
    private Set<TargetType> mApplicableTargetTypes;
    
    static {
        init();
    }
    
    /**
     * 
     * @param NeedsTargetIdentity
     * @param attrClass
     * @param applicableTargetTypes target types of rights that can be granted on this target type
     *                              if null, all target types
     */
    TargetType(boolean NeedsTargetIdentity, AttributeClass attrClass) {
        mNeedsTargetIdentity = NeedsTargetIdentity;
        mAttrClass = attrClass;
    }
    
    void setApplicableTargetTypes(TargetType[] applicableTargetTypes) {
        mApplicableTargetTypes = (applicableTargetTypes==null)? null : new HashSet<TargetType>(Arrays.asList(applicableTargetTypes));
    }
    
    static void init() {
        TargetType.account.setApplicableTargetTypes(new TargetType[]{TargetType.account});
        TargetType.resource.setApplicableTargetTypes(new TargetType[]{TargetType.account, TargetType.resource});
        TargetType.distributionlist.setApplicableTargetTypes(new TargetType[]{TargetType.account, TargetType.resource, TargetType.distributionlist});
        TargetType.domain.setApplicableTargetTypes(new TargetType[]{TargetType.account, TargetType.resource, TargetType.distributionlist, TargetType.domain});
        TargetType.cos.setApplicableTargetTypes(new TargetType[]{TargetType.cos});
        TargetType.server.setApplicableTargetTypes(new TargetType[]{TargetType.server});
        TargetType.xmppcomponent.setApplicableTargetTypes(new TargetType[]{TargetType.xmppcomponent});
        TargetType.config.setApplicableTargetTypes(new TargetType[]{TargetType.config});
        TargetType.global.setApplicableTargetTypes(null);
    }
    
    public static TargetType fromString(String s) throws ServiceException {
        try {
            return TargetType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("unknown target type: " + s, e);
        }
    }
    
    public String getCode() {
        return name();
    }
    
    public boolean needsTargetIdentity() {
        return mNeedsTargetIdentity;
    }
    
    AttributeClass getAttributeClass() {
        return mAttrClass;
    }
    
    /**
     * returns if a right will be effective if granted on this target type(right granted on) entry
     * 
     * @param right must be a preset right
     * @return
     */
    boolean isRightApplicable(Right right) throws ServiceException {
        if (!right.isPresetRight())
            throw ServiceException.FAILURE("internal error", null);
        
        if (mApplicableTargetTypes == null)
            return true;
        else 
            return mApplicableTargetTypes.contains(right.getTargetType());
    }
    
    /**
     * central place where a target should be loaded
     * 
     * @param prov
     * @param targetType
     * @param targetBy
     * @param target
     * @return
     * @throws ServiceException
     */
    public static Entry lookupTarget(Provisioning prov, TargetType targetType, TargetBy targetBy, String target) throws ServiceException {
        Entry targetEntry = null;
        
        switch (targetType) {
        case account:
            targetEntry = prov.get(AccountBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(target); 
            break;
        case resource:
            targetEntry = prov.get(CalendarResourceBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(target); 
            break;
        case distributionlist:
            targetEntry = prov.getAclGroup(DistributionListBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(target); 
            break;
        case domain:
            targetEntry = prov.get(DomainBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(target); 
            break;
        case cos:
            targetEntry = prov.get(CosBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_COS(target); 
            break;
        case server:
            targetEntry = prov.get(ServerBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_SERVER(target); 
            break;
        case xmppcomponent:
            targetEntry = prov.get(XMPPComponentBy.fromString(targetBy.name()), target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_XMPP_COMPONENT(target); 
            break;    
        case zimlet:
            ZimletBy zimletBy = ZimletBy.fromString(targetBy.name());
            if (zimletBy != ZimletBy.name)
                throw ServiceException.INVALID_REQUEST("zimlet must be by name", null);
            targetEntry = prov.getZimlet(target);
            if (targetEntry == null)
                throw AccountServiceException.NO_SUCH_ZIMLET(target); 
            break;
        case config:
            targetEntry = prov.getConfig();
            break;
        case global:
            targetEntry = prov.getGlobalGrant();
            break;
        default:
            ServiceException.INVALID_REQUEST("invallid target type for lookupTarget:" + targetType.toString(), null);
        }
    
        return targetEntry;
    }


    
    static AttributeClass getAttributeClass(Entry target) throws ServiceException{
        
        if (target instanceof CalendarResource)
            return AttributeClass.calendarResource;
        else if (target instanceof Account)
            return AttributeClass.account;
        else if (target instanceof Domain)
            return AttributeClass.domain;
        else if (target instanceof Cos)
            return AttributeClass.cos;
        else if (target instanceof DistributionList)
            return AttributeClass.distributionList;
        else if (target instanceof Server)
            return AttributeClass.server;
        else if (target instanceof Config)
            return AttributeClass.globalConfig;
        else if (target instanceof GlobalGrant)
            return AttributeClass.aclTarget;
        else if (target instanceof Zimlet)
            return AttributeClass.zimletEntry;
        else if (target instanceof XMPPComponent)
            return AttributeClass.xmppComponent;
        else
            throw ServiceException.FAILURE("internal error", null);
    }
    
    static TargetType getTargetType(Entry target) throws ServiceException{
        
        if (target instanceof CalendarResource)
            return TargetType.resource;
        else if (target instanceof Account)
            return TargetType.account;
        else if (target instanceof Domain)
            return TargetType.domain;
        else if (target instanceof Cos)
            return TargetType.cos;
        else if (target instanceof DistributionList)
            return TargetType.distributionlist;
        else if (target instanceof Server)
            return TargetType.server;
        else if (target instanceof Config)
            return TargetType.config;
        else if (target instanceof GlobalGrant)
            return TargetType.global;
        else if (target instanceof Zimlet)
            return TargetType.zimlet;
        else if (target instanceof XMPPComponent)
            return TargetType.xmppcomponent;
        else
            throw ServiceException.FAILURE("internal error", null);
    }


}
