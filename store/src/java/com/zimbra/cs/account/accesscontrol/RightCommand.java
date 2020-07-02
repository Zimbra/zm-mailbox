/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.Right.RightType;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;
import com.zimbra.cs.account.accesscontrol.SearchGrants.GrantsOnTarget;
import com.zimbra.soap.admin.message.GetAllEffectiveRightsResponse;
import com.zimbra.soap.admin.message.GetEffectiveRightsResponse;
import com.zimbra.soap.admin.type.EffectiveAttrInfo;
import com.zimbra.soap.admin.type.EffectiveAttrsInfo;
import com.zimbra.soap.admin.type.EffectiveRightsInfo;
import com.zimbra.soap.admin.type.EffectiveRightsTarget;
import com.zimbra.soap.admin.type.EffectiveRightsTargetInfo;
import com.zimbra.soap.admin.type.EffectiveRightsTargetSelector;
import com.zimbra.soap.admin.type.GranteeInfo;
import com.zimbra.soap.admin.type.GranteeSelector;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.admin.type.InDomainInfo;
import com.zimbra.soap.admin.type.RightWithName;
import com.zimbra.soap.admin.type.RightsEntriesInfo;
import com.zimbra.soap.type.NamedElement;
import com.zimbra.soap.type.TargetBy;

public class RightCommand {

    /*
     * Grants and ACE are aux classes for ProvUtil.
     *
     * Use String instead of TargetType/GranteeType/Right data members in those classes
     * so they can be readily displayed/serialize without further dependency of any server
     * side logic, e.g. RightManager, which would access LDAP for custom rights that are
     * defined in LDAP.
     */

    public static class Grants {
        Set<ACE> mACEs = new HashSet<ACE>();

        Grants() {
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
        public Grants(Element parent) throws ServiceException {
            for (Element eGrant : parent.listElements(AdminConstants.E_GRANT)) {
                // target
                Element eTarget = eGrant.getElement(AdminConstants.E_TARGET);
                String targetType = eTarget.getAttribute(AdminConstants.A_TYPE, "");
                String targetId = eTarget.getAttribute(AdminConstants.A_ID, "");
                String targetName = eTarget.getAttribute(AdminConstants.A_NAME, "");

                // grantee
                Element eGrantee = eGrant.getElement(AdminConstants.E_GRANTEE);
                String granteeType = eGrantee.getAttribute(AdminConstants.A_TYPE, "");
                String granteeId = eGrantee.getAttribute(AdminConstants.A_ID, "");
                String granteeName = eGrantee.getAttribute(AdminConstants.A_NAME, "");

                // right
                Element eRight = eGrant.getElement(AdminConstants.E_RIGHT);
                String right = eRight.getText();
                boolean deny = eRight.getAttributeBool(AdminConstants.A_DENY, false);
                boolean canDelegate = eRight.getAttributeBool(AdminConstants.A_CAN_DELEGATE, false);
                boolean disinheritSubGroups = eRight.getAttributeBool(AdminConstants.A_DISINHERIT_SUB_GROUPS, false);
                boolean subDomain = eRight.getAttributeBool(AdminConstants.A_SUB_DOMAIN, false);

                RightModifier rightModifier = null;

                /*
                 * only one of deny/canDelegate/disinheritSubGroups/subDomain can be true
                 */
                if (deny)
                    rightModifier = RightModifier.RM_DENY;
                else if (canDelegate)
                    rightModifier = RightModifier.RM_CAN_DELEGATE;
                else if (disinheritSubGroups)
                    rightModifier = RightModifier.RM_DISINHERIT_SUB_GROUPS;
                else if (subDomain)
                    rightModifier = RightModifier.RM_SUBDOMAIN;

                ACE ace = new ACE(targetType, targetId, targetName,
                        granteeType, granteeId, granteeName,
                        right, rightModifier);
                addACE(ace);
            }
        }

        /*
         * add grants from a ZimbraACL
         * called in server
         */
        private void addGrants(TargetType targetType, Entry target, ZimbraACL acl,
                Set<String> granteeFilter, Boolean isGranteeAnAdmin) {
            if (acl == null)
                return;

            for (ZimbraACE ace : acl.getAllACEs()) {
                boolean isAdminRight = !ace.getRight().isUserRight();

                if (isAdminRight && Boolean.FALSE == isGranteeAnAdmin) {
                    continue;
                }

                if (granteeFilter == null || granteeFilter.contains(ace.getGrantee())) {
                    addACE(new ACE(targetType, target, ace));
                }
            }
        }


        public void toXML(Element parent) {

            for (ACE ace : mACEs) {
                /*
                 * for backward compatibility
                 *
                 * <grant type="{grantee-type}" id="{grantee-id}" name="{grantee-name}"
                 *        right="{right-name}" deny="{deny}" canDelegate="{canDelegate}"/>
                 */
                Element eGrant = parent.addNonUniqueElement(AdminConstants.E_GRANT);

                RightModifier rightModifier = ace.rightModifier();
                boolean deny = (rightModifier == RightModifier.RM_DENY);
                boolean canDelegate = (rightModifier == RightModifier.RM_CAN_DELEGATE);
                boolean disinheritSubGroups = (rightModifier == RightModifier.RM_DISINHERIT_SUB_GROUPS);
                boolean subDomain = (rightModifier == RightModifier.RM_SUBDOMAIN);

                /*
                 * new format:
                 *
                 * <grant>
                 *   <target type={target-type} by="{target-by}">{target-name-or-id}</target>
                 *   <grantee type={grantee-type} by="{grantee-by}">{grantee-name-or-id}</grantee>
                 *   <right [deny="${deny}"] [canDelegate="${canDelegate}"]>{right}</right>
                 * </grant>
                 */
                Element eTarget = eGrant.addNonUniqueElement(AdminConstants.E_TARGET);
                eTarget.addAttribute(AdminConstants.A_TYPE, ace.targetType());
                eTarget.addAttribute(AdminConstants.A_ID, ace.targetId());
                eTarget.addAttribute(AdminConstants.A_NAME, ace.targetName());

                Element eGrantee = eGrant.addNonUniqueElement(AdminConstants.E_GRANTEE);
                eGrantee.addAttribute(AdminConstants.A_TYPE, ace.granteeType());
                eGrantee.addAttribute(AdminConstants.A_ID, ace.granteeId());
                eGrantee.addAttribute(AdminConstants.A_NAME, ace.granteeName());

                Element eRight = eGrant.addNonUniqueElement(AdminConstants.E_RIGHT);
                eRight.addAttribute(AdminConstants.A_DENY, deny);
                eRight.addAttribute(AdminConstants.A_CAN_DELEGATE, canDelegate);
                eRight.addAttribute(AdminConstants.A_DISINHERIT_SUB_GROUPS, disinheritSubGroups);
                eRight.addAttribute(AdminConstants.A_SUB_DOMAIN, subDomain);
                eRight.setText(ace.right());
            }
        }
    }

    public static class ACE {
        private final String mTargetType;
        private final String mTargetId;
        private final String mTargetName;
        private final String mGranteeType;
        private final String mGranteeId;
        private final String mGranteeName;
        private final String mRight;
        private final RightModifier mRightModifier;

