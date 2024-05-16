package xyz.unirail.collections;


import xyz.unirail.JsonWriter;


public interface ByteSet {
	
	interface NonNullKeysIterator {
		
		int INIT = -1;
		
		static byte key( R src, int token )  { return (byte) token; }
		
		static int index( R src, int token ) { return token >>> 8 & 0xFF; }
		
		static int token( R src, int token ) {
			token += 0b1_0000_0000;
			// index bits                   ___________
			if( src.size << 8 == (token & 0b1_1111_1111_0000_0000) ) return INIT;
			
			final int info = token & ~0xFF;
			//key bits                    _________
			final int key = token + 1 & 0b1111_1111;
			
			long l;
			
			switch( key & 0b1100_0000 )
			{
				case 0b1100_0000:
					l = src._4 >>> key;
					break;
				case 0b1000_0000:
					if( (l = src._3 >>> key) == 0 ) return info | 192 + Long.numberOfTrailingZeros( src._4 );
					break;
				case 0b0100_0000:
					if( (l = src._2 >>> key) == 0 )
						return info | (src._3 != 0 ? 128 + Long.numberOfTrailingZeros( src._3 ) : 192 + Long.numberOfTrailingZeros( src._4 ));
					break;
				default:
					if( (l = src._1 >>> key) == 0 )
						return info | (src._2 != 0 ? 64 + Long.numberOfTrailingZeros( src._2 ) :
						               src._3 != 0 ? 128 + Long.numberOfTrailingZeros( src._3 ) : 192 + Long.numberOfTrailingZeros( src._4 ));
			}
			
			
			return info | key + Long.numberOfTrailingZeros( l );
		}
	}
	
	abstract class R implements Cloneable, JsonWriter.Source {
		long
				_1,
				_2,
				_3,
				_4;
		
		int size = 0;
		
		protected boolean hasNullKey;
		
		protected boolean add( final int value ) {
			
			final int  val = value & 0xFF;
			final long bit = 1L << val;
			switch( val & 0b1100_0000 )
			{
				case 0b1100_0000:
					if( (_4 & bit) == 0 ) _4 |= bit;
					else return false;
					break;
				case 0b1000_0000:
					if( (_3 & bit) == 0 ) _3 |= bit;
					else return false;
					break;
				case 0b0100_0000:
					if( (_2 & bit) == 0 ) _2 |= bit;
					else return false;
					break;
				default:
					if( (_1 & bit) == 0 ) _1 |= bit;
					else return false;
			}
			
			size++;
			return true;
		}
		
		
		public int size()                           { return hasNullKey ? size + 1 : size; }
		
		public boolean isEmpty()                    { return size < 1; }
		
		public boolean contains(  Byte      key ) { return key == null ? hasNullKey : contains( (byte) (key + 0) ); }
		
		public boolean contains( byte key ) {
			if( size() == 0 ) return false;
			
			final int val = key & 0xFF;
			return ((val < 128 ? val < 64 ? _1 : _2 : val < 192 ? _3 : _4) & 1L << val) != 0;
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
				if( val < 64 ) break a;
				if( l != 0 ) ret += Long.bitCount( l );
				base = 64;
				l    = _2;
				if( val < 128 ) break a;
				if( l != 0 ) ret += Long.bitCount( l );
				base = 128;
				l    = _3;
				if( val < 192 ) break a;
				if( l != 0 ) ret += Long.bitCount( l );
				base = 192;
				l    = _4;
			}
			
			while( l != 0 )
			{
				final int s = Long.numberOfTrailingZeros( l );
				
				if( (base += s) == val ) return ret + 1;
				if( val < base ) return ~ret;
				
				ret++;
				base++;
				l >>>= s + 1;
			}
			
			return ~ret;
		}
		
		//Returns true if this set contains all of the elements of the specified collection.
		public boolean containsAll( R subset ) {
			
			if(
					size() < subset.size() ||
					!hasNullKey && subset.hasNullKey ||
					Long.bitCount( _1 ) < Long.bitCount( _1 | subset._1 ) &&
					64 < size && Long.bitCount( _2 ) < Long.bitCount( _2 | subset._2 ) &&
					128 < size && Long.bitCount( _3 ) < Long.bitCount( _3 | subset._3 ) &&
					192 < size && Long.bitCount( _4 ) < Long.bitCount( _4 | subset._4 )
			) return false;
			
			return true;
		}
		
