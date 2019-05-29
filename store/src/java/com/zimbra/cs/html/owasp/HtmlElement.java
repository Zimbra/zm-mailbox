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

import static com.zimbra.cs.html.owasp.HtmlElementsBuilder.COMMA;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.owasp.html.AttributePolicy;
import org.owasp.html.ElementPolicy;
import org.owasp.html.FilterUrlByProtocolAttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.HtmlPolicyBuilder.AttributeBuilder;

import com.google.common.base.Optional;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.html.owasp.policies.BackgroundPolicy;
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
        if (neuterImages) {
            policyBuilder.allowElements(new BackgroundPolicy(), elementName);
        }
        Set<String> allowedAttributes = attributesAndPolicies.keySet();
        AttributeBuilder attributesBuilder = null;
        for (String attribute : allowedAttributes) {
            attributesBuilder = policyBuilder.allowAttributes(attribute);
            if (attributesBuilder != null) {
                String urlProtocols = OwaspPolicy.getElementUrlProtocols(element);
                if (!StringUtil.isNullOrEmpty(urlProtocols)) {
                    String[] allowedProtocols = urlProtocols.split(COMMA);
                    List<String> urlList = Arrays.asList(allowedProtocols);
                    AttributePolicy URLPolicy = new FilterUrlByProtocolAttributePolicy(urlList);
                    attributesBuilder.matching(URLPolicy);
                }
                AttributePolicy attrPolicy = attributesAndPolicies.get(attribute);
                if (attrPolicy != null) {
                    if (attrPolicy instanceof SrcAttributePolicy) {
                        if (neuterImages
                            && (elementName.equals("img") || elementName.equals("input"))) {
                            attributesBuilder.matching(attrPolicy);
                        }
                    } else {
                        attributesBuilder.matching(attrPolicy);
                    }
                }
                attributesBuilder.onElements(elementName);
            }
        }
    }

}