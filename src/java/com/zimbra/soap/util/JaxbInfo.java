/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

/**
 * Zimbra SOAP interfaces are more flexible than Jaxb in what is acceptable.
 * In particular, attributes can be represented as elements.
 * This class provides a limited amount of Jaxb information for a class to
 * aid transforming Zimbra acceptable Xml into Jaxb acceptable Xml.
 *
 * @author gren
 *
 * Treatment of annotations:
 *     XmlAccessType      : Supported
 *     XmlAccessorType    : Supported
 *     XmlAnyAttribute    : Ignored
 *     XmlAnyElement      : Ignored
 *     XmlAttribute       : Supported
 *     XmlElement         : Supported
 *     XmlElementRef      : Supported
 *     XmlElementRefs     : Supported
 *     XmlElementWrapper  : Supported
 *     XmlElements        : Supported
 *     XmlEnum            : Ignored
 *     XmlEnumValue       : Ignored
 *     XmlMixed           : Ignored
 *     XmlRootElement     : Supported
 *     XmlSeeAlso         : Not supported - useful for finding subclasses
 *     XmlTransient       : Supported
 *     XmlType            : Useful for propOrder
 *     XmlValue           : Ignored
 */

public class JaxbInfo {

    private static final Log LOG = ZimbraLog.soap;
    // Various annotation classes use this
    public final static String DEFAULT_MARKER = "##default";

    private Class<?> jaxbClass = null;
    private Class<?> superClass = null;
    private String stamp = null;
    private String rootElementName = null;
    private XmlAccessType accessType = null;
    private XmlType xmlType = null;
    
    /**
     * names of known attributes that can be associated with the element
     */
    private List<String> attributeNames = Lists.newArrayList();

    public static HashMap<String,JaxbInfo> cache = Maps.newHashMap();

    private HashMap<String,JaxbAttributeInfo> attrInfo = Maps.newHashMap();
    private HashMap<String,JaxbElementInfo> elemInfo = Maps.newHashMap();
    private HashMap<String,WrappedElementInfo> wrappedElemInfo = Maps.newHashMap();
    private JaxbValueInfo elementValue = null;

    /**
     * @param klass is a JAXB annotated class associated with a particular element
     */
    private JaxbInfo(Class<?> klass) {
        this.jaxbClass = klass;
        if (klass == null) {
            LOG.error("null class provided to JaxbInfo constructor");
            return;
        }
        stamp = "JaxbInfo class=" + this.jaxbClass.getName() + ":";
        gatherInfo();
        synchronized(cache) {
            cache.put(klass.getName(), this);
        }
    }

    public static JaxbInfo getFromCache(Class<?> klass) {
        if (klass == null || klass.isPrimitive())
            return null;
        JaxbInfo jbi = null;
        synchronized(cache) {
            jbi = cache.get(klass.getName());
        }
        if (jbi == null)
            jbi = new JaxbInfo(klass);
        return jbi;
    }

    public static void clearCache() {
        synchronized(cache) {
            cache.clear();
        }
    }

    private JaxbInfo getSuperClassInfo() {
        superClass = jaxbClass.getSuperclass();
        if (superClass == null) {
            return null;
        }
        JaxbInfo encJaxbInfo = JaxbInfo.getFromCache(superClass);
        if (encJaxbInfo == null) {
            encJaxbInfo = new JaxbInfo(superClass);
        }
        return encJaxbInfo;
    }

    public Iterable<String> getAttributeNames() {
        List<String> allNames = Lists.newArrayList();
        Iterables.addAll(allNames, this.attributeNames);
        JaxbInfo encClassInfo = getSuperClassInfo();
        if (encClassInfo != null)
            Iterables.addAll(allNames, encClassInfo.getAttributeNames());
        return allNames;
    }

    public Iterable<JaxbAttributeInfo> getAttributes() {
        List<JaxbAttributeInfo> attrs = Lists.newArrayList();
        Iterables.addAll(attrs, this.attrInfo.values());
        JaxbInfo encClassInfo = getSuperClassInfo();
        if (encClassInfo != null)
            Iterables.addAll(attrs, encClassInfo.getAttributes());
        return attrs;
    }

    public Iterable<String> getElementNames() {
        List<String> elemNames = Lists.newArrayList();
        Iterables.addAll(elemNames, this.elemInfo.keySet());
        Iterables.addAll(elemNames, this.wrappedElemInfo.keySet());
        JaxbInfo encClassInfo = getSuperClassInfo();
        if (encClassInfo != null)
            Iterables.addAll(elemNames, encClassInfo.getElementNames());
        return elemNames;
    }

