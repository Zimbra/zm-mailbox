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
package com.zimbra.cs.html.owasp.policies;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

public class StyleTagReceiver implements HtmlStreamEventReceiver {

    private static final String STYLE_TAG = "style";
    private static final String STYLE_OPENING_TAG = "<style>";
    private static final String STYLE_CLOSING_TAG = "</style>";
    private static final AntiSamy as = new AntiSamy();
    private static Policy policy = null;
    private final HtmlStreamEventReceiver wrapped;
    private boolean inStyleTag;

    static {
        final String FS = File.separator;
        String antisamyXML = LC.zimbra_home.value() + FS + "conf" + FS + "antisamy.xml";
        File myFile = new File(antisamyXML);
        try {
            URL url = myFile.toURI().toURL();
            policy = Policy.getInstance(url);
        } catch (PolicyException | MalformedURLException e) {
            ZimbraLog.mailbox.warn("Failed to load antisamy policy: %s", e.getMessage());
        }
        ZimbraLog.mailbox.info("Antisamy policy loaded");
    }

    public StyleTagReceiver(HtmlStreamEventReceiver wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void openDocument() {
        wrapped.openDocument();
        inStyleTag = false;
    }

    @Override
    public void closeDocument() {
        wrapped.closeDocument();
    }

    @Override
    public void openTag(String elementName, List<String> attrs) {
        wrapped.openTag(elementName, attrs);
        inStyleTag = STYLE_TAG.equalsIgnoreCase(elementName);
    }

    @Override
    public void closeTag(String elementName) {
        wrapped.closeTag(elementName);
        inStyleTag = false;
    }

    @Override
    public void text(String text) {
        if (inStyleTag) {
            String sanitizedStyle = "";
            if (policy != null) {
                try {
                    sanitizedStyle = as
                        .scan(STYLE_OPENING_TAG + text + STYLE_CLOSING_TAG, policy, AntiSamy.DOM)
                        .getCleanHTML();
                    sanitizedStyle = sanitizedStyle.replace(STYLE_OPENING_TAG, "")
                        .replace(STYLE_CLOSING_TAG, "");
                } catch (ScanException | PolicyException e) {
                    ZimbraLog.mailbox.warn("Failed to sanitize html style element");
                    sanitizedStyle = "";
                }
            }
            wrapped.text(sanitizedStyle);
        } else {
            wrapped.text(text);
        }
    }
}
