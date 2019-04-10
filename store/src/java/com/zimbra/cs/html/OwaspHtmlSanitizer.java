package com.zimbra.cs.html;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.owasp.html.AttributePolicy;
import org.owasp.html.CssSchema;
import org.owasp.html.ElementPolicy;
import org.owasp.html.Handler;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlSanitizer.Policy;
import org.owasp.html.HtmlStreamRenderer;
import org.owasp.html.PolicyFactory;

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.servlet.ZThreadLocal;

class OwaspHtmlSanitizer implements Callable<String> {

    /** The Host header received in the request. */
    private String reqVirtualHost = null;
    /** enable same host post request for a form in email */
    private static boolean sameHostFormPostCheck = DebugConfig.defang_block_form_same_host_post_req;
    // regexes inside of attr values to strip out
    private static final Pattern AV_JS_ENTITY = Pattern.compile(DebugConfig.defangAvJsEntity);
    private static final Pattern AV_SCRIPT_TAG = Pattern.compile(DebugConfig.defangAvScriptTag,
        Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_IMG_FILE = Pattern.compile(DebugConfig.defangValidImgFile);
    private static final Pattern VALID_INT_IMG = Pattern.compile(DebugConfig.defangValidIntImg,
        Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_EXT_URL = Pattern.compile(DebugConfig.defangValidExtUrl,
        Pattern.CASE_INSENSITIVE);
    // matches the file format that convertd uses so it doesn't get removed
    private static final Pattern VALID_CONVERTD_FILE = Pattern
        .compile(DebugConfig.defangValidConvertdFile);

    public OwaspHtmlSanitizer(String html, boolean neuterImages) {
        this.html = html;
        this.neuterImages = neuterImages;
        if (ZThreadLocal.getRequestContext() != null) {
            this.reqVirtualHost = ZThreadLocal.getRequestContext().getVirtualHost();
        }
    }

    /**
     * The following CSS properties do not appear in the default whitelist from
     * OWASP, but they improve the fidelity of the HTML display without
     * unacceptable risk.
     */
    private static final CssSchema ADDITIONAL_CSS = CssSchema
        .withProperties(ImmutableSet.of("float"));

    /**
     * Translates <div> tags surrounding quoted text into
     * <div class="elided-text"> which allows quoted text collapsing in
     * ConversationViewFragment.
     */
    private static final ElementPolicy TRANSLATE_DIV_CLASS = new ElementPolicy() {

        public String apply(String elementName, List<String> attrs) {
            boolean showHideQuotedText = false;
            // check if the class attribute is listed
            final int classIndex = attrs.indexOf("class");
            if (classIndex >= 0) {
                // remove the class attribute and its value
                final String value = attrs.remove(classIndex + 1);
                attrs.remove(classIndex);
                // gmail and yahoo use a specific div class name to indicate
                // quoted text
                showHideQuotedText = "gmail_quote".equals(value) || "yahoo_quoted".equals(value);
            }
            // check if the id attribute is listed
            final int idIndex = attrs.indexOf("id");
            if (idIndex >= 0) {
                // remove the id attribute and its value
                final String value = attrs.remove(idIndex + 1);
                attrs.remove(idIndex);
                // AOL uses a specific id value to indicate quoted text
                showHideQuotedText = value.startsWith("AOLMsgPart");
            }
            // insert a class attribute with a value of "elided-text" to
            // hide/show quoted text
            if (showHideQuotedText) {
                attrs.add("class");
                attrs.add("elided-text");
            }
            return "div";
        }
    };

    private static AttributePolicy IMG_SRC_NEUTER = new AttributePolicy() {

        public String apply(String elementName, String attributeName, String srcValue) {
            if (VALID_EXT_URL.matcher(srcValue).find() || (!VALID_INT_IMG.matcher(srcValue).find()
                && !VALID_IMG_FILE.matcher(srcValue).find())) {
                return null;
            } else if (!VALID_INT_IMG.matcher(srcValue).find()
                && VALID_IMG_FILE.matcher(srcValue).find()
                && !VALID_CONVERTD_FILE.matcher(srcValue).find()) {
                return null;
            }
            return srcValue;
        }
    };

    private static AttributePolicy BG_NEUTER = new AttributePolicy() {

        public String apply(String elementName, String attributeName, String srcValue) {
            return null;
        }
    };

    private static final AttributePolicy NO_SPACE_ENCODED_CHARS = new AttributePolicy() {

        public String apply(String elementName, String attributeName, String value) {
            value = removeAnySpacesAndEncodedChars(value);
            value = AV_JS_ENTITY.matcher(value).replaceAll("JS-ENTITY-BLOCKED");
            value = AV_SCRIPT_TAG.matcher(value).replaceAll("SCRIPT-TAG-BLOCKED");
            return value;
        }
    };

    private AttributePolicy BLOCK_FORM_SAME_HOST_POST_REQ = new AttributePolicy() {

        public String apply(String elementName, String attributeName, String value) {
            if (sameHostFormPostCheck == true && reqVirtualHost != null) {
                try {
                    URL url = new URL(value);
                    String formActionHost = url.getHost().toLowerCase();

                    if (formActionHost.equalsIgnoreCase(reqVirtualHost)) {
                        value = value.replace(formActionHost, "SAMEHOSTFORMPOST-BLOCKED");
                    }
                } catch (MalformedURLException e) {
                    ZimbraLog.soap
                        .warn("Failure while trying to block malicious code. Check for URL"
                            + " match between the host and the action URL of a FORM."
                            + " Error parsing URL, possible relative URL." + e.getMessage());
                }
            }
            return value;
        }
    };

    private PolicyFactory POLICY_DEFINITION;

    /**
     * This sanitizer policy removes these elements and the content within:
     * <ul>
     * <li>APPLET</li>
     * <li>FRAMESET</li>
     * <li>OBJECT</li>
     * <li>SCRIPT</li>
     * <li>STYLE</li>
     * <li>TITLE</li>
     * </ul>
     *
     * This sanitizer policy removes these elements but preserves the content
     * within:
     * <ul>
     * <li>BASEFONT</li>
     * <li>FRAME</li>
     * <li>HEAD</li>
     * <li>IFRAME</li>
     * <li>ISINDEX</li>
     * <li>LINK</li>
     * <li>META</li>
     * <li>NOFRAMES</li>
     * <li>PARAM</li>
     * <li>NOSCRIPT</li>
     * </ul>
     *
     * This sanitizer policy removes these attributes from all elements:
     * <ul>
     * <li>code</li>
     * <li>codebase</li>
     * <li>id</li>
     * <li>for</li>
     * <li>headers</li>
     * <li>onblur</li>
     * <li>onchange</li>
     * <li>onclick</li>
     * <li>ondblclick</li>
     * <li>onfocus</li>
     * <li>onkeydown</li>
     * <li>onkeypress</li>
     * <li>onkeyup</li>
     * <li>onload</li>
     * <li>onmousedown</li>
     * <li>onmousemove</li>
     * <li>onmouseout</li>
     * <li>onmouseover</li>
     * <li>onmouseup</li>
     * <li>onreset</li>
     * <li>onselect</li>
     * <li>onsubmit</li>
     * <li>onunload</li>
     * <li>tabindex</li>
     * </ul>
     */

    private void instantiatePolicy() {
        POLICY_DEFINITION = new HtmlPolicyBuilder().allowAttributes("dir")
            .matching(true, "ltr", "rtl").globally()
            .allowStyling(CssSchema.union(CssSchema.DEFAULT, ADDITIONAL_CSS))
            .disallowTextIn("applet", "frameset", "object", "script", "title").allowElements("a")
            .allowAttributes("coords", "href", "name", "shape").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("a").allowElements("abbr").allowAttributes("title").onElements("abbr")
            .allowElements("acronym").allowAttributes("title").onElements("acronym")
            .allowElements("address").allowElements("area")
            .allowAttributes("alt", "coords", "href", "nohref", "name", "shape")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("area")
            .allowUrlProtocols("cid", "http", "https", "mailto", "data").allowElements("article")
            .allowElements("aside").allowElements("b").allowElements("base").allowAttributes("href")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("base").allowElements("bdi")
            .allowAttributes("dir").matching(NO_SPACE_ENCODED_CHARS).onElements("bdi")
            .allowElements("bdo").allowAttributes("dir").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("bdo").allowElements("big").allowElements("blockquote")
            .allowAttributes("cite").matching(NO_SPACE_ENCODED_CHARS).onElements("blockquote")
            .allowElements("body").allowElements("br").allowAttributes("clear")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("br").allowElements("button")
            .allowAttributes("autofocus", "disabled", "form", "formaction", "formenctype",
                "formmethod", "formnovalidate", "formtarget", "name", "type", "value")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("button").allowElements("canvas")
            .allowAttributes("width", "height").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("canvas").allowElements("caption").allowAttributes("align")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("caption").allowElements("center")
            .allowElements("cite").allowElements("code").allowElements("col")
            .allowAttributes("align", "bgcolor", "char", "charoff", "span", "valign", "width")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("col").allowElements("colgroup")
            .allowAttributes("align", "char", "charoff", "span", "valign", "width")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("colgroup").allowElements("datalist")
            .allowElements("dd").allowElements("del").allowAttributes("cite", "datetime")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("del").allowElements("details")
            .allowElements("dfn").allowElements("dir").allowAttributes("compact")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("dir")
            .allowElements(TRANSLATE_DIV_CLASS, "div").allowAttributes("align", "class", "id")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("div").allowAttributes("background")
            .matching(BG_NEUTER).matching(NO_SPACE_ENCODED_CHARS).onElements("div")
            .allowElements("dl").allowElements("dt").allowElements("em").allowElements("fieldset")
            .allowAttributes("disabled", "form", "name").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("fieldset").allowElements("figcaption").allowElements("figure")
            .allowElements("font").allowAttributes("color", "face", "size")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("font").allowElements("footer")
            .allowElements("form")
            .allowAttributes("accept", "action", "accept-charset", "autocomplete", "enctype",
                "method", "name", "novalidate", "target")
            .matching(NO_SPACE_ENCODED_CHARS).matching(BLOCK_FORM_SAME_HOST_POST_REQ)
            .onElements("form").allowElements("header").allowElements("html").allowElements("h1")
            .allowAttributes("align").matching(NO_SPACE_ENCODED_CHARS).onElements("h1")
            .allowElements("h2").allowAttributes("align").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("h2").allowElements("h3").allowAttributes("align")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("h3").allowElements("h4")
            .allowAttributes("align").matching(NO_SPACE_ENCODED_CHARS).onElements("h4")
            .allowElements("h5").allowAttributes("align").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("h5").allowElements("h6").allowAttributes("align")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("h6").allowElements("hr")
            .allowAttributes("align", "noshade", "size", "width").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("hr").allowElements("i").allowElements("img")
            .allowAttributes("align", "alt", "border", "crossorigin", "height", "hspace", "ismap",
                "longdesc", "usemap", "vspace", "width")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("img").allowAttributes("src")
            .matching(IMG_SRC_NEUTER).matching(NO_SPACE_ENCODED_CHARS).onElements("img")
            .allowElements("input")
            .allowAttributes("accept", "align", "alt", "autocomplete", "autofocus", "checked",
                "disabled", "form", "formaction", "formenctype", "formmethod", "formnovalidate",
                "formtarget", "height", "list", "max", "maxlength", "min", "multiple", "name",
                "pattern", "placeholder", "readonly", "required", "size", "step", "type", "value",
                "width")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("input").allowAttributes("src")
            .matching(NO_SPACE_ENCODED_CHARS).matching(IMG_SRC_NEUTER).onElements("input")
            .allowElements("ins").allowAttributes("cite", "datetime")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("ins").allowElements("kbd")
            .allowElements("keygen")
            .allowAttributes("autofocus", "challenge", "disabled", "form", "keytype", "name")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("keygen").allowElements("label")
            .allowAttributes("form").matching(NO_SPACE_ENCODED_CHARS).onElements("label")
            .allowElements("legend").allowAttributes("align").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("legend").allowElements("li").allowAttributes("type", "value")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("li").allowElements("main")
            .allowElements("map").allowAttributes("name").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("map").allowElements("mark").allowElements("menu")
            .allowAttributes("label", "type").matching(NO_SPACE_ENCODED_CHARS).onElements("menu")
            .allowElements("menuitem")
            .allowAttributes("checked", "command", "default", "disabled", "icon", "label", "type",
                "radiogroup")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("menuitem").allowElements("meter")
            .allowAttributes("form", "high", "low", "max", "min", "optimum", "value")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("meter").allowElements("nav")
            .allowElements("ol").allowAttributes("compact", "reversed", "start", "type")
            .onElements("ol").allowElements("optgroup").allowAttributes("disabled", "label")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("optgroup").allowElements("option")
            .allowAttributes("disabled", "label", "selected", "value")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("option").allowElements("output")
            .allowAttributes("form", "name").matching(NO_SPACE_ENCODED_CHARS).onElements("output")
            .allowElements("p").allowAttributes("align").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("p").allowElements("pre").allowAttributes("width")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("pre").allowElements("progress")
            .allowAttributes("max", "value").matching(NO_SPACE_ENCODED_CHARS).onElements("progress")
            .allowElements("q").allowAttributes("cite").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("q").allowElements("rp").allowElements("rt").allowElements("ruby")
            .allowElements("s").allowElements("samp").allowElements("section")
            .allowElements("select")
            .allowAttributes("autofocus", "disabled", "form", "multiple", "name", "required",
                "size")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("select").allowElements("small")
            .allowElements("span").allowElements("strike").allowElements("strong")
            .allowElements("sub").allowElements("summary").allowElements("sup")
            .allowElements("table")
            .allowAttributes("align", "bgcolor", "border", "cellpadding", "cellspacing", "frame",
                "rules", "sortable", "summary", "width")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("table").allowElements("tbody")
            .allowAttributes("align", "char", "charoff", "valign").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("tbody").allowElements("td")
            .allowAttributes("abbr", "align", "axis", "bgcolor", "char", "charoff", "colspan",
                "height", "nowrap", "rowspan", "scope", "valign", "width")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("td").allowElements("textarea")
            .allowAttributes("autofocus", "cols", "disabled", "form", "maxlength", "name",
                "placeholder", "readonly", "required", "rows", "wrap")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("textarea").allowElements("tfoot")
            .allowAttributes("align", "char", "charoff", "valign").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("tfoot").allowElements("th")
            .allowAttributes("abbr", "align", "axis", "bgcolor", "char", "charoff", "colspan",
                "height", "nowrap", "rowspan", "scope", "sorted", "valign", "width")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("th").allowElements("thead")
            .allowAttributes("align", "char", "charoff", "valign").matching(NO_SPACE_ENCODED_CHARS)
            .onElements("thead").allowElements("time").allowAttributes("datetime")
            .onElements("time").allowElements("tr")
            .allowAttributes("align", "bgcolor", "char", "charoff", "valign")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("tr").allowElements("tt")
            .allowElements("u").allowElements("ul").allowAttributes("compact", "type")
            .matching(NO_SPACE_ENCODED_CHARS).onElements("ul").allowElements("var")
            .allowElements("wbr").toFactory();
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
        if (!neuterImages) {
            IMG_SRC_NEUTER = null;
            BG_NEUTER = null;
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

    private static String removeAnySpacesAndEncodedChars(String result) {
        String sanitizedStr = result;
        StringBuilder sb = new StringBuilder();
        int index = result.indexOf(":");
        if (index > -1) {
            String jsString = result.substring(0, index);
            char[] chars = jsString.toCharArray();
            for (int i = 0; i < chars.length; ++i) {
                if (!Character.isSpace(chars[i])) {
                    sb.append(chars[i]);
                }
            }
        }
        String temp = sb.toString();
        temp = StringEscapeUtils.unescapeHtml(temp);
        if (index != -1 && (temp.toLowerCase().contains("javascript")
            || temp.toLowerCase().contains("vbscript"))) {
            sanitizedStr = temp + result.substring(index);
        }
        return sanitizedStr;
    }

    private String html;
    private boolean neuterImages;

    @Override
    public String call() throws Exception {
        return sanitize();
    }
}
