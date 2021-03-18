package org.unirail.collections;

public interface UByteSet {
	
	class R implements Cloneable, Comparable<R> {
		long
				_1,
				_2,
				_3,
				_4;
		
		int size = 0;
		
		private static boolean add( UByteSet.R dst, final char value ) {
			
			final int val = value & 0xFF;
			
			if (val < 128)
				if (val < 64)
					if ((dst._1 & 1L << val) == 0) dst._1 |= 1L << val;
					else return false;
				else if ((dst._2 & 1L << val - 64) == 0) dst._2 |= 1L << val - 64;
				else return false;
			else if (val < 192)
				if ((dst._3 & 1L << val - 128) == 0) dst._3 |= 1L << val - 128;
				else return false;
			else if ((dst._4 & 1L << val - 192) == 0) dst._4 |= 1L << val - 192;
			else return false;
			
			dst.size++;
			return true;
			
		}
		
		public int size()        { return size; }
		
		public boolean isEmpty() { return size < 1; }
		
		public boolean contains( char key ) {
			if (size == 0) return false;
			
			final int val = key & 0xFF;
			return
					(
							val < 128 ?
							val < 64 ? _1 & 1L << val : _2 & 1L << val - 64 :
							val < 192 ? _3 & 1L << val - 128 : _4 & 1L << val - 192
					) != 0;
		}
		
		public R()                        {}
		
		public R( char... items ) { for (char i : items) add( this, i ); }
		
		//Rank returns the number of integers that are smaller or equal to x
		public int rank( char key ) {
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
		
		public char[] toArray( char[] dst ) {
			if (dst == null || dst.length < size) dst = new char[size];
			
			int ind = 0;
			
			int  i;
			long l;
			
			for (i = 0, l = _1; l != 0; l >>>= ++i)
			     dst[ind++] = (char) (i += Long.numberOfTrailingZeros( l ));
			
			for (i = 0, l = _2; l != 0; l >>>= ++i)
			     dst[ind++] = (char) (i += Long.numberOfTrailingZeros( l ) + 64);
			
			for (i = 0, l = _3; l != 0; l >>>= ++i)
			     dst[ind++] = (char) (i += Long.numberOfTrailingZeros( l ) + 128);
			
			for (i = 0, l = _4; l != 0; l >>>= ++i)
			     dst[ind++] = (char) (i += Long.numberOfTrailingZeros( l ) + 192);
			
			return dst;
		}
		
		
		public boolean containsAll( CharList.Consumer ask ) {
			
			int  i;
			long l;
			
			for (i = 0, l = _1; l != 0; l >>>= ++i)
				if (!ask.add( (char) (i += Long.numberOfTrailingZeros( l )) )) return false;
			
			for (i = 0, l = _2; l != 0; l >>>= ++i)
				if (!ask.add( (char) (i += Long.numberOfTrailingZeros( l ) + 64) )) return false;
			
			for (i = 0, l = _3; l != 0; l >>>= ++i)
				if (!ask.add( (char) (i += Long.numberOfTrailingZeros( l ) + 128) )) return false;
			
			for (i = 0, l = _4; l != 0; l >>>= ++i)
				if (!ask.add( (char) (i += Long.numberOfTrailingZeros( l ) + 192) )) return false;
			
			return true;
		}
		
		public boolean containsAll( CharList.Producer src ) {
			
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag )) if (!contains( src.value( tag ) )) return false;
			return true;
		}
		
		private CharList.Producer producer;
		
