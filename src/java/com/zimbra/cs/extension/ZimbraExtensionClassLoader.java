package com.zimbra.cs.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.zimbra.cs.util.ZimbraLog;

/**
 * Loads an extension.
 */
public class ZimbraExtensionClassLoader extends URLClassLoader {
	
	public static final String ZIMBRA_EXTENSION_CLASS = "Zimbra-Extension-Class";
	
	private List mExtensionClassNames = new ArrayList();

	/**
	 * Load classes from all jar files or directories in the directory
	 * 'dir'.
	 */
	public ZimbraExtensionClassLoader(File dir, ClassLoader parent) {
		this(new File[] { dir }, parent);
	}

	/**
	 * Private for now - we can use this variant if we need to throw all
	 * extensions (or some of them) into the same classloader instance.
	 */
	private ZimbraExtensionClassLoader(File[] dirs, ClassLoader parent) {
		super(filesToURLs(dirs), parent);
		findExtensions();
	}

	private void findExtensions() {
		URL[] urls = getURLs();
		for (int i = 0; i < urls.length; i++) {
			File entry = new File(urls[i].getFile());
			String className = getExtensionClassInManifest(entry);
			if (className != null) {
				mExtensionClassNames.add(className);
			}
		}
	}

	public boolean hasExtensions() {
		return mExtensionClassNames.size() > 0;
	}
	
	public List getExtensionClassNames() {
		return mExtensionClassNames;
	}
	
	/*
	 * URLClassLoader does not provide access to manifest of each
	 * jar files.  I don't want to do this with resources,
	 * so we have to walk all the jar files or directories and
	 * read manifest ourselves.
	 */
	private String getExtensionClassInManifest(File file) {
		Manifest man = null;
		if (file.isDirectory()) {
			File manifestFile = new File(file, JarFile.MANIFEST_NAME);
			if (manifestFile.exists()) {
				// don't need BufferedInputStream because Manifest uses it's
				// own buffering stream around the InputStream we provide.
				try {
					man = new Manifest(new FileInputStream(manifestFile));
				} catch (IOException ioe) {
					if (ZimbraLog.extensions.isDebugEnabled()) {
						ZimbraLog.extensions.debug("exception looking for manifest in directory: " + file, ioe);
					}
				}
				if (man == null) {
					if (ZimbraLog.extensions.isDebugEnabled()) {
						ZimbraLog.extensions.debug("no manifest for directory: " + file);
					}
					return null;
				}
			}
		} else if (file.isFile()) {
			JarFile jarFile;
			try {
				jarFile = new JarFile(file);
				man = jarFile.getManifest();
			} catch (IOException ioe) {
				if (ZimbraLog.extensions.isDebugEnabled()) {
					ZimbraLog.extensions.debug("exception looking for manifest in jar file: " + file, ioe);
				}
			}
			if (man == null) {
				if (ZimbraLog.extensions.isDebugEnabled()) {
					ZimbraLog.extensions.debug("no manifest for jar file: " + file);
				}
				return null;
			}
		} else {
			ZimbraLog.extensions.warn("entry in extension load path is not file or directory: " + file);
			return null;
		}

		Attributes attrs = man.getMainAttributes();
		if (ZimbraLog.extensions.isDebugEnabled()) {
			for (Iterator iter = attrs.keySet().iterator(); iter.hasNext();) {
				Attributes.Name name = (Attributes.Name)iter.next();
				ZimbraLog.extensions.debug("Manifest attribute=" + name + " value=" + attrs.getValue(name));
			}
		}
		String classname = (String)attrs.getValue(ZIMBRA_EXTENSION_CLASS);
		if (classname == null) {
			if (ZimbraLog.extensions.isDebugEnabled()) {
				ZimbraLog.extensions.debug("no extension class found in manifest of: " + file);
			}
		} else {
			ZimbraLog.extensions.info("extension " + classname + " found in " + file);
		}
		return classname;
	}

	private static URL[] filesToURLs(File[] dirs) {
		List urls = new ArrayList();
		for (int i = 0; i < dirs.length; i++) {
			File[] files = dirs[i].listFiles();
			if (files == null) {
				continue;
			}
			for (int j = 0; j < files.length; j++) {
				try {
					URL url = files[i].toURL();
					urls.add(url);
					if (ZimbraLog.extensions.isDebugEnabled()) {
						ZimbraLog.extensions.debug("adding url: " + url);
					}
				} catch (MalformedURLException mue) {
					ZimbraLog.extensions.warn("exception creating url for " + files[i], mue);
				}
			}
		}
		return (URL[])urls.toArray(new URL[0]);
	}
	
}
