package org.unirail.collections;


import org.unirail.JsonWriter;

import static org.unirail.collections.BitsList.*;

public interface BoolNullList {
	
	abstract class R extends BitsList.R {
		
		protected R(int items)                     {super(2, items);}
		
		protected R(Boolean fill_value, int items) {super(2, fill_value == null ? 0 : fill_value ? 1 : 2, items);}
		
		public boolean hasValue(int index)         {return get(1) != 0;}
		
		public Boolean asBoolean(int index) {
			switch ((int) get(index))
			{
				case 3:
					return Boolean.TRUE;
				case 2:
					return Boolean.FALSE;
			}
			return null;
		}
		
		public String toString() {return toJSON();}
		@Override public void toJSON(JsonWriter json) {
			json.enterArray();
			
			json.preallocate(size * 4);
			if (0 < size)
			{
				long src = values[0];
				
				for (int bp = 0, max = size * bits, i = 1; bp < max; bp += bits, i++)
				{
					final int bit   = bit(bp);
					long      value = (BITS < bit + bits ? value(src, src = values[index(bp) + 1], bit, bits) : src >>> bit & mask);
					if ((value & 2) == 0) json.value();
					else json.value(value == 3);
				}
			}
			json.exitArray();
		}
		
		public R clone() {return (R) super.clone();}
	}
	
	class RW extends R {
		
		public RW(int items)                     {super(items);}
		
		public RW(Boolean fill_value, int items) {super(fill_value, items);}
		
		public RW(boolean... values) {
			super(values.length);
			set(0, values);
		}
		
		public RW(Boolean... values) {
			super(values.length);
			set(0, values);
		}
		
		public void add(boolean value)           {add(this, value ? 1 : 2);}
		
		public void add(Boolean value)           {add(this, value == null ? 0 : value ? 1 : 2);}
		
		
		public void remove(Boolean value)        {remove(this, value == null ? 0 : value ? 1 : 2);}
		
		public void remove(boolean value)        {remove(this, value ? 1 : 2);}
		
		
		public void removeAt(int item)           {removeAt(this, item);}
		
		
		public void set(boolean value)           {set(this, size, value ? 1 : 2);}
		
		public void set(Boolean value)           {set(this, size, value == null ? 0 : value ? 1 : 2);}
		
		public void set(int item, int value)     {set(this, item, value);}
		
		
		public void set(int item, boolean value) {set(this, item, value ? 1 : 2);}
		
		public void set(int item, Boolean value) {set(this, item, value == null ? 0 : value ? 1 : 2);}
		
		
		public void set(int index, boolean... values) {
			for (int i = 0, max = values.length; i < max; i++)
			     set(index + i, values[i]);
		}
		
		public void set(int index, Boolean... values) {
			for (int i = 0, max = values.length; i < max; i++)
			     set(index + i, values[i]);
		}
		
		public void clear() {super.clear();}
		
		
		public RW clone()   {return (RW) super.clone();}
	}
}

