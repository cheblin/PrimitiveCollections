package org.unirail.collections;

import org.unirail.Hash;

public interface UByteSet {
	
	interface NonNullKeysIterator {
		
		int END = -1;
		
		static char key( int token ) {return (char) (token & 0xFF);}
		
		static int token( R src ) {return token( src, END );}
		
		static int token( R src, int token ) {
			token++;
			token &= 0xFF;
			long l;
			if (token < 128)
			{
				if (token < 64)
				{
					if ((l = src._1 >>> token) != 0) return (token + Long.numberOfTrailingZeros( l ));
					token = 0;
				}
				else token -= 64;
				
				if ((l = src._2 >>> token) != 0) return ((token + Long.numberOfTrailingZeros( l ) + 64));
				token = 128;
			}
			
			if (token < 192)
			{
				if ((l = src._3 >>> (token - 128)) != 0) return ((token + Long.numberOfTrailingZeros( l ) + 128));
				
				token = 0;
			}
			else token -= 192;
			
			if ((l = src._4 >>> token) != 0) return ((token + Long.numberOfTrailingZeros( l ) + 192));
			
			return END;
		}
	}
	
	abstract class R implements Cloneable, Comparable<R> {
		long
				_1,
				_2,
				_3,
				_4;
		
		int size = 0;
		
		protected boolean hasNullKey;
		
		protected boolean add( final char value ) {
			
			final int val = value & 0xFF;
			
			if (val < 128)
				if (val < 64)
					if ((_1 & 1L << val) == 0) _1 |= 1L << val;
					else return false;
				else if ((_2 & 1L << val - 64) == 0) _2 |= 1L << val - 64;
				else return false;
			else if (val < 192)
				if ((_3 & 1L << val - 128) == 0) _3 |= 1L << val - 128;
				else return false;
			else if ((_4 & 1L << val - 192) == 0) _4 |= 1L << val - 192;
			else return false;
			
			size++;
			return true;
		}
		
		
		public int size()                           {return hasNullKey ? size + 1 : size;}
		
		public boolean isEmpty()                    {return size < 1;}
		
		public boolean contains(  Character key ) {return key == null ? hasNullKey : contains( (char) (key + 0) );}
		
		public boolean contains( int key ) {
			if (size() == 0) return false;
			
			final int val = key & 0xFF;
			return
					(
							val < 128 ?
							val < 64 ? _1 & 1L << val : _2 & 1L << val - 64 :
							val < 192 ? _3 & 1L << val - 128 : _4 & 1L << val - 192
					) != 0;
		}
		
		
		//Rank returns the number of integers that are smaller or equal to x
		//return inversed value if key does not exists
		protected int rank( char key ) {
			final int val = key & 0xFF;
			
			int  ret  = 0;
			long l    = _1;
			int  base = 0;
a:
			{
				if (val < 64) break a;
				if (l != 0) ret += Long.bitCount( l );
				base = 64;
				l    = _2;
				if (val < 128) break a;
				if (l != 0) ret += Long.bitCount( l );
				base = 128;
				l    = _3;
				if (val < 192) break a;
				if (l != 0) ret += Long.bitCount( l );
				base = 192;
				l    = _4;
			}
			
			while (l != 0)
			{
				final int s = Long.numberOfTrailingZeros( l );
				
				if ((base += s) == val) return ret + 1;
				if (val < base) return ~ret;
				
				ret++;
				l >>>= s + 1;
			}
			
			return ~ret;
		}
		
		public boolean containsAll( R ask ) {
			
			if (hasNullKey != ask.hasNullKey) return false;
			
			int  i;
			long l;
			
			for (i = 0, l = _1; l != 0; l >>>= ++i)
				if (!ask.contains( (char) (i += Long.numberOfTrailingZeros( l )) )) return false;
			
			for (i = 0, l = _2; l != 0; l >>>= ++i)
				if (!ask.contains( (char) (i += Long.numberOfTrailingZeros( l ) + 64) )) return false;
			
			for (i = 0, l = _3; l != 0; l >>>= ++i)
				if (!ask.contains( (char) (i += Long.numberOfTrailingZeros( l ) + 128) )) return false;
			
			for (i = 0, l = _4; l != 0; l >>>= ++i)
				if (!ask.contains( (char) (i += Long.numberOfTrailingZeros( l ) + 192) )) return false;
			
			return true;
		}
		
