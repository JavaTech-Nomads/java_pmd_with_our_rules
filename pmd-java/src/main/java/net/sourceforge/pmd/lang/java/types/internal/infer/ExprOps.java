/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.types.internal.infer;

import static net.sourceforge.pmd.lang.java.types.TypeConversion.capture;
import static net.sourceforge.pmd.util.CollectionUtil.listOf;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.lang.java.symbols.JClassSymbol;
import net.sourceforge.pmd.lang.java.symbols.JTypeDeclSymbol;
import net.sourceforge.pmd.lang.java.types.JClassType;
import net.sourceforge.pmd.lang.java.types.JMethodSig;
import net.sourceforge.pmd.lang.java.types.JTypeMirror;
import net.sourceforge.pmd.lang.java.types.JTypeVar;
import net.sourceforge.pmd.lang.java.types.TypeOps;
import net.sourceforge.pmd.lang.java.types.TypeSystem;
import net.sourceforge.pmd.lang.java.types.internal.infer.ExprMirror.BranchingMirror;
import net.sourceforge.pmd.lang.java.types.internal.infer.ExprMirror.InvocationMirror;
import net.sourceforge.pmd.lang.java.types.internal.infer.ExprMirror.LambdaExprMirror;
import net.sourceforge.pmd.lang.java.types.internal.infer.ExprMirror.MethodRefMirror;
import net.sourceforge.pmd.util.CollectionUtil;

final class ExprOps {

    private final Infer infer;
    private final TypeSystem ts;

    ExprOps(Infer infer) {
        this.infer = infer;
        this.ts = infer.getTypeSystem();
        assert ts != null;
    }

    /**
     * Returns true if the argument expression is potentially
     * compatible with type t, as specified by JLS§15.12.2.1:
     *
     * https://docs.oracle.com/javase/specs/jls/se9/html/jls-15.html#jls-15.12.1
     *
     * @param m Method for which the potential applicability is being tested
     * @param e Argument expression
     * @param t Formal parameter type
     */
    boolean isPotentiallyCompatible(JMethodSig m, ExprMirror e, JTypeMirror t) {
        if (e instanceof BranchingMirror) {
            // A conditional expression (§15.25) is potentially compatible with a type if each
            // of its second and third operand expressions are potentially compatible with that type.

            BranchingMirror cond = (BranchingMirror) e;
            return cond.getBranches().allMatch(branch -> isPotentiallyCompatible(m, branch, t));

        }

        boolean isLambdaOrRef = e instanceof LambdaExprMirror || e instanceof MethodRefMirror;

        if (isLambdaOrRef && t instanceof JTypeVar) {
            //  A lambda expression or a method reference expression is potentially compatible with
            //  a type variable if the type variable is a type parameter of the candidate method.
            return m.getTypeParameters().contains(t);
        }

        if (isLambdaOrRef && t instanceof JClassType) {
            JMethodSig fun = TypeOps.findFunctionalInterfaceMethod((JClassType) t);
            if (fun == null) {
                // t is not a functional interface
                return false;
            }

            if (e instanceof LambdaExprMirror) {
                LambdaExprMirror lambda = (LambdaExprMirror) e;
                if (fun.getArity() != lambda.getParamCount()) {
                    return false;
                }


                boolean expectsVoid = fun.getReturnType() == ts.NO_TYPE;

                return expectsVoid && lambda.isVoidCompatible() || lambda.isValueCompatible();

            } else {
                // is method reference

                // TODO need to look for potentially applicable methods
                return true;
            }
        }

        // A class instance creation expression, a method invocation expression, or an expression of
        // a standalone form (§15.2) is potentially compatible with any type.
        // (ie anything else)
        return true;
    }

