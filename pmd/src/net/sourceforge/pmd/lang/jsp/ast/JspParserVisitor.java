/* Generated By:JavaCC: Do not edit this line. JspParserVisitor.java Version 4.1d1 */
package net.sourceforge.pmd.lang.jsp.ast;

public interface JspParserVisitor
{
  public Object visit(JspNode node, Object data);
  public Object visit(ASTCompilationUnit node, Object data);
  public Object visit(ASTContent node, Object data);
  public Object visit(ASTJspDirective node, Object data);
  public Object visit(ASTJspDirectiveAttribute node, Object data);
  public Object visit(ASTJspScriptlet node, Object data);
  public Object visit(ASTJspExpression node, Object data);
  public Object visit(ASTJspDeclaration node, Object data);
  public Object visit(ASTJspComment node, Object data);
  public Object visit(ASTText node, Object data);
  public Object visit(ASTUnparsedText node, Object data);
  public Object visit(ASTElExpression node, Object data);
  public Object visit(ASTValueBinding node, Object data);
  public Object visit(ASTCData node, Object data);
  public Object visit(ASTElement node, Object data);
  public Object visit(ASTAttribute node, Object data);
  public Object visit(ASTAttributeValue node, Object data);
  public Object visit(ASTJspExpressionInAttribute node, Object data);
  public Object visit(ASTCommentTag node, Object data);
  public Object visit(ASTDeclaration node, Object data);
  public Object visit(ASTDoctypeDeclaration node, Object data);
  public Object visit(ASTDoctypeExternalId node, Object data);
}
/* JavaCC - OriginalChecksum=0022153635b5970c76fca343197f84a7 (do not edit this line) */
