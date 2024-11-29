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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.owasp.html.Handler;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlSanitizer.Policy;
import org.owasp.html.ExtensibleHtmlStreamRenderer;
import org.owasp.html.PolicyFactory;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.html.owasp.policies.StyleTagReceiver;

/*
 * Task to sanitize HTML code
 */
public class OwaspHtmlSanitizer implements Callable<String> {
    private String html;
    private boolean neuterImages;
    public static final ThreadLocal<OwaspThreadLocal> zThreadLocal = new ThreadLocal<OwaspThreadLocal>();
    private String vHost;

    public OwaspHtmlSanitizer(String html, boolean neuterImages, String vHost) {
        // fix to check open tags & remove
        this.html = html;
        this.neuterImages = neuterImages;
        this.vHost = vHost;
    }

    private PolicyFactory POLICY_DEFINITION;

    private void instantiatePolicy() {
        POLICY_DEFINITION = OwaspPolicyProducer.getPolicyFactoryInstance(neuterImages);
    }

    /**
     * Sanitizing email is treated as an expensive operation; this method should
     * be called from a background Thread.
     * 
     * @return the sanitized form of the <code>html</code>; <code>null</code> if
     *         <code>html</code> was <code>null</code>
     * @throws UnsupportedEncodingException
     */
    public String sanitize() throws UnsupportedEncodingException {
        OwaspThreadLocal threadLocalInstance = new OwaspThreadLocal();
        threadLocalInstance.setVHost(vHost);
        OwaspHtmlSanitizer.zThreadLocal.set(threadLocalInstance);
        if (StringUtil.isNullOrEmpty(html)) {
            return null;
        }
        // create the builder into which the sanitized email will be written
        final StringBuilder htmlBuilder = new StringBuilder(html.length());

        boolean zimbraStrictUnclosedCommentTag = LC.zimbra_strict_unclosed_comment_tag.booleanValue();
        Set<String> zimbraSkipTagsWithUnclosedCdata = getSkipTagsUnclosedCdata();
        // create the renderer that will write the sanitized HTML to the builder
        final ExtensibleHtmlStreamRenderer renderer = ExtensibleHtmlStreamRenderer.create(htmlBuilder,
                Handler.PROPAGATE,
                // log errors resulting from exceptionally bizarre inputs
                new Handler<String>() {

            public void handle(final String x) {
                throw new AssertionError(x);
            }
        }, zimbraStrictUnclosedCommentTag, zimbraSkipTagsWithUnclosedCdata);
        // create a thread-specific policy
        instantiatePolicy();
        final Policy policy = POLICY_DEFINITION.apply(new StyleTagReceiver(renderer));
        // run the html through the sanitizer
        HtmlSanitizer.sanitize(html, policy);
        // return the resulting HTML from the builder
        OwaspHtmlSanitizer.zThreadLocal.remove();
        return htmlBuilder.toString();
    }

    @Override
    public String call() throws Exception {
        return sanitize();
    }

    protected Set<String> getSkipTagsUnclosedCdata() {
        Set<String> zimbraSkipTagsWithUnclosedCdata = new HashSet<>();
        try {
            zimbraSkipTagsWithUnclosedCdata = new HashSet<>(
                    Arrays.asList(LC.zimbra_skip_tags_with_unclosed_cdata.value().toString().split(",")));
        } catch (NullPointerException ex) {
            ZimbraLog.mailbox.warn("Issue getting local config value of zimbra_skip_tags_with_unclosed_cdata ", ex);
        }
        return zimbraSkipTagsWithUnclosedCdata;
    }
}
