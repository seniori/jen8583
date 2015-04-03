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

import org.chiknrice.iso.codec.BitmapCodec;
import org.chiknrice.iso.codec.BitmapCodec.Bitmap.Type;
import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
public class BitmapCodecTest {

    @Test
    public void tesEncodeBinary() {
        BitmapCodec codec = new BitmapCodec(Type.BINARY);
        ByteBuffer buf = ByteBuffer.allocate(8);
        Set<Integer> enabledBits = new HashSet<>();
        enabledBits.add(1);
        enabledBits.add(2);
        enabledBits.add(3);
        enabledBits.add(5);
        enabledBits.add(8);
        enabledBits.add(13);
        enabledBits.add(21);
        codec.encode(buf, enabledBits);

        byte[] encoded = buf.array();

        StringBuilder sb = new StringBuilder();
        for (byte b : encoded) {
            sb.append(String.format("%08d", Integer.parseInt(Integer.toBinaryString(b & 0xFF))));
        }
        String expected = "1110100100001000000010000000000000000000000000000000000000000000";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void tesEncodeHex() {
        BitmapCodec codec = new BitmapCodec(Type.HEX);
        ByteBuffer buf = ByteBuffer.allocate(16);
        Set<Integer> enabledBits = new HashSet<>();
        enabledBits.add(1);
        enabledBits.add(2);
        enabledBits.add(3);
        enabledBits.add(5);
        enabledBits.add(8);
        enabledBits.add(13);
        enabledBits.add(21);
        codec.encode(buf, enabledBits);

        byte[] encoded = buf.array();

        StringBuilder sb = new StringBuilder();
        for (byte b : encoded) {
            sb.append(String.format("%04d",
                    Integer.parseInt(Integer.toBinaryString(Integer.parseInt(Character.toString((char) b), 16)))));
        }
        String expected = "1110100100001000000010000000000000000000000000000000000000000000";
        assertEquals(expected, sb.toString());
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
