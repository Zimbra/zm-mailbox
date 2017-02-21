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
package com.zimbra.cs.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.zip.GeneralPurposeBit;
import org.apache.commons.compress.archivers.zip.UnicodePathExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.util.ZimbraLog;

public class ZipUtil {

    private static final Map<Locale, List<String>> defaultCharsetForLocale;
    static {
        Map<Locale, List<String>> map = Maps.newHashMap();
        map.put(Locale.JAPANESE, Lists.newArrayList("ISO-2022-JP-2", "EUC-JP", "SJIS"));
        map.put(Locale.CHINESE, Lists.newArrayList("GB18030","ISO-2022-CN"));
        map.put(Locale.SIMPLIFIED_CHINESE, map.get(Locale.CHINESE));
        map.put(Locale.TRADITIONAL_CHINESE, Lists.newArrayList("Big5"));
        map.put(Locale.forLanguageTag("zh_HK"), map.get(Locale.TRADITIONAL_CHINESE));
        map.put(Locale.KOREAN, Lists.newArrayList("EUC-KR"));
        defaultCharsetForLocale = Collections.unmodifiableMap(map);
    }

    public final static Charset cp437charset = Charset.forName("CP437");

    /**
     * Traditional java.util.zip processing either assumes archives use UTF-8 for filenames or requires that
     * you know up front what charset is used for filenames.
     * This class uses the more versatile org.apache.commons.compress.archivers.zip package combined with
     * language information to make a best guess at what the filenames might be.
     *
     */
    public static List <String> getZipEntryNames(InputStream inputStream, Locale locale)
    throws IOException {
        List <String> zipEntryNames = Lists.newArrayList();

        /*
         * From http://commons.apache.org/proper/commons-compress/zip.html
         * Traditionally the ZIP archive format uses CodePage 437 as encoding for file name, which is not sufficient for
         * many international character sets. Over time different archivers have chosen different ways to work around
         * the limitation - the java.util.zip packages simply uses UTF-8 as its encoding for example.
         *
         * For our purposes, CP437 has the advantage that all byte sequences are valid, so it works well as a final
         * fallback charset to assume for the name.
         */
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(inputStream, cp437charset.name(),
                false /* useUnicodeExtraFields - we do our own handling of this */)) {
            ZipArchiveEntry ze;
            while ((ze = zis.getNextZipEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }
                String entryName = bestGuessAtEntryName(ze, locale);
                zipEntryNames.add(entryName);
            }
        }
        return zipEntryNames;
    }

    public static class ZipNameAndSize {
        public String name;
        public long size;
        public InputStream inputStream;
        public ZipNameAndSize(String name, long size, InputStream inputStream) {
            this.name = name;
            this.size = size;
            this.inputStream = inputStream;
        }
    }

    /**
     *
     * @param inputStream archive input stream
     * @param locale - best guess as to locale for the filenames in the archive
     * @param seqNo - the order of the item to return (excluding directory entries)
     * @return
     * @throws IOException
     */
    public static ZipNameAndSize getZipEntryNameAndSize(InputStream inputStream, Locale locale, int seqNo)
    throws IOException {
        ZipArchiveInputStream zis = new ZipArchiveInputStream(inputStream, cp437charset.name(),
                false /* useUnicodeExtraFields - we do our own handling of this */);
        ZipArchiveEntry ze;
        int idx = 0;
        while ((ze = zis.getNextZipEntry()) != null) {
            if (ze.isDirectory()) {
                continue;
            }
            if (idx++ == seqNo) {
                String entryName = bestGuessAtEntryName(ze, locale);
                return new ZipNameAndSize(entryName, ze.getSize(), zis);
            }
        }
        zis.close();
        throw new IOException("file " + seqNo + " not in archive");
    }

    public static String bestGuessAtEntryName(ZipArchiveEntry zae, Locale locale) {
        GeneralPurposeBit gbp = zae.getGeneralPurposeBit();
        if ((null != gbp) && gbp.usesUTF8ForNames()) {
            return(zae.getName());
        }
        byte[] rawName = zae.getRawName();
        /* First of all, assume UTF-8.  java.util.zip assumes UTF-8 names, so this is a reasonable first
         * guess.  If the name isn't US-ASCII and it isn't UTF-8 there is a reasonable chance this
         * will fail because of the way UTF-8 works - and we can try other charsets.
         */
        String guess = convertBytesIfPossible(rawName, StandardCharsets.UTF_8);
        if (guess != null) {
            return guess;
        }
        guess = getNameFromUnicodeExtraPathIfPresent(zae);
        if (guess != null) {
            return guess;
        }
        List<String> charsetsForLocale = defaultCharsetForLocale.get(locale);
        if (null != charsetsForLocale) {
            for (String charsetForLocale : charsetsForLocale) {
                guess = convertBytesIfPossible(rawName, Charset.forName(charsetForLocale));
                if (guess != null) {
                    return guess;
                }
            }
        }
        return zae.getName();
    }

    private static String convertBytesIfPossible(byte[] rawBytes, Charset cset) {
        CharsetDecoder charsetDecoder = reportingDecoder(cset);
        try {
            CharBuffer cb = charsetDecoder.decode(ByteBuffer.wrap(rawBytes));
            String val = cb.toString();
            ZimbraLog.misc.debug("ZipUtil name '%s' decoded from Charset='%s'", val, cset.name());
            return val;
        } catch (Exception ex) {
            ZimbraLog.misc.trace("ZipUtil failed decode from Charset='%s' %s", cset.name(), ex.getMessage());
        }
        return null;
    }

    private static CharsetDecoder reportingDecoder(Charset cset) {
        return cset.newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT);
    }

    /**
     * Use InfoZIP Unicode Extra Fields (if present) to set the filename
     */
    private static String getNameFromUnicodeExtraPathIfPresent(ZipArchiveEntry zae) {
        UnicodePathExtraField unicodePathExtraField =
                (UnicodePathExtraField) zae.getExtraField(UnicodePathExtraField.UPATH_ID);
        if (null == unicodePathExtraField) {
            return null;
        }
        CRC32 crc32 = new CRC32();
        crc32.update(zae.getRawName());
        long origCRC32 = crc32.getValue();

        if (origCRC32 == unicodePathExtraField.getNameCRC32()) {
            String val = convertBytesIfPossible(unicodePathExtraField.getUnicodeName(), StandardCharsets.UTF_8);
            if (null != val) {
                ZimbraLog.misc.debug("ZipUtil name '%s' from unicodeExtraPath", val);
            }
            return val;
        }
        return null;
    }
}