        /*
         * called from CLI
         */
        private ACE(String targetType, String targetId, String targetName,
            String granteeType, String granteeId, String granteeName,
            String right, RightModifier rightModifier) {

            mTargetType = targetType;
            mTargetId = targetId;
            mTargetName = targetName;
            mGranteeType = granteeType;
            mGranteeId = granteeId;
            mGranteeName = granteeName;
            mRight = right;
            mRightModifier = rightModifier;
        }

        /*
         * called in server
         */
        private ACE(TargetType targetType, Entry target, ZimbraACE ace) {
            mTargetType = targetType.getCode();
            mTargetId = TargetType.getId(target);
            mTargetName = target.getLabel();
            mGranteeType = ace.getGranteeType().getCode();
            mGranteeId = ace.getGrantee();
            mGranteeName = ace.getGranteeDisplayName();
            mRight = ace.getRight().getName();
            mRightModifier = ace.getRightModifier();
        }

        public String targetType() { return mTargetType; }
        public String targetId()   { return (mTargetId!=null)?mTargetId:""; }
        public String targetName() { return mTargetName; }
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
        private static final SortedMap<String, EffectiveAttr>
            EMPTY_MAP = new TreeMap<String, EffectiveAttr>();

        String mTargetType;
        String mTargetId;
        String mTargetName;
        String mGranteeId;
        String mGranteeName;

        String mDigest;

        // preset
        List<String> mPresetRights = new ArrayList<String>();       // sorted by right name

        // setAttrs
        boolean mCanSetAllAttrs = false;
        SortedMap<String, EffectiveAttr> mCanSetAttrs = EMPTY_MAP;  // sorted by attr name

        // getAttrs
        boolean mCanGetAllAttrs = false;
        SortedMap<String, EffectiveAttr> mCanGetAttrs = EMPTY_MAP;  // sorted by attr name

        EffectiveRights(String targetType, String targetId, String targetName,
                String granteeId, String granteeName) {
            mTargetType = targetType;
            mTargetId = targetId==null ? "" : targetId;
            mTargetName = targetName;
            mGranteeId = granteeId;
            mGranteeName = granteeName;
        }

        private EffectiveRights() {
        }

        private boolean hasSameRights(EffectiveRights other) {
            return getDigest().equals(other.getDigest());
        }

        private boolean hasNoRight() {
            return (mPresetRights.isEmpty() &&
                    (!mCanSetAllAttrs && mCanSetAttrs.isEmpty()) &&
                    (!mCanGetAllAttrs && mCanGetAttrs.isEmpty()));
        }

        /*
         * digest is in the format of:
         *
         * preset:{hash-code-of-mPresetRights};setAttrs:all|{hash-code-of-key-list-of-mCanSetAttrs};getAttrs:all|{hash-code-of-key-list-of-mCanGetAttrs}
         *
         * Note: for set/get attrs rights, defaults and constraints(i.e. data in EffectiveAttr) are not included in the computation.
         * As long as the attr list are equal, the two rights are consider equal.
         */
        private String getDigest() {
            if (mDigest != null)
                return mDigest;

            StringBuilder rights = new StringBuilder();

            // preset rights
            rights.append("preset:" + mPresetRights.hashCode() + ";");

            // setAttrs rights
            rights.append("setAttrs:");
            if (mCanSetAllAttrs)
                rights.append("all;");
            else {
                List<String> attrs = new ArrayList<String>(mCanSetAttrs.keySet());
                rights.append(attrs.hashCode() + ";");
            }

            // getAttrs rights
            rights.append("getAttrs:");
            if (mCanGetAllAttrs)
                rights.append("all;");
            else {
                List<String> attrs = new ArrayList<String>(mCanGetAttrs.keySet());
                rights.append(attrs.hashCode() + ";");
            }

            mDigest = rights.toString();
            return mDigest;
        }

        public static EffectiveRights fromJaxb_EffectiveRights(
                GetEffectiveRightsResponse resp)
        throws ServiceException {
            EffectiveRights er = fromJaxb(resp.getTarget());
            er.mGranteeId = resp.getGrantee().getId();
            er.mGranteeName= resp.getGrantee().getName();
            return er;
        }

        public static EffectiveRights fromXML_CreateObjectAttrs(Element parent)
        throws ServiceException {
            EffectiveRights er = new EffectiveRights();

            // setAttrs
            Element eSetAttrs = parent.getElement(AdminConstants.E_SET_ATTRS);
            if (eSetAttrs.getAttributeBool(AdminConstants.A_ALL, false))
                er.mCanSetAllAttrs = true;

            er.mCanSetAttrs = fromXML_attrs(eSetAttrs);

            return er;
        }

        private static EffectiveRights fromJaxb(EffectiveRightsInfo eRights)
        throws ServiceException {
            EffectiveRights er = new EffectiveRights();

            // preset rights
            er.mPresetRights = new ArrayList<String>();
            for (RightWithName eRight : eRights.getRights())
                er.mPresetRights.add(eRight.getName());

            // setAttrs
            EffectiveAttrsInfo eSetAttrs = eRights.getSetAttrs();
            Boolean setAll = eSetAttrs.getAll();
            er.mCanSetAllAttrs = (setAll != null && setAll);
            er.mCanSetAttrs = from_attrs(eSetAttrs);

            // getAttrs
            EffectiveAttrsInfo eGetAttrs = eRights.getGetAttrs();
            Boolean getAll = eGetAttrs.getAll();
            er.mCanGetAllAttrs = (getAll != null && getAll);
            er.mCanGetAttrs = from_attrs(eGetAttrs);
            if (eRights instanceof EffectiveRightsTargetInfo) {
                EffectiveRightsTargetInfo eTargInfo =
                    (EffectiveRightsTargetInfo) eRights;
                er.mTargetType = eTargInfo.getType().toString();
                er.mTargetId = eTargInfo.getId();
                er.mTargetName= eTargInfo.getName();
            }
            return er;
        }

        private static TreeMap<String, EffectiveAttr> from_attrs(
                EffectiveAttrsInfo eAttrs)
        throws ServiceException {
            TreeMap<String, EffectiveAttr> attrs = new TreeMap<String, EffectiveAttr>();
            AttributeManager am = AttributeManager.getInstance();

            for (EffectiveAttrInfo eAttr : eAttrs.getAttrs()) {
                String attrName = eAttr.getName();

                // constraints
                AttributeConstraint constraint =
                    AttributeConstraint.fromJaxb(am, attrName,
                        eAttr.getConstraint());

                // default
                Set<String> defaultValues = null;
                List<String> values = eAttr.getValues();
                if ((values != null) && (values.size() > 0)) {
                    defaultValues = Sets.newHashSet();
                    defaultValues.addAll(values);
                }
                // TODO: leaving 3rd arg as null rather than constraint.
                //       This is same handling as in fromXML_attrs which
                //       performed the same function before.
                EffectiveAttr ea = new EffectiveAttr(attrName, defaultValues,
                        null);
                attrs.put(attrName, ea);
            }
            return attrs;
        }

