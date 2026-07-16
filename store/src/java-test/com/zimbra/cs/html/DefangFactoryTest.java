
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
import static org.junit.Assert.assertFalse;
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
        // Test the specific content type mentioned in the requirement
        assertTrue("application/mathml+xml should be recognized as defang XML content type",
                DefangFactory.isDefangXmlContentType("application/mathml+xml"));
    }

    @Test
    public void testDefangXmlContentTypeRdf() {
        // Test the specific content type mentioned in the requirement
        assertTrue("application/rdf+xml should be recognized as defang XML content type",
                DefangFactory.isDefangXmlContentType("application/rdf+xml"));
    }

    @Test
    public void testAllDefaultDefangXmlContentTypes() {
        // Test all content types from LC.DEFANG_XML_CONTENT_TYPES default value
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
            assertTrue(contentType + " should be recognized as defang XML content type",
                    DefangFactory.isDefangXmlContentType(contentType));
        }
    }

    @Test
    public void testNonDefangXmlContentTypes() {
        // Test content types that should NOT be recognized as defang XML
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
            assertFalse(contentType + " should NOT be recognized as defang XML content type",
                    DefangFactory.isDefangXmlContentType(contentType));
        }
    }

    @Test
    public void testCaseSensitivityDefangXmlContentType() {
        // Test that the method is case sensitive (as it should be)
        assertTrue("application/mathml+xml should be recognized",
                DefangFactory.isDefangXmlContentType("application/mathml+xml"));

        // These should fail if the method is case sensitive
        assertFalse("APPLICATION/mathml+xml should NOT be recognized (wrong case)",
                DefangFactory.isDefangXmlContentType("APPLICATION/mathml+xml"));
        assertFalse("Application/MathMl+xml should NOT be recognized (wrong case)",
                DefangFactory.isDefangXmlContentType("Application/MathMl+xml"));
    }

    @Test
    public void testNullAndEmptyDefangXmlContentType() {
        // Test edge cases
        assertFalse("Null content type should not be recognized",
                DefangFactory.isDefangXmlContentType(null));
        assertFalse("Empty content type should not be recognized",
                DefangFactory.isDefangXmlContentType(""));
        assertFalse("Whitespace content type should not be recognized",
                DefangFactory.isDefangXmlContentType("   "));
    }

    @Test
    public void testPartialMatchesForDefangXmlContentType() {
        // Test that partial matches don't work
        assertFalse("Partial match should not work",
                DefangFactory.isDefangXmlContentType("mathml+xml"));
        assertFalse("Partial match should not work",
                DefangFactory.isDefangXmlContentType("application/mathml"));
        assertFalse("Extra content should not match",
                DefangFactory.isDefangXmlContentType("application/mathml+xml; charset=utf-8"));
    }
}
