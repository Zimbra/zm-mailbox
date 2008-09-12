package com.zimbra.cs.account.accesscontrol;

import java.io.File;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class RightManager {
    private static final String E_DEFAULT = "default";
    private static final String E_DESC   = "desc";
    private static final String E_DOC    = "doc";
    private static final String E_RIGHTS = "rights";
    private static final String E_RIGHT  = "right";
    private static final String A_NAME   = "name";
    
    private static RightManager mInstance;
    private Map<String, Right> sRights = new TreeMap<String, Right>();  // keep the map sorted so "zmmailbox lp" can display in alphabetical order 

    public static synchronized RightManager getInstance() throws ServiceException {
        if (mInstance != null) {
            return mInstance;
        }
        String dir = LC.zimbra_rights_directory.value();
        mInstance = new RightManager(dir);
        
        try {
            Right.initKnownRights(mInstance);
        } catch (ServiceException e) {
            ZimbraLog.account.error("failed to initialize known right from: " + dir, e);
            throw e;
        }
        return mInstance;
    }
    
    private RightManager(String dir) throws ServiceException {
        File fdir = new File(dir);
        if (!fdir.exists()) {
            throw ServiceException.FAILURE("rights directory does not exists: " + dir, null);
        }
        if (!fdir.isDirectory()) {
            throw ServiceException.FAILURE("rights directory is not a directory: " + dir, null);
        }
        
        File[] files = fdir.listFiles();
        for (File file : files) { 
            if (!file.getPath().endsWith(".xml")) {
                ZimbraLog.misc.warn("while loading attrs, ignoring not .xml file: " + file);
                continue;
            }
            if (!file.isFile()) {
                ZimbraLog.misc.warn("while loading attrs, ignored non-file: " + file);
            }
            try {
                loadRights(file);
            } catch (DocumentException de) {
                throw ServiceException.FAILURE("error loading rights file: " + file, de);
            }
        }
    }
    
    private void loadRights(File file) throws DocumentException, ServiceException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(file);
        Element root = doc.getRootElement();
        if (!root.getName().equals(E_RIGHTS))
            throw ServiceException.FAILURE("root tag is not " + E_RIGHTS + ", file=" + file , null);

        for (Iterator iter = root.elementIterator(); iter.hasNext();) {
            Element eRight = (Element) iter.next();
            if (!eRight.getName().equals(E_RIGHT))
                throw ServiceException.FAILURE("unknown element: " + eRight.getName() + ", file=" + file , null);

            String name = eRight.attributeValue(A_NAME);
            if (name == null)
                throw ServiceException.FAILURE("no name specified" + ", file=" + file , null);
            
            if (sRights.get(name) != null)
                throw ServiceException.FAILURE("right " + name + " is already defined, file=" + file , null);
            
            Right right = new Right(name);

            for (Iterator elemIter = eRight.elementIterator(); elemIter.hasNext();) {
                Element elem = (Element)elemIter.next();
                if (elem.getName().equals(E_DESC))
                    right.setDesc(elem.getText());
                else if (elem.getName().equals(E_DOC))
                    right.setDoc(elem.getText());
                else if (elem.getName().equals(E_DEFAULT)) {
                    String defaultValue = elem.getText();
                    if ("true".equalsIgnoreCase(defaultValue))
                        right.setDefault(Boolean.TRUE);
                    else if ("false".equalsIgnoreCase(defaultValue))
                        right.setDefault(Boolean.FALSE);
                    else
                        throw ServiceException.FAILURE("invalid default value: " + defaultValue, null);
                } else
                    throw ServiceException.FAILURE("invalid element: " + elem.getName(), null);
            }
            if (right.getDesc() == null)
                throw ServiceException.FAILURE("no desc specified for " + right.getCode() + ", file=" + file , null);
            
            sRights.put(name, right);
        }
    }
    
    public Right getRight(String right) throws ServiceException {
        Right r = sRights.get(right);
        if (r == null)
            throw ServiceException.FAILURE("invalid right " + right, null);
        return r;
    }
    
    public Map<String, Right> getAllRights() {
        return sRights;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws ServiceException {
        for (Right r : RightManager.getInstance().getAllRights().values()) {
            System.out.println(r.getCode());
            System.out.println("    desc: " + r.getDesc());
            System.out.println("    doc: " + r.getDoc());
            System.out.println();
        }
    }
    
}
