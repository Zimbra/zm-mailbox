package com.zimbra.cs.html.owasp;

import java.util.concurrent.Callable;
import org.owasp.html.Handler;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlSanitizer.Policy;
import org.owasp.html.HtmlStreamRenderer;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import com.zimbra.common.util.StringUtil;

/*
 * Task to sanitize HTML code
 */
public class OwaspHtmlSanitizer implements Callable<String> {
    private String html;
    private boolean neuterImages;

    public OwaspHtmlSanitizer(String html, boolean neuterImages) {
        this.html = html;
        this.neuterImages = neuterImages;
    }

    private PolicyFactory POLICY_DEFINITION;

    private void instantiatePolicy() {
        POLICY_DEFINITION = OwaspPolicyProducer.getPolicyFactoryInstance(neuterImages);
        POLICY_DEFINITION = POLICY_DEFINITION.and(Sanitizers.LINKS);
    }

    /**
     * Sanitizing email is treated as an expensive operation; this method should
     * be called from a background Thread.
     * 
     * @return the sanitized form of the <code>html</code>; <code>null</code> if
     *         <code>html</code> was <code>null</code>
     */
    public String sanitize() {
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
        final Policy policy = POLICY_DEFINITION.apply(renderer);
        // run the html through the sanitizer
        HtmlSanitizer.sanitize(html, policy);
        // return the resulting HTML from the builder
        return htmlBuilder.toString();
    }

    @Override
    public String call() throws Exception {
        return sanitize();
    }

}
