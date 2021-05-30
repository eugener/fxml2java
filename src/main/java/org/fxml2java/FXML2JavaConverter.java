package org.fxml2java;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class FXML2JavaConverter {

    private static final String FXML_CLASS_NAME = "FXMLDocumentBase";
    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private static DocumentBuilder builder;

    static {
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private final List<FXNode> allControls = new ArrayList<>();
    private final FXNode root;

    public FXML2JavaConverter(InputStream is) throws IOException, SAXException {
        Document doc = builder.parse(is);
        root = new FXNode(doc.getDocumentElement(), node -> {
            if (!node.isRoot()) allControls.add(node);
        }, true);
//        root.printTree(1);
    }

    public void convertTo(OutputStream os) {

        //TODO imports

        /// Update component ids
        for (int i = 0; i < allControls.size(); i++) {
            allControls.get(i).updateId(i);
        }

        try (CodeWriter w = new CodeWriter(os)) {

            // print class
            String classStart = String.format("public abstract class %s extends %s {\n", FXML_CLASS_NAME, root.getName());
            w.printBlock(classStart, "}", w1 -> {

                // print class attribute declaration
                allControls.forEach(n -> n.printDeclaration(w1));
                w1.println();

                // print class constructor
                String constructorStart = String.format("public %s() {\n", FXML_CLASS_NAME);
                w1.printBlock(constructorStart, "}", w2 -> {

                    // print class attribute initialization
                    allControls.forEach(n -> n.printInitialization(w2));
                    w2.println();

                    // print component children initialization
                    root.printAppendChildren(w2);
                    allControls.forEach(n -> n.printAppendChildren(w2));
                    w2.println();

                    // print component attribute assignment
                    root.printSetAttributes(w2);
                    allControls.forEach(n -> n.printSetAttributes(w2));
                    w2.println();

                });

                // print abstract methods
                allControls.forEach(n -> n.printMethods(w1));

            });

        }

    }

}