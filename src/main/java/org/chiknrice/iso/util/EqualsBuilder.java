/**
 * 
 */
package org.chiknrice.iso.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:chiknrice@gmail.com">Ian Bondoc</a>
 *
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
        boolean equal = true;
        for (int i = 0; i < left.size(); i++) {
            Object left = this.left.get(i);
            Object right = this.right.get(i);
            if (!eq(left, right)) {
                return false;
            }
        }
        return equal;
    }

    private boolean eq(Object x, Object y) {
        return x == null ? y == null : x.equals(y);
    }

}
