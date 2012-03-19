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

package com.zimbra.common.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import com.google.common.collect.Iterators;

/**
 * Utility methods used for processing image data.
 */
public class ImageUtil {

    private static final Log log = LogFactory.getLog(ImageUtil.class);
    
    public static BufferedImage resize(BufferedImage img, int targetWidth, int targetHeight) {
        log.debug("Resizing image from %dx%d to %dx%d pixels.",
            img.getWidth(), img.getHeight(), targetWidth, targetHeight);
        return getScaledInstance(img, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
    }
    
    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in down-scaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    private static BufferedImage getScaledInstance(BufferedImage img,
                                                  int targetWidth,
                                                  int targetHeight,
                                                  Object hint,
                                                  boolean higherQuality)
    {
        // This code was taken from Chris Campbell's article, titled
        // "The Perils of Image.getScaledInstance().
        
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        int w, h;
        if (higherQuality && (img.getWidth() > targetWidth && img.getHeight() > targetHeight)) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }
        
        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }
    
    /**
     * Returns the {@code ImageReader}, or {@code null} if none is available.
     * @param contentType the MIME content type, or {@code null} 
     * @param filename the filename, or {@code null}
     */
    public static ImageReader getImageReader(String contentType, String filename) {
        log.debug("Looking up ImageReader for %s, %s", contentType, filename);
        ImageReader reader = null;
        if (!StringUtil.isNullOrEmpty(contentType)) {
            reader = Iterators.getNext(ImageIO.getImageReadersByMIMEType(contentType), null);
        }
        if (reader == null && !StringUtil.isNullOrEmpty(filename)) {
            String ext = FileUtil.getExtension(filename);
            if (!StringUtil.isNullOrEmpty(ext)) {
                reader = Iterators.getNext(ImageIO.getImageReadersBySuffix(ext), null);
            }
        }
        log.debug("Returning %s", reader);
        return reader;
    }
}
