package org.unirail;

import org.unirail.collections.Array;
import org.unirail.collections.BitsList;

public final class JsonWriter {
	
	public interface Source {
		void toJSON(JsonWriter json);
		
		default String toJSON() {
			final JsonWriter        json   = get();
			final JsonWriter.Config config = json.enter();
			toJSON(json);
			return json.exit(config);
		}
	}
	
	/**
	 An array with no elements requires no separators or newlines before
	 it is closed.
	 */
	private static final int EMPTY_ARRAY = 0;
	
	/**
	 A array with at least one value requires a comma and newline before
	 the next element.
	 */
	private static final int NONEMPTY_ARRAY = 1;
	
	/**
	 An object with no name/value pairs requires no separators or newlines
	 before it is closed.
	 */
	private static final int EMPTY_OBJECT = 2;
	
	/**
	 An object whose most recent element is a key. The next element must
	 be a value.
	 */
	private static final int DANGLING_NAME = 3;
	
	/**
	 An object with at least one name/value pair requires a comma and
	 newline before the next element.
	 */
	private static final int NONEMPTY_OBJECT = 4;
	
	/**
	 No object or array has been started.
	 */
	private static final int EMPTY_DOCUMENT = 5;
	
	/**
	 A document with at an array or object.
	 */
	private static final int NONEMPTY_DOCUMENT = 6;
	
	private final BitsList.RW stack = new BitsList.RW(3, 32);
	
	{stack.add(EMPTY_DOCUMENT);}
	
	public void preallocate(int chars) {dst.ensureCapacity(dst.length() + chars);}
	
	
	public JsonWriter enterArray() {
		writeDeferredName();
		beforeValue();
		stack.add(EMPTY_ARRAY);
		dst.append('[');
		return this;
	}
	
	
	public JsonWriter exitArray() {return exit(EMPTY_ARRAY, NONEMPTY_ARRAY, ']');}
	
	
	public JsonWriter enterObject() {
		writeDeferredName();
		beforeValue();
		stack.add(EMPTY_OBJECT);
		dst.append('{');
		return this;
	}
	
	public JsonWriter exitObject() {return exit(EMPTY_OBJECT, NONEMPTY_OBJECT, '}');}
	
	private String deferredName;
	
	private JsonWriter exit(int empty, int nonempty, char bracket) {
		int context = stack.get();
		if (context != nonempty && context != empty) throw new IllegalStateException("Nesting problem.\n" + dst);
		if (deferredName != null) throw new IllegalStateException("Dangling name: " + deferredName + "\n" + dst);
		stack.remove();
		if (context == nonempty) newline();
		dst.append(bracket);
		return this;
	}
	
	private String null_name = "␀";
	
	public JsonWriter name(long name)   {return name(Long.toString(name));}
	
	public JsonWriter name(double name) {return name(Double.toString(name));}
	public JsonWriter name()            {return name(null);}
	
	public JsonWriter name(String name) {
		if (name == null) name = null_name;
		if (deferredName != null) throw new IllegalStateException(dst.toString());
		if (stack.size() == 0) throw new IllegalStateException("JsonWriter is closed.\n" + dst);
		deferredName = name;
		return this;
	}
	
	private void writeDeferredName() {
		if (deferredName == null) return;
		int context = stack.get();
		if (context == NONEMPTY_OBJECT) dst.append(','); // first in object
		else // not in an object!
			if (context != EMPTY_OBJECT) throw new IllegalStateException("Nesting problem.\n" + dst);
		newline();
		stack.set(DANGLING_NAME);
		string(deferredName);
		deferredName = null;
	}
	
	public JsonWriter value() {
		if (deferredName != null) if (writeWithNullValue) writeDeferredName();
		else
		{
			deferredName = null;
			return this;
		}
		beforeValue();
		dst.append("null");
		return this;
	}
	
