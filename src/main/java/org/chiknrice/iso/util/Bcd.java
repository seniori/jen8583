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

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
public class Bcd {

    /**
     * @param bytes
     * @return
     */
    public static String decode(byte[] bytes) {
        char[] digits = new char[bytes.length * 2];
        int charPos = digits.length - 1;// LSB

        for (int bytePos = bytes.length - 1; bytePos >= 0; bytePos--) {
            digits[charPos--] = (char) ((bytes[bytePos] & 0x0f) + 48);
            digits[charPos--] = (char) (((bytes[bytePos] & 0xf0) >> 4) + 48);
        }
        return new String(digits);
    }

    /**
     * @param value
     * @return
     */
    public static byte[] encode(String value) {
        byte[] bytes = new byte[value.length() / 2 + value.length() % 2];
        int length = value.length();

        for (int charPos = length - 1; charPos >= 0; charPos--) {
            int bytePos = bytes.length - ((length - charPos - 1) / 2) - 1;
            boolean hi = (length - charPos) % 2 == 0;

            char c = value.charAt(charPos);
            if (!Character.isDigit(c)) {
                throw new RuntimeException(String.format("Invalid numeric value [%s]", value));
            }

            bytes[bytePos] = (byte) (bytes[bytePos] | ((c - 48) << (hi ? 4 : 0)));
            if (hi) {
                bytePos--;
            }
        }
        return bytes;
    }

}
