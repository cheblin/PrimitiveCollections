// MIT License
//
// Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
// For inquiries, please contact: al8v5C6HU4UtqE9@gmail.com
// GitHub Repository: https://github.com/AdHoc-Protocol
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit others to do so, subject to the following conditions:
//
// 1. The above copyright notice and this permission notice must be included in all
//    copies or substantial portions of the Software.
//
// 2. Users of the Software must provide a clear acknowledgment in their user
//    documentation or other materials that their solution includes or is based on
//    this Software, prominently visible as:
//    "This product includes software developed by Chikirev Sirguy and the Unirail Group
//    (https://github.com/AdHoc-Protocol)."
//
// 3. Modified distributions of the Software must include a prominent notice stating
//    that changes have been made.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM,
// OUT OF, OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.unirail;

import org.unirail.collections.Array;
import org.unirail.collections.BitsList;

/**
 * A lightweight, high-performance JSON writer that generates human-readable JSON output.
 * <p>
 * This class provides a fluent API to construct JSON documents programmatically, supporting objects,
 * arrays, primitive types (e.g., strings, numbers, booleans, null), and custom {@link Source} objects
 * capable of self-serialization. It ensures structural integrity using a stack-based state machine
 * and offers pretty-printing with customizable indentation. Optional {@link Listener} callbacks enable
 * handling of large JSON outputs.
 * </p>
 * <p>
 * Thread-safe when retrieved via {@link #get()} for single-threaded use; instances should be managed carefully
 * within the same thread during {@link #enter()} and {@link #exit(Config)} operations to avoid concurrency issues.
 * Direct instantiation is also supported for single-threaded scenarios.
 * </p>
 */
public final class JsonWriter {

    /**
     * Interface for objects that can serialize themselves to JSON using this writer.
     */
    public interface Source {
        /**
         * Serializes this object to JSON using the provided writer.
         *
         * @param json The {@link JsonWriter} to write the JSON representation to.
         */
        void toJSON(JsonWriter json);

        /**
         * Serializes this object to a JSON string using default settings.
         * <p>
         * Retrieves a thread-local {@link JsonWriter}, serializes the object, and returns the result.
         * The writer is reset afterward for reuse.
         * </p>
         *
         * @return The JSON string representation of this object.
         */
        default String toJSON() {
            final JsonWriter json = get();
             json.reset();
            final Config config = json.enter();
            try {
                toJSON(json);
                return json.exit(config);
            } finally {
                json.reset();
            }
        }
    }

    // State machine constants for JSON structure tracking
    private static final int EMPTY_ARRAY = 0;      // Represents an empty array
    private static final int NONEMPTY_ARRAY = 1;   // Array with elements, expecting more
    private static final int EMPTY_OBJECT = 2;     // Represents an empty object
    private static final int DANGLING_NAME = 3;    // Object expecting a value for the current key
    private static final int NONEMPTY_OBJECT = 4;  // Object with key-value pairs, expecting more
    private static final int EMPTY_DOCUMENT = 5;   // Initial state before document starts
    private static final int NONEMPTY_DOCUMENT = 6;// Document with a root element

    /**
     * Stack tracking nesting levels and JSON document state.
     * Uses {@link BitsList.RW} for efficient integer state storage.
     */
    private final BitsList.RW stack = new BitsList.RW(3, 32) {
        @Override
        public String toString() {
            if (size == 0) return "[]";
            StringBuilder dst = new StringBuilder(size * 4);
            dst.append("[\n");
            long src = values[0];
            for ( int bp = 0, max = size * bits_per_item, i = 1; bp < max; bp += bits_per_item, i++) {
                final int bit = BitsList.R.bit(bp);
                long value = (BitsList.R.BITS < bit + bits_per_item ?
                        BitsList.R.value( src, src = values[BitsList.R.index(bp) + 1], bit, bits_per_item, mask) :
                        BitsList.R.value(src, bit, mask));
                dst.append('\t').append(value).append('\n');
            }
            dst.append("]");
            return dst.toString();
        }
    };

