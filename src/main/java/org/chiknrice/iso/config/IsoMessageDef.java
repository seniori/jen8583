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
package org.chiknrice.iso.config;

import java.util.Map;

import org.chiknrice.iso.codec.CompositeCodec;
import org.chiknrice.iso.codec.NumericCodec;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
public final class IsoMessageDef {

    private final CompositeCodec headerCodec;
    private final NumericCodec mtiCodec;
    private final Map<Integer, CompositeCodec> fieldsCodec;

    public IsoMessageDef(CompositeCodec headerCodec, NumericCodec mtiCodec, Map<Integer, CompositeCodec> fieldsCodec) {
        this.headerCodec = headerCodec;
        this.mtiCodec = mtiCodec;
        this.fieldsCodec = fieldsCodec;
    }

    public CompositeCodec getHeaderCodec() {
        return headerCodec;
    }

    public NumericCodec getMtiCodec() {
        return mtiCodec;
    }

    public Map<Integer, CompositeCodec> getFieldsCodec() {
        return fieldsCodec;
    }
    
    public static IsoMessageDef build(String configXml) {
        return new ConfigBuilder(configXml).build();
    }

}
