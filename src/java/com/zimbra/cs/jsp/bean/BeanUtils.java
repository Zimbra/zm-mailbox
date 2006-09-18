package com.zimbra.cs.jsp.bean;

import java.util.List;

import com.zimbra.cs.zclient.ZEmailAddress;

public class BeanUtils {

    private static void addAddr(StringBuilder sb, ZEmailAddress email) {
        if (email == null) return;
        if (sb.length() > 0) sb.append(", ");        
        if (email.getDisplay() != null)
            sb.append(email.getDisplay());        
        else if (email.getPersonal() != null)
            sb.append(email.getPersonal());
        else if (email.getAddress() != null)
            sb.append(email.getAddress());
    }
    
    public static String getAddrs(List<ZEmailAddress> addrs) {
        if ( addrs == null) return "";
        StringBuilder sb = new StringBuilder();
        for (ZEmailAddress addr: addrs) {
            addAddr(sb, addr);
        }
        return sb.toString();
    }
    
    public static String getAddr(ZEmailAddress addr) {
        if ( addr == null) return "";
        else if (addr.getPersonal() != null)
            return addr.getPersonal();
        else if (addr.getAddress() != null)
            return addr.getAddress();
        else
            return "";
    }

}
