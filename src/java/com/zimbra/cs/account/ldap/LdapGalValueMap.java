package com.zimbra.cs.account.ldap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.util.ZimbraLog;

public class LdapGalValueMap {
    private String mFieldName;
    private Pattern mRegex;
    private String mReplacement;
    
    private static Pattern sPattern = Pattern.compile("(\\S*): (\\S*) (.*)");
    
    /**
     * 
     * 
     * @param valueMap in the format of {gal contact filed}: {regex} {replacement}
     */
    LdapGalValueMap(String valueMap) {
        Matcher matcher = sPattern.matcher(valueMap);
        if (matcher.matches()) {
            mFieldName = matcher.group(1);
            mRegex = Pattern.compile(matcher.group(2));
            mReplacement  = matcher.group(3);
            ZimbraLog.gal.debug("Gal value map: field=" + mFieldName + ", regex=" + mRegex + ", replacement=" + mReplacement);
        } else {
            ZimbraLog.gal.warn("unable to parse gal attr map map: " + valueMap);
        }
    }
    
    String getFieldName() {
        return mFieldName;
    }
    
    Object apply(Object value) {
        if (value instanceof String) {
            return replace((String)value); 
        } else if (value instanceof String[]) {
            String[] val = (String[])value;
            String[] newValue = new String[val.length];
            for (int i = 0; i < val.length; i++)
                newValue[i] = replace(val[i]);
            
            return newValue;
        } else
            return value;  // is really an internal error
    }
    
    private String replace(String value) {
        if (mRegex == null)
            return value;
        
        Matcher matcher = mRegex.matcher(value);
        if (matcher.matches())
            return matcher.replaceAll(mReplacement);
        else
            return value;
    }

    public static void main(String[] ags) {
        LdapGalValueMap valueMap = new LdapGalValueMap("zimbraCalResType: [R|r]oom Location");
        
        Object newVal = valueMap.apply("Room");
        System.out.println((String)newVal);
        
        newVal = valueMap.apply(new String[]{"Room", "room"});
        for (String v : (String[])newVal)
            System.out.println(v);
        
        valueMap = new LdapGalValueMap("zimbraAccountCalendarUserType: Room|Equipment RESOURCE");
        newVal = valueMap.apply("Equipment");
        System.out.println((String)newVal);
    }
}
