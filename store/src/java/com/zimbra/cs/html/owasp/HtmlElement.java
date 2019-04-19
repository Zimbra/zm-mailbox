package com.zimbra.cs.html.owasp;

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
import java.util.Map;
import java.util.Set;

import org.owasp.html.AttributePolicy;
import org.owasp.html.ElementPolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.HtmlPolicyBuilder.AttributeBuilder;

import com.google.common.base.Optional;
import com.zimbra.cs.html.owasp.policies.BackgroundAttributePolicy;
import com.zimbra.cs.html.owasp.policies.NoSpaceEncodedCharAttributePolicy;
import com.zimbra.cs.html.owasp.policies.SrcAttributePolicy;

/*
 * HTML element along with its policy and attributes
 */
public class HtmlElement {

    private String element;
    private Optional<ElementPolicy> elementPolicy;
    private Map<String, AttributePolicy> attributesAndPolicies;

    HtmlElement(String element, ElementPolicy elementPolicy,
                Map<String, AttributePolicy> attributesAndPolicies) {
        this.element = element;
        this.elementPolicy = Optional.fromNullable(elementPolicy);
        this.attributesAndPolicies = attributesAndPolicies;
    }

    public String getElement() {
        return element;
    }

    public void configure(HtmlPolicyBuilder policyBuilder, boolean neuterImages) {
        String elementName = getElement();

        if (elementPolicy.isPresent()) {
            policyBuilder.allowElements(elementPolicy.get(), elementName);
        } else {
            policyBuilder.allowElements(elementName);
        }

        Set<String> allowedAttributes = attributesAndPolicies.keySet();
        AttributeBuilder attributesBuilder = null;
        for (String attribute : allowedAttributes) {
            attributesBuilder = policyBuilder.allowAttributes(attribute);
            if (attributesBuilder != null) {
                attributesBuilder.matching(new NoSpaceEncodedCharAttributePolicy());
                AttributePolicy attrPolicy = attributesAndPolicies.get(attribute);
                if (attrPolicy != null) {
                    if (neuterImages && (attrPolicy instanceof SrcAttributePolicy
                        || attrPolicy instanceof BackgroundAttributePolicy)) {
                        attributesBuilder.matching(attrPolicy);
                    }
                }
                attributesBuilder.onElements(elementName);
            }
        }
    }

}