		public int hashCode() {return Hash.code( Hash.code( Hash.code( Hash.code( hasNullKey ? 184889743 : 22633363, _1 ), _2 ), _3 ), _4 );}
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		public boolean equals( R other ) {return other != null && compareTo( other ) == 0;}
		
		public int compareTo( R other ) {
			
			return
					size == other.size ?
					(int) (_1 == other._1 ?
					       _2 == other._2 ?
					       _3 == other._3 ?
					       _4 == other._4 ? 0 : _4 - other._4 : _3 - other._3 : _2 - other._2 : _1 - other._1) : size - other.size;
		}
		
		public R clone() {
			try
			{
				return (R) super.clone();
			} catch (CloneNotSupportedException e) {e.printStackTrace();}
			return null;
		}
		
		
		public String toString() {return toString( null ).toString();}
		
		public StringBuilder toString( StringBuilder dst ) {
			int size = size();
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			if (hasNullKey) dst.append( "null\n" );
			
			
			for (int token = NonNullKeysIterator.token( this ); token != NonNullKeysIterator.END; token = NonNullKeysIterator.token( this, token ))
			     dst.append( (int) NonNullKeysIterator.key( token ) ).append( '\n' );
			
			return dst;
		}
	}
	
	class RW extends R {
		
		public RW()                            {}
		
		public RW( char... items )     {for (char i : items) this.add( i );}
		
		public RW(  Character... items )     {for ( Character key : items) add( key );}
		
		public boolean add(  Character key ) {return key == null ? !hasNullKey && (hasNullKey = true) : super.add( (char) (key + 0) );}
		
		public boolean add( char key ) {return super.add( key );}
		
		public boolean retainAll( R src ) {
			boolean ret = false;
			
			if (_1 != src._1)
			{
				_1 &= src._1;
				ret = true;
			}
			if (_2 != src._2)
			{
				_2 &= src._2;
				ret = true;
			}
			if (_3 != src._3)
			{
				_3 &= src._3;
				ret = true;
			}
			if (_4 != src._4)
			{
				_4 &= src._4;
				ret = true;
			}
			
			if (ret) size = Long.bitCount( _1 ) + Long.bitCount( _2 ) + Long.bitCount( _3 ) + Long.bitCount( _4 );
			return ret;
		}
		
		
		public boolean remove(  Character key ) {return key == null ? hasNullKey && !(hasNullKey = false) : remove( (char) (key + 0) );}
		
		public boolean remove( char value ) {
			if (size == 0) return false;
			
			final int val = value & 0xFF;
			
			if (val < 128)
				if (val < 64)
					if ((_1 & 1L << val) == 0) return false;
					else _1 &= ~(1L << val);
				else if ((_2 & 1L << val - 64) == 0) return false;
				else _2 &= ~(1L << val - 64);
			else if (val < 192)
				if ((_3 & 1L << val - 128) == 0) return false;
				else _3 &= ~(1L << val - 128);
			else if ((_4 & 1L << val - 192) == 0) return false;
			else _4 &= ~(1L << val - 192);
			
			size--;
			return true;
		}
		
		
		public void clear() {
			_1         = 0;
			_2         = 0;
			_3         = 0;
			_4         = 0;
			size       = 0;
			hasNullKey = false;
		}
		
		public boolean addAll( R src ) {
			boolean      ret = false;
			char key;
			
			for (int token = NonNullKeysIterator.END, i = 0, size = src.size(); i < size; i++)
				if (!contains( key = NonNullKeysIterator.key( NonNullKeysIterator.token( src, token ) ) ))
				{
					ret = true;
					this.add( key );
				}
			
			return ret;
		}
		
		public RW clone() {return (RW) super.clone();}
	}
}
