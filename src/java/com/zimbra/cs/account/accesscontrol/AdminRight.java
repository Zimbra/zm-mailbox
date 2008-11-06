package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

public class AdminRight extends Right {
    
    // known rights
    public static AdminRight RT_createAccount;
    public static AdminRight RT_renameAccount;
    public static AdminRight RT_renameCalendarResource;
    public static AdminRight RT_renameDistributionList;
    public static AdminRight RT_renameCos;
    public static AdminRight RT_renameServer;
    public static AdminRight RT_testGlobalConfigRemoveMe;
    public static AdminRight RT_testGlobalGrantRemoveMe;
    
    enum RightType {
        preset,
        getAttrs,
        modifyAttrs;
        
        public static RightType fromString(String s) throws ServiceException {
            try {
                return RightType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR("unknown right type: " + s, e);
            }
        }
    }
    
    enum EntryType {
        account,
        cos;
        
        public static EntryType fromString(String s) throws ServiceException {
            try {
                return EntryType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR("unknown entry type: " + s, e);
            }
        }
    }
    
    class AttrRight {
        
        private String mAttrName;
        private Set<EntryType> mOnEntries = new HashSet<EntryType>();
        private boolean mLimit;
        
        AttrRight(String name) {
            mAttrName = name;
        }
        
        void addOnEntry(EntryType entryType) {
            mOnEntries.add(entryType);
        }
    
        void setOnAllEntries() {
            for (EntryType et : EntryType.values())
                addOnEntry(et);
        }
        
        void setLimit(boolean limit) {
            mLimit = limit;
        }
    }
    
    private RightType mRightType;
    private Map<String, AttrRight> mAttrs;
    
    static void initKnownAdminRights(RightManager rm) throws ServiceException {
        RT_createAccount = rm.getAdminRight("createAccount");
        RT_renameAccount = rm.getAdminRight("renameAccount");
        RT_renameCalendarResource = rm.getAdminRight("renameCalendarResource");
        RT_renameDistributionList = rm.getAdminRight("renameDistributionList");
        RT_renameCos = rm.getAdminRight("renameCos");
        RT_renameServer = rm.getAdminRight("renameServer");
        RT_testGlobalConfigRemoveMe = rm.getAdminRight("testGlobalConfigRemoveMe");
        RT_testGlobalGrantRemoveMe = rm.getAdminRight("testGlobalGrantRemoveMe");
    }
    
    AdminRight(String name, RightType rightType) {
        super(name);
        mRightType = rightType;
        if (mRightType == RightType.getAttrs || mRightType == RightType.modifyAttrs)
            mAttrs = new HashMap<String, AttrRight>();
    }
    
    AttrRight addAttr(String attrName) {
        AttrRight attrRight = new AttrRight(attrName);
        mAttrs.put(attrName, attrRight);
        return attrRight;
    }
    
    RightType getRightType() {
        return mRightType;
    }
    
}