	public JsonWriter value(boolean value) {
		writeDeferredName();
		beforeValue();
		dst.append(value);
		return this;
	}
	
	
	public JsonWriter value(double value) {
		writeDeferredName();
		beforeValue();
		dst.append(value);
		return this;
	}
	
	public JsonWriter value(long value) {
		writeDeferredName();
		beforeValue();
		dst.append(value);
		return this;
	}
	
	
	public JsonWriter value(boolean[] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (boolean v : src) value(v);
		exitArray();
		return this;
	}
	
	public JsonWriter value(byte[] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (byte v : src) value(v);
		exitArray();
		return this;
	}
	
	public JsonWriter value(short[] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (short v : src) value(v);
		exitArray();
		return this;
	}
	
	public JsonWriter value(char[] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (char v : src) value(v);
		exitArray();
		return this;
	}
	
	public JsonWriter value(int[] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (int v : src) value(v);
		exitArray();
		return this;
	}
	
	public JsonWriter value(long[] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (long v : src) value(v);
		exitArray();
		return this;
	}
	
	public JsonWriter value(float[] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (float v : src) value(v);
		exitArray();
		return this;
	}
	
	public JsonWriter value(double[] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (double v : src) value(v);
		exitArray();
		return this;
	}
	public JsonWriter value(Object[] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		if (src instanceof String[]) for (String v : (String[]) src) value(v);
		else if (src instanceof Source[])
			for (Source v : (Source[]) src)
				if (v == null) value();
				else v.toJSON(this);
		else
			for (Object v : src) value(v);
		
		exitArray();
		return this;
	}
	
	public JsonWriter value(String[] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (String v : src) value(v);
		exitArray();
		return this;
	}
	
	public JsonWriter value(boolean[][] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (boolean[] v : src) value(v);
		exitArray();
		return this;
	}
	public JsonWriter value(byte[][] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (byte[] v : src) value(v);
		exitArray();
		return this;
	}
	public JsonWriter value(short[][] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (short[] v : src) value(v);
		exitArray();
		return this;
	}
	public JsonWriter value(char[][] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (char[] v : src) value(v);
		exitArray();
		return this;
	}
	public JsonWriter value(int[][] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (int[] v : src) value(v);
		exitArray();
		return this;
	}
	public JsonWriter value(long[][] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (long[] v : src) value(v);
		exitArray();
		return this;
	}
	public JsonWriter value(float[][] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (float[] v : src) value(v);
		exitArray();
		return this;
	}
	public JsonWriter value(double[][] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (double[] v : src) value(v);
		exitArray();
		return this;
	}
	public JsonWriter value(String[][] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (String[] v : src) value(v);
		exitArray();
		return this;
	}
	public JsonWriter value(Object[][] src) {
		if (src == null)
		{
			value();
			return this;
		}
		enterArray();
		for (Object[] v : src) value(v);
		exitArray();
		return this;
	}
	
	@Override public String toString() {return dst.toString();}
	public StringBuilder dst = new StringBuilder(1024);
	
	
	public JsonWriter value(Number value) {
		if (value == null) return value();
		writeDeferredName();
		beforeValue();
		dst.append(value);
		return this;
	}
	
	public JsonWriter value(Object value) {
		if (value == null) return value();
		if (value instanceof String) return value((String) value);
		
		if (value instanceof Source) ((Source) value).toJSON(this);
		else
		{
			String str = value.toString().trim();
			if (str.startsWith("{") && str.endsWith("}") ||
			    str.startsWith("[") && str.endsWith("]"))
				dst.append(str);
			else
				value(value.getClass() + ".toString() produce " + str + " instead of JSON.");
		}
		
		return this;
	}
	
	public JsonWriter value(String value) {
		if (value == null) return value();
		writeDeferredName();
		beforeValue();
		string(value);
		return this;
	}
	
