/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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
package com.zimbra.cs.lmtpserver;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

import com.zimbra.common.localconfig.LC;

/**
 * ExternalEmailWarning is a singleton class that provides assorted utilities
 * for the External Email Warning functionality.
 * 
 * @author telus
 *
 */
public class ExternalEmailWarning {

    private static ExternalEmailWarning instance;
    private boolean isEnabled;
    private String baseWarningMessage;
    private String textPlainWarning;
    private String textHtmlWarning;
    private String encodedTextPlainWarning;
    private String encodedTextHtmlWarning;

    private ExternalEmailWarning() {
        isEnabled = LC.zimbra_external_email_warning_enabled.booleanValue();
        baseWarningMessage = LC.zimbra_external_email_warning_message.value();
        textPlainWarning = StringEscapeUtils.escapeHtml(baseWarningMessage) + "\r\n";
        textHtmlWarning = "<div><p>" + baseWarningMessage + "</p><hr/></div>";
        encodedTextPlainWarning = Base64.getEncoder().encodeToString(textPlainWarning.getBytes());
        encodedTextHtmlWarning = Base64.getEncoder().encodeToString(textHtmlWarning.getBytes());
    }

    public static ExternalEmailWarning getInstance() {
        if (instance == null) {
            synchronized (ExternalEmailWarning.class) {
                if (instance == null) {
                    instance = new ExternalEmailWarning();
                }
            }
        }
        return instance;
    }

    /**
     * Get whether External Email Warning feature is enabled.
     * 
     * @return true if EEW feature is enabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Get configured warning to be used on text/html mime message parts.
     * 
     * @return string with warning note.
     */
    public String getTextHtmlWarning() {
        return textHtmlWarning;
    }

