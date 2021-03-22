package org.unirail.collections;

public interface ByteSet {
	
	interface Consumer {
		boolean add( byte value );
		
		boolean add(  Byte      key );
		
		default boolean add( int value )     { return add( (byte) (value & 0xFF) ); }
	}
	
	interface Producer {
		@Nullable int tag();
		
		@Nullable int tag( int tag );
		
		default boolean ok( @Nullable int tag ) {return tag != Nullable.NONE;}
		
		boolean hasNullKey();
		
		byte  key( @Nullable int tag );
		
		
		default StringBuilder toString( StringBuilder dst ) {
			if (dst == null) dst = new StringBuilder( 255 );
			if (hasNullKey()) dst.append( "null\n" );
			
			for (int tag = tag(); ok( tag ); tag = tag( tag ))
			     dst.append( key( tag ) ).append( '\n' );
			return dst;
		}
	}
	
	class R implements Cloneable, Comparable<R> {
		long
				_1,
				_2,
				_3,
				_4;
		
		int size = 0;
		protected boolean hasNull;
		
		public R()                        { }
		
		public R( byte... items ) { for (byte i : items) add( this, i ); }
		
		private static void add( ByteSet.R dst, final byte value ) {
			
			final int val = value & 0xFF;
			
			if (val < 128)
				if (val < 64)
					if ((dst._1 & 1L << val) == 0) dst._1 |= 1L << val;
					else return;
				else if ((dst._2 & 1L << val - 64) == 0) dst._2 |= 1L << val - 64;
				else return;
			else if (val < 192)
				if ((dst._3 & 1L << val - 128) == 0) dst._3 |= 1L << val - 128;
				else return;
			else if ((dst._4 & 1L << val - 192) == 0) dst._4 |= 1L << val - 192;
			else return;
			
			dst.size++;
		}
		
		
		public int size()                           { return hasNull ? size + 1 : size; }
		
		public boolean isEmpty()                    { return size < 1; }
		
		public boolean contains(  Byte      key ) { return key == null ? hasNull : contains( (byte) (key+0) ); }
		
		public boolean contains( byte key ) {
			if (size == 0) return false;
			
			final int val = key & 0xFF;
			return
					(
							val < 128 ?
							val < 64 ? _1 & 1L << val : _2 & 1L << val - 64 :
							val < 192 ? _3 & 1L << val - 128 : _4 & 1L << val - 192
					) != 0;
		}
		
		protected @Nullable int tag()          { return 0 < size ? tag( Nullable.NONE, Nullable.NONE ) : hasNull ? Nullable.VALUE : Nullable.NONE; }
		
		protected @Nullable int tag( int tag ) {return tag == Nullable.VALUE ? Nullable.NONE : tag( tag >> 8, tag & 0xFF );}
		
		private @Nullable int tag( int key, int index ) {
			index++;
			key++;
			
			long l;
			if (key < 128)
			{
				if (key < 64)
				{
					if ((l = _1 >>> key) != 0) return (key + Long.numberOfTrailingZeros( l )) << 8 | index;
					key = 0;
				}
				else key -= 64;
				
				if ((l = _2 >>> key) != 0) return ((key + Long.numberOfTrailingZeros( l ) + 64)) << 8 | index;
				key = 128;
			}
			
			if (key < 192)
			{
				if ((l = _3 >>> (key - 128)) != 0) return ((key + Long.numberOfTrailingZeros( l ) + 128)) << 8 | index;
				
				key = 0;
			}
			else key -= 192;
			
			if ((l = _4 >>> key) != 0) return ((key + Long.numberOfTrailingZeros( l ) + 192)) << 8 | index;
			
			
			return hasNull ? Nullable.VALUE : -1;
		}
		
		
		//Rank returns the number of integers that are smaller or equal to x
		//return inversed value if key does not exists
		protected int rank( byte key ) {
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
		
		public boolean containsAll( Consumer ask ) {
			
			if (hasNull && !ask.add( null )) return false;
			
			int  i;
			long l;
			
			for (i = 0, l = _1; l != 0; l >>>= ++i)
				if (!ask.add( (byte) (i += Long.numberOfTrailingZeros( l )) )) return false;
			
			for (i = 0, l = _2; l != 0; l >>>= ++i)
				if (!ask.add( (byte) (i += Long.numberOfTrailingZeros( l ) + 64) )) return false;
			
			for (i = 0, l = _3; l != 0; l >>>= ++i)
				if (!ask.add( (byte) (i += Long.numberOfTrailingZeros( l ) + 128) )) return false;
			
			for (i = 0, l = _4; l != 0; l >>>= ++i)
				if (!ask.add( (byte) (i += Long.numberOfTrailingZeros( l ) + 192) )) return false;
			
			return true;
		}
		
		public boolean containsAll( Producer src ) {
			if (src.hasNullKey() != hasNull) return false;
			
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag )) if (!contains( src.key( tag ) )) return false;
			return true;
		}
		
