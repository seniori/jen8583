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
package org.chiknrice.iso.config;

import org.chiknrice.iso.ConfigException;
import org.chiknrice.iso.codec.NumericCodec;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class IsoMessageDefTest {

    @Test
    public void testExtension() {
        IsoMessageDef def = IsoMessageDef.build("test.xml");
        CompositeDef def100 = def.getFieldsDef().get(100);
        CompositeDef def110 = def.getFieldsDef().get(110);
        CompositeDef def200 = def.getFieldsDef().get(200);

        assertNotEquals(def100, def110);
        assertEquals(def100, def200);

        Map<Integer, ComponentDef> def100Components = def100.getSubComponentDefs();
        Map<Integer, ComponentDef> def110Components = def110.getSubComponentDefs();

        assertTrue(def100Components.get(5) instanceof CompositeDef);
        assertNotNull(def110Components.get(5).getLengthCodec());

        CompositeDef codec100_5 = (CompositeDef) def100Components.get(5);
        CompositeDef codec110_5 = (CompositeDef) def110Components.get(5);

        assertNotEquals(codec100_5, codec110_5);
        assertEquals(null, codec100_5.getSubComponentDefs().get(3));
        assertEquals(null, codec110_5.getSubComponentDefs().get(1));
        assertEquals(codec100_5.getSubComponentDefs().get(2), codec110_5.getSubComponentDefs().get(2));

        CompositeDef codec100_6 = (CompositeDef) def100Components.get(6);
        CompositeDef codec110_6 = (CompositeDef) def110Components.get(6);

        assertEquals(5, codec100_6.getSubComponentDefs().size());
        assertEquals(4, codec110_6.getSubComponentDefs().size());
        assertTrue(codec100_6.getSubComponentDefs().get(3) != null);
        assertTrue(codec110_6.getSubComponentDefs().get(3) != null);
        assertNotEquals(codec100_6.getSubComponentDefs().get(2), codec110_6.getSubComponentDefs().get(2));
        assertEquals(codec100_6.getSubComponentDefs().get(3), codec110_6.getSubComponentDefs().get(3));
        assertTrue(codec110_6.getSubComponentDefs().get(2).getValueCodec() instanceof NumericCodec);

        assertTrue(def100Components.get(4) != null);
        assertTrue(def110Components.get(4) == null);
    }

    @Test
    public void testToString() {
        IsoMessageDef def = IsoMessageDef.build("test.xml");
        CompositeDef codec100 = def.getFieldsDef().get(100);
        CompositeDef def6 = (CompositeDef) codec100.getSubComponentDefs().get(6);
        ComponentDef def2 = def6.getSubComponentDefs().get(2);
        assertEquals("6.2", def2.toString());
    }

    @Test(expected = ConfigException.class)
    public void testInvalidExtension() {
        IsoMessageDef.build("test-invalid-extension.xml");
    }

    @Test(expected = ConfigException.class)
    public void testInvalidBitmapComposite() {
        IsoMessageDef.build("test-invalid-bitmap-composite.xml");
    }

    @Test(expected = ConfigException.class)
    public void testDuplicateMti() {
        IsoMessageDef.build("test-duplicate-mti.xml");
    }

    @Test(expected = ConfigException.class)
    public void testDuplicateMtiExtension() {
        IsoMessageDef.build("test-duplicate-mti-extension.xml");
    }

    @Test(expected = ConfigException.class)
    public void testDuplicateField() {
        IsoMessageDef.build("test-duplicate-field.xml");
    }

    @Test(expected = ConfigException.class)
    public void testInvalidSchema() {
        IsoMessageDef.build("test-invalid-schema.xml");
    }

}
