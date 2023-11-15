package net.sourceforge.pmd.lang.java.rule.codesmells;

import net.sourceforge.pmd.lang.java.ast.*;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.java.rule.internal.JavaRuleUtil;
import net.sourceforge.pmd.lang.java.symbols.JFieldSymbol;


import java.util.ArrayList;

public class FeatureEnvyRule extends AbstractJavaRule {

    /**
     * Few means between 2 and 5. See: Lanza. Object-Oriented Metrics in Practice. Page 18.
     */
    private static final int FEW_ATFD_THRESHOLD = 3;
    private static final int FEW_FDP_THRESHOLD = 3;

    /**
     * One third is a low value. See: Lanza. Object-Oriented Metrics in Practice. Page 17.
     */
    private static final double LAA_THRESHOLD = 1.0 / 3.0;

    //Used to count atfd
    private int foreign_data_count;

    //Will be used to count the number of attributes for the method (used in LAA)
    private int local_attribute_count;

    //Used to count fdp
    private ArrayList<String> foreignClasses;

    public FeatureEnvyRule() {
    }

    @Override
    public Object visit(ASTMethodDeclaration node, Object data) {
        foreign_data_count = 0;
        local_attribute_count = 0;
        foreignClasses = new ArrayList<>();

        super.visit(node, data);

        int atfd = foreign_data_count;
        double laa = 0;
        int fdp = 0;
        if(atfd != 0){ //Only calculate fdp and laa if atfd is non zero.
            /**
             LAA is defined as "The number of attributes from the method’s definition class, divided by the
             total number of variables accessed (including attributes used via accessor methods, see ATFD),
             whereby the number of local attributes accessed is computed in conformity with the LAA specifications"
             See: Lanza. Object-Oriented Metrics in Practice. Page 171
             */
            laa = (double) local_attribute_count /(double)atfd;
            /**
             * The number of classes in which the attributes accessed — in conformity with
             * the ATFD metric — are defined
             * See: Lanza. Object-Oriented Metrics in Practice. Page 171
             */
            fdp = foreignClasses.size();
        }

        System.out.println("atfd: " + atfd);
        System.out.println("laa: " + laa);
        System.out.println("attributes: " + local_attribute_count);
        System.out.println("fdp: " + fdp);
        System.out.println("");

        if(atfd > FEW_ATFD_THRESHOLD && laa < LAA_THRESHOLD && fdp <= FEW_FDP_THRESHOLD) {
            addViolation(data, node);
        }

        return null;
    }

    @Override
    public Object visit(ASTMethodCall node, Object data) {
        if (isForeignMethod(node)) {
            foreign_data_count++;
            //TODO find a way to count fdp for method calls
        }

        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTFieldAccess node, Object data) {
        if (isForeignField(node)) {
            foreign_data_count++;

            //Store the name of the class where this foreign field is defined, used for fdp
            String className = node.getReferencedSym().getEnclosingClass().getSimpleName();
            if(!foreignClasses.contains(className)) {
                foreignClasses.add(className);
            }
        } else {
            /**
             * Don't count if this node is a class, as we will then be accessing
             * a field or a variable within the class, leading to it being counted twice
             */
            if(!node.getTypeMirror().isClassOrInterface()) {
                local_attribute_count++;
            }
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTVariableAccess node, Object data) {
        /**
         * Don't count if this node is a class, as we will then be accessing
         * a field or a variable within the class, leading to it being counted twice
         */
        if(!node.getTypeMirror().isClassOrInterface()) {
            local_attribute_count++;
        }
        return super.visit(node, data);
    }

    /**
     * Taken from net/sourceforge/pmd/lang/java/metrics/internal/AtfdBaseVisitor.java
     */
    private boolean isForeignField(ASTFieldAccess node) {
        JFieldSymbol sym = node.getReferencedSym();
        if (sym == null || sym.isStatic()) {
            return false;
        }
        ASTExpression qualifier = node.getQualifier();
        return !(qualifier instanceof ASTThisExpression
                || qualifier instanceof ASTSuperExpression
                || sym.getEnclosingClass().equals(node.getEnclosingType().getSymbol())
        );
    }

    /**
     * Taken from net/sourceforge/pmd/lang/java/metrics/internal/AtfdBaseVisitor.java
     */
    private boolean isForeignMethod(ASTMethodCall node) {
        return JavaRuleUtil.isGetterOrSetterCall(node)
                && node.getQualifier() != null
                && !(node.getQualifier() instanceof ASTThisExpression);
    }
}
