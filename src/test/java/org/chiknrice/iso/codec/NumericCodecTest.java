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
import org.chiknrice.iso.ConfigException;
import org.chiknrice.iso.config.ComponentDef.Encoding;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class NumericCodecTest {

    @Test
    public void testEncodeChar() {
        NumericCodec codec = new NumericCodec(Encoding.CHAR);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, 1234);
        String encoded = new String(buf.array(), 0, 4, StandardCharsets.ISO_8859_1);
        assertThat(encoded, is("1234"));
    }

    @Test
    public void testEncodeFixedLengthChar() {
        NumericCodec codec = new NumericCodec(Encoding.CHAR, 9);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, 1234);
        String encoded = new String(buf.array(), 0, 9, StandardCharsets.ISO_8859_1);
        assertThat(encoded, is("000001234"));
    }

    @Test
    public void testEncodeBcd() {
        NumericCodec codec = new NumericCodec(Encoding.BCD);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, 12345);
        byte[] encoded = buf.array();
        assertThat(encoded[0], is((byte) 0x01));
        assertThat(encoded[1], is((byte) 0x23));
        assertThat(encoded[2], is((byte) 0x45));
    }

    @Test
    public void testEncodeFixedLengthBcd() {
        NumericCodec codec = new NumericCodec(Encoding.BCD, 9);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, 12345);
        byte[] encoded = buf.array();
        assertThat(encoded[0], is((byte) 0x00));
        assertThat(encoded[1], is((byte) 0x00));
        assertThat(encoded[2], is((byte) 0x01));
        assertThat(encoded[3], is((byte) 0x23));
        assertThat(encoded[4], is((byte) 0x45));
    }

    @Test
    public void testEncodeFixedLengthBinary() {
        NumericCodec codec = new NumericCodec(Encoding.BINARY, 5);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, 12345);
        byte[] encoded = buf.array();
        assertThat(encoded[0], is((byte) 0x00));
        assertThat(encoded[1], is((byte) 0x00));
        assertThat(encoded[2], is((byte) 0x00));
        assertThat(encoded[3], is((byte) 0x30));
        assertThat(encoded[4], is((byte) 0x39));
    }

    @Test(expected = ConfigException.class)
    public void testEncodeExceedBinary() {
        new NumericCodec(Encoding.BINARY, 9);
    }

    @Test
    public void testEncodeMaxLongBinary() {
        NumericCodec codec = new NumericCodec(Encoding.BINARY, 8);
        ByteBuffer buf = ByteBuffer.allocate(20);
        codec.encode(buf, Long.MAX_VALUE);
        byte[] encoded = buf.array();
        assertThat(encoded[0], is((byte) 0x7F));
        assertThat(encoded[1], is((byte) 0xFF));
        assertThat(encoded[2], is((byte) 0xFF));
        assertThat(encoded[3], is((byte) 0xFF));
        assertThat(encoded[4], is((byte) 0xFF));
        assertThat(encoded[5], is((byte) 0xFF));
        assertThat(encoded[6], is((byte) 0xFF));
        assertThat(encoded[7], is((byte) 0xFF));
    }

    @Test(expected = CodecException.class)
    public void testEncodeExceedLongBinary() {
        NumericCodec codec = new NumericCodec(Encoding.BINARY, 8);
        ByteBuffer buf = ByteBuffer.allocate(20);
        BigInteger value = new BigInteger("8000000000000000", 16);
        assertThat(value, is(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(1L))));
        codec.encode(buf, value);
    }

    @Test(expected = ConfigException.class)
    public void testVariableBinary() {
        new NumericCodec(Encoding.BINARY);
    }

    @Test(expected = ConfigException.class)
    public void testNullEncoding() {
        new NumericCodec(null);
    }

    @Test
    public void testDecodeChar() {
        NumericCodec codec = new NumericCodec(Encoding.CHAR);
        byte[] bytes = new byte[]{0x31, 0x32, 0x33};
        Number decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertThat(decoded, is(instanceOf(Integer.class)));
        assertThat(decoded.toString(), is("123"));
    }

    @Test
    public void testDecodeLongChar() {
        NumericCodec codec = new NumericCodec(Encoding.CHAR);
        byte[] bytes = new byte[]{0x31, 0x32, 0x33, 0x31, 0x32, 0x33, 0x31, 0x32, 0x33, 0x31, 0x32, 0x33};
        Number decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertThat(decoded, is(instanceOf(Long.class)));
        assertThat(decoded.toString(), is("123123123123"));
    }

    @Test
    public void testDecodeBigIntegerChar() {
        NumericCodec codec = new NumericCodec(Encoding.CHAR);
        byte[] bytes = new byte[]{0x31, 0x32, 0x33, 0x31, 0x32, 0x33, 0x31, 0x32, 0x33, 0x31, 0x32, 0x33, 0x31, 0x32,
                0x33, 0x31, 0x32, 0x33, 0x31, 0x32, 0x33, 0x31, 0x32, 0x33};
        Number decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertThat(decoded, is(instanceOf(BigInteger.class)));
        assertThat(decoded.toString(), is("123123123123123123123123"));
    }

    @Test
    public void testDecodeFixedLengthChar() {
        NumericCodec codec = new NumericCodec(Encoding.CHAR, 2);
        byte[] bytes = new byte[]{0x31, 0x32, 0x33};
        Number decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertThat(decoded, is(instanceOf(Integer.class)));
        assertThat(decoded.toString(), is("12"));
    }

    @Test
    public void testDecodeInsufficientFixedLengthChar() {
        NumericCodec codec = new NumericCodec(Encoding.CHAR, 4);
        byte[] bytes = new byte[]{0x31, 0x32, 0x33};
        try {
            codec.decode(ByteBuffer.wrap(bytes));
            fail("Failure expected due to not enough bytes in buffer");
        } catch (CodecException e) {
            assertThat(e.getMessage(), is("Expecting 4 bytes, only 3 remaining"));
        }
    }

    @Test
    public void testDecodeBcd() {
        NumericCodec codec = new NumericCodec(Encoding.BCD);
        byte[] bytes = new byte[]{0x05, 0x43, 0x21};
        Number decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertThat(decoded, is(instanceOf(Integer.class)));
        assertThat(decoded.toString(), is("54321"));
    }

    @Test
    public void testDecodeFixedLengthBcd() {
        NumericCodec codec = new NumericCodec(Encoding.BCD, 3);
        byte[] bytes = new byte[]{0x05, 0x43, 0x21};
        Number decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertThat(decoded, is(instanceOf(Integer.class)));
        assertThat(decoded.toString(), is("543"));
    }

    @Test
    public void testDecodeBinary() {
        NumericCodec codec = new NumericCodec(Encoding.BINARY, 3);
        byte[] bytes = new byte[]{0x05, 0x43, 0x21};
        Number decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertThat(decoded, is(instanceOf(Long.class)));
        assertThat(decoded, CoreMatchers.<Number>is(344865L));
    }

    @Test
    public void testDecodeExceedingLongBinary() {
        NumericCodec codec = new NumericCodec(Encoding.BINARY, 8);
        byte[] bytes = new byte[]{0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF};
        Number decoded = codec.decode(ByteBuffer.wrap(bytes));
        assertThat(decoded, is(instanceOf(Long.class)));
        assertThat(decoded, CoreMatchers.<Number>is(Long.MAX_VALUE));

        bytes = new byte[]{(byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        try {
            codec.decode(ByteBuffer.wrap(bytes));
            fail("Failure due to exceeding long type");
        } catch (CodecException e) {
            assertThat(e.getMessage(), is("Value exceeds long type 8000000000000000"));
        }
    }

    @Test
    public void testGetEncoding() {
        NumericCodec codec = new NumericCodec(Encoding.BCD);
        assertThat(codec.getEncoding(), is(Encoding.BCD));
    }

    @Test
    @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "EqualsWithItself", "ObjectEqualsNull"})
    public void testEqualsAndHashCode() {
        NumericCodec codec1 = new NumericCodec(Encoding.CHAR, 5);
        NumericCodec codec2 = new NumericCodec(Encoding.CHAR, 5);
        NumericCodec codec3 = new NumericCodec(Encoding.CHAR, 4);
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
