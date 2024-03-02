package xyz.unirail.collections;


import xyz.unirail.JsonWriter;

import java.util.Arrays;

public interface ObjectList {
	
	abstract class R<V> implements Cloneable, JsonWriter.Source {
		
		
		protected       V[]                        values;
		protected final Array.EqualHashOf<V> equal_hash_V;
		private final   boolean                    V_is_string;
		
		protected R( Class<V> clazzV ) {
			equal_hash_V = Array.get( clazzV );
			V_is_string        = clazzV == String.class;
		}
		
		public R( Array.EqualHashOf<V> equal_hash_V ) {
			this.equal_hash_V = equal_hash_V;
			V_is_string             = false;
		}
		
		int size = 0;
		
		public int size() { return size; }
		
		public V[] toArray( int index, int len, V[] dst ) {
			if( size == 0 ) return null;
			if( dst == null || dst.length < len ) return Arrays.copyOfRange( values, index, len );
			System.arraycopy( values, index, dst, 0, len );
			return dst;
		}
		
		public boolean containsAll( R<V> src ) {
			
			for( int i = 0, s = src.size(); i < s; i++ )
				if( -1 < indexOf( src.get( i ) ) ) return false;
			return true;
		}
		
		public boolean hasValue( int index ) { return index < size && values[index] != null; }
		
		public V get( int index )            { return values[index]; }
		
		
		public int indexOf( V value ) {
			for( int i = 0; i < size; i++ )
				if( equal_hash_V.equals( values[i], value ) ) return i;
			return -1;
		}
		
		public int lastIndexOf( V value ) {
			for( int i = size; -1 < --i; )
				if( equal_hash_V.equals( values[i], value ) ) return i;
			return -1;
		}
		
		@SuppressWarnings( "unchecked" )
		public boolean equals( Object obj ) { return obj != null && getClass() == obj.getClass() && equals( (R<V>) obj ); }
		
		public boolean equals( R<V> other ) { return other != null && size == other.size && equal_hash_V.equals( values, other.values, size ); }
		
		public int hashCode()               { return Array.finalizeHash( V_is_string ? Array.hash( (String[]) values, size ) : equal_hash_V.hashCode( values, size ), size() ); }
		
		
		@SuppressWarnings( "unchecked" )
		public R<V> clone() {
			try
			{
				R<V> dst = (R<V>) super.clone();
				if( dst.values != null ) dst.values = values.clone();
				
			} catch( CloneNotSupportedException e ) { e.printStackTrace(); }
			
			return null;
		}
		
		
		public String toString() { return toJSON(); }
		
		@Override public void toJSON( JsonWriter json ) {
			json.enterArray();
			
			int size = size();
			
			if( 0 < size )
			{
				json.preallocate( size * 10 );
				int i = 0;
				if( V_is_string )
					for( String[] strs = (String[]) values; i < size; i++ ) json.value( strs[i] );
				else
					for( ; i < size; i++ ) json.value( values[i] );
			}
			json.exitArray();
		}
	}
	
	class RW<V> extends R<V> {
		public final V default_value;
		
		@SuppressWarnings( "unchecked" )
		public RW( Class<V> clazz, int length ) { this( Array.get( clazz ), length ); }
		
		public RW( Array.EqualHashOf<V> equal_hash_V, int length ) {
			super( equal_hash_V );
			default_value = null;
			
			values = length == 0 ? equal_hash_V.OO : equal_hash_V.copyOf( null, length );
		}
		
		
		public RW( Class<V> clazz, V default_value, int size ) { this( Array.get( clazz ), default_value, size ); }
		
		public RW( Array.EqualHashOf<V> equal_hash_V, V default_value, int size ) {
			super( equal_hash_V );
			this.default_value = default_value;
			
			if( (this.size = size) == 0 )
			{
				values = equal_hash_V.OO;
				return;
			}
			
			values = equal_hash_V.copyOf( null, size );
			if( default_value == null ) return;
			while( -1 < --size ) values[size] = default_value;
		}
		
		
		public RW<V> add1( V value ) { return add1( size, value ); }
		
