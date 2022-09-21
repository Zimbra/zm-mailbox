/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.formatter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.mime.MimeConstants;

/**
 * This factory provides access to the formatters that are currently registered with the system.
 * @author jpowers
 *
 */
public class FormatterFactory {
    /**
     * The list of supported formatter types.
     * These are the desired output type of the formatting operation
     */
    public enum FormatType
    {
            ATOM("atom", "application/atom+xml"),
            CONTACT_FOLDER("cf", "text/x-zimbra-delimitted-fields"),
            CSV("csv", "text/csv"),
            LDIF("ldif", MimeConstants.CT_TEXT_LDIF),
            NETSCAPELDIF("netscapeldif", MimeConstants.CT_TEXT_LDIF),
            FREE_BUSY("freebusy", MimeConstants.CT_TEXT_HTML),
            HTML("html", MimeConstants.CT_TEXT_HTML),
            HTML_CONVERTED("native", MimeConstants.CT_TEXT_HTML),
            HTML_ONLYOFFICE("onlyoffice", MimeConstants.CT_TEXT_HTML),
            ICS("ics", MimeConstants.CT_TEXT_CALENDAR),
            IFB("ifb", MimeConstants.CT_TEXT_CALENDAR),
            JSON("json", MimeConstants.CT_APPLICATION_JSON),
            RSS("rss", "application/rss+xml"),
            SYNC("sync", MimeConstants.CT_APPLICATION_OCTET_STREAM),
            TAR("tar", "application/x-tar"),
            TGZ("tgz", "application/x-compressed"),
            VCF("vcf", MimeConstants.CT_TEXT_VCARD),
            XML("xml", MimeConstants.CT_TEXT_XML),
            ZIP("zip", MimeConstants.CT_APPLICATION_ZIP),
            OPATCH("opatch", MimeConstants.CT_APPLICATION_OCTET_STREAM),
            MOBILE_CONFIG("mobileconfig", MimeConstants.CT_TEXT_XML);

            /**
             * cache of available format types
             */
            private static Map<String, FormatType> strToType = Collections.emptyMap();

            /*
             * The string name of the format type
             * mostly for backwards compatibility
             */
            private final String strName;

            /* See http://www.iana.org/assignments/media-types/media-types.xhtml for official Content-Type values */
            private final String contentType;


            /*
             * Creates a new format type using the legacy string name
             */
            FormatType(String strName, String contentType){
                this.strName = strName;
                this.contentType = contentType;
            }


            @Override
            public String toString() {
                return strName;
            }

            public String getContentType() {
                return contentType;
            }

            /**
             * Gets a type form a string if its registered
             * @param str The string to check (should match the output of the toString() method)
             * @return The enum value if present, null if not
             */
            public static FormatType fromString(String str)
            {
                             // Attempt to get the type from the map
                FormatType result = strToType.get(str);

                // if we missed this is likely the first call
                if(result == null && strToType.isEmpty()) {
                    Map<String, FormatType> tempMap = new HashMap<String, FormatType>();
                    for (FormatType type :FormatType.values()) {
                        tempMap.put(type.toString(),type);
                        if (type.toString().equals(str)) {
                            // if we happen to pass by the right one, save it
                            result = type;
                        }
                   }
                   strToType = Collections.unmodifiableMap(tempMap);
                }

                return result;
            }

        }

    public static Map<FormatType, Formatter> mFormatters;
    public static Map<String, Formatter> mDefaultFormatters;
    static {
        mFormatters = new HashMap<FormatType, Formatter>();
        mDefaultFormatters = new HashMap<String, Formatter>();
        mFormatters = Collections.synchronizedMap(mFormatters);
        mDefaultFormatters = Collections.synchronizedMap(mDefaultFormatters);

        addFormatter(new CsvFormatter());
        addFormatter(new LdifFormatter());
        addFormatter(new NetscapeLdifFormatter());
        addFormatter(new VcfFormatter());
        addFormatter(new IcsFormatter());
        addFormatter(new RssFormatter());
        addFormatter(new AtomFormatter());
        addFormatter(new NativeFormatter());
        addFormatter(new FreeBusyFormatter());
        addFormatter(new IfbFormatter());
        addFormatter(new SyncFormatter());
        addFormatter(new XmlFormatter());
        addFormatter(new JsonFormatter());
        addFormatter(new HtmlFormatter());
        addFormatter(new TarFormatter());
        addFormatter(new TgzFormatter());
        addFormatter(new ZipFormatter());
        addFormatter(new ContactFolderFormatter());
        addFormatter(new OctopusPatchFormatter());
        addFormatter(new MobileConfigFormatter());
    }

    /**
     * Adds a new formatter to the factory. Using this method it is possible to override the default formatters for a specificy
     * FormatType. The last formatter added to the the factory will always win.
     *
     * @param formatter The formatter to add to the factory
     */
    public  static void addFormatter(Formatter formatter) {
        mFormatters.put(formatter.getType(), formatter);
        for (String mimeType : formatter.getDefaultMimeTypes())
            mDefaultFormatters.put(mimeType, formatter);
    }


}