    // Initialize stack with the initial document state
    {
        stack.add1(EMPTY_DOCUMENT);
    }

    /**
     * Pre-allocates capacity in the output buffer to optimize performance for large JSON documents.
     *
     * @param chars The number of characters to reserve.
     */
    public void preallocate(int chars) {
        dst.ensureCapacity(dst.length() + chars);
    }

    /**
     * Starts a new JSON array. Must be closed with {@link #exitArray()}.
     *
     * @return This writer for method chaining.
     * @throws IllegalStateException If an array cannot be started in the current state.
     */
    public JsonWriter enterArray() {
        writeDeferredName();
        beforeValue();
        stack.add1(EMPTY_ARRAY);
        dst.append('[');
        return this;
    }

    /**
     * Ends the current JSON array.
     *
     * @return This writer for method chaining.
     * @throws IllegalStateException If not currently in an array.
     */
    public JsonWriter exitArray() {
        return exit(EMPTY_ARRAY, NONEMPTY_ARRAY, ']');
    }

    /**
     * Starts a new JSON object. Must be closed with {@link #exitObject()}.
     *
     * @return This writer for method chaining.
     * @throws IllegalStateException If an object cannot be started in the current state.
     */
    public JsonWriter enterObject() {
        writeDeferredName();
        beforeValue();
        stack.add1(EMPTY_OBJECT);
        dst.append('{');
        return this;
    }

    /**
     * Ends the current JSON object.
     *
     * @return This writer for method chaining.
     * @throws IllegalStateException If not currently in an object.
     */
    public JsonWriter exitObject() {
        return exit(EMPTY_OBJECT, NONEMPTY_OBJECT, '}');
    }

    /** Holds the key for the next object value, or null if none is pending. */
    private String deferredName;

    /**
     * Common logic to end arrays or objects, handling state transitions and closing brackets.
     *
     * @param empty    State for an empty structure (e.g., {@link #EMPTY_ARRAY}).
     * @param nonempty State for a non-empty structure (e.g., {@link #NONEMPTY_ARRAY}).
     * @param bracket  The closing bracket ('}' or ']').
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid or a key is dangling.
     */
    private JsonWriter exit(int empty, int nonempty, char bracket) {
        int context = stack.get();
        if (context != nonempty && context != empty)
            throw new IllegalStateException("Nesting error.\nStack: " + stack + "\nOutput: " + dst);
        if (deferredName != null)
            throw new IllegalStateException("Dangling key: " + deferredName + "\nStack: " + stack + "\nOutput: " + dst);
        stack.remove();
        if (context == nonempty) newline();
        dst.append(bracket);
        return this;
    }

    /** String used for null keys, configurable via {@link Config#null_name(String)}. Defaults to "🛑". */
    private String null_name = "🛑";

    /**
     * Sets a numeric key for the next object value as a string.
     *
     * @param name The key as a long value.
     * @return This writer for method chaining.
     * @throws IllegalStateException If a key is already pending or not in an object.
     */
    public JsonWriter name(long name) {
        return name(Long.toString(name));
    }

    /**
     * Sets a numeric key for the next object value as a string.
     *
     * @param name The key as a double value.
     * @return This writer for method chaining.
     * @throws IllegalStateException If a key is already pending or not in an object.
     */
    public JsonWriter name(double name) {
        return name(Double.toString(name));
    }

    /**
     * Sets a null key for the next object value, using {@link #null_name}.
     *
     * @return This writer for method chaining.
     * @throws IllegalStateException If a key is already pending or not in an object.
     */
    public JsonWriter name() {
        return name(null);
    }

