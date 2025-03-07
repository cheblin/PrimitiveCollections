//MIT License
//
//Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//For inquiries, please contact:  al8v5C6HU4UtqE9@gmail.com
//GitHub Repository: https://github.com/AdHoc-Protocol
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//1. The above copyright notice and this permission notice must be included in all
//   copies or substantial portions of the Software.
//
//2. Users of the Software must provide a clear acknowledgment in their user
//   documentation or other materials that their solution includes or is based on
//   this Software. This acknowledgment should be prominent and easily visible,
//   and can be formatted as follows:
//   "This product includes software developed by Chikirev Sirguy and the Unirail Group
//   (https://github.com/AdHoc-Protocol)."
//
//3. If you modify the Software and distribute it, you must include a prominent notice
//   stating that you have changed the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package org.unirail.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

/**
 * {@code Array} is a utility interface providing efficient array operations
 * such as copying, resizing, filling, equality checks, and hash code generation.
 * It is designed to handle various array types, including primitives and objects,
 * with optimized implementations for performance.
 *
 * <p>This interface also includes an inner class {@code EqualHashOf} which serves
 * a similar purpose to C#'s {@code IEqualityComparer}, enabling custom equality
 * and hash code computations for array elements.
 */
public interface Array {
	/**
	 * {@code EqualHashOf<T>} provides functionalities for comparing arrays and computing
	 * their hash codes, similar to C#'s {@code IEqualityComparer<T>}. It offers optimized
	 * implementations for different array types to enhance performance.
	 *
	 * @param <T> The type of the array elements.
	 */
	class EqualHashOf< T > { //C#  IEqualityComparer (https://learn.microsoft.com/en-us/dotnet/api/system.collections.generic.iequalitycomparer-1?view=net-8.0) equivalent
		/**
		 * A cached empty array instance of type {@code T}.
		 */
		public final Object O;
		/**
		 * A cached empty array of type {@code T[]} (array of arrays).
		 */
		public final T[]    OO;
		
		/**
		 * Creates a copy of the source array with the specified length.
		 * If the specified length is less than 1, it returns a cached empty array.
		 *
		 * @param src The source array to copy, can be null.
		 * @param len The desired length of the copy.
		 * @return A new array of type {@code T[]} which is a copy of the source array,
		 * or an empty array if {@code len} is less than 1.
		 */
		@SuppressWarnings( "unchecked" )
		public T[] copyOf( T[] src, int len ) {
			return len < 1 ?
					OO :
					Arrays.copyOf( src == null ?
							               OO :
							               src, len );
		}
		
		/**
		 * Indicates whether the handled type {@code T} is an array type itself.
		 */
		private final boolean array;
		
		/**
		 * Constructs an {@code EqualHashOf} instance for a specific class.
		 * Initializes cached empty array instances based on the provided class type.
		 *
		 * @param clazz The class representing the type {@code T}.
		 */
		@SuppressWarnings( "unchecked" )
		public EqualHashOf( Class< T > clazz ) {
			
			OO = ( T[] ) java.lang.reflect.Array.newInstance( clazz, 0 );
			
			O = ( array = clazz.isArray() ) ?
					java.lang.reflect.Array.newInstance( clazz.componentType(), 0 ) :
					OO;
		}
		
		/**
		 * Computes the hash code for the given object {@code src}.
		 * If the type {@code T} is an array, it computes the hash code of the array;
		 * otherwise, it computes the hash code of the object itself.
		 *
		 * @param src The object for which to compute the hash code.
		 * @return The computed hash code.
		 */
		@SuppressWarnings( "unchecked" )
		public int hashCode( T src ) {
			return array ?
					hashCode( ( T[] ) src ) :
					hash( src );
		}
		
		/**
		 * Checks if two objects are deeply equal.
		 *
		 * @param v1 The first object.
		 * @param v2 The second object.
		 * @return {@code true} if the objects are deeply equal, {@code false} otherwise.
		 */
		public boolean equals( T v1, T v2 ) { return Objects.deepEquals( v1, v2 ); }
		
		/**
		 * Computes the hash code for the given array.
		 * Handles null arrays by returning 0.
		 *
		 * @param src The array for which to compute the hash code.
		 * @return The computed hash code.
		 */
		public int hashCode( T[] src ) {
			return src == null ?
					0 :
					hashCode( src, src.length );
		}
		
		/**
		 * Computes the hash code for a given array up to the specified size.
		 * This method provides optimized hash code calculation, especially for primitive type arrays.
		 * For array types, it iterates through the array and combines hash codes of elements.
		 * For non-array types, it applies a specific hash algorithm that may include range difference
		 * optimization for potentially sorted or partially sorted arrays.
		 *
		 * @param src  The array for which to compute the hash code.
		 * @param size The number of elements to consider for hash code computation.
		 * @return The computed hash code.
		 */
		@SuppressWarnings( "unchecked" )
		public int hashCode( T[] src, int size ) {
			int seed = EqualHashOf.class.hashCode();
			if( array ) {
				while( -1 < --size )
					seed = ( size + 10153331 ) + hash( seed, hash( src[ size ] ) );
				return seed;
			}
			
			switch( size ) {
				case 0:
					return Array.finalizeHash( seed, 0 );
				case 1:
					return Array.finalizeHash( Array.mix( seed, Array.hash( src[ 0 ] ) ), 1 );
			}
			
			final int initial   = Array.hash( src[ 0 ] );
			int       prev      = Array.hash( src[ 1 ] );
			final int rangeDiff = prev - initial;
			int       h         = Array.mix( seed, initial );
			
			for( int i = 2; i < size; ++i ) {
				h = Array.mix( h, prev );
				final int hash = Array.hash( src[ i ] );
				if( rangeDiff != hash - prev ) {
					for( h = Array.mix( h, hash ), ++i; i < size; ++i )
					     h = Array.mix( h, Array.hash( src[ i ] ) );
					
					return Array.finalizeHash( h, size );
				}
				prev = hash;
			}
			
			return Array.avalanche( Array.mix( Array.mix( h, rangeDiff ), prev ) );
		}
		
		/**
		 * Checks if two arrays are equal up to the specified length.
		 * It handles null arrays, length mismatches, and performs element-wise equality checks.
		 * For arrays of arrays, it uses {@code Arrays.deepEquals} for element comparison;
		 * otherwise, it uses the {@code equals} method of this {@code EqualHashOf} instance.
		 *
		 * @param O   The first array.
		 * @param X   The second array.
		 * @param len The length up to which to compare the arrays.
		 * @return {@code true} if the arrays are equal up to the specified length, {@code false} otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		public boolean equals( T[] O, T[] X, int len ) {
			if( O != X ) {
				if( O == null || X == null || O.length < len || X.length < len )
					return false;
				if( array ) {
					for( T o, x; -1 < --len; )
						if( ( o = O[ len ] ) != ( x = X[ len ] ) )
							if( o == null || x == null || !Arrays.deepEquals( ( T[] ) o, ( T[] ) x ) )
								return false;
				}
				else
					for( T o, x; -1 < --len; )
						if( ( o = O[ len ] ) != ( x = X[ len ] ) )
							if( o == null || x == null || !equals( o, x ) )
								return false;
			}
			return true;
		}
		
		/**
		 * Checks if two arrays are equal.
		 * It uses {@link #equals(Object[], Object[], int)} to compare the arrays up to their lengths.
		 *
		 * @param O The first array.
		 * @param X The second array.
		 * @return {@code true} if the arrays are equal, {@code false} otherwise.
		 */
		public boolean equals( T[] O, T[] X ) { return O == X || O != null && X != null && O.length == X.length && equals( O, X, O.length ); }
		
		/**
		 * Singleton instance for boolean arrays.
		 */
		public static final _booleans booleans = new _booleans();
		
		/**
		 * {@code _booleans} is a specialized {@code EqualHashOf} for {@code boolean[]} arrays.
		 * It leverages {@link Arrays#hashCode(boolean[])} and {@link Arrays#equals(boolean[], boolean[])}
		 * for optimized hash code and equality computations.
		 */
		public static final class _booleans extends EqualHashOf< boolean[] > {
			/**
			 * Cached empty boolean array.
			 */
			public static final boolean[] O = new boolean[ 0 ];
			
			private _booleans() { super( boolean[].class ); }
			
			@Override
			public int hashCode( boolean[] src ) { return Arrays.hashCode( src ); }
			
			@Override
			public boolean equals( boolean[] v1, boolean[] v2 ) { return Arrays.equals( v1, v2 ); }
			
			@Override
			public int hashCode( boolean[][] src, int size ) {
				int hash = boolean[][].class.hashCode();
				while( -1 < --size )
					hash = ( size + 10153331 ) + hash( hash, Arrays.hashCode( src[ size ] ) );
				return hash;
			}
			
			@Override
			public boolean equals( boolean[][] O, boolean[][] X, int len ) {
				if( O != X ) {
					if( O == null || X == null || O.length < len || X.length < len )
						return false;
					for( boolean[] o, x; -1 < --len; )
						if( ( o = O[ len ] ) != ( x = X[ len ] ) )
							if( o == null || x == null || !Arrays.equals( o, x ) )
								return false;
				}
				return true;
			}
		}
		
		/**
		 * Singleton instance for byte arrays.
		 */
		public static final _bytes bytes = new _bytes();
		
		/**
		 * {@code _bytes} is a specialized {@code EqualHashOf} for {@code byte[]} arrays.
		 * It provides optimized hash code computation for byte arrays, leveraging bitwise
		 * operations for enhanced performance. Equality checks are handled by {@link Arrays#equals(byte[], byte[])}.
		 */
		public static final class _bytes extends EqualHashOf< byte[] > {
			/**
			 * Cached empty byte array.
			 */
			public static final byte[] O = new byte[ 0 ];
			
			private _bytes() { super( byte[].class ); }
			
			@Override
			public int hashCode( byte[] src ) { return Arrays.hashCode( src ); }
			
			@Override
			public boolean equals( byte[] v1, byte[] v2 ) { return Arrays.equals( v1, v2 ); }
			
			@Override
			public int hashCode( byte[][] src, int size ) {
				int hash = byte[][].class.hashCode();
				while( -1 < --size ) {
					final byte[] data = src[ size ];
					int          len  = data.length, i, k = 0;
					
					// Optimized byte array hash calculation by processing 4 bytes at a time
					for( i = 0; 3 < len; i += 4, len -= 4 )
					     hash = mix( hash, data[ i ] & 0xFF | ( data[ i + 1 ] & 0xFF ) << 8 | ( data[ i + 2 ] & 0xFF ) << 16 | ( data[ i + 3 ] & 0xFF ) << 24 );
					switch( len ) {
						case 3:
							k ^= ( data[ i + 2 ] & 0xFF ) << 16;
						case 2:
							k ^= ( data[ i + 1 ] & 0xFF ) << 8;
					}
					
					hash = finalizeHash( mixLast( hash, k ^ data[ i ] & 0xFF ), data.length );
				}
				
				return finalizeHash( hash, src.length );
			}
			
			@Override
			public boolean equals( byte[][] O, byte[][] X, int len ) {
				if( O != X ) {
					if( O == null || X == null || O.length < len || X.length < len )
						return false;
					for( byte[] o, x; -1 < --len; )
						if( ( o = O[ len ] ) != ( x = X[ len ] ) )
							if( o == null || x == null || !Arrays.equals( o, x ) )
								return false;
				}
				return true;
			}
		}
		
		/**
		 * Singleton instance for short arrays.
		 */
		public static final _shorts shorts = new _shorts();
		
