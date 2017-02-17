/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;

public class ComboRight extends AdminRight {
    // directly contained rights
    private Set<Right> mRights = new HashSet<Right>();

    // directly and indirectly contained rights
    private Set<Right> mAllRights = new HashSet<Right>();

    // all preset rights contained in this combo right
    private Set<Right> mPresetRights = new HashSet<Right>();

    // all attr rights contained in this combo right
    private Set<AttrRight> mAttrRights = new HashSet<AttrRight>();

    ComboRight(String name) {
        super(name, RightType.combo);
    }

    @Override
    public boolean isComboRight() {
        return true;
    }

    public String dump(StringBuilder sb) {
        super.dump(sb);

        sb.append("===== combo right properties: =====\n");

        sb.append("rights:\n");
        for (Right r : mRights) {
            sb.append("    ");
            sb.append(r.getName());
            sb.append("\n");
        }

        return sb.toString();
    }

    void addRight(Right right) throws ServiceException {
        // sanity check, combo right can only contain admin rights
        if (right.isUserRight())
            throw ServiceException.FAILURE("internal error", null);
        mRights.add(right);
    }

    @Override
    boolean overlaps(Right other) throws ServiceException {

        for (Right r : getAllRights()) {
            // r is either a preset right or an attr right
            // delegate to the overlaps method of those
            if (r.overlaps(other))
                return true;
        }
        return false;
    }

    @Override
    boolean executableOnTargetType(TargetType targetType) {
        return true;
    }

    @Override
    boolean isValidTargetForCustomDynamicGroup() {
        for (Right right : getAllRights()) {
            if (!right.isValidTargetForCustomDynamicGroup()) {
                return false;
            }
        }
        return true;
    }

    @Override
    boolean grantableOnTargetType(TargetType targetType) {

        // true if *all* of the rights in the combo right are
        // grantable on targetType
        for (Right r : getAllRights()) {
            if (!r.grantableOnTargetType(targetType)) {
                return false;
            }
        }
        return true;
    }

    @Override
    boolean allowSubDomainModifier() {
        // true if *any* of the rights in the combo right are
        // executable on targetType domain
        for (Right r : getAllRights()) {
            if (r.executableOnTargetType(TargetType.domain)) {
                return true;
            }
        }
        return false;
    }

    @Override
    boolean allowDisinheritSubGroupsModifier() {
        // true if *any* of the rights in the combo right are
        // executable on targetType dl or account or calresource
        for (Right r : getAllRights()) {
            if (r.executableOnTargetType(TargetType.dl) ||
                r.executableOnTargetType(TargetType.account) ||
                r.executableOnTargetType(TargetType.calresource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    Set<TargetType> getGrantableTargetTypes() {
        // return *intersect* of target types from which *all* of the target types
        // for the right can inherit from
        Set<TargetType> targetTypes = null;
        for (Right r : getAllRights()) {
            Set<TargetType> tts = r.getGrantableTargetTypes();
            if (targetTypes == null)
                targetTypes = tts;
            else
                targetTypes = SetUtil.intersect(targetTypes, tts);
        }

        return targetTypes;
    }

    @Override
    void setTargetType(TargetType targetType) throws ServiceException {
        throw ServiceException.FAILURE("target type is now allowed for combo right", null);
    }

    @Override
    void verifyTargetType() throws ServiceException {
    }

    @Override
    public TargetType getTargetType() throws ServiceException {
        throw ServiceException.FAILURE("internal error", null);
    }

    @Override
    public String getTargetTypeStr() {
        return null;
    }

    @Override
    void completeRight() throws ServiceException {
        super.completeRight();

        expand(this, mPresetRights, mAttrRights);
        mAllRights.addAll(mPresetRights);
        mAllRights.addAll(mAttrRights);
    }

    private static void expand(ComboRight right, Set<Right> presetRights,
            Set<AttrRight> attrRights) throws ServiceException {
        for (Right r : right.getRights()) {
            if (r.isPresetRight())
                presetRights.add(r);
            else if (r.isAttrRight())
                attrRights.add((AttrRight)r);
            else if (r.isComboRight())
                expand((ComboRight)r, presetRights, attrRights);
            else
                throw ServiceException.FAILURE("internal error", null);
        }
    }

    boolean containsPresetRight(Right right) {
        return mPresetRights.contains(right);
    }

    // get all (direct or indirect) preset rights contained in this combo right
    Set<Right> getPresetRights() {
        return mPresetRights;
    }

    // get all (direct or indirect) attr rights contained in this combo right
    Set<AttrRight> getAttrRights() {
        return mAttrRights;
    }

    // get rights directly contained in this combo right
    public Set<Right> getRights() {
        return mRights;
    }

    // get all (direct or indirect) rights contained in this combo right
    public Set<Right> getAllRights() {
        return mAllRights;
    }

}
