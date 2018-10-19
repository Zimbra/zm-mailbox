/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.Version;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeManager.ObjectClassInfo;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.util.MemoryUnitUtil;

public class AttributeManagerUtil {


    private static Log logger = LogFactory.getLog(AttributeManagerUtil.class);

    private static Options options = new Options();

    // multi-line continuation prefix chars
    private static final String ML_CONT_PREFIX = "  ";

    private final AttributeManager attrMgr;

    static {
        options.addOption("h", "help", false, "display this  usage info");
        options.addOption("o", "output", true, "output file (default it to generate output to stdout)");
        options.addOption("a", "action", true, "[generateLdapSchema | generateGlobalConfigLdif | generateDefaultCOSLdif | generateSchemaLdif]");
        options.addOption("t", "template", true, "template for LDAP schema");
        options.addOption("r", "regenerateFile", true, "Java file to regenerate");


        Option iopt = new Option("i", "input", true,"attrs definition xml input file (can repeat)");
        iopt.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(iopt);

        /*
         * options for the listAttrs action
         */
        Option copt = new Option("c", "inclass", true, "list attrs in class  (can repeat)");
        copt.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(copt);

        Option nopt = new Option("n", "notinclass", true, "not list attrs in class  (can repeat)");
        nopt.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(nopt);

        Option fopt = new Option("f", "flags", true, "flags to print  (can repeat)");
        fopt.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(fopt);
    }

    private enum Action {
        dump,
        generateDefaultCOSLdif,
        generateDefaultExternalCOSLdif,
        generateGetters,
        generateGlobalConfigLdif,
        generateLdapSchema,
        generateMessageProperties,
        generateProvisioning,
        generateSchemaLdif,
        listAttrs
    }

    static class CLOptions {

        private static String get(String key, String defaultValue) {
            String value = System.getProperty(key);
            if (value == null)
                return defaultValue;
            else
                return value;
        }

        public static String buildVersion() {
            return get("zimbra.version", "unknown");
        }

        public static String getBaseDn(String entry) {
            return get(entry + ".basedn", "cn=zimbra");
        }

        public static String getEntryName(String entry, String defaultValue) {
            return get(entry + ".name", defaultValue);
        }

        public static String getEntryId(String entry, String defaultValue) {
            return get(entry + ".id", defaultValue);
        }
    }

    private AttributeManagerUtil(AttributeManager am) {
        attrMgr = am;
    }

    private Map<String, AttributeInfo> getAttrs() {
        return attrMgr.getAttrs();
    }

    private Map<String, ObjectClassInfo> getOCs() {
        return attrMgr.getOCs();
    }

    private Map<Integer,String> getGroupMap() {
        return attrMgr.getGroupMap();
    }

    private Map<Integer,String> getOCGroupMap() {
        return attrMgr.getOCGroupMap();
    }

    private String doNotModifyDisclaimer(String prefix) {
        return FileGenUtil.genDoNotModifyDisclaimer(prefix, AttributeManagerUtil.class.getSimpleName(), CLOptions.buildVersion());
    }

    private void generateDefaultCOSLdif(PrintWriter pw) {
        pw.println(doNotModifyDisclaimer("#"));
        pw.println("#");
        pw.println("# LDAP entry for the default Zimbra COS.");
        pw.println("#");

        String baseDn = CLOptions.getBaseDn("cos");
        String cosName = CLOptions.getEntryName("cos", Provisioning.DEFAULT_COS_NAME);
        String cosId = CLOptions.getEntryId("cos", "e00428a1-0c00-11d9-836a-000d93afea2a");

        pw.println("dn: cn=" + cosName +",cn=cos," + baseDn);
        pw.println("cn: " + cosName);
        pw.println("objectclass: zimbraCOS");
        pw.println("zimbraId: " + cosId);
        pw.println("description: The " + cosName + " COS");

        List<String> out = new LinkedList<String>();
        for (AttributeInfo attr : getAttrs().values()) {
           List<String> gcv = attr.getDefaultCosValues();
           if (gcv != null) {
               for (String v : gcv) {
                   out.add(attr.getName() + ": " + v);
               }
           }
        }
        String[] outs = out.toArray(new String[out.size()]);
        Arrays.sort(outs);
        for (String o : outs) {
            pw.println(o);
        }
    }

    private void generateDefaultExternalCOSLdif(PrintWriter pw) {
        pw.println(doNotModifyDisclaimer("#"));
        pw.println("#");
        pw.println("# LDAP entry for default COS for external user accounts.");
        pw.println("#");

        String baseDn = CLOptions.getBaseDn("cos");
        String cosName = CLOptions.getEntryName("cos", Provisioning.DEFAULT_EXTERNAL_COS_NAME);
        String cosId = CLOptions.getEntryId("cos", "f27456a8-0c00-11d9-280a-286d93afea2g");

        pw.println("dn: cn=" + cosName +",cn=cos," + baseDn);
        pw.println("cn: " + cosName);
        pw.println("objectclass: zimbraCOS");
        pw.println("zimbraId: " + cosId);
        pw.println("description: The default external users COS");

        List<String> out = new LinkedList<String>();
        for (AttributeInfo attr : getAttrs().values()) {
            List<String> defaultValues = attr.getDefaultExternalCosValues();
            if (defaultValues != null && !defaultValues.isEmpty()) {
                for (String v : defaultValues) {
                    out.add(attr.getName() + ": " + v);
                }
            } else {
                defaultValues = attr.getDefaultCosValues();
                if (defaultValues != null) {
                    for (String v : defaultValues) {
                        out.add(attr.getName() + ": " + v);
                    }
                }
            }
        }
        String[] outs = out.toArray(new String[out.size()]);
        Arrays.sort(outs);
        for (String o : outs) {
            pw.println(o);
        }
    }

    private void generateGlobalConfigLdif(PrintWriter pw) {
        pw.println(doNotModifyDisclaimer("#"));
        pw.println("#");
        pw.println("# LDAP entry that contains initial default Zimbra global config.");
        pw.println("#");

        String baseDn = CLOptions.getBaseDn("config");
        pw.println("dn: cn=config," + baseDn);
        pw.println("objectclass: organizationalRole");
        pw.println("cn: config");
        pw.println("objectclass: zimbraGlobalConfig");

        List<String> out = new LinkedList<String>();
        for (AttributeInfo attr : getAttrs().values()) {
           List<String> gcv = attr.getGlobalConfigValues();
           if (gcv != null) {
               for (String v : gcv) {
                   out.add(attr.getName() + ": " + v);
               }
           }
        }
        String[] outs = out.toArray(new String[out.size()]);
        Arrays.sort(outs);
        for (String o : outs) {
            pw.println(o);
        }
    }


    private List<AttributeInfo> getAttrList(int groupId) {
        List<AttributeInfo> list = new ArrayList<AttributeInfo>(getAttrs().size());
        for (AttributeInfo ai : getAttrs().values()) {
            if (ai.getId() > -1 && ai.getGroupId() == groupId) {
                list.add(ai);
            }
        }
        return list;
    }

    private void sortAttrsByOID(List<AttributeInfo> list) {
        Collections.sort(list, new Comparator<AttributeInfo>() {
            @Override
            public int compare(AttributeInfo a1, AttributeInfo b1) {
                return a1.getId() - b1.getId();
            }
        });
    }

