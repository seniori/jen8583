package org.chiknrice.iso.util;

import org.chiknrice.iso.CodecException;

/**
 * Encodes and decodes values that are in a BCD format and can contain a trailing F when the number of BCD digits is
 * odd, i.e. the value is left justified and right padded with an F.
 *
 */
public class Bcdf {

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

        String result = new String(digits);
        // remove any non-digit characters, i.e.'F'
        return result!=null ? result.replaceAll("[^0-9]+", "") : result;
    }

    public static byte[] encode(String value) {
        byte[] bytes = new byte[value.length() / 2 + value.length() % 2];
        int length = value.length();

        // insert the trailing f if value digit length is odd
        int offset = 1;
        if((value.length() % 2)!=0){
            bytes[bytes.length-1] = 0x0f;
            length++;
            offset=2;
        }

        for (int charPos = length-offset; charPos >= 0; charPos--) {
            int bytePos = bytes.length - ((length - charPos - 1) / 2) - 1;
            boolean hi = (length - charPos) % 2 == 0;

            char c = value.charAt(charPos);
            if (!Character.isDigit(c)) {
                throw new CodecException(String.format("Invalid numeric value [%s]", value));
            }

            bytes[bytePos] = (byte) (bytes[bytePos] | ((c - 48) << (hi ? 4 : 0)));
            if (hi) {
                bytePos--;
            }
        }
        return bytes;
    }


}
