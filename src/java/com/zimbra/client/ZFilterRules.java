/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.client;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.mail.type.FilterRule;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ZFilterRules implements ToZJSONObject {

    private final List<ZFilterRule> rules;

    public ZFilterRules(List<ZFilterRule> rules) {
        this.rules = rules;
    }

    public ZFilterRules(ZFilterRules rules) {
        this.rules = new ArrayList<ZFilterRule>(rules.getRules());
    }

    public ZFilterRules(Collection<FilterRule> list) throws ServiceException {
        this.rules = Lists.newArrayListWithCapacity(list.size());
        for (FilterRule rule : list) {
            this.rules.add(new ZFilterRule(rule));
        }
    }

    public List<FilterRule> toJAXB() {
        List<FilterRule> list = Lists.newArrayListWithCapacity(rules.size());
        for (ZFilterRule rule : rules) {
            list.add(rule.toJAXB());
        }
        return list;
    }

    public List<ZFilterRule> getRules() {
        return rules;
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("rules", rules);
        return jo;
    }

    @Override
    public String toString() {
        return String.format("[ZFilterRules]");
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
