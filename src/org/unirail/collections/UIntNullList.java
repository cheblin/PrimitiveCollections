package org.unirail.collections;


public interface UIntNullList {
	interface Consumer {
		void add(long value);
		
		void add( Integer   value);
		
		void write(int size);
	}
	
	interface Producer {
		
		int size();
		
		@Positive_OK int nextValueIndex(int index);
		
		long value(@Positive_ONLY int index);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 4);
			else dst.ensureCapacity(dst.length() + size * 64);
			
			for (int i = 0, n; i < size; dst.append(value(i)).append('\n'), i++)
				for (n = nextValueIndex(i); i < n; i++) dst.append("null\n");
			
			return dst;
		}
	}
	
	
	class R implements Comparable<R>, Producer {
		
		BitList.RW         nulls;
		UIntList.RW values;
		
		protected R(int length) {
			nulls = new BitList.RW(length);
			values = new UIntList.RW(length);
		}
		
		public R( Integer  ... values) {
			this(values.length);
			fill(this, values);
		}
		
		public R(long... values) {
			this(values.length);
			fill(this, values);
		}
		
		public R(R src, int fromIndex, int toIndex) {
			
			nulls = new BitList.RW(src.nulls, fromIndex, toIndex);
			values = nulls.array.length == 0 ?
			         new UIntList.RW(0) :
			         new UIntList.RW(src.values, src.nulls.rank(fromIndex), src.nulls.rank(toIndex));
			
		}
		
		protected static void fill(R dst,  Integer  ... values) {
			for ( Integer   value : values)
				if (value == null) dst.size++;
				else
				{
					dst.values.add((long) (value + 0));
					dst.nulls.set1(dst.size);
					dst.size++;
				}
		}
		
		protected static void fill(R dst, long... values) {
			for (long value : values) dst.values.add((long) value);
			
			dst.size = values.length;
			dst.nulls.set1(0, dst.size - 1);
		}
		
		public int length() {return values.length();}
		
		int size = 0;
		
		public int size()                                 { return size; }
		
		public boolean isEmpty()                          { return size < 1; }
		
		public boolean hasValue(int index)                {return nulls.value(index);}
		
		@Positive_OK public int nextValueIndex(int index) {return nulls.next1(index);}
		
		@Positive_OK public int prevValueIndex(int index) {return nulls.prev1(index);}
		
		@Positive_OK public int nextNullIndex(int index)  {return nulls.next0(index);}
		
		@Positive_OK public int prevNullIndex(int index)  {return nulls.prev0(index);}
		
		public long value( @Positive_ONLY int index) {return (0xFFFFFFFFL &  values.array[nulls.rank(index) - 1]); }
		
		
		public int indexOf()                              { return nulls.next0(0); }
		
		public int indexOf(long value) {
			int i = values.indexOf(value);
			return i < 0 ? i : nulls.bit(i);
		}
		
		public int lastIndexOf() { return nulls.prev0(size);}
		
		public int lastIndexOf(long value) {
			int i = values.lastIndexOf(value);
			return i < 0 ? i : nulls.bit(i);
		}
		
		
		public boolean equals(Object obj) {
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public int hashCode() {
			int hashCode = 1;
			for (int i = 0; i < size; i++) hashCode = 31 * hashCode + (hasValue(i) ? 0 : Array.hash(value(i)));
			
			return hashCode;
		}
		
		
		public boolean equals(R other) { return other != null && compareTo(other) == 0; }
		
		public int compareTo(R other) {
			if (other == null) return -1;
			if (other.size != size) return other.size - size;
			
			int diff;
			
			if ((diff = values.compareTo(other.values)) != 0 || (diff = nulls.compareTo(other.nulls)) != 0) return diff;
			
			return 0;
		}
		
		public R clone() {
			try
			{
				R dst = (R) super.clone();
				dst.values = values.clone();
				dst.nulls = nulls.clone();
				return dst;
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		public String toString() { return toString(null).toString();}
		
		protected static void set(R dst, int index,  Integer   value) {
			
			if (value == null)
			{
				if (dst.size <= index) dst.size = index + 1;
				if (!dst.nulls.value(index)) return;
				dst.nulls.remove(index);
				dst.values.remove(dst.nulls.rank(index));
			}
			else set(dst, index, (long) (value + 0));
		}
		
		protected static void set(R dst, int index, long value) {
			if (dst.size <= index) dst.size = index + 1;
			
			if (dst.nulls.value(index)) dst.values.set(dst.nulls.rank(index) - 1, value);
			else
			{
				dst.nulls.set1(index);
				dst.values.add(dst.nulls.rank(index) - 1, value);
			}
		}
	}
	
	
	class RW extends R implements Consumer {
		
		public RW(int length)                        { super(length); }
		
		public RW( Integer  ... values)         { super(values); }
		
		
		public RW(long... values)             { super(values); }
		
		public RW(R src, int fromIndex, int toIndex) { super(src, fromIndex, toIndex); }
		
		public void length(int length) {
			values.length(length);
			nulls.length(length);
		}
		
		public void fit() {
			values.length(values.size);
			nulls.fit();
		}
		
		public RW clone()    { return (RW) super.clone(); }
		
		
		public void remove() { remove(size - 1); }
		
		public void remove(int index) {
			if (size < 1 || size <= index) return;
			
			size--;
			
			if (nulls.value(index)) values.remove(nulls.rank(index) - 1);
			nulls.remove(index);
		}
		
		public void add( Integer   value) {
			if (value == null) size++;
			else add((long) (value + 0));
		}
		
		public void add(long value) {
			values.add(value);
			nulls.add(size, true);
			size++;
		}
		
		
	
		
		public void add(int index,  Integer   value) {
			if (value == null)
			{
				nulls.add(index, false);
				size++;
			}
			else add(index, (long) (value + 0));
		}
		
		public void add(int index, long value) {
			if (index < size)
			{
				nulls.add(index, true);
				values.add(nulls.rank(index) - 1, value);
				size++;
			}
			else set(index, value);
		}
		
		public void set( Integer   value)            { set(this, size, value); }
		
		public void set(long value)                {set(this, size, value); }
		
		public void set(int index,  Integer   value) { set(this, index, value); }
		
		public void set(int index, long value)     {set(this, index, value); }
		
		public void set(int index, long... values) {
			for (int i = 0, max = values.length; i < max; i++)
				set(this, index + i, (long) values[i]);
		}
		
		public void set(int index,  Integer  ... values) {
			for (int i = 0, max = values.length; i < max; i++)
				if (values[i] == null) R.set(this, index + i, null);
				else set(this, index + i, (long) (values[i] + 0));
		}
		
		public int addAll(R src) {
			int fix = size;
			
			for (int i = 0, s = src.size(); i < s; i++)
				if (src.hasValue(i)) add(src.value(i));
				else size++;
			
			return size - fix;
		}
		
		public void clear() {
			values.clear();
			nulls.clear();
			size = 0;
		}
		//region  consumer
		@Override public void write(int size) {
			values.write(size);
			nulls.write(size);
			nulls.clear();//!!!
			this.size = 0;
		}
		//endregion
		public void swap(int index1, int index2) {
			
			int exist, empty;
			if (nulls.value(index1))
				if (nulls.value(index2))
				{
					values.swap(nulls.rank(index1) - 1, nulls.rank(index2) - 1);
					return;
				}
				else
				{
					exist = nulls.rank(index1) - 1;
					empty = nulls.rank(index2);
					nulls.set0(index1);
					nulls.set1(index2);
				}
			else if (nulls.value(index2))
			{
				exist = nulls.rank(index2) - 1;
				empty = nulls.rank(index1);
				
				nulls.set1(index1);
				nulls.set0(index2);
			}
			else return;
			
			long v = values.value(exist);
			values.remove(exist);
			values.add(empty, v);
		}
	}
}
