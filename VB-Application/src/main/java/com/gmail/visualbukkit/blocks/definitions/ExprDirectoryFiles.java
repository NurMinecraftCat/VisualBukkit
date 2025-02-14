package com.gmail.visualbukkit.blocks.definitions;

import com.gmail.visualbukkit.blocks.ClassInfo;
import com.gmail.visualbukkit.blocks.Expression;
import com.gmail.visualbukkit.blocks.parameters.ExpressionParameter;

import java.io.File;

public class ExprDirectoryFiles extends Expression {

    public ExprDirectoryFiles() {
        super("expr-directory-files", ClassInfo.LIST);
    }

    @Override
    public Block createBlock() {
        return new Block(this, new ExpressionParameter(ClassInfo.of(File.class))) {
            @Override
            public String toJava() {
                return "PluginMain.createList(" + arg(0) + ".listFiles())";
            }
        };
    }
}
