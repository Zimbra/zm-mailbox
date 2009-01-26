package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

public enum RightModifier {
    RM_DENY('-', AdminConstants.A_DENY),                   // the right is specifically denied
    RM_CAN_DELEGATE('+', AdminConstants.A_CAN_DELEGATE);   // the same right or part of the right can be delegated(granted) to others
    
    // urg, our soap intereface is already published with with the deny attribute, 
    // for backward compatibility, we map the modifier to soap instead of changing soap 
    // to take a modifier attribute
    private String mSoapAttrMapping;
    private char mModifier;
    
    private RightModifier(char modifier, String soapAttrMapping) {
        mModifier = modifier;
        mSoapAttrMapping = soapAttrMapping;
    }
    
    public static RightModifier fromChar(char c) throws ServiceException {
        
        if (RM_DENY.mModifier == c)
            return RM_DENY;
        else if (RM_CAN_DELEGATE.mModifier == c)
            return RM_CAN_DELEGATE;
        else
            return null;
    }
    
    public char getModifier() {
        return mModifier;
    }
    
    public String getSoapAttrMapping() {
        return mSoapAttrMapping;
    }
}
