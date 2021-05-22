package org.unirail.collections;


public interface UByteShortNullMap {
	
	interface IDst {
		boolean put(char key, short value);
		
		boolean put(char key,  Short     value);
		
		boolean put( Character key,  Short     value);
		
		boolean put( Character key, short value);
		
		void write(int size);
	}
	
	interface ISrc {
		int size();
		
		boolean read_has_null_key();
		
		short read_null_key_val();
		
		@Positive_YES int read_has_val(int info);
		
		char read_key(int info);
		
		short read_val(@Positive_ONLY int info);
		
		default StringBuilder toString(StringBuilder dst) {
			int size = size();
			if (dst == null) dst = new StringBuilder(size * 10);
			else dst.ensureCapacity(dst.length() + size * 10);
			
			if (read_has_null_key())
			{
				dst.append("null -> ").append(read_null_key_val()).append('\n');
				size--;
			}
			
			for (int p = -1, i = 0; i < size; i++, dst.append('\n'))
			{
				dst.append(read_key(p = read_has_val(p))).append(" -> ");
				
				if (p < 0) dst.append("null");
				else dst.append(read_val(p));
			}
			
			return dst;
		}
	}
	
	abstract class R implements Cloneable, Comparable<R>, ISrc {
		
		ByteSet.RW             keys = new ByteSet.RW();
		ShortNullList.RW values;
		
		
		@Override public int size()                        { return keys.size(); }
		
		public boolean isEmpty()                           { return keys.isEmpty();}
		
		
		public @Positive_Values int info( Character key) {return key == null ? hasNullKey : info((char) (key + 0));}
		
		public @Positive_Values int info(char key) {
			
			return keys.contains((byte) key) ? values.hasValue(key) ? key : Positive_Values.NULL : Positive_Values.NONE;
		}
		
		public boolean hasValue(int info) {return -1 < info;}
		
		public boolean hasNone(int info)  {return info == Positive_Values.NONE;}
		
		public boolean hasNull(int info)  {return info == Positive_Values.NULL;}
		
		
		public short value(@Positive_ONLY int info) {
			if (info == Positive_Values.VALUE) return nullKeyValue;
			return    values.get(keys.rank((byte) info));
		}
		
		
		@Positive_Values int hasNullKey = Positive_Values.NONE;
		short nullKeyValue = 0;
		
		public boolean equals(Object obj) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo(getClass().cast(obj)) == 0;
		}
		
		public boolean equals(R other) { return other != null && compareTo(other) == 0; }
		
		@Override public int compareTo(R other) {
			if (other == null) return -1;
			
			int diff;
			if (hasNullKey != other.nullKeyValue || hasNullKey == Positive_Values.VALUE && nullKeyValue != other.nullKeyValue) return 1;
			if ((diff = other.keys.compareTo(keys)) != 0 || (diff = other.values.compareTo(values)) != 0 || (diff = other.values.compareTo(values)) != 0) return diff;
			
			return 0;
		}
		
		@Override public R clone() {
			
			try
			{
				R dst = (R) super.clone();
				dst.keys = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		
		//region  ISrc
		@Override public short read_null_key_val() { return nullKeyValue; }
		
		@Override public boolean read_has_null_key() { return keys.hasNullKey; }
		
		
		@Override public @Positive_YES int read_has_val(int info) {
			int key  = keys.read(info);
			int item = info >> 8 & 0xFF;
			
			return (values.hasValue(key) ? Integer.MIN_VALUE | item << 8 : (item + 1 & 0xFF) << 8) | key;
		}
		
		@Override public char read_key(int info) {return (char) (info & 0xFF);}
		
		@Override public short read_val(@Positive_ONLY int info) { return  values.get(info >> 8); }
		
		//endregion
		public String toString() { return toString(null).toString();}
	}
	
	class RW extends R implements IDst {
		
		public RW(int length) { values = new ShortNullList.RW(265 < length ? 256 : length); }
		
		
		public void clear() {
			if (keys.size() < 1) return;
			
			keys.clear();
			values.clear();
		}
		
		//region  IDst
		@Override public void write(int size) {
			keys.write(size);
			values.write(size);
		}
		
		//endregion
		
		
		@Override public boolean put( Character key, short value) {
			if (key != null) return put((char) (key + 0), value);
			
			int h = hasNullKey;
			hasNullKey = Positive_Values.VALUE;
			nullKeyValue = value;
			return h != Positive_Values.VALUE;
		}
		
		public boolean put( Character key,  Short     value) {
			if (key != null) return put((char) (key + 0), value);
			
			int h = hasNullKey;
			
			if (value == null)
			{
				hasNullKey = Positive_Values.NULL;
				return h == Positive_Values.NULL;
			}
			
			hasNullKey = Positive_Values.VALUE;
			nullKeyValue = value;
			return h == Positive_Values.VALUE;
		}
		
		
		public boolean put(char key,  Short     value) {
			if (value != null) return put(key, (short) value);
			
			
			if (keys.add((byte) key))
			{
				values.add(keys.rank((byte) key) - 1, ( Short    ) null);
				return true;
			}
			values.set(keys.rank((byte) key) - 1, ( Short    ) null);
			
			return false;
		}
		
		@Override public boolean put(char key, short value) {
			
			if (keys.add((byte) key))
			{
				values.add(keys.rank((byte) key) - 1, value);
				return true;
			}
			
			values.set(keys.rank((byte) key) - 1, value);
			return false;
		}
		
		public boolean remove( Character  key) {
			return key == null ?
			       hasNullKey != Positive_Values.NONE && (hasNullKey = Positive_Values.NONE) == Positive_Values.NONE :
			       remove((char) (key + 0));
		}
		
		public boolean remove(char key) {
			final byte k = (byte) key;
			if (!keys.contains(k)) return false;
			
			values.remove(keys.rank(k) - 1);
			keys.remove(k);
			return true;
		}
		
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
