package com.zimbra.cs.service.formatter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
            ATOM("atom"),
            CONTACT_FOLDER("cf"),
            CSV("csv"),
            FREE_BUSY("freebusy"),
            HTML("html"),
            HTML_CONVERTED("native"),
            ICS("ics"),
            IFB("ifb"),
            JSON("json"),
            RSS("rss"),
            SYNC("sync"),
            TAR("tar"),
            TGZ("tgz"),
            VCF("vcf"),
            XML("xml"),
            ZIP("zip"),
            OPATCH("opatch");

            /**
             * cache of available format types
             */
            private static Map<String, FormatType> strToType = Collections.emptyMap();

            /*
             * The string name of the format type
             * mostly for backwards compatibility
             */
            private String strName;


            /*
             * Creates a new format type using the legacy string name
             */
            FormatType(String strName){
                this.strName = strName;
            }


            public String toString() {
                return strName;
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
