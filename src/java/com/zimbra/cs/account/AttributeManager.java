package com.zimbra.cs.account;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

public class AttributeManager {

    private static final String ZIMBRA_ATTRS_RESOURCE = "liquidattrs.xml";

    private static final String E_ATTRS = "attrs";
    private static final String E_ATTR = "attr";
    private static final String A_NAME = "name";
    private static final String A_IMMUTABLE = "immutable";
    private static final String A_TYPE = "type";
    private static final String A_VALUE = "value";
    private static final String A_MAX = "max";
    private static final String A_MIN = "min";
    private static final String A_CALLBACK = "callback";

    private static AttributeManager mInstance;
    
    private HashMap mAttrs = new HashMap();

    public static AttributeManager getInstance() {
        if (mInstance == null) synchronized(AttributeManager.class) {
            if (mInstance == null)
                mInstance = new AttributeManager();
                String file = ZIMBRA_ATTRS_RESOURCE;
                
                InputStream is = null;
                try {
                    is = mInstance.getClass().getResourceAsStream(file);
                    if (is == null) {
                        ZimbraLog.misc.warn("unable to find attr file resource: "+file);
                    } else {
                        mInstance.loadAttrs(is, file);
                    }
                } catch (DocumentException e) {
                    ZimbraLog.misc.warn("unable to parse attr file: "+file+" "+e.getMessage(), e);
                } catch (Exception e) {
                    // swallow all of them
                    ZimbraLog.misc.warn("unable to load attr file: "+file+" "+e.getMessage(), e);                    
                } finally {
                    if (is != null)
                        try { is.close();}  catch (IOException e) { }
                }
        }
        return mInstance;
    }

    private void loadAttrs(InputStream attrsFile, String file) throws DocumentException {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(attrsFile);
            Element root = doc.getRootElement();
            if (!root.getName().equals(E_ATTRS)) {
                throw new DocumentException("attr file " + file + " root tag is not "+E_ATTRS);
            }
            for (Iterator iter = root.elementIterator(); iter.hasNext();) {
                Element eattr = (Element) iter.next();
                if (!eattr.getName().equals(E_ATTR)) {
                    ZimbraLog.misc.warn("attrs file("+file+") unknown element: "+eattr.getName());
                    continue;
                }
                String name = null;
                AttributeCallback callback = null;
                int type = AttributeInfo.TYPE_UNKNOWN;
                String value = null;
                int min = Integer.MIN_VALUE;
                int max = Integer.MAX_VALUE;
                boolean immutable = false;
                boolean ignore = false;
                
                for (Iterator attrIter = eattr.attributeIterator(); attrIter.hasNext();) {
                    Attribute attr = (Attribute) attrIter.next();
                    String aname = attr.getName();
                    if (aname.equals(A_NAME)) {
                        name = attr.getValue().toLowerCase();
                    } else if (aname.equals(A_CALLBACK)) {
                        callback = loadCallback(attr.getValue());
                    } else if (aname.equals(A_IMMUTABLE)) {
                        immutable = "1".equals(attr.getValue());
                    } else if (aname.equals(A_MAX)) {
                        max = AttributeInfo.parseInt(attr.getValue(), Integer.MAX_VALUE);
                    } else if (aname.equals(A_MIN)) {
                        min = AttributeInfo.parseInt(attr.getValue(), Integer.MIN_VALUE);
                    } else if (aname.equals(A_TYPE)) {
                         type = AttributeInfo.getType(attr.getValue());
                         if (type == AttributeInfo.TYPE_UNKNOWN) {
                             ZimbraLog.misc.warn("attrs file("+file+") unknown <attr> type: "+attr.getValue());
                             ignore = true;
                         }
                         
                    } else if (aname.equals(A_VALUE)) { 
                        value = attr.getValue();
                    } else {
                        ZimbraLog.misc.warn("attrs file("+file+") unknown <attr> attr: "+aname);
                    }
                }

                if (!ignore) {
                    if (name == null) {
                        ZimbraLog.misc.warn("attrs file("+file+") no name specified for attr");
                        continue;
                    }
                    if (type == AttributeInfo.TYPE_UNKNOWN) {
                        ZimbraLog.misc.warn("attrs file("+file+") no type specified for attr: "+name);
                        continue;
                    }
                    AttributeInfo info = new AttributeInfo(name, callback, type, value, immutable, min, max);
                    mAttrs.put(name, info);
                }
            }
    }

  /**
     * @param type
     * @return
     */
    private static AttributeCallback loadCallback(String clazz) {
        AttributeCallback cb = null;
        if (clazz == null)
            return null;
        if (clazz.indexOf('.') == -1)
            clazz = "com.zimbra.cs.account.callback." + clazz;
        try {
            cb = (AttributeCallback) Class.forName(clazz).newInstance();
        } catch (Exception e) {
            ZimbraLog.misc.warn("loadCallback caught exception", e);
        }
        return cb;
    }

    public void preModify(Map attrs, Entry entry, Map context, boolean isCreate, boolean checkImmutable) throws ServiceException
    {
       for (Iterator mit=attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry me = (Map.Entry) mit.next();
            Object value = me.getValue();
            String name = (String) me.getKey();
            AttributeInfo info = (AttributeInfo) mAttrs.get(name.toLowerCase());
            if (info != null) {
                info.checkValue(value, checkImmutable);
                if (info.getCallback() != null)
                    info.getCallback().preModify(context, name, value, attrs, entry, isCreate);
            } else {
                ZimbraLog.misc.warn("checkValue: no attribute info for: "+name);
            }
       }
    }

    public void postModify(Map attrs, Entry entry, Map context, boolean isCreate)
    {
       for (Iterator mit=attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry me = (Map.Entry) mit.next();
            Object v = me.getValue();
            String name = (String) me.getKey();
            AttributeInfo info = (AttributeInfo) mAttrs.get(name.toLowerCase());
            if (info != null) {
                if (info.getCallback() != null) {
                    try {
                        info.getCallback().postModify(context, name, entry, isCreate);
                    } catch (Exception e) {
                        // need to swallow all exceptions as postModify shouldn't throw any...
                        ZimbraLog.account.warn("postModify caught exception: "+e.getMessage(), e);
                    }
                }
            }
       }
    }

    public static void main(String args[]) throws ServiceException {
        ZimbraLog.toolSetupLog4j("info");
        AttributeManager mgr = AttributeManager.getInstance();
        HashMap attrs = new HashMap();
        attrs.put(Provisioning.A_liquidAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        attrs.put(Provisioning.A_liquidImapBindPort, "143");
        attrs.put("xxxliquidImapBindPort", "143");
        attrs.put(Provisioning.A_liquidPrefOutOfOfficeReply, null);
        attrs.put(Provisioning.A_liquidPrefOutOfOfficeReplyEnabled, "FALSE");
        Map context = new HashMap();
        mgr.preModify(attrs, null, context, false, true);
        // modify
        mgr.postModify(attrs, null, context, false);
    }
}
