package com.gmail.visualbukkit.blocks.definitions;

import com.gmail.visualbukkit.blocks.ClassInfo;
import com.gmail.visualbukkit.blocks.Expression;
import com.gmail.visualbukkit.blocks.parameters.ExpressionParameter;

import java.io.File;

public class ExprFileParentDirectory extends Expression {

    public ExprFileParentDirectory() {
        super("expr-file-parent-directory", ClassInfo.of(File.class));
    }

    @Override
    public Block createBlock() {
        return new Block(this, new ExpressionParameter(ClassInfo.of(File.class))) {
            @Override
            public String toJava() {
                return arg(0) + ".getParentFile()";
            }
        };
    }
}
