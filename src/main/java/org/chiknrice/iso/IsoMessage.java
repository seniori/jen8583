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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main class which represents the structure of an ISO8583 message. Fields and header components are structured as a
 * map having their indexes as keys for their values. Fields can have sub fields which are also structured as a map.
 * Adding and getting a field can be done by passing a field index. For sub fields, corresponding methods for setting
 * and retrieving the values are provided. The parameter to these sub field related methods are expressions in the form
 * of a dot separated field indexes (e.g. field 5 in field 2 in field 63, the expression would be 63.2.5).
 * 
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 * 
 */
@SuppressWarnings("unchecked")
public class IsoMessage {

    private final Map<Integer, Object> header;

    private final Integer mti;

    private final Map<Integer, Object> fields;

    public IsoMessage(int mti) {
        header = new TreeMap<>();
        this.mti = mti;
        fields = new TreeMap<>();
    }

    /**
     * Returns the header component located at the index. Indexes start with 1.
     * 
     * @param index
     * @return the value or null if the header component doesn't exist.
     */
    public <T> T getHeader(Integer index) {
        return (T) header.get(index);
    }

    /**
     * Returns an unmodifiable copy of the map backing the header values.
     * 
     * @return the header map.
     */
    public Map<Integer, Object> getHeader() {
        return Collections.unmodifiableMap(header);
    }

    /**
     * Clears the header fields and sets the values from the parameter in the order defined by the list.
     * 
     * @param header
     *            a list of header components to be set.
     */
    public void setHeader(List<Object> header) {
        this.header.clear();
        for (int i = 0; i < header.size(); i++) {
            this.header.put(i + 1, header.get(i));
        }
    }

    /**
     * Appends a value at the end of the existing header components.
     * 
     * @param value
     *            the value to be set appended
     */
    public void appendHeader(Object value) {
        this.header.put(header.size() + 1, value);
    }

    /**
     * Returns the mti of the message.
     * 
     * @return the mti
     */
    public Integer getMti() {
        return mti;
    }

    /**
     * Returns the header component located at the index. Indexes start with 2 as field 1 in ISO8583 is the bitmap.
     * 
     * @param index
     *            the index of the field
     * @return the value or null if the field doesn't exist.
     */
    public <T> T getField(Integer index) {
        return (T) fields.get(index);
    }

    /**
     * Returns the field component located at the position expressed by recursiveExpression.
     * 
     * @param recursiveExpression
     * @return the value or null if the field doesn't exist.
     */
    public <T> T getField(String recursiveExpression) {
        Pattern p = Pattern.compile("\\d+(\\.\\d+)+");
        Matcher m = p.matcher(recursiveExpression);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("%s is not a valid iso field expression",
                    recursiveExpression));
        } else {
            String[] indexes = recursiveExpression.split("\\.");
            Object subField = fields.get(Integer.valueOf(indexes[0]));
            for (int i = 1; i < indexes.length; i++) {
                if (subField instanceof Map) {
                    subField = ((Map<Integer, Object>) subField).get(Integer.valueOf(indexes[i]));
                } else {
                    subField = null;
                    break;
                }
            }
            return (T) subField;
        }
    }

    /**
     * Returns an unmodifiable copy of the map backing the fields.
     * 
     * @return the field map.
     */
    public Map<Integer, Object> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    public Map<String, Object> getAllFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        recordFieldMap(null, this.fields, fields);
        return fields;
    }

    private void recordFieldMap(String parent, Map<?, ?> fields, Map<String, Object> allFields) {
        for (Entry<?, ?> field : fields.entrySet()) {
            String key = parent != null ? parent.concat(".").concat(field.getKey().toString()) : field.getKey()
                    .toString();
            Object value = field.getValue();
            if (value instanceof Map) {
                recordFieldMap(key, (Map<?, ?>) value, allFields);
            } else {
                allFields.put(key, value);
            }
        }
    }

    /**
     * Sets the value of a field at the given index.
     * 
     * @param index
     *            the field index.
     * @param value
     *            the value.
     */
    public void setField(Integer index, Object value) {
        fields.put(index, value);
    }

    /**
     * Sets the value of the field at the position expressed by recursiveExpression.
     * 
     * @param recursiveExpression
     *            the position where the value should be set.
     * @param value
     *            the value.
     */
    public void setField(String recursiveExpression, Object value) {
        // TODO: remove value if null is passed
        if (value == null) {
            return;
        }
        Pattern p = Pattern.compile("\\d+(\\.\\d+)+");
        Matcher m = p.matcher(recursiveExpression);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("%s is not a valid iso field expression",
                    recursiveExpression));
        } else {
            String[] indexes = recursiveExpression.split("\\.");

            Map<Integer, Object> components = fields;

            for (int i = 0; i < indexes.length; i++) {
                if (i == (indexes.length - 1)) {
                    components.put(Integer.valueOf(indexes[i]), value);
                } else {
                    Integer key = Integer.valueOf(indexes[i]);
                    Object currentValue = components.get(key);
                    if (currentValue == null) {
                        currentValue = new TreeMap<>();
                        components.put(key, currentValue);
                    }

                    if (currentValue instanceof Map) {
                        components = (Map<Integer, Object>) currentValue;
                    } else {
                        // TODO: warn overwriting non TreeMap value
                        break;
                    }
                }
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = header.hashCode();
        hash ^= mti;
        return hash ^ fields.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof IsoMessage) {
            IsoMessage other = (IsoMessage) obj;
            if (header.equals(other.header) && mti == other.mti && fields.equals(other.fields)) {
                return true;
            }
        }
        return false;
    }

    private static final String TAB = "  ";

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(System.lineSeparator());
        if (header.size() > 0) {
            sb.append("Header:+").append(System.lineSeparator());
            for (Object headerComponent : header.values()) {
                sb.append(TAB).append(headerComponent).append(System.lineSeparator());
            }
        }
        sb.append("MTI: ").append(mti).append(System.lineSeparator());
        sb.append("Fields:+").append(System.lineSeparator());
        appendFields(fields, sb, 1);
        return sb.toString();
    }

    private void appendFields(Map<?, ?> components, StringBuilder sb, int level) {
        for (Entry<?, ?> component : components.entrySet()) {
            Object value = component.getValue();
            indent(sb, level);
            sb.append(component.getKey()).append(':');
            if (value instanceof Map) {
                sb.append('+').append(System.lineSeparator());
                appendFields((Map<?, ?>) value, sb, level + 1);
            } else {
                sb.append(' ').append(value).append(System.lineSeparator());
            }
        }
    }

    private void indent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append(TAB);
        }
    }

    public void copyFields(IsoMessage other, Integer... indexes) {
        for (Integer index : indexes) {
            Object value = other.getField(index);
            if (value != null) {
                setField(index, value);
            }
        }
    }

}
