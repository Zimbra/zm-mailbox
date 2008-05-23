package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;

public enum GranteeType {
    /*
     * Conflict resolution rules for multiple matches:
     * 
     * Rule 1. If multiple ACEs for a target apply to a grantee, the most specific ACE takes precedence.
     *         e.g. - ACL says: allow user A and deny group G
     *              - user A is in group G
     *              => user A will be allowed
     * 
     * Rule 2. If the grantee is in multiple matching groups in the ACL, the ACE for the most specific group takes precedence.
     *         e.g. - ALC says: allow group G1 and deny group G2
     *              - user A is in both group G1 and G2
     *              - group G1 is in Group G2
     *              => user A will be allowed
     *         
     * Rule 3. If multiple ACEs on the same level conflict for a target, then denied take precedence over allowed.
     *         e.g. - ACL says: allow group G1 and deny group G2
     *              - user A is in both group G1 and G2
     *              => user A will be denied.
     * 
     * 
     * A more complete example - not realistic, but an extent example:
     * 
     * For ACL:
     *      id-of-account-A  usr rightX
     *      id-of-group-G1   grp -rightX
     *      id-of-group-G2   grp rightX
     *      id-of-all-authed all -rightX
     *      id-of-the-public pub rightX
     *      
     * and group membership:
     *      group-G1 has members account-A and account-B and account-C
     *      group-G2 has members account-A and account-B and account-D and account-E
     *      group-G3 has members group G2 and account-E
     *   
     * then for rightX:
     *                       account-A is allowed (because "id-of-account-A  usr rightX"  - rule 1)
     *                        account-B is denied (because "id-of-group-G1   grp -rightX" - rule 3)
     *                        account-C is denied (because "id-of-group-G1   grp -rightX" - single match)
     *                       account-D is allowed (because "id-of-group-G2   grp rightX"  - single match)
     *                       account-E is allowed (because "id-of-group-G2   grp rightX"  - rule 2)
     *                        account-F is denied (because "id-of-all-authed all -rightX" - single match)
     *      external email foo@bar.com is allowed (because "id-of-the-public pub rightX"  - single match)
     *       
     */
    
    GT_USER("usr"),     // compare grantee ID with Account's zimbraId
    GT_GROUP("grp"),    // compare grantee ID with Account's zimbraMemberOf values
    GT_AUTHUSER("all"), // the caller needs to present a valid Zimbra credential
    GT_GUEST("gst"),    // the caller needs to present a non-Zimbra email address and password
    GT_PUBLIC("pub");   // always succeeds

    private static class GT {
        static Map<String, GranteeType> sCodeMap = new HashMap<String, GranteeType>();
    }
    
    private String mCode;
    
    GranteeType(String code) {
        mCode = code;
        GT.sCodeMap.put(code, this);
    }
    
    public static GranteeType fromCode(String granteeType) throws ServiceException {
        // GUEST-TODO master control for turning off guest grantee for now
        if (granteeType.equals(GT_GUEST.getCode()))
            throw ServiceException.FAILURE("guest grantee not yet supported", null);
        
        GranteeType gt = GT.sCodeMap.get(granteeType);
        if (gt == null)
            throw ServiceException.PARSE_ERROR("invalid grantee type: " + granteeType, null);
        
        return gt;
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


}
