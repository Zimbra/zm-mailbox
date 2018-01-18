/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016, 2017 Synacor, Inc.
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

package com.zimbra.cs.util.tnef.mapi;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.tnef.IcalUtil;

import net.fortuna.ical4j.data.ContentHandler;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.UtcOffset;
import net.fortuna.ical4j.model.component.Daylight;
import net.fortuna.ical4j.model.component.Standard;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.TzId;
import net.fortuna.ical4j.model.property.TzOffsetFrom;
import net.fortuna.ical4j.model.property.TzOffsetTo;
import net.fortuna.ical4j.util.TimeZones;
import net.fortuna.ical4j.validate.ValidationException;
import net.freeutils.tnef.RawInputStream;

/**
 * From MS-OXCICAL this is used for
 *     PidLidAppointmentTimeZoneDefinitionRecur
 *         Specifies time zone information that describes how to convert the
 *         meeting date and time on a recurring series to and from UTC.
 *         If this property is set, but it has data that is inconsistent with
 *         the data that is represented by PidLidTimeZoneStruct, then use
 *         PidLidTimeZoneStruct instead of this property.
 *
 *     PidLidAppointmentTimeZoneDefinitionStartDisplay
 *         Specifies time zone information that indicates the time zone of
 *         the PidLidAppointmentStartWhole property. The value of this
 *         property is used to convert the start date and time from UTC to
 *         this time zone for display purposes. The fields in this BLOB are
 *         encoded exactly as specified for
 *         PidLidAppointmentTimeZoneDefinitionRecur with one exception.
 *         For each TZRule specified by this property, the R flag in the
 *         TZRule flags field is not set (for example, if the TZRule is the
 *         effective rule, the value of the field TZRule flags MUST be
 *         0x0002; otherwise, it MUST be 0x0000).
 *     PidLidAppointmentTimeZoneDefinitionEndDisplay
 *         Specifies time zone information that indicates the time zone of
 *         the PidLidAppointmentEndWhole property. The format, constraints,
 *         and computation of this property are the same as specified in the
 *         PidLidAppointmentTimeZoneDefinitionStartDisplay property.
 *
 * MS-OXOCAL Timezone structure definition
 *           byte    MajorVersion;         // 0x02
 *           byte    MinorVersion;         // 0x01
 *           int16_t cbHeader;             // ByteCount for Reserved .. cRules
 *           int16_t Reserved;             // 0x0002
 *           int16_t cchKeyName;           // count of Unicode chars in KeyName
 *           int16_t KeyName[cchKeyName];  // Timezone name
 *           int16_t cRules;               // count of TZRules 1 <= cRules <= 1024
 *           TZRules[cRules];              //
 */

public class TimeZoneDefinition {

    static Log sLog = ZimbraLog.tnef;
    static final TimeZoneRegistry tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();

    private String timezoneName; // KeyName
    private TZRule effectiveRule;
    private TimeZone theZone;
    private MapiPropertyId mpi;

