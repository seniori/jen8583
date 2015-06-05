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
import org.chiknrice.iso.util.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class NumericCodec implements Codec<Number> {

    private final Encoding encoding;
    private final Integer fixedLength;
    private final boolean supportsBigInteger;

    public NumericCodec(Encoding encoding) {
        this(encoding, null);
    }

    public NumericCodec(Encoding encoding, Integer fixedLength) {
        this.encoding = encoding;
        this.fixedLength = fixedLength;
        if (Encoding.BINARY.equals(encoding)) {
            if (fixedLength == null) {
                throw new ConfigException(String.format(
                        "Variable length numeric field encoded in %s could exceed long type", encoding));
            } else if (fixedLength > 8) {
                throw new ConfigException(String.format(
                        "Numeric field encoded in %s with %d bytes won't fit long type", encoding, fixedLength));
            }
            supportsBigInteger = false;
        } else if (Encoding.CHAR.equals(encoding) || Encoding.BCD.equals(encoding)) {
            supportsBigInteger = true;
        } else {
            throw new ConfigException(String.format("Unsupported encoding %s", encoding));
        }
    }

    public Number decode(ByteBuffer buf) {
        int bytesToDecode;
        if (fixedLength != null) {
            if (buf.remaining() < fixedLength) {
                throw new CodecException(String.format("Expecting %d bytes, only %d remaining", fixedLength,
                        buf.remaining()));
            } else if (Encoding.BCD == encoding) {
                bytesToDecode = fixedLength / 2 + fixedLength % 2;
            } else {
                bytesToDecode = fixedLength;
            }
        } else {
            bytesToDecode = buf.limit() - buf.position();
        }

        byte[] bytes = new byte[bytesToDecode];
        buf.get(bytes);
        Object value;
        if (Encoding.CHAR.equals(encoding)) {
            value = new String(bytes, StandardCharsets.ISO_8859_1);
        } else if (Encoding.BCD.equals(encoding)) {
            value = Bcd.decode(bytes);
        } else {
            if (bytes.length == 8 && (bytes[0] & 0x80) > 0) {
                throw new CodecException(String.format("Value exceeds long type %s", Hex.encode(bytes)));
            }
            value = Binary.decodeLong(bytes);
        }

        if (value instanceof String) {
            String numericString = ((String) value);
            int stringLength = numericString.length();
            int pos = 0;
            while (pos < stringLength - 2 && numericString.charAt(pos) == '0') {
                pos++;
            }
            if (pos > 0) {
                numericString = numericString.substring(pos);
            }
            int digits = numericString.length();
            if (digits < 10) {
                return Integer.valueOf(numericString);
            } else if (digits < 19) {
                return Long.valueOf(numericString);
            } else {
                return new BigInteger(numericString);
            }
        } else {
            return (Long) value;
        }
    }

    public void encode(ByteBuffer buf, Number value) {
        if (!supportsBigInteger && !((value instanceof Long) || (value instanceof Integer))) {
            throw new CodecException(String.format("Value %s exceeds capacity of field", value));
        }
        if (Encoding.BINARY == encoding) {
            Long longValue = value.longValue();
            buf.put(Binary.encode(longValue, fixedLength));
        } else {
            String stringValue;
            if (fixedLength != null) {
                stringValue = String.format("%0" + fixedLength + "d", value);
            } else {
                stringValue = value.toString();
            }

            if (Encoding.CHAR.equals(encoding)) {
                buf.put(stringValue.getBytes(StandardCharsets.ISO_8859_1));
            } else {
                buf.put(Bcd.encode(stringValue));
            }
        }
    }

    @Override
    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public int hashCode() {
        return Hash.build(this, encoding, fixedLength);
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
            NumericCodec other = (NumericCodec) o;
            return EqualsBuilder.newInstance(other.encoding, encoding).append(other.fixedLength, fixedLength).isEqual();
        }
    }

}
