package com.mrfuzzihead.fuzziids.csv;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/** Minimal RFC-4180-ish CSV writer (UTF-8, comma separated, quoted when needed). */
public final class CsvWriter implements Closeable {

    private final Writer writer;

    public CsvWriter(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory " + parent);
        }
        this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
    }

    public void writeRow(Object... cells) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            appendEscaped(line, cells[i] == null ? "" : String.valueOf(cells[i]));
        }
        line.append("\r\n");
        writer.write(line.toString());
    }

    private static void appendEscaped(StringBuilder line, String cell) {
        if (cell.indexOf(',') >= 0 || cell.indexOf('"') >= 0 || cell.indexOf('\n') >= 0 || cell.indexOf('\r') >= 0) {
            line.append('"')
                .append(cell.replace("\"", "\"\""))
                .append('"');
        } else {
            line.append(cell);
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