    /**
     * Sets the key for the next object value. The next call must provide a value.
     *
     * @param name The key, or null to use {@link #null_name}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If a key is already pending or not in an object.
     */
    public JsonWriter name(String name) {
        if (name == null) name = null_name;
        if (deferredName != null)
            throw new IllegalStateException("Key already pending: " + deferredName + "\nOutput: " + dst);
        if (stack.size() == 0)
            throw new IllegalStateException("Writer is closed.\nOutput: " + dst);
        deferredName = name;
        return this;
    }

    /**
     * Writes a pending key to the output, preparing for its value.
     *
     * @throws IllegalStateException If not in an object or in an invalid state.
     */
    private void writeDeferredName() {
        if (deferredName == null) return;
        int context = stack.get();
        if (context == NONEMPTY_OBJECT) dst.append(',');
        else if (context != EMPTY_OBJECT)
            throw new IllegalStateException("Nesting error for key.\nStack: " + stack + "\nOutput: " + dst);
        newline();
        stack.set1(DANGLING_NAME);
        string(deferredName);
        deferredName = null;
    }

    /**
     * Writes a JSON {@code null} value.
     *
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value() {
        if (deferredName != null) {
            if (writeWithNullValue) writeDeferredName();
            else {
                deferredName = null;
                return this;
            }
        }
        beforeValue();
        dst.append("null");
        return this;
    }

    /**
     * Writes a boolean value.
     *
     * @param value The boolean to write.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(boolean value) {
        writeDeferredName();
        beforeValue();
        dst.append(value);
        return this;
    }

    /**
     * Writes a double value.
     *
     * @param value The double to write.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(double value) {
        writeDeferredName();
        beforeValue();
        dst.append(value);
        return this;
    }

    /**
     * Writes a long value.
     *
     * @param value The long to write.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(long value) {
        writeDeferredName();
        beforeValue();
        dst.append(value);
        return this;
    }

    /**
     * Writes a boolean array as a JSON array.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(boolean[] src) {
        if (src == null) return value();
        enterArray();
        for (boolean v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a byte array as a JSON array.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(byte[] src) {
        if (src == null) return value();
        enterArray();
        for (byte v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a short array as a JSON array.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(short[] src) {
        if (src == null) return value();
        enterArray();
        for (short v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a char array as a JSON array.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(char[] src) {
        if (src == null) return value();
        enterArray();
        for (char v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes an int array as a JSON array.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(int[] src) {
        if (src == null) return value();
        enterArray();
        for (int v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a long array as a JSON array.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(long[] src) {
        if (src == null) return value();
        enterArray();
        for (long v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a float array as a JSON array.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(float[] src) {
        if (src == null) return value();
        enterArray();
        for (float v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a double array as a JSON array.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(double[] src) {
        if (src == null) return value();
        enterArray();
        for (double v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes an object array as a JSON array, handling various types.
     * <p>
     * Supports {@link String}, {@link Source}, and generic objects via {@link Object#toString()}.
     * </p>
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(Object[] src) {
        if (src == null) return value();
        enterArray();
        if (src instanceof String[]) for (String v : (String[]) src) value(v);
        else if (src instanceof Source[]) for (Source v : (Source[]) src) value(v);
        else for (Object v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a string array as a JSON array.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(String[] src) {
        if (src == null) return value();
        enterArray();
        for (String v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a {@link Source} array as a JSON array.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(Source[] src) {
        if (src == null) return value();
        enterArray();
        for (Source v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D boolean array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(boolean[][] src) {
        if (src == null) return value();
        enterArray();
        for (boolean[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D byte array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(byte[][] src) {
        if (src == null) return value();
        enterArray();
        for (byte[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D short array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(short[][] src) {
        if (src == null) return value();
        enterArray();
        for (short[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D char array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(char[][] src) {
        if (src == null) return value();
        enterArray();
        for (char[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D int array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(int[][] src) {
        if (src == null) return value();
        enterArray();
        for (int[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D long array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(long[][] src) {
        if (src == null) return value();
        enterArray();
        for (long[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D float array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(float[][] src) {
        if (src == null) return value();
        enterArray();
        for (float[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D double array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(double[][] src) {
        if (src == null) return value();
        enterArray();
        for (double[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D string array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(String[][] src) {
        if (src == null) return value();
        enterArray();
        for (String[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D {@link Source} array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(Source[][] src) {
        if (src == null) return value();
        enterArray();
        for (Source[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Writes a 2D object array as a JSON array of arrays.
     *
     * @param src The array to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(Object[][] src) {
        if (src == null) return value();
        enterArray();
        for (Object[] v : src) value(v);
        exitArray();
        return this;
    }

    /**
     * Returns the current JSON output as a string.
     * <p>
     * This does not finalize the document; use {@link #exit(Config)} after {@link #enter()} for the final output.
     * </p>
     *
     * @return The current JSON string.
     */
    @Override
    public String toString() {
        return dst.toString();
    }

