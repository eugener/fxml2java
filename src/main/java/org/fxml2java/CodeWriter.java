package org.fxml2java;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class CodeWriter implements Closeable {

    private final PrintWriter writer;
    private final int indentSize;

    private int indent = 0;
    private String sIndent = "";

    public CodeWriter(OutputStream outputStream, int indentSize) {
        this.writer = new PrintWriter(outputStream);
        this.indentSize = indentSize;
    }

    public CodeWriter(OutputStream outputStream) {
        this(outputStream, 3);
    }

    @Override
    public void close() {
        writer.close();
    }

    private void incIndent(int inc) {
        this.indent += inc;
        sIndent = " ".repeat(indentSize * indent);
    }

//    public void print(CharSequence s) {
//        writer.print(sIndent + s);
//    }

    public void println(CharSequence s) {
        writer.println(sIndent + s);
    }

    public void println() {
        writer.println();
    }

    public void printf(String format, Object... args) {
        String s = String.format(format, args);
        writer.print(sIndent + s);
    }

    public void printBlock(String start, String end, Consumer<CodeWriter> writeBlock) {
        try {
            println(start);
            incIndent(1);
            writeBlock.accept(this);
        } finally {
            incIndent(-1);
            println(end);
        }
    }
}
