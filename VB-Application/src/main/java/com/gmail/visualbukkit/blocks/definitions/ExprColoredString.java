package com.gmail.visualbukkit.blocks.definitions;

import com.gmail.visualbukkit.blocks.ClassInfo;
import com.gmail.visualbukkit.blocks.Expression;
import com.gmail.visualbukkit.blocks.parameters.ExpressionParameter;

public class ExprColoredString extends Expression {

    public ExprColoredString() {
        super("expr-colored-string", ClassInfo.STRING);
    }

    @Override
    public Block createBlock() {
        return new Block(this, new ExpressionParameter(ClassInfo.STRING)) {
            @Override
            public String toJava() {
                return "ChatColor.translateAlternateColorCodes('&'," + arg(0) + ")";
            }
        };
    }
}
