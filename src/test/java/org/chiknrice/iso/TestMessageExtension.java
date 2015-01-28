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

import org.chiknrice.iso.codec.CompositeCodec;
import org.chiknrice.iso.codec.VarCodec;
import org.chiknrice.iso.config.ComponentDef;
import org.chiknrice.iso.config.IsoMessageDef;
import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public class TestMessageExtension extends BaseTest {

    @Test
    public void testClone() {
        IsoMessageDef def = IsoMessageDef.build("test.xml");
        ComponentDef def100 = def.getFieldsDef().get(100);
        ComponentDef def110 = def.getFieldsDef().get(110);

        assertEquals(def100.getCodec(), def110.getCodec());
        assertEquals(((CompositeCodec) def100.getCodec()).getSubComponentDefs().get(5),
                ((CompositeCodec) def110.getCodec()).getSubComponentDefs().get(5));

    }

    @Test
    public void testToString() {
        IsoMessageDef def = IsoMessageDef.build("test.xml");
        ComponentDef codec100 = def.getFieldsDef().get(100);
        assertEquals("6.2", ((CompositeCodec) ((VarCodec<?>) ((CompositeCodec) codec100.getCodec())
                .getSubComponentDefs().get(6).getCodec()).getCodec()).getSubComponentDefs().get(2).toString());
    }

}
