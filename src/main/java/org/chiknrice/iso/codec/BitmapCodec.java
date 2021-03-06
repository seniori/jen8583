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
import org.chiknrice.iso.codec.BitmapCodec.Bitmap.Type;
import org.chiknrice.iso.util.EqualsBuilder;
import org.chiknrice.iso.util.Hash;
import org.chiknrice.iso.util.Hex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class BitmapCodec {

    private final Type type;

    public BitmapCodec(Type type) {
        this.type = type;
    }

    /**
     * @param buf
     * @return the decoded bitmap
     */
    public Bitmap decode(ByteBuffer buf) {
        byte[] bytes;
        if (Type.BINARY.equals(type)) {
            buf.mark();
            if ((buf.get() & 0x80) == 0) {
                bytes = new byte[8];
            } else {
                bytes = new byte[16];
            }
            buf.reset();
            buf.get(bytes);
        } else if (Type.HEX.equals(type)) {
            buf.mark();
            if ((Hex.value((char) buf.get()) & 0x8) == 0) {
                bytes = new byte[16];
            } else {
                bytes = new byte[32];
            }
            buf.reset();
            buf.get(bytes);
            bytes = Hex.decode(new String(bytes, StandardCharsets.ISO_8859_1));
        } else {
            buf.mark();
            int total = 0;
            bytes = new byte[2];
            boolean hasNext;
            do {
                buf.get(bytes);
                total += bytes.length;
                hasNext = (bytes[0] & 0x80) > 0;
                bytes = new byte[1];
            } while (hasNext);
            buf.reset();
            bytes = new byte[total];
            buf.get(bytes);
        }
        return new Bitmap(bytes, type);
    }

    /**
     * @param buf
     * @param bitsParam
     */
    public void encode(ByteBuffer buf, Set<Integer> bitsParam) {
        TreeSet<Integer> bits;
        if (bitsParam instanceof TreeSet) {
            bits = (TreeSet<Integer>) bitsParam;
        } else {
            bits = new TreeSet<>(bitsParam);
        }
        if (Type.COMPRESSED.equals(type)) {
            writeBytes(buf, bits, new byte[2], 1, false);
        } else {
            writeBytes(buf, bits, new byte[8], 8, Type.HEX.equals(type));
        }
    }

    private static void writeBytes(ByteBuffer buf, TreeSet<Integer> bits, byte[] primaryBitmap, int extendedSize, boolean hex) {
        int offset = 0;
        byte[] bytes = primaryBitmap;
        for (Integer bit : bits) {
            int byteIndex = byteIndex(bit);
            while (byteIndex >= (bytes.length + offset)) {
                if ((bytes[0] & 0x80) > 0) {
                    throw new CodecException("Extension bit should not be set");
                }
                bytes[0] |= 0x80;
                buf.put(hex ? Hex.encode(bytes).getBytes(StandardCharsets.ISO_8859_1) : bytes);
                offset += bytes.length;
                bytes = new byte[extendedSize];
            }
            bytes[byteIndex - offset] |= mask(bit);
        }
        if ((bytes[0] & 0x80) > 0) {
            throw new CodecException("Extension bit should not be set");
        }
        buf.put(hex ? Hex.encode(bytes).getBytes(StandardCharsets.ISO_8859_1) : bytes);
    }

    private static int mask(int bit) {
        return 128 >> ((bit - 1) % 8);
    }

    private static int byteIndex(int bit) {
        return (bit - 1) / 8;
    }

    @Override
    public int hashCode() {
        return Hash.build(this, type);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o.getClass() != getClass()) {
            return false;
        } else {
            BitmapCodec other = (BitmapCodec) o;
            return EqualsBuilder.newInstance(other.type, type).isEqual();
        }
    }

    public static class Bitmap {

        public enum Type {
            BINARY, HEX, COMPRESSED
        }

        private final byte[] bytes;
        private final Type type;

        private Bitmap(byte[] bytes, Type type) {
            this.bytes = bytes;
            this.type = type;
        }

        public boolean isSet(int bit) {
            int byteIndex = byteIndex(bit);
            return byteIndex < bytes.length && (bytes[byteIndex] & mask(bit)) > 0;
        }

        public boolean isControlBit(int bit) {
            boolean controlBit = false;
            switch (type) {
                case HEX:
                case BINARY:
                    if (bit == 1) {
                        controlBit = true;
                    }
                    break;
                case COMPRESSED:
                    if (bit == 1 || bit == 17 || bit == 25) {
                        controlBit = true;
                    }
                    break;
                default:
            }
            return controlBit;
        }

        public void set(int bit) {
            bytes[byteIndex(bit)] = (byte) (bytes[byteIndex(bit)] | mask(bit));
        }

        public void unSet(int bit) {
            bytes[byteIndex(bit)] = (byte) (bytes[byteIndex(bit)] & (mask(bit) ^ 0xFF));
        }

        @Override
        public String toString() {
            Set<Integer> setBits = new TreeSet<>();
            int bits = bytes.length * 8;
            for (int i = 1; i <= bits; i++) {
                if (isSet(i)) {
                    setBits.add(i);
                }
            }
            return setBits.toString();
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes) ^ type.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o == this) {
                return true;
            } else if (o.getClass() != getClass()) {
                return false;
            } else {
                Bitmap other = (Bitmap) o;
                return Arrays.equals(bytes, other.bytes) && type.equals(other.type);
            }
        }
    }

}