		/**
		 * {@code _shorts} is a specialized {@code EqualHashOf} for {@code short[]} arrays.
		 * It uses {@link Arrays#hashCode(short[])} and {@link Arrays#equals(short[], short[])}
		 * for hash code and equality computations.
		 */
		public static final class _shorts extends EqualHashOf< short[] > {
			/**
			 * Cached empty short array.
			 */
			public static final short[] O = new short[ 0 ];
			
			private _shorts() { super( short[].class ); }
			
			@Override
			public int hashCode( short[] src ) { return Arrays.hashCode( src ); }
			
			@Override
			public boolean equals( short[] v1, short[] v2 ) { return Arrays.equals( v1, v2 ); }
			
			@Override
			public int hashCode( short[][] src, int size ) {
				int hash = short[][].class.hashCode();
				while( -1 < --size )
					hash = ( size + 10153331 ) + hash( hash, Arrays.hashCode( src[ size ] ) );
				return hash;
			}
			
			@Override
			public boolean equals( short[][] O, short[][] X, int len ) {
				if( O != X ) {
					if( O == null || X == null || O.length < len || X.length < len )
						return false;
					for( short[] o, x; -1 < --len; )
						if( ( o = O[ len ] ) != ( x = X[ len ] ) )
							if( o == null || x == null || !Arrays.equals( o, x ) )
								return false;
				}
				return true;
			}
		}
		
		/**
		 * Singleton instance for char arrays.
		 */
		public static final _chars chars = new _chars();
		
		/**
		 * {@code _chars} is a specialized {@code EqualHashOf} for {@code char[]} arrays.
		 * It uses {@link Arrays#hashCode(char[])} and {@link Arrays#equals(char[], char[])}
		 * for hash code and equality computations.
		 */
		public static final class _chars extends EqualHashOf< char[] > {
			/**
			 * Cached empty char array.
			 */
			
			public static final char[] O = new char[ 0 ];
			
			private _chars() { super( char[].class ); }
			
			@Override
			public int hashCode( char[] src ) { return Arrays.hashCode( src ); }
			
			@Override
			public boolean equals( char[] v1, char[] v2 ) { return Arrays.equals( v1, v2 ); }
			
			@Override
			public int hashCode( char[][] src, int size ) {
				int hash = char[][].class.hashCode();
				while( -1 < --size )
					hash = ( size + 10153331 ) + hash( hash, Arrays.hashCode( src[ size ] ) );
				return hash;
			}
			
			@Override
			public boolean equals( char[][] O, char[][] X, int len ) {
				if( O != X ) {
					if( O == null || X == null || O.length < len || X.length < len )
						return false;
					for( char[] o, x; -1 < --len; )
						if( ( o = O[ len ] ) != ( x = X[ len ] ) )
							if( o == null || x == null || !Arrays.equals( o, x ) )
								return false;
				}
				return true;
			}
		}
		
		/**
		 * Singleton instance for int arrays.
		 */
		public static final _ints ints = new _ints();
		
		/**
		 * {@code _ints} is a specialized {@code EqualHashOf} for {@code int[]} arrays.
		 * It uses {@link Arrays#hashCode(int[])} and {@link Arrays#equals(int[], int[])}
		 * for hash code and equality computations.
		 */
		public static final class _ints extends EqualHashOf< int[] > {
			/**
			 * Cached empty int array.
			 */
			
			public static final int[] O = new int[ 0 ];
			
			private _ints() { super( int[].class ); }
			
			@Override
			public int hashCode( int[] src ) { return Arrays.hashCode( src ); }
			
			@Override
			public boolean equals( int[] v1, int[] v2 ) { return Arrays.equals( v1, v2 ); }
			
			@Override
			public int hashCode( int[][] src, int size ) {
				int hash = int[][].class.hashCode();
				while( -1 < --size )
					hash = ( size + 10153331 ) + hash( hash, Arrays.hashCode( src[ size ] ) );
				return hash;
			}
			
			@Override
			public boolean equals( int[][] O, int[][] X, int len ) {
				if( O != X ) {
					if( O == null || X == null || O.length < len || X.length < len )
						return false;
					for( int[] o, x; -1 < --len; )
						if( ( o = O[ len ] ) != ( x = X[ len ] ) )
							if( o == null || x == null || !Arrays.equals( o, x ) )
								return false;
				}
				return true;
			}
		}
		
		/**
		 * Singleton instance for long arrays.
		 */
		public static final _longs longs = new _longs();
		
		/**
		 * {@code _longs} is a specialized {@code EqualHashOf} for {@code long[]} arrays.
		 * It uses {@link Arrays#hashCode(long[])} and {@link Arrays#equals(long[], long[])}
		 * for hash code and equality computations.
		 */
		public static final class _longs extends EqualHashOf< long[] > {
			/**
			 * Cached empty long array.
			 */
			public static final long[] O = new long[ 0 ];
			
			private _longs() { super( long[].class ); }
			
			@Override
			public int hashCode( long[] src ) { return Arrays.hashCode( src ); }
			
			@Override
			public boolean equals( long[] v1, long[] v2 ) { return Arrays.equals( v1, v2 ); }
			
			@Override
			public int hashCode( long[][] src, int size ) {
				int hash = long[][].class.hashCode();
				while( -1 < --size )
					hash = ( size + 10153331 ) + hash( hash, Arrays.hashCode( src[ size ] ) );
				return hash;
			}
			
			@Override
			public boolean equals( long[][] O, long[][] X, int len ) {
				if( O != X ) {
					if( O == null || X == null || O.length < len || X.length < len )
						return false;
					for( long[] o, x; -1 < --len; )
						if( ( o = O[ len ] ) != ( x = X[ len ] ) )
							if( o == null || x == null || !Arrays.equals( o, x ) )
								return false;
				}
				return true;
			}
		}
		
		/**
		 * Singleton instance for float arrays.
		 */
		public static final _floats floats = new _floats();
		
		/**
		 * {@code _floats} is a specialized {@code EqualHashOf} for {@code float[]} arrays.
		 * It uses {@link Arrays#hashCode(float[])} and {@link Arrays#equals(float[], float[])}
		 * for hash code and equality computations.
		 */
		public static final class _floats extends EqualHashOf< float[] > {
			/**
			 * Cached empty float array.
			 */
			public static final float[] O = new float[ 0 ];
			
			private _floats() { super( float[].class ); }
			
			@Override
			public int hashCode( float[] src ) { return Arrays.hashCode( src ); }
			
			@Override
			public boolean equals( float[] v1, float[] v2 ) { return Arrays.equals( v1, v2 ); }
			
			@Override
			public int hashCode( float[][] src, int size ) {
				int hash = float[][].class.hashCode();
				while( -1 < --size )
					hash = ( size + 10153331 ) + hash( hash, Arrays.hashCode( src[ size ] ) );
				return hash;
			}
			
			@Override
			public boolean equals( float[][] O, float[][] X, int len ) {
				if( O != X ) {
					if( O == null || X == null || O.length < len || X.length < len )
						return false;
					for( float[] o, x; -1 < --len; )
						if( ( o = O[ len ] ) != ( x = X[ len ] ) )
							if( o == null || x == null || !Arrays.equals( o, x ) )
								return false;
				}
				return true;
			}
		}
		
		/**
		 * Singleton instance for double arrays.
		 */
		public static final _doubles doubles = new _doubles();
		
		/**
		 * {@code _doubles} is a specialized {@code EqualHashOf} for {@code double[]} arrays.
		 * It uses {@link Arrays#hashCode(double[])} and {@link Arrays#equals(double[], double[])}
		 * for hash code and equality computations.
		 */
		public static final class _doubles extends EqualHashOf< double[] > {
			/**
			 * Cached empty double array.
			 */
			
			public static final double[] O = new double[ 0 ];
			
			private _doubles() { super( double[].class ); }
			
			@Override
			public int hashCode( double[] src ) { return Arrays.hashCode( src ); }
			
			@Override
			public boolean equals( double[] v1, double[] v2 ) { return Arrays.equals( v1, v2 ); }
			
			@Override
			public int hashCode( double[][] src, int size ) {
				int hash = double[][].class.hashCode();
				while( -1 < --size )
					hash = ( size + 10153331 ) + hash( hash, Arrays.hashCode( src[ size ] ) );
				return hash;
			}
			
			@Override
			public boolean equals( double[][] O, double[][] X, int len ) {
				if( O != X ) {
					if( O == null || X == null || O.length < len || X.length < len )
						return false;
					for( double[] o, x; -1 < --len; )
						if( ( o = O[ len ] ) != ( x = X[ len ] ) )
							if( o == null || x == null || !Arrays.equals( o, x ) )
								return false;
				}
				return true;
			}
		}
		
		/**
		 * {@code objects} interface provides static methods for equality and hash code
		 * computation for 2D Object arrays ({@code Object[][]}).
		 */
		public interface objects {
			/**
			 * Cached empty Object array.
			 */
			
			Object[]   O  = new Object[ 0 ];
			/**
			 * Cached empty 2D Object array.
			 */
			Object[][] OO = new Object[ 0 ][];
			
			/**
			 * Checks if two 2D object arrays are deeply equal.
			 * It uses {@link Arrays#equals(Object[], Object[])} to compare each sub-array.
			 *
			 * @param O The first 2D array.
			 * @param X The second 2D array.
			 * @return {@code true} if the arrays are deeply equal, {@code false} otherwise.
			 */
			static boolean equals( Object[][] O, Object[][] X ) {
				if( O != X ) {
					if( O == null || X == null || O.length != X.length )
						return false;
					for( int i = O.length; -1 < --i; )
						if( !Arrays.equals( O[ i ], X[ i ] ) )
							return false;
				}
				return true;
			}
			
			/**
			 * Computes the hash code of a 2D object array.
			 * It iterates through each sub-array and XORs their hash codes.
			 *
			 * @param hash The initial hash value to start with.
			 * @param src  The source 2D array.
			 * @return The computed hash code.
			 */
			static int hash( int hash, Object[][] src ) {
				if( src == null )
					return hash ^ 10153331;
				for( Object[] s : src )
					hash ^= Arrays.hashCode( s );
				return hash;
			}
		}
	}
	
	/**
	 * Creates a copy of the source array with the specified length.
	 * If the length is less than 1, it returns a cached empty array of the same type.
	 *
	 * @param <T> The type of the array elements.
	 * @param src The source array to copy, can be null.
	 * @param len The desired length of the copy.
	 * @return A new array of type {@code T[]} which is a copy of the source array,
	 * or an empty array if {@code len} is less than 1.
	 */
	@SuppressWarnings( "unchecked" )
	static < T > T[] copyOf( T[] src, int len ) {
		return len < 1 ?
				( T[] ) get( src.getClass() ).O :
				Arrays.copyOf( src, len );
	}
	
	/**
	 * Resizes the given array to the new length.
	 * If the new length is the same as the original length, the original array is returned.
	 * If the new length is greater than the original, the new elements are filled with the provided value.
	 * If the new length is less than 1, it returns a cached empty array of the same type.
	 *
	 * @param <T> The type of the array elements.
	 * @param src The source array to resize, can be null.
	 * @param len The desired new length.
	 * @param val The value to fill new elements with if the array is enlarged.
	 * @return A resized array of type {@code T[]}, or a cached empty array if {@code len} is less than 1.
	 */
	static < T > T[] resize( T[] src, int len, T val ) {
		return src == null ?
				fill( copyOf( ( T[] ) null, len ), val ) :
				src.length == len ?
						src
						:
						fill( copyOf( src, len ), src.length, len, val );
	}
	