    public Iterable<JaxbNodeInfo> getElements() {
        List<JaxbNodeInfo> elems = Lists.newArrayList();
        Iterables.addAll(elems, this.elemInfo.values());
        Iterables.addAll(elems, this.wrappedElemInfo.values());
        JaxbInfo encClassInfo = getSuperClassInfo();
        if (encClassInfo != null)
            Iterables.addAll(elems, encClassInfo.getElements());
        return elems;
    }

    public boolean hasElement(String name) {
        if (this.elemInfo.containsKey(name))
            return true;
        if (this.wrappedElemInfo.containsKey(name))
            return true;
        JaxbInfo encClassInfo = getSuperClassInfo();
        if (encClassInfo != null)
            return encClassInfo.hasElement(name);
        return false;
    }

    public boolean hasWrappedElement(String name) {
        if (this.wrappedElemInfo.containsKey(name))
            return true;
        JaxbInfo encClassInfo = getSuperClassInfo();
        if (encClassInfo != null)
            return encClassInfo.hasWrappedElement(name);
        return false;
    }

    public Iterable<String> getWrappedSubElementNames(String wrapperName) {
        WrappedElementInfo info = this.wrappedElemInfo.get(wrapperName);
        if (info != null)
            return info.getElementNames();
        JaxbInfo encClassInfo = getSuperClassInfo();
        if (encClassInfo != null)
            return encClassInfo.getWrappedSubElementNames(wrapperName);
        return null;
    }

    public Class<?> getClassForWrappedElement(String wrapperName,
            String elementName) {
        WrappedElementInfo info = this.wrappedElemInfo.get(wrapperName);
        if (info == null) {
            JaxbInfo encClassInfo = getSuperClassInfo();
            if (encClassInfo != null) {
                return encClassInfo.getClassForWrappedElement(wrapperName, elementName);
            }
            LOG.debug(stamp + "Unknown wrapped element wrapperName=" + wrapperName + " element=" + elementName);
            return null;
        }
        Class<?> wKlass = info.getClassForElementName(elementName);
        if (wKlass == null) {
            LOG.debug(stamp + "No class for wrapperName=" + wrapperName + " element=" + elementName);
            return null;
        }
        return wKlass;
    }

    public boolean hasAttribute(String name) {
        return Iterables.contains(this.getAttributeNames(), name);
    }

    public List<String> getPropOrder() {
        List<String> propOrder = Lists.newArrayList();
        if ( (null == xmlType) || (null == xmlType.propOrder()) )
            return propOrder;
        for (String prop : xmlType.propOrder())
            propOrder.add(prop);
        return propOrder;
    }

    public static String getRootElementName(Class<?> kls) {
        String name;
        if (kls == null)
            return null;
        XmlRootElement re = kls.getAnnotation(XmlRootElement.class);
        if (re != null) {
            name = re.name();
        } else {
            name = kls.getName();
            int lastDot =  name.lastIndexOf('.');
            if (lastDot >= 0)
                name = name.substring(lastDot + 1);
        }
        return name;
    }

    public String getRootElementName() {
        if (rootElementName == null) {
            rootElementName = getRootElementName(jaxbClass);
        }
        return rootElementName;
    }

    public Class<?> getClassForElement(String name) {
        JaxbElementInfo info = elemInfo.get(name);
        if (info == null) {
            JaxbInfo encClassInfo = getSuperClassInfo();
            if (encClassInfo != null) {
                return encClassInfo.getClassForElement(name);
            }
            return null;
        }
        return info.getAtomClass();
    }

    public Class<?> getClassForAttribute(String name) {
        JaxbAttributeInfo info = attrInfo.get(name);
        if (info == null) {
            JaxbInfo encClassInfo = getSuperClassInfo();
            if (encClassInfo != null) {
                return encClassInfo.getClassForAttribute(name);
            }
        }
        return info.getAtomClass();
    }

    private void setXmlAttributeInfo(XmlAttribute attr, String fieldName, Type defaultGenericType) {
        JaxbAttributeInfo info = new JaxbAttributeInfo(this, attr, fieldName, defaultGenericType);
        String name = info.getName();
        Class<?> atomClass = info.getAtomClass();
        if (atomClass != null && !Strings.isNullOrEmpty(name)) {
            attrInfo.put(name, info);
        }
    }

