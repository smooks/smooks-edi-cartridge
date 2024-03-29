/*-
 * ========================LICENSE_START=================================
 * smooks-edi-sax
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 *
 * ======================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.edi.edisax;

import org.smooks.edi.edisax.model.internal.Delimiters;
import org.smooks.edi.edisax.util.EDIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Stack;

/**
 * Buffered EDI Stream Segment reader.
 *
 * @author tfennelly
 */
public class BufferedSegmentReader {

    private static final int MAX_MARK_READ = 512;

    private static final Logger LOGGER = LoggerFactory.getLogger(BufferedSegmentReader.class);

    public static String IGNORE_CR_LF = "!$";

    private InputStream underlyingByteStream;
    private boolean marked = false;
    private Charset readEncoding;
    private Reader reader;
    private StringBuffer segmentBuffer = new StringBuffer(512);
    private String[] currentSegmentFields = null;
    private int currentSegmentNumber = 0;
    private Stack<Delimiters> delimitersStack = new Stack<Delimiters>();
    private Delimiters currentDelimiters;
    private BufferedSegmentListener segmentListener;
    private boolean ignoreNewLines;
    private int charReadCount = 0;


    /**
     * Construct the stream reader.
     *
     * @param ediInputSource EDI Stream input source.
     * @param rootDelimiters Root currentDelimiters.  New currentDelimiters can be pushed and popped.
     */
    public BufferedSegmentReader(InputSource ediInputSource, Delimiters rootDelimiters) {
        underlyingByteStream = ediInputSource.getByteStream();
        reader = ediInputSource.getCharacterStream();
        if (reader == null) {
            readEncoding = Charset.defaultCharset();
            reader = new InputStreamReader(underlyingByteStream, readEncoding);
        } else if (reader instanceof InputStreamReader) {
            readEncoding = Charset.forName(((InputStreamReader) reader).getEncoding());
        }
        this.currentDelimiters = rootDelimiters;
    }

    /**
     * Try mark the stream so we can support changing of the reader encoding.
     *
     * @see #changeEncoding(Charset)
     */
    public void mark() {
        if (underlyingByteStream != null) {
            if (underlyingByteStream.markSupported()) {
                // We don't support reader changing after we've read MAX_MARK_READ bytes...
                underlyingByteStream.mark(MAX_MARK_READ);
                marked = true;
            } else {
                LOGGER.debug("Unable to mark EDI Reader for rest (to change reader encoding).  Underlying InputStream type '" + underlyingByteStream.getClass().getName() + "' does not support mark.");
            }
        } else {
            LOGGER.debug("Unable to mark EDI Reader for rest (to change reader encoding).  BufferedSegmentReader instance does not have access to the underlying InputStream.");
        }
    }

    /**
     * Change the encoding used to read the underlying EDI data stream.
     * <br><br>
     * {@link #mark()} should have been called first.
     *
     * @param encoding The new encoding.
     * @return The old/replaced encoding if known, otherwise null.
     * @throws IOException Failed to skip already read characters.
     */
    public Charset changeEncoding(Charset encoding) throws IOException {
        if (underlyingByteStream == null) {
            throw new IllegalStateException("Unable to change stream read encoding to '" + encoding + "'.  BufferedSegmentReader does not have access to the underlying stream.");
        }
        if (readEncoding != null && encoding.equals(readEncoding)) {
            return readEncoding;
        }
        if (!underlyingByteStream.markSupported()) {
            LOGGER.debug("Unable to to change stream read encoding on a stream that does not support 'mark'.");
            return readEncoding;
        }
        if (!marked) {
            LOGGER.debug("Unable to to change stream read encoding on a stream.  'mark' was not called, or was called and failed.");
            return readEncoding;
        }

        // reset the stream...
        try {
            underlyingByteStream.reset();
            marked = false;
        } catch (IOException e) {
            LOGGER.debug("Unable to to change stream read encoding on stream because reset failed.  Probably because the mark has been invalidated after reading more than " + MAX_MARK_READ + " bytes from the stream.", e);
            return readEncoding;
        }

        // Create a new reader and skip passed the already read characters...
        reader = new InputStreamReader(underlyingByteStream, encoding);
        underlyingByteStream.skip(charReadCount);
        try {
            return readEncoding;
        } finally {
            readEncoding = encoding;
        }
    }

    /**
     * Get the current delimiter set.
     *
     * @return the currentDelimiters The current delimiter set.
     */
    public Delimiters getDelimiters() {
        return currentDelimiters;
    }

