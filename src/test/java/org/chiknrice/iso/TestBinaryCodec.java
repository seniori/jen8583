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
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.chiknrice.iso.codec.BinaryCodec;
import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
public class TestBinaryCodec extends BaseTest {

    @Test
    public void testEncode() {
        BinaryCodec codec = new BinaryCodec();
        byte[] bytes = new byte[] { 0x11, 0x22, 0x33, 0x44, 0x55 };
        ByteBuffer buf = ByteBuffer.allocate(10);
        codec.encode(buf, bytes);
        byte[] encoded = buf.array();
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], encoded[i]);
        }
        for (int i = buf.position(); i < encoded.length; i++) {
            assertEquals(0x00, encoded[i]);
        }
    }

    @Test
    public void testEncodeFixedLength() {
        BinaryCodec codec = new BinaryCodec(7);
        byte[] bytes = new byte[] { 0x11, 0x22, 0x33, 0x44, 0x55 };
        ByteBuffer buf = ByteBuffer.allocate(10);
        codec.encode(buf, bytes);
        byte[] encoded = buf.array();
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], encoded[i + 2]);
        }
        for (int i = buf.position(); i < encoded.length; i++) {
            assertEquals(0x00, encoded[i]);
        }
    }

    @Test
    public void testDecode() {
        BinaryCodec codec = new BinaryCodec();
        byte[] bytes = new byte[] { 0x11, 0x22, 0x33, 0x44, 0x55, 0x00, 0x00 };
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        byte[] decoded = codec.decode(buf);
        assertTrue(bytes != decoded);
        assertEquals(bytes.length, decoded.length);
        for (int i = 0; i < decoded.length; i++) {
            assertEquals(decoded[i], bytes[i]);
        }
    }

    @Test
    public void testDecodeFixed() {
        BinaryCodec codec = new BinaryCodec(4);
        byte[] bytes = new byte[] { 0x11, 0x22, 0x33, 0x44, 0x55, 0x00, 0x00 };
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        byte[] decoded = codec.decode(buf);
        assertTrue(bytes != decoded);
        assertEquals(4, decoded.length);
        for (int i = 0; i < decoded.length; i++) {
            assertEquals(decoded[i], bytes[i]);
        }
    }

    @Test(expected = CodecException.class)
    public void testEncodeExceedingFixedLength() {
        BinaryCodec codec = new BinaryCodec(3);
        byte[] bytes = new byte[] { 0x11, 0x22, 0x33, 0x44, 0x55 };
        ByteBuffer buf = ByteBuffer.allocate(10);
        codec.encode(buf, bytes);
    }

}
