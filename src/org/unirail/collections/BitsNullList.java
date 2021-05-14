package org.unirail.collections;


public interface BitsNullList {
	
	class R extends BitsList.Base {
		
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
		
		public R(char null_val, int bits_per_item, int... values) {
			this(null_val, bits_per_item, values.length);
			fill(this, values);
		}
		
		protected static void fill(R dst, int... items) {
			
			final int bits = dst.bits;
			for (int i : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (long) i << bit;
			}
		}
		
		public R(char null_val, int bits_per_item, Integer... values) {
			this(null_val, bits_per_item, values.length);
			fill(this, values);
		}
		
		protected static void fill(R dst, Integer... items) {
			
			final int bits = dst.bits;
			for (Integer i : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (long) (i == null ? dst.null_val : i) << bit;
			}
		}
		
		protected static void nulls(R dst, int from, int upto) {
			
			final long null_val = dst.null_val;
			
			for (int bits = dst.bits; from < upto; )
			{
				final int item  = bits * from++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= null_val << bit;
			}
		}
		
		public boolean hasValue(int index) {return super.get(index) != null_val ;}
		
		public char get(int index)       {return super.get(index); }
		
		@Override public R clone()     {return (R) super.clone();}
		
	}
	
	
	class RW extends R implements BitsList.Consumer {
		
		@Override public void consume(int index, long src)             { array[index] = src; }
		
		@Override public void consume(int items, int bits) { BitsList.Base.consume(this, items, bits);}
		
		public RW(char null_val, int bits_per_item)                    { super(null_val, bits_per_item); }
		
		public RW(char null_val, int bits_per_item, int items)         { super(null_val, bits_per_item, items); }
		
		public RW(char null_val, int bits_per_item, int... values)     { super(null_val, bits_per_item, values); }
		
		public RW(char null_val, int bits_per_item, Integer... values) { super(null_val, bits_per_item, values); }
		
		
		public void add(char value)                                    { add(this, value); }
		
		public void add(Integer value)                                 { add(this, value == null ? null_val : (char) (value & 0xFFFF)); }
		
		public void remove(Integer value)                              { remove(this, value == null ? null_val : (char) (value & 0xFFFF)); }
		
		public void remove(int value)                                  { remove(this, value); }
		
		public void remove(char value)                                 { remove(this, value); }
		
		public void removeAt(int item)                                 { removeAt(this, item); }
		
		
		public void set(int value)                                     {set(this, size, value); }
		
		public void set(char value)                                    {set(this, size, value); }
		
		public void set(Integer value)                                 { set(this, size, value == null ? null_val : (char) (value & 0xFFFF)); }
		
		
		public void set(int item, char value)                          {set(item, (int) value);}
		
		public void set(int item, int value) {
			final int fix = size;
			set(this, item, value);
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void seT(int item, int... values) {
			final int fix = size;
			for (int i = 0, max = values.length; i < max; i++)
				set(this, item + i, (char) values[i]);
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, Integer value) {
			final int fix = size;
			set(this, item, value == null ? null_val : (char) (value & 0xFFFF));
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void set(int item, Integer... values) {
			final int fix = size;
			for (int i = 0, max = values.length; i < max; i++)
				set(this, item + i, values[i] == null ? null_val : (char) (values[i] & 0xFFFF));
			if (fix < item && null_val != 0) nulls(this, fix, item);
		}
		
		public void clear()         { clear(this); }
		
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