    /** Buffer for building JSON output, initialized with a capacity of 1024 characters. */
    public StringBuilder dst = new StringBuilder(1024);

    /**
     * Writes a {@link Number} value (e.g., Integer, Double).
     *
     * @param value The number to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(Number value) {
        if (value == null) return value();
        writeDeferredName();
        beforeValue();
        dst.append(value);
        return this;
    }

    /**
     * Writes a {@link Source} object by delegating to its serialization method.
     *
     * @param value The source object to write.
     * @return This writer for method chaining.
     * @throws NullPointerException If value is null.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(Source value) {
        if (value == null) throw new NullPointerException("Source value cannot be null");
        value.toJSON(this);
        return this;
    }

    /**
     * Writes an arbitrary object, handling various types.
     * <p>
     * Supports {@link String}, {@link Source}, arrays, and falls back to {@link Object#toString()}.
     * If the string resembles JSON, it’s appended directly; otherwise, it’s escaped as a string.
     * </p>
     *
     * @param value The object to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(Object value) {
        if (value == null) return value();
        if (value instanceof String) return value((String) value);
        if (value instanceof Source) ((Source) value).toJSON(this);
        else if (value instanceof Source[]) value((Source[]) value);
        else if (value instanceof Source[][]) value((Source[][]) value);
        else {
            String str = value.toString().trim();
            if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]")))
                dst.append(str);
            else if (value instanceof boolean[]) value((boolean[]) value);
            else if (value instanceof byte[]) value((byte[]) value);
            else if (value instanceof short[]) value((short[]) value);
            else if (value instanceof char[]) value((char[]) value);
            else if (value instanceof int[]) value((int[]) value);
            else if (value instanceof long[]) value((long[]) value);
            else if (value instanceof float[]) value((float[]) value);
            else if (value instanceof double[]) value((double[]) value);
            else if (value instanceof String[]) value((String[]) value);
            else if (value instanceof boolean[][]) value((boolean[][]) value);
            else if (value instanceof byte[][]) value((byte[][]) value);
            else if (value instanceof short[][]) value((short[][]) value);
            else if (value instanceof char[][]) value((char[][]) value);
            else if (value instanceof int[][]) value((int[][]) value);
            else if (value instanceof long[][]) value((long[][]) value);
            else if (value instanceof float[][]) value((float[][]) value);
            else if (value instanceof double[][]) value((double[][]) value);
            else if (value instanceof String[][]) value((String[][]) value);
            else if (value instanceof Object[][]) value((Object[][]) value);
            else if (value instanceof Object[]) value((Object[]) value);
            else value(str);
        }
        return this;
    }

    /**
     * Writes a string value with JSON escaping.
     *
     * @param value The string to write, or null to write {@code null}.
     * @return This writer for method chaining.
     * @throws IllegalStateException If the state is invalid.
     */
    public JsonWriter value(String value) {
        if (value == null) return value();
        writeDeferredName();
        beforeValue();
        string(value);
        return this;
    }

