//  MIT License
//
//  Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//  For inquiries, please contact:  al8v5C6HU4UtqE9@gmail.com
//  GitHub Repository: https://github.com/AdHoc-Protocol
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to use,
//  copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
//  the Software, and to permit others to do so, under the following conditions:
//
//  1. The above copyright notice and this permission notice must be included in all
//     copies or substantial portions of the Software.
//
//  2. Users of the Software must provide a clear acknowledgment in their user
//     documentation or other materials that their solution includes or is based on
//     this Software. This acknowledgment should be prominent and easily visible,
//     and can be formatted as follows:
//     "This product includes software developed by Chikirev Sirguy and the Unirail Group
//     (https://github.com/AdHoc-Protocol)."
//
//  3. If you modify the Software and distribute it, you must include a prominent notice
//     stating that you have changed the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM,
//  OUT OF, OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//  SOFTWARE.

package org.unirail.collections;


import org.unirail.JsonWriter;

public interface UByteShortMap {
	
	// Interface for iterating over non-null keys in the map
	interface NonNullKeysIterator {
		int INIT = -1; // Initial token value for iteration
		
		// Get the next token for iteration
		// src: The map instance
		// token: The current token
		static int token( R src, int token ) { return ByteSet.NonNullKeysIterator.token( src.keys, token ); }
		
		// Get the key by the token
		// src: The map instance
		// token: The current token
		static char key( R src, int token ) { return (char) (ByteSet.NonNullKeysIterator.key( null, token )); }
		
		// Get the value by the token
		// src: The map instance
		// token: The current token
		static short value( R src, int token ) { return (short) src.values[ByteSet.NonNullKeysIterator.index( null, token )]; }
	}
	
	// Abstract base class for the map implementation
	abstract class R implements Cloneable, JsonWriter.Source {
		
		ByteSet.RW    keys = new ByteSet.RW(); // Set of keys
		short[] values; // Array of values
		
		// Get the number of key-value pairs in the map
		public int size() { return keys.size(); }
		
		// Check if the map is empty
		public boolean isEmpty() { return keys.isEmpty(); }
		
		// Check if the map contains a given key
		// key: The key to check
		public boolean contains(  Character key ) { return key == null ? keys.contains( null ) : keys.contains( (byte) (key + 0) ); }
		
		// Check if the map contains a given key (int version)
		// key: The key to check
		public boolean contains( int key ) { return keys.contains( (byte) key ); }
		
		// Get the value associated with a given key
		// key: The key to get the value for
		public short value(  Character key ) { return key == null ? NullKeyValue : value( key + 0 ); }
		
		// Get the value associated with a given key (int version)
		// key: The key to get the value for
		public short  value( int key ) { return  (short)  values[keys.rank( (byte) key )]; }
		
		short NullKeyValue = 0; // Value associated with the null key
		
		// Calculate the hash code for the map
		public int hashCode() { return Array.finalizeHash( Array.hash( Array.hash( contains( null ) ? Array.hash( NullKeyValue ) : 77415193, keys ), values ), size() ); }
		
		// Check if the map is equal to another object
		// obj: The object to compare with
		public boolean equals( Object obj ) {
			
			return obj != null &&
			       getClass() == obj.getClass() &&
			       equals( getClass().cast( obj ) );
		}
		
		// Check if the map is equal to another map
		// other: The other map to compare with
		public boolean equals( R other ) {
			return other != null && other.keys.equals( keys ) && other.values.equals( values ) &&
			       (!keys.hasNullKey || NullKeyValue == other.NullKeyValue);
		}
		
		// Clone the map
		@Override public R clone() {
			try
			{
				R dst = (R) super.clone();
				dst.keys   = keys.clone();
				dst.values = values.clone();
				return dst;
				
			} catch( CloneNotSupportedException e )
			{
				e.printStackTrace();
			}
			return null;
		}
		
		// Convert the map to a string representation
		public String toString() { return toJSON(); }
		
		// Write the map to a JSON writer
		// json: The JSON writer to write to
		@Override public void toJSON( JsonWriter json ) {
			json.enterObject();
			
			int size = keys.size();
			if( 0 < size )
			{
				json.preallocate( size * 10 );
				
				if( keys.hasNullKey ) json.name().value( NullKeyValue );
				
				for( int token = NonNullKeysIterator.INIT; (token = NonNullKeysIterator.token( this, token )) != NonNullKeysIterator.INIT; )
				     json.
						     name( NonNullKeysIterator.key( this, token ) ).
						     value( NonNullKeysIterator.value( this, token ) );
			}
			
			json.exitObject();
		}
	}
	
	// Interface for map operations
	interface Interface {
		
		int size(); // Get the number of key-value pairs in the map
		
		boolean contains(  Character key ); // Check if the map contains a given key
		
		boolean contains( int key ); // Check if the map contains a given key (int version)
		
        short value( Character key); // Get the value associated with a given key
		
        short value(int key); // Get the value associated with a given key (int version)
		
        boolean put( Character key, short value); // Put a key-value pair into the map (Byte version)
		
        boolean put(char key, short value); // Put a key-value pair into the map (byte version)
	}
	
    // Mutable implementation of the map
	class RW extends R implements Interface {
		
        // Constructor to initialize the values array
        // length: The initial length of the values array
		public RW( int length ) { values = new short[265 < length ? 256 : length]; }
		
        // Clear the map
		public RW clear() {
			keys.clear();
			return this;
		}
		
        // Put a key-value pair into the map (Byte version)
        // key: The key to put
        // value: The value to associate with the key
		public boolean put(  Character key, short value ) {
			if( key == null )
			{
				NullKeyValue = value;
				boolean ret = keys.contains( null );
				keys.add( null );
				return !ret;
			}
			
			return put( (char) (key + 0), value );
		}
		
        // Put a key-value pair into the map (byte version)
        // key: The key to put
        // value: The value to associate with the key
		public boolean put( char key, short value ) {
			boolean ret = keys.add( (byte) key );
			values[keys.rank( (byte) key ) - 1] = (short) value;
			return ret;
		}
		
        // Remove a key from the map (Byte version)
        // key: The key to remove
		public boolean remove(  Character  key ) { return key == null ? keys.remove( null ) : remove( (char) (key + 0) ); }
		
        // Remove a key from the map (byte version)
        // key: The key to remove
		public boolean remove( char key ) {
			final byte k = (byte) key;
			if( !keys.contains( k ) ) return false;
			
			Array.resize( values, values, keys.rank( k ) - 1, size(), -1 );
			
			keys.remove( k );
			
			return true;
		}
		
        // Clone the mutable map
		@Override public RW clone() { return (RW) super.clone(); }
	}
}
