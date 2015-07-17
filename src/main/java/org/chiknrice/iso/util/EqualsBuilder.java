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

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 */
public class EqualsBuilder {

    private final List<Object> left;
    private final List<Object> right;

    public static EqualsBuilder newInstance(Object left, Object right) {
        return new EqualsBuilder().append(left, right);
    }

    private EqualsBuilder() {
        left = new ArrayList<>();
        right = new ArrayList<>();
    }

    public EqualsBuilder append(Object left, Object right) {
        this.left.add(left);
        this.right.add(right);
        return this;
    }

    public boolean isEqual() {
        for (int i = 0; i < left.size(); i++) {
            Object left = this.left.get(i);
            Object right = this.right.get(i);
            if (!eq(left, right)) {
                return false;
            }
        }
        return true;
    }

    private boolean eq(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

}
