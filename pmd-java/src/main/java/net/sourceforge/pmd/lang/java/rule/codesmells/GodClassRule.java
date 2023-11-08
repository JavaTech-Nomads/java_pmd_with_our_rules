/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.rule.codesmells;


import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.ACCESS_TO_FOREIGN_DATA;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.TIGHT_CLASS_COHESION;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.WEIGHED_METHOD_COUNT;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.SMALL_PROJECT;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.LARGE_PROJECT;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.NCSS;

import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRulechainRule;
import net.sourceforge.pmd.lang.metrics.MetricsUtil;
import net.sourceforge.pmd.util.StringUtil;


/**
 * The God Class Rule detects the God Class design flaw using metrics. A god class does too many things, is very big and
 * complex. It should be split apart to be more object-oriented. The rule uses the detection strategy described in [1].
 * The violations are reported against the entire class.
 *
 * <p>[1] Lanza. Object-Oriented Metrics in Practice. Page 80.
 *
 * @since 5.0
 */
public class GodClassRule extends AbstractJavaRulechainRule {

    /**
     * Threshold metrics from Object-Oriented Metrics in Practice. Page 16 - Page 17
     */
    private int WMC_VERY_HIGH;
    private int FEW_ATFD_THRESHOLD;
    private double TCC_THRESHOLD;


    public GodClassRule() {
        super(ASTClassOrInterfaceDeclaration.class); 
    }


    @Override
    public Object visit(ASTClassOrInterfaceDeclaration node, Object data) {
        if (!MetricsUtil.supportsAll(node, WEIGHED_METHOD_COUNT, TIGHT_CLASS_COHESION, ACCESS_TO_FOREIGN_DATA, NCSS)) {
            return data;
        }

        int ncss = MetricsUtil.computeMetric(NCSS, node); //ncss = non-commenting source statements - helps us categorize project size

        if (ncss >= LARGE_PROJECT) { //defining thresholds for a large project
            System.out.println("Got a large project");
            WMC_VERY_HIGH = 31;
            FEW_ATFD_THRESHOLD = 5;
            TCC_THRESHOLD = (1.0 / 3.0);
        } else if (ncss > SMALL_PROJECT) { //defining thresholds for a med project
            System.out.println("Got a medium project");
            WMC_VERY_HIGH = 14;
            FEW_ATFD_THRESHOLD = 5;
            TCC_THRESHOLD = (1.0 / 3.0);
        } else { //defining thresholds for a small project
            System.out.println("Got a small project");
            WMC_VERY_HIGH = 5;
            FEW_ATFD_THRESHOLD = 4;
            TCC_THRESHOLD = (1.0 / 3.0);
        }
        
        int calculated_wmc = MetricsUtil.computeMetric(WEIGHED_METHOD_COUNT, node); //measures the complexity of a class
        double calculated_tcc = MetricsUtil.computeMetric(TIGHT_CLASS_COHESION, node); //measures the cohesion of a class
        int calculated_atfd = MetricsUtil.computeMetric(ACCESS_TO_FOREIGN_DATA, node); //measures the number of attributes from unrelated classes that are accessed directly or through accessor methods

        System.out.println("GOD CLASS METRICS:");
        System.out.println("Defined: ");
        System.out.println("wmc = " + WMC_VERY_HIGH + ", tcc = " + TCC_THRESHOLD + ", aftd = " + FEW_ATFD_THRESHOLD);
        System.out.println("Calculated: ");
        System.out.println("NCSS: " + ncss);
        System.out.println("ncsdd = " + ncss + "wmc = " + calculated_wmc + ", tcc = " + calculated_tcc + ", aftd = " + calculated_atfd);

        if (calculated_wmc >= WMC_VERY_HIGH && calculated_atfd > FEW_ATFD_THRESHOLD && calculated_tcc < TCC_THRESHOLD) {
            asCtx(data).addViolation(node, new Object[] {calculated_wmc, StringUtil.percentageString(calculated_tcc, 3), calculated_atfd, });
        }
        return data;
    }

}
