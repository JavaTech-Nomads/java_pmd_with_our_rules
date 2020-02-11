/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.types;


import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.NonNull;

import net.sourceforge.pmd.lang.java.types.internal.infer.JInferenceVar;

/**
 * Common supertype for {@link JTypeVar} and {@link JInferenceVar},
 * the two kinds of types that can be substituted in types.
 *
 * @see TypeOps#subst(JTypeMirror, Function)
 */
public interface SubstVar extends JTypeMirror {

    String getName();

    @Override
    default JTypeMirror subst(Function<? super SubstVar, ? extends @NonNull JTypeMirror> subst) {
        return subst.apply(this);
    }
}
