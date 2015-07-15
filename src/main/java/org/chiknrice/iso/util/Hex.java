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
package org.chiknrice.iso.util;

import org.chiknrice.iso.CodecException;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class Hex {

    private static final String HEX = "0123456789ABCDEF";

    public static int value(char c) {
        return HEX.indexOf(c);
    }

    public static String encode(byte[] bytes) {
        char[] digits = new char[bytes.length * 2];
        int charPos = digits.length - 1;// LSB

        for (int bytePos = bytes.length - 1; bytePos >= 0; bytePos--) {
            digits[charPos--] = HEX.charAt(bytes[bytePos] & 0x0f);
            digits[charPos--] = HEX.charAt((bytes[bytePos] & 0xf0) >> 4);
        }
        return new String(digits);
    }

    public static byte[] decode(String hex) {
        // A null string returns an empty array
        if (hex == null || hex.length() == 0) {
            return new byte[0];
        } else {
            hex = hex.toUpperCase();
            byte[] bytes = new byte[hex.length() / 2 + hex.length() % 2];

            int length = hex.length();

            for (int charPos = length - 1; charPos >= 0; charPos--) {
                int bytePos = bytes.length - ((length - charPos - 1) / 2) - 1;
                boolean hi = (length - charPos) % 2 == 0;

                char c = hex.charAt(charPos);

                if (HEX.indexOf(c) == -1) {
                    throw new CodecException(String.format("Invalid hex char %s", c));
                }

                bytes[bytePos] |= (HEX.indexOf(c) << (hi ? 4 : 0));
            }
            return bytes;
        }
    }

}
