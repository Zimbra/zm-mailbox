package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;

public enum Right {
    
    RT_invite("invite"),
    RT_private("private"),
    RT_viewFreeBusy("viewFreeBusy");
    
    static {        
        
        RT_invite.setDesc("automatically add meeting invites from grantee to the target's calendar");
        RT_invite.setDoc("e.g. " +
        		         "(1) When user Y is invited to a meeting, an appt is added to his calendar automatically(tentatively) only if invite is from A, B, C or anyone in group G. " +
                         "(2) Conf room Y can only be booked by users A, B, C and group G.");

        RT_private.setDesc("view, create, delete, modify private content");
        RT_private.setDoc("e.g. view, create, delete, modify private appointments");
        
        RT_viewFreeBusy.setDesc("view free/busy");
        RT_viewFreeBusy.setDoc("e.g. Free/busy for Y can only be seen by users A, B, C and group G.");
        
        
    }
    
    private static class RT {
        static Map<String, Right> sCodeMap = new HashMap<String, Right>();
    }
    
    private String mCode;
    private String mDesc;  // a brief description
    private String mDoc;   // a more detailed description, use cases, examples
    
    Right(String code) {
        mCode = code;
        RT.sCodeMap.put(code, this);
    }
    
    public static Right fromCode(String right) throws ServiceException {
        Right rt = RT.sCodeMap.get(right);
        if (rt == null)
            throw ServiceException.PARSE_ERROR("invalid right: " + right, null);
        return rt;
    }
    
    /**
     * - code stored in the ACE.
     * - code appear in XML
     * - code displayed by CLI
     * 
     * @return 
     */
    public String getCode() {
        return mCode;
    }
    
    public String getDesc() {
        return mDesc;
    }
    
    public String getDoc() {
        return mDoc;
    }
    
    private void setDesc(String desc) {
        mDesc = desc;
    }
    
    private void setDoc(String doc) {
        mDoc = doc;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        for (Right r : Right.values()) {
            System.out.println(r.getCode());
            System.out.println("    desc: " + r.getDesc());
            System.out.println("    doc: " + r.getDoc());
            System.out.println();
        }
    }

}
