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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.chiknrice.iso.codec.AlphaCodec;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public class TestAlphaCodec extends BaseTest {

    @Test
    public void testSimpleEncode() {
        AlphaCodec codec = new AlphaCodec(StandardCharsets.ISO_8859_1, false);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, "abc");
        byte[] bytes = buf.array();
        String encoded = new String(bytes, 0, 3, StandardCharsets.ISO_8859_1);
        assertEquals("abc", encoded);
    }

    @Test
    public void testEncodeSpecialChar() {
        AlphaCodec codec = new AlphaCodec(StandardCharsets.UTF_8, false);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, "Φ");
        byte[] bytes = buf.array();
        String encoded = new String(bytes, 0, 2, StandardCharsets.UTF_8);
        assertEquals("Φ", encoded);
    }

    @Test
    public void testEncodeSpecialCharPadded() {
        AlphaCodec codec = new AlphaCodec(StandardCharsets.UTF_8, false, true, 3);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, "Φ");
        byte[] bytes = buf.array();
        String encoded = new String(bytes, 0, 4, StandardCharsets.UTF_8);
        assertEquals("Φ  ", encoded);
    }

    @Test(expected = RuntimeException.class)
    public void testInsufficientConstructorArgs() {
        new AlphaCodec(StandardCharsets.UTF_8, true, null, 9);
    }

    @Test
    public void testPadding() {
        AlphaCodec codec = new AlphaCodec(StandardCharsets.UTF_8, true, false, 9);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, "abc");
        byte[] bytes = buf.array();
        String encoded = new String(bytes, 0, 9, StandardCharsets.UTF_8);
        assertEquals("      abc", encoded);
    }

    @Test
    public void testJustified() {
        AlphaCodec codec = new AlphaCodec(StandardCharsets.UTF_8, true, true, 9);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, "abc");
        byte[] bytes = buf.array();
        String encoded = new String(bytes, 0, 9, StandardCharsets.UTF_8);
        assertEquals("abc      ", encoded);
    }

    @Test
    public void testDecode() {
        AlphaCodec codec = new AlphaCodec(StandardCharsets.ISO_8859_1, false);
        byte[] bytes = new byte[] { 0x31, 0x32, 0x33 };
        String decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertEquals("123", decoded);
    }

    @Test
    public void testDecodeSpecialChar() {
        AlphaCodec codec = new AlphaCodec(StandardCharsets.UTF_8, false);
        byte[] bytes = new byte[] { (byte) 0xce, (byte) 0xa6 };
        String decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertEquals("Φ", decoded);
    }

    @Test
    public void testDecodeSpecialCharNoTrim() {
        AlphaCodec codec = new AlphaCodec(StandardCharsets.UTF_8, false);
        byte[] bytes = new byte[] { 0x20, (byte) 0xce, (byte) 0xa6, 0x20, 0x20, 0x20 };
        String decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertEquals(" Φ   ", decoded);
    }

    @Test
    public void testDecodeSpecialCharTrim() {
        AlphaCodec codec = new AlphaCodec(StandardCharsets.UTF_8, true);
        byte[] bytes = new byte[] { 0x20, (byte) 0xce, (byte) 0xa6, 0x20, 0x20, 0x20 };
        String decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertEquals("Φ", decoded);
    }

    @Test
    @Ignore
    // TODO: this cannot really be decoded properly as the fixed length is pertaining to number of bytes and not number
    // of characters
    public void testDecodeSpecialCharFixedLength() {
        AlphaCodec codec = new AlphaCodec(StandardCharsets.UTF_8, false, false, 3);
        byte[] bytes = new byte[] { 0x20, (byte) 0xce, (byte) 0xa6, 0x20, 0x20, 0x20 };
        String decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertEquals(" Φ ", decoded);
    }

}
