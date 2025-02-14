package com.gmail.visualbukkit.blocks.definitions;

import com.gmail.visualbukkit.blocks.ClassInfo;
import com.gmail.visualbukkit.blocks.Expression;
import com.gmail.visualbukkit.blocks.parameters.ExpressionParameter;
import com.gmail.visualbukkit.gui.IconButton;
import com.gmail.visualbukkit.gui.StyleableHBox;
import javafx.scene.Node;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExprCombineStrings extends Expression {

    public ExprCombineStrings() {
        super("expr-combine-strings", ClassInfo.STRING);
    }

    @Override
    public Block createBlock() {
        Block block = new Block(this) {
            @Override
            public String toJava() {
                String java = "(" + arg(0) + "+" + arg(1) + ")";
                for (int i = 2; i < getParameters().size(); i++) {
                    java = "(" + java + "+" + arg(i) + ")";
                }
                return java;
            }
        };

        IconButton increaseSizeButton = new IconButton("plus", null, e -> increaseSize(block));
        IconButton decreaseSizeButton = new IconButton("minus", null, e -> decreaseSize(block));
        Node titleNode = block.getSyntaxBox().getChildren().remove(0);
        block.getSyntaxBox().getChildren().add(new StyleableHBox(titleNode, increaseSizeButton, decreaseSizeButton));
        increaseSize(block);
        increaseSize(block);

        return block;
    }

    @Override
    public Block createBlock(JSONObject json) {
        Block block = createBlock();
        JSONArray parameterArray = json.optJSONArray("parameters");
        if (parameterArray != null) {
            for (int i = 2; i < parameterArray.length(); i++) {
                increaseSize(block);
            }
        }
        block.deserialize(json);
        return block;
    }

    private void increaseSize(Block block) {
        int size = block.getParameters().size();
        if (size < 10) {
            block.addParameterLine(size + ")", new ExpressionParameter(ClassInfo.STRING));
        }
    }

    private void decreaseSize(Block block) {
        int size = block.getParameters().size();
        if (size > 2) {
            block.getParameters().remove(size - 1);
            block.getSyntaxBox().getChildren().remove(size);
        }
    }
}
