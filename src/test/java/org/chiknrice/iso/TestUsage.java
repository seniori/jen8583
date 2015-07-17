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

import org.chiknrice.iso.config.IsoMessageDef;

import java.util.Date;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
@SuppressWarnings("UnusedAssignment")
public class TestUsage {

    public void testUsage() {
        IsoMessage isoMessage = new IsoMessage(200);
        isoMessage.setField(7, new Date());
        isoMessage.setField("28.1", "C");
        isoMessage.setField("28.2", 200);

        IsoMessageDef def = IsoMessageDef.build("iso8583ascii.xml");
        IsoMessageCodec codec = new IsoMessageCodec(def);

        byte[] encodedMessage = codec.encode(isoMessage);

        byte[] isoBytes = new byte[0];

        IsoMessage decodedMessage = codec.decode(isoBytes);

        Date transmissionDate = decodedMessage.getField(7);
        Integer stan = decodedMessage.getField(11);
        String transactionFeePrefix = decodedMessage.getField("28.1");
        Integer transactionFee = decodedMessage.getField("28.2");
    }

}
