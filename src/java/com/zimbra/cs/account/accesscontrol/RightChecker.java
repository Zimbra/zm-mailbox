package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.AccessManager.AllowedAttrs;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.MemberOf;

public class RightChecker {

    private static final Log sLog = LogFactory.getLog(RightChecker.class);
    
    /**
     * Helper class for checking rights.
     * 
     * Wraps:
     *   - List of ACEs on an entry with the needed right, negative grants are in the front
     *   - TargetType on which the right is granted
     *   - Entry on which the right is granted
     *   - Distance to the perspective target
     *   
     *   e.g for this hierarchy:
     *      grantA on globalgrant                                  
     *      grantB on domain test.com                              
     *      grantC on group g1@test.com                            
     *      grantD on group g2@test.com (g2 is a member of g1 and user is a member of g2)     
     *      grantE on group g3@test.com (user is a member of g3)     
     *      grantF on account user@test.com                        
     *      
     *   distance would be:
     *     for canPerform(..., user@test.com, ...), distance for each grant is:
     *         grantF: 0
     *         grantE: 1
     *         grantD: 1
     *         grantC: 2
     *         grantB: 3
     *         grantA: 4
     *         
     *     for canPerform(..., g3@test.com, ...), distance for each grant is:
     *         grantE: 0
     *         grantB: 1
     *         grantA: 2
     *         
     *     for canPerform(..., g2@test.com, ...), distance for each grant is:    
     *         grantD: 0
     *         grantC: 1
     *         grantB: 2
     *         grantA: 3
     *
     *     for canPerform(..., g1@test.com, ...), distance for each grant is:    
     *         grantC: 0
     *         grantB: 1
     *         grantA: 1
     *         
     *     for canPerform(..., test.com, ...), distance for each grant is:    
     *         grantB: 0
     *         grantA: 1
     *         
     *     for canPerform(..., globalgrant, ...), distance for each grant is:    
     *         grantA: 0
     *   
     */
    static class EffectiveACL {
        private TargetType mGrantedOnEntryType; // target type on which the aces are granted
        private Entry mGrantedOnEntry;          // entry object on which the aces are granted
        private int mDistanceToTarget;          // distance to the perspective target
        
        private List<ZimbraACE> mAces;          // grants on the entry with the the right of interest
                
        EffectiveACL(TargetType grantedOnEntryType, Entry grantedOnEntry, int distanceToTarget, List<ZimbraACE> aces) throws ServiceException {
            mGrantedOnEntryType = grantedOnEntryType;
            mGrantedOnEntry = grantedOnEntry;
            mDistanceToTarget = distanceToTarget;
            
            // sanity check, an EffectiveACL must have aces, otherwise it should not be constructed
            if (aces == null)
                throw ServiceException.FAILURE("internal error", null);
            mAces = aces;
        }
        
        List<ZimbraACE> getAces() {
            return mAces;
        }
        
        TargetType getGrantedOnEntryType() {
            return mGrantedOnEntryType;
        }
        
        Entry getGrantedOnEntry() {
            return mGrantedOnEntry;
        }
       
        int getDistanceToTarget() {
            return mDistanceToTarget;
        }
        
        /*
         * ({entry name on which the right is granted}: [grant] [grant] ...)
         */
        String dump() {
            StringBuffer sb = new StringBuffer();
            
            sb.append("(" + mGrantedOnEntry.getLabel() + ": ");
            for (ZimbraACE ace : mAces)
                sb.append(ace.dump() + " ");
            sb.append(")");
            
            return sb.toString();
        }
        
        // dump a List of EffectiveACL as ({entry name on which the right is granted}: [grant] [grant] ...)
        static String dump(List<EffectiveACL> effectiveACLs) {
            StringBuffer sb = new StringBuffer();
            for (EffectiveACL acl : effectiveACLs) {
                sb.append(acl.dump() + " ");
            }
            return sb.toString();
        }
    }
    
    static class ACEViaGrant extends ViaGrant {
        String mTargetType;
        String mTargetName;
        String mGranteeType;
        String mGranteeName;
        String mRight;
        boolean mIsNegativeGrant;
        
