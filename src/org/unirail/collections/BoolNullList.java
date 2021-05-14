package org.unirail.collections;


public interface BoolNullList {
	
	class R extends BitsList.Base {
		
		protected R(int items) { super(2, items); }
		
		public R(boolean... values) {
			super(2, values.length);
			fill(this, values);
		}
		
		protected static void fill(R dst, boolean... items) {
			
			final int bits = dst.bits;
			for (boolean i : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (i ? 1L : 2L) << bit;
			}
		}
		
		public R(Boolean... values) {
			super(2, values.length);
			fill(this, values);
		}
		
		protected static void fill(R dst, Boolean... items) {
			
			final int bits = dst.bits;
			for (Boolean b : items)
			{
				final int item  = bits * dst.size++;
				final int index = item >>> LEN;
				final int bit   = item & MASK;
				
				dst.array[index] |= (b == null ? 0L : b ? 1L : 2L) << bit;
			}
		}
		
		public Boolean value(int index) {
			final int i = get(index);
			return i == 0 ? null : i == 1 ? Boolean.TRUE : Boolean.FALSE;
		}
		
		public R clone()         {return (R) super.clone();}
		
	}
	
	
	class RW extends R implements BitsList.Consumer {
		
		@Override public void consume(int index, long src) { array[index] = src; }
		
		@Override public void consume(int items, int bits) { BitsList.Base.consume(this, items, bits);}
		
		public RW(int items)                               { super(items); }
		
		public RW(boolean... values)                       { super(values); }
		
		public RW(Boolean... values)                       { super(values); }
		
		public void add(int value)                         { add(this, (char) value);}
		
		public void add(boolean value)                     { add(this, value ? (char) 1 : 2); }
		
		public void add(Boolean value)                     { add(this, value == null ? 0 : value ? (char) 1 : 2); }
		
		public void remove(Boolean value)                  { remove(this, value == null ? 0 : value ? (char) 1 : 2); }
		
		public void remove(boolean value)                  { remove(this, value ? (char) 1 : 2); }
		
		public void removeAt(int item)                     { removeAt(this, item); }
		
		
		public void set(boolean value)                     {set(this, size, value ? (char) 1 : 2); }
		
		public void set(Boolean value)                     { set(this, size, value == null ? 0 : value ? (char) 1 : 2); }
		
		
		public void set(int item, int value)               {set(this, item, value); }
		
		public void set(int item, char value)              {set(this, item, value); }
		
		public void set(int item, boolean value)           {set(this, item, value ? (char) 1 : 2); }
		
		public void set(int item, Boolean value)           { set(this, item, value == null ? 0 : value ? (char) 1 : 2); }
		
		
		public void set(int index, boolean... values) {
			for (int i = 0, max = values.length; i < max; i++)
				set(this, index + i, values[i] ? (char) 1 : 2);
		}
		
		public void set(int index, Boolean... values) {
			for (int i = 0, max = values.length; i < max; i++)
				set(this, index + i, values[i] == null ? 0 : values[i] ? (char) 1 : 2);
		}
		
		public void clear() { clear(this); }
		
		
		public RW clone()   { return (RW) super.clone(); }
	}
}

