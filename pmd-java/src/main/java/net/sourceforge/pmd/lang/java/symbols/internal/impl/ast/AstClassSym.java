/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.symbols.internal.impl.ast;

import static net.sourceforge.pmd.util.CollectionUtil.listOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTBodyDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceType;
import net.sourceforge.pmd.lang.java.ast.ASTConstructorCall;
import net.sourceforge.pmd.lang.java.ast.ASTConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTEnumConstant;
import net.sourceforge.pmd.lang.java.ast.ASTEnumDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTRecordComponent;
import net.sourceforge.pmd.lang.java.ast.ASTRecordComponentList;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.java.ast.InternalApiBridge;
import net.sourceforge.pmd.lang.java.symbols.JClassSymbol;
import net.sourceforge.pmd.lang.java.symbols.JConstructorSymbol;
import net.sourceforge.pmd.lang.java.symbols.JExecutableSymbol;
import net.sourceforge.pmd.lang.java.symbols.JFieldSymbol;
import net.sourceforge.pmd.lang.java.symbols.JMethodSymbol;
import net.sourceforge.pmd.lang.java.symbols.JTypeDeclSymbol;
import net.sourceforge.pmd.lang.java.symbols.JTypeParameterOwnerSymbol;
import net.sourceforge.pmd.lang.java.symbols.internal.impl.ImplicitMemberSymbols;
import net.sourceforge.pmd.lang.java.types.JClassType;
import net.sourceforge.pmd.lang.java.types.JTypeMirror;
import net.sourceforge.pmd.lang.java.types.Substitution;
import net.sourceforge.pmd.lang.java.types.TypeOps;
import net.sourceforge.pmd.lang.java.types.TypeSystem;
import net.sourceforge.pmd.util.CollectionUtil;


