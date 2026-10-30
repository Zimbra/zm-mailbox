
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.html;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for DefangFactory.isDefangXmlContentType method.
 * This test specifically verifies that the method correctly identifies
 * application/mathml+xml and application/rdf+xml as defang XML content types,
 * which is used in NativeFormatter.handleMessagePart method.
 */
public class DefangFactoryTest {

    @Test
    public void testDefangXmlContentTypeMathMl() {
        assertTrue("application/mathml+xml should be recognized as a defang XML content type",
                DefangFactory.isDefangXmlContentType("application/mathml+xml"));
    }

    @Test
    public void testDefangXmlContentTypeRdf() {
        assertTrue("application/rdf+xml should be recognized as a defang XML content type",
                DefangFactory.isDefangXmlContentType("application/rdf+xml"));
    }

    @Test
    public void testAllDefaultDefangXmlContentTypes() {
        String[] defangXmlTypes = {
                "application/rss+xml",
                "application/atom+xml",
                "application/svg+xml",
                "application/mathml+xml",
                "application/xhtml+xml",
                "application/xslt+xml",
                "application/xsd+xml",
                "application/gml+xml",
                "application/rdf+xml"
        };

        for (String contentType : defangXmlTypes) {
            assertTrue(contentType + " should be recognized as a defang XML content type",
                    DefangFactory.isDefangXmlContentType(contentType));
        }
    }

    @Test
    public void testNonDefangXmlContentTypes() {
        String[] nonDefangTypes = {
                "text/plain",
                "text/html",
                "text/xml",  // Note: text/xml is handled separately, not through defang XML types
                "application/pdf",
                "image/jpeg",
                "image/png",
                "application/octet-stream",
                "application/json",
                "application/javascript"
        };

        for (String contentType : nonDefangTypes) {
            assertFalse(contentType + " should NOT be recognized as a defang XML content type",
                    DefangFactory.isDefangXmlContentType(contentType));
        }
    }

    @Test
    public void testCaseSensitivityDirectCall() {
        assertTrue("application/mathml+xml should be recognized",
                DefangFactory.isDefangXmlContentType("application/mathml+xml"));

        assertFalse("APPLICATION/MATHML+XML should NOT match when passed directly (not normalized)",
                DefangFactory.isDefangXmlContentType("APPLICATION/MATHML+XML"));
        assertFalse("Application/MathMl+Xml should NOT match when passed directly (not normalized)",
                DefangFactory.isDefangXmlContentType("Application/MathMl+Xml"));
        assertFalse("APPLICATION/rdf+xml should NOT match when passed directly (not normalized)",
                DefangFactory.isDefangXmlContentType("APPLICATION/rdf+xml"));
    }

    @Test
    public void testNullAndEmptyStringsDirectCall() {
        assertFalse("Null should not be recognized as a defang XML content type",
                DefangFactory.isDefangXmlContentType(null));
        assertFalse("Empty string should not be recognized as a defang XML content type",
                DefangFactory.isDefangXmlContentType(""));
        assertFalse("Whitespace-only string should not be recognized as a defang XML content type",
                DefangFactory.isDefangXmlContentType("   "));
    }

    @Test
    public void testPartialMatchesDirectCall() {
        assertFalse("'mathml+xml' (no type prefix) should not match",
                DefangFactory.isDefangXmlContentType("mathml+xml"));

        assertFalse("'application/mathml' (no +xml suffix) should not match",
                DefangFactory.isDefangXmlContentType("application/mathml"));

        assertFalse("'application/mathml+xml; charset=utf-8' should not match when called directly",
                DefangFactory.isDefangXmlContentType("application/mathml+xml; charset=utf-8"));

        assertFalse("'application/rdf+xml; charset=utf-8' should not match when called directly",
                DefangFactory.isDefangXmlContentType("application/rdf+xml; charset=utf-8"));
    }

    @Test
    public void testGetDefangerNeverReturnsNull() {
        assertNotNull("getDefanger(null) must not return null",
                DefangFactory.getDefanger(null));
        assertNotNull("getDefanger(\"\") must not return null",
                DefangFactory.getDefanger(""));
        assertNotNull("getDefanger(\"text/html\") must not return null",
                DefangFactory.getDefanger("text/html"));
        assertNotNull("getDefanger(\"application/mathml+xml\") must not return null",
                DefangFactory.getDefanger("application/mathml+xml"));
    }

    @Test
    public void testGetDefangerNormalizesCase() {
        BrowserDefang defangerLower  = DefangFactory.getDefanger("application/mathml+xml");
        BrowserDefang defangerUpper  = DefangFactory.getDefanger("APPLICATION/MATHML+XML");
        BrowserDefang defangerMixed  = DefangFactory.getDefanger("Application/MathMl+Xml");

        assertNotNull(defangerLower);
        assertNotNull(defangerUpper);
        assertNotNull(defangerMixed);
        assertEquals("Uppercase APPLICATION/MATHML+XML must resolve to the same defanger as lowercase",
                defangerLower.getClass(), defangerUpper.getClass());
        assertEquals("Mixed-case Application/MathMl+Xml must resolve to the same defanger as lowercase",
                defangerLower.getClass(), defangerMixed.getClass());
    }

    @Test
    public void testGetDefangerStripsParametersBeforeLookup() {
        BrowserDefang defangerPlain       = DefangFactory.getDefanger("application/mathml+xml");
        BrowserDefang defangerWithCharset = DefangFactory.getDefanger("application/mathml+xml; charset=utf-8");
        BrowserDefang defangerRdfParam    = DefangFactory.getDefanger("application/rdf+xml; charset=utf-8");

        assertNotNull(defangerWithCharset);
        assertNotNull(defangerRdfParam);

        assertEquals("'application/mathml+xml; charset=utf-8' must resolve to the same defanger as " +
                "'application/mathml+xml'", defangerPlain.getClass(), defangerWithCharset.getClass());
        assertEquals("'application/rdf+xml; charset=utf-8' must resolve to xhtmlDefang", defangerPlain.getClass(),
                defangerRdfParam.getClass());
    }

    @Test
    public void testGetDefangerBuiltInXmlTypes() {
        BrowserDefang xhtmlDefangClass = DefangFactory.getDefanger("text/xml");
        assertNotNull(xhtmlDefangClass);

        String[] builtInXmlTypes = {
                "text/xml",
                "application/xhtml+xml",
                "image/svg+xml",
                "TEXT/XML",
                "IMAGE/SVG+XML"
        };

        for (String ct : builtInXmlTypes) {
            BrowserDefang d = DefangFactory.getDefanger(ct);
            assertNotNull("getDefanger should not return null for: " + ct, d);
            assertEquals("'" + ct + "' should resolve to the same defanger class as text/xml",
                    xhtmlDefangClass.getClass(), d.getClass());
        }
    }

    @Test
    public void testGetDefangerFallsBackToNoopForUnknownTypes() {
        BrowserDefang noopClass = DefangFactory.getDefanger(null);

        String[] unknownTypes = {
                "application/pdf",
                "image/jpeg",
                "image/png",
                "application/octet-stream",
                "application/json",
                "text/plain"
        };

        for (String ct : unknownTypes) {
            BrowserDefang d = DefangFactory.getDefanger(ct);
            assertNotNull("getDefanger should not return null for: " + ct, d);
            assertEquals("'" + ct + "' should fall back to noopDefang", noopClass.getClass(), d.getClass());
        }
    }
}