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
 * @author smukhopadhyay
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
        Assert.assertEquals('\u3041', NormalizeTokenFilter.normalize('\u30A1'));
        Assert.assertEquals('\u3042', NormalizeTokenFilter.normalize('\u30A2'));
        Assert.assertEquals('\u3043', NormalizeTokenFilter.normalize('\u30A3'));
        Assert.assertEquals('\u3044', NormalizeTokenFilter.normalize('\u30A4'));
        Assert.assertEquals('\u3045', NormalizeTokenFilter.normalize('\u30A5'));
        Assert.assertEquals('\u3046', NormalizeTokenFilter.normalize('\u30A6'));
        Assert.assertEquals('\u3047', NormalizeTokenFilter.normalize('\u30A7'));
        Assert.assertEquals('\u3048', NormalizeTokenFilter.normalize('\u30A8'));
        Assert.assertEquals('\u3049', NormalizeTokenFilter.normalize('\u30A9'));
        Assert.assertEquals('\u304A', NormalizeTokenFilter.normalize('\u30AA'));
        Assert.assertEquals('\u304B', NormalizeTokenFilter.normalize('\u30AB'));
        Assert.assertEquals('\u304C', NormalizeTokenFilter.normalize('\u30AC'));
        Assert.assertEquals('\u304D', NormalizeTokenFilter.normalize('\u30AD'));
        Assert.assertEquals('\u304E', NormalizeTokenFilter.normalize('\u30AE'));
        Assert.assertEquals('\u304F', NormalizeTokenFilter.normalize('\u30AF'));
        Assert.assertEquals('\u3051', NormalizeTokenFilter.normalize('\u30B1'));
        Assert.assertEquals('\u3052', NormalizeTokenFilter.normalize('\u30B2'));
        Assert.assertEquals('\u3053', NormalizeTokenFilter.normalize('\u30B3'));
        Assert.assertEquals('\u3054', NormalizeTokenFilter.normalize('\u30B4'));
        Assert.assertEquals('\u3055', NormalizeTokenFilter.normalize('\u30B5'));
        Assert.assertEquals('\u3056', NormalizeTokenFilter.normalize('\u30B6'));
        Assert.assertEquals('\u3057', NormalizeTokenFilter.normalize('\u30B7'));
        Assert.assertEquals('\u3058', NormalizeTokenFilter.normalize('\u30B8'));
        Assert.assertEquals('\u3059', NormalizeTokenFilter.normalize('\u30B9'));
        Assert.assertEquals('\u305A', NormalizeTokenFilter.normalize('\u30BA'));
        Assert.assertEquals('\u305B', NormalizeTokenFilter.normalize('\u30BB'));
        Assert.assertEquals('\u305C', NormalizeTokenFilter.normalize('\u30BC'));
        Assert.assertEquals('\u305D', NormalizeTokenFilter.normalize('\u30BD'));
        Assert.assertEquals('\u305E', NormalizeTokenFilter.normalize('\u30BE'));
        Assert.assertEquals('\u305F', NormalizeTokenFilter.normalize('\u30BF'));
        Assert.assertEquals('\u3061', NormalizeTokenFilter.normalize('\u30C1'));
        Assert.assertEquals('\u3062', NormalizeTokenFilter.normalize('\u30C2'));
        Assert.assertEquals('\u3063', NormalizeTokenFilter.normalize('\u30C3'));
        Assert.assertEquals('\u3064', NormalizeTokenFilter.normalize('\u30C4'));
        Assert.assertEquals('\u3065', NormalizeTokenFilter.normalize('\u30C5'));
        Assert.assertEquals('\u3066', NormalizeTokenFilter.normalize('\u30C6'));
        Assert.assertEquals('\u3067', NormalizeTokenFilter.normalize('\u30C7'));
        Assert.assertEquals('\u3068', NormalizeTokenFilter.normalize('\u30C8'));
        Assert.assertEquals('\u3069', NormalizeTokenFilter.normalize('\u30C9'));
        Assert.assertEquals('\u306A', NormalizeTokenFilter.normalize('\u30CA'));
        Assert.assertEquals('\u306B', NormalizeTokenFilter.normalize('\u30CB'));
        Assert.assertEquals('\u306C', NormalizeTokenFilter.normalize('\u30CC'));
        Assert.assertEquals('\u306D', NormalizeTokenFilter.normalize('\u30CD'));
        Assert.assertEquals('\u306E', NormalizeTokenFilter.normalize('\u30CE'));
        Assert.assertEquals('\u306F', NormalizeTokenFilter.normalize('\u30CF'));
        Assert.assertEquals('\u3071', NormalizeTokenFilter.normalize('\u30D1'));
        Assert.assertEquals('\u3072', NormalizeTokenFilter.normalize('\u30D2'));
        Assert.assertEquals('\u3073', NormalizeTokenFilter.normalize('\u30D3'));
        Assert.assertEquals('\u3074', NormalizeTokenFilter.normalize('\u30D4'));
        Assert.assertEquals('\u3075', NormalizeTokenFilter.normalize('\u30D5'));
        Assert.assertEquals('\u3076', NormalizeTokenFilter.normalize('\u30D6'));
        Assert.assertEquals('\u3077', NormalizeTokenFilter.normalize('\u30D7'));
        Assert.assertEquals('\u3078', NormalizeTokenFilter.normalize('\u30D8'));
        Assert.assertEquals('\u3079', NormalizeTokenFilter.normalize('\u30D9'));
        Assert.assertEquals('\u307A', NormalizeTokenFilter.normalize('\u30DA'));
        Assert.assertEquals('\u307B', NormalizeTokenFilter.normalize('\u30DB'));
        Assert.assertEquals('\u307C', NormalizeTokenFilter.normalize('\u30DC'));
        Assert.assertEquals('\u307D', NormalizeTokenFilter.normalize('\u30DD'));
        Assert.assertEquals('\u307E', NormalizeTokenFilter.normalize('\u30DE'));
        Assert.assertEquals('\u307F', NormalizeTokenFilter.normalize('\u30DF'));
        Assert.assertEquals('\u3081', NormalizeTokenFilter.normalize('\u30E1'));
        Assert.assertEquals('\u3082', NormalizeTokenFilter.normalize('\u30E2'));
        Assert.assertEquals('\u3083', NormalizeTokenFilter.normalize('\u30E3'));
        Assert.assertEquals('\u3084', NormalizeTokenFilter.normalize('\u30E4'));
        Assert.assertEquals('\u3085', NormalizeTokenFilter.normalize('\u30E5'));
        Assert.assertEquals('\u3086', NormalizeTokenFilter.normalize('\u30E6'));
        Assert.assertEquals('\u3087', NormalizeTokenFilter.normalize('\u30E7'));
        Assert.assertEquals('\u3088', NormalizeTokenFilter.normalize('\u30E8'));
        Assert.assertEquals('\u3089', NormalizeTokenFilter.normalize('\u30E9'));
        Assert.assertEquals('\u308A', NormalizeTokenFilter.normalize('\u30EA'));
        Assert.assertEquals('\u308B', NormalizeTokenFilter.normalize('\u30EB'));
        Assert.assertEquals('\u308C', NormalizeTokenFilter.normalize('\u30EC'));
        Assert.assertEquals('\u308D', NormalizeTokenFilter.normalize('\u30ED'));
        Assert.assertEquals('\u308E', NormalizeTokenFilter.normalize('\u30EE'));
        Assert.assertEquals('\u308F', NormalizeTokenFilter.normalize('\u30EF'));
        Assert.assertEquals('\u3091', NormalizeTokenFilter.normalize('\u30F1'));
        Assert.assertEquals('\u3092', NormalizeTokenFilter.normalize('\u30F2'));
        Assert.assertEquals('\u3093', NormalizeTokenFilter.normalize('\u30F3'));
        Assert.assertEquals('\u3094', NormalizeTokenFilter.normalize('\u30F4'));
        Assert.assertEquals('\u3095', NormalizeTokenFilter.normalize('\u30F5'));
        Assert.assertEquals('\u3096', NormalizeTokenFilter.normalize('\u30F6'));
    }

    /**
     * @see http://en.wikipedia.org/wiki/Katakana
     */
    @Test
    public void HalfWidthkatakana() {
        Assert.assertEquals('\u3042', NormalizeTokenFilter.normalize('\uFF71'));
        Assert.assertEquals('\u3044', NormalizeTokenFilter.normalize('\uFF72'));
        Assert.assertEquals('\u3046', NormalizeTokenFilter.normalize('\uFF73'));
        Assert.assertEquals('\u3048', NormalizeTokenFilter.normalize('\uFF74'));
        Assert.assertEquals('\u304A', NormalizeTokenFilter.normalize('\uFF75'));
        Assert.assertEquals('\u304B', NormalizeTokenFilter.normalize('\uFF76'));
        Assert.assertEquals('\u304D', NormalizeTokenFilter.normalize('\uFF77'));
        Assert.assertEquals('\u304F', NormalizeTokenFilter.normalize('\uFF78'));
        Assert.assertEquals('\u3051', NormalizeTokenFilter.normalize('\uFF79'));
        Assert.assertEquals('\u3053', NormalizeTokenFilter.normalize('\uFF7A'));
        Assert.assertEquals('\u3055', NormalizeTokenFilter.normalize('\uFF7B'));
        Assert.assertEquals('\u3057', NormalizeTokenFilter.normalize('\uFF7C'));
        Assert.assertEquals('\u3059', NormalizeTokenFilter.normalize('\uFF7D'));
        Assert.assertEquals('\u305B', NormalizeTokenFilter.normalize('\uFF7E'));
        Assert.assertEquals('\u305D', NormalizeTokenFilter.normalize('\uFF7F'));
        Assert.assertEquals('\u305F', NormalizeTokenFilter.normalize('\uFF80'));
        Assert.assertEquals('\u3061', NormalizeTokenFilter.normalize('\uFF81'));
        Assert.assertEquals('\u3064', NormalizeTokenFilter.normalize('\uFF82'));
        Assert.assertEquals('\u3066', NormalizeTokenFilter.normalize('\uFF83'));
        Assert.assertEquals('\u3068', NormalizeTokenFilter.normalize('\uFF84'));
        Assert.assertEquals('\u306A', NormalizeTokenFilter.normalize('\uFF85'));
        Assert.assertEquals('\u306B', NormalizeTokenFilter.normalize('\uFF86'));
        Assert.assertEquals('\u306C', NormalizeTokenFilter.normalize('\uFF87'));
        Assert.assertEquals('\u306D', NormalizeTokenFilter.normalize('\uFF88'));
        Assert.assertEquals('\u306E', NormalizeTokenFilter.normalize('\uFF89'));
        Assert.assertEquals('\u306F', NormalizeTokenFilter.normalize('\uFF8A'));
        Assert.assertEquals('\u3072', NormalizeTokenFilter.normalize('\uFF8B'));
        Assert.assertEquals('\u3075', NormalizeTokenFilter.normalize('\uFF8C'));
        Assert.assertEquals('\u3078', NormalizeTokenFilter.normalize('\uFF8D'));
        Assert.assertEquals('\u307B', NormalizeTokenFilter.normalize('\uFF8E'));
        Assert.assertEquals('\u307E', NormalizeTokenFilter.normalize('\uFF8F'));
        Assert.assertEquals('\u307F', NormalizeTokenFilter.normalize('\uFF90'));
        Assert.assertEquals('\u3080', NormalizeTokenFilter.normalize('\uFF91'));
        Assert.assertEquals('\u3081', NormalizeTokenFilter.normalize('\uFF92'));
        Assert.assertEquals('\u3082', NormalizeTokenFilter.normalize('\uFF93'));
        Assert.assertEquals('\u3084', NormalizeTokenFilter.normalize('\uFF94'));
        Assert.assertEquals('\u3086', NormalizeTokenFilter.normalize('\uFF95'));
        Assert.assertEquals('\u3088', NormalizeTokenFilter.normalize('\uFF96'));
        Assert.assertEquals('\u3089', NormalizeTokenFilter.normalize('\uFF97'));
        Assert.assertEquals('\u308A', NormalizeTokenFilter.normalize('\uFF98'));
        Assert.assertEquals('\u308B', NormalizeTokenFilter.normalize('\uFF99'));
        Assert.assertEquals('\u308C', NormalizeTokenFilter.normalize('\uFF9A'));
        Assert.assertEquals('\u308D', NormalizeTokenFilter.normalize('\uFF9B'));
        Assert.assertEquals('\u308F', NormalizeTokenFilter.normalize('\uFF9C'));
        Assert.assertEquals('\u3093', NormalizeTokenFilter.normalize('\uFF9D'));
    }
    
    

}
