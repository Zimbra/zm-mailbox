package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.Right.RightType;
import com.zimbra.cs.account.ldap.LdapProvisioning;

public class RightCommand {
    
    /*
     * ACL and ACE are aux class for ProvUtil.  We don't want to pass "live"(those actually being used 
     * in the server) ZimbraACL/ZimbraACE objects to ProvUtil, because:
     *     - Some methods(e.g. ZimbraACE.getGranteeDisplayName ) calls Provisioning.getInstance(), 
     *       which is an instance of LdapProvisioning, which should not be done from ProvUtil when 
     *       the command is via soap.
     *       
     *     - We really just want to pass a "static" object in that all data members are "burned in" 
     *       and cannot be manipulated, since the sole purpose for them is to be displayed/read.
     *       
     * Use String instead of TargetTyep/GranteeType/Right data members in those classes so they 
     * can be readily displayed/serialize without further dependency of any server side logic, e.g.
     * RightManager, which would access LDAP for custom rights that are defined in LDAP.
     */
    
    public static class ACL {
        Set<ACE> mACEs = new HashSet<ACE>();
        
        ACL() {
        }
        
        void addACE(ACE ace) {
            mACEs.add(ace);
        }
        
        public Set<ACE> getACEs() {
            return mACEs;
        }
        
        /*
         * ctor or parsing ACL from a SOAP response
         * called from CLI
         */
        public ACL(Element parent) throws ServiceException {
            for (Element eGrant : parent.listElements(AdminConstants.E_GRANT)) {
                String granteeType = eGrant.getAttribute(AdminConstants.A_TYPE);
                String granteeId = eGrant.getAttribute(AdminConstants.A_ID);
                String granteeName = eGrant.getAttribute(AdminConstants.A_NAME);
                String right = eGrant.getAttribute(AdminConstants.A_RIGHT);
                boolean deny = eGrant.getAttributeBool(AdminConstants.A_DENY, false);
                
                ACE ace = new ACE(granteeType, granteeId, granteeName, right, deny);
                addACE(ace);
            }
        }
        
        /*
         * ctor to construct an ACL from ZimbraACL
         * called in server
         */
        private ACL(ZimbraACL acl) {
            if (acl == null)
                return;
                
            for (ZimbraACE ace : acl.getAllACEs()) {
                addACE(new ACE(ace));
            }
        }
        
        public void toXML(Element parent) {
            for (ACE ace : mACEs) {
                Element eGrant = parent.addElement(AdminConstants.E_GRANT);
                eGrant.addAttribute(AdminConstants.A_TYPE, ace.granteeType());
                eGrant.addAttribute(AdminConstants.A_ID, ace.granteeId());
                eGrant.addAttribute(AdminConstants.A_NAME, ace.granteeName());
                eGrant.addAttribute(AdminConstants.A_RIGHT, ace.right());
                eGrant.addAttribute(AdminConstants.A_DENY, ace.deny());
            }
        }
    }
    
    public static class ACE {
        String mGranteeType;
        String mGranteeId;
        String mGranteeName;
        String mRight;
        boolean mDeny;
    
        /*
         * called from CLI
         */
        private ACE(String granteeType,
            String granteeId,
            String granteeName,
            String right,
            boolean deny) {
            
            mGranteeType = granteeType;
            mGranteeId = granteeId;
            mGranteeName = granteeName;
            mRight = right;
            mDeny = deny;
        }
        
        /*
         * called in server
         */
        private ACE(ZimbraACE ace) {
            mGranteeType = ace.getGranteeType().getCode();
            mGranteeId = ace.getGrantee();
            mGranteeName = ace.getGranteeDisplayName();
            mRight = ace.getRight().getName();
            mDeny = ace.deny();
        }
        
        public String granteeType() { return mGranteeType; }
        public String granteeId()   { return mGranteeId; }
        public String granteeName() { return mGranteeName; }
        public String right()       { return mRight; }
        public boolean deny()       { return mDeny; }
    }
    
    public static class EffectiveAttr {
        private static final Set<String> EMPTY_SET = new HashSet<String>();
        
        String mAttrName;
        Set<String> mDefault;
        
        EffectiveAttr(String attrName, Set<String> defaultValue) {
            mAttrName = attrName;
            mDefault = defaultValue;
        }
        
        public String getAttrName() { return mAttrName; }
        
        public Set<String> getDefault()  { 
            if (mDefault == null)
                return EMPTY_SET;
            else
                return mDefault; 
        }
    }
    
    public static class EffectiveRights {
        private static final SortedMap<String, EffectiveAttr> EMPTY_MAP = new TreeMap<String, EffectiveAttr>();
        private static final List<String> EMPTY_LIST = new ArrayList<String>();
        
        String mTargetType;
        String mTargetId;
        String mTargetName;
        String mGranteeId;
        String mGranteeName;
        
        // preset
        List<String> mPresetRights = new ArrayList<String>();
        
        // setAttrs
        boolean mCanSetAllAttrs = false;
        SortedMap<String, EffectiveAttr> mCanSetAttrs = EMPTY_MAP;
        
