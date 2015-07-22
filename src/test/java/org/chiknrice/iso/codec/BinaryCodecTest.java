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
import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class BinaryCodecTest {

    @Test
    public void testEncode() {
        BinaryCodec codec = new BinaryCodec();
        byte[] bytes = new byte[]{0x11, 0x22, 0x33, 0x44, 0x55};
        ByteBuffer buf = ByteBuffer.allocate(10);
        codec.encode(buf, bytes);
        byte[] encoded = buf.array();
        for (int i = 0; i < bytes.length; i++) {
            assertThat(encoded[i], is(bytes[i]));
        }
        for (int i = buf.position(); i < encoded.length; i++) {
            assertThat(encoded[i], is((byte) 0x00));
        }
    }

    @Test
    public void testEncodeLessThanFixedLength() {
        BinaryCodec codec = new BinaryCodec(7);
        byte[] bytes = new byte[]{0x11, 0x22, 0x33, 0x44, 0x55};
        ByteBuffer buf = ByteBuffer.allocate(10);
        codec.encode(buf, bytes);
        byte[] encoded = buf.array();
        for (int i = 0; i < bytes.length; i++) {
            assertThat(encoded[i + 2], is(bytes[i]));
        }
        for (int i = buf.position(); i < encoded.length; i++) {
            assertThat(encoded[i], is((byte) 0x00));
        }
    }

    @Test
    public void testEncodeExactFixedLength() {
        BinaryCodec codec = new BinaryCodec(5);
        byte[] bytes = new byte[]{0x11, 0x22, 0x33, 0x44, 0x55};
        ByteBuffer buf = ByteBuffer.allocate(10);
        codec.encode(buf, bytes);
        byte[] encoded = buf.array();
        for (int i = 0; i < bytes.length; i++) {
            assertThat(encoded[i], is(bytes[i]));
        }
        for (int i = buf.position(); i < encoded.length; i++) {
            assertThat(encoded[i], is((byte) 0x00));
        }
    }

    @Test
    public void testEncodeExceedingFixedLength() {
        BinaryCodec codec = new BinaryCodec(3);
        byte[] bytes = new byte[]{0x11, 0x22, 0x33, 0x44, 0x55};
        ByteBuffer buf = ByteBuffer.allocate(10);
        try {
            codec.encode(buf, bytes);
            fail("Encoding should fail due to exceeding fixed length");
        } catch (CodecException e) {
            assertThat(e.getMessage(), is("Bytes exceed fixed length 3"));
        }
    }

    @Test
    public void testDecode() {
        BinaryCodec codec = new BinaryCodec();
        byte[] bytes = new byte[]{0x11, 0x22, 0x33, 0x44, 0x55, 0x00, 0x00};
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        byte[] decoded = codec.decode(buf);
        assertTrue(bytes != decoded);
        assertEquals(bytes.length, decoded.length);
        for (int i = 0; i < decoded.length; i++) {
            assertThat(decoded[i], is(bytes[i]));
        }
    }

    @Test
    public void testDecodeFixedLength() {
        BinaryCodec codec = new BinaryCodec(4);
        byte[] bytes = new byte[]{0x11, 0x22, 0x33, 0x44, 0x55, 0x00, 0x00};
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        byte[] decoded = codec.decode(buf);
        assertTrue(bytes != decoded);
        assertEquals(4, decoded.length);
        for (int i = 0; i < decoded.length; i++) {
            assertThat(decoded[i], is(bytes[i]));
        }
    }

    @Test
    public void testGetEncoding() {
        BinaryCodec codec = new BinaryCodec(4);
        assertThat(codec.getEncoding(), is(Encoding.BINARY));
    }

    @Test
    @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "EqualsWithItself", "ObjectEqualsNull"})
    public void testEqualsAndHashCode() {
        BinaryCodec codec1 = new BinaryCodec(4);
        BinaryCodec codec2 = new BinaryCodec(4);
        BinaryCodec codec3 = new BinaryCodec(3);
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
