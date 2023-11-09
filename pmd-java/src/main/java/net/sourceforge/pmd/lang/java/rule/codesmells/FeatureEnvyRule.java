package net.sourceforge.pmd.lang.java.rule.codesmells;

import net.sourceforge.pmd.lang.java.ast.*;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.java.rule.internal.JavaRuleUtil;
import net.sourceforge.pmd.lang.java.symbols.JFieldSymbol;
import net.sourceforge.pmd.lang.metrics.MetricsUtil;


import java.util.ArrayList;

import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.ACCESS_TO_FOREIGN_DATA;

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

    //Will be used to count the number of attributes for a class
    private int attribute_count = 0;

    private ArrayList<Object> foreignClasses = new ArrayList<>();

    public FeatureEnvyRule() {
    }

    @Override
    public Object visit(ASTClassOrInterfaceDeclaration node, Object data) {
        super.visit(node, data);

        if (!MetricsUtil.supportsAll(node, ACCESS_TO_FOREIGN_DATA)) {
            return data;
        }

        int atfd = MetricsUtil.computeMetric(ACCESS_TO_FOREIGN_DATA, node);
        double laa = 0;
        int fdp = 0;
        if(atfd != 0){ //Only calculate fdp and laa if atfd is non zero.
            /**
             LAA is defined as "The number of attributes from the method’s definition class, divided by the
             total number of variables accessed (including attributes used via accessor methods, see ATFD),
             whereby the number of local attributes accessed is computed in conformity with the LAA specifications"
             See: Lanza. Object-Oriented Metrics in Practice. Page 171
             */
            laa = (double)attribute_count/(double)atfd;
            /**
             * The number of classes in which the attributes accessed — in conformity with
             * the ATFD metric — are defined
             * See: Lanza. Object-Oriented Metrics in Practice. Page 171
             */
            fdp = foreignClasses.size();
        }

        if(atfd > FEW_ATFD_THRESHOLD && laa < LAA_THRESHOLD && fdp <= FEW_FDP_THRESHOLD) {
            addViolation(data, node);
        }

        return null;
    }

    @Override
    public Object visit(ASTMethodCall node, Object data) {
        if (isForeignMethod(node)) {
            if(!foreignClasses.contains(node.getClass())) {
                foreignClasses.add(node.getClass());
            }
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTFieldAccess node, Object data) {
        if (isForeignField(node)) {
            if(!foreignClasses.contains(node.getClass())) {
                foreignClasses.add(node.getClass());
            }
        } else{
            attribute_count++;
        }
        return super.visit(node, data);
    }

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

    private boolean isForeignMethod(ASTMethodCall node) {
        return JavaRuleUtil.isGetterOrSetterCall(node)
                && node.getQualifier() != null
                && !(node.getQualifier() instanceof ASTThisExpression);
    }
}
