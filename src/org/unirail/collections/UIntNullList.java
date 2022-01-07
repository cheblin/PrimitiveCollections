package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;

import static org.unirail.collections.Array.hash;

public interface UIntNullList {
	
	
	abstract class R  implements Cloneable{
		
		BitList.RW         nulls;
		UIntList.RW values;
		
		
		public int size()                                 {return nulls.size;}
		
		public boolean isEmpty()                          {return size() < 1;}
		
		public boolean hasValue(int index)                {return nulls.get(index);}
		
		@Positive_OK public int nextValueIndex(int index) {return nulls.next1(index);}
		
		@Positive_OK public int prevValueIndex(int index) {return nulls.prev1(index);}
		
		@Positive_OK public int nextNullIndex(int index)  {return nulls.next0(index);}
		
		@Positive_OK public int prevNullIndex(int index)  {return nulls.prev0(index);}
		
		public long get(@Positive_ONLY int index) {return (0xFFFFFFFFL &  values.get(nulls.rank(index) - 1));}
		
		
		public int indexOf()                              {return nulls.next0(0);}
		
		public int indexOf(long value) {
			int i = values.indexOf(value);
			return i < 0 ? i : nulls.bit(i);
		}
		
		
		public int lastIndexOf(long value) {
			int i = values.lastIndexOf(value);
			return i < 0 ? i : nulls.bit(i);
		}
		
		
		public boolean equals(Object obj) {
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals(getClass().cast(obj));
		}
		
		public int hashCode()          {return hash(hash(nulls), values);}
		
		
		public boolean equals(R other) {return other != null && other.size() == size() && values.equals(other.values) && nulls.equals(other.nulls);}
		
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
		
		
		public String toString() {
			final JsonWriter        json   = JsonWriter.get();
			final JsonWriter.Config config = json.enter();
			json.enterArray();
			int size = size();
			if (0 < size)
			{
				json.preallocate(size * 10);
				for (int i = 0, ii; i < size; )
					if ((ii = nextValueIndex(i)) == i) json.value(get(i++));
					else if (ii == -1 || size <= ii)
					{
						while (i++ < size) json.value();
						break;
					}
					else for (; i < ii; i++) json.value();
			}
			json.exitArray();
			return json.exit(config);
		}
	
		
		protected static void set(R dst, int index,  Integer   value) {
			
			if (value == null)
			{
				if (dst.size() <= index)
				{
					dst.nulls.set0(index);
					return;
				}
				
				if (!dst.nulls.get(index)) return;
				
				dst.values.remove(dst.nulls.rank(index));
				dst.nulls.remove(index);
			}
			else set(dst, index, (long) (value + 0));
		}
		
		protected static void set(R dst, int index, long value) {
			if (dst.nulls.get(index))
			{
				dst.values.set(dst.nulls.rank(index) - 1, value);
				return;
			}
			
			dst.nulls.set1(index);
			dst.values.add(dst.nulls.rank(index) - 1, value);
		}
	}
	
	
	class RW extends R {
		
		public RW(int length) {
			nulls = new BitList.RW(length);
			values = new UIntList.RW(length);
		}
		
		public RW( Integer   fill_value, int size) {
			this(size);
			if (fill_value == null) nulls.size = size;
			else
			{
				values.size = size;
				int v = (int) (fill_value + 0);
				while (-1 < --size) values.values[size] = v;
			}
		}
		
		
		public RW( Integer  ... values) {
			this(values.length);
			for ( Integer   value : values)
				if (value == null) nulls.add(false);
				else
				{
					this.values.add((long) (value + 0));
					nulls.add(true);
				}
		}
		
		public RW(long... values) {
			this(values.length);
			for (long value : values) this.values.add((long) value);
			
			nulls.set1(0, values.length - 1);
		}
		
		public RW(R src, int fromIndex, int toIndex) {
			
			nulls = new BitList.RW(src.nulls, fromIndex, toIndex);
			values = nulls.values.length == 0 ?
			         new UIntList.RW(0) :
			         new UIntList.RW(src.values, src.nulls.rank(fromIndex), src.nulls.rank(toIndex));
			
		}
		
		public void length(int length) {
			values.values = Arrays.copyOf(values.values, length);
			nulls.length(length);
		}
		
		public void fit() {
			values.values = Arrays.copyOf(values.values, values.size);
			nulls.fit();
		}
		
		public RW clone()    {return (RW) super.clone();}
		
		
		public void remove() {remove(size() - 1);}
		
		public void remove(int index) {
			if (size() < 1 || size() <= index) return;
			
			if (nulls.get(index)) values.remove(nulls.rank(index) - 1);
			nulls.remove(index);
		}
		
		public void add( Integer   value) {
			if (value == null) nulls.add(false);
			else add((long) (value + 0));
		}
		
		public void add(long value) {
			values.add(value);
			nulls.add(true);
		}
		
		
		public void add(int index,  Integer   value) {
			if (value == null) nulls.add(index, false);
			else add(index, (long) (value + 0));
		}
		
		public void add(int index, long value) {
			if (index < size())
			{
				nulls.add(index, true);
				values.add(nulls.rank(index) - 1, value);
			}
			else set(index, value);
		}
		
		public void set( Integer   value)            {set(this, size(), value);}
		
		public void set(long value)                {set(this, size(), value);}
		
		public void set(int index,  Integer   value) {set(this, index, value);}
		
		public void set(int index, long value)     {set(this, index, value);}
		
		public void set(int index, long... values) {
			for (int i = 0, max = values.length; i < max; i++)
				set(this, index + i, (long) values[i]);
		}
		
		public void set(int index,  Integer  ... values) {
			for (int i = 0, max = values.length; i < max; i++)
				set(this, index + i, values[i]);
		}
		
		public void addAll(R src) {
			
			for (int i = 0, s = src.size(); i < s; i++)
				if (src.hasValue(i)) add(src.get(i));
				else nulls.add(false);
		}
		
		public void clear() {
			values.clear();
			nulls.clear();
		}
		
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
			
			long v = values.get(exist);
			values.remove(exist);
			values.add(empty, v);
		}
	}
}
