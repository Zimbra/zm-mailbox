/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
