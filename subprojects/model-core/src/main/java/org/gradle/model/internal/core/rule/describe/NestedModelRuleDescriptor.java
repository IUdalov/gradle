/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.internal.core.rule.describe;

import com.google.common.base.Objects;
import net.jcip.annotations.ThreadSafe;
import org.gradle.api.UncheckedIOException;

import java.io.IOException;

@ThreadSafe
public class NestedModelRuleDescriptor extends AbstractModelRuleDescriptor {

    private final ModelRuleDescriptor parent;
    private final ModelRuleDescriptor child;

    public NestedModelRuleDescriptor(ModelRuleDescriptor parent, ModelRuleDescriptor child) {
        this.parent = parent;
        this.child = child;
    }

    public NestedModelRuleDescriptor(ModelRuleDescriptor parent, String child) {
        this(parent, new SimpleModelRuleDescriptor(child));
    }

    public void describeTo(Appendable appendable) {
        parent.describeTo(appendable);
        try {
            appendable.append(" > ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        child.describeTo(appendable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NestedModelRuleDescriptor that = (NestedModelRuleDescriptor) o;
        return Objects.equal(parent, that.parent)
            && Objects.equal(child, that.child);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parent, child);
    }

    public static ModelRuleDescriptor append(ModelRuleDescriptor parent, String str) {
        return new NestedModelRuleDescriptor(parent, new SimpleModelRuleDescriptor(str));
    }

    public static ModelRuleDescriptor append(ModelRuleDescriptor parent, String str, Object... args) {
        return append(parent, String.format(str, args));
    }
}