    /**
     * Escapes and writes a string according to JSON rules.
     *
     * @param value The string to escape and write.
     */
    private void string(String value) {
        dst.append('"');
        int last = 0;
        int length = value.length();
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            String replacement;
            switch (c) {
                case '"': replacement = "\\\""; break;
                case '\\': replacement = "\\\\"; break;
                case '\t': replacement = "\\t"; break;
                case '\b': replacement = "\\b"; break;
                case '\n': replacement = "\\n"; break;
                case '\r': replacement = "\\r"; break;
                case '\f': replacement = "\\f"; break;
                case '\u2028': replacement = "\\u2028"; break;
                case '\u2029': replacement = "\\u2029"; break;
                default:
                    if (c < 32) { replacement = REPLACEMENT_CHARS[c]; break; }
                    continue;
            }
            if (last < i) dst.append(value, last, i);
            dst.append(replacement);
            last = i + 1;
        }
        if (last < length) dst.append(value, last, length);
        dst.append('"');
    }

    /** Precomputed escape sequences for control characters (U+0000 to U+001F). */
    private static final String[] REPLACEMENT_CHARS = new String[32];

    static {
        for (int i = 0; i < 32; i++) REPLACEMENT_CHARS[i] = String.format("\\u%04x", i);
    }

    /**
     * Adds a newline and indentation based on nesting level for pretty printing.
     */
    private void newline() {
        dst.append('\n');
        for (int i = 1, size = stack.size(); i < size; i++) dst.append(indent);
    }

    /**
     * Prepares the output for a value by adding separators and updating state.
     * Notifies the {@link Listener} if the output exceeds the threshold.
     *
     * @throws IllegalStateException If the state is invalid.
     */
    private void beforeValue() {
        if (listener != null && listener_threshold < dst.length()) listener.notify(this);
        switch (stack.get()) {
            case NONEMPTY_DOCUMENT:
            case EMPTY_DOCUMENT:
                stack.set1(NONEMPTY_DOCUMENT);
                return;
            case EMPTY_ARRAY:
                stack.set1(NONEMPTY_ARRAY);
                break;
            case NONEMPTY_ARRAY:
                dst.append(',');
                break;
            case DANGLING_NAME:
                dst.append(": ");
                stack.set1(NONEMPTY_OBJECT);
                return;
            case NONEMPTY_OBJECT:
                dst.append(", ");
                stack.set1(NONEMPTY_OBJECT);
                return;
            default:
                throw new IllegalStateException("Nesting error.\nStack: " + stack + "\nOutput: " + dst);
        }
        newline();
    }

    /** Determines if null values are written in objects. Defaults to true. */
    private boolean writeWithNullValue = true;

    /** Reserved for future packing functionality (unused). */
    public long pack;

    /** Sets the pack value (reserved for future use). */
    public JsonWriter pack(long pack) {
        this.pack = pack;
        return this;
    }

    /** Sorting utilities (currently unused). */
    public final Array.ISort.Primitives.Index2 primitiveIndex = new Array.ISort.Primitives.Index2();
    public final Array.ISort.Anything.Index2 anythingIndex = new Array.ISort.Anything.Index2();

    /** Indentation string for pretty printing. Defaults to "\t"; null for compact output. */
    private String indent = "\t";
    /** Threshold for notifying the {@link Listener}. Defaults to 1024 characters. */
    private int listener_threshold = 1024;
    /** Callback for large output handling. */
    private Listener listener;

    /** Configuration instance for customizing this writer. */
    private final Config config = new Config() {
        @Override public JsonWriter listener(Listener listener, int threshold) {
            listener_threshold = threshold;
            JsonWriter.this.listener = listener;
            return JsonWriter.this;
        }
        @Override public JsonWriter indent(String indent) {
            JsonWriter.this.indent = indent;
            return JsonWriter.this;
        }
        @Override public JsonWriter writeWithNullValue(boolean write) {
            writeWithNullValue = write;
            return JsonWriter.this;
        }
        @Override public JsonWriter null_name(String null_name) {
            JsonWriter.this.null_name = null_name;
            return JsonWriter.this;
        }
    };

    /**
     * Interface for receiving notifications about large JSON output.
     */
    public interface Listener {
        /**
         * Called when the output exceeds the configured threshold.
         *
         * @param src The notifying {@link JsonWriter}.
         */
        void notify(JsonWriter src);
    }

    /**
     * Configuration interface for customizing writer behavior.
     */
    public interface Config {
        JsonWriter listener(Listener listener, int threshold);
        JsonWriter null_name(String null_name);
        JsonWriter indent(String indent);
        JsonWriter writeWithNullValue(boolean write);
    }

    /** Thread-local pool for providing per-thread instances. */
    private static final ThreadLocal<JsonWriter> threadLocal = ThreadLocal.withInitial(JsonWriter::new);

    /**
     * Retrieves a thread-local instance of this writer.
     *
     * @return A thread-local {@link JsonWriter}.
     */
    public static JsonWriter get() {
        return threadLocal.get();
    }

    /** Tracks if this instance is in use within an {@link #enter()}/{@link #exit(Config)} block. */
    private boolean in_use = false;

    /**
     * Begins a new JSON document, resetting the writer and marking it in use.
     *
     * @return A {@link Config} for customization, or null if already in use.
     */
    public Config enter() {
        if (in_use) return null;
        stack.set1(EMPTY_DOCUMENT);
        dst.setLength(0);
        in_use = true;
        return config;
    }

    /**
     * Finalizes the JSON document and returns the output.
     *
     * @param config The {@link Config} from {@link #enter()}.
     * @return The JSON string, or empty string if config mismatches or listener is used.
     */
    public String exit(Config config) {
        if (config != this.config) return "";
        in_use = false;
        if (listener != null) {
            listener.notify(this);
            listener = null;
            return "";
        }
        return dst.toString();
    }

    /**
     * Resets the writer to its initial state for reuse.
     *
     * @return This writer for method chaining.
     */
    public JsonWriter reset() {
        stack.clear();
        stack.add1(EMPTY_DOCUMENT);
        dst.setLength(0);
        in_use = false;
        listener = null;
        deferredName = null;
        return this;
    }

    /**
     * Configures the writer for compact JSON output (no indentation).
     *
     * @return This writer for method chaining.
     */
    public JsonWriter compactOutput() {
        this.indent = null;
        return this;
    }

    /**
     * Configures the writer for pretty-printed JSON with tab indentation.
     *
     * @return This writer for method chaining.
     */
    public JsonWriter prettyPrintOutput() {
        this.indent = "\t";
        return this;
    }

    /**
     * Sets the threshold for listener notifications.
     *
     * @param threshold The character length threshold.
     * @return This writer for method chaining.
     */
    public JsonWriter listenerThreshold(int threshold) {
        this.listener_threshold = threshold;
        return this;
    }

    /**
     * Sets the listener for large output notifications.
     *
     * @param listener The listener, or null to remove it.
     * @return This writer for method chaining.
     */
    public JsonWriter outputListener(Listener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Configures whether to include null values in objects.
     *
     * @param writeNullValue True to write nulls, false to skip them.
     * @return This writer for method chaining.
     */
    public JsonWriter writeNullValues(boolean writeNullValue) {
        this.writeWithNullValue = writeNullValue;
        return this;
    }

    /**
     * Sets the string to use for null keys.
     *
     * @param nullName The string for null keys.
     * @return This writer for method chaining.
     */
    public JsonWriter nullNameString(String nullName) {
        this.null_name = nullName;
        return this;
    }

    /**
     * Sets the indentation string for pretty printing.
     *
     * @param indentString The indentation string (e.g., "\t", "  ").
     * @return This writer for method chaining.
     */
    public JsonWriter indentString(String indentString) {
        this.indent = indentString;
        return this;
    }
}