		public RW<V> add1( int index, V value ) {
			int max = Math.max( index, size + 1 );
			
			size = Array.resize( values, values.length <= max ? values = equal_hash_V.copyOf( null, max + max / 2 ) : values, index, size, 1 );
			
			values[index] = value;
			return this;
		}
		
		@SafeVarargs public final RW<V> add( V... items ) { return add( size(), items, 0, items.length ); }
		
		public RW<V> add( int index, V[] src, int src_index, int len ) {
			int max = Math.max( index, size ) + len;
			
			size = Array.resize( values, values.length < max ? values = equal_hash_V.copyOf( null, max + max / 2 ) : values, index, size, len );
			
			System.arraycopy( src, src_index, values, index, len );
			return this;
		}
		
		public RW<V> remove() { return remove( size - 1 ); }
		
		public RW<V> remove( int index ) {
			size = Array.resize( values, values, index, size, -1 );
			return this;
		}
		
		public RW<V> remove_fast( int index ) {
			if( size < 1 || size <= index ) return this;
			size--;
			values[index] = values[size];
			values[size]  = null;
			return this;
		}
		
		public RW<V> set1( V value ) { return set1( size, value ); }
		
		public RW<V> set1( int index, V value ) {
			if( size <= index )
			{
				if( values.length <= index ) values = equal_hash_V.copyOf( values, index + index / 2 );
				size = index + 1;
			}
			values[index] = value;
			return this;
		}
		
		@SafeVarargs
		public final RW<V> set( int index, V... src ) { return set( index, src, 0, src.length ); }
		
		
		public RW<V> set( int index, V[] src, int src_index, int len ) {
			for( int i = len; -1 < --i; )
			     set1( index + i, src[src_index + i] );
			return this;
		}
		
		
		public RW<V> removeAll( R<V> src ) {
			for( int i = 0, max = src.size(), p; i < max; i++ )
			     removeAll( src.get( i ) );
			return this;
		}
		
		public RW<V> removeAll( V src ) {
			for( int k; -1 < (k = indexOf( src )); ) remove( k );
			return this;
		}
		
		public RW<V> removeAll_fast( V src ) {
			for( int k; -1 < (k = indexOf( src )); ) remove_fast( k );
			return this;
		}
		
		public RW<V> retainAll( R<V> chk ) {
			for( int i = 0; i < size; i++ )
			{
				final V val = values[i];
				if( chk.indexOf( val ) == -1 ) removeAll( val );
			}
			return this;
		}
		
		public RW<V> clone() { return (RW<V>) super.clone(); }
		
		public RW<V> clear() {
			if( size < 1 ) return this;
			Arrays.fill( values, 0, size - 1, default_value );//release objects
			size = 0;
			return this;
		}
		
		public RW<V> length( int items ) {
			if( items < 1 )
			{
				values = equal_hash_V.OO;
				size   = 0;
				return this;
			}
			
			equal_hash_V.copyOf( values, items );
			
			if( items < size ) size = items;
			return this;
		}
		
		public RW<V> size( int size ) {
			if( size < 1 )
			{
				clear();
				return this;
			}
			
			if( values.length < size )
			{
				equal_hash_V.copyOf( values, size );
				if( default_value != null ) Arrays.fill( values, this.size, (this.size = size) - 1, default_value );
				return this;
			}
			
			if( this.size < size )
			{
				if( default_value != null ) Arrays.fill( values, this.size, size - 1, default_value );
				this.size = size;
			}
			return this;
		}
		
		private static final Object OBJECT = new Array.EqualHashOf<>( RW.class );
	}
	
	
	@SuppressWarnings( "unchecked" )
	static <V> Array.EqualHashOf<RW<V>> equal_hash() { return (Array.EqualHashOf<RW<V>>) RW.OBJECT; }
}
	