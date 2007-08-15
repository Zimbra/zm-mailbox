package com.zimbra.cs.account;

import org.apache.commons.collections.bidimap.DualHashBidiMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.zimbra.common.service.ServiceException;

public class Signature extends NamedEntry implements Comparable {

    private final String mAcctId;
    
    private static final DualHashBidiMap sAttrTypeMap = new DualHashBidiMap();

    static {
        sAttrTypeMap.put(Provisioning.A_zimbraPrefMailSignature, "text/plain");
        sAttrTypeMap.put(Provisioning.A_zimbraPrefMailSignatureHTML, "text/html");
    }
    
    public Signature(Account acct, String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs, null);
        mAcctId = acct.getId();
    }
    
    /**
     * this should only be used internally by the server. it doesn't modify the real id, just
     * the cached one.
     * @param id
     */
    public void setId(String id) {
        mId = id;
        getRawAttrs().put(Provisioning.A_zimbraSignatureId, id);
    }
    
    /*
     * get account of the signature
     */
    public Account getAccount() throws ServiceException {
        return Provisioning.getInstance().get(Provisioning.AccountBy.id, mAcctId);
    }
    
    public static class SignatureContent {
        private String mMimeType;
        private String mContent;
        
        public SignatureContent(String mimeType, String content) {
            mMimeType = mimeType;
            mContent = content;
        }
        
        public String getMimeType() { return mMimeType; }
        public String getContent() { return mContent; }
    }
    
    public Set<SignatureContent> getContents() {
        Set<SignatureContent> contents = new HashSet<SignatureContent>();
        
        for (Iterator it = sAttrTypeMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            
            String content = getAttr((String)entry.getKey());
            if (content != null)
                contents.add(new SignatureContent((String)entry.getValue(), content));
        }
        
        return contents;
    }
    
    public static String mimeTypeToAttrName(String mimeType) {
        return (String)sAttrTypeMap.getKey(mimeType);
    }
    
    public static String attrNameToMimeType(String attrName) {
        return (String)sAttrTypeMap.get(attrName);
    }

}
