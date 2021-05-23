package org.unirail.collections;


public interface DoubleNullList {
	interface IDst {
		void add(double value);
		
		void add( Double    value);
		
		void write(int size);
	}
	
	interface ISrc {
		
		int size();
		
		@Positive_OK int nextValueIndex(int index);
		
		double get(@Positive_ONLY int index);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 4);
			else dst.ensureCapacity(dst.length() + size * 64);
			
			for (int i = 0, n; i < size; dst.append(get(i)).append('\n'), i++)
				for (n = nextValueIndex(i); i < n; i++) dst.append("null\n");
			
			return dst;
		}
	}
	
	
	abstract class R implements Comparable<R>, ISrc {
		
		BitList.RW         nulls;
		DoubleList.RW values;
		
		
		public int length() {return values.length();}
		
		int size = 0;
		
		public int size()                                 { return size; }
		
		public boolean isEmpty()                          { return size < 1; }
		
		public boolean hasValue(int index)                {return nulls.get(index);}
		
		@Positive_OK public int nextValueIndex(int index) {return nulls.next1(index);}
		
		@Positive_OK public int prevValueIndex(int index) {return nulls.prev1(index);}
		
		@Positive_OK public int nextNullIndex(int index)  {return nulls.next0(index);}
		
		@Positive_OK public int prevNullIndex(int index)  {return nulls.prev0(index);}
		
		public double get(@Positive_ONLY int index) {return  values.array[nulls.rank(index) - 1]; }
		
		
		public int indexOf()                              { return nulls.next0(0); }
		
		public int indexOf(double value) {
			int i = values.indexOf(value);
			return i < 0 ? i : nulls.bit(i);
		}
		
		public int lastIndexOf() { return nulls.prev0(size);}
		
		public int lastIndexOf(double value) {
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
			for (int i = 0; i < size; i++) hashCode = 31 * hashCode + (hasValue(i) ? 0 : Array.hash(get(i)));
			
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
		
		protected static void set(R dst, int index,  Double    value) {
			
			if (value == null)
			{
				if (dst.size <= index) dst.size = index + 1;
				if (!dst.nulls.get(index)) return;
				dst.nulls.remove(index);
				dst.values.remove(dst.nulls.rank(index));
			}
			else set(dst, index, (double) (value + 0));
		}
		
		protected static void set(R dst, int index, double value) {
			if (dst.size <= index) dst.size = index + 1;
			
			if (dst.nulls.get(index)) dst.values.set(dst.nulls.rank(index) - 1, value);
			else
			{
				dst.nulls.set1(index);
				dst.values.add(dst.nulls.rank(index) - 1, value);
			}
		}
	}
	
	
	class RW extends R implements IDst {
		
		public RW(int length) {
			nulls = new BitList.RW(length);
			values = new DoubleList.RW(length);
		}
		
		public RW( Double   ... values) {
			this(values.length);
			for ( Double    value : values)
				if (value == null) size++;
				else
				{
					this.values.add((double) (value + 0));
					nulls.set1(size);
					size++;
				}
		}
		
		public RW(double... values) {
			this(values.length);
			for (double value : values) this.values.add((double) value);
			
			size = values.length;
			nulls.set1(0, size - 1);
		}
		
		public RW(R src, int fromIndex, int toIndex) {
			
			nulls = new BitList.RW(src.nulls, fromIndex, toIndex);
			values = nulls.array.length == 0 ?
			         new DoubleList.RW(0) :
			         new DoubleList.RW(src.values, src.nulls.rank(fromIndex), src.nulls.rank(toIndex));
			
		}
		
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
			
			if (nulls.get(index)) values.remove(nulls.rank(index) - 1);
			nulls.remove(index);
		}
		
		public void add( Double    value) {
			if (value == null) size++;
			else add((double) (value + 0));
		}
		
		public void add(double value) {
			values.add(value);
			nulls.add(size, true);
			size++;
		}
		
		
	
		
		public void add(int index,  Double    value) {
			if (value == null)
			{
				nulls.add(index, false);
				size++;
			}
			else add(index, (double) (value + 0));
		}
		
		public void add(int index, double value) {
			if (index < size)
			{
				nulls.add(index, true);
				values.add(nulls.rank(index) - 1, value);
				size++;
			}
			else set(index, value);
		}
		
		public void set( Double    value)            { set(this, size, value); }
		
		public void set(double value)                {set(this, size, value); }
		
		public void set(int index,  Double    value) { set(this, index, value); }
		
		public void set(int index, double value)     {set(this, index, value); }
		
		public void set(int index, double... values) {
			for (int i = 0, max = values.length; i < max; i++)
				set(this, index + i, (double) values[i]);
		}
		
		public void set(int index,  Double   ... values) {
			for (int i = 0, max = values.length; i < max; i++)
				set(this, index + i, values[i]);
		}
		
		public int addAll(R src) {
			int fix = size;
			
			for (int i = 0, s = src.size(); i < s; i++)
				if (src.hasValue(i)) add(src.get(i));
				else size++;
			
			return size - fix;
		}
		
		public void clear() {
			values.clear();
			nulls.clear();
			size = 0;
		}
		//region  IDst
		@Override public void write(int size) {
			values.write(size);
			nulls.write(size);
			nulls.clear();//!!!
			this.size = 0;
		}
		//endregion
		public void swap(int index1, int index2) {
			
			int exist, empty;
			if (nulls.get(index1))
				if (nulls.get(index2))
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
			else if (nulls.get(index2))
			{
				exist = nulls.rank(index2) - 1;
				empty = nulls.rank(index1);
				
				nulls.set1(index1);
				nulls.set0(index2);
			}
			else return;
			
			double v = values.get(exist);
			values.remove(exist);
			values.add(empty, v);
		}
	}
}
