package org.unirail.collections;


import static org.unirail.collections.BitsList.*;

public interface BitsNullList {
	
	interface IDst {
		void write(int index, long src);
		
		void write(long null_val, long shift, int size, int bits);
	}
	
	interface ISrc extends BitsList.ISrc {
		long null_val();
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 4);
			else dst.ensureCapacity(dst.length() + size * 4);
			
			int  bits     = bits();
			long shift    = shift();
			long null_val = null_val();
			
			int  mask = (int) mask(bits);
			long src  = read(0);
			for (int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++)
			{
				final int bit   = bit(bp);
				long      value = (BITS < bit + bits ? value(src, src = read(index(bp) + 1), bit, bits, mask) : value(src, bit, mask)) + shift;
				
				if (value == null_val) dst.append("null");
				else dst.append( value);
				
				dst.append('\t');
				
				if (i % 10 == 0) dst.append('\t').append(i / 10 * 10).append('\n');
			}
			
			return dst;
		}
	}
	
	
	abstract class R extends BitsList.R implements ISrc {
		
		public final long null_val;
		@Override public long null_val() { return null_val; }
		
		protected R(long null_val, int bits_per_item, long shift) {
			super(shift, bits_per_item);
			this.null_val = null_val;
		}
		
		//0 < items prefill  with nulls
		protected R(long null_val, int bits_per_item, long shift, int items) {
			super(shift, bits_per_item, items);
			this.null_val = null_val;
			if (0 < items && null_val != 0) nulls(this, 0, items);
		}
		
		
		protected static void nulls(R dst, int from, int upto) { while (from < upto) set(dst, from++, dst.null_val); }
		
		public boolean hasValue(int index)                     {return get(index) != null_val;}
		
		@Override public R clone()                             {return (R) super.clone();}
	}
	
	class RW extends R implements IDst {
		
		//region  IDst
		@Override public void write(int index, long src) { array[index] = src; }
		
		@Override public void write(long null_val, long shift, int size, int bits) { write(this, size);}
		
		//endregion
		
		
		public RW(long null_val, int bits_per_item, long shift)            { super(null_val, bits_per_item, shift); }
		
		public RW(long null_val, int bits_per_item, long shift, int items) { super(null_val, bits_per_item, shift, items); }
		
		
		public RW(long null_val, int bits_per_item, byte... values)        {this(0, null_val, bits_per_item, values);}
		public RW(long shift, long null_val, int bits_per_item, byte... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		public RW(long null_val, int bits_per_item, Byte... values) {this(0, null_val, bits_per_item, values);}
		public RW(long shift, long null_val, int bits_per_item, Byte... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		public RW(long null_val, int bits_per_item, char... values) {this(0, null_val, bits_per_item, values);}
		public RW(long shift, long null_val, int bits_per_item, char... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		public RW(long null_val, int bits_per_item, Character... values) {this(0, null_val, bits_per_item, values);}
		public RW(long shift, long null_val, int bits_per_item, Character... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		public RW(long null_val, int bits_per_item, short... values) {this(0, null_val, bits_per_item, values);}
		public RW(long shift, long null_val, int bits_per_item, short... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		public RW(long null_val, int bits_per_item, Short... values) {this(0, null_val, bits_per_item, values);}
		public RW(long shift, long null_val, int bits_per_item, Short... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		public RW(long null_val, int bits_per_item, int... values) {this(0, null_val, bits_per_item, values);}
		public RW(long shift, long null_val, int bits_per_item, int... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		public RW(long null_val, int bits_per_item, Integer... values) {this(0, null_val, bits_per_item, values);}
		public RW(long shift, long null_val, int bits_per_item, Integer... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		public RW(long null_val, int bits_per_item, long... values) {this(0, null_val, bits_per_item, values);}
		public RW(long shift, long null_val, int bits_per_item, long... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		public RW(long null_val, int bits_per_item, Long... values) {this(0, null_val, bits_per_item, values);}
		public RW(long shift, long null_val, int bits_per_item, Long... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		
		public void add(Byte value)                 { add(size, value == null ? null_val : value); }
		public void add(Character value)            { add(size, value == null ? null_val : value); }
		public void add(Short value)                { add(size, value == null ? null_val : value); }
		public void add(Integer value)              { add(size, value == null ? null_val : value); }
		public void add(Long value)                 { add(size, value == null ? null_val : value); }
		public void add(long value)                 { add(size, value); }
		
		public void add(int index, Byte value)      { add(index, value == null ? null_val : value); }
		public void add(int index, Character value) { add(index, value == null ? null_val : value); }
		public void add(int index, Short value)     { add(index, value == null ? null_val : value); }
		public void add(int index, Integer value)   { add(index, value == null ? null_val : value); }
		public void add(int index, Long value)      { add(index, value == null ? null_val : value); }
		public void add(int index, long src) {
			if (index < size) add(this, index, src);
			else set(index, src);
		}
		
		public void remove(Byte value)              { remove(this, value == null ? null_val : value); }
		public void remove(Character value)         { remove(this, value == null ? null_val : value); }
		public void remove(Short value)             { remove(this, value == null ? null_val : value); }
		public void remove(Integer value)           { remove(this, value == null ? null_val : value); }
		public void remove(Long value)              { remove(this, value == null ? null_val : value); }
		public void remove(long value)              { remove(this, value); }
		
		
		public void removeAt(int item)              { removeAt(this, item); }
		
		
		public void set(int index, Byte value)      { set(index, value == null ? null_val : value); }
		public void set(int index, Character value) { set(index, value == null ? null_val : value); }
		public void set(int index, Short value)     { set(index, value == null ? null_val : value); }
		public void set(int index, Integer value)   { set(index, value == null ? null_val : value); }
		public void set(int index, Long value)      { set(index, value == null ? null_val : value); }
		public void set(int item, long value) {
			final int fix = size;
			set(this, item, value);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		
		public void set(int item, Byte... values) {
			final int fix = size;
			
			for (int i = values.length; -1 < --i; )
				set(this, item + i, values[i] == null ? null_val : values[i]);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		public void set(int item, Character... values) {
			final int fix = size;
			
			for (int i = values.length; -1 < --i; )
				set(this, item + i, values[i] == null ? null_val : values[i]);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, Short... values) {
			final int fix = size;
			
			for (int i = values.length; -1 < --i; )
				set(this, item + i, values[i] == null ? null_val : values[i]);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, Integer... values) {
			final int fix = size;
			
			for (int i = values.length; -1 < --i; )
				set(this, item + i, values[i] == null ? null_val : values[i]);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, Long... values) {
			final int fix = size;
			
			for (int i = values.length; -1 < --i; )
				set(this, item + i, values[i] == null ? null_val : values[i]);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, byte... values) {
			final int fix = size;
			set(this, item, values);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, char... values) {
			final int fix = size;
			set(this, item, values);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, short... values) {
			final int fix = size;
			set(this, item, values);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, int... values) {
			final int fix = size;
			set(this, item, values);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, long... values) {
			final int fix = size;
			set(this, item, values);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void clear() {
			if (size < 1) return;
			nulls(this, 0, size);
			size = 0;
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
