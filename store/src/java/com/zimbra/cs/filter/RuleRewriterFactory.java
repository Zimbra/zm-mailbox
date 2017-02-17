/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.filter;

import java.util.List;

import org.apache.jsieve.parser.generated.Node;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.ElementFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;

class RuleRewriterFactory {
	
	private static RuleRewriterFactory instance = null;	
	
    synchronized static RuleRewriterFactory getInstance() {
		if (instance == null) {
	        String className = LC.zimbra_class_rulerewriterfactory.value();
	        if (className != null && !className.equals("")) {
	            try {
	                instance = (RuleRewriterFactory) Class.forName(className).newInstance();
	            } catch (Exception e) {
	                ZimbraLog.filter.error("could not instantiate RuleRewriterFactory interface of class '" + className + "'; defaulting to RuleRewriterFactory", e);
	            }
	        }
	        if (instance == null)
	            instance = new RuleRewriterFactory();
		}
        return instance;
    }
    
    RuleRewriter createRuleRewriter() {
    	return new RuleRewriter();
    }
    
    RuleRewriter createRuleRewriter(ElementFactory factory, Node node, List<String> ruleNames) {
    	RuleRewriter rrw = createRuleRewriter();
    	rrw.initialize(factory, node, ruleNames);
    	return rrw;
    }
    
    RuleRewriter createRuleRewriter(Element eltRules, Mailbox mbox) {
     	RuleRewriter rrw = createRuleRewriter();
    	rrw.initialize(eltRules, mbox);
    	return rrw;
    }
}