		public CharList.Producer producer() {
			return producer == null ? producer = new CharList.Producer() {
				public int tag() { return 0 < size() ? 0 : -1; }
				
				public int tag( int tag ) {
					tag++;
					
					long l;
					if (tag < 128)
					{
						if (tag < 64)
						{
							if ((l = _1 >>> tag) != 0) return Long.numberOfTrailingZeros( l );
							tag = 0;
						}
						else tag -= 64;
						
						if ((l = _2 >>> tag) != 0) return (Long.numberOfTrailingZeros( l ) + 64);
						tag = 128;
					}
					
					if (tag < 192)
					{
						if ((l = _3 >>> (tag - 128)) != 0) return (Long.numberOfTrailingZeros( l ) + 128);
						
						tag = 0;
					}
					else tag -= 192;
					
					if ((l = _4 >>> tag) != 0) return (Long.numberOfTrailingZeros( l ) + 192);
					
					return -1;
				}
				
				
				public char value( int tag ) { return (char) tag; }
				
			} : producer;
		}
		
		
		@Override public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       compareTo( getClass().cast( obj ) ) == 0;
		}
		
		
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
			} catch (CloneNotSupportedException e)
			{
				e.printStackTrace();
			}
			return null;
			
		}
		
		public StringBuilder toString( StringBuilder dst ) {
			
			if (dst == null) dst = new StringBuilder( size * 10 );
			else dst.ensureCapacity( dst.length() + size * 10 );
			
			final CharList.Producer src = producer();
			for (int tag = src.tag(); tag != -1; dst.append( '\n' ), tag = src.tag( tag ))
			     dst.append( src.value( tag ) );
			
			return dst;
		}
		
		public String toString() { return toString( null ).toString();}
	}
	
	class RW extends R implements CharList.Consumer {
		
		public boolean add( char value ) { return R.add( this, value ); }
		
		
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
		
		public boolean retainAll( CharList.Consumer ask ) {
			boolean ret = false;
			int     i;
			long    l;
			
			for (i = 0, l = _1; l != 0; l >>>= ++i)
				if (!ask.add( (char) (i += Long.numberOfTrailingZeros( l )) ))
				{
					ret = true;
					_1 &= ~(1L << i);
				}
			
			for (i = 0, l = _2; l != 0; l >>>= ++i)
				if (!ask.add( (char) (i += Long.numberOfTrailingZeros( l ) + 64) ))
				{
					ret = true;
					_2 &= ~(1L << i);
				}
			
			for (i = 0, l = _3; l != 0; l >>>= ++i)
				if (!ask.add( (char) (i += Long.numberOfTrailingZeros( l ) + 128) ))
				{
					ret = true;
					_3 &= ~(1L << i);
				}
			
			for (i = 0, l = _4; l != 0; l >>>= ++i)
				if (!ask.add( (char) (i += Long.numberOfTrailingZeros( l ) + 192) ))
				{
					ret = true;
					_4 &= ~(1L << i);
				}
			
			if (ret)
				size = (_1 == 0 ? 0 : Long.bitCount( _1 )) + (_2 == 0 ? 0 : Long.bitCount( _2 )) + (_3 == 0 ? 0 : Long.bitCount( _3 )) + (_4 == 0 ? 0 : Long.bitCount( _4 ));
			
			return ret;
		}
		
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
		
		
		public boolean retainAll( CharList.Producer src ) {
			long
					_1 = 0,
					_2 = 0,
					_3 = 0,
					_4 = 0;
			
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag ))
			{
				final int val = src.value( tag ) & 0xFF;
				
				if (val < 128)
					if (val < 64) _1 |= 1L << val;
					else _2 |= 1L << val - 64;
				else if (val < 192) _3 |= 1L << val - 128;
				else _4 |= 1L << val - 192;
			}
			
			
			boolean ret = false;
			
			if (_1 != this._1)
			{
				this._1 &= _1;
				ret = true;
			}
			if (_2 != this._2)
			{
				this._2 &= _2;
				ret = true;
			}
			if (_3 != this._3)
			{
				this._3 &= _3;
				ret = true;
			}
			if (_4 != this._4)
			{
				this._4 &= _4;
				ret = true;
			}
			
			if (ret) size = (_1 == 0 ? 0 : Long.bitCount( _1 )) + (_2 == 0 ? 0 : Long.bitCount( _2 )) + (_3 == 0 ? 0 : Long.bitCount( _3 )) + (_4 == 0 ? 0 : Long.bitCount( _4 ));
			return ret;
		}
		
		public boolean removeAll( CharList.Producer src ) {
			boolean ret = false;
			
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag ))
			{
				remove( src.value( tag ) );
				ret = true;
			}
			return ret;
		}
		
		public void clear() {
			size = 0;
			_1   = 0;
			_2   = 0;
			_3   = 0;
			_4   = 0;
		}
		
		
		public boolean addAll( CharList.Producer src ) {
			boolean      ret = false;
			char val;
			
			for (int tag = src.tag(); tag != -1; tag = src.tag( tag ))
				if (!contains( val = src.value( tag ) ))
				{
					ret = true;
					R.add( this, val );
				}
			
			return ret;
		}
		
		public CharList.Consumer consumer() {return this; }
		
		public RW clone()                           { return (RW) super.clone(); }
	}
}