        // getAttrs
        boolean mCanGetAllAttrs = false;
        SortedMap<String, EffectiveAttr> mCanGetAttrs = EMPTY_MAP;
        
        EffectiveRights(String targetType, String targetId, String targetName, String granteeId, String granteeName) {
            mTargetType = targetType;
            mTargetId = targetId;
            mTargetName = targetName;
            mGranteeId = granteeId;
            mGranteeName = granteeName;
        }
        
        public EffectiveRights(Element parent) throws ServiceException {
            //
            // grantee
            //
            Element eGrantee = parent.getElement(AdminConstants.E_GRANTEE);
            mGranteeId = eGrantee.getAttribute(AdminConstants.A_ID);
            mGranteeName= eGrantee.getAttribute(AdminConstants.A_NAME);
            
            //
            // target
            //
            Element eTarget = parent.getElement(AdminConstants.E_TARGET);
            mTargetType = eTarget.getAttribute(AdminConstants.A_TYPE);
            mTargetId = eTarget.getAttribute(AdminConstants.A_ID);
            mTargetName= eTarget.getAttribute(AdminConstants.A_NAME);
            
            // preset rights
            mPresetRights = new ArrayList<String>();
            for (Element eRight : eTarget.listElements(AdminConstants.E_RIGHT)) {
                mPresetRights.add(eRight.getAttribute(AdminConstants.A_N));
            }
                
            // setAttrs
            Element eSetAttrs = eTarget.getElement(AdminConstants.E_SET_ATTRS);
            if (eSetAttrs.getAttributeBool(AdminConstants.A_ALL, false))
                mCanSetAllAttrs = true;
            
            mCanSetAttrs = fromXML(eSetAttrs);
            
            // getAttrs
            Element eGetAttrs = eTarget.getElement(AdminConstants.E_GET_ATTRS);
            if (eGetAttrs.getAttributeBool(AdminConstants.A_ALL, false))
                mCanGetAllAttrs = true;
                
            mCanGetAttrs = fromXML(eGetAttrs);
        }
        
        private TreeMap<String, EffectiveAttr> fromXML(Element eAttrs) throws ServiceException {
            TreeMap<String, EffectiveAttr> attrs = new TreeMap<String, EffectiveAttr>();
            
            for (Element eAttr : eAttrs.listElements(AdminConstants.E_A)) {
                String attrName = eAttr.getAttribute(AdminConstants.A_N);
                Element eDefault = eAttr.getOptionalElement(AdminConstants.E_DEFAULT);
                Set<String> defaultValues = null;
                if (eDefault != null) {
                    defaultValues = new HashSet<String>();
                    for (Element eValue : eDefault.listElements(AdminConstants.E_VALUE)) {
                        defaultValues.add(eValue.getText());
                    }
                }
                EffectiveAttr ea = new EffectiveAttr(attrName, defaultValues);
                attrs.put(attrName, ea);
            }
            
            return attrs;
        }
        
        public void toXML(Element parent) {
            //
            // grantee
            //
            Element eGrantee = parent.addElement(AdminConstants.E_GRANTEE);
            eGrantee.addAttribute(AdminConstants.A_ID, mGranteeId);
            eGrantee.addAttribute(AdminConstants.A_NAME, mGranteeName);
            
            //
            // target
            //
            Element eTarget = parent.addElement(AdminConstants.E_TARGET);
            eTarget.addAttribute(AdminConstants.A_TYPE, mTargetType);
            eTarget.addAttribute(AdminConstants.A_ID, mTargetId);
            eTarget.addAttribute(AdminConstants.A_NAME, mTargetName);
            
            // preset rights
            for (String r : mPresetRights) {
                Element eRight = eTarget.addElement(AdminConstants.E_RIGHT).addAttribute(AdminConstants.A_N, r);
            }
            
            // setAttrs
            toXML(eTarget, AdminConstants.E_SET_ATTRS, mCanSetAllAttrs, mCanSetAttrs);

            // getAttrs
            toXML(eTarget, AdminConstants.E_GET_ATTRS, mCanGetAllAttrs, mCanGetAttrs);
           
        }
        
        private void toXML(Element parent, String elemName, boolean allAttrs, SortedMap<String, EffectiveAttr> attrs) {
            Element eAttrs = parent.addElement(elemName);
            if (allAttrs) {
                eAttrs.addAttribute(AdminConstants.A_ALL, true);
            }
               
            for (EffectiveAttr ea : attrs.values()) {
                Element eAttr = eAttrs.addElement(AdminConstants.E_A);
                eAttr.addAttribute(AdminConstants.A_N, ea.getAttrName());
                if (!ea.getDefault().isEmpty()) {
                    Element eDefault = eAttr.addElement(AdminConstants.E_DEFAULT);
                    for (String v : ea.getDefault())
                        eDefault.addElement(AdminConstants.E_VALUE).setText(v);
                }
            }
        }