    /**
     * Initialise TimeZoneDefinition object from one of the MAPI TimeZoneDefinition
     * properties (as opposed to the simpler TimeZoneStruct property)
     *
     * @param mpi is one of the known ones associated with TimeZoneDefinition - see above
     * @param ris
     * @throws IOException
     */
    public TimeZoneDefinition(MapiPropertyId mpi, RawInputStream ris)
                            throws IOException {
        theZone = null;
        this.mpi = mpi;
        int MajorVersion = ris.readU8();
        int MinorVersion = ris.readU8();
        int cbHeader = ris.readU16();
        int ReservedInt = ris.readU16();
        int cchKeyName = ris.readU16();
        setTimezoneName(ris.readStringUnicode(cchKeyName * 2));
        int cRules = ris.readU16();
        if (sLog.isDebugEnabled()) {
            StringBuffer debugInfo = new StringBuffer();
            debugInfo.append("TimeZoneName=");
            debugInfo.append(getTimezoneName());
            if (mpi.equals(MapiPropertyId.PidLidAppointmentTimeZoneDefinitionStartDisplay)) {
                debugInfo.append("(PidLidAppointmentTimeZoneDefinitionStartDisplay)");
            } else if (mpi.equals(MapiPropertyId.PidLidAppointmentTimeZoneDefinitionEndDisplay)) {
                debugInfo.append("(PidLidAppointmentTimeZoneDefinitionEndDisplay)");
            } else if (mpi.equals(MapiPropertyId.PidLidAppointmentTimeZoneDefinitionRecur)) {
                debugInfo.append("(PidLidAppointmentTimeZoneDefinitionRecur)");
            }
            debugInfo.append(":\n");
            if (MajorVersion != 0x2) {
                debugInfo.append("    Unexpected MajorVersion=");
                debugInfo.append(MajorVersion);
                debugInfo.append("\n");
            }
            if (MinorVersion != 0x1) {
                debugInfo.append("    Unexpected MinorVersion=");
                debugInfo.append(MinorVersion);
                debugInfo.append("\n");
            }
            if (ReservedInt != 0x2) {
                debugInfo.append("    Unexpected Reserved=0x");
                debugInfo.append(Integer.toHexString(ReservedInt));
                debugInfo.append("\n");
            }
            debugInfo.append("    cbHeader=");
            debugInfo.append(cbHeader);
            debugInfo.append(" cchKeyName=");
            debugInfo.append(cchKeyName);
            debugInfo.append(" cRules=");
            debugInfo.append(cRules);
            sLog.debug(debugInfo);
        }
        for (int cnt = 0;cnt < cRules; cnt++) {
            TZRule currRule = new TZRule(ris);
            if (currRule == null) {
                continue;
            }
            if (currRule.isEffectiveRule()) {
                // We only care about the Effective Rule to keep IOP simple.
                setEffectiveRule(currRule);
                break;
            }
        }
    }

    /**
     * Initialise TimeZoneDefinition object from MAPI
     * PidLidTimeZoneStruct property.
     * This property is set on a recurring series to specify time zone
     * information. This property specifies how to convert time fields between
     * local time and UTC.
     *
     * @param ris
     * @param tzName
     * @throws IOException
     */
    public TimeZoneDefinition(RawInputStream ris, String tzName) throws IOException {
        theZone = null;
        setTimezoneName(tzName);
        if (sLog.isDebugEnabled()) {
            StringBuffer debugInfo = new StringBuffer();
            debugInfo.append("TimeZoneName=");
            debugInfo.append(getTimezoneName());
            debugInfo.append(" (from PidLidDescription):\n");
            sLog.debug(debugInfo);
        }
        TZRule currRule = new TZRule();
        currRule.setBias(IcalUtil.readI32(ris));
        currRule.setStandardBias(IcalUtil.readI32(ris));
        currRule.setDaylightBias(IcalUtil.readI32(ris));
        ris.readU16();  // wStandardYear
        currRule.setStandardDate(new SYSTEMTIME(ris));
        ris.readU16();  // wDaylightYear
        currRule.setDaylightDate(new SYSTEMTIME(ris));
        setEffectiveRule(currRule);
    }

    /**
     * Initialize TimeZoneDefinition object from input parameters
     * @param bias UTC offset in minutes
     * @param standardBias offset in minutes from bias during standard time.; has a value of 0 in most cases
     * @param daylightBias offset in minutes from bias during daylight saving time.
     * @param standardDate the date when the time zone will transition to standard time
     * @param daylightDate the time zone will transition to daylight time
     * @throws IOException
     */
    public TimeZoneDefinition(String tzName, int bias, int standardBias, int daylightBias, SYSTEMTIME standardDate, SYSTEMTIME daylightDate) throws IOException {
        theZone = null;
        setTimezoneName(tzName);

        TZRule currRule = new TZRule();
        currRule.setBias(bias);
        currRule.setStandardBias(standardBias);
        currRule.setDaylightBias(daylightBias);
        currRule.setStandardDate(standardDate);
        currRule.setDaylightDate(daylightDate);
        setEffectiveRule(currRule);
    }

