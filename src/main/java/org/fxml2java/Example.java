package org.fxml2java;

import org.xml.sax.SAXException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {

    public static void main(String[] args) {
        try {
            var converter = new FXML2JavaConverter(Example.class.getResourceAsStream("/FXMLDocument.fxml"));

            // output to console
            //  converter.convert(System.out);

            // output to file
            Path path = Path.of("FXMLDocumentBase.java");
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            converter.convertTo( new FileOutputStream( path.toFile()));
            System.out.println("Written to " + path.toAbsolutePath());

        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
    }

}
