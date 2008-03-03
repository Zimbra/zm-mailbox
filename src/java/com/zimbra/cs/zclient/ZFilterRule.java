/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.zclient.ZFilterCondition.ZAttachmentExistsCondition;
import com.zimbra.cs.zclient.ZFilterCondition.ZHeaderExistsCondition;
import com.zimbra.cs.zclient.ZFilterCondition.HeaderOp;
import com.zimbra.cs.zclient.ZFilterCondition.ZAddressBookCondition;
import com.zimbra.cs.zclient.ZFilterCondition.ZHeaderCondition;
import com.zimbra.cs.zclient.ZFilterCondition.ZBodyCondition;
import com.zimbra.cs.zclient.ZFilterCondition.AddressBookOp;
import com.zimbra.cs.zclient.ZFilterCondition.BodyOp;
import com.zimbra.cs.zclient.ZFilterCondition.SizeOp;
import com.zimbra.cs.zclient.ZFilterCondition.ZSizeCondition;
import com.zimbra.cs.zclient.ZFilterCondition.DateOp;
import com.zimbra.cs.zclient.ZFilterCondition.ZDateCondition;
import com.zimbra.cs.zclient.ZFilterAction.MarkOp;
import com.zimbra.cs.zclient.ZFilterAction.ZFileIntoAction;
import com.zimbra.cs.zclient.ZFilterAction.ZMarkAction;
import com.zimbra.cs.zclient.ZFilterAction.ZKeepAction;
import com.zimbra.cs.zclient.ZFilterAction.ZRedirectAction;
import com.zimbra.cs.zclient.ZFilterAction.ZDiscardAction;
import com.zimbra.cs.zclient.ZFilterAction.ZTagAction;
import com.zimbra.cs.zclient.ZFilterAction.ZStopAction;

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
        mAllConditions = groupEl.getAttribute(MailConstants.A_OPERATION, "allof").equalsIgnoreCase("allof");
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

    static String quotedString(String s) {
        return "\"" + s.replaceAll("\"", "\\\"") + "\"";
    }

    /**
     * generate the human-readable form of a rule that parseFilterRule supports.
     * @return human-readable rule
     */
    public String generateFilterRule() {
        StringBuilder sb = new StringBuilder();
        sb.append(quotedString(mName)).append(' ');
        sb.append(mActive ? "active" : "inactive").append(' ');
        sb.append(mAllConditions ? "all" : "any").append(' ');
        boolean needSpace = false;
        for (ZFilterCondition cond : mConditions) {
            if (needSpace) sb.append(' ');
            sb.append(cond.toConditionString());
            needSpace = true;
        }

        for (ZFilterAction action : mActions) {
            if (needSpace) sb.append(' ');
            sb.append(action.toActionString());
            needSpace = true;
        }
        return sb.toString();
    }
    /**
     * 
     *
     * <pre>
  {name}  [*active|inactive] [any|*all] {conditions}+ {actions}+

  {conditions}:

  header "name" is|not_is|contains|not_contains|matches|not_matches "value"
  header "name" exists|not_exists
  date before|not_before|after|not_after "YYYYMMDD"
  size under|not_under|over|not_over "1|1K|1M"
  body contains|not_contains "text"
  in_addressbook|not_in_addressbook
  attachment exists|not_exists

  {actions}:

  keep
  discard
  fileinto "/path"
  tag "/tag"
  mark read|flagged
  redirect "address"
  stop
</pre>

     */
    public static ZFilterRule parseFilterRule(String[] args) throws ServiceException {
        String name = args[0];
        boolean all = true;
        boolean active = true;

        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();

        int i = 1;
        while (i < args.length) {
            String a = args[i++];
            if (a.equals("active")) {
                active = true;
            } else if (a.equals("inactive")) {
                active = false;
            } else if (a.equals("any")) {
                all = false;
            } else if (a.equals("all")) {
                all = true;
            } else if (a.equals("addressbook")) {
                if (i + 2 > args.length) throw ZClientException.CLIENT_ERROR("missing args", null);
                conditions.add(new ZAddressBookCondition(
                        args[i++].equals("in") ? AddressBookOp.IN : AddressBookOp.NOT_IN,
                        args[i++]));
            } else if (a.equals("attachment")) {
                if (i + 1 > args.length) throw ZClientException.CLIENT_ERROR("missing exists arg", null);
                conditions.add(new ZAttachmentExistsCondition(args[i++].equals("exists")));
            } else if (a.equals("body")) {
                if (i + 2 > args.length) throw ZClientException.CLIENT_ERROR("missing args", null);
                conditions.add(new ZBodyCondition(BodyOp.fromString(args[i++]), args[i++]));
            } else if (a.equals("size")) {
                if (i + 2 > args.length) throw ZClientException.CLIENT_ERROR("missing args", null);
                conditions.add(new ZSizeCondition(SizeOp.fromString(args[i++]), args[i++]));
            } else if (a.equals("date")) {
                if (i + 2 > args.length) throw ZClientException.CLIENT_ERROR("missing args", null);
                conditions.add(new ZDateCondition(DateOp.fromString(args[i++]), args[i++]));
            } else if (a.equals("header")) {
                if (i + 2 > args.length) throw ZClientException.CLIENT_ERROR("missing args", null);
                String headerName = args[i++];
                String op = args[i++];
                if (op.equals("exists")) {
                    conditions.add(new ZHeaderExistsCondition(headerName, true));
                } else if (op.equals("not_exists")) {
                    conditions.add(new ZHeaderExistsCondition(headerName, false));
                } else {
                    if (i + 1 > args.length) throw ZClientException.CLIENT_ERROR("missing args", null);
                    String value = args[i++];
                    conditions.add(new ZHeaderCondition(headerName, HeaderOp.fromString(op), value));
                }
            } else if (a.equals("keep")) {
                actions.add(new ZKeepAction());
            } else if (a.equals("discard")) {
                actions.add(new ZDiscardAction());
            } else if (a.equals("fileinto")) {
                if (i + 1 > args.length) throw ZClientException.CLIENT_ERROR("missing args", null);
                actions.add(new ZFileIntoAction(args[i++]));
            } else if (a.equals("tag")) {
                if (i + 1 > args.length) throw ZClientException.CLIENT_ERROR("missing args", null);
                actions.add(new ZTagAction(args[i++]));
            } else if (a.equals("mark")) {
                if (i + 1 > args.length) throw ZClientException.CLIENT_ERROR("missing args", null);
                actions.add(new ZMarkAction(MarkOp.fromString(args[i++])));
            } else if (a.equals("redirect")) {
                if (i + 1 > args.length) throw ZClientException.CLIENT_ERROR("missing args", null);
                actions.add(new ZRedirectAction(args[i++]));
            } else if (a.equals("stop")) {
                actions.add(new ZStopAction());
            } else {
                throw ZClientException.CLIENT_ERROR("unknown keyword: "+a, null);
            }
        }

        if (name == null || name.length() == 0)
            throw ZClientException.CLIENT_ERROR("missing filter name", null);
        if (actions.isEmpty())
            throw ZClientException.CLIENT_ERROR("must have at least one action", null);
        if (conditions.isEmpty())
            throw ZClientException.CLIENT_ERROR("must have at least one condition", null);

        return new ZFilterRule(name, active, all, conditions, actions);
    }

}
