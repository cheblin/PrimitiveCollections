package org.unirail.collections;


import org.unirail.JsonWriter;

import java.util.Arrays;

public interface ObjectList {
	
	abstract class R<V> implements Cloneable, JsonWriter.Client {
		
		
		protected       V[]         values;
		protected final Array.Of<V> array;
		private final   boolean     V_is_string;
		
		protected R(Class<V> clazzV) {
			array       = Array.get(clazzV);
			V_is_string = clazzV == String.class;
		}
		
		
		int size = 0;
		
		public int size() {return size;}
		
		public V[] toArray(int index, int len, V[] dst) {
			if (size == 0) return null;
			if (dst == null || dst.length < len) return Arrays.copyOfRange(values, index, len);
			System.arraycopy(values, index, dst, 0, len);
			return dst;
		}
		
		public boolean containsAll(R<V> src) {
			
			for (int i = 0, s = src.size(); i < s; i++)
				if (-1 < indexOf(src.get(i))) return false;
			return true;
		}
		
		public boolean hasValue(int index) {return index < size && values[index] != null;}
		
		public V get(int index)            {return values[index];}
		
		
		public int indexOf(V value) {
			for (int i = 0; i < size; i++)
				if (array.equals(values[i], value)) return i;
			return -1;
		}
		
		public int lastIndexOf(V value) {
			for (int i = size; -1 < --i; )
				if (array.equals(values[i], value)) return i;
			return -1;
		}
		
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {return obj != null && getClass() == obj.getClass() && equals((R<V>) obj);}
		
		public boolean equals(R<V> other) {return other != null && size == other.size && array.equals(values, other.values, size);}
		
		public int hashCode()             {return array.hashCode(values, size);}
		
		@SuppressWarnings("unchecked")
		public R<V> clone() {
			try
			{
				R<V> dst = (R<V>) super.clone();
				if (dst.values != null) dst.values = values.clone();
				
			} catch (CloneNotSupportedException e) {e.printStackTrace();}
			
			return null;
		}
		
		
		public String toString() {return toJSON();}
		@Override public void toJSON(JsonWriter json) {
			json.enterArray();
			
			int size = size();
			
			if (0 < size)
			{
				json.preallocate(size * 10);
				int i = 0;
				if (V_is_string)
					for (String[] strs = (String[]) values; i < size; i++) json.value(strs[i]);
				else
					for (; i < size; i++) json.value(values[i]);
			}
			json.exitArray();
		}
	}
	
	
	class RW<V> extends R<V> {
		@SuppressWarnings("unchecked")
		public RW(Class<V> clazz, int length) {
			super(clazz);
			values = array.copyOf(null, length);
		}
		
		@SafeVarargs public RW(Class<V> core_storage_class, V... items) {
			this(core_storage_class, 0);
			if (items == null) return;
			values = items.clone();
			size   = items.length;
		}
		
		public RW(Class<V> core_storage_class, V fill_value, int size) {
			this(core_storage_class, size);
			this.size = size;
			if (fill_value == null) return;
			while (-1 < --size) values[size] = fill_value;
		}
		
		
		public void clear() {
			if (size < 1) return;
			Arrays.fill(values, 0, size - 1, null);
			size = 0;
		}
		
		
		public V add(V value) {return add(size, value);}
		
		public V add(int index, V value) {
			int max = Math.max(index, size + 1);
			
			size = org.unirail.collections.Array.resize(values, values.length <= max ? values = array.copyOf(null, max + max / 2) : values, index, size, 1);
			
			return values[index] = value;
		}
		
		public void add(int index, V[] src, int src_index, int len) {
			int max = Math.max(index, size) + len;
			
			size = org.unirail.collections.Array.resize(values, values.length < max ? values = array.copyOf(null, max + max / 2) : values, index, size, len);
			
			System.arraycopy(src, src_index, values, index, len);
		}
		
		public void remove()          {remove(size - 1);}
		
		public void remove(int index) {size = org.unirail.collections.Array.resize(values, values, index, size, -1);}
		
		public void remove_fast(int index) {
			if (size < 1 || size <= index) return;
			size--;
			values[index] = values[size];
			values[size]  = null;
		}
		
		public V set(V value) {return set(size, value);}
		
		
		@SafeVarargs
		public final void set(int index, V... src) {
			int len = src.length;
			int max = index + len;
			
			if (size < max)
			{
				if (values.length < max) org.unirail.collections.Array.copy(values, index, len, size, values = array.copyOf(null, max + max / 2));
				size = max;
			}
			
			System.arraycopy(src, 0, values, index, len);
		}
		
		
		public V set(int index, V value) {
			if (size <= index)
			{
				if (values.length <= index) values = array.copyOf(values, index + index / 2);
				size = index + 1;
			}
			return values[index] = value;
		}
		
		
		public void removeAll(R<V> src)   {for (int i = 0, max = src.size(), p; i < max; i++) removeAll(src.get(i));}
		
		public void removeAll(V src)      {for (int k; -1 < (k = indexOf(src)); ) remove(k);}
		
		public void removeAll_fast(V src) {for (int k; -1 < (k = indexOf(src)); ) remove_fast(k);}
		
		public void retainAll(R<V> chk) {
			for (int i = 0; i < size; i++)
			{
				final V val = values[i];
				if (chk.indexOf(val) == -1) removeAll(val);
			}
		}
		
		public RW<V> clone() {return (RW<V>) super.clone();}
	}
}
	