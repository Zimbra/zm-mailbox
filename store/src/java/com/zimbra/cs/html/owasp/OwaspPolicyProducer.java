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

import java.util.List;
import java.util.Set;

import org.owasp.html.CssSchema;
import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

/*
 * Instantiate owasp policy instance with neuter images true/false at load time
 */
public class OwaspPolicyProducer {
    private static PolicyFactory policyNeuterImagesTrue;
    private static PolicyFactory policyNeuterImagesFalse;

    private static void setUp(boolean neuterImages) {
        HtmlElementsBuilder builder = new HtmlElementsBuilder(new HtmlAttributesBuilder(), neuterImages);
        List<HtmlElement> allowedElements = builder.build();
        HtmlPolicyBuilder policyBuilder = new HtmlPolicyBuilder();

        policyBuilder.requireRelNofollowOnLinks();

        for (HtmlElement htmlElement : allowedElements) {
            htmlElement.configure(policyBuilder, neuterImages);
        }

        for (String disAllowTextElement : OwaspPolicy.getDisallowTextElements()) {
            policyBuilder.disallowTextIn(disAllowTextElement.trim());
        }

        for (String allowTextElement : OwaspPolicy.getAllowTextElements()) {
            policyBuilder.allowTextIn(allowTextElement.trim());
        }

        /**
         * The following CSS properties do not appear in the default whitelist from
         * OWASP, but they improve the fidelity of the HTML display without
         * unacceptable risk.
         */
        Set<String> cssWhitelist = OwaspPolicy.getCssWhitelist();
        CssSchema ADDITIONAL_CSS = null;
        if (!cssWhitelist.isEmpty()) {
            ADDITIONAL_CSS = CssSchema.withProperties(cssWhitelist);
        }

        for (String urlProtocol : OwaspPolicy.getURLProtocols()) {
            policyBuilder.allowUrlProtocols(urlProtocol.trim());
        }

        if (neuterImages) {
            if (policyNeuterImagesTrue == null) {
                policyNeuterImagesTrue = policyBuilder.allowStyling(ADDITIONAL_CSS == null
                    ? CssSchema.DEFAULT : CssSchema.union(CssSchema.DEFAULT, ADDITIONAL_CSS))
                    .toFactory();
            }
        } else {
            if (policyNeuterImagesFalse == null) {
                policyNeuterImagesFalse = policyBuilder.allowStyling(ADDITIONAL_CSS == null
                    ? CssSchema.DEFAULT : CssSchema.union(CssSchema.DEFAULT, ADDITIONAL_CSS))
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
