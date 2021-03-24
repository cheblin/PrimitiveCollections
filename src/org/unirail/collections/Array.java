package org.unirail.collections;


public interface Array extends Cloneable {
	
	
	Object array();
	
	Object length( int size );
	
	int length();
	
	static int resize( Array array, int size, int index, final int resize, final boolean fit ) {
fit:
		{
			if (size < 0) size = 0;
			if (resize == 0) return size;
			
			
			if (index < 0) index = 0;
			
			
			if (resize < 0)
			{
				if (size == 0)
				{
					if (fit) array.length( 0 );
					return 0;
				}
				
				if (size <= index)
				{
					if (fit) break fit;
					return size;
				}
				
				if (index == 0 && size <= -resize)
				{
					if (fit) array.length( 0 );
					return 0;
				}
				
				if (index + (-resize) < size)//есть хвост который надо перенести
				{
					final Object tmp = array.array();
					System.arraycopy( tmp, index + (-resize), tmp, index, size - (index + (-resize)) );
					
					size += resize;
				}
				else
					size = index + 1;
				
				if (fit) break fit;
				
				return size;
			}
			
			final int new_size = index <= size ? size + resize : index + 1 + resize;
			
			final int length = array.length();
			
			if (length < 1)
			{
				array.length( new_size );
				return new_size;
			}
			
			Object src = array.array();
			Object dst = new_size < length ? src : array.length( fit ? new_size : Math.max( new_size, length + length / 2 ) );
			
			if (0 < size)
				if (index < size)
					if (index == 0)
					{
						System.arraycopy( src, 0, dst, resize, size );
					}
					else
					{
						if (src != dst) System.arraycopy( src, 0, dst, 0, index );
						System.arraycopy( src, index, dst, index + resize, size - index );
					}
				else if (src != dst) System.arraycopy( src, 0, dst, 0, size );
			
			size = new_size;
			
			if (!fit) return size;
		}
		
		if (size < array.length())
			if (size == 0) array.length( 0 );
			else System.arraycopy( array.array(), 0, array.length( size ), 0, size );
		
		return size;
	}
	
	
	static int hash( Object val ) {return val == null ? 0 : hash( val.hashCode() );}
	
	static int hash( double val ) {return hash( Double.doubleToLongBits( val ) );}
	
	static int hash( float val )  {return hash( Float.floatToIntBits( val ) );}
	
	static int hash( long val ) {
		val = (val ^ (val >>> 32)) * 0x4cd6944c5cc20b6dL;
		val = (val ^ (val >>> 29)) * 0xfc12c5b19d3259e9L;
		return (int) (val ^ (val >>> 32));
		
	}
	
	static int hash( int val ) {
		val = (val ^ (val >>> 16)) * 0x85ebca6b;
		val = (val ^ (val >>> 13)) * 0xc2b2ae35;
		return val ^ (val >>> 16);
	}
	
	static int hashKey( int key ) {
		
		final int h = key * 0x9e3779b9;
		return h ^ h >>> 16;
	}
	
	static int hashKey( long key ) {
		final long h = key * 0x9e3779b97f4a7c15L;
		return (int) (h ^ (h >>> 32));
	}
	
	static int hashKey( Object key ) {
		final int h = key.hashCode() * 0x9e3779b9;
		return h ^ h >>> 16;
	}
	
	static long nextPowerOf2( long v ) {
		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		v |= v >> 32;
		v++;
		return v;
	}
	
	
}