    private void setXmlElementInfo(XmlElement elem, String fieldName, Type defaultGenericType) {
        JaxbElementInfo info = new JaxbElementInfo(elem, fieldName, defaultGenericType);
        String name = info.getName();
        Class<?> atomClass = info.getAtomClass();
        if (atomClass != null && !Strings.isNullOrEmpty(name)) {
            elemInfo.put(name, info);
        }
    }

    private void setXmlElementInfo(XmlElementRef elemRef, String fieldName, Type defaultGenericType) {
        JaxbElementInfo info = new JaxbElementInfo(elemRef, fieldName, defaultGenericType);
        String name = info.getName();
        Class<?> atomClass = info.getAtomClass();
        if (atomClass != null && !Strings.isNullOrEmpty(name)) {
            elemInfo.put(name, info);
        }
    }

    private void processFieldRelatedAnnotations(Annotation annots[], String fieldName, Type defaultGenericType) {
        WrappedElementInfo wrappedInfo = null;
        for (Annotation annot : annots) {
            if (annot instanceof XmlElementWrapper) {
                XmlElementWrapper wrapper = (XmlElementWrapper) annot;
                wrappedInfo = new WrappedElementInfo(wrapper, fieldName);
                this.wrappedElemInfo.put(wrapper.name(), wrappedInfo);
                break;
            }
        }
        for (Annotation annot : annots) {
            if (annot instanceof XmlValue) {
                elementValue = new JaxbValueInfo((XmlValue)annot, fieldName, defaultGenericType);
            } else if (annot instanceof XmlAttribute) {
                XmlAttribute attr = (XmlAttribute) annot;
                String attrName = attr.name();
                if ((attrName == null) || DEFAULT_MARKER.equals(attrName)) {
                    attrName = fieldName;
                }
                this.setXmlAttributeInfo(attr, fieldName, defaultGenericType);
                this.attributeNames.add(attrName);
            } else if (annot instanceof XmlElement) {
                XmlElement xmlElem = (XmlElement) annot;
                if (wrappedInfo == null) {
                    setXmlElementInfo(xmlElem, fieldName, defaultGenericType);
                } else {
                    wrappedInfo.add(xmlElem, fieldName, classFromType(defaultGenericType));
                }
            } else if (annot instanceof XmlElementRef) {
                XmlElementRef xmlElemR = (XmlElementRef) annot;
                if (wrappedInfo == null) {
                    setXmlElementInfo(xmlElemR, null, null);
                } else {
                    wrappedInfo.add(xmlElemR, null, null);
                }
            } else if (annot instanceof XmlElements) {
                XmlElements xmlElemsAnnot = (XmlElements) annot;
                for (XmlElement xmlE : xmlElemsAnnot.value()) {
                    if (wrappedInfo == null) {
                        setXmlElementInfo(xmlE, null, null);
                    } else {
                        wrappedInfo.add(xmlE, null, null);
                    }
                }
            } else if (annot instanceof XmlElementRefs) {
                XmlElementRefs elemRefs = (XmlElementRefs) annot;
                for (XmlElementRef xmlE : elemRefs.value()) {
                    if (wrappedInfo == null) {
                        setXmlElementInfo(xmlE, null, null);
                    } else {
                        wrappedInfo.add(xmlE, null, null);
                    }
                }
            }
        }
    }

    public static boolean isGetter(Method method) {
        String methodName = method.getName();
        if (!methodName.startsWith("get")) {
            if (!methodName.startsWith("is"))
                return false;
        }
        if (method.getParameterTypes().length != 0)
            return false;  
        return (!void.class.equals(method.getReturnType()));
    }

    public static boolean isSetter(Method method) {
        if (!method.getName().startsWith("set"))
            return false;
        return (method.getParameterTypes().length == 1);
    }

    public static boolean isGetterOrSetter(Method method) {
        return isGetter(method) || isSetter(method);
    }

    private String guessFieldNameFromGetterOrSetter(String methodName) {
        String fieldName = null;
        if ((methodName.startsWith("set")) || (methodName.startsWith("get"))) {
            fieldName = methodName.substring(3,4).toLowerCase() + methodName.substring(4);
        } else if (methodName.startsWith("is")) {
            fieldName = methodName.substring(2,3).toLowerCase() + methodName.substring(3);
        }
        return fieldName;
    }

