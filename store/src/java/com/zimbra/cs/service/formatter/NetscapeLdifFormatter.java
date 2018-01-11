package com.zimbra.cs.service.formatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;


public class NetscapeLdifFormatter extends LdifFormatter {

    private Map<String, String> netscapeAttrMap = new HashMap<String, String>();
    private static final String MOZILLA_HOME_STREET = "mozillaHomeStreet";
    private static final String MOZILLA_HOME_LOCALITY_NAME = "mozillaHomeLocalityName";
    private static final String MOZILLA_HOME_STATE = "mozillaHomeState";
    private static final String MOZILLA_HOME_POSTAL_CODE = "mozillaHomePostalCode";
    private static final String MOZILLA_HOME_COUNTRY_NAME = "mozillaHomeCountryName";
    private static final String MOZILLA_WORK_URL = "mozillaWorkUrl";
    private static final String MOZILLA_HOME_URL = "mozillaHomeUrl";
    private static final String MOZILLA_SECOND_EMAIL = "mozillaSecondEmail";
    private static final String MOZILLA_BIRTH_YEAR = "birthyear";
    private static final String MOZILLA_BIRTH_MONTH = "birthmonth";
    private static final String MOZILLA_BIRTH_DAY = "birthday";
    private static final String MOZILLA_WORK_COUNTRY = "c";
    private static final String MOZILLA_CUSTOM1 = "mozillaCustom1";
    private static final String MOZILLA_CUSTOM2 = "mozillaCustom2";
    private static final String MOZILLA_CUSTOM3 = "mozillaCustom3";
    private static final String MOZILLA_CUSTOM4 = "mozillaCustom4";
    private static final String MOZILLA_AIM = "nsAIMid";

    @Override
    public FormatType getType() {
        return FormatType.NETSCAPELDIF;
    }

    @Override
    protected boolean toLDIFContact(Map<String, String> contact, StringBuilder sb, String[] galLdapAttrMap) {
        populateNetscapeAttrMap();
        workCountryAttributeName = MOZILLA_WORK_COUNTRY;
        boolean hasReuiredFields = super.toLDIFContact(contact, sb, galLdapAttrMap);
        if (hasReuiredFields) {
            addNetscapeAttributes(sb, contact);
            return true;
        }
        return false;
    }

    private void populateNetscapeAttrMap() {
        netscapeAttrMap.put(ContactConstants.A_homeStreet, MOZILLA_HOME_STREET);
        netscapeAttrMap.put(ContactConstants.A_homeCity, MOZILLA_HOME_LOCALITY_NAME);
        netscapeAttrMap.put(ContactConstants.A_homeState, MOZILLA_HOME_STATE);
        netscapeAttrMap.put(ContactConstants.A_homePostalCode, MOZILLA_HOME_POSTAL_CODE);
        netscapeAttrMap.put(ContactConstants.A_homeCountry, MOZILLA_HOME_COUNTRY_NAME);
        netscapeAttrMap.put(ContactConstants.A_workURL, MOZILLA_WORK_URL);
        netscapeAttrMap.put(ContactConstants.A_homeURL, MOZILLA_HOME_URL);
        netscapeAttrMap.put(ContactConstants.A_email2, MOZILLA_SECOND_EMAIL);
        netscapeAttrMap.put(ContactConstants.A_workCountry, MOZILLA_WORK_COUNTRY);
        netscapeAttrMap.put(ContactConstants.A_custom1, MOZILLA_CUSTOM1);
        netscapeAttrMap.put(ContactConstants.A_custom2, MOZILLA_CUSTOM2);
        netscapeAttrMap.put(ContactConstants.A_custom3, MOZILLA_CUSTOM3);
        netscapeAttrMap.put(ContactConstants.A_custom4, MOZILLA_CUSTOM4);
    }

    private void addNetscapeAttributes(StringBuilder sb, Map<String, String> contact) {
        String birthday = contact.get(ContactConstants.A_birthday);
        if (!StringUtil.isNullOrEmpty(birthday)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date parse;
            try {
                parse = sdf.parse(birthday);
                Calendar c = Calendar.getInstance();
                c.setTime(parse);
                System.out
                    .println(c.get(Calendar.MONTH) + c.get(Calendar.DATE) + c.get(Calendar.YEAR));
                addLDIFEntry(MOZILLA_BIRTH_YEAR, String.valueOf(c.get(Calendar.YEAR)), sb,
                    false);
                addLDIFEntry(MOZILLA_BIRTH_MONTH, String.valueOf(c.get(Calendar.MONTH) + 1),
                    sb, false);
                addLDIFEntry(MOZILLA_BIRTH_DAY, String.valueOf(c.get(Calendar.DATE)), sb,
                    false);
            } catch (ParseException e) {
                ZimbraLog.contact.warn("Unable to parse birth date", e);
            }

        }
        Set<String> contactKeys = contact.keySet();
        for (String contactKey : contactKeys) {
            if (contactKey.startsWith("imAddress")) {
                String value = contact.get(contactKey);
                if (value.startsWith("aol://")) {
                    encodeAndAddLDIFEntry(MOZILLA_AIM, value, sb);
                    break;
                }
            }
        }
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_email2),
            contact.get(ContactConstants.A_email2), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_homeCity),
            contact.get(ContactConstants.A_homeCity), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_homeCountry),
            contact.get(ContactConstants.A_homeCountry), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_homePostalCode),
            contact.get(ContactConstants.A_homePostalCode), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_homeState),
            contact.get(ContactConstants.A_homeState), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_homeStreet),
            contact.get(ContactConstants.A_homeStreet), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_homeURL),
            contact.get(ContactConstants.A_homeURL), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_workURL),
            contact.get(ContactConstants.A_workURL), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_custom1),
            contact.get(ContactConstants.A_custom1), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_custom2),
            contact.get(ContactConstants.A_custom2), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_custom3),
            contact.get(ContactConstants.A_custom3), sb);
        encodeAndAddLDIFEntry(netscapeAttrMap.get(ContactConstants.A_custom4),
            contact.get(ContactConstants.A_custom4), sb);
    }
}