    /**
     * @param timezoneName the timezoneName to set
     */
    private void setTimezoneName(String timezoneName) {
        this.timezoneName = timezoneName;
    }

    /**
     * @return the timezoneName
     */
    public String getTimezoneName() {
        return timezoneName;
    }

    /**
     * @param effectiveRule the effectiveRule to set
     */
    private void setEffectiveRule(TZRule effectiveRule) {
        this.effectiveRule = effectiveRule;
    }

    /**
     * @return the effectiveRule
     */
    public TZRule getEffectiveRule() {
        return effectiveRule;
    }

    /**
     *
     * @return the appropriate iCal4j TimeZone for this TimeZoneDefinition
     */
    public TimeZone getTimeZone() {
        if (theZone != null) {
            return theZone;
        }
        if (effectiveRule == null) {
            return tzRegistry.getTimeZone(TimeZones.UTC_ID);

        }

        if (! effectiveRule.hasDaylightSaving()) {
            // TODO - we may be better off creating a new timezone here to make sure offset is correct
            // theZone = new TimeZone(effectiveRule.getStandardUtcOffsetMillis(), getTimezoneName());
            theZone = tzRegistry.getTimeZone(getTimezoneName());
            return theZone;
        }

        UtcOffset stdOffset = effectiveRule.getStandardUtcOffset();
        UtcOffset dlOffset = effectiveRule.getDaylightUtcOffset();
        PropertyList vtzProps = new PropertyList();
        TzId myTzid = new TzId(getTimezoneName());
        vtzProps.add(myTzid);
        VTimeZone vtz = new VTimeZone(vtzProps);
        Standard stdComp = new Standard();
        stdComp.getProperties().add(effectiveRule.getStandardDtStart());
        stdComp.getProperties().add(effectiveRule.icalStandardRRule());
        TzOffsetFrom offsetFrom = new TzOffsetFrom(dlOffset);
        TzOffsetTo offsetTo = new TzOffsetTo(stdOffset);
        stdComp.getProperties().add(offsetFrom);
        stdComp.getProperties().add(offsetTo);
        Daylight dayComp = new Daylight();
        dayComp.getProperties().add(effectiveRule.getDaylightDtStart());
        dayComp.getProperties().add(effectiveRule.icalDaylightRRule());
        offsetFrom = new TzOffsetFrom(stdOffset);
        offsetTo = new TzOffsetTo(dlOffset);
        dayComp.getProperties().add(offsetFrom);
        dayComp.getProperties().add(offsetTo);
        vtz.getObservances().add(stdComp);
        vtz.getObservances().add(dayComp);
        try {
            vtz.validate(true);
            theZone = new TimeZone(vtz);
        } catch (ValidationException e) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Problem with property %s - will default to UTC" + this.mpi.toString(), e);
            }
            theZone = tzRegistry.getTimeZone(TimeZones.UTC_ID);
        }
        theZone = new TimeZone(vtz);
        return theZone;
    }

    public void addVtimezone(ContentHandler icalOutput) throws ParserException, URISyntaxException, IOException, ParseException {
        if (getTimezoneName() == null) {
            return;
        }
        if (effectiveRule == null) {
            return;
        }
        getTimeZone();
        if (theZone == null) {
            return;
        }
        VTimeZone vtz = theZone.getVTimeZone();
        icalOutput.startComponent(Component.VTIMEZONE);
        for (Object obj : vtz.getProperties()) {
            if (obj instanceof Property) {
                Property currProp = (Property) obj;
                IcalUtil.addProperty(icalOutput, currProp);
            }
        }
        for (Object obj : vtz.getObservances()) {
            if (obj instanceof Component) {
                Component currComp = (Component) obj;
                icalOutput.startComponent(currComp.getName());
                for (Object propObj : currComp.getProperties()) {
                    if (propObj instanceof Property) {
                        Property obsProp = (Property) propObj;
                        IcalUtil.addProperty(icalOutput, obsProp);
                    }
                }
                icalOutput.endComponent(currComp.getName());
            }
        }
        icalOutput.endComponent(Component.VTIMEZONE);
        if (true) {
            return;
        }
    }
}
