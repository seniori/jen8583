/* 
 * Copyright (c) 2014 Ian Bondoc
 * 
 * This file is part of Jen8583
 * 
 * Jen8583 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or(at your option) any later version.
 * 
 * Jen8583 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 */
package org.chiknrice.iso;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public class TestIsoMessage {

    @Test
    public void testValidFieldExpression() {
        IsoMessage m = new IsoMessage(100);
        Map<Integer, Object> f1 = new TreeMap<>();
        m.setField(1, f1);
        Map<Integer, Object> f12 = new TreeMap<>();
        f1.put(2, f12);
        String f123 = "A Value";
        f12.put(3, f123);

        String returned = m.getField("1.2.3");
        assertEquals(returned, f123);

        IsoMessage m1 = new IsoMessage(100);
        m1.setField("1.2.3", f123);
        assertEquals(m1, m);
    }

    @Test
    public void testCopyRootField() {
        IsoMessage m = new IsoMessage(100);
        Map<Integer, Object> f1 = new TreeMap<>();
        m.setField(1, f1);
        Map<Integer, Object> f12 = new TreeMap<>();
        f1.put(2, f12);
        String f123 = "A Value";
        f12.put(3, f123);
        String f124 = "Another Value";
        f12.put(4, f124);

        IsoMessage m2 = new IsoMessage(110);
        m2.copyFields(m, "1.2");
        assertEquals(f123, m2.getField("1.2.3"));
        assertEquals(f124, m2.getField("1.2.4"));
    }

    @Test
    public void testNotExistingField() {
        IsoMessage m = new IsoMessage(100);
        Map<Integer, Object> f1 = new TreeMap<>();
        m.setField(1, f1);
        Map<Integer, Object> f12 = new TreeMap<>();
        f1.put(2, f12);
        Map<Integer, Object> f123 = new TreeMap<>();
        f12.put(3, f123);

        Map<Integer, Object> returned = m.getField("1.3.5");
        assertThat(returned, nullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFieldExpression1() {
        IsoMessage m = new IsoMessage(100);
        m.getField("a.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFieldExpression2() {
        IsoMessage m = new IsoMessage(100);
        m.getField("1.a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFieldExpression3() {
        IsoMessage m = new IsoMessage(100);
        m.getField("a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFieldExpression4() {
        IsoMessage m = new IsoMessage(100);
        m.getField("1.");
    }

}
