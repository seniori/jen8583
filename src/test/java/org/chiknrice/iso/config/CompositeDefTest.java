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

import org.chiknrice.iso.CodecException;
import org.chiknrice.iso.ConfigException;
import org.chiknrice.iso.codec.AlphaCodec;
import org.chiknrice.iso.codec.BitmapCodec;
import org.chiknrice.iso.codec.Codec;
import org.chiknrice.iso.codec.NumericCodec;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.chiknrice.iso.config.ComponentDef.Encoding;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class CompositeDefTest {

    @Test(expected = ConfigException.class)
    public void testBitmapNotAllowedWithSubfield1() {
        BitmapCodec bitmapCodec = mock(BitmapCodec.class);
        SortedMap<Integer, ComponentDef> subFieldsDef = new TreeMap<>();
        subFieldsDef.put(1, mock(ComponentDef.class));
        new CompositeDef(subFieldsDef, bitmapCodec);
    }

    @Test
    public void testEncodeNonCompositeSubFields() {
        Codec<String> codec1 = new AlphaCodec(false);
        ComponentDef def1 = new ComponentDef(codec1);

        SortedMap<Integer, ComponentDef> subFieldsDef = new TreeMap<>();
        subFieldsDef.put(1, def1);
        subFieldsDef.put(2, def1);

        CompositeDef compositeDef = new CompositeDef(subFieldsDef);

        ByteBuffer buf = ByteBuffer.allocate(20);

        Map<Integer, Object> values = new HashMap<>();
        values.put(1, "1");
        values.put(2, "2");

        compositeDef.encode(buf, values);

        assertThat(buf.position(), is(2));
        byte[] encoded = new byte[buf.position()];
        buf.clear();
        buf.get(encoded);
        assertThat(new String(encoded, StandardCharsets.ISO_8859_1), is("12"));
    }

    @Test
    public void testEncodeVarSubField() {
        Codec<String> alphaCodec = new AlphaCodec(false);
        ComponentDef alphaComponent = new ComponentDef(alphaCodec);
        Codec<Number> lengthCodec = new NumericCodec(Encoding.CHAR, 2);

        ComponentDef varAlphaComponent = new ComponentDef(lengthCodec, alphaCodec);

        SortedMap<Integer, ComponentDef> subFieldsDef = new TreeMap<>();
        subFieldsDef.put(1, alphaComponent);
        subFieldsDef.put(2, varAlphaComponent);
        CompositeDef compositeDef = new CompositeDef(subFieldsDef);

        ByteBuffer buf = ByteBuffer.allocate(20);

        Map<Integer, Object> values = new HashMap<>();
        values.put(1, "1");
        values.put(2, "abcde");

        compositeDef.encode(buf, values);

        assertThat(buf.position(), is(8));
        byte[] encoded = new byte[buf.position()];
        buf.clear();
        buf.get(encoded);
        assertThat(new String(encoded, StandardCharsets.ISO_8859_1), is("105abcde"));
    }

    @Test(expected = CodecException.class)
    public void testCodecMismatch() {
        SortedMap<Integer, ComponentDef> subComponentDefs = new TreeMap<>();
        subComponentDefs.put(1, new ComponentDef(new AlphaCodec(false)));
        CompositeDef codec = new CompositeDef(subComponentDefs);

        Map<Integer, Object> values = new HashMap<>();
        values.put(1, 5);

        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, values);
    }

    @Test
    public void testEncodeCompositeSubField() {
        SortedMap<Integer, ComponentDef> level1Defs = new TreeMap<>();
        SortedMap<Integer, ComponentDef> level2Defs = new TreeMap<>();
        level1Defs.put(1, new ComponentDef(new AlphaCodec(false)));
        level1Defs.put(2, new ComponentDef(new AlphaCodec(false)));
        level1Defs.put(3, new ComponentDef(new CompositeDef(level2Defs)));

        level2Defs.put(1, new ComponentDef(new AlphaCodec(false)));
        level2Defs.put(2, new ComponentDef(new AlphaCodec(false)));


        CompositeDef codec = new CompositeDef(level1Defs);

        ByteBuffer buf = ByteBuffer.allocate(64);
        Map<Integer, Object> level1Values = new HashMap<>();
        level1Values.put(1, "1");
        level1Values.put(2, "2");

        Map<Integer, Object> level2Values = new HashMap<>();
        level2Values.put(1, "1");
        level2Values.put(2, "2");

        level1Values.put(3, level2Values);


        codec.encode(buf, level1Values);

        assertThat(buf.position(), is(4));

        byte[] result = new byte[4];
        buf.clear();
        buf.get(result);

        byte[] expected = "1212".getBytes(StandardCharsets.ISO_8859_1);

        assertThat(result, is(expected));
    }

}