    /**
     * Push in a new {@link Delimiters} set into the reader.
     *
     * @param delimiters New delimiters.
     */
    public void pushDelimiters(Delimiters delimiters) {
        delimitersStack.push(currentDelimiters);
        currentDelimiters = delimiters;
    }

    /**
     * Restore the parent delimiters set.
     * <br><br>
     * Be sure to {@link #getDelimitersStack() get the delimiters stack} and check
     * that it is not empty before popping.
     */
    public void popDelimiters() {
        currentDelimiters = delimitersStack.pop();
    }

    /**
     * Get the
     *
     * @return the delimitersStack
     */
    public Stack<Delimiters> getDelimitersStack() {
        return delimitersStack;
    }

    /**
     * Set ignore new lines in the EDI Stream.
     * <br><br>
     * Some EDI messages are formatted with new lines for readability and so the
     * new line characters should be ignored.
     *
     * @param ignoreNewLines True if new line characters should be ignored, otherwise false.
     */
    public void setIgnoreNewLines(boolean ignoreNewLines) {
        this.ignoreNewLines = ignoreNewLines;
    }

    /**
     * Read a fixed number of characters from the input source.
     *
     * @param numChars The number of characters to read.
     * @return The characters in a String.  If the end of the input source
     * was reached, the length of the string will be less than the requested number
     * of characters.
     * @throws IOException Error reading from input source.
     */
    public String read(int numChars) throws IOException {
        segmentBuffer.setLength(0);
        try {
            return peek(numChars);
        } finally {
            segmentBuffer.setLength(0);
        }
    }

    /**
     * Peek a fixed number of characters from the input source.
     * <br><br>
     * Peek differs from {@link #read(int)} in that it leaves the
     * characters in the segment buffer.
     *
     * @param numChars The number of characters to peeked.
     * @return The characters in a String.  If the end of the input source
     * was reached, the length of the string will be less than the requested number
     * of characters.
     * @throws IOException Error reading from input source.
     */
    public String peek(int numChars) throws IOException {
        return peek(numChars, false);
    }

    /**
     * Peek a fixed number of characters from the input source.
     * <br><br>
     * Peek differs from {@link #read(int)} in that it leaves the
     * characters in the segment buffer.
     *
     * @param numChars                The number of characters to peeked.
     * @param ignoreLeadingWhitespace Ignore leading whitespace.
     * @return The characters in a String.  If the end of the input source
     * was reached, the length of the string will be less than the requested number
     * of characters.
     * @throws IOException Error reading from input source.
     */
    public String peek(int numChars, boolean ignoreLeadingWhitespace) throws IOException {
        boolean ignoreCRLF;

        // Ignoring of new lines can be set as part of the segment delimiter, or
        // as a feature on the parser (the later is the preferred method)...
        ignoreCRLF = (currentDelimiters.ignoreCRLF() || ignoreNewLines);

        if (segmentBuffer.length() < numChars) {
            int c;

            if (ignoreLeadingWhitespace) {
                c = forwardPastWhitespace();
            } else {
                c = readChar();
            }

            while (c != -1) {
                if (ignoreCRLF && (c == '\n' || c == '\r')) {
                    c = readChar();
                    continue;
                }

                segmentBuffer.append((char) c);
                if (segmentBuffer.length() == numChars) {
                    break;
                }

                c = readChar();
            }
        }

        int endIndex = Math.min(numChars, segmentBuffer.length());

        return segmentBuffer.substring(0, endIndex);
    }

    /**
     * Set the segment listener.
     *
     * @param segmentListener The segment listener.
     */
    public void setSegmentListener(BufferedSegmentListener segmentListener) {
        this.segmentListener = segmentListener;
    }

    /**
     * Move to the next EDI segment.
     * <br><br>
     * Simply reads and buffers the next EDI segment.  Clears the current contents of
     * the buffer before reading.
     *
     * @return True if a "next" segment exists, otherwise false.
     * @throws IOException Error reading from EDI stream.
     */
    public boolean moveToNextSegment() throws IOException {
        return moveToNextSegment(true);
    }

