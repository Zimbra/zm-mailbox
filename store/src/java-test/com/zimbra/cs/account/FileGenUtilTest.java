/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import com.zimbra.common.util.ByteUtil;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link FileGenUtil}. The comment-wrapping and disclaimer
 * generators are exercised as pure transformations; the file-rewrite workflows
 * ({@code replaceJavaFile}, {@code replaceFile}) are exercised end-to-end against
 * real files in a temporary folder and verified by reading the result back.
 */
public class FileGenUtilTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static String readFile(File f) throws IOException {
        return new String(ByteUtil.getContent(f), "utf-8");
    }

    /* Runs r with System.out redirected to a buffer and returns what was printed. */
    private static String captureStdout(RunnableX r) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, "utf-8"));
        try {
            r.run();
        } finally {
            System.setOut(original);
        }
        return new String(buf.toByteArray(), "utf-8");
    }

    private interface RunnableX {
        void run() throws Exception;
    }

    @Test
    public void wrapCommentsCollapsesWhitespaceAndWrapsUnderMaxLength() {
        // Arrange
        String comments = "one   two\tthree\nfour five";

        // Act — small max forces a wrap; prefix applied at start of each line
        String result = FileGenUtil.wrapComments(comments, 8, "# ");

        // Assert — whitespace collapsed (no double space / tab / newline runs), wrapping occurred
        assertFalse("tabs must be collapsed", result.contains("\t"));
        assertTrue("at least one wrap newline expected", result.contains("\n"));
        assertTrue("prefix must be applied", result.startsWith("# "));
        assertTrue("all words must be present", result.contains("five"));
    }

    @Test
    public void wrapCommentsEmptyInputEmitsOnlyPrefix() {
        // Act — no words at all: result builder ends empty, so the prefix is appended once
        String result = FileGenUtil.wrapComments("   ", 40, "// ");

        // Assert
        assertEquals("// ", result);
    }

    @Test
    public void wrapCommentsWithSuffixAppendsSuffixBeforeWrapNewline() {
        // Arrange — words long enough to force a wrap so the suffix branch fires
        String comments = "alpha beta gamma delta";

        // Act
        String result = FileGenUtil.wrapComments(comments, 6, "", " \\");

        // Assert — the suffix must appear immediately before a wrapping newline
        assertTrue("suffix+newline must appear on a wrapped line", result.contains(" \\\n"));
        assertTrue(result.contains("delta"));
    }

    @Test
    public void wrapCommentsSingleShortWordNoWrapNoTrailingNewline() {
        // Act
        String result = FileGenUtil.wrapComments("hello", 80, "// ");

        // Assert — fits on one line, prefix applied, no wrap newline introduced
        assertEquals("// hello", result);
    }

    @Test
    public void wrapCommentsExactWrapArithmeticMaxFour() {
        // Two-char words with maxLineLength 4 and an empty prefix.  The wrap test is
        // `lineLength + word.length() + 1 > maxLineLength` (L66).  After "aa" (len 2), adding
        // "bb" computes 2 + 2 + 1 = 5 > 4 -> wrap; same for "cc".  Expected: each word on its own
        // line.  Dropping the "+1" (MathMutator) would compute 2 + 2 = 4 (not > 4) and keep
        // "aa bb" together, producing "aa bb\ncc" instead of "aa\nbb\ncc".
        String result = FileGenUtil.wrapComments("aa bb cc", 4, "", null);
        assertEquals("aa\nbb\ncc", result);
    }

    @Test
    public void wrapCommentsBoundaryStrictGreaterThanMaxFive() {
        // maxLineLength 5, empty prefix.  After "aa" (len 2), adding "bb": 2 + 2 + 1 = 5, and the
        // guard is strictly `> 5` so 5 is NOT greater than 5 -> "bb" stays on the line giving
        // "aa bb"; then "cc": 5 + 2 + 1 = 8 > 5 -> wrap.  Expected "aa bb\ncc".  A boundary
        // mutation to `>= 5` would wrap "bb" too, yielding "aa\nbb\ncc".
        String result = FileGenUtil.wrapComments("aa bb cc", 5, "", null);
        assertEquals("aa bb\ncc", result);
    }

    @Test
    public void wrapCommentsSpaceLengthCountedAffectsNextWrap() {
        // maxLineLength 6, empty prefix.  "aa" (len 2), then "bb": 2 + 2 + 1 = 5, not > 6, so a
        // space is appended and lineLength is incremented for it (L78: `lineLength++`), making
        // lineLength 5 ("aa bb").  Then "x": 5 + 1 + 1 = 7 > 6 -> wrap, giving "aa bb\nx".
        // If the space-increment on L78 were removed, lineLength would be 4 after "bb", so "x"
        // would compute 4 + 1 + 1 = 6 (not > 6) and stay on the line: "aa bb x".
        String result = FileGenUtil.wrapComments("aa bb x", 6, "", null);
        assertEquals("aa bb\nx", result);
    }

    @Test
    public void genDoNotModifyDisclaimerExplicitVersionUsesGivenVersion() {
        // Act
        String result = FileGenUtil.genDoNotModifyDisclaimer("#", "AttributeManagerUtil", "9.9.9");

        // Assert — full structure: generator name, explicit version, and prefix on every line
        assertTrue(result.contains("DO NOT MODIFY - generated by AttributeManagerUtil"));
        assertTrue("explicit version must be used verbatim", result.contains("# 9.9.9"));
        assertEquals("six prefixed lines are emitted", 6, result.split("\n").length);
    }

    @Test
    public void genDoNotModifyDisclaimerNullVersionFallsBackToBuildInfoVersion() {
        // Act — three-arg overload with null version exercises the BuildInfo fallback branch
        String result = FileGenUtil.genDoNotModifyDisclaimer("//", "Gen", null);

        // Assert
        assertTrue(result.contains("DO NOT MODIFY - generated by Gen"));
        assertTrue("every line is prefixed", result.startsWith("//"));
    }

    @Test
    public void genDoNotModifyDisclaimerTwoArgDelegatesToBuildInfoVersion() {
        // Act — two-arg form delegates to the three-arg form with null version
        String twoArg = FileGenUtil.genDoNotModifyDisclaimer("#", "Gen");
        String threeArg = FileGenUtil.genDoNotModifyDisclaimer("#", "Gen", null);

        // Assert — both paths must produce identical output
        assertEquals(threeArg, twoArg);
    }

    @Test
    public void replaceJavaFileReplacesBetweenMarkersKeepingSurroundingLines() throws Exception {
        // Arrange — a source file with begin/end markers wrapping stale content
        File java = tmp.newFile("Foo.java");
        try (FileWriter w = new FileWriter(java)) {
            w.write("public class Foo {\n");
            w.write("    // BEGIN-AUTO-GEN-REPLACE\n");
            w.write("    int stale;\n");
            w.write("    // END-AUTO-GEN-REPLACE\n");
            w.write("}\n");
        }

        // Act
        FileGenUtil.replaceJavaFile(java.getAbsolutePath(), "    int fresh;");

        // Assert — fresh content present, stale gone, surrounding lines preserved
        String out = readFile(java);
        assertTrue("generated content inserted", out.contains("int fresh;"));
        assertFalse("stale content between markers removed", out.contains("int stale;"));
        assertTrue("opening line preserved", out.contains("public class Foo {"));
        assertTrue("both markers preserved", out.contains("BEGIN-AUTO-GEN-REPLACE")
                && out.contains("END-AUTO-GEN-REPLACE"));
    }

    @Test
    public void replaceJavaFileProducesExactLineStructure() throws Exception {
        // Arrange — a minimal source with begin/end markers around a stale line.
        File java = tmp.newFile("Exact.java");
        try (FileWriter w = new FileWriter(java)) {
            w.write("header\n");
            w.write("BEGIN-AUTO-GEN-REPLACE\n");
            w.write("stale\n");
            w.write("END-AUTO-GEN-REPLACE\n");
            w.write("footer\n");
        }

        // Act
        FileGenUtil.replaceJavaFile(java.getAbsolutePath(), "FRESH");

        // Assert — exact, newline-by-newline output.  Each emitted line is followed by
        // out.newLine() (L113/L115/L120/L123): the begin marker, then the injected content,
        // then the preserved end marker, then surrounding lines.  Removing any of those
        // newLine() calls would merge adjacent lines and break this exact-match assertion.
        String nl = System.lineSeparator();
        String expected =
                "header" + nl +
                "BEGIN-AUTO-GEN-REPLACE" + nl +
                "FRESH" + nl +
                "END-AUTO-GEN-REPLACE" + nl +
                "footer" + nl;
        assertEquals(expected, readFile(java));
    }

    @Test
    public void replaceJavaFilePrintsGeneratedBannerToStdout() throws Exception {
        // Arrange
        final File java = tmp.newFile("Banner.java");
        try (FileWriter w = new FileWriter(java)) {
            w.write("BEGIN-AUTO-GEN-REPLACE\n");
            w.write("END-AUTO-GEN-REPLACE\n");
        }

        // Act — capture stdout while the rewrite runs.
        String out = captureStdout(new RunnableX() {
            @Override
            public void run() throws Exception {
                FileGenUtil.replaceJavaFile(java.getAbsolutePath(), "X");
            }
        });

        // Assert — the success banner (System.out.println on L140-L142) names the file.  Removing
        // those println calls would leave stdout without the "generated:" line.
        assertTrue("success banner must be printed", out.contains("======================================"));
        assertTrue("generated line must name the rewritten file",
                out.contains("generated: " + java.getAbsolutePath()));
    }

    @Test
    public void replaceJavaFileNoMarkersCopiesContentVerbatim() throws Exception {
        // Arrange — a file with no markers: every line is in non-replace mode
        File java = tmp.newFile("Bar.java");
        try (FileWriter w = new FileWriter(java)) {
            w.write("line A\n");
            w.write("line B\n");
        }

        // Act
        FileGenUtil.replaceJavaFile(java.getAbsolutePath(), "IGNORED");

        // Assert — content is copied through unchanged; injected content never appears
        String out = readFile(java);
        assertTrue(out.contains("line A"));
        assertTrue(out.contains("line B"));
        assertFalse("with no markers, replacement content is not inserted", out.contains("IGNORED"));
    }

    @Test
    public void replaceFileWithContentOverwritesTargetFile() throws Exception {
        // Arrange
        File out = tmp.newFile("out.txt");
        try (FileWriter w = new FileWriter(out)) {
            w.write("old data");
        }

        // Act
        FileGenUtil.replaceFile(out.getAbsolutePath(), "brand new data");

        // Assert — file fully replaced with the new content
        assertEquals("brand new data", readFile(out));
    }

    @Test
    public void replaceFilePrintsGeneratedBannerToStdout() throws Exception {
        // Arrange
        final File out = tmp.newFile("banner-out.txt");
        try (FileWriter w = new FileWriter(out)) {
            w.write("old");
        }

        // Act — capture stdout while writeToFileFile runs (via the content overload).
        String printed = captureStdout(new RunnableX() {
            @Override
            public void run() throws Exception {
                FileGenUtil.replaceFile(out.getAbsolutePath(), "new content");
            }
        });

        // Assert — writeToFileFile prints the success banner (System.out.println on L191-L193)
        // naming the output file.  Removing those println calls leaves stdout without it.
        assertTrue("success banner must be printed", printed.contains("======================================"));
        assertTrue("generated line must name the output file",
                printed.contains("generated: " + out.getAbsolutePath()));
        // And the file really was rewritten.
        assertEquals("new content", readFile(out));
    }

    @Test
    public void replaceFileWithTemplateFillsPlaceholdersAndWrites() throws Exception {
        // Arrange — a template with a ${KEY} placeholder and the target to overwrite
        File template = tmp.newFile("tmpl.txt");
        try (FileWriter w = new FileWriter(template)) {
            w.write("Hello ${WHO}, welcome.");
        }
        File out = tmp.newFile("greeting.txt");
        try (FileWriter w = new FileWriter(out)) {
            w.write("placeholder");
        }
        Map<String, String> fillers = new HashMap<String, String>();
        fillers.put("WHO", "Zimbra");

        // Act
        FileGenUtil.replaceFile(out.getAbsolutePath(), template.getAbsolutePath(), fillers);

        // Assert — placeholder substituted, written to the output file
        String result = readFile(out);
        assertTrue("placeholder substituted", result.contains("Hello Zimbra, welcome."));
        assertFalse("raw placeholder must be gone", result.contains("${WHO}"));
    }

    @Test
    public void replaceJavaFileResultIsReadableLineByLineAfterRewrite() throws Exception {
        // Arrange
        File java = tmp.newFile("Baz.java");
        try (FileWriter w = new FileWriter(java)) {
            w.write("header\n");
            w.write("BEGIN-AUTO-GEN-REPLACE\n");
            w.write("END-AUTO-GEN-REPLACE\n");
            w.write("footer\n");
        }

        // Act
        FileGenUtil.replaceJavaFile(java.getAbsolutePath(), "GEN1\nGEN2");

        // Assert — first line is still the header (renameTo workflow produced a valid file)
        try (BufferedReader r = new BufferedReader(new FileReader(java))) {
            assertEquals("header", r.readLine());
        }
    }
}