final class AstClassSym
    extends AbstractAstTParamOwner<ASTAnyTypeDeclaration>
    implements JClassSymbol {

    private final @Nullable JTypeParameterOwnerSymbol enclosing;
    private final List<JClassSymbol> declaredClasses;
    private final List<JMethodSymbol> declaredMethods;
    private final List<JConstructorSymbol> declaredCtors;
    private final List<JFieldSymbol> declaredFields;

    AstClassSym(ASTAnyTypeDeclaration node,
                AstSymFactory factory,
                @Nullable JTypeParameterOwnerSymbol enclosing) {
        super(node, factory);
        this.enclosing = enclosing;

        // evaluate everything strictly
        // this populates symbols on the relevant AST nodes

        final List<JClassSymbol> myClasses = new ArrayList<>();
        final List<JMethodSymbol> myMethods = new ArrayList<>();
        final List<JConstructorSymbol> myCtors = new ArrayList<>();
        final List<JFieldSymbol> myFields = new ArrayList<>();

        final List<JFieldSymbol> recordComponents;
        if (isRecord()) {
            ASTRecordComponentList components = Objects.requireNonNull(node.getRecordComponents(),
                                                                       "Null component list for " + node);
            recordComponents = mapComponentsToMutableList(factory, components);
            myFields.addAll(recordComponents);

            JConstructorSymbol canonicalRecordCtor = ImplicitMemberSymbols.recordConstructor(this, recordComponents, components.isVarargs());
            myCtors.add(canonicalRecordCtor);
            InternalApiBridge.setSymbol(components, canonicalRecordCtor);

        } else {
            recordComponents = Collections.emptyList();
            if (node instanceof ASTEnumDeclaration) {
                node.getEnumConstants()
                    .forEach(constant -> myFields.add(new AstFieldSym(constant.getVarId(), factory, this)));
            }
        }


        for (ASTBodyDeclaration dnode : node.getDeclarations()) {

            if (dnode instanceof ASTAnyTypeDeclaration) {
                myClasses.add(new AstClassSym((ASTAnyTypeDeclaration) dnode, factory, this));
            } else if (dnode instanceof ASTMethodDeclaration) {
                if (!recordComponents.isEmpty() && ((ASTMethodDeclaration) dnode).getArity() == 0) {
                    // filter out record component, so that the accessor is not generated
                    recordComponents.removeIf(f -> f.getSimpleName().equals(((ASTMethodDeclaration) dnode).getName()));
                }
                myMethods.add(new AstMethodSym((ASTMethodDeclaration) dnode, factory, this));
            } else if (dnode instanceof ASTConstructorDeclaration) {
                myCtors.add(new AstCtorSym((ASTConstructorDeclaration) dnode, factory, this));
            } else if (dnode instanceof ASTFieldDeclaration) {
                for (ASTVariableDeclaratorId varId : ((ASTFieldDeclaration) dnode).getVarIds()) {
                    myFields.add(new AstFieldSym(varId, factory, this));
                }
            }
        }

        if (!recordComponents.isEmpty()) {
            // then the recordsComponents contains all record components
            // for which we must synthesize an accessor (explicitly declared
            // accessors have been filtered out)
            for (JFieldSymbol component : recordComponents) {
                myMethods.add(ImplicitMemberSymbols.recordAccessor(this, component));
            }
        }

        if (myCtors.isEmpty() && isClass() && !isAnonymousClass()) {
            myCtors.add(ImplicitMemberSymbols.defaultCtor(this));
        }

        if (this.isEnum()) {
            myMethods.add(ImplicitMemberSymbols.enumValues(this));
            myMethods.add(ImplicitMemberSymbols.enumValueOf(this));
        }

        this.declaredClasses = Collections.unmodifiableList(myClasses);
        this.declaredMethods = Collections.unmodifiableList(myMethods);
        this.declaredCtors = Collections.unmodifiableList(myCtors);
        this.declaredFields = Collections.unmodifiableList(myFields);
    }

    private List<JFieldSymbol> mapComponentsToMutableList(AstSymFactory factory, ASTRecordComponentList components) {
        List<JFieldSymbol> list = new ArrayList<>();
        for (ASTRecordComponent comp : components) {
            list.add(new AstFieldSym(comp.getVarId(), factory, this));
        }
        return list;
    }

    @Override
    public @NonNull String getSimpleName() {
        return node.getSimpleName();
    }


    @Override
    public @NonNull String getBinaryName() {
        return node.getBinaryName();
    }

    @Override
    public @Nullable String getCanonicalName() {
        return node.getCanonicalName();
    }

    @Override
    public boolean isUnresolved() {
        return false;
    }

    @Override
    public @Nullable JClassSymbol getEnclosingClass() {
        if (enclosing instanceof JClassSymbol) {
            return (JClassSymbol) enclosing;
        } else if (enclosing instanceof JExecutableSymbol) {
            return enclosing.getEnclosingClass();
        }
        assert enclosing == null;
        return null;
    }

    @Override
    public @Nullable JExecutableSymbol getEnclosingMethod() {
        return enclosing instanceof JExecutableSymbol ? (JExecutableSymbol) enclosing : null;
    }

    @Override
    public List<JClassSymbol> getDeclaredClasses() {
        return declaredClasses;
    }

    @Override
    public List<JMethodSymbol> getDeclaredMethods() {
        return declaredMethods;
    }

    @Override
    public List<JConstructorSymbol> getConstructors() {
        return declaredCtors;
    }

    @Override
    public List<JFieldSymbol> getDeclaredFields() {
        return declaredFields;
    }

    @Override
    public @Nullable JClassType getSuperclassType(Substitution substitution) {
        TypeSystem ts = getTypeSystem();
        if (node instanceof ASTEnumDeclaration) {
            JClassSymbol enumClass = ts.getClassSymbol(Enum.class);
            return (JClassType) ts.parameterise(enumClass, listOf(ts.declaration(this)));
        } else if (node instanceof ASTClassOrInterfaceDeclaration) {
            ASTClassOrInterfaceType superClass = ((ASTClassOrInterfaceDeclaration) node).getSuperClassTypeNode();
            return superClass == null
                   ? ts.OBJECT
                   // this cast relies on the fact that the superclass is not a type variable
                   : (JClassType) TypeOps.subst(superClass.getTypeMirror(), substitution);

        } else if (isAnonymousClass()) {

            if (node.getParent() instanceof ASTEnumConstant) {

                return node.getEnclosingType().getTypeMirror().subst(substitution);

            } else if (node.getParent() instanceof ASTConstructorCall) {

                @NonNull JTypeMirror sym = ((ASTConstructorCall) node.getParent()).getTypeNode().getTypeMirror();

                return sym instanceof JClassType && !sym.isInterface()
                       ? (JClassType) sym
                       : factory.types().OBJECT;
            }
        }

        // TODO records
        return null;
    }

    @Override
    public @Nullable JClassSymbol getSuperclass() {
        // notice this relies on the fact that the extends clause
        // (or the type node of the constructor call, for an anonymous class),
        // was disambiguated early
        JClassType sup = getSuperclassType(Substitution.EMPTY);
        return sup == null ? null : sup.getSymbol();
    }

    @Override
    public List<JClassSymbol> getSuperInterfaces() {
        return CollectionUtil.mapNotNull(
            node.getSuperInterfaceTypeNodes(),
            n -> {
                // we play safe here, but the symbol is either a JClassSymbol
                // or a JTypeParameterSymbol, with the latter case being a
                // compile-time error
                JTypeDeclSymbol sym = n.getReferencedSym();
                return sym instanceof JClassSymbol ? (JClassSymbol) sym : null;
            }
        );
    }

    @Override
    public List<JClassType> getSuperInterfaceTypes(Substitution subst) {
        return CollectionUtil.map(node.getSuperInterfaceTypeNodes(), n -> (JClassType) TypeOps.subst(n.getTypeMirror(), subst));
    }

    @Override
    public @Nullable JTypeDeclSymbol getArrayComponent() {
        return null;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isInterface() {
        return node.isInterface();
    }

    @Override
    public boolean isEnum() {
        return node.isEnum();
    }

    @Override
    public boolean isRecord() {
        return node.isRecord();
    }

    @Override
    public boolean isAnnotation() {
        return node.isAnnotation();
    }

    @Override
    public boolean isLocalClass() {
        return node.isLocal();
    }

    @Override
    public boolean isAnonymousClass() {
        return node.isAnonymous();
    }

    @Override
    public @Nullable Class<?> getJvmRepr() {
        return null;
    }

}
