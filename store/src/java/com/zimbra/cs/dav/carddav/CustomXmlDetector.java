/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021, 2022, 2023,2024 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.carddav;

import com.zimbra.common.mime.MimeConstants;
import org.apache.commons.fileupload.FileItem;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.InputStream;
import java.io.IOException;

/**
 * Custom detector for XML content types .
 * Extends Apache Tika's Detector interface to provide specialized XML detection.
 */
public class CustomXmlDetector implements Detector {

    private final Detector defaultDetector;
    private final FileItem file;
    private String cType;

    /**
     * Constructs a new CustomXmlDetector with a default detector and file .
     *
     * @param defaultDetector The fallback detector to use if content is not XML
     * @param attachment The FileItem attachment to analyze
     */
    public CustomXmlDetector(Detector defaultDetector, FileItem attachment) {
        this.defaultDetector = defaultDetector;
        this.file = attachment;
    }

    /**
     * Detects the media type of the input stream, specifically handling XML content.
     *
     * @param input The input stream to analyze
     * @param metadata Additional metadata about the content
     * @return MediaType.APPLICATION_XML if content type is "text/xml",
     *         otherwise delegates to default detector
     * @throws IOException If an error occurs while reading the input stream
     */
    @Override
    public MediaType detect(InputStream input, Metadata metadata) throws IOException {
        cType = file.getContentType();
        if (cType != null && (cType.equalsIgnoreCase(MimeConstants.CT_TEXT_XML) || cType.equalsIgnoreCase(MimeConstants.CT_TEXT_XML_LEGACY))) {
            return MediaType.APPLICATION_XML;
        }
        return defaultDetector.detect(input, metadata);
    }
}