        private static TreeMap<String, EffectiveAttr> fromXML_attrs(Element eAttrs)
        throws ServiceException {
            TreeMap<String, EffectiveAttr> attrs = new TreeMap<String, EffectiveAttr>();

            AttributeManager am = AttributeManager.getInstance();

            for (Element eAttr : eAttrs.listElements(AdminConstants.E_A)) {
                String attrName = eAttr.getAttribute(AdminConstants.A_N);

                // constraints
                AttributeConstraint constraint = null;
                Element eConstraint = eAttr.getOptionalElement(AdminConstants.E_CONSTRAINT);
                if (eConstraint != null)
                    constraint = AttributeConstraint.fromXML(am, attrName, eConstraint);

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
            Element eGrantee = parent.addNonUniqueElement(AdminConstants.E_GRANTEE);
            eGrantee.addAttribute(AdminConstants.A_ID, mGranteeId);
            eGrantee.addAttribute(AdminConstants.A_NAME, mGranteeName);

            //
            // target
            //
            Element eTarget = parent.addNonUniqueElement(AdminConstants.E_TARGET);
            eTarget.addAttribute(AdminConstants.A_TYPE, mTargetType);
            eTarget.addAttribute(AdminConstants.A_ID, mTargetId);
            eTarget.addAttribute(AdminConstants.A_NAME, mTargetName);

            toXML(eTarget);
        }

        public void toXML_getCreateObjectAttrs(Element parent) {
            // setAttrs
            toXML(parent, AdminConstants.E_SET_ATTRS, mCanSetAllAttrs, mCanSetAttrs);
        }

        private void toXML(Element eParent) {
            // preset rights
            for (String r : mPresetRights) {
                eParent.addNonUniqueElement(AdminConstants.E_RIGHT).addAttribute(AdminConstants.A_N, r);
            }

            // setAttrs
            toXML(eParent, AdminConstants.E_SET_ATTRS, mCanSetAllAttrs, mCanSetAttrs);

            // getAttrs
            toXML(eParent, AdminConstants.E_GET_ATTRS, mCanGetAllAttrs, mCanGetAttrs);
        }

        private void toXML(Element parent, String elemName, boolean allAttrs,
                SortedMap<String, EffectiveAttr> attrs) {
            Element eAttrs = parent.addNonUniqueElement(elemName);
            if (allAttrs) {
                eAttrs.addAttribute(AdminConstants.A_ALL, true);
            }

            for (EffectiveAttr ea : attrs.values()) {
                Element eAttr = eAttrs.addNonUniqueElement(AdminConstants.E_A);
                String attrName =  ea.getAttrName();
                eAttr.addAttribute(AdminConstants.A_N, attrName);

                // constraint
                AttributeConstraint constraint = ea.getConstraint();
                if (constraint != null)
                    constraint.toXML(eAttr);

                // default
                if (!ea.getDefault().isEmpty()) {
                    Element eDefault = eAttr.addNonUniqueElement(AdminConstants.E_DEFAULT);
                    for (String v : ea.getDefault()) {
                        Element valueElem = eDefault.addNonUniqueElement(AdminConstants.E_VALUE /* v */);
                        valueElem.setText(Provisioning.sanitizedAttrValue(attrName, v).toString());
                    }
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

    /*
     * an assembly of target entries on which a grantee bear the same set of rights
     *
     * e.g. account-1, account-2, account-3: right A, B, C
     */
    public static class RightAggregation {
        // target names
        Set<String> mEntries;

        // effective rights
        EffectiveRights mRights;

        public Set<String> entries() { return mEntries; }
        public EffectiveRights effectiveRights() { return mRights; }

        private RightAggregation(String name, EffectiveRights rights) {
            mEntries = new HashSet<String>();
            mEntries.add(name);
            mRights = rights;
        }

        private RightAggregation(Set<String> names, EffectiveRights rights) {
            mEntries = new HashSet<String>();
            mEntries.addAll(names);
            mRights = rights;
        }

        private EffectiveRights getRights() {
            return mRights;
        }

        private void addEntry(String name) {
            mEntries.add(name);
        }

        private void addEntries(Set<String> names) {
            mEntries.addAll(names);
        }

        private boolean hasEntry(String name) {
            return mEntries.contains(name);
        }

        private void removeEntry(String name) {
            mEntries.remove(name);
        }

        private boolean hasSameRights(EffectiveRights er) {
            return mRights.hasSameRights(er);
        }
    }

    /*
     * aggregation of all effective rights executable on a target type
     */
    public static class RightsByTargetType {
        //
        // effective rights on all entries of a target type
        // e.g. rights A, B, C
        //
        EffectiveRights mAll = null;

        //
        // e.g. account-1, account-2, account-3: rights A
        //      account-4, account-5:            rights B
        //      account-6:                       rights X, Y
        //
        Set<RightAggregation> mEntries = new HashSet<RightAggregation>();

        public EffectiveRights all() { return mAll; }
        public Set<RightAggregation> entries() { return mEntries; }

        void setAll(EffectiveRights er) {
            mAll = er;
        }

        protected static void add(Set<RightAggregation> entries, String name, EffectiveRights er) {
            // if the entry is already in one of the RightAggregation, remove it
            for (RightAggregation ra : entries) {
                if (ra.hasEntry(name)) {
                    ra.removeEntry(name);
                    break;
                }
            }

            // add the entry to an aggregation if there is one with the same rights
            // otherwise create a new aggregation
            for (RightAggregation ra : entries) {
                if (ra.hasSameRights(er)) {
                    ra.addEntry(name);
                    return;
                }
            }
            entries.add(new RightAggregation(name, er));
        }

        protected static void addAggregation(Set<RightAggregation> entries,
                Set<String> names, EffectiveRights er) {

            // add the entry to an aggregation if there is one with the same rights
            // otherwise create a new aggregation
            for (RightAggregation ra : entries) {
                if (ra.hasSameRights(er)) {
                    ra.addEntries(names);
                    return;
                }
            }
            entries.add(new RightAggregation(names, er));
        }

        private void addEntry(String name, EffectiveRights er) {
            add(mEntries, name, er);
        }

        private void addAggregation(Set<String> names, EffectiveRights er) {
            addAggregation(mEntries, names, er);
        }

        public boolean hasNoRight() {
            return mAll == null && mEntries.isEmpty();
        }
    }

    /*
     * aggregation of all effective rights executable on a "domained"
     * (i.e. entries can be aggregated by a domain) target type:
     * account, calresource, dl, dynamic group
     *
     * e.g.
     *     all accounts: rights A, B, C
     *
     *     all accounts in domain-1, domain-2: rights A, B
     *     all accounts in domain-3:           rights B, C, D
     *
     *     account-1, account-2, account-3: rights A
     *     account-4, account-5:            rights B
     *     account-6:                       rights X, Y
     *
     */
    public static class DomainedRightsByTargetType extends RightsByTargetType {
        //
        // effective rights on all entries of a target type in a domain
        //
        // e.g. all accounts in domain-1, domain-2: rights A, B
        //      all accounts in domain-3:           rights B, C, D
        //
        Set<RightAggregation> mDomains = new HashSet<RightAggregation>();

        public Set<RightAggregation> domains() { return mDomains; }

        void addDomainEntry(String domainName, EffectiveRights er) {
            add(mDomains, domainName, er);
        }

        @Override
        public boolean hasNoRight() {
            return super.hasNoRight() && mDomains.isEmpty();
        }
    }

    public static class AllEffectiveRights {
        String mGranteeType;
        String mGranteeId;
        String mGranteeName;

        Map<TargetType, RightsByTargetType> mRightsByTargetType =
            new HashMap<TargetType, RightsByTargetType>();

        AllEffectiveRights(String granteeType, String granteeId, String granteeName) {
            mGranteeType = granteeType;
            mGranteeId = granteeId;
            mGranteeName = granteeName;

            for (TargetType tt : TargetType.values()) {
                if (tt.isDomained())
                    mRightsByTargetType.put(tt, new DomainedRightsByTargetType());
                else
                    mRightsByTargetType.put(tt, new RightsByTargetType());
            }
        }

        public String granteeType() { return mGranteeType; }

        public String granteeId() { return mGranteeId; }

        public String granteeName() { return mGranteeName; }

        public Map<TargetType, RightsByTargetType> rightsByTargetType() { return mRightsByTargetType; }

        void setAll(TargetType targetType, EffectiveRights er) {
            if (er.hasNoRight())
                return;
            mRightsByTargetType.get(targetType).setAll(er);
        }

        void addEntry(TargetType targetType, String name, EffectiveRights er) {
            if (er.hasNoRight())
                return;
            mRightsByTargetType.get(targetType).addEntry(name, er);
        }

        void addAggregation(TargetType targetType, Set<String> names, EffectiveRights er) {
            if (er.hasNoRight())
                return;
            mRightsByTargetType.get(targetType).addAggregation(names, er);
        }

        void addDomainEntry(TargetType targetType, String domainName, EffectiveRights er) {
            if (er.hasNoRight())
                return;
            DomainedRightsByTargetType drbtt =
                (DomainedRightsByTargetType)mRightsByTargetType.get(targetType);
            drbtt.addDomainEntry(domainName, er);
        }

        public static AllEffectiveRights fromJaxb(GetAllEffectiveRightsResponse resp)
        throws ServiceException {
            GranteeInfo grantee = resp.getGrantee();
            com.zimbra.soap.type.GranteeType gt = grantee.getType();
            String granteeType = (gt == null) ? null : gt.toString();

            AllEffectiveRights aer = new AllEffectiveRights(granteeType,
                    grantee.getId(), grantee.getName());

            for (EffectiveRightsTarget target : resp.getTargets()) {
                TargetType targetType = TargetType.fromCode(
                        target.getType().toString());
                RightsByTargetType rbtt =
                    aer.mRightsByTargetType.get(targetType);
                EffectiveRightsInfo allR = target.getAll();
                if (allR != null) {
                    rbtt.mAll = EffectiveRights.fromJaxb(allR);
                }

                if (rbtt instanceof DomainedRightsByTargetType) {
                    DomainedRightsByTargetType drbtt =
                        (DomainedRightsByTargetType)rbtt;
                    for (InDomainInfo inDomInfo : target.getInDomainLists()) {
                        Set<String> domains = new HashSet<String>();
                        for (NamedElement dom : inDomInfo.getDomains())
                            domains.add(dom.getName());
                        EffectiveRights er =
                            EffectiveRights.fromJaxb(inDomInfo.getRights());
                        RightAggregation ra = new RightAggregation(domains, er);
                        drbtt.mDomains.add(ra);
                    }
                }
                for (RightsEntriesInfo eEntries : target.getEntriesLists()) {
                    Set<String> entries = new HashSet<String>();
                    for (NamedElement eEntry : eEntries.getEntries())
                        entries.add(eEntry.getName());
                    EffectiveRights er = EffectiveRights.fromJaxb(eEntries.getRights());
                    RightAggregation ra = new RightAggregation(entries, er);
                    rbtt.mEntries.add(ra);
                }
            }
            return aer;
        }

        public void toXML(Element parent) {
            //
            // grantee
            //
            Element eGrantee = parent.addNonUniqueElement(AdminConstants.E_GRANTEE);
            eGrantee.addAttribute(AdminConstants.A_TYPE, mGranteeType);
            eGrantee.addAttribute(AdminConstants.A_ID, mGranteeId);
            eGrantee.addAttribute(AdminConstants.A_NAME, mGranteeName);

            //
            // rights by target type
            //
            for (Map.Entry<TargetType, RightsByTargetType> rightsByTargetType : mRightsByTargetType.entrySet()) {
                TargetType targetType = rightsByTargetType.getKey();
                RightsByTargetType rbtt = rightsByTargetType.getValue();

                Element eTarget = parent.addNonUniqueElement(AdminConstants.E_TARGET);
                eTarget.addAttribute(AdminConstants.A_TYPE, targetType.getCode());

                EffectiveRights er = rbtt.all();
                if (er != null) {
                    Element eAll = eTarget.addNonUniqueElement(AdminConstants.E_ALL);
                    er.toXML(eAll);
                }

                if (rbtt instanceof DomainedRightsByTargetType) {
                    DomainedRightsByTargetType domainedRights =
                        (RightCommand.DomainedRightsByTargetType)rbtt;

                    for (RightAggregation rightsByDomains : domainedRights.domains()) {
                        Element eInDomains = eTarget.addNonUniqueElement(AdminConstants.E_IN_DOMAINS);
                        for (String domain : rightsByDomains.entries()) {
                            Element eDomain = eInDomains.addNonUniqueElement(AdminConstants.E_DOMAIN);
                            eDomain.addAttribute(AdminConstants.A_NAME, domain);
                        }
                        Element eRights = eInDomains.addNonUniqueElement(AdminConstants.E_RIGHTS);
                        er = rightsByDomains.getRights();
                        er.toXML(eRights);
                    }
                }

                for (RightAggregation rightsByEntries : rbtt.entries()) {
                    Element eEntries = eTarget.addNonUniqueElement(AdminConstants.E_ENTRIES);
                    for (String entry : rightsByEntries.entries()) {
                        Element eEntry = eEntries.addNonUniqueElement(AdminConstants.E_ENTRY);
                        eEntry.addAttribute(AdminConstants.A_NAME, entry);
                    }
                    Element eRights = eEntries.addNonUniqueElement(AdminConstants.E_RIGHTS);
                    er = rightsByEntries.getRights();
                    er.toXML(eRights);
                }
            }
        }
    }


    private static void verifyAccessManager() throws ServiceException {
        if (!(AccessManager.getInstance() instanceof ACLAccessManager))
            throw ServiceException.FAILURE("method is not supported by the current AccessManager: " +
                    AccessManager.getInstance().getClass().getCanonicalName() +
                    ", this method requires access manager " +
                    ACLAccessManager.class.getCanonicalName(), null);
    }

    private static AdminConsoleCapable verifyAdminConsoleCapable() throws ServiceException {
        AccessManager am = AccessManager.getInstance();

        if (am instanceof AdminConsoleCapable)
            return (AdminConsoleCapable)am;
        else
            throw ServiceException.FAILURE("method is not supported by the current AccessManager: " +
                    AccessManager.getInstance().getClass().getCanonicalName() +
                    ", this method requires an admin console capable access manager", null);
    }

    public static Right getRight(String rightName) throws ServiceException {
        verifyAccessManager();
        return RightManager.getInstance().getRight(rightName);
    }

    /**
     * return rights that can be granted on target with the specified targetType
     *     e.g. renameAccount can be granted on a domain, a distribution list, or an
     *          account target
     *
     * Note: this is *not* the same as "rights executable on targetType"
     *     e.g. renameAccount is executable on account entries.
     *
     * @param targetType
     * @return
     * @throws ServiceException
     */
    public static List<Right> getAllRights(String targetType, String rightClass)
    throws ServiceException {
        verifyAccessManager();

        List<Right> result = new ArrayList<Right>();

        TargetType tt = (targetType==null)? null : TargetType.fromCode(targetType);
        RightClass rc = (rightClass==null)? RightClass.ADMIN : RightClass.fromString(rightClass);

        switch (rc) {
        case USER:
            getAllRights(tt, RightManager.getInstance().getAllUserRights(), result);
            break;
        case ALL:
            getAllRights(tt, RightManager.getInstance().getAllAdminRights(), result);
            getAllRights(tt, RightManager.getInstance().getAllUserRights(), result);
            break;
        case ADMIN:
        default:
            // returning only admin rights
            getAllRights(tt, RightManager.getInstance().getAllAdminRights(), result);
        }

        return result;
    }

    private static void getAllRights(TargetType targetType,
            Map<String, ? extends Right> rights, List<Right> result)
    throws ServiceException {

        for (Map.Entry<String, ? extends Right> right : rights.entrySet()) {
            Right r = right.getValue();
            if (targetType == null || r.grantableOnTargetType(targetType)) {
                result.add(r);
            }
        }
    }

    public static boolean checkRight(Provisioning prov, String targetType, TargetBy targetBy, String target,
            MailTarget grantee, String right, Map<String, Object> attrs, AccessManager.ViaGrant via)
    throws ServiceException {
        verifyAccessManager();

        // target
        TargetType tt = TargetType.fromCode(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);

        // right
        Right r = RightManager.getInstance().getRight(right);

        if (r.getRightType() == Right.RightType.setAttrs) {
            /*
            if (attrs == null || attrs.isEmpty())
                throw ServiceException.INVALID_REQUEST("attr map is required for checking a setAttrs right: " + r.getName(), null);
            */
        } else {
            if (attrs != null && !attrs.isEmpty())
                throw ServiceException.INVALID_REQUEST(
                        "attr map is not allowed for checking a non-setAttrs right: " + r.getName(), null);
        }

        AccessManager am = AccessManager.getInstance();

        // as admin if the grantee under testing is an admin account
        boolean asAdmin = (grantee instanceof Account) ? am.isAdequateAdminAccount((Account)grantee) : false;
        boolean result =  am.canPerform(grantee, targetEntry, r, false, attrs, asAdmin, via);
        return result;
    }

    /**
     * Particularly in the case of delegated administration, CollectAllEffectiveRights.collect() Stage1 can
     * be expensive.  This cache trades accuracy for speed, so provides a short term cache
     */
    private static final Cache<String, AllEffectiveRights> ALL_EFFECTIVE_RIGHTS_CACHE;
    private static final long MAX_CACHE_EXPIRY = 30 * Constants.MILLIS_PER_MINUTE;

    static {
        long allEffectiveRightsCacheSize= 0;
        long allEffectiveRightsCacheExpireAfterMillis = 0;
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            allEffectiveRightsCacheSize = server.getShortTermAllEffectiveRightsCacheSize();
            if (allEffectiveRightsCacheSize > 0) {
                allEffectiveRightsCacheExpireAfterMillis = server.getShortTermAllEffectiveRightsCacheExpiration();
                if (allEffectiveRightsCacheExpireAfterMillis < 0) {
                    allEffectiveRightsCacheExpireAfterMillis = 0;
                    allEffectiveRightsCacheSize = 0;
                } else if (allEffectiveRightsCacheExpireAfterMillis > MAX_CACHE_EXPIRY) {
                    allEffectiveRightsCacheExpireAfterMillis = MAX_CACHE_EXPIRY;
                }
            }
        } catch (ServiceException e) {
            allEffectiveRightsCacheSize = 0;
        }
        if (allEffectiveRightsCacheSize > 0) {
            ALL_EFFECTIVE_RIGHTS_CACHE =
                    CacheBuilder.newBuilder()
                    .maximumSize(allEffectiveRightsCacheSize)
                    .expireAfterWrite(allEffectiveRightsCacheExpireAfterMillis,
                            TimeUnit.MILLISECONDS) /* regard data as potentially stale after this time */
                    .build();
        } else {
            ALL_EFFECTIVE_RIGHTS_CACHE = null;
        }
    }

    public static void clearAllEffectiveRightsCache() {
        if (null != ALL_EFFECTIVE_RIGHTS_CACHE) {
            ZimbraLog.acl.debug("Clearing short term all effective rights cache of %d items.",
                    ALL_EFFECTIVE_RIGHTS_CACHE.size());
            ALL_EFFECTIVE_RIGHTS_CACHE.invalidateAll();
        }
    }

    private static AllEffectiveRights getAllEffectiveRights( AdminConsoleCapable acc, GranteeType granteeType,
            NamedEntry granteeEntry, RightBearer rightBearer, boolean expandSetAttrs, boolean expandGetAttrs)
    throws ServiceException {
        AllEffectiveRights aer = new AllEffectiveRights(
                granteeType.getCode(), granteeEntry.getId(), granteeEntry.getName());
        acc.getAllEffectiveRights(rightBearer, expandSetAttrs, expandGetAttrs, aer);
        return aer;
    }

    public static AllEffectiveRights getAllEffectiveRights(Provisioning prov,
            String granteeType, GranteeBy granteeBy, String grantee,
            boolean expandSetAttrs, boolean expandGetAttrs) throws ServiceException {
        AdminConsoleCapable acc = verifyAdminConsoleCapable();

        // grantee
        GranteeType gt = GranteeType.fromCode(granteeType);
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
        RightBearer rightBearer = RightBearer.newRightBearer(granteeEntry);

        AllEffectiveRights aer;
        String cacheKey = null;
        if (ALL_EFFECTIVE_RIGHTS_CACHE != null) {
            cacheKey = String.format("%s-%s-%s-%b-%b", rightBearer.getId(), gt.getCode(),
                    granteeEntry.getId(), expandSetAttrs, expandGetAttrs);
            aer = ALL_EFFECTIVE_RIGHTS_CACHE.getIfPresent(cacheKey);
            if (aer == null) {
                aer = getAllEffectiveRights(acc, gt, granteeEntry, rightBearer, expandSetAttrs, expandGetAttrs);
                ALL_EFFECTIVE_RIGHTS_CACHE.put(cacheKey, aer);
            }
        } else {
            aer = getAllEffectiveRights(acc, gt, granteeEntry, rightBearer, expandSetAttrs, expandGetAttrs);
        }
        return aer;
    }

    public static EffectiveRights getEffectiveRights(Provisioning prov,
            String targetType, TargetBy targetBy, String target,
            GranteeBy granteeBy, String grantee,
            boolean expandSetAttrs, boolean expandGetAttrs)
    throws ServiceException {
        AdminConsoleCapable acc = verifyAdminConsoleCapable();

        // target
        TargetType tt = TargetType.fromCode(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);

        // grantee
        GranteeType gt = GranteeType.GT_USER;
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
        // granteeEntry right must be an Account
        Account granteeAcct = (Account)granteeEntry;
        RightBearer rightBearer = RightBearer.newRightBearer(granteeEntry);

        EffectiveRights er = new EffectiveRights(targetType,
                TargetType.getId(targetEntry), targetEntry.getLabel(),
                granteeAcct.getId(), granteeAcct.getName());

        acc.getEffectiveRights(rightBearer, targetEntry, expandSetAttrs, expandGetAttrs, er);
        return er;
    }

    public static EffectiveRights getCreateObjectAttrs(
            Provisioning prov, String targetType,
            Key.DomainBy domainBy, String domainStr,
            Key.CosBy cosBy, String cosStr,
            GranteeBy granteeBy, String grantee)
    throws ServiceException {

        AdminConsoleCapable acc = verifyAdminConsoleCapable();

        TargetType tt = TargetType.fromCode(targetType);

        String domainName = null;
        if (tt == TargetType.domain) {
            if (domainBy != Key.DomainBy.name) {
                throw ServiceException.INVALID_REQUEST("must be by name for domain target", null);
            }

            domainName = domainStr;
        }
        Entry targetEntry = PseudoTarget.createPseudoTarget(prov, tt, domainBy, domainStr,
                false, cosBy, cosStr, domainName);

        // grantee
        GranteeType gt = GranteeType.GT_USER;
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
        // granteeEntry right must be an Account
        Account granteeAcct = (Account)granteeEntry;
        RightBearer rightBearer = RightBearer.newRightBearer(granteeEntry);

        EffectiveRights er = new EffectiveRights(targetType,
                TargetType.getId(targetEntry), targetEntry.getLabel(),
                granteeAcct.getId(), granteeAcct.getName());

        acc.getEffectiveRights(rightBearer, targetEntry, true, true, er);
        return er;
    }

    public static Grants getGrants(Provisioning prov,
            String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee,
             boolean granteeIncludeGroupsGranteeBelongs)
    throws ServiceException {
        verifyAccessManager();

        if (targetType == null && granteeType == null) {
            throw ServiceException.INVALID_REQUEST(
                    "at least one of target or grantee must be specified", null);
        }

        // target
        TargetType tt = null;
        Entry targetEntry = null;
        if (targetType != null) {
            tt = TargetType.fromCode(targetType);
            targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        }

        // grantee
        GranteeType gt = null;
        NamedEntry granteeEntry = null;
        Set<String> granteeFilter = null;
        Boolean isGranteeAnAdmin = null;
        if (granteeType != null) {
            gt = GranteeType.fromCode(granteeType);
            granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
            isGranteeAnAdmin = RightBearer.isValidGranteeForAdminRights(gt, granteeEntry);

            if (granteeIncludeGroupsGranteeBelongs) {
                Grantee theGrantee = Grantee.getGrantee(granteeEntry, false);
                granteeFilter = theGrantee.getIdAndGroupIds();
            } else {
                granteeFilter = new HashSet<String>();
                granteeFilter.add(granteeEntry.getId());
            }
        }

        Grants grants = new Grants();

        if (targetEntry != null) {
            // get ACL from the target
            ZimbraACL zimbraAcl = ACLUtil.getACL(targetEntry);

            // then filter by grnatee if grantee is specified
            grants.addGrants(tt, targetEntry, zimbraAcl, granteeFilter, isGranteeAnAdmin);

        } else {
            /*
             * no specific target, search for grants granted to
             * the grantee (and optionally groups the specified
             * grantee belongs to)
             *
             * If we come to this path, grantee must have been
             * specified.
             */

            // we want all target types
            Set<TargetType> targetTypesToSearch =
                new HashSet<TargetType>(Arrays.asList(TargetType.values()));

            SearchGrants searchGrants = new SearchGrants(prov, targetTypesToSearch, granteeFilter);
            Set<GrantsOnTarget> grantsOnTargets = searchGrants.doSearch().getResults();

            for (GrantsOnTarget grantsOnTarget : grantsOnTargets) {
                Entry grantedOnEntry = grantsOnTarget.getTargetEntry();
                ZimbraACL acl = grantsOnTarget.getAcl();
                TargetType grantedOnTargetType = TargetType.getTargetType(grantedOnEntry);
                grants.addGrants(grantedOnTargetType, grantedOnEntry, acl, granteeFilter, isGranteeAnAdmin);
            }
        }

        return grants;
    }

    private static void validateGrant(Account authedAcct,
            TargetType targetType, Entry targetEntry,
            GranteeType granteeType, NamedEntry granteeEntry, String secret,
            Right right, RightModifier rightModifier, boolean revoking)
    throws ServiceException {

        /*
         * check grantee if the right is an admin right, or if the right is an
         * user right with can_delegate modifier
         */
        if (!right.isUserRight() || RightModifier.RM_CAN_DELEGATE == rightModifier){

            /*
             * check if the grantee is an admin account or admin group
             *
             * If this is revoking, skip this check, just let the revoke through.
             * The grantee could have been taken away the admin privilege.
             */
            if (!revoking) {
                boolean isCDARight = CrossDomain.validateCrossDomainAdminGrant(right, granteeType);
                if (!isCDARight &&
                    !RightBearer.isValidGranteeForAdminRights(granteeType, granteeEntry)) {
                    throw ServiceException.INVALID_REQUEST("grantee for admin right or " +
                            "for user right with the canDelegate modifier must be a " +
                            "delegated admin account or admin group, it cannot be a " +
                            "global admin account or a regular user account.", null);
                }
            }

            /*
             * check if the grantee type can be used for an admin right
             */
            if (!granteeType.allowedForAdminRights()) {
                throw ServiceException.INVALID_REQUEST("grantee type " +
                        granteeType.getCode() +  " is not allowed for admin right", null);
            }
        }

        /*
         * check if the right can be granted on the target type
         */
        // first the "normal" checking
        if (!right.grantableOnTargetType(targetType)) {
            throw ServiceException.INVALID_REQUEST(
                    "right " + right.getName() +
                    " cannot be granted on a " + targetType.getCode() + " entry. " +
                    "It can only be granted on target types: " +
                    right.reportGrantableTargetTypes(), null);
        }

        /*
         * then the ugly special group target checking
         */
        if (targetType.isGroup() && !CheckRight.allowGroupTarget(right)) {
            throw ServiceException.INVALID_REQUEST(
                    "group target is not supported for right: " + right.getName(), null);
        }

        /*
         * check if the right modifier is applicable on the target and right
         */
        if (RightModifier.RM_SUBDOMAIN == rightModifier) {
            // can only be granted on domain targets
            if (targetType != TargetType.domain) {
                throw ServiceException.INVALID_REQUEST("right modifier " +
                        RightModifier.RM_SUBDOMAIN.getModifier() +
                        " can only be granted on domain targets", null);
            }

            if (!right.allowSubDomainModifier()) {
                throw ServiceException.INVALID_REQUEST("right modifier " +
                        RightModifier.RM_SUBDOMAIN.getModifier() +
                        " is not allowed for the right: " + right.getName(), null);
            }
        } else if (RightModifier.RM_DISINHERIT_SUB_GROUPS == rightModifier) {
            // can only be granted on group targets
            if (targetType != TargetType.dl) {
                throw ServiceException.INVALID_REQUEST("right modifier " + RightModifier.RM_DISINHERIT_SUB_GROUPS.getModifier() +
                        " can only be granted on group targets", null);
            }

            if (!right.allowDisinheritSubGroupsModifier()) {
                throw ServiceException.INVALID_REQUEST("right modifier " + RightModifier.RM_DISINHERIT_SUB_GROUPS.getModifier() +
                        " is not allowed for the right: " + right.getName(), null);
            }
        }

        /*
         * check if the authed account can grant this right on this target
         *
         * A grantor can only delegate the whole or part of his delegable rights
         * (rights with t he canDelegate modifier) on the same target or a subset
         * of targets on which the grantor's own rights were granted.
         *
         * Once that check is passed, the admin can grant the right to any grantees
         * (e.g. to a group, or for user rights to pub, all, guest, ...).
         *
         * The same rule applies when and admin is granting an user right.
         * e.g. if and admin is granting the invite right on a domain, the
         *      admin must have effective +invite right on the domain.
         *
         * Only a global admin can grant/revoke rights for external group grantees.
         *
         * if authedAcct==null, the call site is either LdapProvisioning or internal code,
         * treat it as a system admin and skip this check.
         */
        if (authedAcct != null) {
            AccessManager am = AccessManager.getInstance();

            if (granteeType == GranteeType.GT_EXT_GROUP) {
                // must be system admin
                if (!AccessControlUtil.isGlobalAdmin(authedAcct)) {
                    throw ServiceException.PERM_DENIED("only global admins can grant to external group");
                }
            } else {
                boolean canGrant = am.canPerform(authedAcct, targetEntry, right, true, null, true, null);
                if (!canGrant) {
                    throw ServiceException.PERM_DENIED(String.format("insufficient right to %s '%s' right",
                            (revoking?"revoke":"grant"), right.getName()));
                }

                ParticallyDenied.checkPartiallyDenied(authedAcct, targetType, targetEntry, right);
            }
        }

        if (secret != null && !granteeType.allowSecret()) {
            throw ServiceException.PERM_DENIED("password is not allowed for grantee type " +
                    granteeType.getCode());
        }
    }

    /*
     * only verifies if the grant can be made, does NOT action commit the grant
     */
    public static void verifyGrantRight(
            Provisioning prov, Account authedAcct,
            String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee, String secret,
            String right, RightModifier rightModifier)
    throws ServiceException {
        grantRightInternal(prov, authedAcct, TargetType.fromCode(targetType), targetBy, target,
                GranteeType.fromCode(granteeType), granteeBy, grantee, secret,
                right, rightModifier, true);
    }

    public static void grantRight(Provisioning prov, Account authedAcct,
            String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee, String secret,
            String right, RightModifier rightModifier)
    throws ServiceException {
        grantRight( prov, authedAcct, TargetType.fromCode(targetType), targetBy, target, granteeType,
                granteeBy, grantee, secret, right, rightModifier);
    }

    public static void grantRight(Provisioning prov, Account authedAcct, EffectiveRightsTargetSelector targSel,
            GranteeSelector granteeSel, String right, RightModifier rightModifier) throws ServiceException {
        grantRightInternal(prov, authedAcct, TargetType.fromJaxb(targSel.getType()), targSel.getBy(), targSel.getValue(),
                GranteeType.fromJaxb(granteeSel.getType()), granteeSel.getBy(),
                granteeSel.getKey(), granteeSel.getSecret(), right, rightModifier, false);
    }

    public static void grantRight(Provisioning prov, Account authedAcct,
            TargetType targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee, String secret,
            String right, RightModifier rightModifier)
    throws ServiceException {
        grantRightInternal(prov, authedAcct, targetType, targetBy, target,
                GranteeType.fromCode(granteeType), granteeBy, grantee, secret, right, rightModifier, false);
    }

    private static void grantRightInternal(
            Provisioning prov, Account authedAcct,
            TargetType tt, TargetBy targetBy, String target,
            GranteeType gt, GranteeBy granteeBy, String grantee, String secret,
            String right, RightModifier rightModifier, boolean dryRun)
    throws ServiceException {

        verifyAccessManager();

        // target
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);

        // right
        Right r = RightManager.getInstance().getRight(right);

        // grantee
        NamedEntry granteeEntry = null;
        String granteeId;
        if (gt.isZimbraEntry()) {
            granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
            granteeId = granteeEntry.getId();
        } else if (gt == GranteeType.GT_EXT_GROUP) {
            boolean asAdmin = !r.isUserRight();
            ExternalGroup extGroup = ExternalGroup.get(DomainBy.name, grantee, asAdmin);
            if (extGroup == null) {
                throw ServiceException.INVALID_REQUEST("unable to find external group " +
                        grantee, null);
            }
            granteeId = extGroup.getId();

        } else {
            // for all and pub, ZimbraACE will use the correct id, granteeId here will be ignored
            // for guest, grantee id is the email
            // for key, grantee id is the display name
            granteeId = grantee;
        }

        validateGrant(authedAcct, tt, targetEntry, gt, granteeEntry, secret, r, rightModifier, false);

        if (dryRun) {
            return;
        }

        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        ZimbraACE ace = new ZimbraACE(granteeId, gt, r, rightModifier, secret);
        aces.add(ace);

        ACLUtil.grantRight(prov, targetEntry, aces);
    }

    public static void revokeRight(Provisioning prov, Account authedAcct, EffectiveRightsTargetSelector targSel,
            GranteeSelector granteeSel, String right, RightModifier rightModifier) throws ServiceException {
        revokeRight(prov, authedAcct,
            TargetType.fromJaxb(targSel.getType()), targSel.getBy(), targSel.getValue(),
            GranteeType.fromJaxb(granteeSel.getType()), granteeSel.getBy(), granteeSel.getKey(),
            right, rightModifier);
    }

    public static void revokeRight(
            Provisioning prov, Account authedAcct,
            String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee,
            String right, RightModifier rightModifier)
    throws ServiceException {
        revokeRight(prov, authedAcct, TargetType.fromCode(targetType), targetBy, target,
                GranteeType.fromCode(granteeType), granteeBy, grantee, right, rightModifier);
    }

    public static void revokeRight(
            Provisioning prov, Account authedAcct,
            TargetType tt, TargetBy targetBy, String target,
            GranteeType gt, GranteeBy granteeBy, String grantee,
            String right, RightModifier rightModifier)
    throws ServiceException {

        verifyAccessManager();

        // target
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);

        // grantee
        NamedEntry granteeEntry = null;
        String granteeId = null;
        try {
            if (gt.isZimbraEntry()) {
                granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
                granteeId = granteeEntry.getId();
            } else {
                // for all and pub, ZimbraACE will use the correct id, granteeId here will be ignored
                // for guest, grantee id is the email
                // for key, grantee id is the display name
                granteeId = grantee;
            }
        } catch (AccountServiceException e) {
            String code = e.getCode();
            if (AccountServiceException.NO_SUCH_ACCOUNT.equals(code) ||
                AccountServiceException.NO_SUCH_DISTRIBUTION_LIST.equals(code) ||
                Constants.ERROR_CODE_NO_SUCH_DOMAIN.equals(code)) {

                ZimbraLog.acl.warn("revokeRight: no such grantee " + grantee);

                // grantee had been probably deleted.
                // if granteeBy is id, we try to revoke the orphan grant
                if (granteeBy == GranteeBy.id)
                    granteeId = grantee;
                else
                    throw ServiceException.INVALID_REQUEST("cannot find grantee by name: " + grantee +
                            ", try revoke by grantee id if you want to remove the orphan grant", e);
            } else
                throw e;
        }

        // right
        // note: if a forbidden attr is persisted in an ACL in an inline attr right
        //       (it can get in in a release before the attr is considered forbidden),
        //       the getRight() call will throw exception.
        //       Such grants will have to be removed by "zmprov modify{Entry} zimbraACE ..."
        //       command.  We do NOT want to do any special treatment here because those
        //       grants are not even loaded into memory, which is nice and clean, we don't
        //       want to hack that part.
        Right r = RightManager.getInstance().getRight(right);

        if (granteeEntry != null) {
            validateGrant(authedAcct, tt, targetEntry, gt, granteeEntry, null, r, rightModifier, true);
        }

        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        ZimbraACE ace = new ZimbraACE(granteeId, gt, r, rightModifier, null);
        aces.add(ace);

        List<ZimbraACE> revoked = ACLUtil.revokeRight(prov, targetEntry, aces);
        if (revoked.isEmpty())
            throw AccountServiceException.NO_SUCH_GRANT(ace.dump(true));
    }

    /**
     * revoke all grants granted to the specified grantee, invoked from
     * LdapProvisioning.deleteAccount.
     *
     * note: no verification (things done in verifyGrant) is done in this method
     *       if the authed user can delete the account, it can delete all grants
     *       granted to the account.
     *
     */
    public static void revokeAllRights(Provisioning prov,
            GranteeType granteeType, String granteeId) throws ServiceException {

        AdminConsoleCapable acc = verifyAdminConsoleCapable();

        //
        // search grants
        //
        Set<TargetType> targetTypesToSearch = acc.targetTypesForGrantSearch();

        // search for grants granted to this grantee
        Set<String> granteeIdsToSearch = new HashSet<String>();
        granteeIdsToSearch.add(granteeId);

        SearchGrants searchGrants = new SearchGrants(prov, targetTypesToSearch, granteeIdsToSearch);
        Set<GrantsOnTarget> grantsOnTargets = searchGrants.doSearch().getResults();

        for (GrantsOnTarget grantsOnTarget : grantsOnTargets) {
            Entry targetEntry = grantsOnTarget.getTargetEntry();

            Set<ZimbraACE> acesToRevoke = new HashSet<ZimbraACE>();
            for (ZimbraACE ace : grantsOnTarget.getAcl().getAllACEs()) {
                if (granteeId.equals(ace.getGrantee())) {
                    acesToRevoke.add(ace);
                }
            }
            ACLUtil.revokeRight(prov, targetEntry, acesToRevoke);
        }
        RightBearer.Grantee.clearGranteeCache();  // as a precaution in case a new account is created with same name
    }

    public static Element rightToXML(Element parent, Right right, boolean expandAllAtrts,
            Locale locale) throws ServiceException {
        Element eRight = parent.addNonUniqueElement(AdminConstants.E_RIGHT);
        eRight.addAttribute(AdminConstants.E_NAME, right.getName());
        eRight.addAttribute(AdminConstants.A_TYPE, right.getRightType().name());
        eRight.addAttribute(AdminConstants.A_TARGET_TYPE, right.getTargetTypeStr());
        eRight.addAttribute(AdminConstants.A_RIGHT_CLASS, right.getRightClass().name());

        String desc = L10nUtil.getMessage(false /* shoutIfMissing */,
                L10nUtil.MSG_RIGHTS_FILE_BASENAME, right.getName(), locale);
        if (desc == null) {
            desc = right.getDesc();
        }

        /*
         * Don't do this.  Help text is too long and all the formatting will be lost so
         * it doesn't look good in admin console anyway.
         * Just display help text in zmprov.
         *
        Help help = right.getHelp();
        if (help != null) {
            String helpTxt = L10nUtil.getMessage(L10nUtil.MSG_RIGHTS_FILE_BASENAME, help.getName(), locale);
            if (helpTxt != null) {
                desc = desc + ".  " + helpTxt;
            }
        }
        */


        eRight.addNonUniqueElement(AdminConstants.E_DESC).setText(desc);

        if (right.isPresetRight()) {
            // nothing to do here
        } else if (right.isAttrRight()) {
            Element eAttrs = eRight.addNonUniqueElement(AdminConstants.E_ATTRS);
            AttrRight attrRight = (AttrRight)right;

            if (attrRight.allAttrs()) {
                eAttrs.addAttribute(AdminConstants.A_ALL, true);
                if (expandAllAtrts) {
                    Set<String> attrs = attrRight.getAllAttrs();
                    for (String attr : attrs) {
                        if (right.getRightType() != RightType.setAttrs || !HardRules.isForbiddenAttr(attr)) {
                            eAttrs.addNonUniqueElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, attr);
                        }
                    }

                }
            } else {
                for (String attrName :attrRight.getAttrs()) {
                    eAttrs.addNonUniqueElement(attrName);
                }
            }
        } else if (right.isComboRight()) {
            Element eRights = eRight.addNonUniqueElement(AdminConstants.E_RIGHTS);
            ComboRight comboRight = (ComboRight)right;
            for (Right r : comboRight.getRights()) {
                Element eNestedRight = eRights.addNonUniqueElement(AdminConstants.E_R);
                eNestedRight.addAttribute(AdminConstants.A_N, r.getName());
                eNestedRight.addAttribute(AdminConstants.A_TYPE, r.getRightType().name());
                eNestedRight.addAttribute(AdminConstants.A_TARGET_TYPE, r.getTargetTypeStr());
            }
        }

        return eRight;
    }

    /*
     * Hack.  We do *not* parse the SOAP response.   Instead we just get the right from
     * right manager.  The Right object is only used by zmprov, not by generic SOAP clients.
     */
    public static Right XMLToRight(Element eRight) throws ServiceException  {
        String rightName = eRight.getAttribute(AdminConstants.E_NAME);
        return RightNameToRight(rightName);
    }

    public static Right RightNameToRight(String rightName)
    throws ServiceException  {
        return RightManager.getInstance().getRight(rightName);
    }

}
