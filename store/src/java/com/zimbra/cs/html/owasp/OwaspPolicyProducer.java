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
import java.util.List;
import java.util.Set;

import org.owasp.html.CssSchema;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import com.google.common.collect.ImmutableSet;

/*
 * Instantiate owasp policy instance with neuter images true/false at load time
 */
public class OwaspPolicyProducer {

    private static PolicyFactory policyNeuterImagesTrue;
    private static PolicyFactory policyNeuterImagesFalse;

    /**
     * The following CSS properties do not appear in the default whitelist from
     * OWASP, but they improve the fidelity of the HTML display without
     * unacceptable risk.
     */
    private static final CssSchema ADDITIONAL_CSS = CssSchema
        .withProperties(ImmutableSet.of("float"));

    private static void setUp(boolean neuterImages) {
        HtmlElementsBuilder builder = new HtmlElementsBuilder(new HtmlAttributesBuilder());
        List<HtmlElement> allowedElements = builder.build();
        HtmlPolicyBuilder policyBuilder = new HtmlPolicyBuilder();
        for (HtmlElement htmlElement : allowedElements) {
            htmlElement.configure(policyBuilder, neuterImages);
        }
        Set<String> disallowTextElements = OwaspPolicy.getDisallowTextElements();
        for (String disAllowTextElement : disallowTextElements) {
            policyBuilder.disallowTextIn(disAllowTextElement.trim());
        }
        Set<String> urlProtocols = OwaspPolicy.getURLProtocols();
        for (String urlProtocol : urlProtocols) {
            policyBuilder.allowUrlProtocols(urlProtocol.trim());
        }
        if(neuterImages) {
            if(policyNeuterImagesTrue == null) {
            policyNeuterImagesTrue = policyBuilder.allowStyling(CssSchema.union(CssSchema.DEFAULT, ADDITIONAL_CSS))
            .toFactory();
            }
        } else {
            if(policyNeuterImagesFalse == null) {
            policyNeuterImagesFalse = policyBuilder.allowStyling(CssSchema.union(CssSchema.DEFAULT, ADDITIONAL_CSS))
                .toFactory();
            }
        }
    }

    public static PolicyFactory getPolicyFactoryInstance(boolean neuterImages) {
        if (neuterImages) {
            return policyNeuterImagesTrue;
        } else {
            return policyNeuterImagesFalse;
        }
    }

    static {
        setUp(true); // setup policy for neuter image true
        setUp(false); // setup policy for neuter image false
    }

}