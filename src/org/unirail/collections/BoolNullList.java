package org.unirail.collections;


public interface BoolNullList {
	
	abstract class R extends BitsList.R {
		
		protected R(int items) { super(2, 0, items); }
		
		public Boolean asBoolean(int index) {
			switch ((int) get(index))
			{
				case 1:
					return Boolean.TRUE;
				case 2:
					return Boolean.FALSE;
			}
			return null;
		}
		
		public R clone() {return (R) super.clone();}
	}
	
	class RW extends R implements BitsList.IDst {
		
		//region  IDst
		@Override public void write(int index, long src) { array[index] = src; }
		
		@Override public void write(int size, int bits) { write(this, size, bits);}
		
		//endregion
		
		public RW(int items) { super(items); }
		
		public RW(boolean... values) {
			super(values.length);
			set(0, values);
		}
		
		public RW(Boolean... values) {
			super(values.length);
			set(0, values);
		}
		
		public void add(boolean value)           { add(this, value ? 1 : 2); }
		public void add(Boolean value)           { add(this, value == null ? 0 : value ? 1 : 2); }
		
		
		public void remove(Boolean value)        { remove(this, value == null ? 0 : value ? 1 : 2); }
		public void remove(boolean value)        { remove(this, value ? 1 : 2); }
		
		
		public void removeAt(int item)           { removeAt(this, item); }
		
		
		public void set(boolean value)           {set(this, size, value ? 1 : 2); }
		public void set(Boolean value)           { set(this, size, value == null ? 0 : value ? 1 : 2); }
		
		
		public void set(int item, boolean value) {set(this, item, value ? 1 : 2); }
		public void set(int item, Boolean value) { set(this, item, value == null ? 0 : value ? 1 : 2); }
		
		
		public void set(int index, boolean... values) {
			for (int i = 0, max = values.length; i < max; i++)
				set(index + i, values[i]);
		}
		
		public void set(int index, Boolean... values) {
			for (int i = 0, max = values.length; i < max; i++)
				set(index + i, values[i]);
		}
		
		public void clear() { super.clear(); }
		
		
		public RW clone()   { return (RW) super.clone(); }
	}
}

