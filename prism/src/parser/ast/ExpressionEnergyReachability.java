package parser.ast;

import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public abstract class ExpressionEnergyReachability extends Expression {

    @Override
    public Object accept(ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public Object evaluate(EvaluateContext ec) throws PrismLangException {
        throw new PrismLangException("This expression can't be evaluated without a model: "+this);
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public boolean isProposition() {
        return false;
    }

    @Override
    public boolean returnsSingleValue() {
        return false;
    }

}