    /**
     * Returns true if the the argument expression is pertinent
     * to applicability for the potentially applicable method m,
     * called at site 'invoc', as specified by JLS§15.12.2.2:
     *
     * https://docs.oracle.com/javase/specs/jls/se9/html/jls-15.html#jls-15.12.2.2
     *
     * @param arg        Argument expression
     * @param m          Method type
     * @param formalType Type of the formal parameter
     * @param invoc      Invocation expression
     */
    static boolean isPertinentToApplicability(ExprMirror arg, JMethodSig m, JTypeMirror formalType, InvocationMirror invoc) {
        // An argument expression is considered pertinent to applicability
        // for a potentially applicable method m unless it has one of the following forms:

        if (arg instanceof LambdaExprMirror) {
            LambdaExprMirror lambda = (LambdaExprMirror) arg;

            // An implicitly typed lambda expression(§ 15.27 .1).
            if (!lambda.isExplicitlyTyped()) {
                return false;
            }

            // An explicitly typed lambda expression where at least
            // one result expression is not pertinent to applicability.
            for (ExprMirror it : lambda.getResultExpressions()) {
                if (!isPertinentToApplicability(it, m, formalType, invoc)) {
                    return false;
                }
            }

            //  If m is a generic method and the method invocation does
            //  not provide explicit type arguments, an explicitly typed
            //  lambda expression for which the corresponding target type
            //  (as derived from the signature of m) is a type parameter of m.
            if (m.isGeneric() && invoc.getExplicitTypeArguments().isEmpty()) {
                return !formalType.isTypeVariable();
            }

            return true;
        }

        if (arg instanceof MethodRefMirror) {
            // An inexact method reference expression(§ 15.13 .1).
            if (getExactMethod((MethodRefMirror) arg) == null) {
                return false;
            }
            //  If m is a generic method and the method invocation does
            //  not provide explicit type arguments, an exact method
            //  reference expression for which the corresponding target type
            //  (as derived from the signature of m) is a type parameter of m.
            if (m.isGeneric() && invoc.getExplicitTypeArguments().isEmpty()) {
                return !formalType.isTypeVariable();
            }
            return true;
        }

        if (arg instanceof BranchingMirror) {
            // A conditional expression (§15.25) is potentially compatible with a type if each
            // of its second and third operand expressions are potentially compatible with that type.

            BranchingMirror cond = (BranchingMirror) arg;
            return cond.getBranches().allMatch(branch -> isPertinentToApplicability(branch, m, formalType, invoc));
        }

        return true;
    }


    /**
     * Returns null if the method reference is inexact.
     */
    @Nullable
    static JMethodSig getExactMethod(MethodRefMirror mref) {
        JMethodSig cached = mref.getCachedExactMethod();

        if (cached == null) { // inexact
            return null;
        }

        if (cached.getTypeSystem().UNRESOLVED_METHOD == cached) {
            cached = computeExactMethod(mref);
            mref.setCachedExactMethod(cached); // set to null if inexact, sentinel is UNRESOLVED_METHOD
        }

        return cached;
    }

    @Nullable
    private static JMethodSig computeExactMethod(MethodRefMirror mref) {


        final @Nullable JTypeMirror lhs = mref.getLhsIfType();

        List<JMethodSig> accessible;

        if (mref.isConstructorRef()) {
            if (lhs == null) {
                // ct error, already reported as a missing symbol in our system
                return null;
            } else if (lhs.isArray()) {
                // A method reference expression of the form ArrayType :: new is always exact.
                // But:  If a method reference expression has the form ArrayType :: new, then ArrayType
                // must denote a type that is reifiable (§4.7), or a compile-time error occurs.
                if (lhs.isReifiable()) {
                    JTypeDeclSymbol symbol = lhs.getSymbol();
                    assert symbol instanceof JClassSymbol
                        && ((JClassSymbol) symbol).isArray()
                        : "Reifiable array should present a symbol! " + lhs;
                    return lhs.getConstructors().get(0);
                } else {
                    // todo compile time error
                    return null;
                }
            } else {

                if (lhs.isRaw() || !(lhs instanceof JClassType)) {
                    return null;
                }

                JClassType enclosing = (JClassType) lhs;

                accessible = enclosing.getConstructors().stream()
                                      .filter(it -> it.isAccessible(enclosing))
                                      .collect(Collectors.toList());
            }
        } else {
            JTypeMirror typeToSearch = mref.getTypeToSearch();

            OverloadSet overloads = new OverloadSet();
            String name = mref.getMethodName();
            JClassType enclosing = mref.getEnclosingType();
            typeToSearch.streamMethods(TypeOps.accessibleMethodFilter(name, enclosing.getSymbol()))
                        .forEach(it -> overloads.add(it, enclosing));
            accessible = overloads.getOverloads();
        }

        if (accessible.size() == 1) {
            JMethodSig candidate = accessible.get(0);
            if (candidate.isVarargs()
                || candidate.isGeneric() && mref.getExplicitTypeArguments() == null) {
                return null;
            }

            if (lhs != null && lhs.isRaw()) {
                // can be raw if the method doesn't mention type vars
                // of the original owner, ie the erased method is the
                // same as the generic method.
                JClassType lhsClass = (JClassType) candidate.getDeclaringType();
                JMethodSig unerased = candidate.internalApi().withOwner(lhsClass.getGenericTypeDeclaration()).internalApi().originalMethod();
                if (TypeOps.mentionsAnyTvar(unerased, lhsClass.getFormalTypeParams())) {
                    return null;
                }
            }

            return adaptGetClass(candidate, mref.getTypeToSearch().getErasure());
        } else {
            return null;
        }
    }


