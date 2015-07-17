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
package org.chiknrice.iso.codec;

import org.chiknrice.iso.CodecException;
import org.chiknrice.iso.codec.BitmapCodec.Bitmap;
import org.chiknrice.iso.codec.BitmapCodec.Bitmap.Type;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
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
    public void testEncodeBinaryExtended() {
        BitmapCodec codec = new BitmapCodec(Type.BINARY);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new TreeSet<>();
        enabledBits.add(2);
        enabledBits.add(3);
        enabledBits.add(5);
        enabledBits.add(8);
        enabledBits.add(13);
        enabledBits.add(21);
        enabledBits.add(66);

        assertEquals(0, buf.position());
        codec.encode(buf, enabledBits);
        assertEquals(16, buf.position());

        byte[] encoded = buf.array();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < buf.position(); i++) {
            sb.append(String.format("%08d", Integer.parseInt(Integer.toBinaryString(encoded[i] & 0xFF))));
        }
        String expected = "11101001000010000000100000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testNonTreeSet() {
        BitmapCodec codec = new BitmapCodec(Type.BINARY);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new HashSet<>();
        enabledBits.add(2);
        assertEquals(0, buf.position());
        codec.encode(buf, enabledBits);
        assertEquals(8, buf.position());

        byte[] encoded = buf.array();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < buf.position(); i++) {
            sb.append(String.format("%08d", Integer.parseInt(Integer.toBinaryString(encoded[i] & 0xFF))));
        }
        String expected = "0100000000000000000000000000000000000000000000000000000000000000";
        assertEquals(expected, sb.toString());
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

        assertEquals(0, buf.position());
        codec.encode(buf, enabledBits);
        assertEquals(2, buf.position());

        byte[] encoded = buf.array();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < buf.position(); i++) {
            sb.append(String.format("%08d", Integer.parseInt(Integer.toBinaryString(encoded[i] & 0xFF))));
        }
        String expected = "0001000100001001";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testEncodeCompressedSecondary() {
        BitmapCodec codec = new BitmapCodec(Type.COMPRESSED);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new TreeSet<>();
        enabledBits.add(4);
        enabledBits.add(8);
        enabledBits.add(13);
        enabledBits.add(16);
        enabledBits.add(21);

        assertEquals(0, buf.position());
        codec.encode(buf, enabledBits);
        assertEquals(3, buf.position());

        byte[] encoded = buf.array();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < buf.position(); i++) {
            sb.append(String.format("%08d", Integer.parseInt(Integer.toBinaryString(encoded[i] & 0xFF))));
        }
        String expected = "100100010000100100001000";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testEncodeCompressedFourthExtension() {
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
    public void testCompressedMidExtensionBitSet() {
        BitmapCodec codec = new BitmapCodec(Type.COMPRESSED);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new TreeSet<>();
        enabledBits.add(25);
        enabledBits.add(36);
        codec.encode(buf, enabledBits);
    }

    @Test(expected = CodecException.class)
    public void testCompressedLastExtensionBitSet() {
        BitmapCodec codec = new BitmapCodec(Type.COMPRESSED);
        ByteBuffer buf = ByteBuffer.allocate(20);
        Set<Integer> enabledBits = new TreeSet<>();
        enabledBits.add(33);
        enabledBits.add(36);
        codec.encode(buf, enabledBits);
    }

    @Test
    public void testDecodeBinary() {
        // 8 bytes for primary bitmap
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
    public void testDecodeBinaryExtended() {
        // 16 bytes for primary + secondary bitmap
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

    @Test
    public void testDecodeHexAllCaps() {
        // 16 bytes for primary bitmap (16 hex characters for an 8 byte bitmap)

        // [0] 01101001 = 0x69
        // [1] 00001000 = 0x08
        // [2] 00001000 = 0x08
        // [3] 10101011 = 0xAB
        byte[] bytes = "690808AB00000000".getBytes(StandardCharsets.ISO_8859_1);

        BitmapCodec codec = new BitmapCodec(Type.HEX);
        Bitmap bitmap = codec.decode(ByteBuffer.wrap(bytes));

        for (int i = 1; i <= 128; i++) {
            switch (i) {
                case 2:
                case 3:
                case 5:
                case 8:
                case 13:
                case 21:
                case 25:
                case 27:
                case 29:
                case 31:
                case 32:
                    assertTrue(String.format("Expected set bit %d unset", i), bitmap.isSet(i));
                    break;
                default:
                    assertTrue(String.format("Unexpected bit set %d", i), !bitmap.isSet(i));
            }
        }
    }

    @Test
    public void testDecodeHexLowerCase() {
        // 16 bytes for primary bitmap (16 hex characters for an 8 byte bitmap)

        // [0] 01101001 = 0x69
        // [1] 00001000 = 0x08
        // [2] 00001000 = 0x08
        // [3] 10101011 = 0xab
        byte[] bytes = "690808ab00000000".getBytes(StandardCharsets.ISO_8859_1);

        BitmapCodec codec = new BitmapCodec(Type.HEX);
        Bitmap bitmap = codec.decode(ByteBuffer.wrap(bytes));

        for (int i = 1; i <= 128; i++) {
            switch (i) {
                case 2:
                case 3:
                case 5:
                case 8:
                case 13:
                case 21:
                case 25:
                case 27:
                case 29:
                case 31:
                case 32:
                    assertTrue(String.format("Expected set bit %d unset", i), bitmap.isSet(i));
                    break;
                default:
                    assertTrue(String.format("Unexpected bit set %d", i), !bitmap.isSet(i));
            }
        }
    }

    @Test
    public void testDecodeHexAllCapsExtended() {
        // 16 bytes for primary bitmap (16 hex characters for an 8 byte bitmap)

        // [0] 11101001 = 0xE9
        // [1] 00001000 = 0x08
        // [2] 00001000 = 0x08
        // [7] 00000001 = 0x01
        byte[] bytes = "E90808AB000000000000000000000001".getBytes(StandardCharsets.ISO_8859_1);

        BitmapCodec codec = new BitmapCodec(Type.HEX);
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
                case 25:
                case 27:
                case 29:
                case 31:
                case 32:
                case 128:
                    assertTrue(String.format("Expected set bit %d unset", i), bitmap.isSet(i));
                    break;
                default:
                    assertTrue(String.format("Unexpected bit set %d", i), !bitmap.isSet(i));
            }
        }
    }

    @Test
    public void testDecodeHexLowerCaseExtended() {
        // 16 bytes for primary bitmap (16 hex characters for an 8 byte bitmap)

        // [0] 11101001 = 0xe9
        // [1] 00001000 = 0x08
        // [2] 00001000 = 0x08
        // [7] 00000001 = 0x01
        byte[] bytes = "e90808AB000000000000000000000001".getBytes(StandardCharsets.ISO_8859_1);

        BitmapCodec codec = new BitmapCodec(Type.HEX);
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
                case 25:
                case 27:
                case 29:
                case 31:
                case 32:
                case 128:
                    assertTrue(String.format("Expected set bit %d unset", i), bitmap.isSet(i));
                    break;
                default:
                    assertTrue(String.format("Unexpected bit set %d", i), !bitmap.isSet(i));
            }
        }
    }

    @Test
    public void testDecodeCompressed() {
        // 8 bytes for primary bitmap
        byte[] bytes = new byte[8];
        bytes[0] = (byte) Integer.parseInt("01101001", 2);
        bytes[1] = (byte) Integer.parseInt("00001000", 2);

        BitmapCodec codec = new BitmapCodec(Type.COMPRESSED);
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        Bitmap bitmap = codec.decode(buf);
        assertEquals(2, buf.position());

        for (int i = 1; i <= 128; i++) {
            switch (i) {
                case 2:
                case 3:
                case 5:
                case 8:
                case 13:
                    assertTrue(String.format("Expected set bit %d unset", i), bitmap.isSet(i));
                    break;
                default:
                    assertTrue(String.format("Unexpected bit set %d", i), !bitmap.isSet(i));
            }
        }
    }

    @Test
    public void testDecodeCompressedSecondary() {
        // 8 bytes for primary bitmap
        byte[] bytes = new byte[8];
        bytes[0] = (byte) Integer.parseInt("11101001", 2);
        bytes[1] = (byte) Integer.parseInt("00001000", 2);
        bytes[2] = (byte) Integer.parseInt("00001000", 2);

        BitmapCodec codec = new BitmapCodec(Type.COMPRESSED);
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        Bitmap bitmap = codec.decode(buf);
        assertEquals(3, buf.position());

        for (int i = 1; i <= 128; i++) {
            switch (i) {
                case 1: // extension bit
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
    public void testDecodeCompressedFourthExtension() {
        // 8 bytes for primary bitmap
        byte[] bytes = new byte[8];
        bytes[0] = (byte) Integer.parseInt("11101001", 2);
        bytes[1] = (byte) Integer.parseInt("00001000", 2);
        bytes[2] = (byte) Integer.parseInt("10001000", 2);
        bytes[3] = (byte) Integer.parseInt("10000010", 2);
        bytes[4] = (byte) Integer.parseInt("00100000", 2);

        BitmapCodec codec = new BitmapCodec(Type.COMPRESSED);
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        Bitmap bitmap = codec.decode(buf);
        assertEquals(5, buf.position());

        for (int i = 1; i <= 128; i++) {
            switch (i) {
                case 1: // extension bit
                case 2:
                case 3:
                case 5:
                case 8:
                case 13:
                case 17: // extension bit
                case 21:
                case 25: // extension bit
                case 31:
                case 35:
                    assertTrue(String.format("Expected set bit %d unset", i), bitmap.isSet(i));
                    break;
                default:
                    assertTrue(String.format("Unexpected bit set %d", i), !bitmap.isSet(i));
            }
        }
    }

    @Test
    @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "EqualsWithItself", "ObjectEqualsNull"})
    public void testBitmapEqualsAndHashCode() {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) Integer.parseInt("01101001", 2);
        bytes[1] = (byte) Integer.parseInt("00001000", 2);
        bytes[2] = (byte) Integer.parseInt("00001000", 2);

        BitmapCodec codec = new BitmapCodec(Type.BINARY);
        Bitmap bitmap1 = codec.decode(ByteBuffer.wrap(bytes));
        Bitmap bitmap2 = codec.decode(ByteBuffer.wrap(bytes));

        bytes[2] = 0x00;
        Bitmap bitmap3 = codec.decode(ByteBuffer.wrap(bytes));

        assertTrue(!bitmap1.equals(null));
        assertTrue(!bitmap1.equals("a"));
        assertTrue(bitmap1.equals(bitmap1));
        assertTrue(bitmap1.equals(bitmap2));
        assertTrue(!bitmap1.equals(bitmap3));
        assertEquals(bitmap1.hashCode(), bitmap2.hashCode());
        assertNotEquals(bitmap1.hashCode(), bitmap3.hashCode());
    }

    @Test
    public void testBitmapToString() {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) Integer.parseInt("01101001", 2);
        bytes[1] = (byte) Integer.parseInt("00001000", 2);
        bytes[2] = (byte) Integer.parseInt("00001000", 2);

        BitmapCodec codec = new BitmapCodec(Type.BINARY);
        Bitmap bitmap = codec.decode(ByteBuffer.wrap(bytes));

        String expected = "[2, 3, 5, 8, 13, 21]";

        assertEquals(expected, bitmap.toString());
    }

    @Test
    public void testBitmapSetBitUnsetBit() {
        byte[] bytes = new byte[16];
        bytes[0] = (byte) Integer.parseInt("11000000", 2);
        bytes[8] = (byte) Integer.parseInt("01000000", 2);

        BitmapCodec codec = new BitmapCodec(Type.BINARY);
        Bitmap bitmap = codec.decode(ByteBuffer.wrap(bytes));

        assertTrue(bitmap.isSet(2));
        assertTrue(bitmap.isSet(66));
        assertTrue(!bitmap.isSet(3));
        assertTrue(!bitmap.isSet(67));

        bitmap.set(3);
        bitmap.set(67);
        assertTrue(bitmap.isSet(2));
        assertTrue(bitmap.isSet(66));
        assertTrue(bitmap.isSet(3));
        assertTrue(bitmap.isSet(67));

        bitmap.unSet(2);
        bitmap.unSet(66);
        assertTrue(!bitmap.isSet(2));
        assertTrue(!bitmap.isSet(66));
        assertTrue(bitmap.isSet(3));
        assertTrue(bitmap.isSet(67));
    }

    @Test
    @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "EqualsWithItself", "ObjectEqualsNull"})
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
