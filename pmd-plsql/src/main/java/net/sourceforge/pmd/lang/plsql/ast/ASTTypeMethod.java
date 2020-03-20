/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.plsql.ast;

public final class ASTTypeMethod extends AbstractPLSQLNode implements ExecutableCode {

    ASTTypeMethod(int id) {
        super(id);
    }

    @Override
    public Object jjtAccept(PLSQLParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Gets the name of the method.
     *
     * @return a String representing the name of the method
     */
    @Override
    public String getMethodName() {
        ASTMethodDeclarator md = getFirstChildOfType(ASTMethodDeclarator.class);
        if (md != null) {
            return md.getImage();
        }
        return null;
    }

    public String getName() {
        return getMethodName();
    }
}
