package au.com.bytecode.opencsv;

/**
 Copyright 2005 Bytecode Pty Ltd.
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 @see "http://sourceforge.net/p/opencsv/code/HEAD/tree/trunk/src/au/com/bytecode/opencsv/CSVReader.java"
 @author Glen Smith
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * A very simple CSV reader released under a commercial-friendly license.
 *
 *
 * @author Glen Smith
 *
 */
public class CSVReader {

    private final BufferedReader br;
    private boolean hasNext = true;
    private final char separator;
    private final char quotechar;
    private final int skipLines;
    private boolean linesSkiped;
    /** The default separator to use if none is supplied to the constructor. */
    public static final char DEFAULT_SEPARATOR = ',';
    /**
     * The default quote character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';
    /**
     * The default line to start reading.
     */
    public static final int DEFAULT_SKIP_LINES = 0;

    /**
     * Constructs CSVReader using a comma for the separator.
     *
     * @param reader
     *            the reader to an underlying CSV source.
     */
    public CSVReader(final Reader reader) {
        this(reader, DEFAULT_SEPARATOR);
    }

    /**
     * Constructs CSVReader with supplied separator.
     *
     * @param reader
     *            the reader to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries.
     */
    public CSVReader(final Reader reader, final char separator) {
        this(reader, separator, DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * Constructs CSVReader with supplied separator and quote char.
     *
     * @param reader
     *            the reader to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     */
    public CSVReader(final Reader reader, final char separator,
                     final char quotechar) {
        this(reader, separator, quotechar, DEFAULT_SKIP_LINES);
    }

    /**
     * Constructs CSVReader with supplied separator and quote char.
     *
     * @param reader
     *            the reader to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     * @param line
     *            the line number to skip for start reading
     */
    public CSVReader(final Reader reader, final char separator,
                     final char quotechar, final int line) {
        this.br = new BufferedReader(reader);
        this.separator = separator;
        this.quotechar = quotechar;
        this.skipLines = line;
    }

    /**
     * Reads the entire file into a List with each element being a String[] of
     * tokens.
     *
     * @return a List of String[], with each String[] representing a line of the
     *         file.
     *
     * @throws IOException
     *             if bad things happen during the read
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List readAll() throws IOException {
        final List allElements = new ArrayList();
        while (this.hasNext) {
            final String[] nextLineAsTokens = readNext();
            if (nextLineAsTokens != null) {
                allElements.add(nextLineAsTokens);
            }
        }
        return allElements;
    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     *
     * @return a string array with each comma-separated element as a separate
     *         entry.
     *
     * @throws IOException
     *             if bad things happen during the read
     */
    public String[] readNext() throws IOException {
        final String nextLine = getNextLine();
        return this.hasNext ? parseLine(nextLine) : null;
    }

    /**
     * Reads the next line from the file.
     *
     * @return the next line from the file without trailing newline
     * @throws IOException
     *             if bad things happen during the read
     */
    private String getNextLine() throws IOException {
        if (!this.linesSkiped) {
            for (int i = 0; i < this.skipLines; i++) {
                this.br.readLine();
            }
            this.linesSkiped = true;
        }
        final String nextLine = this.br.readLine();
        if (nextLine == null) {
            this.hasNext = false;
        }
        return this.hasNext ? nextLine : null;
    }

    /**
     * Parses an incoming String and returns an array of elements.
     *
     * @param nextLine
     *            the string to parse
     * @return the comma-tokenized list of elements, or null if nextLine is null
     * @throws IOException
     *             if bad things happen during the read
     */
    @SuppressWarnings("unchecked")
    private String[] parseLine(String nextLine) throws IOException {
        if (nextLine == null) {
            return null;
        }
        @SuppressWarnings("rawtypes")
        final List tokensOnThisLine = new ArrayList();
        StringBuffer sb = new StringBuffer();
        boolean inQuotes = false;
        do {
            if (inQuotes) {
                // continuing a quoted section, reappend newline
                sb.append("\n");
                nextLine = getNextLine();
                if (nextLine == null) {
                    break;
                }
            }
            for (int i = 0; i < nextLine.length(); i++) {
                final char c = nextLine.charAt(i);
                if (c == this.quotechar) {
                    // this gets complex... the quote may end a quoted block, or
                    // escape another quote.
                    // do a 1-char lookahead:
                    if (inQuotes // we are in quotes, therefore there can be
                        // escaped quotes in here.
                        && nextLine.length() > i + 1 // there is indeed
                        // another character
                        // to check.
                        && nextLine.charAt(i + 1) == this.quotechar) { // ..and
                        // that
                        // char.
                        // is
                        // a
                        // quote
                        // also.
                        // we have two quote chars in a row == one quote char,
                        // so
                        // consume them both and
                        // put one on the token. we do *not* exit the quoted
                        // text.
                        sb.append(nextLine.charAt(i + 1));
                        i++;
                    } else {
                        inQuotes = !inQuotes;
                        // the tricky case of an embedded quote in the middle:
                        // a,bc"d"ef,g
                        if (i > 2 // not on the begining of the line
                            && nextLine.charAt(i - 1) != this.separator // not
                            // at
                            // the
                            // begining
                            // of
                            // an
                            // escape
                            // sequence
                            && nextLine.length() > i + 1
                            && nextLine.charAt(i + 1) != this.separator // not
                            // at
                            // the
                            // end
                            // of
                            // an
                            // escape
                            // sequence
                           ) {
                            sb.append(c);
                        }
                    }
                } else if (c == this.separator && !inQuotes) {
                    tokensOnThisLine.add(sb.toString());
                    sb = new StringBuffer(); // start work on next token
                } else {
                    sb.append(c);
                }
            }
        } while (inQuotes);
        tokensOnThisLine.add(sb.toString());
        return (String[]) tokensOnThisLine.toArray(new String[0]);
    }

    /**
     * Closes the underlying reader.
     *
     * @throws IOException
     *             if the close fails
     */
    public void close() throws IOException {
        this.br.close();
    }

}
