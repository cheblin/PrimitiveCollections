package org.unirail.collections;


public interface BitsNullList {
	
	abstract class R extends BitsList.R {
		
		public final long null_val;
		
		protected R(long null_val, int bits_per_item, long shift) {
			super(bits_per_item, shift);
			this.null_val = null_val;
		}
		
		//0 < items prefill  with nulls
		protected R(long null_val, int bits_per_item, long shift, int items) {
			super(bits_per_item, shift, items);
			this.null_val = null_val;
			if (0 < items && null_val != 0) nulls(this, 0, items);
		}
		
		
		protected static void nulls(R dst, int from, int upto) { while (from < upto) set(dst, from++, dst.null_val); }
		
		public boolean hasValue(int index)                     {return get(index) != null_val;}
		
		@Override public R clone()                             {return (R) super.clone();}
	}
	
	class RW extends R implements BitsList.IDst {
		
		//region  IDst
		@Override public void write(int index, long src) { array[index] = src; }
		
		@Override public void write(int size, int bits) { write(this, size, bits);}
		
		//endregion
		
		
		public RW(long null_val, int bits_per_item, long shift)            { super(null_val, bits_per_item, shift); }
		
		public RW(long null_val, int bits_per_item, long shift, int items) { super(null_val, bits_per_item, shift, items); }
		
		public RW(long null_val, int bits_per_item, long shift, char... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		public RW(long null_val, int bits_per_item, long shift, Character... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		public RW(long null_val, int bits_per_item, long shift, short... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		public RW(long null_val, int bits_per_item, long shift, Short... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		public RW(long null_val, int bits_per_item, long shift, int... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		public RW(long null_val, int bits_per_item, long shift, Integer... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		public RW(long null_val, int bits_per_item, long shift, long... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		public RW(long null_val, int bits_per_item, long shift, Long... values) {
			super(null_val, bits_per_item, shift, values.length);
			set(0, values);
		}
		
		
		public void add(Character value)            { add(this, value == null ? null_val : value); }
		public void add(Short value)                { add(this, value == null ? null_val : value); }
		public void add(Integer value)              { add(this, value == null ? null_val : value); }
		public void add(Long value)                 { add(this, value == null ? null_val : value); }
		public void add(long value)                 {add(this, value); }
		
		public void add(int index, Character value) { add(index, value == null ? null_val : value); }
		public void add(int index, Short value)     { add(index, value == null ? null_val : value); }
		public void add(int index, Integer value)   { add(index, value == null ? null_val : value); }
		public void add(int index, Long value)      { add(index, value == null ? null_val : value); }
		public void add(int index, long src) {
			if (size <= index) set(index, src);
			else add(this, index, src);
		}
		
		public void remove(Character value)         { remove(this, value == null ? null_val : value); }
		public void remove(Short value)             { remove(this, value == null ? null_val : value); }
		public void remove(Integer value)           { remove(this, value == null ? null_val : value); }
		public void remove(Long value)              { remove(this, value == null ? null_val : value); }
		public void remove(long value)              { remove(this, value); }
		
		
		public void removeAt(int item)              { removeAt(this, item); }
		
		
		public void set(Character value)            { add(this, value == null ? null_val : value); }
		public void set(Short value)                { add(this, value == null ? null_val : value); }
		public void set(Integer value)              { add(this, value == null ? null_val : value); }
		public void set(Long value)                 { add(this, value == null ? null_val : value); }
		public void set(long value)                 {add(this, value); }
		
		public void set(int index, Character value) { add(index, value == null ? null_val : value); }
		public void set(int index, Short value)     { add(index, value == null ? null_val : value); }
		public void set(int index, Integer value)   { add(index, value == null ? null_val : value); }
		public void set(int index, Long value)      { add(index, value == null ? null_val : value); }
		public void set(int item, long value) {
			final int fix = size;
			set(this, item, value);
			
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
		
		public void clear()         { super.clear(); }
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