        void setPresetRights(List<String> presetRights) { mPresetRights = presetRights; } 
        void setCanSetAllAttrs() { mCanSetAllAttrs = true; }
        void setCanSetAttrs(SortedMap<String, EffectiveAttr> canSetAttrs) { mCanSetAttrs = canSetAttrs; }
        void setCanGetAllAttrs() { mCanGetAllAttrs = true; }
        void setCanGetAttrs(SortedMap<String, EffectiveAttr> canGetAttrs) { mCanGetAttrs = canGetAttrs; }
        
        public String targetType() { return mTargetType; }
        public String targetId()   { return mTargetId; }
        public String targetName() { return mTargetName; }
        public String granteeId() { return mGranteeId; }
        public String granteeName() { return mGranteeName; }
        public List<String> presetRights() { return mPresetRights; }
        public boolean canSetAllAttrs() { return mCanSetAllAttrs; } 
        public SortedMap<String, EffectiveAttr> canSetAttrs() { return mCanSetAttrs; }
        public boolean canGetAllAttrs() { return mCanGetAllAttrs; } 
        public SortedMap<String, EffectiveAttr> canGetAttrs() { return mCanGetAttrs; }
    }
    
    
    public static boolean checkRight(Provisioning prov,
                                     String targetType, TargetBy targetBy, String target,
                                     GranteeBy granteeBy, String grantee,
                                     String right,
                                     AccessManager.ViaGrant via) throws ServiceException {
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.GT_USER;
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);  // grantee for check right must be an Account
        
        // right
        Right r = RightManager.getInstance().getRight(right);
        
        boolean canPerform = AccessManager.getInstance().canDo((Account)granteeEntry, targetEntry, r, true, false, via);
        return canPerform;
    }
    
    public static EffectiveRights getEffectiveRights(Provisioning prov,
                                                     String targetType, TargetBy targetBy, String target,
                                                     GranteeBy granteeBy, String grantee,
                                                     boolean expandSetAttrs, boolean expandGetAttrs) throws ServiceException {

        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.GT_USER;
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);  
        // granteeEntry right must be an Account
        Account granteeAcct = (Account)granteeEntry;
        
        String targetId = (targetEntry instanceof NamedEntry)? ((NamedEntry)targetEntry).getId() : "";
        EffectiveRights er = new EffectiveRights(targetType, targetId, targetEntry.getLabel(), granteeAcct.getId(), granteeAcct.getName());
        
        RightChecker.getEffectiveRights(granteeAcct, targetEntry, expandSetAttrs, expandGetAttrs, er);
        return er;
    }
    
    public static ACL getGrants(Provisioning prov,
                                String targetType, TargetBy targetBy, String target) throws ServiceException {
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        ZimbraACL zimbraAcl = RightUtil.getACL(targetEntry);
        return new ACL(zimbraAcl);
    }
            
    public static void grantRight(Provisioning prov,
                                  String targetType, TargetBy targetBy, String target,
                                  String granteeType, GranteeBy granteeBy, String grantee,
                                  String right, boolean deny) throws ServiceException {
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.fromCode(granteeType);
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
        
        // right
        Right r = RightManager.getInstance().getRight(right);
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        ZimbraACE ace = new ZimbraACE(granteeEntry.getId(), gt, r, deny, null);
        aces.add(ace);
        
        RightUtil.grantRight(prov, targetEntry, aces);
    }

    public static void revokeRight(Provisioning prov,
                                   String targetType, TargetBy targetBy, String target,
                                   String granteeType, GranteeBy granteeBy, String grantee,
                                   String right, boolean deny) throws ServiceException {
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.fromCode(granteeType);
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
        
        // right
        Right r = RightManager.getInstance().getRight(right);
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        ZimbraACE ace = new ZimbraACE(granteeEntry.getId(), gt, r, deny, null);
        aces.add(ace);
        
        RightUtil.revokeRight(prov, targetEntry, aces);
    }

    public static Element rightToXML(Element parent, Right right) throws ServiceException {
        Element eRight = parent.addElement(AdminConstants.E_RIGHT);
        eRight.addAttribute(AdminConstants.E_NAME, right.getName());
        eRight.addAttribute(AdminConstants.A_TYPE, right.getRightType().name());
        eRight.addAttribute(AdminConstants.A_TARGET_TYPE, right.getTargetTypeStr());
        // todo defined by 
            
        eRight.addElement(AdminConstants.E_DESC).setText(right.getDesc());
            
        if (right.isPresetRight()) {
            // nothing to do here
        } else if (right.isAttrRight()) {
            Element eAttrs = eRight.addElement(AdminConstants.E_ATTRS);
            AttrRight attrRight = (AttrRight)right;
            
            if (attrRight.allAttrs()) {
                Set<String> attrs = attrRight.getAllAttrs();
                for (String attr : attrs)
                    eAttrs.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, attr);
            } else {
                for (AttrRight.Attr attr :attrRight.getAttrs())
                    eAttrs.addElement(attr.getAttrName());
            }
        } else if (right.isComboRight()) {
            Element eRights = eRight.addElement(AdminConstants.E_RIGHTS);
            ComboRight comboRight = (ComboRight)right;
            for (Right r : comboRight.getRights())
                eRights.addElement(AdminConstants.E_R).addAttribute(AdminConstants.A_N, r.getName());
        }

        return eRight;
    }

}
