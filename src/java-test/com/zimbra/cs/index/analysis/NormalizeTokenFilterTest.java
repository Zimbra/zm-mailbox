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
package com.zimbra.cs.index.analysis;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit test for {@link NormalizeTokenFilter}.
 *
 * @author ysasaki
 */
public class NormalizeTokenFilterTest {

    @Test
    public void alphabet() {
        Assert.assertEquals('a', NormalizeTokenFilter.normalize('\uFF21'));
        Assert.assertEquals('b', NormalizeTokenFilter.normalize('\uFF22'));
        Assert.assertEquals('c', NormalizeTokenFilter.normalize('\uFF23'));
        Assert.assertEquals('d', NormalizeTokenFilter.normalize('\uFF24'));
        Assert.assertEquals('e', NormalizeTokenFilter.normalize('\uFF25'));
        Assert.assertEquals('f', NormalizeTokenFilter.normalize('\uFF26'));
        Assert.assertEquals('g', NormalizeTokenFilter.normalize('\uFF27'));
        Assert.assertEquals('h', NormalizeTokenFilter.normalize('\uFF28'));
        Assert.assertEquals('i', NormalizeTokenFilter.normalize('\uFF29'));
        Assert.assertEquals('j', NormalizeTokenFilter.normalize('\uFF2A'));
        Assert.assertEquals('k', NormalizeTokenFilter.normalize('\uFF2B'));
        Assert.assertEquals('l', NormalizeTokenFilter.normalize('\uFF2C'));
        Assert.assertEquals('m', NormalizeTokenFilter.normalize('\uFF2D'));
        Assert.assertEquals('n', NormalizeTokenFilter.normalize('\uFF2E'));
        Assert.assertEquals('o', NormalizeTokenFilter.normalize('\uFF2F'));
        Assert.assertEquals('p', NormalizeTokenFilter.normalize('\uFF30'));
        Assert.assertEquals('q', NormalizeTokenFilter.normalize('\uFF31'));
        Assert.assertEquals('r', NormalizeTokenFilter.normalize('\uFF32'));
        Assert.assertEquals('s', NormalizeTokenFilter.normalize('\uFF33'));
        Assert.assertEquals('t', NormalizeTokenFilter.normalize('\uFF34'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\uFF35'));
        Assert.assertEquals('v', NormalizeTokenFilter.normalize('\uFF36'));
        Assert.assertEquals('w', NormalizeTokenFilter.normalize('\uFF37'));
        Assert.assertEquals('x', NormalizeTokenFilter.normalize('\uFF38'));
        Assert.assertEquals('y', NormalizeTokenFilter.normalize('\uFF39'));
        Assert.assertEquals('z', NormalizeTokenFilter.normalize('\uFF3A'));

        Assert.assertEquals('a', NormalizeTokenFilter.normalize('\uFF41'));
        Assert.assertEquals('b', NormalizeTokenFilter.normalize('\uFF42'));
        Assert.assertEquals('c', NormalizeTokenFilter.normalize('\uFF43'));
        Assert.assertEquals('d', NormalizeTokenFilter.normalize('\uFF44'));
        Assert.assertEquals('e', NormalizeTokenFilter.normalize('\uFF45'));
        Assert.assertEquals('f', NormalizeTokenFilter.normalize('\uFF46'));
        Assert.assertEquals('g', NormalizeTokenFilter.normalize('\uFF47'));
        Assert.assertEquals('h', NormalizeTokenFilter.normalize('\uFF48'));
        Assert.assertEquals('i', NormalizeTokenFilter.normalize('\uFF49'));
        Assert.assertEquals('j', NormalizeTokenFilter.normalize('\uFF4A'));
        Assert.assertEquals('k', NormalizeTokenFilter.normalize('\uFF4B'));
        Assert.assertEquals('l', NormalizeTokenFilter.normalize('\uFF4C'));
        Assert.assertEquals('m', NormalizeTokenFilter.normalize('\uFF4D'));
        Assert.assertEquals('n', NormalizeTokenFilter.normalize('\uFF4E'));
        Assert.assertEquals('o', NormalizeTokenFilter.normalize('\uFF4F'));
        Assert.assertEquals('p', NormalizeTokenFilter.normalize('\uFF50'));
        Assert.assertEquals('q', NormalizeTokenFilter.normalize('\uFF51'));
        Assert.assertEquals('r', NormalizeTokenFilter.normalize('\uFF52'));
        Assert.assertEquals('s', NormalizeTokenFilter.normalize('\uFF53'));
        Assert.assertEquals('t', NormalizeTokenFilter.normalize('\uFF54'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\uFF55'));
        Assert.assertEquals('v', NormalizeTokenFilter.normalize('\uFF56'));
        Assert.assertEquals('w', NormalizeTokenFilter.normalize('\uFF57'));
        Assert.assertEquals('x', NormalizeTokenFilter.normalize('\uFF58'));
        Assert.assertEquals('y', NormalizeTokenFilter.normalize('\uFF59'));
        Assert.assertEquals('z', NormalizeTokenFilter.normalize('\uFF5A'));
    }

    @Test
    public void number() {
        Assert.assertEquals('0', NormalizeTokenFilter.normalize('\uFF10'));
        Assert.assertEquals('1', NormalizeTokenFilter.normalize('\uFF11'));
        Assert.assertEquals('2', NormalizeTokenFilter.normalize('\uFF12'));
        Assert.assertEquals('3', NormalizeTokenFilter.normalize('\uFF13'));
        Assert.assertEquals('4', NormalizeTokenFilter.normalize('\uFF14'));
        Assert.assertEquals('5', NormalizeTokenFilter.normalize('\uFF15'));
        Assert.assertEquals('6', NormalizeTokenFilter.normalize('\uFF16'));
        Assert.assertEquals('7', NormalizeTokenFilter.normalize('\uFF17'));
        Assert.assertEquals('8', NormalizeTokenFilter.normalize('\uFF18'));
        Assert.assertEquals('9', NormalizeTokenFilter.normalize('\uFF19'));
    }

    /**
     * @see http://en.wikipedia.org/wiki/Trema_(diacritic)
     */
    @Test
    public void trema() {
        Assert.assertEquals('a', NormalizeTokenFilter.normalize('\u00c4'));
        Assert.assertEquals('a', NormalizeTokenFilter.normalize('\u00e4'));
        Assert.assertEquals('a', NormalizeTokenFilter.normalize('\u01de'));
        Assert.assertEquals('a', NormalizeTokenFilter.normalize('\u01df'));
        Assert.assertEquals('e', NormalizeTokenFilter.normalize('\u00cb'));
        Assert.assertEquals('e', NormalizeTokenFilter.normalize('\u00eb'));
        Assert.assertEquals('h', NormalizeTokenFilter.normalize('\u1e26'));
        Assert.assertEquals('h', NormalizeTokenFilter.normalize('\u1e27'));
        Assert.assertEquals('i', NormalizeTokenFilter.normalize('\u00cf'));
        Assert.assertEquals('i', NormalizeTokenFilter.normalize('\u00ef'));
        Assert.assertEquals('i', NormalizeTokenFilter.normalize('\u1e2e'));
        Assert.assertEquals('i', NormalizeTokenFilter.normalize('\u1e2f'));
        Assert.assertEquals('o', NormalizeTokenFilter.normalize('\u00d6'));
        Assert.assertEquals('o', NormalizeTokenFilter.normalize('\u00f6'));
        Assert.assertEquals('o', NormalizeTokenFilter.normalize('\u022a'));
        Assert.assertEquals('o', NormalizeTokenFilter.normalize('\u022b'));
        Assert.assertEquals('o', NormalizeTokenFilter.normalize('\u1e4e'));
        Assert.assertEquals('o', NormalizeTokenFilter.normalize('\u1e4f'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u00dc'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u00fc'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u01d5'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u01d6'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u01d7'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u01d8'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u01d9'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u01da'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u01db'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u01dc'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u1e72'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u1e73'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u1e7a'));
        Assert.assertEquals('u', NormalizeTokenFilter.normalize('\u1e7b'));
        Assert.assertEquals('w', NormalizeTokenFilter.normalize('\u1e84'));
        Assert.assertEquals('w', NormalizeTokenFilter.normalize('\u1e85'));
        Assert.assertEquals('x', NormalizeTokenFilter.normalize('\u1e8c'));
        Assert.assertEquals('x', NormalizeTokenFilter.normalize('\u1e8d'));
        Assert.assertEquals('y', NormalizeTokenFilter.normalize('\u0178'));
        Assert.assertEquals('y', NormalizeTokenFilter.normalize('\u00ff'));
    }

    /**
     * @see http://en.wikipedia.org/wiki/Katakana
     */
    @Test
    public void katakana() {
        Assert.assertEquals('\u30A2', NormalizeTokenFilter.normalize('\uFF71'));
        Assert.assertEquals('\u30A4', NormalizeTokenFilter.normalize('\uFF72'));
        Assert.assertEquals('\u30A6', NormalizeTokenFilter.normalize('\uFF73'));
        Assert.assertEquals('\u30A8', NormalizeTokenFilter.normalize('\uFF74'));
        Assert.assertEquals('\u30AA', NormalizeTokenFilter.normalize('\uFF75'));
        Assert.assertEquals('\u30AB', NormalizeTokenFilter.normalize('\uFF76'));
        Assert.assertEquals('\u30AD', NormalizeTokenFilter.normalize('\uFF77'));
        Assert.assertEquals('\u30AF', NormalizeTokenFilter.normalize('\uFF78'));
        Assert.assertEquals('\u30B1', NormalizeTokenFilter.normalize('\uFF79'));
        Assert.assertEquals('\u30B3', NormalizeTokenFilter.normalize('\uFF7A'));
        Assert.assertEquals('\u30B5', NormalizeTokenFilter.normalize('\uFF7B'));
        Assert.assertEquals('\u30B7', NormalizeTokenFilter.normalize('\uFF7C'));
        Assert.assertEquals('\u30B9', NormalizeTokenFilter.normalize('\uFF7D'));
        Assert.assertEquals('\u30BB', NormalizeTokenFilter.normalize('\uFF7E'));
        Assert.assertEquals('\u30BD', NormalizeTokenFilter.normalize('\uFF7F'));
        Assert.assertEquals('\u30BF', NormalizeTokenFilter.normalize('\uFF80'));
        Assert.assertEquals('\u30C1', NormalizeTokenFilter.normalize('\uFF81'));
        Assert.assertEquals('\u30C4', NormalizeTokenFilter.normalize('\uFF82'));
        Assert.assertEquals('\u30C6', NormalizeTokenFilter.normalize('\uFF83'));
        Assert.assertEquals('\u30C8', NormalizeTokenFilter.normalize('\uFF84'));
        Assert.assertEquals('\u30CA', NormalizeTokenFilter.normalize('\uFF85'));
        Assert.assertEquals('\u30CB', NormalizeTokenFilter.normalize('\uFF86'));
        Assert.assertEquals('\u30CC', NormalizeTokenFilter.normalize('\uFF87'));
        Assert.assertEquals('\u30CD', NormalizeTokenFilter.normalize('\uFF88'));
        Assert.assertEquals('\u30CE', NormalizeTokenFilter.normalize('\uFF89'));
        Assert.assertEquals('\u30CF', NormalizeTokenFilter.normalize('\uFF8A'));
        Assert.assertEquals('\u30D2', NormalizeTokenFilter.normalize('\uFF8B'));
        Assert.assertEquals('\u30D5', NormalizeTokenFilter.normalize('\uFF8C'));
        Assert.assertEquals('\u30D8', NormalizeTokenFilter.normalize('\uFF8D'));
        Assert.assertEquals('\u30DB', NormalizeTokenFilter.normalize('\uFF8E'));
        Assert.assertEquals('\u30DE', NormalizeTokenFilter.normalize('\uFF8F'));
        Assert.assertEquals('\u30DF', NormalizeTokenFilter.normalize('\uFF90'));
        Assert.assertEquals('\u30E0', NormalizeTokenFilter.normalize('\uFF91'));
        Assert.assertEquals('\u30E1', NormalizeTokenFilter.normalize('\uFF92'));
        Assert.assertEquals('\u30E2', NormalizeTokenFilter.normalize('\uFF93'));
        Assert.assertEquals('\u30E4', NormalizeTokenFilter.normalize('\uFF94'));
        Assert.assertEquals('\u30E6', NormalizeTokenFilter.normalize('\uFF95'));
        Assert.assertEquals('\u30E8', NormalizeTokenFilter.normalize('\uFF96'));
        Assert.assertEquals('\u30E9', NormalizeTokenFilter.normalize('\uFF97'));
        Assert.assertEquals('\u30EA', NormalizeTokenFilter.normalize('\uFF98'));
        Assert.assertEquals('\u30EB', NormalizeTokenFilter.normalize('\uFF99'));
        Assert.assertEquals('\u30EC', NormalizeTokenFilter.normalize('\uFF9A'));
        Assert.assertEquals('\u30ED', NormalizeTokenFilter.normalize('\uFF9B'));
        Assert.assertEquals('\u30EF', NormalizeTokenFilter.normalize('\uFF9C'));
        Assert.assertEquals('\u30F3', NormalizeTokenFilter.normalize('\uFF9D'));
    }

}
