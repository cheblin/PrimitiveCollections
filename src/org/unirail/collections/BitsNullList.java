package org.unirail.collections;


public interface BitsNullList {
	
	abstract class R extends BitsList.R {
		
		protected final char null_val;
		
		protected R(char null_val, int bits_per_item) {
			super(bits_per_item);
			this.null_val = null_val;
		}
		
		protected R(char null_val, int bits_per_item, int items) {
			super(bits_per_item, Math.abs(items));
			this.null_val = null_val;
			
			if (0 < items && null_val != 0) nulls(this, 0, items);
		}
		
		
		protected static void nulls(R dst, int from, int upto) {
			
			for (int bits = dst.bits; from < upto; )
			{
				final int item  = bits * from++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (long) dst.null_val << bit;
			}
		}
		
		public boolean hasValue(int index) {return get(index) != null_val;}
		
		@Override public R clone()         {return (R) super.clone();}
	}
	
	
	class RW extends R implements BitsList.IDst {
		
		//region  IDst
		@Override public void write(int index, long src) { array[index] = src; }
		
		@Override public void write(int size, int bits) { write(this, size, bits);}
		
		//endregion
		
		public RW(char null_val, int bits_per_item)            { super(null_val, bits_per_item); }
		
		public RW(char null_val, int bits_per_item, int items) { super(null_val, bits_per_item, items); }
		
		public RW(char null_val, int bits_per_item, char... values) {
			this(null_val, bits_per_item, values.length);
			
			for (long i : values)
			{
				final int item  = bits * size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				array[index] |= i << bit;
			}
		}
		
		public RW(char null_val, int bits_per_item, Character... values) {
			this(null_val, bits_per_item, values.length);
			
			for (Character i : values)
			{
				final int item  = bits * size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				array[index] |= (long) (i == null ? null_val : i) << bit;
			}
		}
		
		public void add(char value) {add(this, value); }
		
		public void add(int index, char src) {
			if (size <= index) set(index, src);
			else add(this, index, src);
		}
		
		
		public void add(Character value)           { add(this, value == null ? null_val : value); }
		
		public void remove(Integer value)          { remove(this, value == null ? null_val : (char) (value & 0xFFFF)); }
		
		public void remove(int value)              { remove(this, value); }
		
		
		public void removeAt(int item)             { removeAt(this, item); }
		
		
		public void set(Character value)           { set(size, value == null ? null_val : (char) (value & 0xFFFF)); }
		
		public void set(char value)                { set(size, value); }
		
		public void set(int item, Character value) {set(item, value == null ? null_val : (char) (value & 0xFFFF));}
		
		public void set(int item, char value) {
			final int fix = size;
			set(this, item, value);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, Character... values) {
			final int fix = size;
			for (int i = 0, max = values.length; i < max; i++)
				set(this, item + i, values[i] == null ? null_val : (char) (values[i] & 0xFFFF));
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, char... values) {
			final int fix = size;
			for (int i = 0, max = values.length; i < max; i++)
				set(this, item + i, values[i]);
			
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void clear()         { super.clear(); }
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
