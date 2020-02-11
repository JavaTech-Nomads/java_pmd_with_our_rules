/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.types;

import static java.util.Arrays.asList;
import static net.sourceforge.pmd.lang.java.types.TypeConversion.UncheckedConversion.NONE;
import static net.sourceforge.pmd.lang.java.types.TypeConversion.UncheckedConversion.NO_WARNING;
import static net.sourceforge.pmd.lang.java.types.TypeConversion.UncheckedConversion.WARNING;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.pmd.lang.java.types.JTypeVar.FreshTypeVar;
import net.sourceforge.pmd.lang.java.types.internal.infer.JInferenceVar;
import net.sourceforge.pmd.util.CollectionUtil;

/**
 * Utility class for type conversions, as defined in <a href="https://docs.oracle.com/javase/specs/jls/se10/html/jls-5.html">JLS§5</a>.
 */
public final class TypeConversion {

    private TypeConversion() {

    }

    /**
     * Returns whether the type 'from' is convertible to the type 'to'
     * by unchecked conversion. There is an unchecked conversion from the
     * raw class or interface type G to any parameterized type {@code G<...>} etc.
     *
     * <p>https://docs.oracle.com/javase/specs/jls/se9/html/jls-5.html#jls-5.1.9
     */
    public static UncheckedConversion uncheckedConversionExists(JTypeMirror from, JTypeMirror to) {

        if (from instanceof JArrayType && to instanceof JArrayType) {
            return uncheckedConversionExists(((JArrayType) from).getComponentType(),
                                             ((JArrayType) to).getComponentType());
        }

        if (!(from.isRaw() && to instanceof JClassType)) {
            return NONE;
        }

        JClassType toC = (JClassType) to;

        JClassType asSuper = ((JClassType) from).getAsSuper(toC.getSymbol());
        if (asSuper == null) {
            return NONE; // unrelated types
        }

        return TypeOps.allArgsAreUnboundedWildcards(toC.getTypeArgs())
               ? NO_WARNING // also catches the case where 'asSuper' is raw as well
               : WARNING;
    }

    public enum UncheckedConversion {
        /** Eg {@code Class<?> ~/> Class<String>}. */
        NONE,
        /** Eg {@code Class ~> Class<String>}. */
        WARNING,
        /** Eg {@code Class ~> Class<?>}, or special case of {@code Class ~> Class} (identity conversion). */
        NO_WARNING
    }

    /**
     * Performs <a href="https://docs.oracle.com/javase/specs/jls/se9/html/jls-5.html#jls-5.6.1">Unary numeric promotion
     * (JLS§5.6.1)</a>.
     * <p>This occurs in the following situations:
     * <ul>
     * <li>Each dimension expression in an array creation expression (§15.10.1)
     * <li>The index expression in an array access expression (§15.10.3)
     * <li>The operand of a unary plus operator + (§15.15.3)
     * <li>The operand of a unary minus operator - (§15.15.4)
     * <li>The operand of a bitwise complement operator ~ (§15.15.5)
     * <li>Each operand, separately, of a shift operator <<, >>, or >>> (§15.19).
     * </ul>
     *
     * <p>Returns {@link TypeSystem#ERROR_TYPE} if the given type is
     * not a numeric type, {@link TypeSystem#UNRESOLVED_TYPE} if the type
     * is unresolved.
     */
    public static JTypeMirror unaryNumericPromotion(JTypeMirror t) {
        t = t.unbox();

        TypeSystem ts = t.getTypeSystem();

        if (t == ts.BYTE || t == ts.SHORT || t == ts.CHAR) {
            return ts.INT;
        }

        return t.isNumeric() || t == ts.UNRESOLVED_TYPE ? t : ts.ERROR_TYPE;
    }

    /**
     * JLS§5.6.2
     * https://docs.oracle.com/javase/specs/jls/se9/html/jls-5.html#jls-5.6.2
     *
     * Binary numeric promotion is performed on the operands of certain operators:
     * <ul>
     * <li>The multiplicative operators *, /, and % (§15.17)
     * <li>The addition and subtraction operators for numeric types + and - (§15.18.2)
     * <li>The numerical comparison operators <, <=, >, and >= (§15.20.1)
     * <li>The numerical equality operators == and != (§15.21.1)
     * <li>The integer bitwise operators &, ^, and | (§15.22.1)
     * <li>In certain cases, the conditional operator ? : (§15.25)
     * </ul>
     * <p>Returns {@link TypeSystem#ERROR_TYPE} if either of the parameters
     * is not numeric. This DOES NOT care for unresolved types.
     */
    public static JTypeMirror binaryNumericPromotion(JTypeMirror t, JTypeMirror s) {
        JTypeMirror t1 = t.unbox();
        JTypeMirror s1 = s.unbox();

        TypeSystem ts = t.getTypeSystem();

        if (t1 == ts.DOUBLE || s1 == ts.DOUBLE) {
            return ts.DOUBLE;
        } else if (t1 == ts.FLOAT || s1 == ts.FLOAT) {
            return ts.FLOAT;
        } else if (t1 == ts.LONG || s1 == ts.LONG) {
            return ts.LONG;
        } else if (t1.isNumeric() && s1.isNumeric()) {
            return ts.INT;
        } else {
            // this is a typing error, both types should be referring to a numeric type
            return ts.ERROR_TYPE;
        }
    }

