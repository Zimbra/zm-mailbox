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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;

import org.owasp.html.Handler;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlSanitizer.Policy;
import org.owasp.html.HtmlStreamRenderer;
import org.owasp.html.PolicyFactory;
import org.w3c.tidy.Tidy;

import com.zimbra.common.localconfig.DebugConfig;
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
        return processSanitization(true).toString();
    }

    public String sanitize(boolean cleanData) throws UnsupportedEncodingException {
        return processSanitization(cleanData).toString();
    }

    private StringBuilder processSanitization(boolean cleanData) throws UnsupportedEncodingException {
        OwaspThreadLocal threadLocalInstance = new OwaspThreadLocal();
        threadLocalInstance.setVHost(vHost);
        OwaspHtmlSanitizer.zThreadLocal.set(threadLocalInstance);
        if (StringUtil.isNullOrEmpty(html)) {
            return null;
        }
        // create the builder into which the sanitized email will be written
        final StringBuilder htmlBuilder = new StringBuilder(html.length());
        // create the renderer that will write the sanitized HTML to the builder
        final HtmlStreamRenderer renderer = HtmlStreamRenderer.create(htmlBuilder,
                Handler.PROPAGATE,
                // log errors resulting from exceptionally bizarre inputs
                new Handler<String>() {

            public void handle(final String x) {
                throw new AssertionError(x);
            }
        });
        // create a thread-specific policy
        instantiatePolicy();
        final Policy policy = POLICY_DEFINITION.apply(new StyleTagReceiver(renderer));
        // run the html through the sanitizer
        runSanitizer(html, policy, cleanData);
        // return the resulting HTML from the builder
        OwaspHtmlSanitizer.zThreadLocal.remove();
        return htmlBuilder;
    }

    private void runSanitizer(String str, Policy policy, boolean cleanData) throws UnsupportedEncodingException {
        if (cleanData) {
            HtmlSanitizer.sanitize(cleanMalformedHtml(str, false), policy);
        } else {
            HtmlSanitizer.sanitize(str, policy);
        }
    }

    public String cleanMalformedHtml(String str, boolean printBodyOnly) throws UnsupportedEncodingException {
        if (DebugConfig.jtidyEnabled) {
            long startTime = System.currentTimeMillis();
            ZimbraLog.mailbox.debug("Start - Using JTidy library for cleaning the markup.");
            Tidy tidy = new Tidy();
            tidy.setInputEncoding("UTF-8");
            tidy.setOutputEncoding("UTF-8");
            tidy.setPrintBodyOnly(printBodyOnly);
            tidy.setXHTML(true);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(str.getBytes("UTF-8"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            tidy.parseDOM(inputStream, outputStream);
            String outStream = outputStream.toString("UTF-8");
            if (outStream == null || outStream.trim().isEmpty()) {
                return str;
            }
            ZimbraLog.mailbox.debug("End - Using JTidy library for cleaning the markup. Taken %d milliseconds.",
                    (System.currentTimeMillis() - startTime));
            return outStream;
        }
        return str;
    }

    @Override
    public String call() throws Exception {
        return sanitize();
    }

}