    @Nullable
    JMethodSig findRefCompileTimeDecl(MethodRefMirror mref, JMethodSig targetType) {
        JTypeMirror lhsIfType = mref.getLhsIfType();
        boolean acceptLowerArity = lhsIfType != null && lhsIfType.isClassOrInterface() && !mref.isConstructorRef();

        // TODO asInstanceMethod doe
        MethodCallSite site1 = infer.newCallSite(methodRefAsInvocation(mref, targetType, false), null);
        site1.setLogging(false);
        JMethodSig m1 = infer.determineInvocationTypeResult(site1);

        if (acceptLowerArity) {
            // then we need to perform two searches, one with arity n, looking for static methods,
            // one with n-1, looking for instance methods

            MethodCallSite site2 = infer.newCallSite(methodRefAsInvocation(mref, targetType, true), null);
            site2.setLogging(false);
            JMethodSig m2 = infer.determineInvocationTypeResult(site2);

            //  If the first search produces a most specific method that is static,
            //  and the set of applicable methods produced by the second search
            //  contains no non-static methods, then the compile-time declaration
            //  is the most specified method of the first search.
            if (m1 != ts.UNRESOLVED_METHOD && m1.isStatic() && (m2 == ts.UNRESOLVED_METHOD || m2.isStatic())) {
                return m1;
            } else if (m2 != ts.UNRESOLVED_METHOD && !m2.isStatic() && (m1 == ts.UNRESOLVED_METHOD || !m1.isStatic())) {
                // Otherwise, if the set of applicable methods produced by the
                // first search contains no static methods, and the second search
                // produces a most specific method that is non-static, then the
                // compile-time declaration is the most specific method of the second search.
                return m2;
            }

            //  Otherwise, there is no compile-time declaration.
            return null;
        } else if (m1 == ts.UNRESOLVED_METHOD || m1.isStatic()) {
            // if the most specific applicable method is static, there is no compile-time declaration.
            return null;
        } else {
            // Otherwise, the compile-time declaration is the most specific applicable method.
            return m1;
        }
    }

