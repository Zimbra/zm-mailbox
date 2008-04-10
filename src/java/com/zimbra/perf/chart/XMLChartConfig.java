/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.perf.chart;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class XMLChartConfig {

    // elements
    public static final String E_CHARTS = "charts";
    public static final String E_CHART = "chart";
    public static final String E_PLOT = "plot";
    public final static String E_GROUP_PLOT = "groupplot";

    // <chart> attributes
    public static final String A_CHART_TITLE = "title";
    public static final String A_CHART_CATEGORY = "category";
    public static final String A_CHART_OUTFILE = "outfile";
    public static final String A_CHART_XAXIS = "xAxis";
    public static final String A_CHART_YAXIS = "yAxis";
    public static final String A_CHART_ALLOW_LOG_SCALE = "allowLogScale";
    public static final String A_CHART_PLOT_ZERO = "plotZero";
    public static final String A_CHART_WIDTH = "width";
    public static final String A_CHART_HEIGHT = "height";
    public final static String A_CHART_DOCUMENT = "outDocument";

    // <plot> attributes
    public static final String A_PLOT_LEGEND = "legend";
    public static final String A_PLOT_INFILE = "infile";
    public static final String A_PLOT_DATA_COLUMN = "data";
    public static final String A_PLOT_SHOW_RAW = "showRaw";
    public static final String A_PLOT_SHOW_MOVING_AVG = "showMovingAvg";
    public static final String A_PLOT_MOVING_AVG_POINTS = "movingAvgPoints";
    public static final String A_PLOT_MULTIPLIER = "multiplier";
    public static final String A_PLOT_DIVISOR = "divisor";
    public static final String A_PLOT_NON_NEGATIVE = "nonNegative";
    public static final String A_PLOT_PERCENT_TIME = "percentTime";
    public static final String A_PLOT_DATA_FUNCTION = "dataFunction";
    public static final String A_PLOT_AGGREGATE_FUNCTION = "aggregateFunction";
    public static final String A_PLOT_OPTIONAL = "optional";
    public static final String A_PLOT_RATIO_TOP = "ratioTop";
    public static final String A_PLOT_RATIO_BOTTOM = "ratioBottom";
    public static final String A_PLOT_GROUP_BY = "groupBy";

    private static String getAttr(Element elem, String name)
            throws DocumentException {
        String val = elem.attributeValue(name, null);
        if (val != null)
            return val;
        else
            throw new DocumentException("Missing required attribute " + name
                    + " in element " + elem.getName());
    }

    private static String getAttr(Element elem, String name, String defaultValue) {
        String val = elem.attributeValue(name, null);
        if (val != null)
            return val;
        else
            return defaultValue;
    }

    private static String getInheritedAttr(Element elem, String name,
            String defaultValue) {
        String val = null;
        for (Element e = elem; val == null && e != null; e = e.getParent()) {
            val = e.attributeValue(name, null);
        }
        return val != null ? val : defaultValue;
    }

    private static Boolean getInheritedAttrBoolean(Element elem, String name,
            boolean defaultValue) {
        String val = getInheritedAttr(elem, name, null);
        if (val != null) {
            val = val.toLowerCase();
            return val.equalsIgnoreCase("true") || val.equals("1");
        } else
            return defaultValue;
    }

    private static int getInheritedAttrInt(Element elem, String name,
            int defaultValue) throws DocumentException {
        String val = getInheritedAttr(elem, name, null);
        if (val != null) {
            try {
                int i = Integer.parseInt(val);
                return i;
            } catch (NumberFormatException ex) {
                throw new DocumentException("Invalid integer value " + val
                        + " for attribute " + name + " in element "
                        + elem.getName());
            }
        } else
            return defaultValue;
    }

    // allows 'k', 'm', 'g' suffix for kilo, mega and giga
    private static double getInheritedAttrDouble(Element elem, String name,
            double defaultValue) throws DocumentException {
        String val = getInheritedAttr(elem, name, null);
        if (val != null) {
            int len = val.length();
            if (len == 0) return defaultValue;
            char unit = val.toLowerCase().charAt(len - 1);
            int multiplier = 1;
            if (unit == 'k')
                multiplier = 1024;
            else if (unit == 'm')
                multiplier = 1024 * 1024;
            else if (unit == 'g')
                multiplier = 1024 * 1024 * 1024;
            String digits;
            if (multiplier == 1)
                digits = val;
            else
                digits = val.substring(0, len - 1);
            try {
                double d = Double.parseDouble(digits);
                return d * multiplier;
            } catch (NumberFormatException ex) {
                throw new DocumentException("Invalid double value " + val
                        + " for attribute " + name + " in element "
                        + elem.getName());
            }
        } else
            return defaultValue;
    }

    /**
     * Loads chart settings from the specified XML file.
     * 
     * @throws IOException
     *             if there was an error reading the file
     * @throws DocumentException
     *             if there was an error parsing the file
     * @throws IllegalArgumentException
     *             if any attribute has invalid value
     */
    public static List<ChartSettings> load(File xmlFile)
            throws IOException, DocumentException {
        List<ChartSettings> charts = new ArrayList<ChartSettings>();
        SAXReader reader = new SAXReader();
        Document document = reader.read(xmlFile);
        Element chartsElem = document.getRootElement();
        if (!chartsElem.getName().equals(E_CHARTS))
            throw new DocumentException("Missing <" + E_CHARTS
                    + "> root element");
        for (Iterator iter = chartsElem.elementIterator(E_CHART);
                iter.hasNext();) {
            Element chartElem = (Element) iter.next();
            String chartTitle = getAttr(chartElem, A_CHART_TITLE);
            String category = getAttr(chartElem, A_CHART_CATEGORY, "unknown");
            String outfile = getAttr(chartElem, A_CHART_OUTFILE);

            // inheritable attributes
            String xAxis = getInheritedAttr(chartElem, A_CHART_XAXIS,
                    ChartSettings.DEFAULT_CHART_XAXIS);
            String yAxis = getInheritedAttr(chartElem, A_CHART_YAXIS, "");
            boolean allowLogScale = getInheritedAttrBoolean(chartElem,
                    A_CHART_ALLOW_LOG_SCALE,
                    ChartSettings.DEFAULT_CHART_ALLOW_LOG_SCALE);
            boolean plotZero = getInheritedAttrBoolean(chartElem,
                    A_CHART_PLOT_ZERO, ChartSettings.DEFAULT_CHART_PLOT_ZERO);
            if (!allowLogScale)
                plotZero = true;
            int width = getInheritedAttrInt(chartElem, A_CHART_WIDTH,
                    ChartSettings.DEFAULT_CHART_WIDTH);
            int height = getInheritedAttrInt(chartElem, A_CHART_HEIGHT,
                    ChartSettings.DEFAULT_CHART_HEIGHT);
            String outDoc = getAttr(chartElem, A_CHART_DOCUMENT, null);

            ChartSettings chart = new ChartSettings(chartTitle, category, outfile, xAxis,
                    yAxis, allowLogScale, plotZero, width, height, outDoc);

            for (Iterator plotIter = chartElem.elementIterator(E_PLOT);
                    plotIter.hasNext();) {
                Element plotElem = (Element) plotIter.next();
                String dataCol = getAttr(plotElem, A_PLOT_DATA_COLUMN, null);

                // inheritable attributes
                String legend = getInheritedAttr(plotElem, A_PLOT_LEGEND, null);
                String infile = getInheritedAttr(plotElem, A_PLOT_INFILE, null);

                boolean showRaw = getInheritedAttrBoolean(plotElem,
                        A_PLOT_SHOW_RAW, PlotSettings.DEFAULT_PLOT_SHOW_RAW);
                boolean showMovingAvg = getInheritedAttrBoolean(plotElem,
                        A_PLOT_SHOW_MOVING_AVG,
                        PlotSettings.DEFAULT_PLOT_SHOW_MOVING_AVG);
                int movingAvgPoints = getInheritedAttrInt(plotElem,
                        A_PLOT_MOVING_AVG_POINTS,
                        PlotSettings.DEFAULT_PLOT_MOVING_AVG_POINTS);
                double multiplier = getInheritedAttrDouble(plotElem,
                        A_PLOT_MULTIPLIER, PlotSettings.DEFAULT_PLOT_MULTIPLIER);
                double divisor = getInheritedAttrDouble(plotElem,
                        A_PLOT_DIVISOR, PlotSettings.DEFAULT_PLOT_DIVISOR);

                boolean nonNegative = getInheritedAttrBoolean(plotElem,
                        A_PLOT_NON_NEGATIVE,
                        PlotSettings.DEFAULT_PLOT_NON_NEGATIVE);
                boolean percentTime = getInheritedAttrBoolean(plotElem,
                        A_PLOT_PERCENT_TIME,
                        PlotSettings.DEFAULT_PLOT_PERCENT_TIME);
                String dataFunction = getAttr(plotElem, A_PLOT_DATA_FUNCTION,
                        PlotSettings.DEFAULT_PLOT_DATA_FUNCTION);
                String aggFunction = getAttr(plotElem, A_PLOT_AGGREGATE_FUNCTION,
                        PlotSettings.DEFAULT_PLOT_AGGREGATE_FUNCTION);
                String ratioTop = getAttr(plotElem, A_PLOT_RATIO_TOP, null);
                String ratioBottom = getAttr(plotElem,
                        A_PLOT_RATIO_BOTTOM, null);

                if ((ratioTop == null && ratioBottom != null)
                        || (ratioTop != null && ratioBottom == null)) {
                    throw new DocumentException(
                            "Both ratioTop/ratioBottom need to be specified");
                }
                if ((ratioTop == null && dataCol == null)
                        || (ratioTop != null && dataCol != null)) {
                    throw new DocumentException("Specify either ratio or data");
                }
                boolean optional = getInheritedAttrBoolean(plotElem,
                        A_PLOT_OPTIONAL,
                        PlotSettings.DEFAULT_PLOT_OPTIONAL);

                PlotSettings plot = new PlotSettings(
                        legend, infile, dataCol, showRaw, showMovingAvg,
                        movingAvgPoints, multiplier, divisor,
                        nonNegative, percentTime,
                        dataFunction, aggFunction, optional,
                        ratioTop, ratioBottom);
                chart.addPlot(plot);
            }
            for (Iterator plotIter = chartElem.elementIterator(E_GROUP_PLOT);
                    plotIter.hasNext();) {
                Element plotElem = (Element) plotIter.next();
                String dataCol = getAttr(plotElem, A_PLOT_DATA_COLUMN);

                // inheritable attributes
                String infile = getInheritedAttr(plotElem, A_PLOT_INFILE, null);

                boolean showRaw = getInheritedAttrBoolean(plotElem,
                        A_PLOT_SHOW_RAW, PlotSettings.DEFAULT_PLOT_SHOW_RAW);
                boolean showMovingAvg = getInheritedAttrBoolean(plotElem,
                        A_PLOT_SHOW_MOVING_AVG,
                        PlotSettings.DEFAULT_PLOT_SHOW_MOVING_AVG);
                int movingAvgPoints = getInheritedAttrInt(plotElem,
                        A_PLOT_MOVING_AVG_POINTS,
                        PlotSettings.DEFAULT_PLOT_MOVING_AVG_POINTS);
                double multiplier = getInheritedAttrDouble(plotElem,
                        A_PLOT_MULTIPLIER, PlotSettings.DEFAULT_PLOT_MULTIPLIER);
                double divisor = getInheritedAttrDouble(plotElem,
                        A_PLOT_DIVISOR, PlotSettings.DEFAULT_PLOT_DIVISOR);

                boolean nonNegative = getInheritedAttrBoolean(plotElem,
                        A_PLOT_NON_NEGATIVE,
                        PlotSettings.DEFAULT_PLOT_NON_NEGATIVE);
                boolean percentTime = getInheritedAttrBoolean(plotElem,
                        A_PLOT_PERCENT_TIME,
                        PlotSettings.DEFAULT_PLOT_PERCENT_TIME);
                String dataFunction = getAttr(plotElem, A_PLOT_DATA_FUNCTION,
                        PlotSettings.DEFAULT_PLOT_DATA_FUNCTION);
                String aggFunction = getAttr(plotElem, A_PLOT_AGGREGATE_FUNCTION,
                        PlotSettings.DEFAULT_PLOT_AGGREGATE_FUNCTION);
                String groupBy = getAttr(plotElem, A_PLOT_GROUP_BY);

                boolean optional = getInheritedAttrBoolean(plotElem,
                        A_PLOT_OPTIONAL,
                        PlotSettings.DEFAULT_PLOT_OPTIONAL);

                GroupPlotSettings plot = new GroupPlotSettings(
                        groupBy, infile, dataCol, showRaw, showMovingAvg,
                        movingAvgPoints, multiplier, divisor,
                        nonNegative, percentTime,
                        dataFunction, aggFunction, optional);
                chart.addPlot(plot);
            }
            charts.add(chart);
        }
        return charts;
    }
}