	private void string(String value) {
		dst.append('"');
		
		int last   = 0;
		int length = value.length();
		for (int i = 0; i < length; i++)
		{
			char   c = value.charAt(i);
			String replacement;
			switch (c)
			{
				case '"':
					replacement = "\\\"";
					break;
				case '\\':
					replacement = "\\\\";
					break;
				case '\t':
					replacement = "\\t";
					break;
				case '\b':
					replacement = "\\b";
					break;
				case '\n':
					replacement = "\\n";
					break;
				case '\r':
					replacement = "\\r";
					break;
				case '\f':
					replacement = "\\f";
					break;
				case '\u2028'://LINE SEPARATOR
					replacement = "\\u2028";
					break;
				case '\u2029'://PARAGRAPH SEPARATOR
					replacement = "\\u2029";
					break;
				default:
					if (c < 32)
					{
						replacement = REPLACEMENT_CHARS[c];
						break;
					}
					continue;
			}
			if (last < i) dst.append(value, last, i);
			dst.append(replacement);
			last = i + 1;
		}
		
		if (last < length) dst.append(value, last, length);
		
		dst.append('"');
	}
	
	private static final String[] REPLACEMENT_CHARS = new String[32];
	
	static
	{
		for (int i = 0; i < 32; i++) REPLACEMENT_CHARS[i] = String.format("\\u%04x", i);
	}
	
	private void newline() {
		dst.append('\n');
		for (int i = 1, size = stack.size(); i < size; i++) dst.append(indent);
	}
	
	
	/**
	 Inserts any necessary separators and whitespace before a literal value,
	 inline array, or inline object. Also adjusts the stack to expect either a
	 closing bracket or another element.
	 */
	private void beforeValue() {
		if (listener != null && listener_threshold < dst.length()) listener.notify(this);
		switch (stack.get())
		{
			case NONEMPTY_DOCUMENT:
			case EMPTY_DOCUMENT: // first in document
				stack.set(NONEMPTY_DOCUMENT);
				return;
			case EMPTY_ARRAY: // first in array
				stack.set(NONEMPTY_ARRAY);
				break;
			case NONEMPTY_ARRAY: // another in array
				dst.append(',');
				break;
			case DANGLING_NAME: // value for name
				dst.append(": ");
				stack.set(NONEMPTY_OBJECT);
				return;
			default:
				throw new IllegalStateException("Nesting problem.\n" + dst);
		}
		newline();
	}
	/**
	 Sets whether object members are serialized when their value is null.
	 This has no impact on array elements. The default is true.
	 */
	private boolean writeWithNullValue = true;
	
	public final Array.ISort.Primitives.Index2 primitiveIndex = new Array.ISort.Primitives.Index2();
	public final Array.ISort.Anything.Index2   anythingIndex  = new Array.ISort.Anything.Index2();
	
	/**
	 A string containing a full set of spaces for a single level of
	 indentation, or null for no pretty printing.
	 */
	private String   indent             = "\t";
	private int      listener_threshold = 1024;
	private Listener listener;
	
	private final Config config = new Config() {
		@Override public JsonWriter listener(Listener listener, int threshold) {
			listener_threshold       = threshold;
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
	
	public interface Listener {
		void notify(JsonWriter src);
	}
	
	public interface Config {
		JsonWriter listener(Listener listener, int threshold);
		
		JsonWriter null_name(String null_name);
		
		JsonWriter indent(String indent);
		
		JsonWriter writeWithNullValue(boolean write);
	}
	
	private static final ThreadLocal<JsonWriter> threadLocal = ThreadLocal.withInitial(JsonWriter::new);
	
	public static JsonWriter get() {return threadLocal.get();}
	private boolean in_use = false;
	
	public Config enter() {
		if (in_use) return null;
		stack.set(EMPTY_DOCUMENT);
		dst.setLength(0);
		in_use = true;
		return config;
	}
	
	public String exit(Config config) {
		if (config != this.config) return "";
		in_use = false;
		if (listener != null)
		{
			listener.notify(this);
			listener = null;
			return "";
		}
		return dst.toString();
	}
}