	/**
	 * Fills the entire array with the specified value.
	 *
	 * @param <T> The type of the array elements.
	 * @param dst The array to fill.
	 * @param val The value to fill the array with.
	 * @return The filled array ({@code dst}).
	 */
	static < T > T[] fill( T[] dst, T val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Fills a portion of the array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
	 * with the specified value.
	 *
	 * @param <T>       The type of the array elements.
	 * @param dst       The array to fill.
	 * @param fromIndex The starting index (inclusive).
	 * @param toIndex   The ending index (exclusive).
	 * @param val       The value to fill the array portion with.
	 * @return The filled array ({@code dst}).
	 */
	static < T > T[] fill( T[] dst, int fromIndex, int toIndex, T val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Creates a copy of the source 2D array with the specified length.
	 * If the length is less than 1, it returns a cached empty 2D array of the same type.
	 *
	 * @param <T> The type of the array elements.
	 * @param src The source 2D array to copy, can be null.
	 * @param len The desired length of the copy.
	 * @return A new 2D array of type {@code T[][]} which is a copy of the source array,
	 * or an empty 2D array if {@code len} is less than 1.
	 */
	@SuppressWarnings( "unchecked" )
	static < T > T[][] copyOf( T[][] src, int len ) {
		return len < 1 ?
				( T[][] ) get( src.getClass() ).OO :
				Arrays.copyOf( src, len );
	}
	
	/**
	 * Resizes the given 2D array to the new length.
	 * If the new length is the same as the original length, the original array is returned.
	 * If the new length is greater than the original, the new rows are filled with the provided array value.
	 * If the new length is less than 1, it returns a cached empty 2D array of the same type.
	 *
	 * @param <T> The type of the array elements.
	 * @param src The source 2D array to resize, can be null.
	 * @param len The desired new length.
	 * @param val The array value to fill new rows with if the 2D array is enlarged.
	 * @return A resized 2D array of type {@code T[][]}, or a cached empty 2D array if {@code len} is less than 1.
	 */
	static < T > T[][] resize( T[][] src, int len, T[] val ) {
		return src == null ?
				fill( copyOf( ( T[][] ) null, len ), val ) :
				src.length == len ?
						src
						:
						fill( copyOf( src, len ), src.length, len, val );
	}
	
	/**
	 * Fills the entire 2D array with the specified array value.
	 *
	 * @param <T> The type of the array elements.
	 * @param dst The 2D array to fill.
	 * @param val The array value to fill the 2D array with.
	 * @return The filled 2D array ({@code dst}).
	 */
	static < T > T[][] fill( T[][] dst, T[] val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Fills a portion of the 2D array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
	 * with the specified array value.
	 *
	 * @param <T>       The type of the array elements.
	 * @param dst       The 2D array to fill.
	 * @param fromIndex The starting index (inclusive).
	 * @param toIndex   The ending index (exclusive).
	 * @param val       The array value to fill the 2D array portion with.
	 * @return The filled 2D array ({@code dst}).
	 */
	static < T > T[][] fill( T[][] dst, int fromIndex, int toIndex, T[] val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Creates a copy of the source boolean array with the specified length.
	 * If the length is less than 1, it returns a cached empty boolean array.
	 *
	 * @param src The source boolean array to copy, can be null.
	 * @param len The desired length of the copy.
	 * @return A new boolean array which is a copy of the source array,
	 * or a cached empty boolean array if {@code len} is less than 1.
	 */
	static boolean[] copyOf( boolean[] src, int len ) {
		return len < 1 ?
				EqualHashOf._booleans.O :
				Arrays.copyOf( src == null ?
						               EqualHashOf._booleans.O :
						               src, len );
	}
	
	/**
	 * Resizes the given boolean array to the new length.
	 * If the new length is the same as the original length, the original array is returned.
	 * If the new length is greater than the original, the new elements are filled with the provided boolean value.
	 * If the new length is less than 1, it returns a cached empty boolean array.
	 *
	 * @param src The source boolean array to resize, can be null.
	 * @param len The desired new length.
	 * @param val The boolean value to fill new elements with if the array is enlarged.
	 * @return A resized boolean array, or a cached empty boolean array if {@code len} is less than 1.
	 */
	static boolean[] resize( boolean[] src, int len, boolean val ) {
		return src == null ?
				fill( copyOf( EqualHashOf._booleans.O, len ), val ) :
				src.length == len ?
						src
						:
						fill( copyOf( src, len ), src.length, len, val );
	}
	
	/**
	 * Fills the entire boolean array with the specified value.
	 *
	 * @param dst The boolean array to fill.
	 * @param val The boolean value to fill the array with.
	 * @return The filled boolean array ({@code dst}).
	 */
	static boolean[] fill( boolean[] dst, boolean val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Fills a portion of the boolean array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
	 * with the specified value.
	 *
	 * @param dst       The boolean array to fill.
	 * @param fromIndex The starting index (inclusive).
	 * @param toIndex   The ending index (exclusive).
	 * @param val       The boolean value to fill the array portion with.
	 * @return The filled boolean array ({@code dst}).
	 */
	static boolean[] fill( boolean[] dst, int fromIndex, int toIndex, boolean val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Creates a copy of the source byte array with the specified length.
	 * If the length is less than 1, it returns a cached empty byte array.
	 *
	 * @param src The source byte array to copy, can be null.
	 * @param len The desired length of the copy.
	 * @return A new byte array which is a copy of the source array,
	 * or a cached empty byte array if {@code len} is less than 1.
	 */
	static byte[] copyOf( byte[] src, int len ) {
		return len < 1 ?
				EqualHashOf._bytes.O :
				Arrays.copyOf( src == null ?
						               EqualHashOf._bytes.O :
						               src, len );
	}
	
	/**
	 * Resizes the given byte array to the new length.
	 * If the new length is the same as the original length, the original array is returned.
	 * If the new length is greater than the original, the new elements are filled with the provided byte value.
	 * If the new length is less than 1, it returns a cached empty byte array.
	 *
	 * @param src The source byte array to resize, can be null.
	 * @param len The desired new length.
	 * @param val The byte value to fill new elements with if the array is enlarged.
	 * @return A resized byte array, or a cached empty byte array if {@code len} is less than 1.
	 */
	static byte[] resize( byte[] src, int len, byte val ) {
		return src == null ?
				fill( copyOf( EqualHashOf._bytes.O, len ), val ) :
				src.length == len ?
						src
						:
						fill( copyOf( src, len ), src.length, len, val );
	}
	
	/**
	 * Fills the entire byte array with the specified value.
	 *
	 * @param dst The byte array to fill.
	 * @param val The byte value to fill the array with.
	 * @return The filled byte array ({@code dst}).
	 */
	static byte[] fill( byte[] dst, byte val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Fills a portion of the byte array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
	 * with the specified value.
	 *
	 * @param dst       The byte array to fill.
	 * @param fromIndex The starting index (inclusive).
	 * @param toIndex   The ending index (exclusive).
	 * @param val       The byte value to fill the array portion with.
	 * @return The filled byte array ({@code dst}).
	 */
	static byte[] fill( byte[] dst, int fromIndex, int toIndex, byte val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Creates a copy of the source short array with the specified length.
	 * If the length is less than 1, it returns a cached empty short array.
	 *
	 * @param src The source short array to copy, can be null.
	 * @param len The desired length of the copy.
	 * @return A new short array which is a copy of the source array,
	 * or a cached empty short array if {@code len} is less than 1.
	 */
	static short[] copyOf( short[] src, int len ) {
		return len < 1 ?
				EqualHashOf._shorts.O :
				Arrays.copyOf( src == null ?
						               EqualHashOf._shorts.O :
						               src, len );
	}
	
	/**
	 * Resizes the given short array to the new length.
	 * If the new length is the same as the original length, the original array is returned.
	 * If the new length is greater than the original, the new elements are filled with the provided short value.
	 * If the new length is less than 1, it returns a cached empty short array.
	 *
	 * @param src The source short array to resize, can be null.
	 * @param len The desired new length.
	 * @param val The short value to fill new elements with if the array is enlarged.
	 * @return A resized short array, or a cached empty short array if {@code len} is less than 1.
	 */
	static short[] resize( short[] src, int len, short val ) {
		return src == null ?
				fill( copyOf( EqualHashOf._shorts.O, len ), val ) :
				src.length == len ?
						src
						:
						fill( copyOf( src, len ), src.length, len, val );
	}
	
	/**
	 * Fills the entire short array with the specified value.
	 *
	 * @param dst The short array to fill.
	 * @param val The short value to fill the array with.
	 * @return The filled short array ({@code dst}).
	 */
	static short[] fill( short[] dst, short val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Fills a portion of the short array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
	 * with the specified value.
	 *
	 * @param dst       The short array to fill.
	 * @param fromIndex The starting index (inclusive).
	 * @param toIndex   The ending index (exclusive).
	 * @param val       The short value to fill the array portion with.
	 * @return The filled short array ({@code dst}).
	 */
	static short[] fill( short[] dst, int fromIndex, int toIndex, short val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Creates a copy of the source char array with the specified length.
	 * If the length is less than 1, it returns a cached empty char array.
	 *
	 * @param src The source char array to copy, can be null.
	 * @param len The desired length of the copy.
	 * @return A new char array which is a copy of the source array,
	 * or a cached empty char array if {@code len} is less than 1.
	 */
	static char[] copyOf( char[] src, int len ) {
		return len < 1 ?
				EqualHashOf._chars.O :
				Arrays.copyOf( src == null ?
						               EqualHashOf._chars.O :
						               src, len );
	}
	
	/**
	 * Resizes the given char array to the new length.
	 * If the new length is the same as the original length, the original array is returned.
	 * If the new length is greater than the original, the new elements are filled with the provided char value.
	 * If the new length is less than 1, it returns a cached empty char array.
	 *
	 * @param src The source char array to resize, can be null.
	 * @param len The desired new length.
	 * @param val The char value to fill new elements with if the array is enlarged.
	 * @return A resized char array, or a cached empty char array if {@code len} is less than 1.
	 */
	static char[] resize( char[] src, int len, char val ) {
		return src == null ?
				fill( copyOf( EqualHashOf._chars.O, len ), val ) :
				src.length == len ?
						src
						:
						fill( copyOf( src, len ), src.length, len, val );
	}
	
	/**
	 * Fills the entire char array with the specified value.
	 *
	 * @param dst The char array to fill.
	 * @param val The char value to fill the array with.
	 * @return The filled char array ({@code dst}).
	 */
	static char[] fill( char[] dst, char val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Fills a portion of the char array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
	 * with the specified value.
	 *
	 * @param dst       The char array to fill.
	 * @param fromIndex The starting index (inclusive).
	 * @param toIndex   The ending index (exclusive).
	 * @param val       The char value to fill the array portion with.
	 * @return The filled char array ({@code dst}).
	 */
	static char[] fill( char[] dst, int fromIndex, int toIndex, char val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Creates a copy of the source int array with the specified length.
	 * If the length is less than 1, it returns a cached empty int array.
	 *
	 * @param src The source int array to copy, can be null.
	 * @param len The desired length of the copy.
	 * @return A new int array which is a copy of the source array,
	 * or a cached empty int array if {@code len} is less than 1.
	 */
	static int[] copyOf( int[] src, int len ) {
		return len < 1 ?
				EqualHashOf._ints.O :
				Arrays.copyOf( src == null ?
						               EqualHashOf._ints.O :
						               src, len );
	}
	
	/**
	 * Resizes the given int array to the new length.
	 * If the new length is the same as the original length, the original array is returned.
	 * If the new length is greater than the original, the new elements are filled with the provided int value.
	 * If the new length is less than 1, it returns a cached empty int array.
	 *
	 * @param src The source int array to resize, can be null.
	 * @param len The desired new length.
	 * @param val The int value to fill new elements with if the array is enlarged.
	 * @return A resized int array, or a cached empty int array if {@code len} is less than 1.
	 */
	static int[] resize( int[] src, int len, int val ) {
		return src == null ?
				fill( copyOf( EqualHashOf._ints.O, len ), val ) :
				src.length == len ?
						src
						:
						fill( copyOf( src, len ), src.length, len, val );
	}
	
	/**
	 * Fills the entire int array with the specified value.
	 *
	 * @param dst The int array to fill.
	 * @param val The int value to fill the array with.
	 * @return The filled int array ({@code dst}).
	 */
	static int[] fill( int[] dst, int val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Fills a portion of the int array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
	 * with the specified value.
	 *
	 * @param dst       The int array to fill.
	 * @param fromIndex The starting index (inclusive).
	 * @param toIndex   The ending index (exclusive).
	 * @param val       The int value to fill the array portion with.
	 * @return The filled int array ({@code dst}).
	 */
	static int[] fill( int[] dst, int fromIndex, int toIndex, int val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Creates a copy of the source long array with the specified length.
	 * If the length is less than 1, it returns a cached empty long array.
	 *
	 * @param src The source long array to copy, can be null.
	 * @param len The desired length of the copy.
	 * @return A new long array which is a copy of the source array,
	 * or a cached empty long array if {@code len} is less than 1.
	 */
	static long[] copyOf( long[] src, int len ) {
		return len < 1 ?
				EqualHashOf._longs.O :
				Arrays.copyOf( src == null ?
						               EqualHashOf._longs.O :
						               src, len );
	}
	
	/**
	 * Resizes the given long array to the new length.
	 * If the new length is the same as the original length, the original array is returned.
	 * If the new length is greater than the original, the new elements are filled with the provided long value.
	 * If the new length is less than 1, it returns a cached empty long array.
	 *
	 * @param src The source long array to resize, can be null.
	 * @param len The desired new length.
	 * @param val The long value to fill new elements with if the array is enlarged.
	 * @return A resized long array, or a cached empty long array if {@code len} is less than 1.
	 */
	static long[] resize( long[] src, int len, long val ) {
		return src == null ?
				fill( copyOf( EqualHashOf._longs.O, len ), val ) :
				src.length == len ?
						src
						:
						fill( copyOf( src, len ), src.length, len, val );
	}
	
	/**
	 * Fills the entire long array with the specified value.
	 *
	 * @param dst The long array to fill.
	 * @param val The long value to fill the array with.
	 * @return The filled long array ({@code dst}).
	 */
	static long[] fill( long[] dst, long val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Fills a portion of the long array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
	 * with the specified value.
	 *
	 * @param dst       The long array to fill.
	 * @param fromIndex The starting index (inclusive).
	 * @param toIndex   The ending index (exclusive).
	 * @param val       The long value to fill the array portion with.
	 * @return The filled long array ({@code dst}).
	 */
	static long[] fill( long[] dst, int fromIndex, int toIndex, long val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Creates a copy of the source float array with the specified length.
	 * If the length is less than 1, it returns a cached empty float array.
	 *
	 * @param src The source float array to copy, can be null.
	 * @param len The desired length of the copy.
	 * @return A new float array which is a copy of the source array,
	 * or a cached empty float array if {@code len} is less than 1.
	 */
	static float[] copyOf( float[] src, int len ) {
		return len < 1 ?
				EqualHashOf._floats.O :
				Arrays.copyOf( src == null ?
						               EqualHashOf._floats.O :
						               src, len );
	}
	
	/**
	 * Resizes the given float array to the new length.
	 * If the new length is the same as the original length, the original array is returned.
	 * If the new length is greater than the original, the new elements are filled with the provided float value.
	 * If the new length is less than 1, it returns a cached empty float array.
	 *
	 * @param src The source float array to resize, can be null.
	 * @param len The desired new length.
	 * @param val The float value to fill new elements with if the array is enlarged.
	 * @return A resized float array, or a cached empty float array if {@code len} is less than 1.
	 */
	static float[] resize( float[] src, int len, float val ) {
		return src == null ?
				fill( copyOf( EqualHashOf._floats.O, len ), val ) :
				src.length == len ?
						src
						:
						fill( copyOf( src, len ), src.length, len, val );
	}
	
	/**
	 * Fills the entire float array with the specified value.
	 *
	 * @param dst The float array to fill.
	 * @param val The float value to fill the array with.
	 * @return The filled float array ({@code dst}).
	 */
	static float[] fill( float[] dst, float val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Fills a portion of the float array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
	 * with the specified value.
	 *
	 * @param dst       The float array to fill.
	 * @param fromIndex The starting index (inclusive).
	 * @param toIndex   The ending index (exclusive).
	 * @param val       The float value to fill the array portion with.
	 * @return The filled float array ({@code dst}).
	 */
	static float[] fill( float[] dst, int fromIndex, int toIndex, float val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Creates a copy of the source double array with the specified length.
	 * If the length is less than 1, it returns a cached empty double array.
	 *
	 * @param src The source double array to copy, can be null.
	 * @param len The desired length of the copy.
	 * @return A new double array which is a copy of the source array,
	 * or a cached empty double array if {@code len} is less than 1.
	 */
	static double[] copyOf( double[] src, int len ) {
		return len < 1 ?
				EqualHashOf._doubles.O :
				Arrays.copyOf( src == null ?
						               EqualHashOf._doubles.O :
						               src, len );
	}
	
	/**
	 * Resizes the given double array to the new length.
	 * If the new length is the same as the original length, the original array is returned.
	 * If the new length is greater than the original, the new elements are filled with the provided double value.
	 * If the new length is less than 1, it returns a cached empty double array.
	 *
	 * @param src The source double array to resize, can be null.
	 * @param len The desired new length.
	 * @param val The double value to fill new elements with if the array is enlarged.
	 * @return A resized double array, or a cached empty double array if {@code len} is less than 1.
	 */
	static double[] resize( double[] src, int len, double val ) {
		return src == null ?
				fill( copyOf( EqualHashOf._doubles.O, len ), val ) :
				src.length == len ?
						src
						:
						fill( copyOf( src, len ), src.length, len, val );
	}
	
	/**
	 * Fills the entire double array with the specified value.
	 *
	 * @param dst The double array to fill.
	 * @param val The double value to fill the array with.
	 * @return The filled double array ({@code dst}).
	 */
	static double[] fill( double[] dst, double val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Fills a portion of the double array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
	 * with the specified value.
	 *
	 * @param dst       The double array to fill.
	 * @param fromIndex The starting index (inclusive).
	 * @param toIndex   The ending index (exclusive).
	 * @param val       The double value to fill the array portion with.
	 * @return The filled double array ({@code dst}).
	 */
	static double[] fill( double[] dst, int fromIndex, int toIndex, double val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[ i ] = val;
		return dst;
	}
	
	/**
	 * Pool to store instances of {@code EqualHashOf} for different classes.
	 * This is used for caching and reusing {@code EqualHashOf} instances to improve performance.
	 */
	HashMap< Class< ? >, Object > pool = new HashMap<>( 8 );
	
	/**
	 * Retrieves an {@code EqualHashOf} instance for the specified class.
	 * It uses a cache to reuse instances if available; otherwise, it creates a new instance
	 * and adds it to the cache. This method is thread-safe.
	 *
	 * @param <T>   The type of the array elements.
	 * @param clazz The class for which to get the {@code EqualHashOf} instance.
	 * @return An {@code EqualHashOf} instance for the specified class.
	 */
	@SuppressWarnings( "unchecked" )
	static < T > EqualHashOf< T > get( Class< T > clazz ) {
		final Object c = clazz;
		if( c == boolean[].class )
			return ( EqualHashOf< T > ) EqualHashOf.booleans;
		if( c == byte[].class )
			return ( EqualHashOf< T > ) EqualHashOf.bytes;
		if( c == short[].class )
			return ( EqualHashOf< T > ) EqualHashOf.shorts;
		if( c == int[].class )
			return ( EqualHashOf< T > ) EqualHashOf.ints;
		if( c == long[].class )
			return ( EqualHashOf< T > ) EqualHashOf.longs;
		if( c == char[].class )
			return ( EqualHashOf< T > ) EqualHashOf.chars;
		if( c == float[].class )
			return ( EqualHashOf< T > ) EqualHashOf.floats;
		if( c == double[].class )
			return ( EqualHashOf< T > ) EqualHashOf.doubles;
		
		EqualHashOf< T > ret = ( EqualHashOf< T > ) pool.get( clazz );
		if( ret != null )
			return ret;
		synchronized( pool ) {
			ret = ( EqualHashOf< T > ) pool.get( clazz );
			if( ret != null )
				return ret;
			
			ret = new EqualHashOf<>( clazz );
			pool.put( clazz, ret );
			return ret;
		}
	}
	
	EqualHashOf< String > string = get( String.class );
	
	/**
	 * Resizes an array by moving elements within or between arrays, optimized for dynamic array resizing.
	 * This method is a low-level utility for array resizing, allowing elements to be shifted
	 * to accommodate insertion or removal of space at a specified index.  **Unlike a simple expansion
	 * using `Arrays.copyOf` followed by manual shifting, this method directly positions elements
	 * in the destination array (`dst`) with the required shift in a single, coordinated operation.**
	 * This approach can be more efficient, especially in scenarios like dynamic array implementations
	 * (e.g., internal resizing of ArrayList or Vector) where you need to insert or remove elements
	 * at a specific index and want to avoid unnecessary intermediate steps.
	 * <p>
	 * **Example illustrating the efficiency advantage for enlargement:**
	 * <p>
	 * **Typical approach (less efficient for this specific case):**
	 * ```java
	 * newArray = Arrays.copyOf(oldArray, newLength); // (Expands, but data is at the beginning)
	 * System.arraycopy(newArray, index, newArray, index + resize, oldArray.length - index); // (Manual shifting to make space)
	 * ```
	 * <p>
	 * **Using `resize` function (more direct and potentially efficient):**
	 * ```java
	 * newArray = new Object[newLength]; // (Create new array)
	 * resize(oldArray, newArray, index, oldArray.length, resize); // (Directly places data in `newArray` with the necessary shift in one call)
	 * ```
	 * <p>
	 * For enlargement (positive `resize`), it creates space at the specified `index` by shifting
	 * elements from `src` to `dst` in a way that elements after `index` are placed at `index + resize` in `dst`,
	 * effectively inserting space at `index`.  For shrinking (negative `resize`), it removes elements
	 * starting from `index` by shifting elements from `src` after the removed section to `dst` starting at `index`,
	 * effectively overwriting the elements to be removed.
	 *
	 * @param src    The source array containing the original elements.
	 * @param dst    The destination array where elements are moved. Can be the same as src for in-place resizing.
	 *               When enlarging, `dst` is typically a newly allocated array with sufficient capacity.
	 * @param index  The starting index where resizing occurs (elements are inserted or removed).
	 *               Must be non-negative and typically less than or equal to size, though larger values
	 *               are handled by extending the size in the enlarging case.
	 * @param size   The current number of valid elements in the source array (non-negative).
	 * @param resize The resize amount. Positive values enlarge the array by adding space at index,
	 *               shifting elements right; negative values shrink it by removing elements from index,
	 *               shifting elements left. Zero leaves the size unchanged but is not optimized separately.
	 * @return The new size of the array after resizing, representing the updated number of valid elements
	 * in the destination array up to which elements are properly copied.
	 */
	static int resize( Object src, Object dst, int index, int size, final int resize ) {
		if( resize < 0 ) {
			
			if( src != dst && 0 < index )
				System.arraycopy( src, 0, dst, 0, index );
			
			if( index + ( -resize ) < size ) {
				System.arraycopy( src, index + ( -resize ), dst, index, size - ( index + ( -resize ) ) );
				return size + resize;
			}
			
			return index;
		}
		
		if( 0 < size )
			if( index < size )
				if( index == 0 )
					System.arraycopy( src, 0, dst, resize, size );
				else {
					if( src != dst && 0 < index ) System.arraycopy( src, 0, dst, 0, index );
					System.arraycopy( src, index, dst, index + resize, size - index );
				}
			else if( src != dst )
				System.arraycopy( src, 0, dst, 0, size );
		
		return Math.max( index, size ) + resize;
	}
	
	/**
	 * Copies elements from a source array to a destination array, skipping a section in the source.
	 * This method is a low-level utility for array manipulation.
	 *
	 * @param src   The source array to copy from.
	 * @param index The index up to which elements are copied from the beginning of the source array.
	 * @param skip  The number of elements to skip in the source array after the initial copy.
	 * @param size  The total number of elements in the source array.
	 * @param dst   The destination array to copy to.
	 */
	static void copy( Object src, int index, final int skip, int size, Object dst ) {
		if( 0 < index )
			System.arraycopy( src, 0, dst, 0, index );
		if( ( index += skip ) < size )
			System.arraycopy( src, index, dst, index, size - index );
	}
	
	/**
	 * Computes the next power of 2 greater than or equal to the given value.
	 * This is commonly used for sizing data structures like hash tables or arrays for optimal performance.
	 *
	 * @param v The value for which to find the next power of 2.
	 * @return The next power of 2 greater than or equal to {@code v}.
	 */
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
	
	/**
	 * Checks if two 2D object arrays are deeply equal.
	 * It iterates through each sub-array and uses {@link Arrays#equals(Object[], Object[])} for comparison.
	 *
	 * @param O The first 2D array.
	 * @param X The second 2D array.
	 * @return {@code true} if the arrays are deeply equal, {@code false} otherwise.
	 */
	static boolean equals( Object[][] O, Object[][] X ) {
		//Check if the two arrays are the same reference
		if( O != X ) {
			//If they are not the same reference, check for null values and equality of lengths
			if( O == null || X == null || O.length != X.length )
				return false;
			//Iterate over each sub-array and check for equality using Arrays.equals()
			for( int i = O.length; -1 < --i; )
				if( !Arrays.equals( O[ i ], X[ i ] ) )
					return false;
		}
		return true;
	}
	
	/**
	 * Mixes an integer data into a hash value using a MurmurHash3-like mixing strategy.
	 * This is a component of the hash function used for array hash code generation.
	 *
	 * @param hash The current hash value.
	 * @param data The integer data to mix in.
	 * @return The updated hash value after mixing.
	 */
	static int mix( int hash, int data ) {
		//Mix the data into the hash using bit operations
		return Integer.rotateLeft( mixLast( hash, data ), 13 ) * 5 + 0xe6546b64;
	}
	
	/**
	 * Mixes the last integer data into a hash value, part of the finalization process in hashing.
	 * This step is crucial for ensuring good distribution of hash values.
	 *
	 * @param hash The current hash value.
	 * @param data The last integer data to mix in.
	 * @return The updated hash value after mixing the last data.
	 */
	static int mixLast( int hash, int data ) {
		//Mix the data into the hash using bit operations
		return hash ^ Integer.rotateLeft( data * 0xcc9e2d51, 15 ) * 0x1b873593;
	}
	
	/**
	 * Finalizes the hash value by incorporating the length and applying an avalanche effect.
	 * This ensures that the length of the data also influences the final hash code and improves distribution.
	 *
	 * @param hash   The current hash value.
	 * @param length The length of the data being hashed.
	 * @return The finalized hash value.
	 */
	static int finalizeHash( int hash, int length ) {
		//Finalize the hash by applying the avalanche function and XORing with the length
		return avalanche( hash ^ length );
	}
	
	/**
	 * Applies an avalanche effect to a hash value to further randomize and distribute it.
	 * This is a standard technique in hash function design to improve the quality of hash codes.
	 *
	 * @param size The hash value to apply the avalanche effect to.
	 * @return The hash value after applying the avalanche effect.
	 */
	static int avalanche( int size ) {
		//Apply the avalanche function to the size
		size = ( size ^ size >>> 16 ) * 0x85ebca6b;
		size = ( size ^ size >>> 13 ) * 0xc2b2ae35;
		return size ^ size >>> 16;
	}
	
	/**
	 * Computes the hash code for a 2D object array.
	 * It iterates through each sub-array and XORs their hash codes.
	 * Returns 0 for null input array.
	 *
	 * @param hash The initial hash value to start with.
	 * @param src  The source 2D array.
	 * @return The computed hash code.
	 */
	static int hash( int hash, Object[][] src ) {
		if( src == null )
			return 0;
		for( Object[] s : src )
			hash ^= Arrays.hashCode( s );
		
		return hash;
	}
	
	/**
	 * Computes the combined hash of an existing hash and an object's hash code.
	 *
	 * @param hash The current hash value.
	 * @param src  The object to compute the hash code for and combine.
	 * @return The combined hash value.
	 */
	static int hash( int hash, Object src ) { return hash ^ hash( src ); }
	
	/**
	 * Computes the combined hash of an existing hash and a double value's hash code.
	 *
	 * @param hash The current hash value.
	 * @param src  The double value to compute the hash code for and combine.
	 * @return The combined hash value.
	 */
	static int hash( int hash, double src ) { return hash ^ hash( src ); }
	
	/**
	 * Computes the combined hash of an existing hash and a float value's hash code.
	 *
	 * @param hash The current hash value.
	 * @param src  The float value to compute the hash code for and combine.
	 * @return The combined hash value.
	 */
	static int hash( int hash, float src ) { return hash ^ hash( src ); }
	
	/**
	 * Computes the combined hash of an existing hash and an integer value.
	 *
	 * @param hash The current hash value.
	 * @param src  The integer value to combine with the hash.
	 * @return The combined hash value.
	 */
	static int hash( int hash, int src ) { return hash ^ hash( src ); }
	
	/**
	 * Computes the combined hash of an existing hash and a long value's hash code.
	 *
	 * @param hash The current hash value.
	 * @param src  The long value to compute the hash code for and combine.
	 * @return The combined hash value.
	 */
	static int hash( int hash, long src ) { return hash ^ hash( src ); }
	
	
	/**
	 * Computes the hash code of a double value using {@link Double#hashCode(double)}.
	 *
	 * @param src The double value to compute the hash code for.
	 * @return The hash code of the double value.
	 */
	static int hash( double src ) { return Double.hashCode( src ); }
	
	/**
	 * Computes the hash code of a float value using {@link Float#hashCode(float)}.
	 *
	 * @param src The float value to compute the hash code for.
	 * @return The hash code of the float value.
	 */
	static int hash( float src ) { return Float.hashCode( src ); }
	
	/**
	 * Returns the integer value itself as its hash code.
	 *
	 * @param src The integer value.
	 * @return The integer value itself.
	 */
	static int hash( int src ) { return src; }
	
	/**
	 * Computes the hash code of a long value using {@link Long#hashCode(long)}.
	 *
	 * @param src The long value to compute the hash code for.
	 * @return The hash code of the long value.
	 */
	static int hash( long src ) { return Long.hashCode( src ); }
	
	/**
	 * Computes the combined hash code of elements in an Object array from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static int hash( int hash, Object[] src, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) hash = hash( hash, src[ i ] );
		return hash;
	}
	
	/**
	 * Computes the combined hash code of elements in a boolean array from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static int hash( int hash, boolean[] src, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) hash = hash( hash, src[ i ] );
		return hash;
	}
	
	/**
	 * Computes the combined hash code of elements in a byte array from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static int hash( int hash, byte[] src, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) hash = hash( hash, src[ i ] );
		return hash;
	}
	
	/**
	 * Computes the combined hash code of elements in a char array from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static int hash( int hash, char[] src, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) hash = hash( hash, src[ i ] );
		return hash;
	}
	
	/**
	 * Computes the combined hash code of elements in a short array from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static int hash( int hash, short[] src, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) hash = hash( hash, src[ i ] );
		return hash;
	}
	
	/**
	 * Computes the combined hash code of elements in an int array from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static int hash( int hash, int[] src, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) hash = hash( hash, src[ i ] );
		return hash;
	}
	
	/**
	 * Computes the combined hash code of elements in a long array from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static int hash( int hash, long[] src, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) hash = hash( hash, src[ i ] );
		return hash;
	}
	
	/**
	 * Computes the combined hash code of elements in a float array from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static int hash( int hash, float[] src, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) hash = hash( hash, src[ i ] );
		return hash;
	}
	
	/**
	 * Computes the combined hash code of elements in a double array from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static int hash( int hash, double[] src, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) hash = hash( hash, src[ i ] );
		return hash;
	}
	
	/**
	 * Compares two Object arrays for equality from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static boolean equals( Object[] a, Object[] b, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( !a[ i ].equals( b[ i ] ) ) return false;
		return true;
	}
	
	/**
	 * Compares two boolean arrays for equality from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static boolean equals( boolean[] a, boolean[] b, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( a[ i ] != b[ i ] ) return false;
		return true;
	}
	
	/**
	 * Compares two byte arrays for equality from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static boolean equals( byte[] a, byte[] b, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( a[ i ] != b[ i ] ) return false;
		return true;
	}
	
	/**
	 * Compares two char arrays for equality from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static boolean equals( char[] a, char[] b, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( a[ i ] != b[ i ] ) return false;
		return true;
	}
	
	/**
	 * Compares two short arrays for equality from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static boolean equals( short[] a, short[] b, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( a[ i ] != b[ i ] ) return false;
		return true;
	}
	
	/**
	 * Compares two int arrays for equality from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static boolean equals( int[] a, int[] b, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( a[ i ] != b[ i ] ) return false;
		return true;
	}
	
	/**
	 * Compares two long arrays for equality from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static boolean equals( long[] a, long[] b, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( a[ i ] != b[ i ] ) return false;
		return true;
	}
	
	/**
	 * Compares two float arrays for equality from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static boolean equals( float[] a, float[] b, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( a[ i ] != b[ i ] ) return false;
		return true;
	}
	
	/**
	 * Compares two double arrays for equality from fromIndex (inclusive) to toIndex (exclusive).
	 */
	static boolean equals( double[] a, double[] b, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( a[ i ] != b[ i ] ) return false;
		return true;
	}
	
	/**
	 * Returns the index of the first occurrence of the specified value in the Object array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int indexOf( Object[] src, Object value, int fromIndex, int toIndex ) {
		if( value == null ) {
			for( int i = fromIndex; i < toIndex; i++ )
				if( src[ i ] == null ) return i;
		}
		else for( int i = fromIndex; i < toIndex; i++ )
			if( value.equals( src[ i ] ) ) return i;
		
		return -1;
	}
	
	/**
	 * Returns the index of the first occurrence of the specified value in the boolean array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int indexOf( boolean[] src, boolean value, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the first occurrence of the specified value in the byte array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int indexOf( byte[] src, byte value, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the first occurrence of the specified value in the char array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int indexOf( char[] src, char value, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the first occurrence of the specified value in the short array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int indexOf( short[] src, short value, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the first occurrence of the specified value in the int array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int indexOf( int[] src, int value, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the first occurrence of the specified value in the long array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int indexOf( long[] src, long value, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the first occurrence of the specified value in the float array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int indexOf( float[] src, float value, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the first occurrence of the specified value in the double array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int indexOf( double[] src, double value, int fromIndex, int toIndex ) {
		for( int i = fromIndex; i < toIndex; i++ ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the last occurrence of the specified value in the Object array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int lastIndexOf( Object[] src, Object value, int fromIndex, int toIndex ) {
		if( value == null ) {
			for( int i = toIndex - 1; i >= fromIndex; i-- )
				if( src[ i ] == null ) return i;
		}
		else for( int i = toIndex - 1; fromIndex <= i; i-- )
			if( value.equals( src[ i ] ) ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the last occurrence of the specified value in the boolean array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int lastIndexOf( boolean[] src, boolean value, int fromIndex, int toIndex ) {
		for( int i = toIndex - 1; i >= fromIndex; i-- ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the last occurrence of the specified value in the byte array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int lastIndexOf( byte[] src, byte value, int fromIndex, int toIndex ) {
		for( int i = toIndex - 1; i >= fromIndex; i-- ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the last occurrence of the specified value in the char array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int lastIndexOf( char[] src, char value, int fromIndex, int toIndex ) {
		for( int i = toIndex - 1; i >= fromIndex; i-- ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the last occurrence of the specified value in the short array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int lastIndexOf( short[] src, short value, int fromIndex, int toIndex ) {
		for( int i = toIndex - 1; i >= fromIndex; i-- ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the last occurrence of the specified value in the int array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int lastIndexOf( int[] src, int value, int fromIndex, int toIndex ) {
		for( int i = toIndex - 1; i >= fromIndex; i-- ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the last occurrence of the specified value in the long array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int lastIndexOf( long[] src, long value, int fromIndex, int toIndex ) {
		for( int i = toIndex - 1; i >= fromIndex; i-- ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the last occurrence of the specified value in the float array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int lastIndexOf( float[] src, float value, int fromIndex, int toIndex ) {
		for( int i = toIndex - 1; i >= fromIndex; i-- ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Returns the index of the last occurrence of the specified value in the double array
	 * between fromIndex (inclusive) and toIndex (exclusive), or -1 if not found.
	 */
	static int lastIndexOf( double[] src, double value, int fromIndex, int toIndex ) {
		for( int i = toIndex - 1; i >= fromIndex; i-- ) if( src[ i ] == value ) return i;
		return -1;
	}
	
	/**
	 * Computes the hash code of an object using its {@link Object#hashCode()} method.
	 * Returns 0 if the object is null.
	 *
	 * @param src The object to compute the hash code for.
	 * @return The hash code of the object, or 0 if {@code src} is null.
	 */
	static int hash( Object src ) {
		return src == null ?
				0 :
				src.hashCode();
	}
	
	/**
	 * Computes a custom hash code for a string.
	 * This method processes the string in chunks of characters to generate a hash value.
	 *
	 * @param str The string to compute the hash code for.
	 * @return The computed hash code for the string.
	 */
	static int hash( final String str ) {
		if( str == null )
			return 52861;
		if( str.isEmpty() )
			return 37607;
		
		int h = 61667;
		
		long x = 0;
		for( int i = 0, b = 0; i < str.length(); i++ ) {
			long ch = str.charAt( i );
			x |= ch << b;
			if( ( b += ch < 0x100 ?
					8 :
					16 ) < 32 )
				continue;
			
			h = mix( h, ( int ) x );
			x >>>= 32;
			b -= 32;
		}
		
		h = mixLast( h, ( int ) x );
		
		return finalizeHash( h, str.length() );
	}
	
	/**
	 * Computes the hash code for an array of strings up to a given size.
	 * It iterates through the string array and combines the hash code of each string.
	 *
	 * @param src  The array of strings.
	 * @param size The number of strings to consider for hash code computation.
	 * @return The computed hash code for the string array.
	 */
	static int hash( final String[] src, int size ) {
		int h = String[].class.hashCode();
		
		for( int i = src.length - 1; -1 < i; i-- )
		     h = mix( h, hash( src[ i ] ) );
		return finalizeHash( h, size );
	}
	
	/**
	 * {@code ISort} interface defines contracts for sorting algorithms.
	 * It provides methods for comparing, swapping, copying, and holding elements during sorting.
	 * Implementations are designed to work with different data types and sorting strategies.
	 */
	interface ISort {
		
		/**
		 * {@code Index} abstract class provides a base for sorting algorithms that use an index array
		 * for sorting characters. It implements {@code ISort} and provides basic swap and copy operations.
		 */
		abstract class Index implements ISort {
			/**
			 * Destination character array to be sorted.
			 */
			public char[] dst;
			/**
			 * Size of the array to be sorted.
			 */
			public int    size;
			
			@Override
			public void swap( int ia, int ib ) {
				final char t = dst[ ia ];
				dst[ ia ] = dst[ ib ];
				dst[ ib ] = t;
			}
			
			@Override
			public void copy( int idst, int isrc ) { dst[ idst ] = dst[ isrc ]; }
			
			/**
			 * Fixed character value used for dropping elements during sorting.
			 */
			char fixi = 0;
			
			@Override
			public void drop( int idst ) { dst[ idst ] = fixi; }
		}
		
		/**
		 * {@code Index2} abstract class provides a base for sorting algorithms that use an index array
		 * for sorting integers. It implements {@code ISort} and provides basic swap and copy operations, similar to {@code Index} but for integer arrays.
		 */
		abstract class Index2 implements ISort {
			/**
			 * Destination integer array to be sorted.
			 */
			public int[] dst;
			/**
			 * Size of the array to be sorted.
			 */
			public int   size;
			
			@Override
			public void swap( int ia, int ib ) {
				//Swap two elements in the int array
				final int t = dst[ ia ];
				dst[ ia ] = dst[ ib ];
				dst[ ib ] = t;
			}
			
			@Override
			public void copy( int idst, int isrc ) { dst[ idst ] = dst[ isrc ]; }
			
			/**
			 * Fixed integer value used for dropping elements during sorting.
			 */
			int fixi = 0;
			
			@Override
			public void drop( int idst ) { dst[ idst ] = fixi; }
		}
		
		/**
		 * {@code Primitives} interface defines contracts for sorting algorithms that operate on primitive types.
		 * It extends {@code ISort} and provides methods specific to primitive types like getting and comparing primitive values.
		 */
		interface Primitives {
			/**
			 * Retrieves a primitive value at the specified index.
			 *
			 * @param index The index of the value to retrieve.
			 * @return The primitive value at the specified index as a long.
			 */
			long get( int index );
			
			/**
			 * Compares two primitive values.
			 *
			 * @param x The first primitive value.
			 * @param y The second primitive value.
			 * @return A negative integer, zero, or a positive integer as the first value
			 * is less than, equal to, or greater than the second value.
			 */
			default int compare( long x, long y ) { return Long.compare( x, y ); }
			
			/**
			 * {@code floats} interface specializes {@code Primitives} for float types.
			 * It adds methods to handle float-specific operations such as getting float values and comparing them with special handling for NaN and +/- 0.0.
			 */
			interface floats extends Primitives {
				/**
				 * Retrieves a float value at the specified index.
				 *
				 * @param index The index of the value to retrieve.
				 * @return The float value at the specified index.
				 */
				float get2( int index );
				
				@Override
				default long get( int index ) { return Float.floatToIntBits( get2( index ) ); }
				
				@Override
				default int compare( long x, long y ) {
					final float X = Float.intBitsToFloat( ( int ) x );
					final float Y = Float.intBitsToFloat( ( int ) y );
					return X < Y ?
							-1 :
							Y < X ?
									1
									:
									( Long.compare( x, y ) );
				}
			}
			
			/**
			 * {@code doubles} interface specializes {@code Primitives} for double types.
			 * It adds methods to handle double-specific operations such as getting double values and comparing them with special handling for NaN and +/- 0.0.
			 */
			interface doubles extends Primitives {
				/**
				 * Retrieves a double value at the specified index.
				 *
				 * @param index The index of the value to retrieve.
				 * @return The double value at the specified index.
				 */
				double get2( int index );
				
				@Override
				default long get( int index ) { return Double.doubleToLongBits( get2( index ) ); }
				
				@Override
				default int compare( long x, long y ) {
					final double X = Double.longBitsToDouble( x );
					final double Y = Double.longBitsToDouble( y );
					return X < Y ?
							-1 :
							Y < X ?
									1
									:
									( Long.compare( x, y ) );
				}
			}
			
			/**
			 * {@code Index} class implements {@code ISort.Primitives} for sorting primitive arrays using an index array.
			 * It uses a {@code Primitives} source to access and compare primitive values based on indices in the destination index array.
			 */
			class Index extends ISort.Index {
				/**
				 * Source of primitive values to be sorted.
				 */
				public Primitives src;
				
				@Override
				public int compare( int ia, int ib ) { return src.compare( src.get( dst[ ia ] ), src.get( dst[ ib ] ) ); }
				
				@Override
				public int compare( int isrc ) { return src.compare( fix, src.get( dst[ isrc ] ) ); }
				
				/**
				 * Fixed primitive value for comparison during sorting.
				 */
				long fix = 0;
				
				@Override
				public void hold( int isrc ) { fix = src.get( fixi = dst[ isrc ] ); }
			}
			
			/**
			 * {@code Index2} class is similar to {@code Index} but operates on integer index arrays.
			 * It is designed for sorting primitive arrays indirectly through integer indices.
			 */
			class Index2 extends ISort.Index2 {
				/**
				 * Source of primitive values to be sorted.
				 */
				public Primitives src;
				
				@Override
				public int compare( int ia, int ib ) { return src.compare( src.get( dst[ ia ] ), src.get( dst[ ib ] ) ); }
				
				@Override
				public int compare( int isrc ) { return src.compare( fix, src.get( dst[ isrc ] ) ); }
				
				/**
				 * Fixed primitive value for comparison during sorting.
				 */
				long fix = 0;
				
				@Override
				public void hold( int isrc ) { fix = src.get( fixi = dst[ isrc ] ); }
			}
			
			/**
			 * {@code Direct} class implements {@code ISort.Primitives} for direct sorting of primitive arrays.
			 * It operates directly on the primitive array, swapping and copying elements in place.
			 */
			class Direct implements ISort {
				/**
				 * {@code PrimitivesSet} interface extends {@code ISort.Primitives} to include a method for setting primitive values at a given index,
				 * enabling in-place modification of primitive arrays during sorting.
				 */
				interface PrimitivesSet extends ISort.Primitives {
					/**
					 * Sets a primitive value at the specified index.
					 *
					 * @param index The index where to set the value.
					 * @param value The primitive value to set.
					 */
					void set( int index, long value );
				}
				
				/**
				 * Primitive array to be sorted directly.
				 */
				public PrimitivesSet array;
				
				@Override
				public int compare( int ia, int ib ) { return array.compare( array.get( ia ), array.get( ib ) ); }
				
				@Override
				public int compare( int isrc ) { return array.compare( fix, array.get( isrc ) ); }
				
				/**
				 * Fixed primitive value for comparison during sorting.
				 */
				long fix = 0;
				
				@Override
				public void hold( int isrc ) { fix = array.get( fixi = isrc ); }
				
				@Override
				public void swap( int ia, int ib ) {
					final long t = array.get( ia );
					array.set( ia, array.get( ib ) );
					array.set( ib, t );
				}
				
				@Override
				public void copy( int idst, int isrc ) { array.set( idst, array.get( isrc ) ); }
				
				/**
				 * Fixed index value used for dropping elements during sorting.
				 */
				int fixi = 0;
				
				@Override
				public void drop( int idst ) { array.set( idst, fixi ); }
			}
		}
		
		/**
		 * {@code Anything} interface defines contracts for sorting algorithms that handle generic object types.
		 * It extends {@code ISort} and provides methods for comparing, holding, and manipulating generic array elements.
		 */
		interface Anything {
			
			/**
			 * Holds a generic object at the specified source index for comparison during sorting.
			 *
			 * @param isrc The source index of the object to hold.
			 */
			void hold( int isrc );
			
			/**
			 * Compares two generic objects at the specified indices in the array.
			 *
			 * @param ia The index of the first object.
			 * @param ib The index of the second object.
			 * @return A negative integer, zero, or a positive integer as the first object
			 * is less than, equal to, or greater than the second object.
			 */
			int compare( int ia, int ib );
			
			/**
			 * Compares a held (fixed) generic object with another object at the specified index.
			 *
			 * @param ib The index of the object to compare with the held object.
			 * @return A negative integer, zero, or a positive integer as the held object
			 * is less than, equal to, or greater than the object at the index.
			 */
			int compare( int ib );
			
			/**
			 * {@code Index} class implements {@code ISort.Anything} for sorting generic object arrays using an index array.
			 * It uses an {@code Anything} source to access and compare generic objects based on indices in the destination index array.
			 */
			class Index extends ISort.Index {
				/**
				 * Source of generic objects to be sorted.
				 */
				public Anything src;
				
				@Override
				public int compare( int ia, int ib ) { return src.compare( dst[ ia ], dst[ ib ] ); }
				
				@Override
				public int compare( int isrc ) { return src.compare( dst[ isrc ] ); }
				
				@Override
				public void hold( int isrc ) { src.hold( fixi = dst[ isrc ] ); }
			}
			
			/**
			 * {@code Index2} class is similar to {@code Index} but operates on integer index arrays for sorting generic objects.
			 * It is designed for indirect sorting of generic object arrays through integer indices.
			 */
			class Index2 extends ISort.Index2 {
				/**
				 * Source of generic objects to be sorted.
				 */
				public Anything src;
				
				@Override
				public int compare( int ia, int ib ) { return src.compare( dst[ ia ], dst[ ib ] ); }
				
				@Override
				public int compare( int isrc ) { return src.compare( dst[ isrc ] ); }
				
				@Override
				public void hold( int isrc ) { src.hold( fixi = dst[ isrc ] ); }
			}
		}
		
		/**
		 * {@code Objects<T>} abstract class implements {@code ISort.Anything} and {@code Comparator<T>}
		 * for sorting object arrays of type {@code T}. It provides a base for sorting algorithms that operate on object arrays, using hash codes for comparison.
		 *
		 * @param <T> The type of objects to be sorted.
		 */
		abstract class Objects< T > implements Anything, Comparator< T > {
			/**
			 * Retrieves an object at the specified index from the array to be sorted.
			 *
			 * @param index The index of the object to retrieve.
			 * @return The object at the specified index.
			 */
			abstract T get( int index );
			
			@Override
			public int compare( T o1, T o2 ) { return Integer.compare( hash( o1 ), hash( o2 ) ); }
			
			@Override
			public int compare( int ia, int ib ) { return compare( get( ia ), get( ib ) ); }
			
			@Override
			public int compare( int ib ) { return compare( fix, get( ib ) ); }
			
			/**
			 * Fixed object value for comparison during sorting.
			 */
			T fix;
			
			@Override
			public void hold( int isrc ) { fix = get( isrc ); }
		}
		
		/**
		 * Compares elements at two indices in the array being sorted.
		 *
		 * @param ia The index of the first element.
		 * @param ib The index of the second element.
		 * @return A negative integer, zero, or a positive integer as the element at the first index
		 * is less than, equal to, or greater than the element at the second index.
		 */
		int compare( int ia, int ib ); //array[ia] < array[ib] ? -1 : array[ia] == array[ib] ? 0 : 1;
		
		/**
		 * Swaps elements at two indices in the array being sorted.
		 *
		 * @param ia The index of the first element to swap.
		 * @param ib The index of the second element to swap.
		 */
		void swap( int ia, int ib );
		
		/**
		 * Copies an element from a source index to a destination index in the array being sorted.
		 *
		 * @param dst_index The destination index where the element will be copied.
		 * @param src_index The source index from where the element will be copied.
		 */
		void copy( int dst_index, int src_index ); //replace element at dst_index with element at src_index
		
		/**
		 * Compares a fixed (held) element with an element at a specified index in the array being sorted.
		 *
		 * @param src_index The index of the element to compare with the fixed element.
		 * @return A negative integer, zero, or a positive integer as the fixed element
		 * is less than, equal to, or greater than the element at the specified index.
		 */
		int compare( int src_index ); //compare fixed element with element at index;
		
		/**
		 * Holds (fixes) an element at a specified index for subsequent comparisons in sorting.
		 * This is used to select a pivot element or to temporarily store an element during the sorting process.
		 *
		 * @param src_index The index of the element to hold.
		 */
		void hold( int src_index ); //fix a value at index
		
		/**
		 * Drops (places) a held (fixed) element at a specified index in the array being sorted.
		 * This is used to place a pivot element in its correct sorted position or to restore a temporarily stored element.
		 *
		 * @param dst_index The index where to drop the held element.
		 */
		void drop( int dst_index ); //put fixed value at index
		
		/**
		 * Performs a binary search in a sorted array to find the index of an element whose value is equal to a fixed (held) element.
		 *
		 * @param dst The {@code ISort} instance representing the array to search in.
		 * @param lo  The low index of the search range.
		 * @param hi  The high index of the search range.
		 * @return The index of the element if found, otherwise returns {@code ~insertion point}
		 * where insertion point is the index of the first element greater than the key or {@code hi + 1} if all elements in the range are less than the key.
		 */
		static int search( ISort dst, int lo, int hi ) {
			while( lo <= hi ) {
				final int o = lo + ( hi - lo >> 1 ), dir = dst.compare( o );
				
				if( dir == 0 )
					return o;
				if( 0 < dir )
					lo = o + 1;
				else
					hi = o - 1;
			}
			return ~lo;
		}
		
		/**
		 * Sorts a portion of an array using an introspective sorting algorithm which combines quicksort, heapsort, and insertion sort
		 * to provide good performance in most scenarios while avoiding quicksort's worst-case performance.
		 *
		 * @param dst The {@code ISort} instance representing the array to be sorted.
		 * @param lo  The low index of the range to be sorted (inclusive).
		 * @param hi  The high index of the range to be sorted (inclusive).
		 */
		static void sort( ISort dst, int lo, int hi ) {
			
			int len = hi - lo + 1;
			if( len < 2 ) return;
			
			int pow2 = 0;
			while( 0 < len ) {
				pow2++;
				len >>>= 1;
			}
			
			sort( dst, lo, hi, 2 * pow2 );
		}
		
		/**
		 * Recursive helper method for sorting a portion of an array using an introspective sorting algorithm with a recursion limit.
		 * This method selects the best sorting algorithm based on the size of the array portion and the recursion depth to optimize performance and prevent stack overflow.
		 *
		 * @param dst   The {@code ISort} instance representing the array to be sorted.
		 * @param lo    The low index of the range to be sorted (inclusive).
		 * @param hi    The high index of the range to be sorted (inclusive).
		 * @param limit The recursion limit to prevent stack overflow in quicksort.
		 */
		static void sort( ISort dst, final int lo, int hi, int limit ) {
			
			while( hi > lo ) {
				int size = hi - lo + 1;
				
				if( size < 17 )
					switch( size ) {
						case 1:
							return;
						case 2:
							if( dst.compare( lo, hi ) > 0 ) dst.swap( lo, hi );
							return;
						case 3:
							if( dst.compare( lo, hi - 1 ) > 0 ) dst.swap( lo, hi - 1 );
							if( dst.compare( lo, hi ) > 0 ) dst.swap( lo, hi );
							if( dst.compare( hi - 1, hi ) > 0 ) dst.swap( hi - 1, hi );
							return;
						default:
							// Insertion sort for small arrays
							for( int i = lo; i < hi; i++ ) {
								dst.hold( i + 1 );
								
								int j = i;
								while( lo <= j && dst.compare( j ) < 0 )
									dst.copy( j + 1, j-- );
								
								dst.drop( j + 1 );
							}
							return;
					}
				
				if( limit == 0 ) {
					// Heapsort to avoid quicksort's worst case
					final int w = hi - lo + 1;
					for( int i = w >>> 1; 0 < i; i-- )
					     heapify( dst, i, w, lo );
					
					for( int i = w; 1 < i; i-- ) {
						dst.swap( lo, lo + i - 1 );
						heapify( dst, 1, i - 1, lo );
					}
					return;
				}
				
				limit--;
				
				// Median-of-three partitioning for quicksort
				final int o = lo + ( hi - lo >> 1 );
				
				if( dst.compare( lo, o ) > 0 ) dst.swap( lo, o );
				if( dst.compare( lo, hi ) > 0 ) dst.swap( lo, hi );
				if( dst.compare( o, hi ) > 0 ) dst.swap( o, hi );
				
				dst.hold( o );
				dst.swap( o, hi - 1 );
				int l = lo, h = hi - 1;
				
				// Partitioning phase of quicksort
				while( l < h ) {
					while( -1 < dst.compare( ++l ) )
						;
					while( dst.compare( --h ) < 0 )
						;
					
					if( h <= l ) break;
					
					dst.swap( l, h );
				}
				
				if( l != hi - 1 ) dst.swap( l, hi - 1 );
				sort( dst, l + 1, hi, limit ); // Recursively sort right partition
				hi = l - 1; // Iteratively sort left partition (tail recursion optimization)
			}
		}
		
		/**
		 * Transforms a portion of an array into a max-heap.
		 * Used in heapsort algorithm to build and maintain the heap property during sorting.
		 *
		 * @param dst The {@code ISort} instance representing the array to be heapified.
		 * @param i   The index of the root of the subtree to be heapified (1-based index in heap context).
		 * @param w   The size of the heap (number of elements in the heap).
		 * @param lo  The low index of the range of the array that represents the heap.
		 */
		static void heapify( ISort dst, int i, final int w, int lo ) {
			dst.hold( --lo + i ); // Hold the root element of the heap
			
			while( i <= w >>> 1 ) {
				int child = i << 1; // Left child index
				if( child < w && dst.compare( lo + child, lo + child + 1 ) < 0 )
					child++; // Choose the larger child if right child exists and is larger
				
				if( -1 < dst.compare( lo + child ) )
					break; // If heap property is maintained, break
				
				dst.copy( lo + i, lo + child ); // Move down the larger child
				i = child; // Move down to continue heapifying
			}
			
			dst.drop( lo + i ); // Place the held element in its correct position in the heap
		}
	}
	
	static int prime( int capacity ) {
		if( capacity < 0 ) return capacity;
		
		for( int prime : Array.primes )
			if( capacity < prime ) return prime;
		
		return -1;
	}
	
	// Table of prime numbers to use as hash table sizes.
	// A typical resize operation would pick the next larger prime number from this table.
	int[] primes = {
			3, 7, 11, 17, 23, 29, 37, 47, 59, 71, 89, 107, 131, 163, 197, 239, 293, 353, 431, 521, 631, 761, 919,
			1103, 1327, 1597, 1931, 2333, 2801, 3371, 4049, 4861, 5839, 7013, 8419, 10103, 12143, 14591,
			17519, 21023, 25229, 30293, 36353, 43627, 52361, 62851, 75431, 90523, 108631, 130363, 156437,
			187751, 225307, 270371, 324449, 389357, 467237, 560689, 672827, 807403, 968887, 1162667, 1395209,
			1674259, 2009111, 2410933, 2893121, 3471749, 4166099, 4999321, 5999203, 7199047, 8638861,
			10366643, 12439979, 14927993, 17913599, 21496319, 25795597, 30954743, 37145697, 44574859,
			53489839, 64187817, 77025389, 92430497, 110916599, 133099919, 159719903, 191663887, 229996669,
			275995999, 331195199, 397434263, 476921141, 572305373, 686766469, 824119789, 988943741,
			1186732499, 1424078987, 1708894781, 2050673741
	};
	
	// Fixed-size 256-bit Flag Set class.
	// Represents a set of byte values (0-255) using a bitset implemented with four long integers.
	abstract class FF implements Cloneable {
		protected long _1, _2, _3, _4; // Segments representing bits for byte values 0-63, 64-127, 128-191, and 192-255 respectively.
		protected int size = 0;        // The number of elements currently in the set.
		protected int _version;  // Version counter for detecting concurrent modifications. Incremented on any structural modification.
		
		@Override public Object clone() throws CloneNotSupportedException {
			// Creates and returns a shallow copy of this `FF` instance.
			return super.clone();
		}
		
		/**
		 * Checks if this Flag Set is equal to another Flag Set.
		 * Two Flag Sets are considered equal if they have the same size and the same bit representation.
		 *
		 * @param other The other Flag Set to compare with.
		 * @return {@code true} if the Flag Sets are equal, {@code false} otherwise.
		 */
		public boolean equals( ByteSet.R other ) {
			return size == other.size && _4 == other._4 && _3 == other._3 && _2 == other._2 && _1 == other._1;
			
		}
		
		/**
		 * Returns the hash code for this Flag Set.
		 * The hash code is calculated based on the bit representation and the size of the set.
		 *
		 * @return The hash code value for this Flag Set.
		 */
		public int hashCode() {
			return Array.finalizeHash( Array.hash( Array.hash( Array.hash( Array.hash( 22633363, _1 ), _2 ), _3 ), _4 ), size ); // Hash segments _1, _2, _3, _4 and size.
		}
		
		/**
		 * Calculates the rank of a given byte value within the set.
		 * The rank is the number of elements in the set that are strictly less than the given byte value.
		 * This method is useful for operations requiring ordered access or positional information within the set.
		 *
		 * @param key The byte value to determine the rank for. Must be in the range 0-255.
		 * @return The rank of the byte value. Returns the count of elements in the set less than {@code key}.
		 */
		protected int rank( byte key ) {
			
			switch( key & 0xC0 ) { // Determine which segments to count based on the higher 2 bits of the key.
				case 0xC0: // 192-255. Count bits in _1, _2, _3, and bits in _4 less than 'key'.
					return Long.bitCount( _1 ) + Long.bitCount( _2 ) + Long.bitCount( _3 ) + Long.bitCount( _4 << 63 - key );
				case 0x80: // 128-191. Count bits in _1, _2, and bits in _3 less than 'key'.
					return Long.bitCount( _1 ) + Long.bitCount( _2 ) + Long.bitCount( _3 << 63 - key );
				case 0x40: // 64-127. Count bits in _1, and bits in _2 less than 'key'.
					return Long.bitCount( _1 ) + Long.bitCount( _2 << 63 - key );
				default:   // 0-63. Count bits in _1 less than 'key'.
					return Long.bitCount( _1 << 63 - key );
			}
		}
		
		/**
		 * Returns the smallest byte value (0-255) currently present in the set.
		 *
		 * @return The smallest byte value in the set, or -1 if the set is empty.
		 */
		protected int first1() {
			if( 0 < size )
				if( _1 != 0 ) return Long.numberOfTrailingZeros( _1 ); // Find the first set bit in _1 (smallest byte value in 0-63 range).
				else if( _2 != 0 ) return 64 + Long.numberOfTrailingZeros( _2 ); // Find the first set bit in _2 (smallest byte value in 64-127 range).
				else if( _3 != 0 ) return 128 + Long.numberOfTrailingZeros( _3 ); // Find the first set bit in _3 (smallest byte value in 128-191 range).
				else if( _4 != 0 ) return 192 + Long.numberOfTrailingZeros( _4 ); // Find the first set bit in _4 (smallest byte value in 192-255 range).
			return -1; // Set is empty.
		}
		
		/**
		 * Returns the smallest byte value (0-255) in the set that is strictly greater than the given {@code key}.
		 *
		 * @param key The reference byte value. The search starts from the next byte value (key + 1).
		 * @return The smallest byte value greater than {@code key}, or -1 if no such value exists in the set.
		 */
		protected int next1( int key ) {
			
			key++; // Start searching for the next set bit from the byte value immediately after the current one.
			if( key < size ) { // If there are still byte values in the set to iterate over.
				long l;
				switch( key & 0b1100_0000 ) { // Determine which 64-bit segment to check next based on the starting key value.
					case 0b1100_0000: // Range [192, 255]
						l = _4 >>> key; // Right-shift _4 to start searching from 'key' onwards.
						if( l != 0 ) return key + Long.numberOfTrailingZeros( l ); // If found a set bit, create a token for it and return.
						break; // If no set bit in the remaining part of _4, proceed to check the next segment.
					
					case 0b1000_0000: // Range [128, 191]
						l = _3 >>> key; // Right-shift _3 to start searching from 'key' onwards.
						if( l != 0 ) return key + Long.numberOfTrailingZeros( l ); // If found a set bit, create a token for it and return.
						if( _4 != 0 ) return 192 + Long.numberOfTrailingZeros( _4 ); // If no set bit in the remaining part of _3, check if there are any set bits in _4 and return the smallest one.
						break; // If no set bits in _3 or _4, proceed to check the next segment.
					
					case 0b0100_0000: // Range [64, 127]
						l = _2 >>> key; // Right-shift _2 to start searching from 'key' onwards.
						if( l != 0 ) return key + Long.numberOfTrailingZeros( l ); // If found a set bit, create a token for it and return.
						if( _3 != 0 ) return 128 + Long.numberOfTrailingZeros( _3 ); // If no set bit in the remaining part of _2, check if there are any set bits in _3 and return the smallest one.
						if( _4 != 0 ) return 192 + Long.numberOfTrailingZeros( _4 ); // If no set bits in _2 or _3, check if there are any set bits in _4 and return the smallest one.
						break; // If no set bits in _2, _3 or _4, proceed to check the next segment.
					
					default: // Range [0, 63]
						l = _1 >>> key; // Right-shift _1 to start searching from 'key' onwards.
						if( l != 0 ) return key + Long.numberOfTrailingZeros( l ); // If found a set bit, create a token for it and return.
						if( _2 != 0 ) return 64 + Long.numberOfTrailingZeros( _2 ); // If no set bit in the remaining part of _1, check if there are any set bits in _2 and return the smallest one.
						if( _3 != 0 ) return 128 + Long.numberOfTrailingZeros( _3 ); // If no set bits in _1 or _2, check if there are any set bits in _3 and return the smallest one.
						if( _4 != 0 ) return 192 + Long.numberOfTrailingZeros( _4 ); // If no set bits in _1, _2 or _3, check if there are any set bits in _4 and return the smallest one.
				}
			}
			
			return -1;
		}
		
		/**
		 * Removes a byte value from the set.
		 *
		 * @param key The byte value to remove from the set (valid range is 0-255).
		 * @return {@code true} if the set was modified as a result of this operation (i.e., the key was present),
		 * {@code false} otherwise, indicating the key was not in the set.
		 */
		protected boolean _remove( byte key ) {
			if( size == 0 ) return false; // Optimization: empty set cannot contain any key to remove.
			final long bit = ~( 1L << key ); // Calculate bitmask to clear the bit at the given key position. Ensure key is treated as unsigned byte.
			long       t;
			switch( key & 0b1100_0000 ) { // Determine segment to modify based on the higher 2 bits of the key.
				case 0b1100_0000:             // Values 192-255
					t = _4;
					if( ( _4 &= bit ) == t ) return false; // If the bit was already 0, no modification occurred.
					break;
				case 0b1000_0000:             // Values 128-191
					t = _3;
					if( ( _3 &= bit ) == t ) return false; // If the bit was already 0, no modification occurred.
					break;
				case 0b0100_0000:             // Values 64-127
					t = _2;
					if( ( _2 &= bit ) == t ) return false; // If the bit was already 0, no modification occurred.
					break;
				default:                      // Values 0-63
					t = _1;
					if( ( _1 &= bit ) == t ) return false; // If the bit was already 0, no modification occurred.
					break;
			}
			size--; // Decrement size as an element was removed.
			_version++; // Increment version to indicate structural modification.
			return true; // Value was removed.
		}
		
		/**
		 * Adds a byte value to the set.
		 *
		 * @param key The byte value to add to the set (valid range is 0-255).
		 * @return {@code true} if the set was modified as a result of this operation (i.e., the value was not already present),
		 * {@code false} otherwise, indicating the key was already in the set.
		 */
		protected boolean _add( final byte key ) {
			final long bit = 1L << key;        // Calculate the bitmask to set the bit at the given key position. Ensure key is treated as unsigned byte.
			long       t;
			switch( key & 0b1100_0000 ) {       // Determine which 64-bit segment to modify based on the higher 2 bits of the key.
				case 0b1100_0000:             // Values 192-255
					t = _4;
					if( ( _4 |= bit ) == t ) return false; // If the bit was already 1, no modification occurred.
					break;
				case 0b1000_0000:             // Values 128-191
					t = _3;
					if( ( _3 |= bit ) == t ) return false; // If the bit was already 1, no modification occurred.
					break;
				case 0b0100_0000:             // Values 64-127
					t = _2;
					if( ( _2 |= bit ) == t ) return false; // If the bit was already 1, no modification occurred.
					break;
				default:                      // Values 0-63
					t = _1;
					if( ( _1 |= bit ) == t ) return false; // If the bit was already 1, no modification occurred.
					break;
			}
			size++; // Increment size as a new element was added.
			_version++; // Increment version to indicate structural modification.
			return true; // Value was added.
		}
		
		/**
		 * Clears all byte values from the set, making it empty.
		 */
		protected void _clear() {
			_1   = _2 = _3 = _4 = 0; // Reset all bitset segments to 0.
			size = 0; // Reset size counter to 0.
			_version++; // Increment version to indicate structural modification.
		}
		
		/**
		 * Checks if the set contains the specified byte value.
		 * This method is optimized for primitive byte values and does not handle null keys.
		 *
		 * @param key The byte value to check for presence in the set (valid range is 0-255).
		 * @return {@code true} if the set contains the specified byte value, {@code false} otherwise.
		 */
		protected boolean get( byte key ) {
			if( size == 0 ) return false; // Optimization: empty set cannot contain any value.
			final int val = key & 0xFF;    // Ensure the byte value is within the range 0-255 (unsigned byte).
			return ( ( val < 128 ?
					// Check which 64-bit segment the byte value falls into.
					val < 64 ?
							// Check if in the first segment (0-63).
							_1 :
							// Access the second segment (64-127).
							_2 :
					// Access the third segment (128-191).
					val < 192 ?
							// Check if in the fourth segment (192-255).
							_3 :
							// Access the fourth segment (192-255).
							_4 ) & 1L << val ) != 0; // Access the corresponding segment and check if the bit corresponding to 'val' is set.
		}
		
		
	}
}

