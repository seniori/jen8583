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
import org.chiknrice.iso.codec.*;
import org.chiknrice.iso.codec.BitmapCodec.Bitmap;
import org.junit.Test;
import org.mockito.InOrder;

import java.nio.ByteBuffer;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.chiknrice.iso.config.ComponentDef.Encoding;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
    public void testEncodeBitmap() {
        BitmapCodec bitmapCodec = spy(new BitmapCodec(Bitmap.Type.BINARY));
        SortedMap<Integer, ComponentDef> subComponentDefs = new TreeMap<>();
        AlphaCodec alphaCodec = spy(new AlphaCodec(false));
        subComponentDefs.put(2, new ComponentDef(alphaCodec));
        subComponentDefs.put(3, new ComponentDef(alphaCodec));

        CompositeDef compositeDef = new CompositeDef(subComponentDefs, bitmapCodec);

        Map<Integer, Object> values = new HashMap<>();
        values.put(2, "1");
        values.put(3, "2");

        ByteBuffer buf = ByteBuffer.allocate(32);

        compositeDef.encode(buf, values);

        InOrder inOrder = inOrder(bitmapCodec, alphaCodec);
        inOrder.verify(bitmapCodec).encode(buf, values.keySet());
        inOrder.verify(alphaCodec).encode(buf, "1");
        inOrder.verify(alphaCodec).encode(buf, "2");
    }

    @Test
    public void testEncodeVarLengthSubField() {
        Codec<String> alphaCodec = spy(new AlphaCodec(false));
        ComponentDef alphaComponent = new ComponentDef(alphaCodec);
        Codec<Number> lengthCodec = spy(new NumericCodec(Encoding.CHAR, 2));

        ComponentDef varAlphaComponent = new ComponentDef(lengthCodec, alphaCodec);

        SortedMap<Integer, ComponentDef> subFieldsDef = new TreeMap<>();
        subFieldsDef.put(1, alphaComponent);
        subFieldsDef.put(2, varAlphaComponent);
        CompositeDef compositeDef = new CompositeDef(subFieldsDef);

        ByteBuffer buf = ByteBuffer.allocate(32);

        Map<Integer, Object> values = new HashMap<>();
        values.put(1, "1");
        values.put(2, "abcde");

        compositeDef.encode(buf, values);

        InOrder inOrder = inOrder(alphaCodec, lengthCodec);

        inOrder.verify(alphaCodec).encode(buf, "1");
        inOrder.verify(alphaCodec).encode(buf, "abcde");
        inOrder.verify(lengthCodec).encode(buf, 5);

        assertThat(buf.position(), is(8));
        byte[] encoded = new byte[buf.position()];
        buf.clear();
        buf.get(encoded);
        assertThat(new String(encoded, ISO_8859_1), is("105abcde"));
    }

    @Test
    public void testEncodeVarLengthBinarySubField() {
        Codec<String> alphaCodec = spy(new AlphaCodec(false));
        ComponentDef alphaComponent = new ComponentDef(alphaCodec);
        Codec<Number> lengthCodec = spy(new NumericCodec(Encoding.CHAR, 2));

        Codec<byte[]> binaryCodec = spy(new BinaryCodec());
        ComponentDef varBinaryComponent = new ComponentDef(lengthCodec, binaryCodec);

        SortedMap<Integer, ComponentDef> subFieldsDef = new TreeMap<>();
        subFieldsDef.put(1, alphaComponent);
        subFieldsDef.put(2, varBinaryComponent);
        CompositeDef compositeDef = new CompositeDef(subFieldsDef);

        ByteBuffer buf = ByteBuffer.allocate(32);

        byte[] binaryValue = {0x00, 0x01};

        Map<Integer, Object> values = new HashMap<>();
        values.put(1, "1");
        values.put(2, binaryValue);

        compositeDef.encode(buf, values);

        InOrder inOrder = inOrder(alphaCodec, lengthCodec, binaryCodec);

        inOrder.verify(alphaCodec).encode(buf, "1");
        inOrder.verify(binaryCodec).encode(buf, binaryValue);
        inOrder.verify(lengthCodec).encode(buf, 2);

        assertThat(buf.position(), is(5));
        byte[] encoded = new byte[buf.position()];
        buf.clear();
        buf.get(encoded);
        assertThat(new String(encoded, ISO_8859_1), is("102" + new String(binaryValue, ISO_8859_1)));
    }

    @Test
    public void testEncodeTlvSubField() {
        Codec<String> alphaCodec = spy(new AlphaCodec(false));
        ComponentDef alphaComponent = new ComponentDef(alphaCodec);
        Codec<Number> lengthCodec = spy(new NumericCodec(Encoding.CHAR, 2));
        Codec<Number> tagCodec = spy(new NumericCodec(Encoding.CHAR, 2));

        ComponentDef tlvAlphaComponent = new ComponentDef(tagCodec, lengthCodec, alphaCodec);

        SortedMap<Integer, ComponentDef> subFieldsDef = new TreeMap<>();
        subFieldsDef.put(1, alphaComponent);
        subFieldsDef.put(2, tlvAlphaComponent);
        CompositeDef compositeDef = new CompositeDef(subFieldsDef);

        ByteBuffer buf = ByteBuffer.allocate(32);

        Map<Integer, Object> values = new HashMap<>();
        values.put(1, "1");
        values.put(2, "abcde");

        compositeDef.encode(buf, values);

        InOrder inOrder = inOrder(tagCodec, lengthCodec, alphaCodec);

        inOrder.verify(alphaCodec).encode(buf, "1");
        inOrder.verify(tagCodec).encode(buf, 2);
        inOrder.verify(alphaCodec).encode(buf, "abcde");
        inOrder.verify(lengthCodec).encode(buf, 5);

        assertThat(buf.position(), is(10));
        byte[] encoded = new byte[buf.position()];
        buf.clear();
        buf.get(encoded);
        assertThat(new String(encoded, ISO_8859_1), is("10205abcde"));
    }

    @Test
    public void testEncodeNonCompositeSubFields() {
        Codec<String> alphaCodec = new AlphaCodec(false);
        ComponentDef alphaComponentDef = new ComponentDef(alphaCodec);

        SortedMap<Integer, ComponentDef> subFieldsDef = new TreeMap<>();
        subFieldsDef.put(1, alphaComponentDef);
        subFieldsDef.put(2, alphaComponentDef);

        CompositeDef compositeDef = new CompositeDef(subFieldsDef);

        ByteBuffer buf = ByteBuffer.allocate(32);

        Map<Integer, Object> values = new HashMap<>();
        values.put(1, "1");
        values.put(2, "2");

        compositeDef.encode(buf, values);

        assertThat(buf.position(), is(2));
        byte[] encoded = new byte[buf.position()];
        buf.clear();
        buf.get(encoded);
        assertThat(new String(encoded, ISO_8859_1), is("12"));
    }

    @Test(expected = CodecException.class)
    public void testCodecMismatch() {
        SortedMap<Integer, ComponentDef> subComponentDefs = new TreeMap<>();
        subComponentDefs.put(1, new ComponentDef(new AlphaCodec(false)));
        CompositeDef codec = new CompositeDef(subComponentDefs);

        Map<Integer, Object> values = new HashMap<>();
        values.put(1, 5);

        ByteBuffer buf = ByteBuffer.allocate(32);
        codec.encode(buf, values);
    }

    @Test(expected = CodecException.class)
    public void testMissingMandatorySubComponent() {
        SortedMap<Integer, ComponentDef> subComponentDefs = new TreeMap<>();
        subComponentDefs.put(1, new ComponentDef(new AlphaCodec(false), false));
        subComponentDefs.put(2, new ComponentDef(new AlphaCodec(false), true));

        CompositeDef codec = new CompositeDef(subComponentDefs);

        Map<Integer, Object> values = new HashMap<>();
        values.put(1, "a");

        ByteBuffer buf = ByteBuffer.allocate(32);
        codec.encode(buf, values);
    }

    @Test
    public void testEncodeCompositeSubField() {
        AlphaCodec alphaCodec = spy(new AlphaCodec(false));
        ComponentDef componentDef = new ComponentDef(alphaCodec);
        SortedMap<Integer, ComponentDef> level2Defs = new TreeMap<>();
        level2Defs.put(1, componentDef);
        level2Defs.put(2, componentDef);

        CompositeDef compositeDefLevel2 = spy(new CompositeDef(level2Defs));

        SortedMap<Integer, ComponentDef> level1Defs = new TreeMap<>();
        level1Defs.put(1, componentDef);
        level1Defs.put(2, compositeDefLevel2);
        level1Defs.put(3, componentDef);

        CompositeDef compositeDef = new CompositeDef(level1Defs);

        ByteBuffer buf = ByteBuffer.allocate(32);

        Map<Integer, Object> level2Values = new HashMap<>();
        level2Values.put(1, "21");
        level2Values.put(2, "22");

        Map<Integer, Object> level1Values = new HashMap<>();
        level1Values.put(1, "1");
        level1Values.put(2, level2Values);
        level1Values.put(3, "3");

        compositeDef.encode(buf, level1Values);

        InOrder inOrder = inOrder(alphaCodec, compositeDefLevel2);
        inOrder.verify(alphaCodec).encode(buf, "1");
        inOrder.verify(compositeDefLevel2).encode(buf, level2Values);
        inOrder.verify(alphaCodec).encode(buf, "21");
        inOrder.verify(alphaCodec).encode(buf, "22");
        inOrder.verify(alphaCodec).encode(buf, "3");

        assertThat(buf.position(), is(6));

        byte[] result = new byte[6];
        buf.clear();
        buf.get(result);

        byte[] expected = "121223".getBytes(ISO_8859_1);

        assertThat(result, is(expected));
    }

    @Test(expected = ConfigException.class)
    public void testEmptySubComponentDefs() {
        new CompositeDef(new TreeMap<Integer, ComponentDef>());
    }

    @Test
    public void testGetters() {
        BitmapCodec bitmapCodec = mock(BitmapCodec.class);
        SortedMap<Integer, ComponentDef> subComponentDefs = new TreeMap<>();
        subComponentDefs.put(2, mock(ComponentDef.class));

        CompositeDef compositeDef = new CompositeDef(subComponentDefs, bitmapCodec);

        assertThat(compositeDef.getEncoding(), is(Encoding.BINARY));
        assertThat(compositeDef.getBitmapCodec(), is(bitmapCodec));
        assertThat(compositeDef.getSubComponentDefs(), is(subComponentDefs));
    }

    @Test(expected = CodecException.class)
    public void testErrorEncodingBitmap() {
        ByteBuffer buf = mock(ByteBuffer.class);
        BitmapCodec bitmapCodec = mock(BitmapCodec.class);
        doThrow(RuntimeException.class).when(bitmapCodec).encode(any(ByteBuffer.class), any(Set.class));
        SortedMap<Integer, ComponentDef> subComponentDefs = new TreeMap<>();
        subComponentDefs.put(2, mock(ComponentDef.class));

        CompositeDef compositeDef = new CompositeDef(subComponentDefs, bitmapCodec);

        compositeDef.encode(buf, new HashMap<Integer, Object>());
    }

    @Test(expected = CodecException.class)
    public void testUnexpectedFields() {
        Codec<String> alphaCodec = new AlphaCodec(false);
        ComponentDef alphaComponentDef = new ComponentDef(alphaCodec);

        SortedMap<Integer, ComponentDef> subFieldsDef = new TreeMap<>();
        subFieldsDef.put(1, alphaComponentDef);
        subFieldsDef.put(2, alphaComponentDef);

        CompositeDef compositeDef = new CompositeDef(subFieldsDef);

        ByteBuffer buf = ByteBuffer.allocate(32);

        Map<Integer, Object> values = new HashMap<>();
        values.put(1, "1");
        values.put(2, "2");
        values.put(3, "3");

        compositeDef.encode(buf, values);
    }


    @Test
    @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "EqualsWithItself", "ObjectEqualsNull"})
    public void testEqualsAndHashCode() {
        SortedMap<Integer, ComponentDef> subComponentDefs1 = mock(SortedMap.class);
        when(subComponentDefs1.size()).thenReturn(1);
        SortedMap<Integer, ComponentDef> subComponentDefs2 = mock(SortedMap.class);
        when(subComponentDefs2.size()).thenReturn(1);

        BitmapCodec bitmapCodec = mock(BitmapCodec.class);

        Codec<Number> tagCodec = mock(Codec.class);
        Codec<Number> lengthCodec = mock(Codec.class);


        CompositeDef compositeDef1 = new CompositeDef(subComponentDefs1, bitmapCodec, tagCodec, lengthCodec, true);

        CompositeDef compositeDef2 = new CompositeDef(subComponentDefs1, bitmapCodec, tagCodec, lengthCodec, true);

        CompositeDef compositeDef3 = new CompositeDef(subComponentDefs2, bitmapCodec, tagCodec, lengthCodec, true);


        assertTrue(!compositeDef1.equals(null));
        assertTrue(!compositeDef1.equals("a"));
        assertTrue(compositeDef1.equals(compositeDef1));
        assertTrue(compositeDef1.equals(compositeDef2));
        assertEquals(compositeDef1.hashCode(), compositeDef2.hashCode());
        assertTrue(!compositeDef1.equals(compositeDef3));
        assertNotEquals(compositeDef1.hashCode(), compositeDef3.hashCode());
        assertTrue(!compositeDef2.equals(compositeDef3));
        assertNotEquals(compositeDef2.hashCode(), compositeDef3.hashCode());
    }

}
