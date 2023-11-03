package net.sourceforge.pmd.lang.java.rule.codesmells;

import net.sourceforge.pmd.lang.java.ast.*;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.metrics.MetricsUtil;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.ACCESS_TO_FOREIGN_DATA;

public class FeatureEnvyRule  extends AbstractJavaRule {

    /**
     * Few means between 2 and 5. See: Lanza. Object-Oriented Metrics in Practice. Page 18.
     */
    private static final int FEW_ATFD_THRESHOLD = 3;
    private static final int FEW_FDP_THRESHOLD = 3;

    /**
     * One third is a low value. See: Lanza. Object-Oriented Metrics in Practice. Page 17.
     */
    private static final double LAA_THRESHOLD = 1.0 / 3.0;

    int attribute_count = 0;

    public FeatureEnvyRule() {
    }


    @Override
    public Object visit(ASTClassOrInterfaceDeclaration node, Object data) {
        super.visit(node, data);

        if (!MetricsUtil.supportsAll(node, ACCESS_TO_FOREIGN_DATA)) {
            return data;
        }

        int atfd = MetricsUtil.computeMetric(ACCESS_TO_FOREIGN_DATA, node);
        /**
        LAA is defined as "The number of attributes from the method’s definition class, divided by the
        total number of variables accessed (including attributes used via accessor methods, see ATFD),
        whereby the number of local attributes accessed is computed in conformity with the LAA specifications"
        See: Lanza. Object-Oriented Metrics in Practice. Page 171
        */
        int laa = attribute_count/atfd;

        if(atfd > FEW_ATFD_THRESHOLD && laa < LAA_THRESHOLD) {
            addViolation(data, node);
        }

        return null;
    }

    @Override
    public Object visit(ASTLocalVariableDeclaration node, Object data) {
        attribute_count++;
        return super.visit(node, data);
    }
}