    /**
     * Is t convertible to s by boxing/unboxing/widening conversion?
     * Only t can be undergo conversion.
     */
    public static boolean isConvertible(JTypeMirror t, JTypeMirror s) {
        TypeSystem ts = t.getTypeSystem();
        if (t == ts.UNRESOLVED_TYPE || t == ts.ERROR_TYPE) {
            return true;
        }

        if (t instanceof JInferenceVar || s instanceof JInferenceVar) {
            return t.box().isSubtypeOf(s.box());
        }

        if (t.isPrimitive() == s.isPrimitive()) {
            return t.isSubtypeOf(s);
        }

        return t.isPrimitive() ? t.box().isSubtypeOf(s) : t.unbox().isSubtypeOf(s);
    }


    /**
     * Perform capture conversion on the type t. This replaces wildcards
     * with fresh type variables. Capture conversion is not applied recursively.
     * Capture conversion on any type other than a parameterized type (§4.5) acts
     * as an identity conversion (§5.1.1).
     *
     * @return The capture conversion of t
     */
    public static JTypeMirror capture(JTypeMirror t) {
        return t instanceof JClassType ? capture((JClassType) t) : t;
    }

    /**
     * Perform capture conversion on the type t. This replaces wildcards
     * with fresh type variables. Capture conversion is not applied recursively.
     * Capture conversion on any type other than a parameterized type (§4.5) acts
     * as an identity conversion (§5.1.1).
     *
     * @return The capture conversion of t
     */
    public static JClassType capture(JClassType type) {
        if (isNotWilcardParameterized(type)) {
            return type; // 99% take this path
        }

        TypeSystem ts = type.getTypeSystem();
        List<JTypeMirror> typeArgs = type.getTypeArgs();
        List<JTypeVar> typeParams = type.getFormalTypeParams();

        assert typeParams.size() == typeArgs.size() : "Type is not well formed " + type  + " (expects " + typeParams.size() + " params)";

        // This is the algorithm described at https://docs.oracle.com/javase/specs/jls/se10/html/jls-5.html#jls-5.1.10

        // Let G name a generic type declaration (§8.1.2, §9.1.2)
        // with n type parameters A1,...,An with corresponding bounds U1,...,Un.

        // There exists a capture conversion from a parameterized type G<T1,...,Tn> (§4.5)
        // to a parameterized type G<S1,...,Sn>, where, for 1 ≤ i ≤ n :
        // -> see the loop

        // typeParams is A1..An
        // typeArgs is T1..Tn
        // freshVars is S1..Sn

        List<JTypeMirror> freshVars = makeFreshVars(type);

        // Map of Ai to Si, for the substitution
        Substitution subst = Substitution.mapping(typeParams, freshVars);

        for (int i = 0; i < typeArgs.size(); i++) {
            JTypeVar param = typeParams.get(i);         // Ai
            JTypeMirror fresh = freshVars.get(i);       // Si
            JTypeMirror arg = typeArgs.get(i);          // Ti

            // we mutate the bounds to preserve the correct instance in
            // the substitutions

            if (arg instanceof JWildcardType) {
                JWildcardType w = (JWildcardType) arg;        // Ti alias
                FreshTypeVar freshVar = (FreshTypeVar) fresh; // Si alias

                JTypeMirror prevUpper = param.getUpperBound(); // Ui
                JTypeMirror substituted = TypeOps.subst(prevUpper, subst);

                if (w.isUnbounded()) {
                    // If Ti is a wildcard type argument (§4.5.1) of the form ?,
                    // then Si is a fresh type variable whose upper bound is Ui[A1:=S1,...,An:=Sn]
                    // and whose lower bound is the null type (§4.1).

                    freshVar.setUpperBound(substituted);
                    freshVar.setLowerBound(ts.NULL_TYPE);

                } else if (w.isUpperBound()) {
                    // If Ti is a wildcard type argument of the form ? extends Bi,
                    // then Si is a fresh type variable whose upper bound is glb(Bi, Ui[A1:=S1,...,An:=Sn])
                    // and whose lower bound is the null type.
                    freshVar.setUpperBound(ts.glb(asList(substituted, w.getBound())));
                    freshVar.setLowerBound(ts.NULL_TYPE);

                } else {
                    // If Ti is a wildcard type argument of the form ? super Bi,
                    // then Si is a fresh type variable whose upper bound is Ui[A1:=S1,...,An:=Sn]
                    // and whose lower bound is Bi.
                    freshVar.setUpperBound(substituted);
                    freshVar.setLowerBound(w.getBound());
                }
            }
        }

        return type.withTypeArguments(freshVars);
    }

    public static boolean isNotWilcardParameterized(JTypeMirror t) {
        if (!(t instanceof JClassType)) {
            return true;
        }
        return CollectionUtil.none(((JClassType) t).getTypeArgs(), it -> it instanceof JWildcardType);
    }


    private static List<JTypeMirror> makeFreshVars(JClassType type) {
        List<JTypeMirror> freshVars = new ArrayList<>(type.getTypeArgs().size());
        for (JTypeMirror typeArg : type.getTypeArgs()) {
            if (typeArg instanceof JWildcardType) {
                freshVars.add(((JWildcardType) typeArg).captureWildcard());
            } else {
                freshVars.add(typeArg);
            }
        }
        return freshVars;
    }

}