		public int hashCode() { return Array.finalizeHash( Array.hash( Array.hash( Array.hash( Array.hash( hasNullKey ? 184889743 : 22633363, _1 ), _2 ), _3 ), _4 ), size() ); }
		
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		public boolean equals( R other ) {
			
			if( size != other.size ) return false;
			int s = 63 - size & 0b0011_1111;
			
			
			switch( size & 0b1100_0000 )
			{
				case 0b1100_0000:
					return _4 << s == other._4 << s && _3 == other._3 && _2 == other._2 && _1 == other._1;
				
				case 0b1000_0000:
					return _3 << s == other._3 << s && _2 == other._2 && _1 == other._1;
				
				case 0b0100_0000:
					return _2 << s == other._2 << s && _1 == other._1;
			}
			return _1 << s == other._1 << s;
		}
		
		public R clone() {
			try
			{
				return (R) super.clone();
			} catch( CloneNotSupportedException e ) { e.printStackTrace(); }
			return null;
		}
		
		
		public String toString() { return toJSON(); }
		@Override public void toJSON( JsonWriter json ) {
			
			json.enterObject();
			
			if( hasNullKey ) json.name().value();
			
			if( 0 < size )
			{
				json.preallocate( size * 10 );
				
				for( int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
				     json.name( NonNullKeysIterator.key( this, token ) ).value();
			}
			
			json.exitObject();
		}
	}
	
	interface Interface {
		int size();
		
		boolean add(  Byte      key );
		
		boolean add( byte key );
		
		boolean contains(  Byte      key );
		
		boolean contains( byte key );
	}
	
	class RW extends R implements Interface {
		
		public RW()                            { }
		
		public RW( byte... items )     { for( byte i : items ) this.add( i ); }
		
		public RW(  Byte     ... items )     { for(  Byte      key : items ) add( key ); }
		
		public boolean add(  Byte      key ) { return key == null ? !hasNullKey && (hasNullKey = true) : super.add( (byte) (key + 0) ); }
		
		public boolean add( byte key ) { return super.add( key ); }
		
		//Retains only the elements in this set that are contained in the specified collection (optional operation).
		public boolean retainAll( R src ) {
			boolean ret = false;
			
			if( _1 != src._1 )
			{
				_1 &= src._1;
				ret = true;
			}
			if( _2 != src._2 )
			{
				_2 &= src._2;
				ret = true;
			}
			if( _3 != src._3 )
			{
				_3 &= src._3;
				ret = true;
			}
			if( _4 != src._4 )
			{
				_4 &= src._4;
				ret = true;
			}
			
			if( ret ) size = Long.bitCount( _1 ) + Long.bitCount( _2 ) + Long.bitCount( _3 ) + Long.bitCount( _4 );
			return ret;
		}
		
		
		public boolean remove(  Byte      key ) {
			if( key == null )
			{
				boolean ret = hasNullKey;
				hasNullKey = false;
				return ret;
			}
			
			return remove( (byte) (key + 0) );
		}
		
		public boolean remove( byte value ) {
			if( size == 0 ) return false;
			
			final int  val = value & 0xFF;
			final long bit = 1L << val;
			
			switch( val & 0b1100_0000 )
			{
				case 0b1100_0000:
					if( (_4 & bit) == 0 ) return false;
					else _4 &= ~bit;
					break;
				case 0b1000_0000:
					if( (_3 & bit) == 0 ) return false;
					else _3 &= ~bit;
					break;
				case 0b0100_0000:
					if( (_2 & bit) == 0 ) return false;
					else _2 &= ~bit;
					break;
				default:
					if( (_1 & 1L << val) == 0 ) return false;
					else _1 &= ~(1L << val);
			}
			
			size--;
			return true;
		}
		
		
		public RW clear() {
			_1         = 0;
			_2         = 0;
			_3         = 0;
			_4         = 0;
			size       = 0;
			hasNullKey = false;
			return this;
		}
		
		public RW addAll( R src ) {
			
			for( int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( src, token )) != NonNullKeysIterator.INIT; )
			     add( NonNullKeysIterator.key( this, token ) );
			return this;
		}
		
		public RW clone() { return (RW) super.clone(); }
	}
}
