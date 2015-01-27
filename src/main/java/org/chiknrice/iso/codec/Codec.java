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

import java.nio.ByteBuffer;

import org.chiknrice.iso.config.ComponentDef.Encoding;

/**
 * The main contract for a codec used across encoding and decoding of message components. Each field/component of the
 * ISO message would have its own instance of codec which contains the definition of how the value should be
 * encoded/decoded. The codec should be designed to be thread safe as it the instance would live throughout the life of
 * the IsoMessageDef. Any issues encountered during encoding/decoding should be throwing a RuntimeException.
 * 
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public interface Codec<T> extends Cloneable {

    /**
     * The implementation should define how the value T should be decoded from the ByteBuffer provided. The
     * implementation could either decode the value from a certain number of bytes or consume the whole ByteBuffer.
     * 
     * @param buf
     * @return the decoded value
     */
    public T decode(ByteBuffer buf);

    /**
     * The implementation should define how the value T should be encoded to the ByteBuffer provided. The ByteBuffer
     * assumes the value would be encoded from the current position.
     * 
     * @param buf
     * @param value
     *            the value to be encoded
     */
    public void encode(ByteBuffer buf, T value);

    /**
     * Defines how the value should be encoded/decoded.
     * 
     * @return the encoding defined for the value.
     */
    public Encoding getEncoding();

    /**
     * The implementation should create a new codec and set primitive fields to its own fields and the objects to their
     * clones.
     * 
     * @return
     */
    public Codec<T> clone() throws CloneNotSupportedException;

}