    /**
     * Move to the next EDI segment.
     * <br><br>
     * Simply reads and buffers the next EDI segment.
     *
     * @param clearBuffer Clear the segment buffer before reading.
     * @return True if a "next" segment exists, otherwise false.
     * @throws IOException Error reading from EDI stream.
     */
    public boolean moveToNextSegment(boolean clearBuffer) throws IOException {
        char[] segmentDelimiter = currentDelimiters.getSegmentDelimiter();
        int delimiterLen = segmentDelimiter.length;
        String escape = currentDelimiters.getEscape();
        int escapeLen = escape != null ? escape.length() : 0;
        boolean ignoreCRLF;

        int c = readChar();

        // Ignoring of new lines can be set as part of the segment delimiter, or
        // as a feature on the parser (the later is the preferred method)...
        ignoreCRLF = (currentDelimiters.ignoreCRLF() || ignoreNewLines);

        if (clearBuffer) {
            segmentBuffer.setLength(0);
        }
        currentSegmentFields = null;

        // We reached the end of the stream the last time this method was
        // called - see the while loop below...
        if (c == -1) {
            return false;
        }

        // Ignore leading whitespace on a segment...
        c = forwardPastWhitespace(c);

        boolean escapingMode = false;

        // Read the next segment...
        while (c != -1) {
            char theChar = (char) c;

            if (ignoreCRLF && (theChar == '\n' || theChar == '\r')) {
                c = readChar();
                continue;
            }

            segmentBuffer.append((char) c);

            int segLen = segmentBuffer.length();
            if (segLen >= delimiterLen) {
                boolean reachedSegEnd = true;

                for (int i = 0; i < delimiterLen; i++) {
                    char segChar = segmentBuffer.charAt(segLen - 1 - i);
                    char delimChar = segmentDelimiter[delimiterLen - 1 - i];

                    if (escapingMode) {
                        if (segChar == delimChar) {
                            segmentBuffer = segmentBuffer.delete(segLen - 2, segLen - 1);
                        }
                        escapingMode = false;
                        reachedSegEnd = false;
                        break;
                    } else if (escape != null && escape.equals(Character.toString(segChar))) {
                        escapingMode = true;
                    }

                    if (segChar != delimChar) {
                        // Not the end of a segment
                        reachedSegEnd = false;
                        break;
                    }

                }

                // We've reached the end of a segment...
                if (reachedSegEnd) {
                    // Trim off the delimiter and break out...
                    segmentBuffer.setLength(segLen - delimiterLen);
                    break;
                }
            }

            c = readChar();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(segmentBuffer.toString());
        }

        currentSegmentNumber++;

        if (segmentListener != null) {
            return segmentListener.onSegment(this);
        } else {
            return true;
        }
    }

    /**
     * Does the read have a segment buffered and ready for processing.
     *
     * @return True if a current segment exists, otherwise false.
     */
    public boolean hasCurrentSegment() {
        if (segmentListener != null) {
            return segmentListener.onSegment(this);
        } else {
            return segmentBuffer.length() != 0;
        }
    }

    /**
     * Get the segment buffer.
     *
     * @return The segment buffer.
     */
    public StringBuffer getSegmentBuffer() {
        return segmentBuffer;
    }

    /**
     * Get the current EDI segment fields.
     *
     * @return The current EDI segment fields array.
     * @throws IllegalStateException No current Segment.
     */
    public String[] getCurrentSegmentFields() throws IllegalStateException {
        assertCurrentSegmentExists();

        if (currentSegmentFields == null) {
            currentSegmentFields = EDIUtils.split(segmentBuffer.toString(), currentDelimiters.getField(), currentDelimiters.getEscape());

            // If the segment delimiter is a LF, strip off any preceding CR characters...
            if (currentDelimiters.getSegment().equals("\n")) {
                int endIndex = currentSegmentFields.length - 1;
                if (currentSegmentFields[endIndex].endsWith("\r")) {
                    int stringLen = currentSegmentFields[endIndex].length();
                    currentSegmentFields[endIndex] = currentSegmentFields[endIndex].substring(0, stringLen - 1);
                }
            }
        }

        return currentSegmentFields;
    }

    /**
     * Get the current segment "number".
     * <br><br>
     * The first segment is "segment number 1".
     *
     * @return The "number" of the current segment.
     */
    public int getCurrentSegmentNumber() {
        return currentSegmentNumber;
    }

    private int forwardPastWhitespace() throws IOException {
        return forwardPastWhitespace(readChar());
    }

    private int forwardPastWhitespace(int c) throws IOException {
        while (c != -1) {
            if (!Character.isWhitespace((char) c)) {
                // It's not whitespace... stop here and start processing the segment...
                break;
            }

            // It's whitespace... move to next character in segment stream...
            c = readChar();
        }
        return c;
    }

    private int readChar() throws IOException {
        try {
            return reader.read();
        } finally {
            charReadCount++;
        }
    }

    /**
     * Assert that there is a current segment.
     */
    private void assertCurrentSegmentExists() {
        if (segmentBuffer.length() == 0) {
            throw new IllegalStateException("No current segment available.  Possible conditions: \n"
                    + "\t\t1. A call to moveToNextSegment() was not made, or \n"
                    + "\t\t2. The last call to moveToNextSegment() returned false.");
        }
    }
}