    private static InvocationMirror methodRefAsInvocation(final MethodRefMirror mref, JMethodSig targetType, boolean asInstanceMethod) {
        // the arguments are treated as if they were of the type
        // of the formal parameters of the candidate
        List<JTypeMirror> formals = targetType.getFormalParameters();
        if (asInstanceMethod && !formals.isEmpty()) {
            formals = formals.subList(1, formals.size()); // skip first param (receiver)
        }

        List<ExprMirror> arguments = CollectionUtil.map(
            formals,
            fi -> new ExprMirror() {
                @Override
                public JavaNode getLocation() {
                    return mref.getLocation();
                }

                @Override
                public JTypeMirror getStandaloneType() {
                    return fi;
                }

                @Override
                public String toString() {
                    return "formal : " + fi;
                }
            }
        );


        return new InvocationMirror() {

            private MethodCtDecl mt;

            @Override
            public JavaNode getLocation() {
                return mref.getLocation();
            }

            @Override
            public List<JMethodSig> getVisibleCandidates() {
                return ExprOps.getCandidates(mref, asInstanceMethod, targetType);
            }

            @Override
            public JTypeMirror getErasedReceiverType() {
                return mref.getTypeToSearch().getErasure();
            }

            @Override
            public List<JTypeMirror> getExplicitTypeArguments() {
                return mref.getExplicitTypeArguments();
            }

            @Override
            public JavaNode getExplicitTargLoc(int i) {
                throw new IndexOutOfBoundsException();
            }

            @Override
            public String getName() {
                return mref.getMethodName();
            }

            @Override
            public List<ExprMirror> getArgumentExpressions() {
                return arguments;
            }

            @Override
            public int getArgumentCount() {
                return arguments.size();
            }

            @Override
            public void setMethodType(MethodCtDecl methodType) {
                this.mt = methodType;
            }

            @Override
            public @Nullable MethodCtDecl getMethodType() {
                return mt;
            }

            @Override
            public void setInferredType(JTypeMirror mirror) {

            }

            @Override
            public @NonNull JClassType getEnclosingType() {
                return mref.getEnclosingType();
            }

            @Override
            public String toString() {
                return "Method ref adapter (for " + mref + ")";
            }
        };
    }

    private static List<JMethodSig> getCandidates(MethodRefMirror mref, boolean asInstanceMethod, JMethodSig targetType) {
        JMethodSig exactMethod = getExactMethod(mref);
        if (exactMethod != null) {
            return Collections.singletonList(exactMethod);
        } else {
            JTypeMirror typeToSearch = mref.getTypeToSearch();
            if (typeToSearch.isArray() && mref.isConstructorRef()) {
                // ArrayType :: new
                return Collections.singletonList(typeToSearch.getConstructors().get(0));
            } else if (typeToSearch instanceof JClassType && mref.isConstructorRef()) {
                // ClassType :: [TypeArguments] new
                // TODO treatment of raw constructors is whacky
                return typeToSearch.getConstructors();
            }

            if (asInstanceMethod && typeToSearch.isRaw() && typeToSearch instanceof JClassType && targetType.getArity() > 0) {
                //  In the second search, if P1, ..., Pn is not empty
                //  and P1 is a subtype of ReferenceType, then the
                //  method reference expression is treated as if it were
                //  a method invocation expression with argument expressions
                //  of types P2, ..., Pn. If ReferenceType is a raw type,
                //  and there exists a parameterization of this type, G<...>,
                //  that is a supertype of P1, the type to search is the result
                //  of capture conversion (§5.1.10) applied to G<...>; otherwise,
                //  the type to search is the same as the type of the first search.

                JClassType type = (JClassType) typeToSearch;
                JTypeMirror p1 = targetType.getFormalParameters().get(0);
                JTypeMirror asSuper = p1.getAsSuper(type.getSymbol());
                if (asSuper != null && asSuper.isParameterizedType()) {
                    typeToSearch = capture(asSuper);
                }

            }

            // Primary :: [TypeArguments] Identifier
            // ExpressionName :: [TypeArguments] Identifier
            // super :: [TypeArguments] Identifier
            // TypeName.super :: [TypeArguments] Identifier
            // ReferenceType :: [TypeArguments] Identifier
            return typeToSearch.streamMethods(s -> s.getSimpleName().equals(mref.getMethodName()))
                               .collect(Collectors.toList());
        }
    }


    static JMethodSig adaptGetClass(JMethodSig sig, JTypeMirror erasedReceiverType) {
        TypeSystem ts = sig.getTypeSystem();
        if (sig.getName().equals("getClass") && sig.getDeclaringType().equals(ts.OBJECT)) {
            if (erasedReceiverType != null) {
                return sig.internalApi().withReturnType(getClassReturn(erasedReceiverType, ts));
            }
        }
        return sig;
    }

    private static JTypeMirror getClassReturn(JTypeMirror erasedReceiverType, TypeSystem ts) {
        return ts.parameterise(ts.getClassSymbol(Class.class), listOf(ts.wildcard(true, erasedReceiverType)));
    }
}
