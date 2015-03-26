/**
 * 
 */
package org.chiknrice.iso;

import java.util.Date;

import org.chiknrice.iso.config.IsoMessageDef;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
 */
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
