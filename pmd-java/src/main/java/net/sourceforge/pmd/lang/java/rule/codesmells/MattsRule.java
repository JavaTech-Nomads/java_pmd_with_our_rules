/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.rule.codesmells;

import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRulechainRule;
import net.sourceforge.pmd.lang.metrics.MetricsUtil;

import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.ACCESS_TO_FOREIGN_DATA; //just for testing
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.TIGHT_CLASS_COHESION;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.WEIGHED_METHOD_COUNT;

public class MattsRule extends AbstractJavaRulechainRule {

    public MattsRule() {
        super(ASTClassOrInterfaceDeclaration.class);
    }

    @Override
    public Object visit(ASTClassOrInterfaceDeclaration node, Object data) {
        int wmc = MetricsUtil.computeMetric(WEIGHED_METHOD_COUNT, node); //measures the complexity of a class (cyclomatic complexity)
        double tcc = MetricsUtil.computeMetric(TIGHT_CLASS_COHESION, node); //measures the cohesion of a class
        int atfd = MetricsUtil.computeMetric(ACCESS_TO_FOREIGN_DATA, node);

        System.out.println("wmc = " + wmc + ", tcc = " + tcc + ", aftd = " + atfd); //to test god class metrics since we know this rule will always be detected
        addViolation(data, node);
        return data;
    }
}