    /**
     * Returns the most elemental class associated with {@link genericType}
     * May return null
     */
    public static Class<?> classFromType(Type genericType) {
        Class<?> defKlass;
        if (genericType == null)
            return null;
        if (genericType instanceof Class<?>) {
            defKlass = (Class<?>) genericType;
        } else if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type typeArgs[] = pt.getActualTypeArguments();
            if (typeArgs.length != 1) {
                // Odd - better to ignore this
                return null;
            }
            return classFromType(typeArgs[0]);
        } else if (genericType instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) genericType;
            defKlass = gat.getGenericComponentType().getClass();
        } else if (genericType instanceof TypeVariable<?>) {
            TypeVariable<?> tv = (TypeVariable<?>) genericType;
            defKlass = tv.getClass();
        } else {
            LOG.debug("classFromType unknown instance type [" + genericType.toString() + "] - ignoring");
            defKlass = null;
        }
        return defKlass;
    }

    /**
     * Returns the most elemental class associated with {@link genericType}
     * May return null
     */
    public static boolean representsArray(Type genericType) {
        Class<?> defKlass = null;
        if (genericType == null)
            return false;
        if (genericType instanceof Class<?>) {
            defKlass = (Class<?>) genericType;
            return Collection.class.isAssignableFrom(defKlass);
        } else if (genericType instanceof ParameterizedType) {
            // e.g. java.util.List<com.zimbra.soap.type.AttributeName>
            ParameterizedType pt = (ParameterizedType) genericType;
            Type rawType = pt.getRawType();
            if (rawType instanceof Class<?>) {
                return Collection.class.isAssignableFrom((Class<?>) rawType);
            }
            return false;
        } else if (genericType instanceof GenericArrayType) {
            return true;
        } else if (genericType instanceof TypeVariable<?>) {
            TypeVariable<?> tv = (TypeVariable<?>) genericType;
            return Collection.class.isAssignableFrom(tv.getClass());
        } else {
            return false;
        }
    }

    private void gatherInfo() {
        XmlAccessorType accessorType;
        rootElementName = null;
        accessType = null;
        try {
            XmlRootElement rootE = (XmlRootElement) jaxbClass.getAnnotation(XmlRootElement.class);
            if (rootE != null)
                rootElementName = rootE.name();
            xmlType = (XmlType) jaxbClass.getAnnotation(XmlType.class);

            accessorType = (XmlAccessorType) jaxbClass.getAnnotation(XmlAccessorType.class);
            if (accessorType == null) {
                Package pkg = jaxbClass.getPackage();
                accessorType = (XmlAccessorType) pkg.getAnnotation(XmlAccessorType.class);
            }
            if (accessorType != null) {
                accessType = accessorType.value();
            }
            if (accessType == null) {
                // Default value for JAXB
                accessType = XmlAccessType.PUBLIC_MEMBER;
            }

            Field fields[] = jaxbClass.getDeclaredFields();
            for (Field field: fields) {
                XmlTransient xmlTransient = (XmlTransient) field.getAnnotation(XmlTransient.class);
                if (xmlTransient != null) {
                    continue;
                }
                Annotation fAnnots[] = field.getAnnotations();
                if ((fAnnots == null) || (fAnnots.length == 0)) {
                    boolean autoFields =
                        (accessType.equals(XmlAccessType.PUBLIC_MEMBER) || accessType.equals(XmlAccessType.FIELD));
                    if (!autoFields) {
                        continue;
                    }
                }
                processFieldRelatedAnnotations(fAnnots, field.getName(), field.getGenericType());
            }

            Method methods[] = jaxbClass.getDeclaredMethods();
            for (Method method : methods) {
                XmlTransient xmlTransient = (XmlTransient) method.getAnnotation(XmlTransient.class);
                if (xmlTransient != null) {
                    continue;
                }
                if (!isGetterOrSetter(method))
                    continue;
                Annotation mAnnots[] = method.getAnnotations();
                if ((mAnnots == null) || (mAnnots.length == 0)) {
                    boolean autoGettersSetters =
                        (accessType.equals(XmlAccessType.PUBLIC_MEMBER) || accessType.equals(XmlAccessType.PROPERTY));
                    if (!autoGettersSetters) {
                        continue;
                    }
                }
                processFieldRelatedAnnotations(mAnnots,
                        guessFieldNameFromGetterOrSetter(method.getName()),
                        method.getGenericReturnType());
            }
        } catch (Throwable e) {
            LOG.error(stamp + "Problem introspecting class", e);
        }
    }

    protected String getStamp() { return stamp; }
    public Class<?> getJaxbClass() { return jaxbClass; }
    public JaxbValueInfo getElementValue() { return elementValue; }
}