    /**
     * Get configured warning to be used on text/plain mime message parts.
     * 
     * @return string with warning note.
     */
    public String getTextPlainWarning() {
        return textPlainWarning;
    }
    public String getEncodedTextPlainWarning() {
        return encodedTextPlainWarning;
    }
    public String getEncodedTextHtmlWarning() {
        return encodedTextHtmlWarning;
    }
    private static final String EMAIL_REGEX = "^(.+)@(\\S+)$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /**
     * Finds whether a originator is external to a receiver's organization based on
     * the string representation of their addresses.
     * 
     * If their domains are equal or at least one is sub-domain to the other, then
     * the addresses are considered as intra-organization; otherwise, the addresses
     * are considered as extra-organization.
     * 
     * This method works under an optimistic behavior: shall any of the arguments
     * represent an invalid email address, or shall anything goes wrong in any other
     * aspect with the analysis, it will assume that the originator is not external
     * and thus return false.
     * 
     * Please note that no actual directory (LDAP) validations are done here, just
     * analysis of two independent as-is strings.
     * 
     * @param receiverAddress
     *            receiver's email address in RFC5322 format: name@domain.tld
     * 
     * @param originatorAddress
     *            originator's email address in RFC5322 format: name@domain.tld
     * 
     * @return true if originatorAddress is external to receiverAddress domain
     */
    public boolean isExternal(String receiverAddress, String originatorAddress) {
        // finding receiverAddress validity
        final Matcher receiverAddressMatcher = EMAIL_PATTERN.matcher(receiverAddress);
        if (receiverAddressMatcher.matches()) {
            final String receiverDomain = receiverAddressMatcher.group(2);
            // finding originatorAddress validity
            final Matcher originatorAddressMatcher = EMAIL_PATTERN.matcher(originatorAddress);
            if (originatorAddressMatcher.matches()) {
                final String originatorDomain = originatorAddressMatcher.group(2);
                // checking domain names are correctly identified for added safety
                if (receiverDomain != null && originatorDomain != null) {
                    // splitting domain names into segments
                    final List<String> receiverDomainSegments = Arrays.asList(receiverDomain.split("\\."));
                    final List<String> originatorDomainSegments = Arrays.asList(originatorDomain.split("\\."));
                    // find if domains are equal or at least one is sub-domain to the other
                    final int minSize = Math.min(receiverDomainSegments.size(), originatorDomainSegments.size());
                    final int rdsOffset = receiverDomainSegments.size() - minSize;
                    final int odsOffset = originatorDomainSegments.size() - minSize;
                    for (int i = 0; i < minSize; i++) {
                        final String receiverDomainSegment = receiverDomainSegments.get(i + rdsOffset);
                        final String originatorDomainSegment = originatorDomainSegments.get(i + odsOffset);
                        if (!receiverDomainSegment.equalsIgnoreCase(originatorDomainSegment))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    // message must be RTF822 compliant: CRLFCRLF sequence is required to determine
    // where does the header meet the body of the text/plain part
    private static final String CONTENT_TYPE_TEXT_PLAIN_REGEX = "Content-Type: text/plain.*?\\r\\n\\r\\n";
    private static final Pattern CONTENT_TYPE_TEXT_PLAIN_PATTERN = Pattern.compile(CONTENT_TYPE_TEXT_PLAIN_REGEX,
            Pattern.CASE_INSENSITIVE + Pattern.DOTALL);

    // message must be RTF822 compliant: CRLFCRLF sequence is required to determine
    // where does the header meet the body of the text/html part
    private static final String CONTENT_TYPE_TEXT_HTML_REGEX = "Content-Type: text/html.*?\\r\\n\\r\\n.*?<body.*?>";
    private static final Pattern CONTENT_TYPE_TEXT_HTML_PATTERN = Pattern.compile(CONTENT_TYPE_TEXT_HTML_REGEX,
            Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
    private static final String CONTENT_TYPE_TEXT_HTML_WITHOUT_BODY_TAG_REGEX = "Content-Type: text/html.*?\\r\\n\\r\\n";
    private static final Pattern CONTENT_TYPE_TEXT_HTML_WIHOUT_BODY_TAG_PATTERN = Pattern.compile(CONTENT_TYPE_TEXT_HTML_WITHOUT_BODY_TAG_REGEX,
            Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
    private static final String CONTENT_TRANSFER_BASE64_ENCODING = "Content-Transfer-Encoding: base64";
    /**
     * Updates the string representation of a mime message in RFC822 format with the
     * warning note for text/plain and text/html parts.
     * 
     * This method requires the content to be RFC822 compliant, including that CRLF
     * characters are used as line separators as established in the standard.
     * Similarly, each part header should start with Content-Type and end with
     * CRLFCRLF as established in the standard, in order for this method to properly
     * parse and update the content.
     * 
     * @param content
     *            string representation of mime message in RFC822 format
     * @return updated string with warning note
     */
    public String getUpdatedContent(String content) {
        if (content != null) {
            final Matcher ctTextPlainMatcher = CONTENT_TYPE_TEXT_PLAIN_PATTERN.matcher(content);
            if (ctTextPlainMatcher.find()) {
                String textPlainHeader = ctTextPlainMatcher.group();
                final int end = ctTextPlainMatcher.end();
                if (textPlainHeader != null && textPlainHeader.contains(CONTENT_TRANSFER_BASE64_ENCODING)) {
                    content = appendWarning(content, end, getEncodedTextPlainWarning());
                } else {
                    content = appendWarning(content, end, getTextPlainWarning());
                }
            }
            final Matcher ctTextHtmlMatcher = CONTENT_TYPE_TEXT_HTML_PATTERN.matcher(content);
            final Matcher ctTextHtmlWithoutBodyTagMatcher = CONTENT_TYPE_TEXT_HTML_WIHOUT_BODY_TAG_PATTERN.matcher(content);
            if (ctTextHtmlMatcher.find()) {
                final int end = ctTextHtmlMatcher.end();
                content = appendWarning(content, end, getTextHtmlWarning());
            } else if (ctTextHtmlWithoutBodyTagMatcher.find()) {
                String textHtmlHeader = ctTextHtmlWithoutBodyTagMatcher.group();
                final int end = ctTextHtmlWithoutBodyTagMatcher.end();
                if (textHtmlHeader != null && textHtmlHeader.contains(CONTENT_TRANSFER_BASE64_ENCODING)) {
                    content = appendWarning(content, end, getEncodedTextHtmlWarning());
                } else {
                    content = appendWarning(content, end, getTextHtmlWarning());
                }
            }
        }
        return content;
    }
    public String appendWarning(String content, int index, String warning) {
        final StringBuilder sb = new StringBuilder();
        sb.append(content.substring(0, index));
        if (warning != null) {
            sb.append(warning);
        }
        sb.append(content.substring(index));
        content = sb.toString();
        return content;
    }
}