    private void sortAttrsByName(List<AttributeInfo> list) {
        Collections.sort(list, new Comparator<AttributeInfo>() {
            @Override
            public int compare(AttributeInfo a1, AttributeInfo b1) {
                return a1.getName().compareTo(b1.getName());
            }
        });
    }

    private List<ObjectClassInfo> getOCList(int groupId) {
        List<ObjectClassInfo> list = new ArrayList<ObjectClassInfo>(getOCs().size());
        for (ObjectClassInfo oci : getOCs().values()) {
            if (oci.getId() > -1 && oci.getGroupId() == groupId) {
                list.add(oci);
            }
        }
        return list;
    }

    private void sortOCsByOID(List<ObjectClassInfo> list) {
        Collections.sort(list, new Comparator<ObjectClassInfo>() {
            @Override
            public int compare(ObjectClassInfo oc1, ObjectClassInfo oc2) {
                return oc1.getId() - oc2.getId();
            }
        });
    }

    private void sortOCsByName(List<ObjectClassInfo> list) {
        Collections.sort(list, new Comparator<ObjectClassInfo>() {
            @Override
            public int compare(ObjectClassInfo oc1, ObjectClassInfo oc2) {
                return oc1.getName().compareTo(oc2.getName());
            }
        });
    }


    private static String sortCSL(String in) {
        String[] ss = in.split("\\s*,\\s*");
        Arrays.sort(ss);
        StringBuilder sb = new StringBuilder(in.length());
        for (int i = 0; i < ss.length; i++) {
            sb.append(ss[i]);
            if (i < (ss.length-1)) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }


    // escape QQ and QS for rfc4512 dstring(http://www.ietf.org/rfc/rfc4512.txt 4.1.  Schema Definitions)
    private String rfc4512Dstring(String unescaped) {
        String escaped = unescaped.replace("\\", "\\5C").replace("'", "\\27");
        return escaped;
    }

    private void buildSchemaBanner(StringBuilder BANNER) {

        BANNER.append(doNotModifyDisclaimer("#"));
        BANNER.append("#\n");
        BANNER.append("# Zimbra LDAP Schema\n");
        BANNER.append("#\n");
        BANNER.append("#\n");
        BANNER.append("#\n");
        BANNER.append("# our root OID (http://www.iana.org/assignments/enterprise-numbers)\n");
        BANNER.append("#\n");
        BANNER.append("#  1.3.6.1.4.1.19348\n");
        BANNER.append("#  1.3.6.1.4.1.19348.2      LDAP elements\n");
        BANNER.append("#  1.3.6.1.4.1.19348.2.1    Attribute Types\n");
        BANNER.append("#  1.3.6.1.4.1.19348.2.2    Object Classes\n");
        BANNER.append("#");
    }

    private void buildZimbraRootOIDs(StringBuilder ZIMBRA_ROOT_OIDS, String prefix) {
        ZIMBRA_ROOT_OIDS.append(prefix + "ZimbraRoot 1.3.6.1.4.1.19348\n");
        ZIMBRA_ROOT_OIDS.append(prefix + "ZimbraLDAP ZimbraRoot:2\n");
    }

    private void buildAttrDef(StringBuilder ATTRIBUTE_DEFINITIONS, AttributeInfo ai) {
        String lengthSuffix;

        String syntax = null;
        String substr = null;
        String equality = null;
        String ordering = null;

        switch (ai.getType()) {
        case TYPE_BOOLEAN:
            syntax = "1.3.6.1.4.1.1466.115.121.1.7";
            equality = "booleanMatch";
            break;
        case TYPE_BINARY:
            // cannot use the binary syntax because it cannot support adding/deleting individual values
            // in a multi-valued attrs, only replacement(i.e. replace all values) is supported.
            //
            // when a value is added to a multi-valued attr, or when an attempt is made to delete a
            // specific value, will get "no equality matching rule" error from LDAP server, because
            // there is no equality matching for 1.3.6.1.4.1.1466.115.121.1.5
            //
            // Note: 1.3.6.1.4.1.1466.115.121.1.5 attrs, like userSMIMECertificate, when included in
            //       the zimbra schema, are declared as type="binary" in zimbra-attrs.xml.
            //       Handling for the two (1.3.6.1.4.1.1466.115.121.1.5 and 1.3.6.1.4.1.1466.115.121.1.40)
            //       are *exactly the same* in ZCS.  They are:
            //       - transferred as binary on the wire, by setting the JNDI "java.naming.ldap.attributes.binary"
            //         environment property
            //       - stored as base64 encoded string in ZCS memory
            //       - Entry.getAttr(String name) returns the base64 encoded value
            //       - Entry.getBinaryAttr(String name) returns the base64 decoded value
            //
            /*
            lengthSuffix = "";
            if (ai.getMax() != Long.MAX_VALUE) {
                lengthSuffix = "{" + ai.getMax() + "}";
            }
            syntax = "1.3.6.1.4.1.1466.115.121.1.5" + lengthSuffix;
            break;
            */

            // the same as octet string
            lengthSuffix = "";
            if (ai.getMax() != Long.MAX_VALUE) {
                lengthSuffix = "{" + ai.getMax() + "}";
            }
            syntax = "1.3.6.1.4.1.1466.115.121.1.40" + lengthSuffix;
            equality = "octetStringMatch";
            break;
        case TYPE_CERTIFICATE:
            // This type does have a equality matching rule, so adding/deleting individual values
            // is supported.
            //
            lengthSuffix = "";
            if (ai.getMax() != Long.MAX_VALUE) {
                lengthSuffix = "{" + ai.getMax() + "}";
            }
            syntax = "1.3.6.1.4.1.1466.115.121.1.8" + lengthSuffix;
            equality = "certificateExactMatch";
            break;
        case TYPE_EMAIL:
        case TYPE_EMAILP:
        case TYPE_CS_EMAILP:
            syntax = "1.3.6.1.4.1.1466.115.121.1.26{256}";
            equality = "caseIgnoreIA5Match";
            substr = "caseIgnoreSubstringsMatch";
            break;

        case TYPE_INTL_EMAIL:
            syntax = "1.3.6.1.4.1.1466.115.121.1.15{256}";
            equality = "caseIgnoreMatch";
            substr = "caseIgnoreSubstringsMatch";
            break;

        case TYPE_GENTIME:
            syntax = "1.3.6.1.4.1.1466.115.121.1.24";
            equality = "generalizedTimeMatch";
            ordering = "generalizedTimeOrderingMatch ";
            break;

        case TYPE_ID:
            syntax = "1.3.6.1.4.1.1466.115.121.1.15{256}";
            equality = "caseIgnoreMatch";
            substr = "caseIgnoreSubstringsMatch";
            break;

        case TYPE_DURATION:
            syntax = "1.3.6.1.4.1.1466.115.121.1.26{32}";
            equality = "caseIgnoreIA5Match";
            break;

        case TYPE_ENUM:
            int maxLen = Math.max(32, ai.getEnumValueMaxLength());
            syntax = "1.3.6.1.4.1.1466.115.121.1.15{" + maxLen + "}";
            equality = "caseIgnoreMatch";
            substr = "caseIgnoreSubstringsMatch";
            break;

        case TYPE_INTEGER:
        case TYPE_PORT:
        case TYPE_LONG:
            syntax = "1.3.6.1.4.1.1466.115.121.1.27";
            equality = "integerMatch";
            break;

        case TYPE_STRING:
        case TYPE_REGEX:
            lengthSuffix = "";
            if (ai.getMax() != Long.MAX_VALUE) {
                lengthSuffix = "{" + ai.getMax() + "}";
            }
            syntax = "1.3.6.1.4.1.1466.115.121.1.15" + lengthSuffix;
            equality = "caseIgnoreMatch";
            substr = "caseIgnoreSubstringsMatch";
            break;

        case TYPE_ASTRING:
            lengthSuffix = "";
            if (ai.getMax() != Long.MAX_VALUE) {
                lengthSuffix = "{" + ai.getMax() + "}";
            }
            syntax = "1.3.6.1.4.1.1466.115.121.1.26" + lengthSuffix;
            equality = "caseIgnoreIA5Match";
            substr = "caseIgnoreSubstringsMatch";
            break;

        case TYPE_OSTRING:
            lengthSuffix = "";
            if (ai.getMax() != Long.MAX_VALUE) {
                lengthSuffix = "{" + ai.getMax() + "}";
            }
            syntax = "1.3.6.1.4.1.1466.115.121.1.40" + lengthSuffix;
            equality = "octetStringMatch";
            break;

        case TYPE_CSTRING:
            lengthSuffix = "";
            if (ai.getMax() != Long.MAX_VALUE) {
                lengthSuffix = "{" + ai.getMax() + "}";
            }
            syntax = "1.3.6.1.4.1.1466.115.121.1.15" + lengthSuffix;
            equality = "caseExactMatch";
            substr = "caseExactSubstringsMatch";
            break;

        case TYPE_PHONE:
            lengthSuffix = "";
            if (ai.getMax() != Long.MAX_VALUE) {
                lengthSuffix = "{" + ai.getMax() + "}";
            }
            syntax = "1.3.6.1.4.1.1466.115.121.1.50" + lengthSuffix;
            equality = "telephoneNumberMatch";
            substr = "telephoneNumberSubstringsMatch";
            break;

        default:
            throw new RuntimeException("unknown type encountered!");
        }

        ATTRIBUTE_DEFINITIONS.append("( " + ai.getName() + "\n");
        ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "NAME ( '" + ai.getName() + "' )\n");
        ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "DESC '" + rfc4512Dstring(ai.getDescription()) + "'\n");

        ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "SYNTAX " + syntax);

        if (equality != null) {
            ATTRIBUTE_DEFINITIONS.append("\n" + ML_CONT_PREFIX + "EQUALITY " + equality);
        }

        if (substr != null) {
            ATTRIBUTE_DEFINITIONS.append("\n" + ML_CONT_PREFIX + "SUBSTR " + substr);
        }

        if (ordering != null) {
            ATTRIBUTE_DEFINITIONS.append("\n" + ML_CONT_PREFIX + "ORDERING " + ordering);
        } else if (ai.getOrder() != null) {
            ATTRIBUTE_DEFINITIONS.append("\n" + ML_CONT_PREFIX + "ORDERING " + ai.getOrder());
        }

        if (ai.getCardinality() == AttributeCardinality.single) {
            ATTRIBUTE_DEFINITIONS.append("\n" + ML_CONT_PREFIX + "SINGLE-VALUE");
        }

        ATTRIBUTE_DEFINITIONS.append(")");
    }


    private void buildObjectClassOIDs(StringBuilder OC_GROUP_OIDS, StringBuilder OC_OIDS, String prefix) {
        for (Iterator<Integer> iter = getOCGroupMap().keySet().iterator(); iter.hasNext();) {
            int i = iter.next();

            // OC_GROUP_OIDS
            OC_GROUP_OIDS.append(prefix + getOCGroupMap().get(i) + " ZimbraLDAP:" + i + "\n");

            // List all ocs which we define and which belong in this group
            List<ObjectClassInfo> list = getOCList(i);

            // OC_OIDS - sorted by OID
            sortOCsByOID(list);

            for (ObjectClassInfo oci : list) {
                OC_OIDS.append(prefix + oci.getName() + " " + getOCGroupMap().get(i) + ':' + oci.getId() + "\n");
            }
        }
    }

    /**
     *
     * @param OC_DEFINITIONS
     * @param prefix
     * @param blankLineSeperator whether to seperate each OC with a blank line
     */
    private void buildObjectClassDefs(StringBuilder OC_DEFINITIONS, String prefix, boolean blankLineSeperator) {
        for (AttributeClass cls : AttributeClass.values()) {

            String ocName = cls.getOCName();
            String ocCanonicalName = ocName.toLowerCase();
            ObjectClassInfo oci = getOCs().get(ocCanonicalName);
            if (oci == null)
                continue;  // oc not defined in xml, skip

            // OC_DEFINITIONS:
            List<String> comment = oci.getComment();
            OC_DEFINITIONS.append("#\n");
            for (String line : comment) {
                if (line.length() > 0)
                    OC_DEFINITIONS.append("# " + line + "\n");
                else
                    OC_DEFINITIONS.append("#\n");
            }
            OC_DEFINITIONS.append("#\n");

            OC_DEFINITIONS.append(prefix + "( " + oci.getName() + "\n");
            OC_DEFINITIONS.append(ML_CONT_PREFIX + "NAME '" + oci.getName() + "'\n");
            OC_DEFINITIONS.append(ML_CONT_PREFIX + "DESC '" + rfc4512Dstring(oci.getDescription()) + "'\n");
            OC_DEFINITIONS.append(ML_CONT_PREFIX + "SUP ");
            for (String sup : oci.getSuperOCs())
                OC_DEFINITIONS.append(sup);
            OC_DEFINITIONS.append(" " + oci.getType() + "\n");

            StringBuilder value = new StringBuilder();
            buildObjectClassAttrs(cls, value);

            OC_DEFINITIONS.append(value);
            OC_DEFINITIONS.append(")\n");

            if (blankLineSeperator)
                OC_DEFINITIONS.append("\n");

        }
    }

    private void buildObjectClassAttrs(AttributeClass cls, StringBuilder value) {
        List<String> must = new LinkedList<String>();
        List<String> may = new LinkedList<String>();
        for (AttributeInfo ai : getAttrs().values()) {
            if (ai.requiredInClass(cls)) {
                must.add(ai.getName());
            }
            if (ai.optionalInClass(cls)) {
                may.add(ai.getName());
            }
        }
        Collections.sort(must);
        Collections.sort(may);

        if (!must.isEmpty()) {
            value.append(ML_CONT_PREFIX + "MUST (\n");
            Iterator<String> mustIter = must.iterator();
            while (true) {
                value.append(ML_CONT_PREFIX + "  ").append(mustIter.next());
                if (!mustIter.hasNext()) {
                    break;
                }
                value.append(" $\n");
            }
            value.append("\n" + ML_CONT_PREFIX + ")\n");
        }
        if (!may.isEmpty()) {
            value.append(ML_CONT_PREFIX + "MAY (\n");
            Iterator<String> mayIter = may.iterator();
            while (true) {
                value.append(ML_CONT_PREFIX + "  ").append(mayIter.next());
                if (!mayIter.hasNext()) {
                    break;
                }
                value.append(" $\n");
            }
            value.append("\n" + ML_CONT_PREFIX + ")\n");
        }
        value.append(ML_CONT_PREFIX);
    }

    /**
     * This methods uses xml for generating objectclass OIDs and definitions
     */
    private void generateLdapSchema(PrintWriter pw, String schemaTemplateFile) throws IOException {
        byte[] templateBytes = ByteUtil.getContent(new File(schemaTemplateFile));
        String templateString = new String(templateBytes, "utf-8");

        StringBuilder BANNER = new StringBuilder();
        StringBuilder ZIMBRA_ROOT_OIDS = new StringBuilder();
        StringBuilder GROUP_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_DEFINITIONS = new StringBuilder();
        StringBuilder OC_GROUP_OIDS = new StringBuilder();
        StringBuilder OC_OIDS = new StringBuilder();
        StringBuilder OC_DEFINITIONS = new StringBuilder();

        buildSchemaBanner(BANNER);
        buildZimbraRootOIDs(ZIMBRA_ROOT_OIDS, "objectIdentifier ");

        for (Iterator<Integer> iter = getGroupMap().keySet().iterator(); iter.hasNext();) {
            int i = iter.next();

            //GROUP_OIDS
            GROUP_OIDS.append("objectIdentifier " + getGroupMap().get(i) + " ZimbraLDAP:" + i + "\n");

            // List all attrs which we define and which belong in this group
            List<AttributeInfo> list = getAttrList(i);

            // ATTRIBUTE_OIDS - sorted by OID
            sortAttrsByOID(list);

            for (AttributeInfo ai : list) {
                String parentOid = ai.getParentOid();
                if (parentOid == null)
                    ATTRIBUTE_OIDS.append("objectIdentifier " + ai.getName() + " " + getGroupMap().get(i) + ':' + ai.getId() + "\n");
                else
                    ATTRIBUTE_OIDS.append("objectIdentifier " + ai.getName() + " " + parentOid + "." + ai.getId() + "\n");
            }

            // ATTRIBUTE_DEFINITIONS: DESC EQUALITY NAME ORDERING SINGLE-VALUE SUBSTR SYNTAX
            // - sorted by name
            sortAttrsByName(list);

            for (AttributeInfo ai : list) {
                ATTRIBUTE_DEFINITIONS.append("attributetype ");
                buildAttrDef(ATTRIBUTE_DEFINITIONS, ai);
                ATTRIBUTE_DEFINITIONS.append("\n\n");
            }
        }

        // object class OIDs
        buildObjectClassOIDs(OC_GROUP_OIDS, OC_OIDS, "objectIdentifier ");

        // object class definitions
        buildObjectClassDefs(OC_DEFINITIONS, "objectclass ", true);

        Map<String,String> templateFillers = new HashMap<String,String>();
        templateFillers.put("BANNER", BANNER.toString());
        templateFillers.put("ZIMBRA_ROOT_OIDS", ZIMBRA_ROOT_OIDS.toString());
        templateFillers.put("GROUP_OIDS", GROUP_OIDS.toString());
        templateFillers.put("ATTRIBUTE_OIDS", ATTRIBUTE_OIDS.toString());
        templateFillers.put("OC_GROUP_OIDS", OC_GROUP_OIDS.toString());
        templateFillers.put("OC_OIDS", OC_OIDS.toString());
        templateFillers.put("ATTRIBUTE_DEFINITIONS", ATTRIBUTE_DEFINITIONS.toString());
        templateFillers.put("OC_DEFINITIONS", OC_DEFINITIONS.toString());

        pw.print(StringUtil.fillTemplate(templateString, templateFillers));
    }

    private void generateSchemaLdif(PrintWriter pw) {

        StringBuilder BANNER = new StringBuilder();
        StringBuilder ZIMBRA_ROOT_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_GROUP_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_DEFINITIONS = new StringBuilder();
        StringBuilder OC_GROUP_OIDS = new StringBuilder();
        StringBuilder OC_OIDS = new StringBuilder();
        StringBuilder OC_DEFINITIONS = new StringBuilder();

        buildSchemaBanner(BANNER);
        buildZimbraRootOIDs(ZIMBRA_ROOT_OIDS, "olcObjectIdentifier: ");

        for (Iterator<Integer> iter = getGroupMap().keySet().iterator(); iter.hasNext();) {
            int i = iter.next();

            //GROUP_OIDS
            ATTRIBUTE_GROUP_OIDS.append("olcObjectIdentifier: " + getGroupMap().get(i) + " ZimbraLDAP:" + i + "\n");

            // List all attrs which we define and which belong in this group
            List<AttributeInfo> list = getAttrList(i);

            // ATTRIBUTE_OIDS - sorted by OID
            sortAttrsByOID(list);

            for (AttributeInfo ai : list) {
                String parentOid = ai.getParentOid();
                if (parentOid == null)
                    ATTRIBUTE_OIDS.append("olcObjectIdentifier: " + ai.getName() + " " + getGroupMap().get(i) + ':' + ai.getId() + "\n");
                else
                    ATTRIBUTE_OIDS.append("olcObjectIdentifier: " + ai.getName() + " " + parentOid + "." + ai.getId() + "\n");
            }

            // ATTRIBUTE_DEFINITIONS: DESC EQUALITY NAME ORDERING SINGLE-VALUE SUBSTR SYNTAX
            // - sorted by name
            sortAttrsByName(list);

            /* Hack to add the company attribute from Microsoft schema
             * For generateLdapSchema, it is specified in the zimbra.schema-template file
             * We don't use a template file for generateSchemaLdif thus hardcode here.
             * Move to template file if really necessary.
             *
            #### From Microsoft Schema
            olcAttributeTypes ( 1.2.840.113556.1.2.146
                    NAME ( 'company' )
                    SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{512}
                    EQUALITY caseIgnoreMatch
                    SUBSTR caseIgnoreSubstringsMatch
                    SINGLE-VALUE )
            */

            ATTRIBUTE_DEFINITIONS.append("olcAttributeTypes: ( 1.2.840.113556.1.2.146\n");
            ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "NAME ( 'company' )\n");
            ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{512}\n");
            ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "EQUALITY caseIgnoreMatch\n");
            ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "SUBSTR caseIgnoreSubstringsMatch\n");
            ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "SINGLE-VALUE )\n");

            for (AttributeInfo ai : list) {
                ATTRIBUTE_DEFINITIONS.append("olcAttributeTypes: ");
                buildAttrDef(ATTRIBUTE_DEFINITIONS, ai);
                ATTRIBUTE_DEFINITIONS.append("\n");
            }
        }

        // objectclass OIDs
        buildObjectClassOIDs(OC_GROUP_OIDS, OC_OIDS, "olcObjectIdentifier: ");

        // objectclass definitions
        buildObjectClassDefs(OC_DEFINITIONS, "olcObjectClasses: ", false);

        pw.println(BANNER);
        pw.println("dn: cn=zimbra,cn=schema,cn=config");
        pw.println("objectClass: olcSchemaConfig");
        pw.println("cn: zimbra");
        pw.print(ZIMBRA_ROOT_OIDS);
        pw.print(ATTRIBUTE_GROUP_OIDS);
        pw.print(ATTRIBUTE_OIDS);
        pw.print(OC_GROUP_OIDS);
        pw.print(OC_OIDS);
        pw.print(ATTRIBUTE_DEFINITIONS);
        pw.print(OC_DEFINITIONS);
    }

    private void generateMessageProperties(String outFile) throws IOException {
        StringBuilder result = new StringBuilder();

        result.append(doNotModifyDisclaimer("#"));
        result.append("# Zimbra LDAP attributes." + "\n");
        result.append("# \n");

        List<String> attrs = new ArrayList<String>(getAttrs().keySet());
        Collections.sort(attrs);

        for (String attr : attrs) {
            AttributeInfo ai = getAttrs().get(attr);
            String desc = ai.getDescription();
            if (desc != null) {
                String text = FileGenUtil.wrapComments(desc, 80, "  ", " \\").substring(2); // strip off the 2 spaces on the first line
                result.append(ai.getName() + " = " + text + "\n");
            }
        }

        FileGenUtil.replaceFile(outFile, result.toString());
    }

    private void listAttrs(PrintWriter pw, String[] inClass, String[] notInClass, String[] printFlags) throws ServiceException {
        if (inClass == null)
            usage("no class specified");

        Set<String> attrsInClass = new HashSet<String>();
        for (String c : inClass) {
            AttributeClass ac = AttributeClass.valueOf(c);
            SetUtil.union(attrsInClass, attrMgr.getAttrsInClass(ac));
        }

        Set<String> attrsNotInClass = new HashSet<String>();
        if (notInClass != null) {
            for (String c : notInClass) {
                AttributeClass ac = AttributeClass.valueOf(c);
                SetUtil.union(attrsNotInClass, attrMgr.getAttrsInClass(ac));
            }
        }

        attrsInClass = SetUtil.subtract(attrsInClass, attrsNotInClass);

        List<String> list = new ArrayList<String>(attrsInClass);
        Collections.sort(list);

        for (String a : list) {
            StringBuffer flags = new StringBuffer();
            if (printFlags != null) {
                for (String f : printFlags) {
                    AttributeFlag af = AttributeFlag.valueOf(f);
                    if (attrMgr.hasFlag(af, a)) {
                        if (flags.length()>0)
                            flags.append(", ");
                        flags.append(af.name());
                    }
                }

                if (flags.length() > 0) {
                    flags.insert(0, "(").append(")");
                }
            }
            System.out.println(a + " " + flags);
        }

    }

    private static String enumName(AttributeInfo ai) {
        String enumName = ai.getName();
        if (enumName.startsWith("zimbra")) enumName = enumName.substring(6);
        enumName = StringUtil.escapeJavaIdentifier(enumName.substring(0,1).toUpperCase() + enumName.substring(1));
        return enumName;
    }

    private static void generateEnum(StringBuilder result, AttributeInfo ai) throws ServiceException {

        Map<String,String> values = new LinkedHashMap<String, String>();
        for (String v : ai.getEnumSet()) {
            values.put(v, StringUtil.escapeJavaIdentifier(v));
        }

        String enumName = enumName(ai);

        result.append(String.format("%n"));
        result.append(String.format("    public static enum %s {%n", enumName));
        Set<Map.Entry<String,String>> set = values.entrySet();
        int i =1;
        for (Map.Entry<String,String> entry : set) {
            result.append(String.format("        %s(\"%s\")%s%n", entry.getValue(), entry.getKey(), i == set.size() ? ";" : ","));
            i++;
        }

        result.append(String.format("        private String mValue;%n"));
        result.append(String.format("        private %s(String value) { mValue = value; }%n", enumName));
        result.append(String.format("        public String toString() { return mValue; }%n"));
        result.append(String.format("        public static %s fromString(String s) throws ServiceException {%n", enumName));
        result.append(String.format("            for (%s value : values()) {%n", enumName));
        result.append(String.format("                if (value.mValue.equals(s)) return value;%n"));
        result.append(String.format("             }%n"));
        result.append(String.format("             throw ServiceException.INVALID_REQUEST(\"invalid value: \"+s+\", valid values: \"+ Arrays.asList(values()), null);%n"));
        result.append(String.format("        }%n"));
        for (Map.Entry<String,String> entry : set) {
            result.append(String.format("        public boolean is%s() { return this == %s;}%n", StringUtil.capitalize(entry.getValue()), entry.getValue()));
        }
        result.append(String.format("    }%n"));
    }

    /**
    *
    * @param pw
    * @param inClass
    * @param optionValue
    * @throws ServiceException
    */
   private void generateGetters(String inClass, String javaFile) throws ServiceException, IOException {
       if (inClass == null)
           usage("no class specified");


       AttributeClass ac = AttributeClass.valueOf(inClass);
       Set<String> attrsInClass = attrMgr.getAttrsInClass(ac);

       // add in mailRecipient if we need to
       if (ac == AttributeClass.account) {
           SetUtil.union(attrsInClass, attrMgr.getAttrsInClass(AttributeClass.mailRecipient));
       }

       List<String> list = new ArrayList<String>(attrsInClass);
       Collections.sort(list);

       StringBuilder result = new StringBuilder();

       for (String a : list) {
           AttributeInfo ai = getAttrs().get(a.toLowerCase());
           if (ai == null)
               continue;

           switch (ai.getType()) {
               case TYPE_BINARY:
               case TYPE_CERTIFICATE:
               case TYPE_DURATION:
               case TYPE_GENTIME:
               case TYPE_ENUM:
               case TYPE_PORT:
                   if (ai.getCardinality() != AttributeCardinality.multi) {
                       generateGetter(result, ai, false, ac);
                   }
                   generateGetter(result, ai, true, ac);
                   generateSetters(result, ai, false, SetterType.set);
                   if (ai.getType() == AttributeType.TYPE_GENTIME ||
                       ai.getType() == AttributeType.TYPE_ENUM ||
                       ai.getType() == AttributeType.TYPE_PORT) {
                       generateSetters(result, ai, true, SetterType.set);
                   }
                   generateSetters(result, ai, false, SetterType.unset);
                   break;
               default:
                   if (ai.getName().equalsIgnoreCase("zimbraLocale")) {
                       generateGetter(result, ai, true, ac);
                   } else {
                       generateGetter(result, ai, false, ac);
                   }
                   generateSetters(result, ai, false, SetterType.set);
                   if (ai.getCardinality() == AttributeCardinality.multi) {
                       generateSetters(result, ai, false, SetterType.add);
                       generateSetters(result, ai, false, SetterType.remove);
                       if (ai.isEphemeral()) {
                           generateSetters(result, ai, false, SetterType.has);
                       }
                       if (ai.isExpirable()) {
                           generateSetters(result, ai, false, SetterType.purge);
                       }
                   }
                   generateSetters(result, ai, false, SetterType.unset);
                   break;
           }
       }
       FileGenUtil.replaceJavaFile(javaFile, result.toString());
   }

   private static String defaultValue(AttributeInfo ai, AttributeClass ac) {
       List<String> values;
       switch (ac) {
           case account:
           case calendarResource:
           case cos:
               values = ai.getDefaultCosValues();
               break;
           case domain:
               if (ai.hasFlag(AttributeFlag.domainInherited))
                   values = ai.getGlobalConfigValues();
               else
                   return null;
               break;
           case server:
               if (ai.hasFlag(AttributeFlag.serverInherited))
                   values = ai.getGlobalConfigValues();
               else
                   return null;
               break;
           case globalConfig:
               values = ai.getGlobalConfigValues();
               break;
           default:
               return null;
       }
       if (values == null || values.size() == 0)
           return null;

       if (ai.getCardinality() != AttributeCardinality.multi) {
           return values.get(0);
       } else {
           StringBuilder result = new StringBuilder();
           result.append("new String[] {");
           boolean first = true;
           for (String v : values) {
               if (!first) result.append(","); else first = false;
               result.append("\"");
               result.append(v.replace("\"","\\\""));
               result.append("\"");
           }
           result.append("}");
           return result.toString();
       }
   }

   @VisibleForTesting
   public static void generateGetter(StringBuilder result, AttributeInfo ai, boolean asString, AttributeClass ac) throws ServiceException {
       String javaType;
       String javaBody;
       String javaDocReturns;
       String name = ai.getName();
       AttributeType type = asString ? AttributeType.TYPE_STRING : ai.getType();
       boolean asStringDoc = false;

       String methodName = ai.getName();
       if (methodName.startsWith("zimbra")) methodName = methodName.substring(6);
       methodName = (type == AttributeType.TYPE_BOOLEAN ? "is" : "get")+methodName.substring(0,1).toUpperCase() + methodName.substring(1);
       if (asString) methodName += "AsString";

       String defaultValue = defaultValue(ai, ac);
       String dynamic = ai.isDynamic() ? "dynamicComponent" : "null";

       switch (type) {
           case TYPE_BOOLEAN:
               defaultValue = "TRUE".equalsIgnoreCase(defaultValue) ? "true" : "false";
               javaType = "boolean";
               if (ai.isEphemeral()) {
                   javaBody = String.format("return getEphemeralAttr(Provisioning.A_%s, %s).getBoolValue(%s);", name, dynamic, defaultValue);
               } else {
                   javaBody = String.format("return getBooleanAttr(Provisioning.A_%s, %s, true);", name, defaultValue);
               }
               javaDocReturns = String.format(", or %s if unset", defaultValue);
               break;
           case TYPE_BINARY:
           case TYPE_CERTIFICATE:
               defaultValue = "null";
               javaType = "byte[]";
               if (ai.isEphemeral()) {
                   javaBody = String.format("String v = getEphemeralAttr(Provisioning.A_%s, %s).getValue(%s); return v == null ? null : ByteUtil.decodeLDAPBase64(v);", name, dynamic, defaultValue);
               } else {
                   javaBody = String.format("return getBinaryAttr(Provisioning.A_%s, true);", name);
               }
               javaDocReturns = String.format(", or null if unset", defaultValue);
               break;
           case TYPE_INTEGER:
               if (defaultValue == null) defaultValue = "-1";
               javaType = "int";
               if (ai.isEphemeral()) {
                   javaBody = String.format("return getEphemeralAttr(Provisioning.A_%s, %s).getIntValue(%s);", name, dynamic, defaultValue);
               } else {
                   javaBody = String.format("return getIntAttr(Provisioning.A_%s, %s, true);", name, defaultValue);
               }
               javaDocReturns = String.format(", or %s if unset", defaultValue);
               break;
           case TYPE_PORT:
               if (defaultValue == null) defaultValue = "-1";
               javaType = "int";
               if (ai.isEphemeral()) {
                   javaBody = String.format("return getEphemeralAttr(Provisioning.A_%s, %s).getIntValue(%s);", name, dynamic, defaultValue);
               } else {
                   javaBody = String.format("return getIntAttr(Provisioning.A_%s, %s, true);", name, defaultValue);
               }
               javaDocReturns = String.format(", or %s if unset", defaultValue);
               asStringDoc = true;
               break;
           case TYPE_ENUM:
               javaType = "ZAttrProvisioning." + enumName(ai);
               if (defaultValue != null) {
                   defaultValue = javaType + "." + StringUtil.escapeJavaIdentifier(defaultValue);
               } else {
                   defaultValue = "null";
               }
               if (ai.isEphemeral()) {
                   javaBody = String.format("try { String v = getEphemeralAttr(Provisioning.A_%s, %s).getValue(); return v == null ? %s : ZAttrProvisioning.%s.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return %s; }", name, dynamic, defaultValue,enumName(ai), defaultValue);
               } else {
                   javaBody = String.format("try { String v = getAttr(Provisioning.A_%s, true, true); return v == null ? %s : ZAttrProvisioning.%s.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return %s; }", name, defaultValue,enumName(ai), defaultValue);
               }
               javaDocReturns = String.format(", or %s if unset and/or has invalid value", defaultValue);
               break;
           case TYPE_LONG:
               if (defaultValue == null) defaultValue = "-1";
               javaType = "long";
               if (ai.isEphemeral()) {
                   javaBody = String.format("return getEphemeralAttr(Provisioning.A_%s, %s).getLongValue(%sL);", name, dynamic, defaultValue);
               } else {
                   javaBody = String.format("return getLongAttr(Provisioning.A_%s, %sL, true);", name, new MemoryUnitUtil(1024).convertToBytes(defaultValue));
               }
               javaDocReturns = String.format(", or %s if unset", defaultValue);
               break;
           case TYPE_DURATION:
               String defaultDurationStrValue;
               if (defaultValue != null) {
                   defaultDurationStrValue = " ("+defaultValue+") ";
                   defaultValue = String.valueOf(DateUtil.getTimeInterval(defaultValue, -1));
               } else {
                   defaultValue = "-1";
                   defaultDurationStrValue = "";
               }
               if (ai.isEphemeral()) {
                   javaBody = String.format("return getEphemeralTimeInterval(Provisioning.A_%s, %s, %sL);", name, dynamic, defaultValue);
               } else {
                   javaBody = String.format("return getTimeInterval(Provisioning.A_%s, %sL, true);", name, defaultValue);
               }
               javaDocReturns = String.format(" in millseconds, or %s%s if unset", defaultValue, defaultDurationStrValue);
               javaType = "long";
               asStringDoc = true;
               break;
           case TYPE_GENTIME:
               javaType = "Date";
               if (ai.isEphemeral()) {
                   javaBody = String.format("String v = getEphemeralAttr(Provisioning.A_%s, %s).getValue(%s); return v == null ? null : LdapDateUtil.parseGeneralizedTime(v);", name, dynamic, defaultValue);
               } else {
                   javaBody = String.format("return getGeneralizedTimeAttr(Provisioning.A_%s, null, true);", name);
               }
               javaDocReturns = " as Date, null if unset or unable to parse";
               asStringDoc = true;
               break;
           default:
               if (ai.getCardinality() != AttributeCardinality.multi) {
                   if (defaultValue != null) {
                       defaultValue = "\"" + defaultValue.replace("\"", "\\\"") +"\"";
                   } else {
                       defaultValue = "null";
                   }
                   javaType = "String";
                   if (ai.isEphemeral()) {
                       javaBody = String.format("return getEphemeralAttr(Provisioning.A_%s, %s).getValue(%s);", name, dynamic, defaultValue);
                   } else {
                       javaBody = String.format("return getAttr(Provisioning.A_%s, %s, true);", name, defaultValue);
                   }
                   javaDocReturns = String.format(", or %s if unset", defaultValue);
               } else {
                   if (ai.isEphemeral()) {
                       javaType = "String";
                       javaBody = String.format("return getEphemeralAttr(Provisioning.A_%s, %s).getValue(%s);", name, dynamic, defaultValue);
                   } else {
                       javaType = "String[]";
                       if (defaultValue == null) {
                           javaBody = String.format("return getMultiAttr(Provisioning.A_%s, true, true);", name);
                       } else {
                           javaBody = String.format("String[] value = getMultiAttr(Provisioning.A_%s, true, true); return value.length > 0 ? value : %s;", name, defaultValue);
                       }
                   }
                   javaDocReturns = ", or empty array if unset";
               }
               break;
       }

       result.append("\n    /**\n");
       if (ai.getDescription() != null) {
           result.append(FileGenUtil.wrapComments(StringUtil.escapeHtml(ai.getDescription()), 70, "     * "));
           result.append("\n");
       }
       if (ai.getType() == AttributeType.TYPE_ENUM) {
           result.append("     *\n");
           result.append(String.format("     * <p>Valid values: %s%n", ai.getEnumSet().toString()));
       }
       if (asStringDoc) {
           result.append("     *\n");
           result.append(String.format("     * <p>Use %sAsString to access value as a string.%n", methodName));
           result.append("     *\n");
           result.append(String.format("     * @see #%sAsString()%n", methodName));
       }
       if (ai.isEphemeral()) {
           result.append("     *\n");
           result.append("     * Ephemeral attribute - requests routed to EphemeralStore\n");
           result.append("     *\n");
           result.append("     * @throws com.zimbra.common.service.ServiceException if error on accessing ephemeral data\n");
       }
       result.append("     *\n");
       result.append(String.format("     * @return %s%s%n", name, javaDocReturns));
       if (ai.getSince() != null) {
           result.append("     *\n");
           result.append(String.format("     * @since ZCS %s%n", versionListAsString(ai.getSince())));
       }
       result.append("     */\n");
       result.append(String.format("    @ZAttr(id=%d)%n", ai.getId()));
       result.append(String.format("    public %s %s(%s)", javaType, methodName, ai.isDynamic() ? "String dynamicComponent" :""));
       if (ai.isEphemeral()) {
           result.append(" throws com.zimbra.common.service.ServiceException");
       }
       result.append(String.format(" {%n        %s%n    }%n", javaBody));
   }

   private static String versionListAsString(List<Version> versions) {
       if (versions == null || versions.size() == 0) {
           return "";
       } else if (versions.size() == 1) {
           return versions.iterator().next().toString();
       } else {
           StringBuilder sb = new StringBuilder();
           for (Version version : versions) {
               sb.append(version.toString()).append(",");
           }
           sb.setLength(sb.length() - 1);
           return sb.toString();
       }
   }

   @VisibleForTesting
   static enum SetterType { set, add, unset, remove, /* these two are for ephemeral attrs */ purge, has }

   private static void generateSetters(StringBuilder result, AttributeInfo ai, boolean asString, SetterType setterType) throws ServiceException {
       generateSetter(result, ai, asString, setterType, true);
       generateSetter(result, ai, asString, setterType, false);
   }

   @VisibleForTesting
   public static void generateSetter(StringBuilder result, AttributeInfo ai, boolean asString, SetterType setterType, boolean noMap) throws ServiceException {
       if (ai.isEphemeral()) {
           if (!noMap) {
               return; //don't generate any epheemeral setters with the map parameter
           } else if (ai.isDynamic() && (setterType == SetterType.unset || setterType == SetterType.set)) {
               //don't generate ephemeral setters/unsetters for dynamic ephemeral attributes,
               //since we don't support deleting all values for a key.
               return;
           }
       }
       String javaType;
       String putParam;

       String name = ai.getName();

       AttributeType type = asString ? AttributeType.TYPE_STRING : ai.getType();

       String methodName = ai.getName();
       if (methodName.startsWith("zimbra")) methodName = methodName.substring(6);
       methodName = setterType.name()+methodName.substring(0,1).toUpperCase() + methodName.substring(1);
       if (asString) methodName += "AsString";

       switch (type) {
           case TYPE_BOOLEAN:
               javaType = "boolean";
               putParam = String.format("%s ? TRUE : FALSE", name);
               break;
           case TYPE_BINARY:
           case TYPE_CERTIFICATE:
               javaType = "byte[]";
               putParam = String.format("%s==null ? \"\" : ByteUtil.encodeLDAPBase64(%s)", name, name);
               break;
           case TYPE_INTEGER:
           case TYPE_PORT:
               javaType = "int";
               putParam = String.format("Integer.toString(%s)", name);
               break;
           case TYPE_LONG:
               javaType = "long";
               putParam = String.format("Long.toString(%s)", name);
               break;
           case TYPE_GENTIME:
               javaType = "Date";
               putParam = String.format("%s==null ? \"\" : LdapDateUtil.toGeneralizedTime(%s)", name, name);
               break;
           case TYPE_ENUM:
               javaType = "ZAttrProvisioning." + enumName(ai);
               putParam = String.format("%s.toString()", name);
               break;
           default:
               if (ai.getCardinality() != AttributeCardinality.multi) {
                   javaType = "String";
                   putParam = String.format("%s", name);
               } else {
                   if (setterType == SetterType.set) javaType = "String[]";
                   else javaType = "String";
                   putParam = String.format("%s", name);
               }
               break;
       }

       String  mapType= "Map<String,Object>";

       result.append("\n    /**\n");
       if (ai.getDescription() != null) {
           result.append(FileGenUtil.wrapComments(StringUtil.escapeHtml(ai.getDescription()), 70, "     * "));
           result.append("\n");
       }
       if (ai.getType() == AttributeType.TYPE_ENUM) {
           result.append("     *\n");
           result.append(String.format("     * <p>Valid values: %s%n", ai.getEnumSet().toString()));
       }
       result.append("     *\n");

       StringBuilder paramDoc = new StringBuilder();
       String  body = "";
       if (ai.isEphemeral()) {
           paramDoc.append("     * Ephemeral attribute - requests routed to EphemeralStore\n");
           paramDoc.append("     *\n");
       }
       String expiry = ai.isExpirable() ? "expiration" : "null";
       String dynamic = ai.isDynamic() ? "dynamicComponent" : "null";
       switch(setterType) {
           case set:
               if (ai.isEphemeral()) {
                   body = String.format("        modifyEphemeralAttr(Provisioning.A_%s, %s, %s, false, %s);%n", name, dynamic, putParam, expiry);
               } else {
                   body = String.format("        attrs.put(Provisioning.A_%s, %s);%n", name, putParam);
               }
               paramDoc.append(String.format("     * @param %s new value%n", name));
               break;
           case add:
               if (ai.isEphemeral()) {
                   body = String.format("        modifyEphemeralAttr(Provisioning.A_%s, %s, %s, true, %s);%n", name, dynamic, putParam, expiry);
               } else {
                   body = String.format("        StringUtil.addToMultiMap(attrs, \"+\" + Provisioning.A_%s, %s);%n",name, name);
               }
               paramDoc.append(String.format("     * @param %s new to add to existing values%n", name));
               break;
           case remove:
               if (ai.isEphemeral()) {
                   body = String.format("        deleteEphemeralAttr(Provisioning.A_%s, %s, %s);%n", name, dynamic, putParam);
               } else {
                   body = String.format("        StringUtil.addToMultiMap(attrs, \"-\" + Provisioning.A_%s, %s);%n",name, name);
               }
               paramDoc.append(String.format("     * @param %s existing value to remove%n", name));
               break;
           case unset:
               if (ai.isEphemeral()) {
                   body = String.format("        deleteEphemeralAttr(Provisioning.A_%s);%n", name);
               } else {
                   body = String.format("        attrs.put(Provisioning.A_%s, \"\");%n", name);
               }
               // paramDoc = null;
               break;
           case purge:
               body = String.format("        purgeEphemeralAttr(Provisioning.A_%s);%n", name);
               break;
           case has:
               body = String.format("        return hasEphemeralAttr(Provisioning.A_%s, %s);%n", name, dynamic);
               break;
    default:
        break;
       }

       if (paramDoc != null) result.append(paramDoc.toString());
       if (!noMap) {
           result.append(String.format("     * @param attrs existing map to populate, or null to create a new map%n"));
           result.append("     * @return populated map to pass into Provisioning.modifyAttrs\n");
       } else {
           result.append("     * @throws com.zimbra.common.service.ServiceException if error during update\n");
       }
       if (ai.getSince() != null) {
           result.append("     *\n");
           result.append(String.format("     * @since ZCS %s%n", versionListAsString(ai.getSince())));
       }
       result.append("     */\n");
       result.append(String.format("    @ZAttr(id=%d)%n", ai.getId()));
       if (noMap) {
           String expiryParam = ai.isExpirable() ? ", com.zimbra.cs.ephemeral.EphemeralInput.Expiration expiration" : "";
           if (ai.isEphemeral()) {
               switch (setterType) {
               case set:
                   result.append(String.format("    public void %s(%s %s%s) throws com.zimbra.common.service.ServiceException {%n", methodName, javaType, name, expiryParam));
                   break;
               case add:
                   if (ai.isDynamic()) {
                       result.append(String.format("    public void %s(String dynamicComponent, %s %s%s) throws com.zimbra.common.service.ServiceException {%n", methodName, javaType, name, expiryParam));
                   } else {
                       result.append(String.format("    public void %s(%s %s%s) throws com.zimbra.common.service.ServiceException {%n", methodName, javaType, name, expiryParam));
                   }
                   break;
               case unset:
                   result.append(String.format("    public void %s() throws com.zimbra.common.service.ServiceException {%n", methodName));
                   break;
               case remove:
                   if (ai.isDynamic()) {
                       result.append(String.format("    public void %s(String dynamicComponent, %s %s) throws com.zimbra.common.service.ServiceException {%n", methodName, javaType, name));
                   } else {
                       result.append(String.format("    public void %s(%s %s) throws com.zimbra.common.service.ServiceException {%n", methodName, javaType,  name));
                   }
                   break;
               case purge:
                   result.append(String.format("    public void %s() throws com.zimbra.common.service.ServiceException {%n", methodName));
                   break;
               case has:
                   if (ai.isDynamic()) {
                       result.append(String.format("    public boolean %s(String dynamicComponent) throws com.zimbra.common.service.ServiceException {%n", methodName));
                   } else {
                       result.append(String.format("    public boolean %s() throws com.zimbra.common.service.ServiceException {%n", methodName));
                   }
                   break;
               }
           } else {
               if (setterType !=  SetterType.unset)
                   result.append(String.format("    public void %s(%s %s) throws com.zimbra.common.service.ServiceException {%n", methodName, javaType, name));
               else
                   result.append(String.format("    public void %s() throws com.zimbra.common.service.ServiceException {%n", methodName));
           }
           if (!ai.isEphemeral()) {
               result.append(String.format("        HashMap<String,Object> attrs = new HashMap<String,Object>();%n"));
           }
           result.append(body);
           if (!ai.isEphemeral()) {
               result.append(String.format("        getProvisioning().modifyAttrs(this, attrs);%n"));
           }
       } else {
           if (setterType !=  SetterType.unset)
               result.append(String.format("    public %s %s(%s %s, %s attrs) {%n", mapType, methodName, javaType, name, mapType));
           else
               result.append(String.format("    public %s %s(%s attrs) {%n", mapType, methodName, mapType));
           result.append(String.format("        if (attrs == null) attrs = new HashMap<String,Object>();%n"));
           result.append(body);
           result.append(String.format("        return attrs;%n"));
       }

       result.append(String.format("    }%n"));
    }


   /**
    *
    * @param pw
    * @param optionValue
    * @throws ServiceException
    */
   private void generateProvisioningConstants(String javaFile) throws ServiceException, IOException {
       List<String> list = new ArrayList<String>(getAttrs().keySet());
       Collections.sort(list);

       StringBuilder result = new StringBuilder();

       for (String a : list) {
           AttributeInfo ai = getAttrs().get(a.toLowerCase());
           if (ai == null || ai.getType() != AttributeType.TYPE_ENUM)
               continue;
           generateEnum(result, ai);
       }

       for (String a : list) {
           AttributeInfo ai = getAttrs().get(a.toLowerCase());
           if (ai == null)
               continue;

           result.append("\n    /**\n");
           if (ai.getDescription() != null) {
               result.append(FileGenUtil.wrapComments(StringUtil.escapeHtml(ai.getDescription()), 70, "     * "));
               result.append("\n");
           }
           if (ai.getSince() != null) {
               result.append("     *\n");
               result.append(String.format("     * @since ZCS %s%n", versionListAsString(ai.getSince())));
           }
           result.append("     */\n");
           result.append(String.format("    @ZAttr(id=%d)%n", ai.getId()));

           result.append(String.format("    public static final String A_%s = \"%s\";%n", ai.getName(), ai.getName()));
       }

       FileGenUtil.replaceJavaFile(javaFile, result.toString());

    }

    private static void usage(String errmsg) {
        if (errmsg != null) {
            logger.error(errmsg);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("AttributeManagerUtil [options] where [options] are one of:", options);
        System.exit((errmsg == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        StringBuffer gotCL = new StringBuffer("cmdline: ");
        for (int i = 0; i < args.length; i++) {
            gotCL.append("'").append(args[i]).append("' ");
        }
        //mLog.info(gotCL);

        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        if (cl.hasOption('h')) {
            usage(null);
        }
        return cl;
    }

    public static void main(String[] args) throws IOException, ServiceException {
        CliUtil.toolSetup();
        CommandLine cl = parseArgs(args);

        if (!cl.hasOption('a')) usage("no action specified");
        String actionStr = cl.getOptionValue('a');
        Action action = null;
        try {
            action = Action.valueOf(actionStr);
        } catch (IllegalArgumentException iae) {
            usage("unknown action: " + actionStr);
        }

        AttributeManager am = null;
        if (action != Action.dump && action != Action.listAttrs) {
            if (!cl.hasOption('i')) usage("no input attribute xml files specified");
            am = new AttributeManager(cl.getOptionValue('i'));
            if (am.hasErrors()) {
                ZimbraLog.misc.warn(am.getErrors());
                System.exit(1);
            }
        }

        OutputStream os = System.out;
        if (cl.hasOption('o')) {
            os = new FileOutputStream(cl.getOptionValue('o'));
        }
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, "utf8")));

        AttributeManagerUtil amu = new AttributeManagerUtil(am);

        switch (action) {
        case dump:
            LdapProv.getInst().dumpLdapSchema(pw);
            break;
        case generateDefaultCOSLdif:
            amu.generateDefaultCOSLdif(pw);
            break;
        case generateDefaultExternalCOSLdif:
            amu.generateDefaultExternalCOSLdif(pw);
            break;
        case generateGetters:
            amu.generateGetters(cl.getOptionValue('c'), cl.getOptionValue('r'));
            break;
        case generateGlobalConfigLdif:
            amu.generateGlobalConfigLdif(pw);
            break;
        case generateLdapSchema:
            if (!cl.hasOption('t')) {
                usage("no schema template specified");
            }
            amu.generateLdapSchema(pw, cl.getOptionValue('t'));
            break;
        case generateMessageProperties:
            amu.generateMessageProperties(cl.getOptionValue('r'));
            break;
        case generateProvisioning:
            amu.generateProvisioningConstants(cl.getOptionValue('r'));
            break;
        case generateSchemaLdif:
            amu.generateSchemaLdif(pw);
            break;
        case listAttrs:
            amu.listAttrs(pw, cl.getOptionValues('c'), cl.getOptionValues('n'), cl.getOptionValues('f'));
            break;
        }

        pw.close();
    }
}