		private Producer producer;
		
		public Producer producer() {
			return producer == null ? producer = new Producer() {
				
				public int tag() { return R.this.tag(); }
				
				public int tag( int tag ) {return R.this.tag( tag );}
				
				public boolean hasNullKey() { return hasNull; }
				
				public byte key( int tag ) { return (byte) (tag >> 8); }
				
			} : producer;
		}
		
		
		public boolean equals( Object obj ) {
			
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
			return producer().toString( dst );
		}
		
		public String toString() { return toString( null ).toString(); }
	}
	
	class RW extends R implements Consumer {
		
		public boolean add(  Byte      key ) {
			if (key == null) hasNull = true;
			else add( key+0 );
			return true;
		}
		
		public boolean add( byte value ) {
			R.add( this, value );
			return true;
		}
		
		
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
		
		public boolean retainAll( Consumer ask ) {
			boolean ret = false;
			int     i;
			long    l;
			
			for (i = 0, l = _1; l != 0; l >>>= ++i)
				if (!ask.add( (byte) (i += Long.numberOfTrailingZeros( l )) ))
				{
					ret = true;
					_1 &= ~(1L << i);
				}
			
			for (i = 0, l = _2; l != 0; l >>>= ++i)
				if (!ask.add( (byte) (i += Long.numberOfTrailingZeros( l ) + 64) ))
				{
					ret = true;
					_2 &= ~(1L << i);
				}
			
			for (i = 0, l = _3; l != 0; l >>>= ++i)
				if (!ask.add( (byte) (i += Long.numberOfTrailingZeros( l ) + 128) ))
				{
					ret = true;
					_3 &= ~(1L << i);
				}
			
			for (i = 0, l = _4; l != 0; l >>>= ++i)
				if (!ask.add( (byte) (i += Long.numberOfTrailingZeros( l ) + 192) ))
				{
					ret = true;
					_4 &= ~(1L << i);
				}
			
			if (ret)
				size = (_1 == 0 ? 0 : Long.bitCount( _1 )) + (_2 == 0 ? 0 : Long.bitCount( _2 )) + (_3 == 0 ? 0 : Long.bitCount( _3 )) + (_4 == 0 ? 0 : Long.bitCount( _4 ));
			
			return ret;
		}
		
		public boolean remove(  Byte      key ) {
			if (key == null)
				if (hasNull)
				{
					hasNull = false;
					return true;
				}
				else return false;
			
			return remove( (byte) (key+0) );
		}
		
		public boolean remove( byte value ) {
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
		
		
		public boolean retainAll( Producer src ) {
			long
					_1 = 0,
					_2 = 0,
					_3 = 0,
					_4 = 0;
			
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag ))
			{
				final int val = src.key( tag ) & 0xFF;
				
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
		
		public boolean removeAll( Producer src ) {
			boolean ret = false;
			
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag ))
			{
				remove( src.key( tag ) );
				ret = true;
			}
			return ret;
		}
		
		public void clear() {
			size    = 0;
			_1      = 0;
			_2      = 0;
			_3      = 0;
			_4      = 0;
			hasNull = false;
		}
		
		
		public boolean addAll( Producer src ) {
			boolean      ret = false;
			byte val;
			
			for (int tag = src.tag(); src.ok( tag ); tag = src.tag( tag ))
				if (!contains( val = src.key( tag ) ))
				{
					ret = true;
					R.add( this, val );
				}
			
			return ret;
		}
		
		public Consumer consumer() {return this; }
		
		public RW clone()          { return (RW) super.clone(); }
	}
}
