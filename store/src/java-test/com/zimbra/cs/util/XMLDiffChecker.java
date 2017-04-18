package com.zimbra.cs.util;

import org.junit.Assert;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

public class XMLDiffChecker {

    public static void assertXMLEquals(String expected, String actual) {
        Diff myDiff = DiffBuilder.compare(Input.fromString(expected))
            .withTest(Input.fromString(actual)).ignoreWhitespace().build();
        Assert.assertFalse(myDiff.toString(), myDiff.hasDifferences());
    }
}
