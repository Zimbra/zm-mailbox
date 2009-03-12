/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;

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
         * called from SoapProvisioning/ProvUtil
         */
        public ACL(Element parent) throws ServiceException {
            for (Element eGrant : parent.listElements(AdminConstants.E_GRANT)) {
                String granteeType = eGrant.getAttribute(AdminConstants.A_TYPE);
                String granteeId = eGrant.getAttribute(AdminConstants.A_ID);
                String granteeName = eGrant.getAttribute(AdminConstants.A_NAME);
                String right = eGrant.getAttribute(AdminConstants.A_RIGHT);
                
                boolean deny = eGrant.getAttributeBool(AdminConstants.A_DENY, false);
                boolean canDelegate = eGrant.getAttributeBool(AdminConstants.A_CAN_DELEGATE, false);
                
                RightModifier rightModifier = null;
                
                /*
                 * only one of deny/canDelegate can be true
                 */
                if (deny)
                    rightModifier = RightModifier.RM_DENY;
                else if (canDelegate)
                    rightModifier = RightModifier.RM_CAN_DELEGATE;
                
                ACE ace = new ACE(granteeType, granteeId, granteeName, right, rightModifier);
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
                
                RightModifier rightModifier = ace.rightModifier();
                boolean deny = (rightModifier == RightModifier.RM_DENY);
                boolean canDelegate = (rightModifier == RightModifier.RM_CAN_DELEGATE);
                eGrant.addAttribute(AdminConstants.A_DENY, deny);
                eGrant.addAttribute(AdminConstants.A_CAN_DELEGATE, canDelegate);
            }
        }
    }
    
    public static class ACE {
        String mGranteeType;
        String mGranteeId;
        String mGranteeName;
        String mRight;
        RightModifier mRightModifier;
    
        /*
         * called from CLI
         */
        private ACE(String granteeType,
            String granteeId,
            String granteeName,
            String right,
            RightModifier rightModifier) {
            
            mGranteeType = granteeType;
            mGranteeId = granteeId;
            mGranteeName = granteeName;
            mRight = right;
            mRightModifier = rightModifier;
        }
        
        /*
         * called in server
         */
        private ACE(ZimbraACE ace) {
            mGranteeType = ace.getGranteeType().getCode();
            mGranteeId = ace.getGrantee();
            mGranteeName = ace.getGranteeDisplayName();
            mRight = ace.getRight().getName();
            mRightModifier = ace.getRightModifier();
        }
        
        public String granteeType() { return mGranteeType; }
        public String granteeId()   { return mGranteeId; }
        public String granteeName() { return mGranteeName; }
        public String right()       { return mRight; }
        public RightModifier rightModifier()       { return mRightModifier; }
    }
    
    public static class EffectiveAttr {
        private static final Set<String> EMPTY_SET = new HashSet<String>();
        
        String mAttrName;
        Set<String> mDefault;
        AttributeConstraint mConstraint;
        
        EffectiveAttr(String attrName, Set<String> defaultValue, AttributeConstraint constraint) {
            mAttrName = attrName;
            mDefault = defaultValue;
            mConstraint = constraint;
        }
        
        public String getAttrName() { return mAttrName; }
        
        public Set<String> getDefault()  { 
            if (mDefault == null)
                return EMPTY_SET;
            else
                return mDefault; 
        }
        
        AttributeConstraint getConstraint() {
            return mConstraint;
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
        
        public EffectiveRights(Element parent, boolean isCreateObjectAttrs) throws ServiceException {
            if (isCreateObjectAttrs)
                fromXML_CreateObjectAttrs(parent);
            else
                fromXML_EffectiveRights(parent);
        }
        
        private void fromXML_EffectiveRights(Element parent) throws ServiceException {
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
        
        private void fromXML_CreateObjectAttrs(Element parent) throws ServiceException {
            // setAttrs
            Element eSetAttrs = parent.getElement(AdminConstants.E_SET_ATTRS);
            if (eSetAttrs.getAttributeBool(AdminConstants.A_ALL, false))
                mCanSetAllAttrs = true;
            
            mCanSetAttrs = fromXML(eSetAttrs);
        }
        
        private TreeMap<String, EffectiveAttr> fromXML(Element eAttrs) throws ServiceException {
            TreeMap<String, EffectiveAttr> attrs = new TreeMap<String, EffectiveAttr>();
            
            AttributeManager am = AttributeManager.getInstance();
            
            for (Element eAttr : eAttrs.listElements(AdminConstants.E_A)) {
                String attrName = eAttr.getAttribute(AdminConstants.A_N);
                
                // constraint
                Element eConstraint = eAttr.getOptionalElement(AdminConstants.E_CONSTRAINT);
                AttributeConstraint constraint = null;
                if (eConstraint != null) {
                    constraint = AttributeConstraint.newConstratint(am, attrName);
                    
                    Element eMin = eConstraint.getOptionalElement(AdminConstants.E_MIN);
                    if (eMin != null)
                        constraint.setMin(eMin.getText());
                        
                    Element eMax = eConstraint.getOptionalElement(AdminConstants.E_MAX);
                    if (eMax != null)
                        constraint.setMax(eMin.getText());
                    
                    Element eValues = eConstraint.getOptionalElement(AdminConstants.E_VALUES);
                    if (eValues != null) {
                        for (Element eValue : eValues.listElements(AdminConstants.E_VALUE))
                            constraint.addValue(eValue.getText());
                    }
                }
                
                // default
                Element eDefault = eAttr.getOptionalElement(AdminConstants.E_DEFAULT);
                Set<String> defaultValues = null;
                if (eDefault != null) {
                    defaultValues = new HashSet<String>();
                    for (Element eValue : eDefault.listElements(AdminConstants.E_VALUE)) {
                        defaultValues.add(eValue.getText());
                    }
                }
                
                EffectiveAttr ea = new EffectiveAttr(attrName, defaultValues, null);  // TODO, constraint
                attrs.put(attrName, ea);
            }
            
            return attrs;
        }
        
        public void toXML_getEffectiveRights(Element parent) {
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
        
        public void toXML_getCreateObjectAttrs(Element parent) {
            
            // setAttrs
            toXML(parent, AdminConstants.E_SET_ATTRS, mCanSetAllAttrs, mCanSetAttrs);
            
        }
        
        private void toXML(Element parent, String elemName, boolean allAttrs, SortedMap<String, EffectiveAttr> attrs) {
            Element eAttrs = parent.addElement(elemName);
            if (allAttrs) {
                eAttrs.addAttribute(AdminConstants.A_ALL, true);
            }
               
            for (EffectiveAttr ea : attrs.values()) {
                Element eAttr = eAttrs.addElement(AdminConstants.E_A);
                eAttr.addAttribute(AdminConstants.A_N, ea.getAttrName());
                
                // constraint
                AttributeConstraint constraint = ea.getConstraint();
                if (constraint != null) {
                    Element eConstraint = eAttr.addElement(AdminConstants.E_CONSTRAINT);
                    
                    String min = constraint.getMin();
                    if (min != null)
                        eConstraint.addElement(AdminConstants.E_MIN).setText(min);
                    
                    String max = constraint.getMax();
                    if (max != null)
                        eConstraint.addElement(AdminConstants.E_MAX).setText(max);
                    
                    Set<String> values = constraint.getValues();
                    if (values != null) {
                        Element eValues = eConstraint.addElement(AdminConstants.E_VALUES);
                        for (String v : values)
                            eValues.addElement(AdminConstants.E_VALUE).setText(v);
                    }
                        
                }
                
                // default
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
    
    public static Right getRight(String rightName) throws ServiceException {
        verifyAccessManager();
        return RightManager.getInstance().getRight(rightName);
    }
    
    /*
     * master switch to disable non-stable code
     * return false before check into p4
     * 
     * remove when all is well
     * 
     * TODOs:
     *
     * 3. family mailbox (currently in DomainAccessManager, move to AccessManager so can apply to non domain based AM)
     *
     * 4. check all callsites of AuthToken.isAdmin() to call AccessManager.isGlobalAdmin()
     * 
     * 6. domain admin rights     
     * 7. check all rights in zimbra-rights.xml ar used or still need to be used
     *   
     * 7. cross domain right
     * 8. grant the same negative grants of the granter to the grantee
     * 
     * For domain admin login:
     * 
     * zmprov grr config usr da@test.com getGlobalConfig (???)
     * zmprov grr domain test.com usr da@test.com domainAdminRights
     * 
     */
    private static boolean READY() {
        return true;
    }
    
    private static void verifyAccessManager() throws ServiceException {
        if (!READY())
            return;
        
        if (!(AccessManager.getInstance() instanceof ACLAccessManager))
            throw ServiceException.FAILURE("method is not supported by the current AccessManager: " + 
                    AccessManager.getInstance().getClass().getCanonicalName() +
                    ", check localonfig key " + LC.zimbra_class_accessmanager.key() + 
                    ", this method requires " +  ACLAccessManager.class.getCanonicalName(), null);
    }
    
    /**
     * return rights that can be granted on target with the specified targetType
     *     e.g. renameAccount can be granted on a domain, a distribution list, or an account target
     *     
     * Note: this is *not* the same as "rights executable on targetType"
     *     e.g. renameAccount is executable on account entries. 
     * 
     * @param targetType
     * @return
     * @throws ServiceException
     */
    public static List<Right> getAllRights(String targetType) throws ServiceException {
        verifyAccessManager();
        
        Map<String, AdminRight> allRights = RightManager.getInstance().getAllAdminRights();
        
        List<Right> rights = new ArrayList<Right>();
        
        TargetType tt = (targetType==null)? null : TargetType.fromString(targetType);
        for (Map.Entry<String, AdminRight> right : allRights.entrySet()) {
            Right r = right.getValue();
            if (tt == null || r.grantableOnTargetType(tt)) {
                rights.add(r);
            }
        }
        return rights;
    }
    
    public static boolean checkRight(Provisioning prov,
                                     String targetType, TargetBy targetBy, String target,
                                     GranteeBy granteeBy, String grantee,
                                     String right, Map<String, Object> attrs,
                                     AccessManager.ViaGrant via) throws ServiceException {
        verifyAccessManager();
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.GT_USER;  // grantee for check right must be an Account
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);  
        
        // right
        Right r = RightManager.getInstance().getRight(right);
        
        if (r.getRightType() == Right.RightType.setAttrs) {
            /*
            if (attrs == null || attrs.isEmpty())
                throw ServiceException.INVALID_REQUEST("attr map is required for checking a setAttrs right: " + r.getName(), null);
            */
        } else {
            if (attrs != null && !attrs.isEmpty())
                throw ServiceException.INVALID_REQUEST("attr map is not allowed for checking a non-setAttrs right: " + r.getName(), null);
        }
        
        AccessManager am = AccessManager.getInstance();
        return am.canPerform((Account)granteeEntry, targetEntry, r, false, attrs, true, via);
    }
    
    public static EffectiveRights getEffectiveRights(Provisioning prov,
                                                     String targetType, TargetBy targetBy, String target,
                                                     GranteeBy granteeBy, String grantee,
                                                     boolean expandSetAttrs, boolean expandGetAttrs) throws ServiceException {
        verifyAccessManager();
        
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
    
    public static EffectiveRights getCreateObjectAttrs(Provisioning prov,
                                                       String targetType,
                                                       DomainBy domainBy, String domainStr,
                                                       CosBy cosBy, String cosStr,
                                                       GranteeBy granteeBy, String grantee) throws ServiceException {
        
        verifyAccessManager();
        
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = RightChecker.createPseudoTarget(prov, tt, domainBy, domainStr, cosBy, cosStr);
       
        // grantee
        GranteeType gt = GranteeType.GT_USER;
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);  
        // granteeEntry right must be an Account
        Account granteeAcct = (Account)granteeEntry;
        
        String targetId = (targetEntry instanceof NamedEntry)? ((NamedEntry)targetEntry).getId() : "";
        EffectiveRights er = new EffectiveRights(targetType, targetId, targetEntry.getLabel(), granteeAcct.getId(), granteeAcct.getName());
        
        RightChecker.getEffectiveRights(granteeAcct, targetEntry, true, true, er);
        return er;
    }
    
    public static ACL getGrants(Provisioning prov,
                                String targetType, TargetBy targetBy, String target) throws ServiceException {
        verifyAccessManager();
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        ZimbraACL zimbraAcl = RightUtil.getACL(targetEntry);
        return new ACL(zimbraAcl);
    }
    
    private static void verifyGrant(Account authedAcct,
                                    TargetType targetType, Entry targetEntry,
                                    GranteeType granteeType, NamedEntry granteeEntry,
                                    Right right, 
                                    boolean revoking) throws ServiceException {
        
        // TODO: currently user right does not go through RightCommand, it goes 
        //       directly to RightUtil, need to fix for the target type check.
        //       should change the mail GrantPermission to call RightCommand or provisioning
        //       method after we switch to the new access manager(the canPerform method is 
        //       not supported in the current access manager).   or should we?
        if (!right.isUserRight()) {
            verifyAccessManager();
            
            /*
             * check if the grantee is an admin account or admin group
             * 
             * If we are revoking, skip this check, just let the revoke through.  
             * The grantee could have been taken away the admin privilege.
             */ 
            if (!revoking) {
                boolean isCDARight = CrossDomain.validateCrossDomainAdminGrant(right, granteeType);
                if (!isCDARight &&
                    !RightChecker.isValidGranteeForAdminRights(granteeType, granteeEntry))
                    throw ServiceException.INVALID_REQUEST("grantee must be an admin account or group", null);
            }
        }
        
        /*
         * check if the right can be granted on the target type
         */
        if (!right.grantableOnTargetType(targetType))
            throw ServiceException.INVALID_REQUEST(
                    "right " + right.getName() + 
                    " cannot be granted on a " + targetType.getCode() + " entry. " +
                    "It can only be granted on target types: " + right.reportGrantableTargetTypes(), null);
        
        /*
         * check if the authed account can grant this right on this target
         * 
         * a grantor can only delegate the whole or part of his rights on the 
         * same target or a subset of target on which the grantors own rights 
         * were granted.
         * 
         * if authedAcct==null, the call site is LdapProvisioning, treat it as a 
         * system admin and skip this check.
         */
        if (!READY())
            return;
        
        if (authedAcct != null) {
            AccessManager am = AccessManager.getInstance();
            boolean canGrant = am.canPerform((Account)authedAcct, targetEntry, right, true, null, true, null);
            if (!canGrant)
                throw ServiceException.PERM_DENIED("insuffcient right to grant");
        }
    }
            
    public static void grantRight(Provisioning prov,
                                  Account authedAcct,
                                  String targetType, TargetBy targetBy, String target,
                                  String granteeType, GranteeBy granteeBy, String grantee,
                                  String right, RightModifier rightModifier) throws ServiceException {
        
        verifyAccessManager();
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.fromCode(granteeType);
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
        
        // right
        Right r = RightManager.getInstance().getRight(right);
        
        verifyGrant(authedAcct, tt, targetEntry, gt, granteeEntry, r, false);
        
        /*
         * TODO: grant the same negative right on sub targets!!!
         */
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        ZimbraACE ace = new ZimbraACE(granteeEntry.getId(), gt, r, rightModifier, null);
        aces.add(ace);
        
        RightUtil.grantRight(prov, targetEntry, aces);
    }

    public static void revokeRight(Provisioning prov,
                                   Account authedAcct,
                                   String targetType, TargetBy targetBy, String target,
                                   String granteeType, GranteeBy granteeBy, String grantee,
                                   String right, RightModifier rightModifier) throws ServiceException {
        
        verifyAccessManager();
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.fromCode(granteeType);
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
        
        // right
        Right r = RightManager.getInstance().getRight(right);
        
        verifyGrant(authedAcct, tt, targetEntry, gt, granteeEntry, r, true);
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        ZimbraACE ace = new ZimbraACE(granteeEntry.getId(), gt, r, rightModifier, null);
        aces.add(ace);
        
        List<ZimbraACE> revoked = RightUtil.revokeRight(prov, targetEntry, aces);
        if (revoked.isEmpty())
            throw AccountServiceException.NO_SUCH_GRANT(ace.dump());
    }

    public static Element rightToXML(Element parent, Right right, boolean expandAllAtrts) throws ServiceException {
        Element eRight = parent.addElement(AdminConstants.E_RIGHT);
        eRight.addAttribute(AdminConstants.E_NAME, right.getName());
        eRight.addAttribute(AdminConstants.A_TYPE, right.getRightType().name());
        eRight.addAttribute(AdminConstants.A_TARGET_TYPE, right.getTargetTypeStr());
            
        eRight.addElement(AdminConstants.E_DESC).setText(right.getDesc());
            
        if (right.isPresetRight()) {
            // nothing to do here
        } else if (right.isAttrRight()) {
            Element eAttrs = eRight.addElement(AdminConstants.E_ATTRS);
            AttrRight attrRight = (AttrRight)right;
            
            if (attrRight.allAttrs()) {
                eAttrs.addAttribute(AdminConstants.A_ALL, true);
                if (expandAllAtrts) {
                    Set<String> attrs = attrRight.getAllAttrs();
                    for (String attr : attrs)
                        eAttrs.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, attr);
                }
            } else {
                for (String attrName :attrRight.getAttrs())
                    eAttrs.addElement(attrName);
            }
        } else if (right.isComboRight()) {
            Element eRights = eRight.addElement(AdminConstants.E_RIGHTS);
            ComboRight comboRight = (ComboRight)right;
            for (Right r : comboRight.getRights())
                eRights.addElement(AdminConstants.E_R).addAttribute(AdminConstants.A_N, r.getName());
        }

        return eRight;
    }
    
    /*
     * Hack.  We do *not* parse the SOAP response.   Instead we just get the right from 
     * right manager.  The Right object is only used by zmprov, not by generic SOAP clients. 
     */
    public static Right XMLToRight(Element eRight) throws ServiceException  {
        String rightName = eRight.getAttribute(AdminConstants.E_NAME);
        return RightManager.getInstance().getRight(rightName);
    }

}
