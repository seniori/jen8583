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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.chiknrice.iso.codec.CompositeCodec;
import org.chiknrice.iso.codec.NumericCodec;
import org.chiknrice.iso.codec.VarCodec;
import org.chiknrice.iso.config.ComponentDef;
import org.chiknrice.iso.config.IsoMessageDef;
import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public class IsoMessageDefTest {

    @Test
    public void testExtension() {
        IsoMessageDef def = IsoMessageDef.build("test.xml");
        ComponentDef def100 = def.getFieldsDef().get(100);
        ComponentDef def110 = def.getFieldsDef().get(110);
        ComponentDef def200 = def.getFieldsDef().get(200);

        assertNotEquals(def100, def110);
        assertEquals(def100, def200);

        Map<Integer, ComponentDef> def100Components = ((CompositeCodec) def100.getCodec()).getSubComponentDefs();
        Map<Integer, ComponentDef> def110Components = ((CompositeCodec) def110.getCodec()).getSubComponentDefs();

        assertTrue(def100Components.get(5).getCodec() instanceof CompositeCodec);
        assertTrue(def110Components.get(5).getCodec() instanceof VarCodec);

        CompositeCodec codec100_5 = (CompositeCodec) def100Components.get(5).getCodec();
        CompositeCodec codec110_5 = (CompositeCodec) ((VarCodec<?>) def110Components.get(5).getCodec()).getCodec();

        assertNotEquals(codec100_5, codec110_5);
        assertEquals(null, codec100_5.getSubComponentDefs().get(3));
        assertEquals(null, codec110_5.getSubComponentDefs().get(1));
        assertEquals(codec100_5.getSubComponentDefs().get(2), codec110_5.getSubComponentDefs().get(2));

        CompositeCodec codec100_6 = (CompositeCodec) ((VarCodec<?>) def100Components.get(6).getCodec()).getCodec();
        CompositeCodec codec110_6 = (CompositeCodec) ((VarCodec<?>) def110Components.get(6).getCodec()).getCodec();

        assertEquals(5, codec100_6.getSubComponentDefs().size());
        assertEquals(4, codec110_6.getSubComponentDefs().size());
        assertTrue(codec100_6.getSubComponentDefs().get(3) != null);
        assertTrue(codec110_6.getSubComponentDefs().get(3) != null);
        assertNotEquals(codec100_6.getSubComponentDefs().get(2), codec110_6.getSubComponentDefs().get(2));
        assertEquals(codec100_6.getSubComponentDefs().get(3), codec110_6.getSubComponentDefs().get(3));
        assertTrue(codec110_6.getSubComponentDefs().get(2).getCodec() instanceof NumericCodec);

        assertTrue(def100Components.get(4) != null);
        assertTrue(def110Components.get(4) == null);
    }

    @Test
    public void testToString() {
        IsoMessageDef def = IsoMessageDef.build("test.xml");
        ComponentDef codec100 = def.getFieldsDef().get(100);
        assertEquals("6.2", ((CompositeCodec) ((VarCodec<?>) ((CompositeCodec) codec100.getCodec())
                .getSubComponentDefs().get(6).getCodec()).getCodec()).getSubComponentDefs().get(2).toString());
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
