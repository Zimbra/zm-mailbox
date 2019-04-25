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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.util.ZimbraLog;

/*
 * Read html elemets, attributes and other owasp configuration from policy file
 * Populate them in respective map/set
 */
public class OwaspPolicy {

    public static final String E_OWASP_POLICY = "owasp_policy";
    public static final String E_DISALLOW_TEXT_IN = "disallow_text_in";
    public static final String E_URL_PROTOCOLS = "url_protocols";
    public static final String E_HTML = "html";
    public static final String A_ELEMENT = "element";
    public static final String A_REMOVE_TEXT = "removeText";
    public static final String E_ATTRIBUTES = "attributes";
    private static String mPolicyFile;
    /**
     * The singleton instance. This is a volatile variable, so that we can
     * reload the policy file on the fly without locking.
     */
    private static volatile OwaspPolicy mOwaspPolicy;
    private static final Map<String, String> mConfiguredElements = new HashMap<String, String>();
    private static final Set<String> mDisallowTextElements = new HashSet<String>();
    private static final Set<String> mURLProtocols = new HashSet<String>();

    static {
        try {
            load(null);
        } catch (DocumentException de) {
            throw new RuntimeException(de);
        } catch (Exception ce) {
            throw new RuntimeException(ce);
        }
    }

    private OwaspPolicy(String file) throws DocumentException, Exception {
        mPolicyFile = file;
        if (mPolicyFile == null) {
            mPolicyFile = defaultPolicyFile();
        }
        File policyFile = new File(mPolicyFile);
        if (policyFile.exists() && policyFile.canRead()) {
            try (FileInputStream fis = new FileInputStream(policyFile)) {
                Document document = W3cDomUtil.parseXMLToDom4jDocUsingSecureProcessing(fis);
                Element root = document.getRootElement();
                if (!root.getName().equals(E_OWASP_POLICY)) {
                    ZimbraLog.mailbox
                        .warn(String.format("OWASP policy file '%s' root tag is not '%s'",
                            mPolicyFile, E_OWASP_POLICY));
                    throw new DocumentException(
                        String.format("OWASP policy file '%s' root tag is not '%s'", mPolicyFile, E_OWASP_POLICY));
                }
                for (Iterator<?> iter = root.elementIterator(E_HTML); iter.hasNext();) {
                    Element html = (Element) iter.next();
                    String element = html.attributeValue(A_ELEMENT);
                    String attributes = html.elementText(E_ATTRIBUTES);
                    attributes = attributes.replace("CORE", "id,class,title,style")
                        .replace("LANG", "dir,lang,xml:lang").replace("KBD", "accesshtml,tabindex");
                    set(element, attributes);
                }
                String disallowTextElements = root.elementText(E_DISALLOW_TEXT_IN);
                mDisallowTextElements.addAll(Arrays.asList(disallowTextElements.split(COMMA)));
                String urlProtocols = root.elementText(E_URL_PROTOCOLS);
                mURLProtocols.addAll(Arrays.asList(urlProtocols.split(COMMA)));
            } catch (IOException | XmlParseException e) {
                ZimbraLog.mailbox
                    .warn(String.format("Problem parsing owasp policy file '%s'", policyFile), e);
                throw new DocumentException(
                    String.format("Problem parsing owasp policy file '%s'", policyFile));
            }
        } else {
            ZimbraLog.mailbox
                .warn(String.format("Owasp policy file '%s' is not readable", mPolicyFile));
        }
    }

    private String defaultPolicyFile() {
        final String FS = File.separator;
        return LC.zimbra_home.value() + FS + "conf" + FS + "owasp_policy.xml";
    }

    private void set(String key, String value) {
        mConfiguredElements.put(key, value);
    }

    static OwaspPolicy getInstance() {
        return mOwaspPolicy;
    }

    /**
     * Loads the OWASP policy file.
     *
     * @param path policy file path or null to use the default path
     * @throws DocumentException if the policy file was syntactically invalid
     * @throws Exception if the policy file was semantically invalid
     *             
     */
    static synchronized void load(String path) throws DocumentException, Exception {
        mOwaspPolicy = new OwaspPolicy(path);
        ZimbraLog.mailbox.info("OWASP policy '%s' loaded", mPolicyFile);
    }

    public static Set<String> getAllowedElements() {
        return mConfiguredElements.keySet();
    }

    public static String getAttributes(String element) {
        return mConfiguredElements.get(element);
    }

    public static Set<String> getDisallowTextElements() {
        return mDisallowTextElements;
    }

    public static Set<String> getURLProtocols() {
        return mURLProtocols;
    }

}
