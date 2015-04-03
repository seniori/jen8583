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

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.chiknrice.iso.codec.BitmapCodec;
import org.chiknrice.iso.codec.BitmapCodec.Bitmap;
import org.chiknrice.iso.codec.BitmapCodec.Bitmap.Type;
import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
public class BitmapCodecTest {

    @Test
    public void testEncodeBinary() {
        BitmapCodec codec = new BitmapCodec(Type.BINARY);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new TreeSet<>();
        enabledBits.add(2);
        enabledBits.add(3);
        enabledBits.add(5);
        enabledBits.add(8);
        enabledBits.add(13);
        enabledBits.add(21);

        assertEquals(0, buf.position());
        codec.encode(buf, enabledBits);
        assertEquals(8, buf.position());

        byte[] encoded = buf.array();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < buf.position(); i++) {
            sb.append(String.format("%08d", Integer.parseInt(Integer.toBinaryString(encoded[i] & 0xFF))));
        }
        String expected = "0110100100001000000010000000000000000000000000000000000000000000";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testNonTreeSet() {
        BitmapCodec codec = new BitmapCodec(Type.BINARY);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new HashSet<>();
        enabledBits.add(2);
        codec.encode(buf, enabledBits);
    }

    @Test
    public void testEncodeHex() {
        BitmapCodec codec = new BitmapCodec(Type.HEX);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new HashSet<>();
        enabledBits.add(2);
        enabledBits.add(3);
        enabledBits.add(5);
        enabledBits.add(8);
        enabledBits.add(13);
        enabledBits.add(21);

        assertEquals(0, buf.position());
        codec.encode(buf, enabledBits);
        assertEquals(16, buf.position());

        byte[] encoded = buf.array();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < buf.position(); i++) {
            sb.append(String.format("%04d", Integer.parseInt(Integer.toBinaryString(Integer.parseInt(
                    Character.toString((char) encoded[i]), 16)))));
        }
        String expected = "0110100100001000000010000000000000000000000000000000000000000000";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testEncodeExtendedHex() {
        BitmapCodec codec = new BitmapCodec(Type.HEX);
        ByteBuffer buf = ByteBuffer.allocate(40);
        Set<Integer> enabledBits = new HashSet<>();
        enabledBits.add(2);
        enabledBits.add(3);
        enabledBits.add(5);
        enabledBits.add(8);
        enabledBits.add(13);
        enabledBits.add(21);
        enabledBits.add(66);

        assertEquals(0, buf.position());
        codec.encode(buf, enabledBits);
        assertEquals(32, buf.position());

        byte[] encoded = buf.array();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < buf.position(); i++) {
            sb.append(String.format("%04d", Integer.parseInt(Integer.toBinaryString(Integer.parseInt(
                    Character.toString((char) encoded[i]), 16)))));
        }
        String expected = "11101001000010000000100000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testEncodeCompressed() {
        BitmapCodec codec = new BitmapCodec(Type.COMPRESSED);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new TreeSet<>();
        enabledBits.add(4);
        enabledBits.add(8);
        enabledBits.add(13);
        enabledBits.add(16);
        enabledBits.add(21);
        enabledBits.add(27);
        enabledBits.add(36);

        assertEquals(0, buf.position());
        codec.encode(buf, enabledBits);
        assertEquals(5, buf.position());

        byte[] encoded = buf.array();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < buf.position(); i++) {
            sb.append(String.format("%08d", Integer.parseInt(Integer.toBinaryString(encoded[i] & 0xFF))));
        }
        String expected = "1001000100001001100010001010000000010000";
        assertEquals(expected, sb.toString());
    }

    @Test(expected = CodecException.class)
    public void testMidExtensionBitSet() {
        BitmapCodec codec = new BitmapCodec(Type.COMPRESSED);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new TreeSet<>();
        enabledBits.add(25);
        enabledBits.add(36);
        codec.encode(buf, enabledBits);
    }

    @Test(expected = CodecException.class)
    public void testLastExtensionBitSet() {
        BitmapCodec codec = new BitmapCodec(Type.COMPRESSED);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new TreeSet<>();
        enabledBits.add(33);
        enabledBits.add(36);
        codec.encode(buf, enabledBits);
    }

    @Test
    public void decodeBinary() {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) Integer.parseInt("01101001", 2);
        bytes[1] = (byte) Integer.parseInt("00001000", 2);
        bytes[2] = (byte) Integer.parseInt("00001000", 2);

        BitmapCodec codec = new BitmapCodec(Type.BINARY);
        Bitmap bitmap = codec.decode(ByteBuffer.wrap(bytes));

        for (int i = 1; i <= 128; i++) {
            switch (i) {
            case 2:
            case 3:
            case 5:
            case 8:
            case 13:
            case 21:
                assertTrue(String.format("Expected set bit %d unset", i), bitmap.isSet(i));
                break;
            default:
                assertTrue(String.format("Unexpected bit set %d", i), !bitmap.isSet(i));
            }
        }
    }

    @Test
    public void decodeExtendedBinary() {
        byte[] bytes = new byte[16];
        bytes[0] = (byte) Integer.parseInt("11101001", 2);
        bytes[1] = (byte) Integer.parseInt("00001000", 2);
        bytes[2] = (byte) Integer.parseInt("00001000", 2);
        bytes[8] = (byte) Integer.parseInt("01000000", 2);

        BitmapCodec codec = new BitmapCodec(Type.BINARY);
        Bitmap bitmap = codec.decode(ByteBuffer.wrap(bytes));

        for (int i = 1; i <= 128; i++) {
            switch (i) {
            case 1:
            case 2:
            case 3:
            case 5:
            case 8:
            case 13:
            case 21:
            case 66:
                assertTrue(String.format("Expected set bit %d unset", i), bitmap.isSet(i));
                break;
            default:
                assertTrue(String.format("Unexpected bit set %d", i), !bitmap.isSet(i));
            }
        }
    }

    @Test(expected = ConfigException.class)
    public void testNullEncoding() {
        new BitmapCodec(null);
    }

    @Test
    public void testEqualsAndHashCode() {
        BitmapCodec codec1 = new BitmapCodec(Type.BINARY);
        BitmapCodec codec2 = new BitmapCodec(Type.BINARY);
        BitmapCodec codec3 = new BitmapCodec(Type.HEX);
        assertTrue(!codec1.equals(null));
        assertTrue(!codec1.equals("a"));
        assertTrue(codec1.equals(codec1));
        assertTrue(codec1.equals(codec2));
        assertEquals(codec1.hashCode(), codec2.hashCode());
        assertTrue(!codec1.equals(codec3));
        assertNotEquals(codec1.hashCode(), codec3.hashCode());
        assertTrue(!codec2.equals(codec3));
        assertNotEquals(codec2.hashCode(), codec3.hashCode());
    }

}