        ACEViaGrant(TargetType targetType,
                    Entry target,
                    GranteeType granteeType,
                    String granteeName,
                    Right right,
                    boolean isNegativeGrant) {
            mTargetType = targetType.getCode();
            mTargetName = target.getLabel();
            mGranteeType = granteeType.getCode();
            mGranteeName = granteeName;
            mRight = right.getName();
            mIsNegativeGrant = isNegativeGrant;
        }
        
        public String getTargetType() { 
            return mTargetType;
        } 
        
        public String getTargetName() {
            return mTargetName;
        }
        
        public String getGranteeType() {
            return mGranteeType;
        }
        
        public String getGranteeName() {
            return mGranteeName;
        }
        
        public String getRight() {
            return mRight;
        }
        
        public boolean isNegativeGrant() {
            return mIsNegativeGrant;
        }
    }
    
    private static class AttrDist {
        int mDistance;
        boolean mLimit;
        
        AttrDist(AttrRight.Attr attr, int distance) {
            mLimit = attr.getLimit();
            mDistance = distance;
        }
        
        static String dump(Map<String, AttrDist> attrs) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, AttrDist> a : attrs.entrySet())
                sb.append("(" + a.getKey() + ", " + a.getValue().mDistance +  ", " + a.getValue().mLimit + ")");
            return sb.toString();
        }
    }
    
    private static class CollectAttrsResult {
        enum Result {
            SOME,
            ALLOW_ALL,
            DENY_ALL;
        }
        
        CollectAttrsResult(Result result, int distance) {
            mResult = result;
            mDistance = distance;
        }
        
        Result mResult;
        int mDistance;  // distance for next rank of grants
    }
    
    /**
     * returns if grantee 
     * 
     * @param effectiveACEs all grants on the target hierarchy for the requested right
     * @param grantee
     * @return
     * @throws ServiceException
     */
    static boolean canDo(List<EffectiveACL> effectiveACLs, Account grantee, Right right, ViaGrant via) throws ServiceException {
        Boolean result = null;
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("RightChecker.canDo: effectiveACLs=" + EffectiveACL.dump(effectiveACLs));
        }
        
        short adminFlag = (right.isUserRight()? 0 : GranteeFlag.F_ADMIN);
        
        // as an individual
        result = check(effectiveACLs, grantee, right, via, (short)(GranteeFlag.F_INDIVIDUAL | adminFlag));
        if (result != null)
            return result;
        
        // as a group member
        result = checkGroup(effectiveACLs, grantee, right, via, (short)(GranteeFlag.F_GROUP | adminFlag));
        if (result != null)
            return result;
        
        if (right.isUserRight()) {
            // as an authed user
            result = check(effectiveACLs, grantee, right, via, GranteeFlag.F_AUTHUSER);
            if (result != null)
                return result;
            
            // as public 
            result = check(effectiveACLs, grantee, right, via, GranteeFlag.F_PUBLIC);
            if (result != null)
                return result;
        }
        
        // no match, return denied
        return false;
    }
    
    private static Set<String> buildAllowedWithLimit(Map<String, AttrDist> allowSome) {
        // build the limited set
        Set<String> allowedWithlimit = new HashSet<String>();
        for (Map.Entry<String, AttrDist> a : allowSome.entrySet()) {
            if (a.getValue().mLimit) 
                allowedWithlimit.add(a.getKey());
        }
        return allowedWithlimit;
    }
    
    private static AllowedAttrs processDenyAll(Map<String, AttrDist> allowSome, Map<String, AttrDist> denySome, AttributeClass klass) throws ServiceException {
        if (allowSome.isEmpty())
            return AccessManager.DENY_ALL_ATTRS;
        else {
            Set<String> allowed = allowSome.keySet();
            Set<String> allowedWithlimit = buildAllowedWithLimit(allowSome);
            return AccessManager.ALLOW_SOME_ATTRS(allowed, allowedWithlimit);
        }
    }


    private static AllowedAttrs processAllowAll(Map<String, AttrDist> allowSome, Map<String, AttrDist> denySome, AttributeClass klass) throws ServiceException {
        
        if (denySome.isEmpty()) {
            // return AccessManager.ALLOW_ALL_ATTRS;
            if (allowSome.isEmpty())
                return AccessManager.ALLOW_ALL_ATTRS;
            else {
                // there could be some attrs with limit in the allowSome list,
                // remove those from allowed and put them in the allowed with limit set
             
                Set<String> allowed = new HashSet<String>();
                allowed.addAll(AttributeManager.getInstance().getAttrsInClass(klass));
                
                // build the limited set
                Set<String> allowedWithlimit = new HashSet<String>();
                for (Map.Entry<String, AttrDist> a : allowSome.entrySet()) {
                    if (a.getValue().mLimit && allowed.contains(a.getKey())) {
                        allowedWithlimit.add(a.getKey());
                        allowed.remove(a.getKey());
                    }
                }
                
                return allowedWithlimit.isEmpty()? 
                           AccessManager.ALLOW_ALL_ATTRS:  // didn't find any with limit attrs in allowSome, so return allow all
                           AccessManager.ALLOW_SOME_ATTRS(allowed, allowedWithlimit);
            }

        } else {
            // get all attrs that can appear on the target entry
            Set<String> allowed = new HashSet<String>();
            allowed.addAll(AttributeManager.getInstance().getAttrsInClass(klass));
            
            // remove denied from all
            for (String d : denySome.keySet())
                allowed.remove(d);
            
            // build the limited set
            Set<String> allowedWithlimit = new HashSet<String>();
            for (Map.Entry<String, AttrDist> a : allowSome.entrySet()) {
                if (a.getValue().mLimit && allowed.contains(a.getKey())) 
                    allowedWithlimit.add(a.getKey());
            }
            
            return AccessManager.ALLOW_SOME_ATTRS(allowed, allowedWithlimit);
        }
    }
    
    static AllowedAttrs canAccessAttrs(List<EffectiveACL> effectiveACLs, Account grantee, Right right, AttributeClass klass) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        AllowedAttrs result = null;
        
        Map<String, AttrDist> allowSome = new HashMap<String, AttrDist>();
        Map<String, AttrDist> denySome = new HashMap<String, AttrDist>();
        
        /*
         * traverse the effective ACLs, which is sorted in the order of most to least specific targets.
         * first pass visits the user grantees, second pass visits the group grantees
         */
        
        int baseDistance = 0;
        CollectAttrsResult car;
        // collect attrs in grants that are granted directly to the authed account
        // begin with base distance 0
        car = collectAttrs(effectiveACLs, grantee.getId(), 0, allowSome, denySome);
        
        if (car.mResult == CollectAttrsResult.Result.SOME) {
            // walk up the group hierarchy of the authed account, collect attrs that are granted to the group
            // use the distance from previous call to collectAttrs as the base instance
            List<MemberOf> groups = prov.getAclGroups((Account)grantee);
            for (MemberOf group : groups) {
                car = collectAttrs(effectiveACLs, group.getId(), car.mDistance, allowSome, denySome);
                if (car.mResult != CollectAttrsResult.Result.SOME)
                    break; // stop if allow/deny all was encountered
            }
        }
        
        if (sLog.isDebugEnabled()) {
            if (car.mResult == CollectAttrsResult.Result.ALLOW_ALL)
                sLog.debug("allow all at: " + car.mDistance);
            else if (car.mResult == CollectAttrsResult.Result.DENY_ALL)
                sLog.debug("deny all at: " + car.mDistance);
            
            sLog.debug("allowSome: " + AttrDist.dump(allowSome));
            sLog.debug("denySome: " + AttrDist.dump(denySome));
        }
        
        if (car.mResult == CollectAttrsResult.Result.ALLOW_ALL)
            result = processAllowAll(allowSome, denySome, klass);
        else if (car.mResult == CollectAttrsResult.Result.DENY_ALL)
            result = processDenyAll(allowSome, denySome, klass);
        else {
            // now allowSome and denySome contain attrs allowed/denied and their shortest distance 
            // to the target, remove denied ones from allowed if they've got a shorter distance
            Set<String> conflicts = SetUtil.intersect(allowSome.keySet(), denySome.keySet());
            if (!conflicts.isEmpty()) {
                for (String attr : conflicts) {
                    if (denySome.get(attr).mDistance <= allowSome.get(attr).mDistance)
                        allowSome.remove(attr);
                }
            }
            Set<String> allowSomeWithLimit = buildAllowedWithLimit(allowSome);
            result = AccessManager.ALLOW_SOME_ATTRS(allowSome.keySet(), allowSomeWithLimit);
        }
        
        return result;
    }

    
    /**
     * Iterate through the effective ACLs to determine if the right is allowed or denied to the grantee.
     * 
     * The effectiveACLs list contains all grants on all entries with the specified right that could 
     * influence the result.   Each EffectiveACL contains grants with the specified right on one target entry.
     * 
     * The List is sorted as follows:
     * - from the most specific target entry to the least specific target entry
     * - on the same target entry, negative grants are in the front, positive grants are in the rear
     * 
     * @param effectiveACLs
     * @param grantee
     * @param right
     * @param via
     * @param granteeFlags
     * @return
     * @throws ServiceException
     */
    private static Boolean check(List<EffectiveACL> effectiveACLs, Account grantee, Right right, ViaGrant via, short granteeFlags) throws ServiceException {
        Boolean result = null;
        for (EffectiveACL acl : effectiveACLs) {
            for (ZimbraACE ace : acl.getAces()) {
                GranteeType granteeType = ace.getGranteeType();
                if (!granteeType.hasFlags(granteeFlags))
                    continue;
                
                if (ace.matchesGrantee(grantee))
                    return gotResult(acl, ace, grantee, right, via);
            }
        }
        return result;
    }

    /**
     * Check group grantees.  Group grantees are checked differently because we need to take consideration 
     * relativity of a group grantee to the perspective authed user - relativity on target hierarchy and 
     * grantee hierarchy can conflict.  
     * 
     * For example, for this target hierarchy:
     *      domain D
     *          group G1 (allow right R to group GC)
     *              group G2 (deny right R to group GB)
     *                  group G3 (deny right R to group GA)
     *                      user account U   
     *                  
     *  And this grantee hierarchy:
     *      group GA
     *          group GB
     *              group GC
     *                  (admin) account A
     *              
     *  Then A is *allowed* for right R on target account U, because GC is more specific to A than GA and GB.
     *  Even if on the target side, grant on G3(grant to GA) and G2(grant to GB) is more specific than the 
     *  grant on G1(grant to GC).          
     * 
     * @param effectiveACLs
     * @param grantee
     * @param right
     * @param via
     * @param granteeFlags
     * @return
     * @throws ServiceException
     */
    private static Boolean checkGroup(List<EffectiveACL> effectiveACLs, Account grantee, Right right, ViaGrant via, short granteeFlags) throws ServiceException {
        
        EffectiveACL mostSpecificToGrantee_theTarget = null;
        ZimbraACE    mostSpecificToGrantee_theGrant = null;
        int          mostSpecificToGrantee_theDistance = -1;
        
        List<MemberOf> memberOf = Provisioning.getInstance().getAclGroups(grantee);
        
        for (EffectiveACL acl : effectiveACLs) {
            for (ZimbraACE ace : acl.getAces()) {
                GranteeType granteeType = ace.getGranteeType();
                if (!granteeType.hasFlags(granteeFlags))
                    continue;
                
                if (ace.matchesGrantee(grantee)) {
                    
                    int distance = -1;
                    String granteeId = ace.getGrantee();
                    for (MemberOf mo : memberOf) {
                        if (mo.getId().equals(granteeId)) {
                            distance = mo.getDistance();
                            break;
                        }
                    }
                    
                    // not really possible, since we've already passed the matchesGrantee test
                    if (distance == -1)
                        continue;  // log?
                    
                    if (mostSpecificToGrantee_theDistance == -1 ||
                        distance < mostSpecificToGrantee_theDistance ||
                        (distance == mostSpecificToGrantee_theDistance && ace.deny())) {
                        // found a more specific grant, or an ace with the same distance to 
                        // grantee but is a negative grant.
                        mostSpecificToGrantee_theTarget = acl;
                        mostSpecificToGrantee_theGrant = ace;
                        mostSpecificToGrantee_theDistance = distance;
                    }
                }
            }
        }
        
        if (mostSpecificToGrantee_theGrant == null)
            return null;  // nothing matched
        else
            return gotResult(mostSpecificToGrantee_theTarget, mostSpecificToGrantee_theGrant, 
                             grantee, right, via);
    }
    
    private static Boolean gotResult(EffectiveACL acl, ZimbraACE ace, Account grantee, Right right, ViaGrant via) {
        if (ace.deny()) {
            if (sLog.isDebugEnabled())
                sLog.debug("Right " + "[" + right.getName() + "]" + " DENIED to " + grantee.getName() + 
                           " via grant: " + ace.dump() + " on: " + acl.getGrantedOnEntry());
            if (via != null)
                via.setImpl(new ACEViaGrant(acl.getGrantedOnEntryType(),
                                            acl.mGrantedOnEntry,
                                            ace.getGranteeType(),
                                            ace.getGranteeDisplayName(),
                                            ace.getRight(),
                                            ace.deny()));
            return Boolean.FALSE;
        } else {
            if (sLog.isDebugEnabled())
                sLog.debug("Right " + "[" + right.getName() + "]" + " ALLOWED to " + grantee.getName() + 
                           " via grant: " + ace.dump() + " on: " + acl.getGrantedOnEntry());
            if (via != null)
                via.setImpl(new ACEViaGrant(acl.getGrantedOnEntryType(),
                                            acl.mGrantedOnEntry,
                                            ace.getGranteeType(),
                                            ace.getGranteeDisplayName(),
                                            ace.getRight(),
                                            ace.deny()));
            return Boolean.TRUE;
        }
    }

    
    private static CollectAttrsResult collectAttrs(List<EffectiveACL> effectiveACLs, String granteeId, int baseDistance,
                                    Map<String, AttrDist> allowSome, Map<String, AttrDist> denySome) throws ServiceException {
        
        int dist = baseDistance;
        
        CollectAttrsResult seenAllowAll = null;
        
        for (EffectiveACL acl : effectiveACLs) {
            // next farther target
            dist++;
            
            for (ZimbraACE ace : acl.getAces()) {
                dist++;
                
                Integer distance = Integer.valueOf(dist);
                
                if (ace.getGrantee().equals(granteeId)) {
                    // must be an AttrRight by now
                    AttrRight right = (AttrRight)ace.getRight();
                    
                    if (right.allAttrs()) {
                        // all attrs, return if we hit a grant with all attrs
                        if (ace.deny())
                            return new CollectAttrsResult(CollectAttrsResult.Result.DENY_ALL, dist);
                        else {
                            /*
                             * allow all, still need to visit all grants on the same target
                             * this is needed because for allow-all and allow-some-with-limit on the 
                             * same distance, those in allow-some-with limit should be allowed with 
                             * limit.  Grants on the same target are sorted so denied are in the 
                             * front, but allowed(with/wo limit) and allow-all can appear in random 
                             * order.   Remember the allow-all grant, and return it after we've gone 
                             * through all grants on this target to this grantee.
                             */
                            
                            // return new CollectAttrsResult(CollectAttrsResult.Result.ALLOW_ALL, dist);
                            seenAllowAll = new CollectAttrsResult(CollectAttrsResult.Result.ALLOW_ALL, dist);
                        }
                    } else {
                        // some attrs
                        for (AttrRight.Attr attr : right.getAttrs()) {
                            if (ace.deny())
                                denySome.put(attr.getAttrName(), new AttrDist(attr, distance));
                            else
                                allowSome.put(attr.getAttrName(), new AttrDist(attr, distance));
                        }
                    }
                }
            }
            
            if (seenAllowAll != null)
                return seenAllowAll;
            
        }
        
        return new CollectAttrsResult(CollectAttrsResult.Result.SOME, dist);
    }


    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
