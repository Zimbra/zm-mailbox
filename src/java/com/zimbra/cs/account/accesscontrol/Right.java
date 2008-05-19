package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public enum Right {
    
    viewFreeBusy,
    invite;
    
    static {
        viewFreeBusy.setDesc("view free/busy");
        viewFreeBusy.setDoc("e.g. Free/busy for Y can only be seen by users A, B, C and group G.");
        
        invite.setDesc("automatically add meeting invites from grantee to the taeget's calender");
        invite.setDoc("e.g. " +
        		      "(1) When user Y is invited to a meeting, an appt is added to his calendar automatically(tentatively) only if invite is from A, B, C or anyone in group G. " +
                      "(2) Conf room Y can only be booked by users A, B, C and group G.");
    }
    
    private String mDesc;  // a brief description
    private String mDoc;   // a more detailed description, use cases, examples
    
    public static Right fromCode(String right) throws ServiceException {
        try {
            return Right.valueOf(right);
        } catch (IllegalArgumentException e) {
            throw ServiceException.PARSE_ERROR("invalid right: " + right, e);
        }
    }
    
    /**
     * - code stored in the ACE.
     * - code appear in XML
     * - code displayed by CLI
     * 
     * @return 
     */
    public String getCode() {
        return name();
    }
    
    String getDesc() {
        return mDesc;
    }
    
    String getDoc() {
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
