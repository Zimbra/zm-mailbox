/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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
package com.zimbra.cs.html.owasp;

import static com.zimbra.cs.html.owasp.HtmlElementsBuilder.COMMA;

import java.util.HashMap;
import java.util.Map;

import org.owasp.html.AttributePolicy;

import com.zimbra.cs.html.owasp.policies.ActionAttributePolicy;
import com.zimbra.cs.html.owasp.policies.BackgroundAttributePolicy;
import com.zimbra.cs.html.owasp.policies.SrcAttributePolicy;

public class HtmlAttributesBuilder {

    private Map<String, AttributePolicy> attributePolicies = new HashMap<String, AttributePolicy>();

    public HtmlAttributesBuilder() {
    }

    public void setUp() {
        attributePolicies.put("src", new SrcAttributePolicy());
        attributePolicies.put("action", new ActionAttributePolicy());
        attributePolicies.put("background", new BackgroundAttributePolicy());
        // add any other attribute policies here
    }

    public Map<String, AttributePolicy> build(String element) {
        setUp();
        Map<String, AttributePolicy> attributesAndPolicies = new HashMap<>();
        String allowedString = OwaspPolicy.getAttributes(element);
        String[] allowedAttributes = allowedString.split(COMMA);
        for (String attribute : allowedAttributes) {
            AttributePolicy attrPolicy = attributePolicies.get(attribute);
            attributesAndPolicies.put(attribute, attrPolicy);
        }
        return attributesAndPolicies;
    }

}
