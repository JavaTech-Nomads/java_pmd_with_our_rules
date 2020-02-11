/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.symbols.internal.impl.asm;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Opcodes;

import net.sourceforge.pmd.lang.java.symbols.JFieldSymbol;
import net.sourceforge.pmd.lang.java.symbols.internal.impl.SymbolEquality;
import net.sourceforge.pmd.lang.java.symbols.internal.impl.SymbolToStrings;
import net.sourceforge.pmd.lang.java.types.JTypeMirror;
import net.sourceforge.pmd.lang.java.types.Substitution;

class FieldStub extends MemberStubBase implements JFieldSymbol {

    private final LazyTypeSig type;
    private final @Nullable Object constValue;

    FieldStub(ClassStub classStub,
              String name,
              int accessFlags,
              String descriptor,
              String signature,
              @Nullable Object constValue) {
        super(classStub, name, accessFlags);
        this.type = new LazyTypeSig(classStub, descriptor, signature);
        this.constValue = constValue;
    }

    @Override
    public boolean isEnumConstant() {
        return (getModifiers() & Opcodes.ACC_ENUM) != 0;
    }

    @Override
    public JTypeMirror getTypeMirror(Substitution subst) {
        return type.get(subst);
    }

    @Override
    public String toString() {
        return SymbolToStrings.ASM.toString(this);
    }

    @Override
    public int hashCode() {
        return SymbolEquality.FIELD.hash(this);
    }

    @Override
    public boolean equals(Object obj) {
        return SymbolEquality.FIELD.equals(this, obj);
    }

}
