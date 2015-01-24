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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.chiknrice.iso.config.IsoMessageDef;

/**
 * The main codec class which encodes and decodes an IsoMessage to and from a byte[]. The byte[] doesn't include the
 * length bytes usually found in front of an ISO8583 message to indicate how long the message is.
 * 
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public class IsoMessageCodec {

    private final IsoMessageDef config;

    /**
     * The only constructor of the codec which accepts a configuration read from an xml which conforms to iso8583.xsd.
     * The codec is thread safe but is limited to encoding/decoding messages defined by the config. A separate instance
     * would be required if different config needs to be used.
     * 
     * @param config
     *            the IsoMessageDef instance which represents 1 xml config.
     */
    public IsoMessageCodec(IsoMessageDef config) {
        this.config = config;
    }

    /**
     * Decodes the isoBytes based on the rules defined by the config.
     * 
     * @param isoBytes
     *            the bytes to decode.
     * @return the decoded IsoMessage.
     */
    public IsoMessage decode(byte[] isoBytes) {
        ByteBuffer buf = ByteBuffer.wrap(isoBytes);
        Map<Integer, Object> header = null;
        if (config.getHeaderCodec() != null) {
            header = (Map<Integer, Object>) config.getHeaderCodec().decode(buf);
        }
        Integer mti = config.getMtiCodec().decode(buf).intValue();
        IsoMessage m = new IsoMessage(mti);
        if (header != null) {
            m.setHeader(new ArrayList<Object>(header.values()));
        }
        Map<Integer, Object> fields = (Map<Integer, Object>) config.getFieldsCodec().get(mti).decode(buf);
        for (Entry<Integer, Object> field : fields.entrySet()) {
            m.setField(field.getKey(), field.getValue());
        }
        return m;
    }

    /**
     * Encodes the IsoMessage to bytes based on the rules defined by the config.
     * 
     * @param msg
     *            the message to be encoded.
     * @return the encoded bytes.
     */
    public byte[] encode(IsoMessage msg) {
        ByteBuffer buf = ByteBuffer.allocate(0x7FFF);
        if (config.getHeaderCodec() != null) {
            config.getHeaderCodec().encode(buf, msg.getHeader());
        }
        config.getMtiCodec().encode(buf, msg.getMti().longValue());
        config.getFieldsCodec().get(msg.getMti()).encode(buf, msg.getFields());
        byte[] encoded = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, encoded, 0, encoded.length);
        return encoded;
    }

}
