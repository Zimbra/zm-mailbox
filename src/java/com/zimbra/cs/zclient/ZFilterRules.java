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

import java.util.ArrayList;
import java.util.List;

public class ZFilterRules {

    private List<ZFilterRule> mRules;

    public List<ZFilterRule> getRules() {
        return mRules;
    }

    public ZFilterRules(List<ZFilterRule> rules) {
        mRules = rules;
    }

    public ZFilterRules(ZFilterRules rules) {
        mRules = new ArrayList<ZFilterRule>();
        mRules.addAll(rules.getRules());
    }

    public ZFilterRules(Element e) throws ServiceException {
        mRules = new ArrayList<ZFilterRule>();
        for (Element ruleEl : e.listElements(MailConstants.E_RULE)) {
            mRules.add(new ZFilterRule(ruleEl));
        }
    }

    public Element toElement(Element parent) {
        Element r = parent.addElement(MailConstants.E_RULES);
        for (ZFilterRule rule : mRules) {
            rule.toElement(r);
        }
        return r;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("rules", mRules, false, true);
        sb.endStruct();
        return sb.toString();
    }
}
