package com.zimbra.cs.store.helper;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.util.Zimbra;

public class ClassHelper {

    public static boolean isClassExist(String classPath) {
        if (!StringUtil.isNullOrEmpty(classPath)) {
            try {
                try {
                    Class.forName(classPath);
                } catch (ClassNotFoundException e) {
                    ExtensionUtil.findClass(classPath);
                }
                return true;
            } catch (Throwable t) {
                Zimbra.halt("unable to initialize blob store", t);
            }
        }
        return false;
    }

    public static Object getZimbraClassInstanceBy(String className) throws Throwable {
        try {
            return Class.forName(className).newInstance();
        } catch (Exception e) {
            return ExtensionUtil.findClass(className).newInstance();
        }
    }
}
