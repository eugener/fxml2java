package org.fxml2java;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import java.util.*;
import java.util.function.Consumer;

class FXNode {

    private static final List<String> EXCLUDED_ATTRS = Arrays.asList("xmlns", "xmlns:fx", "fx:controller");

    private final boolean isRoot;
    private final String name;
    private final Consumer<FXNode> onCreate;
    private final List<FXNode> children = new ArrayList<>();
    private final Map<String, String> attributes = new HashMap<>();
    private final Map<String, String> methods = new HashMap<>();

    public FXNode(Node node, Consumer<FXNode> onCreate, boolean isRoot) {

        this.isRoot = isRoot;
        this.name = node.getNodeName();
        this.onCreate = onCreate;

        onCreate.accept(this);

        addChildren(node.getChildNodes());

        // read node attributes
        var attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attrNode = attributes.item(i);
            String attrName = attrNode.getNodeName();
            String attrValue = attrNode.getNodeValue();

            if (!isRoot || !EXCLUDED_ATTRS.contains(attrName)) {

                if (attrValue.startsWith("#")) {
                    this.methods.put(attrName, attrValue.substring(1));
                } else {
                    this.attributes.put(attrName, attrValue);
                }
            }
        }

    }

    public FXNode(Node node, Consumer<FXNode> onCreate) {
        this(node, onCreate, false);
    }

    public final boolean isRoot() {
        return isRoot;
    }

    public String getName() {
        return name;
    }

    public List<FXNode> getChildren() {
        return children;
    }

    public String getAttribute(String name) {
        return this.attributes.get(name);
    }

    public String getId() {
        String id = getAttribute("fx:id");
        return id == null ? "this" : id;
    }

    // if id is not set generates one using provided index
    public void updateId(int index) {
        if (getId() == null) {
            this.attributes.put("fx:id", (getName() + index).toLowerCase());
        }
    }

    private void addChildren(NodeList nodes) {

        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            switch (child.getNodeType()) {
                case Node.ELEMENT_NODE:
                    if ("children".equals(child.getNodeName())) {
                        addChildren(child.getChildNodes());
                    } else {
                        this.children.add(new FXNode(child, this.onCreate));
                    }
                    break;
                case Node.PROCESSING_INSTRUCTION_NODE:
                    System.out.println(((ProcessingInstruction) child).getData());
                    break;
            }
        }
    }

    private void printTree(int indent) {
        if (indent < 0) {
            indent = 0;
        }
        String attrs = String.join(",", this.attributes.keySet());
        System.out.println(" ".repeat(indent) + this.name + "(" + attrs + ")");
        for (FXNode fn : this.children) {
            fn.printTree(indent + 3);
        }
    }

    public void printDeclaration(CodeWriter writer) {
        writer.printf("protected final %s %s;\n", this.name, this.getId());
    }

    public void printInitialization(CodeWriter writer) {
        writer.printf("%s = new %s();\n", getId(), this.name);
    }

    public void printAppendChildren(CodeWriter writer) {
        if (getChildren().size() > 0) {
            String start = String.format("%s.getChildren().addAll(", this.getId());
            writer.printBlock(start, ");", w -> getChildren().forEach(child -> w.println(child.getId() + ",")));
            writer.println();
        }
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public void printSetAttributes(CodeWriter writer) {
        attributes.forEach( (attrName, attrValue) -> {
            if (!attrName.contains("fx:")) {
                // TODO check type of attribute using reflection
                writer.printf("%s.set%s(\"%s\");\n", getId(), capitalize(attrName), attrValue);
            }
        });

        methods.forEach( (methodName,methodValue) ->
            writer.printf("%s.set%s(this::%s);\n", getId(), capitalize(methodName), methodValue)
        );
        writer.println();
    }

    public void printMethods(CodeWriter writer) {
        methods.forEach( (methodName,methodValue) ->
            writer.printf("protected abstract void %s(javafx.event.ActionEvent actionEvent);\n", methodName)
        );
    }

}
