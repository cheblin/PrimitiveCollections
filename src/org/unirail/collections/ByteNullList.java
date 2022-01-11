package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;

import static org.unirail.collections.Array.hash;

public interface ByteNullList {
	
	
	abstract class R  implements Cloneable , JsonWriter.Source {
		
		BitList.RW         nulls;
		ByteList.RW values;
		
		
		public int size()                                 {return nulls.size;}
		
		public boolean isEmpty()                          {return size() < 1;}
		
		public boolean hasValue(int index)                {return nulls.get(index);}
		
		@Positive_OK public int nextValueIndex(int index) {return nulls.next1(index);}
		
		@Positive_OK public int prevValueIndex(int index) {return nulls.prev1(index);}
		
		@Positive_OK public int nextNullIndex(int index)  {return nulls.next0(index);}
		
		@Positive_OK public int prevNullIndex(int index)  {return nulls.prev0(index);}
		
		public byte get(@Positive_ONLY int index) {return (byte) values.get(nulls.rank(index) - 1);}
		
		
		public int indexOf()                              {return nulls.next0(0);}
		
		public int indexOf(byte value) {
			int i = values.indexOf(value);
			return i < 0 ? i : nulls.bit(i);
		}
		
		
		public int lastIndexOf(byte value) {
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
		
		
		
public String toString() {return toJSON();}
		@Override public void toJSON(JsonWriter json)  {
			
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
		}
	
		
		protected static void set(R dst, int index,  Byte      value) {
			
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
			else set(dst, index, (byte) (value + 0));
		}
		
		protected static void set(R dst, int index, byte value) {
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
			values = new ByteList.RW(length);
		}
		
		public RW( Byte      fill_value, int size) {
			this(size);
			if (fill_value == null) nulls.size = size;
			else
			{
				values.size = size;
				byte v = (byte) (fill_value + 0);
				while (-1 < --size) values.values[size] = v;
			}
		}
		
		
		public RW( Byte     ... values) {
			this(values.length);
			for ( Byte      value : values)
				if (value == null) nulls.add(false);
				else
				{
					this.values.add((byte) (value + 0));
					nulls.add(true);
				}
		}
		
		public RW(byte... values) {
			this(values.length);
			for (byte value : values) this.values.add((byte) value);
			
			nulls.set1(0, values.length - 1);
		}
		
		public RW(R src, int fromIndex, int toIndex) {
			
			nulls = new BitList.RW(src.nulls, fromIndex, toIndex);
			values = nulls.values.length == 0 ?
			         new ByteList.RW(0) :
			         new ByteList.RW(src.values, src.nulls.rank(fromIndex), src.nulls.rank(toIndex));
			
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
		
		public void add( Byte      value) {
			if (value == null) nulls.add(false);
			else add((byte) (value + 0));
		}
		
		public void add(byte value) {
			values.add(value);
			nulls.add(true);
		}
		
		
		public void add(int index,  Byte      value) {
			if (value == null) nulls.add(index, false);
			else add(index, (byte) (value + 0));
		}
		
		public void add(int index, byte value) {
			if (index < size())
			{
				nulls.add(index, true);
				values.add(nulls.rank(index) - 1, value);
			}
			else set(index, value);
		}
		
		public void set( Byte      value)            {set(this, size(), value);}
		
		public void set(byte value)                {set(this, size(), value);}
		
		public void set(int index,  Byte      value) {set(this, index, value);}
		
		public void set(int index, byte value)     {set(this, index, value);}
		
		public void set(int index, byte... values) {
			for (int i = 0, max = values.length; i < max; i++)
				set(this, index + i, (byte) values[i]);
		}
		
		public void set(int index,  Byte     ... values) {
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
			
			byte v = values.get(exist);
			values.remove(exist);
			values.add(empty, v);
		}
	}
}
