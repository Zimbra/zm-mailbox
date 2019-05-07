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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.owasp.html.ElementPolicy;

import com.zimbra.cs.html.owasp.policies.AElementPolicy;
import com.zimbra.cs.html.owasp.policies.AreaElementPolicy;
import com.zimbra.cs.html.owasp.policies.BaseElementPolicy;
import com.zimbra.cs.html.owasp.policies.DivElementPolicy;

/*
 * Build the list of HtmlElements from policy file
 */
public class HtmlElementsBuilder {

    static final String COMMA = "(\\s)?+,(\\s)?+";
    private HtmlAttributesBuilder builder;
    private Map<String, ElementPolicy> elementSpecificPolicies = new HashMap<String, ElementPolicy>();

    public HtmlElementsBuilder(HtmlAttributesBuilder builder) {
        this.builder = builder;
    }

    public void setUp() {
        elementSpecificPolicies.put("div", new DivElementPolicy());
        elementSpecificPolicies.put("a", new AElementPolicy());
        elementSpecificPolicies.put("area", new AreaElementPolicy());
        elementSpecificPolicies.put("base", new BaseElementPolicy());
        // add any other element policies
    }

    public List<HtmlElement> build() {
        setUp();
        Set<String> allowed = OwaspPolicy.getAllowedElements();
        List<HtmlElement> elements = new ArrayList<>();
        for (String element : allowed) {
            final ElementPolicy policy = elementSpecificPolicies.get(element);
            elements.add(new HtmlElement(element, policy, builder.build(element)));
        }

        return elements;
    }

}
