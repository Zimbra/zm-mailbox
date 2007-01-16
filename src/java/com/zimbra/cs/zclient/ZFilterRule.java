/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;

import java.util.ArrayList;
import java.util.List;

public class ZFilterRule {

    private String mName;
    private boolean mActive;
    private boolean mAllConditions;

    private List<ZFilterCondition> mConditions;
    private List<ZFilterAction> mActions;

    public String getName() {
        return mName;
    }

    public boolean isActive() {
        return mActive;
    }

    public boolean isAllConditions() {
        return mAllConditions;
    }

    public List<ZFilterCondition> getConditions() {
        return mConditions;
    }

    public List<ZFilterAction> getActions() {
        return mActions;
    }

    public ZFilterRule(String name, boolean active, boolean allConditions,
                       List<ZFilterCondition> conditions, List<ZFilterAction> actions) {
        mName = name;
        mActive = active;
        mAllConditions = allConditions;
        mConditions = conditions;
        mActions = actions;
    }

    public ZFilterRule(Element e) throws ServiceException {
        mName = e.getAttribute(MailConstants.A_NAME);
        mActive = e.getAttributeBool(MailConstants.A_ACTIVE, false);
        Element groupEl = e.getElement(MailConstants.E_CONDITION_GROUP);
        mConditions = new ArrayList<ZFilterCondition>();
        mAllConditions = groupEl.getAttribute(MailConstants.A_OPERATION, "allof").equals("allof");
        for (Element condEl : groupEl.listElements(MailConstants.E_CONDITION)) {
            mConditions.add(ZFilterCondition.getCondition(condEl));
        }
        mActions = new ArrayList<ZFilterAction>();
        for (Element actionEl : e.listElements(MailConstants.E_ACTION)) {
            mActions.add(ZFilterAction.getAction(actionEl));
        }
    }

    Element toElement(Element parent) {
        Element r = parent.addElement(MailConstants.E_RULE);
        r.addAttribute(MailConstants.A_NAME, mName);
        r.addAttribute(MailConstants.A_ACTIVE, mActive);
        Element g = r.addElement(MailConstants.E_CONDITION_GROUP);
        g.addAttribute(MailConstants.A_OPERATION, mAllConditions ? "allof" : "anyof");
        for (ZFilterCondition condition : mConditions) {
            condition.toElement(g);
        }
        for (ZFilterAction action : mActions) {
            action.toElement(r);
        }
        return r;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("name", mName);
        sb.add("active", mActive);
        sb.add("allConditions", mAllConditions);
        sb.add("conditions", mConditions, false, true);
        sb.add("actions", mActions, false, true);
        sb.endStruct();
        return sb.toString();
    }
}
