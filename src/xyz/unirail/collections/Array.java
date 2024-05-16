package xyz.unirail.collections;


import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

public interface Array {
	class EqualHashOf<T> { //C#  IEqualityComparer (https://learn.microsoft.com/en-us/dotnet/api/system.collections.generic.iequalitycomparer-1?view=net-8.0) equivalent
		public final T[] OO;
		public T[] copyOf( T[] src, int len ) {
			return len < 1 ?
			       OO :
			       Arrays.copyOf( src == null ?
			                      OO :
			                      src, len );
		}
		private final boolean array;
		@SuppressWarnings( "unchecked" )
		public EqualHashOf( Class<T> clazz ) {
			array = clazz.isArray();
			OO    = (T[]) java.lang.reflect.Array.newInstance( clazz, 0 );
		}
		
		@SuppressWarnings( "unchecked" )
		public int hashCode( T src ) {
			return array ?
			       hashCode( (T[]) src ) :
			       hash( src );
		}
		public boolean equals( T v1, T v2 ) { return Objects.deepEquals( v1, v2 ); }
		
		public int hashCode( T[] src ) {
			return src == null ?
			       0 :
			       hashCode( src, src.length );
		}
		@SuppressWarnings( "unchecked" )
		public int hashCode( T[] src, int size ) {
			int seed = EqualHashOf.class.hashCode();
			if( array )
			{
				while( -1 < --size ) seed = (size + 10153331) + hash( seed, hash( src[size] ) );
				return seed;
			}
			
			switch( size )
			{
				case 0:
					return Array.finalizeHash( seed, 0 );
				case 1:
					return Array.finalizeHash( Array.mix( seed, Array.hash( src[0] ) ), 1 );
			}
			
			final int initial   = Array.hash( src[0] );
			int       prev      = Array.hash( src[1] );
			final int rangeDiff = prev - initial;
			int       h         = Array.mix( seed, initial );
			
			for( int i = 2; i < size; ++i )
			{
				h = Array.mix( h, prev );
				final int hash = Array.hash( src[i] );
				if( rangeDiff != hash - prev )
				{
					for( h = Array.mix( h, hash ), ++i; i < size; ++i )
					     h = Array.mix( h, Array.hash( src[i] ) );
					
					return Array.finalizeHash( h, size );
				}
				prev = hash;
			}
			
			return Array.avalanche( Array.mix( Array.mix( h, rangeDiff ), prev ) );
		}
		
		
		@SuppressWarnings( "unchecked" )
		public boolean equals( T[] O, T[] X, int len ) {
			if( O != X )
			{
				if( O == null || X == null || O.length < len || X.length < len ) return false;
				if( array )
				{
					for( T o, x; -1 < --len; )
						if( (o = O[len]) != (x = X[len]) ) if( o == null || x == null || !Arrays.deepEquals( (T[]) o, (T[]) x ) ) return false;
				}
				else
					for( T o, x; -1 < --len; )
						if( (o = O[len]) != (x = X[len]) ) if( o == null || x == null || !equals( o, x ) ) return false;
			}
			return true;
		}
		public boolean equals( T[] O, T[] X ) { return O == X || O != null && X != null && O.length == X.length && equals( O, X, O.length ); }
		
		public static final booleans booleans = new booleans();
		
		public static final class booleans extends EqualHashOf<boolean[]> {
			public static final boolean[] O = new boolean[0];
			
			
			private booleans()                                            { super( boolean[].class ); }
			@Override public int hashCode( boolean[] src )                { return Arrays.hashCode( src ); }
			@Override public boolean equals( boolean[] v1, boolean[] v2 ) { return Arrays.equals( v1, v2 ); }
			@Override public int hashCode( boolean[][] src, int size ) {
				int hash = boolean[][].class.hashCode();
				while( -1 < --size ) hash = (size + 10153331) + hash( hash, Arrays.hashCode( src[size] ) );
				return hash;
			}
			
			@Override public boolean equals( boolean[][] O, boolean[][] X, int len ) {
				if( O != X )
				{
					if( O == null || X == null || O.length < len || X.length < len ) return false;
					for( boolean[] o, x; -1 < --len; )
						if( (o = O[len]) != (x = X[len]) ) if( o == null || x == null || !Arrays.equals( o, x ) ) return false;
				}
				return true;
			}
		}
		
		public static final bytes bytes = new bytes();
		
		public static final class bytes extends EqualHashOf<byte[]> {
			public static final byte[] O = new byte[0];
			
			private bytes()                                         { super( byte[].class ); }
			@Override public int hashCode( byte[] src )             { return Arrays.hashCode( src ); }
			@Override public boolean equals( byte[] v1, byte[] v2 ) { return Arrays.equals( v1, v2 ); }
			@Override public int hashCode( byte[][] src, int size ) {
				int hash = byte[][].class.hashCode();
				while( -1 < --size )
				{
					final byte[] data = src[size];
					int          len  = data.length, i, k = 0;
					
					for( i = 0; 3 < len; i += 4, len -= 4 )
					     hash = mix( hash, data[i] & 0xFF
					                       | (data[i + 1] & 0xFF) << 8
					                       | (data[i + 2] & 0xFF) << 16
					                       | (data[i + 3] & 0xFF) << 24 );
					switch( len )
					{
						case 3:
							k ^= (data[i + 2] & 0xFF) << 16;
						case 2:
							k ^= (data[i + 1] & 0xFF) << 8;
					}
					
					hash = finalizeHash( mixLast( hash, k ^ data[i] & 0xFF ), data.length );
				}
				
				return finalizeHash( hash, src.length );
			}
			
			@Override public boolean equals( byte[][] O, byte[][] X, int len ) {
				if( O != X )
				{
					if( O == null || X == null || O.length < len || X.length < len ) return false;
					for( byte[] o, x; -1 < --len; )
						if( (o = O[len]) != (x = X[len]) ) if( o == null || x == null || !Arrays.equals( o, x ) ) return false;
				}
				return true;
			}
		}
		
		public static final shorts shorts = new shorts();
		
		public static final class shorts extends EqualHashOf<short[]> {
			public static final short[] O = new short[0];
			
			private shorts()                                          { super( short[].class ); }
			
			@Override public int hashCode( short[] src )              { return Arrays.hashCode( src ); }
			@Override public boolean equals( short[] v1, short[] v2 ) { return Arrays.equals( v1, v2 ); }
			@Override public int hashCode( short[][] src, int size ) {
				int hash = short[][].class.hashCode();
				while( -1 < --size ) hash = (size + 10153331) + hash( hash, Arrays.hashCode( src[size] ) );
				return hash;
			}
			
			@Override public boolean equals( short[][] O, short[][] X, int len ) {
				if( O != X )
				{
					if( O == null || X == null || O.length < len || X.length < len ) return false;
					for( short[] o, x; -1 < --len; )
						if( (o = O[len]) != (x = X[len]) ) if( o == null || x == null || !Arrays.equals( o, x ) ) return false;
				}
				return true;
			}
		}
		
		public static final chars chars = new chars();
		
		public static final class chars extends EqualHashOf<char[]> {
			public static final char[] O = new char[0];
			
			private chars()                                         { super( char[].class ); }
			@Override public int hashCode( char[] src )             { return Arrays.hashCode( src ); }
			@Override public boolean equals( char[] v1, char[] v2 ) { return Arrays.equals( v1, v2 ); }
			@Override public int hashCode( char[][] src, int size ) {
				int hash = char[][].class.hashCode();
				while( -1 < --size ) hash = (size + 10153331) + hash( hash, Arrays.hashCode( src[size] ) );
				return hash;
			}
			
			@Override public boolean equals( char[][] O, char[][] X, int len ) {
				if( O != X )
				{
					if( O == null || X == null || O.length < len || X.length < len ) return false;
					for( char[] o, x; -1 < --len; )
						if( (o = O[len]) != (x = X[len]) ) if( o == null || x == null || !Arrays.equals( o, x ) ) return false;
				}
				return true;
			}
		}
		
		public static final ints ints = new ints();
		
		public static final class ints extends EqualHashOf<int[]> {
			public static final int[] O = new int[0];
			
			private ints()                                        { super( int[].class ); }
			@Override public int hashCode( int[] src )            { return Arrays.hashCode( src ); }
			@Override public boolean equals( int[] v1, int[] v2 ) { return Arrays.equals( v1, v2 ); }
			@Override public int hashCode( int[][] src, int size ) {
				int hash = int[][].class.hashCode();
				while( -1 < --size ) hash = (size + 10153331) + hash( hash, Arrays.hashCode( src[size] ) );
				return hash;
			}
			
			@Override public boolean equals( int[][] O, int[][] X, int len ) {
				if( O != X )
				{
					if( O == null || X == null || O.length < len || X.length < len ) return false;
					for( int[] o, x; -1 < --len; )
						if( (o = O[len]) != (x = X[len]) ) if( o == null || x == null || !Arrays.equals( o, x ) ) return false;
				}
				return true;
			}
		}
		
		public static final longs longs = new longs();
		
		public static final class longs extends EqualHashOf<long[]> {
			public static final long[] O = new long[0];
			
			private longs()                                         { super( long[].class ); }
			@Override public int hashCode( long[] src )             { return Arrays.hashCode( src ); }
			@Override public boolean equals( long[] v1, long[] v2 ) { return Arrays.equals( v1, v2 ); }
			@Override public int hashCode( long[][] src, int size ) {
				int hash = long[][].class.hashCode();
				while( -1 < --size ) hash = (size + 10153331) + hash( hash, Arrays.hashCode( src[size] ) );
				return hash;
			}
			
			@Override public boolean equals( long[][] O, long[][] X, int len ) {
				if( O != X )
				{
					if( O == null || X == null || O.length < len || X.length < len ) return false;
					for( long[] o, x; -1 < --len; )
						if( (o = O[len]) != (x = X[len]) ) if( o == null || x == null || !Arrays.equals( o, x ) ) return false;
				}
				return true;
			}
		}
		
		public static final floats floats = new floats();
		
		public static final class floats extends EqualHashOf<float[]> {
			public static final float[] O = new float[0];
			
			private floats()                                          { super( float[].class ); }
			@Override public int hashCode( float[] src )              { return Arrays.hashCode( src ); }
			@Override public boolean equals( float[] v1, float[] v2 ) { return Arrays.equals( v1, v2 ); }
			@Override public int hashCode( float[][] src, int size ) {
				int hash = float[][].class.hashCode();
				while( -1 < --size ) hash = (size + 10153331) + hash( hash, Arrays.hashCode( src[size] ) );
				return hash;
			}
			
			@Override public boolean equals( float[][] O, float[][] X, int len ) {
				if( O != X )
				{
					if( O == null || X == null || O.length < len || X.length < len ) return false;
					for( float[] o, x; -1 < --len; )
						if( (o = O[len]) != (x = X[len]) ) if( o == null || x == null || !Arrays.equals( o, x ) ) return false;
				}
				return true;
			}
		}
		
		public static final doubles doubles = new doubles();
		
		public static final class doubles extends EqualHashOf<double[]> {
			public static final double[] O = new double[0];
			
			private doubles()                                           { super( double[].class ); }
			@Override public int hashCode( double[] src )               { return Arrays.hashCode( src ); }
			@Override public boolean equals( double[] v1, double[] v2 ) { return Arrays.equals( v1, v2 ); }
			@Override public int hashCode( double[][] src, int size ) {
				int hash = double[][].class.hashCode();
				while( -1 < --size ) hash = (size + 10153331) + hash( hash, Arrays.hashCode( src[size] ) );
				return hash;
			}
			
			@Override public boolean equals( double[][] O, double[][] X, int len ) {
				if( O != X )
				{
					if( O == null || X == null || O.length < len || X.length < len ) return false;
					for( double[] o, x; -1 < --len; )
						if( (o = O[len]) != (x = X[len]) ) if( o == null || x == null || !Arrays.equals( o, x ) ) return false;
				}
				return true;
			}
		}
		
		public static final strings strings = new strings();
		
		public static final class strings extends EqualHashOf<String[]> {
			public static final String[] O = new String[0];
			String[][] OO = new String[0][];
			
			private strings()                                           { super( String[].class ); }
			@Override public int hashCode( String[] src )               { return Arrays.hashCode( src ); }
			@Override public boolean equals( String[] v1, String[] v2 ) { return Arrays.equals( v1, v2 ); }
			@Override public int hashCode( String[][] src, int size ) {
				int hash = strings.class.hashCode();
				if( src == null ) return hash;
				
				int i = size;
				for( String[] o; -1 < --i; )
				     hash = mix( hash, hash( o = src[i], o.length ) );
				
				return finalizeHash( hash, size );
			}
			
			@Override public boolean equals( String[][] O, String[][] X ) {
				if( O != X )
				{
					if( O == null || X == null || O.length != X.length ) return false;
					for( int i = O.length; -1 < --i; )
						if( !Arrays.equals( O[i], X[i] ) ) return false;
				}
				return true;
			}
		}
		
		
		public interface objects {
			Object[]   O  = new Object[0];
			Object[][] OO = new Object[0][];
			
			static boolean equals( Object[][] O, Object[][] X ) {
				if( O != X )
				{
					if( O == null || X == null || O.length != X.length ) return false;
					for( int i = O.length; -1 < --i; ) if( !Arrays.equals( O[i], X[i] ) ) return false;
				}
				return true;
			}
			
			static int hash( int hash, Object[][] src ) {
				if( src == null ) return hash ^ 10153331;
				for( Object[] s : src ) hash ^= Arrays.hashCode( s );
				return hash;
			}
		}
	}
	
	@SuppressWarnings( "unchecked" )
	static <T> T[] copyOf( T[] src, int len ) {
		return len < 1 ?
		       (T[]) EqualHashOf.objects.O :
		       Arrays.copyOf( src == null ?
		                      (T[]) EqualHashOf.objects.O :
		                      src, len );
	}
	
	static <T> T[] resize( T[] src, int len, T val ) {
		return src == null ?
		       fill( copyOf( (T[]) null, len ), val ) :
		       src.length == len ?
		       src :
		       fill( copyOf( src, len ), src.length, len, val );
	}
	
	
	static <T> T[] fill( T[] dst, T val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static <T> T[] fill( T[] dst, int fromIndex, int toIndex, T val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[i] = val;
		return dst;
	}
	
	
	@SuppressWarnings( "unchecked" )
	static <T> T[][] copyOf( T[][] src, int len ) {
		return len < 1 ?
		       (T[][]) EqualHashOf.objects.O :
		       Arrays.copyOf( src == null ?
		                      (T[][]) EqualHashOf.objects.O :
		                      src, len );
	}
	
	static <T> T[][] resize( T[][] src, int len, T[] val ) {
		return src == null ?
		       fill( copyOf( (T[][]) null, len ), val ) :
		       src.length == len ?
		       src :
		       fill( copyOf( src, len ), src.length, len, val );
	}
	
	
	static <T> T[][] fill( T[][] dst, T[] val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static <T> T[][] fill( T[][] dst, int fromIndex, int toIndex, T[] val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[i] = val;
		return dst;
	}
	
	
	static boolean[] copyOf( boolean[] src, int len ) {
		return len < 1 ?
		       EqualHashOf.booleans.O :
		       Arrays.copyOf( src == null ?
		                      EqualHashOf.booleans.O :
		                      src, len );
	}
	
	static boolean[] resize( boolean[] src, int len, boolean val ) {
		return src == null ?
		       fill( copyOf( EqualHashOf.booleans.O, len ), val ) :
		       src.length == len ?
		       src :
		       fill( copyOf( src, len ), src.length, len, val );
	}
	
	static boolean[] fill( boolean[] dst, boolean val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static boolean[] fill( boolean[] dst, int fromIndex, int toIndex, boolean val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static byte[] copyOf( byte[] src, int len ) {
		return len < 1 ?
		       EqualHashOf.bytes.O :
		       Arrays.copyOf( src == null ?
		                      EqualHashOf.bytes.O :
		                      src, len );
	}
	
	static byte[] resize( byte[] src, int len, byte val ) {
		return src == null ?
		       fill( copyOf( EqualHashOf.bytes.O, len ), val ) :
		       src.length == len ?
		       src :
		       fill( copyOf( src, len ), src.length, len, val );
	}
	
	static byte[] fill( byte[] dst, byte val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static byte[] fill( byte[] dst, int fromIndex, int toIndex, byte val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static short[] copyOf( short[] src, int len ) {
		return len < 1 ?
		       EqualHashOf.shorts.O :
		       Arrays.copyOf( src == null ?
		                      EqualHashOf.shorts.O :
		                      src, len );
	}
	
	static short[] resize( short[] src, int len, short val ) {
		return src == null ?
		       fill( copyOf( EqualHashOf.shorts.O, len ), val ) :
		       src.length == len ?
		       src :
		       fill( copyOf( src, len ), src.length, len, val );
	}
	
	static short[] fill( short[] dst, short val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static short[] fill( short[] dst, int fromIndex, int toIndex, short val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static char[] copyOf( char[] src, int len ) {
		return len < 1 ?
		       EqualHashOf.chars.O :
		       Arrays.copyOf( src == null ?
		                      EqualHashOf.chars.O :
		                      src, len );
	}
	
	static char[] resize( char[] src, int len, char val ) {
		return src == null ?
		       fill( copyOf( EqualHashOf.chars.O, len ), val ) :
		       src.length == len ?
		       src :
		       fill( copyOf( src, len ), src.length, len, val );
	}
	
	static char[] fill( char[] dst, char val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static char[] fill( char[] dst, int fromIndex, int toIndex, char val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static int[] copyOf( int[] src, int len ) {
		return len < 1 ?
		       EqualHashOf.ints.O :
		       Arrays.copyOf( src == null ?
		                      EqualHashOf.ints.O :
		                      src, len );
	}
	
	static int[] resize( int[] src, int len, int val ) {
		return src == null ?
		       fill( copyOf( EqualHashOf.ints.O, len ), val ) :
		       src.length == len ?
		       src :
		       fill( copyOf( src, len ), src.length, len, val );
	}
	
	static int[] fill( int[] dst, int val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static int[] fill( int[] dst, int fromIndex, int toIndex, int val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static long[] copyOf( long[] src, int len ) {
		return len < 1 ?
		       EqualHashOf.longs.O :
		       Arrays.copyOf( src == null ?
		                      EqualHashOf.longs.O :
		                      src, len );
	}
	
	static long[] resize( long[] src, int len, long val ) {
		return src == null ?
		       fill( copyOf( EqualHashOf.longs.O, len ), val ) :
		       src.length == len ?
		       src :
		       fill( copyOf( src, len ), src.length, len, val );
	}
	
	static long[] fill( long[] dst, long val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static long[] fill( long[] dst, int fromIndex, int toIndex, long val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[i] = val;
		return dst;
	}
	
	
	static float[] copyOf( float[] src, int len ) {
		return len < 1 ?
		       EqualHashOf.floats.O :
		       Arrays.copyOf( src == null ?
		                      EqualHashOf.floats.O :
		                      src, len );
	}
	
	static float[] resize( float[] src, int len, float val ) {
		return src == null ?
		       fill( copyOf( EqualHashOf.floats.O, len ), val ) :
		       src.length == len ?
		       src :
		       fill( copyOf( src, len ), src.length, len, val );
	}
	
	static float[] fill( float[] dst, float val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static float[] fill( float[] dst, int fromIndex, int toIndex, float val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[i] = val;
		return dst;
	}
	
	
	static double[] copyOf( double[] src, int len ) {
		return len < 1 ?
		       EqualHashOf.doubles.O :
		       Arrays.copyOf( src == null ?
		                      EqualHashOf.doubles.O :
		                      src, len );
	}
	
	static double[] resize( double[] src, int len, double val ) {
		return src == null ?
		       fill( copyOf( EqualHashOf.doubles.O, len ), val ) :
		       src.length == len ?
		       src :
		       fill( copyOf( src, len ), src.length, len, val );
	}
	
	static double[] fill( double[] dst, double val ) {
		for( int i = 0, len = dst.length; i < len; i++ )
		     dst[i] = val;
		return dst;
	}
	
	static double[] fill( double[] dst, int fromIndex, int toIndex, double val ) {
		for( int i = fromIndex; i < toIndex; i++ )
		     dst[i] = val;
		return dst;
	}
	
	
	HashMap<Class<?>, Object> pool = new HashMap<>( 8 );
	
	@SuppressWarnings( "unchecked" )
	static <T> EqualHashOf<T> get( Class<T> clazz ) {
		final Object c = clazz;
		if( c == boolean[].class ) return (EqualHashOf<T>) EqualHashOf.booleans;
		if( c == byte[].class ) return (EqualHashOf<T>) EqualHashOf.bytes;
		if( c == short[].class ) return (EqualHashOf<T>) EqualHashOf.shorts;
		if( c == int[].class ) return (EqualHashOf<T>) EqualHashOf.ints;
		if( c == long[].class ) return (EqualHashOf<T>) EqualHashOf.longs;
		if( c == char[].class ) return (EqualHashOf<T>) EqualHashOf.chars;
		if( c == float[].class ) return (EqualHashOf<T>) EqualHashOf.floats;
		if( c == double[].class ) return (EqualHashOf<T>) EqualHashOf.doubles;
		if( c == String[].class ) return (EqualHashOf<T>) EqualHashOf.strings;
		
		if( pool.containsKey( clazz ) ) return (EqualHashOf<T>) pool.get( clazz );
		synchronized(pool)
		{
			if( pool.containsKey( clazz ) ) return (EqualHashOf<T>) pool.get( clazz );
			EqualHashOf<T> ret = new EqualHashOf<>( clazz );
			pool.put( clazz, ret );
			return ret;
		}
	}
	
	static int resize( Object src, Object dst, int index, int size, final int resize ) {
		if( resize < 0 )
		{
			if( index + (-resize) < size )
			{
				System.arraycopy( src, index + (-resize), dst, index, size - (index + (-resize)) );
				if( src != dst ) System.arraycopy( src, 0, dst, 0, index );
				return size + resize;
			}
			return index;
		}
		final int new_size = Math.max( index, size ) + resize;
		if( 0 < size ) if( index < size ) if( index == 0 ) System.arraycopy( src, 0, dst, resize, size );
		else
		{
			if( src != dst ) System.arraycopy( src, 0, dst, 0, index );
			System.arraycopy( src, index, dst, index + resize, size - index );
		}
		else if( src != dst ) System.arraycopy( src, 0, dst, 0, size );
		return new_size;
	}
	
	static void copy( Object src, int index, final int skip, int size, Object dst ) {
		if( 0 < index ) System.arraycopy( src, 0, dst, 0, index );
		if( (index += skip) < size ) System.arraycopy( src, index, dst, index, size - index );
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
	
	static boolean equals( Object[][] O, Object[][] X ) {
		if( O != X )
		{
			if( O == null || X == null || O.length != X.length ) return false;
			for( int i = O.length; -1 < --i; ) if( !Arrays.equals( O[i], X[i] ) ) return false;
		}
		return true;
	}
	
	static int mix( int hash, int data ) {
		return Integer.rotateLeft( mixLast( hash, data ), 13 ) * 5 + 0xe6546b64;
	}
	
	static int mixLast( int hash, int data ) {
		return hash ^ Integer.rotateLeft( data * 0xcc9e2d51, 15 ) * 0x1b873593;
	}
	
	static int finalizeHash( int hash, int length ) {
		return avalanche( hash ^ length );
	}
	
	static int avalanche( int size ) {
		
		size = (size ^ size >>> 16) * 0x85ebca6b;
		size = (size ^ size >>> 13) * 0xc2b2ae35;
		return size ^ size >>> 16;
	}
	
	static int hash( int hash, Object[][] src ) {
		
		if( src == null ) return 0;
		for( Object[] s : src ) hash ^= Arrays.hashCode( s );
		
		return hash;
	}
	
	static int hash( int hash, Object src ) { return hash ^ hash( src ); }
	
	static int hash( int hash, double src ) { return hash ^ hash( src ); }
	
	static int hash( int hash, float src )  { return hash ^ hash( src ); }
	
	static int hash( int hash, int src )    { return hash ^ hash( src ); }
	
	static int hash( int hash, long src )   { return hash ^ hash( src ); }
	
	static int hash( Object src ) {
		return src == null ?
		       0 :
		       src.hashCode();
	}
	
	static int hash( double src ) { return Double.hashCode( src ); }
	
	static int hash( float src )  { return Float.hashCode( src ); }
	
	static int hash( int src )    { return src; }
	
	static int hash( long src )   { return Long.hashCode( src ); }
	
	static int hash( final String str ) {
		if( str == null ) return 52861;
		if( str.isEmpty() ) return 37607;
		
		int h = 61667;
		
		long x = 0;
		for( int i = 0, b = 0; i < str.length(); i++ )
		{
			long ch = str.charAt( i );
			x |= ch << b;
			if( (b += ch < 0x100 ?
			          8 :
			          16) < 32 ) continue;
			
			h = mix( h, (int) x );
			x >>>= 32;
			b -= 32;
		}
		
		h = mixLast( h, (int) x );
		
		return finalizeHash( h, str.length() );
	}
	
	static int hash( final String[] src, int size ) {
		int h = String[].class.hashCode();
		
		for( int i = src.length - 1; -1 < i; i-- )
		     h = mix( h, hash( src[i] ) );
		return finalizeHash( h, size );
	}
	
	interface ISort {
		
		abstract class Index implements ISort {
			public char[] dst;
			public int    size;
			@Override public void swap( int ia, int ib ) {
				final char t = dst[ia];
				dst[ia] = dst[ib];
				dst[ib] = t;
			}
			@Override public void copy( int idst, int isrc ) { dst[idst] = dst[isrc]; }
			char fixi = 0;
			@Override public void drop( int idst ) { dst[idst] = fixi; }
		}
		
		abstract class Index2 implements ISort {
			public int[] dst;
			public int   size;
			@Override public void swap( int ia, int ib ) {
				final int t = dst[ia];
				dst[ia] = dst[ib];
				dst[ib] = t;
			}
			@Override public void copy( int idst, int isrc ) { dst[idst] = dst[isrc]; }
			int fixi = 0;
			@Override public void drop( int idst ) { dst[idst] = fixi; }
		}
		
		interface Primitives {
			long get( int index );
			
			default int compare( long x, long y ) { return Long.compare( x, y ); }
			
			interface floats extends Primitives {
				float get2( int index );
				
				default long get( int index ) { return Float.floatToIntBits( get2( index ) ); }
				
				default int compare( long x, long y ) {
					final float X = Float.intBitsToFloat( (int) x );
					final float Y = Float.intBitsToFloat( (int) y );
					return X < Y ?
					       -1 :
					       Y < X ?
					       1 :
					       (Long.compare( x, y ));
				}
			}
			
			interface doubles extends Primitives {
				double get2( int index );
				
				default long get( int index ) { return Double.doubleToLongBits( get2( index ) ); }
				
				default int compare( long x, long y ) {
					final double X = Double.longBitsToDouble( x );
					final double Y = Double.longBitsToDouble( y );
					return X < Y ?
					       -1 :
					       Y < X ?
					       1 :
					       (Long.compare( x, y ));
				}
			}
			
			
			class Index extends ISort.Index {
				public Primitives src;
				
				@Override public int compare( int ia, int ib ) { return src.compare( src.get( dst[ia] ), src.get( dst[ib] ) ); }
				@Override public int compare( int isrc )       { return src.compare( fix, src.get( dst[isrc] ) ); }
				long fix = 0;
				@Override public void hold( int isrc ) { fix = src.get( fixi = dst[isrc] ); }
			}
			
			class Index2 extends ISort.Index2 {
				public Primitives src;
				
				@Override public int compare( int ia, int ib ) { return src.compare( src.get( dst[ia] ), src.get( dst[ib] ) ); }
				@Override public int compare( int isrc )       { return src.compare( fix, src.get( dst[isrc] ) ); }
				long fix = 0;
				@Override public void hold( int isrc ) { fix = src.get( fixi = dst[isrc] ); }
			}
			
			class Direct implements ISort {
				
				interface PrimitivesSet extends ISort.Primitives {
					void set( int index, long value );
				}
				
				public PrimitivesSet array;
				
				@Override public int compare( int ia, int ib ) { return array.compare( array.get( ia ), array.get( ib ) ); }
				@Override public int compare( int isrc )       { return array.compare( fix, array.get( isrc ) ); }
				long fix = 0;
				@Override public void hold( int isrc ) { fix = array.get( fixi = isrc ); }
				
				@Override public void swap( int ia, int ib ) {
					final long t = array.get( ia );
					array.set( ia, array.get( ib ) );
					array.set( ib, t );
				}
				@Override public void copy( int idst, int isrc ) { array.set( idst, array.get( isrc ) ); }
				int fixi = 0;
				@Override public void drop( int idst ) { array.set( idst, fixi ); }
			}
		}
		
		interface Anything {
			
			void hold( int isrc );
			
			int compare( int ia, int ib );
			
			int compare( int ib );
			
			class Index extends ISort.Index {
				public Anything src;
				
				@Override public int compare( int ia, int ib ) { return src.compare( dst[ia], dst[ib] ); }
				@Override public int compare( int isrc )       { return src.compare( dst[isrc] ); }
				@Override public void hold( int isrc )         { src.hold( fixi = dst[isrc] ); }
			}
			
			class Index2 extends ISort.Index2 {
				public Anything src;
				
				@Override public int compare( int ia, int ib ) { return src.compare( dst[ia], dst[ib] ); }
				@Override public int compare( int isrc )       { return src.compare( dst[isrc] ); }
				@Override public void hold( int isrc )         { src.hold( fixi = dst[isrc] ); }
			}
		}
		
		
		abstract class Objects<T> implements Anything, Comparator<T> {
			abstract T get( int index );
			public int compare( T o1, T o2 )               { return Integer.compare( hash( o1 ), hash( o2 ) ); }
			
			@Override public int compare( int ia, int ib ) { return compare( get( ia ), get( ib ) ); }
			@Override public int compare( int ib )         { return compare( fix, get( ib ) ); }
			
			T fix;
			@Override public void hold( int isrc ) { fix = get( isrc ); }
		}
		
		
		int compare( int ia, int ib );//array[ia] < array[ib] ? -1 : array[ia] == array[ib] ? 0 : 1;
		
		/**
		 var t = indexes[ia];
		 indexes[ia] = indexes[ib];
		 indexes[ib] = t;
		 */
		void swap( int ia, int ib );
		
		void copy( int dst_index, int src_index );//replace element at dst_index with element at src_index
		
		int compare( int src_index );// compare fixed element with element at index;
		
		void hold( int src_index );//fix a value at index
		
		void drop( int dst_index );//put fixed value at index
		
		//binary search in sorted array index of the element that value is equal to the fixed element
		static int search( ISort dst, int lo, int hi ) {
			while( lo <= hi )
			{
				final int o = lo + (hi - lo >> 1), dir = dst.compare( o );
				
				if( dir == 0 ) return o;
				if( 0 < dir ) lo = o + 1;
				else hi = o - 1;
			}
			return ~lo;
		}
		
		
		static void sort( ISort dst, int lo, int hi ) {
			
			int len = hi - lo + 1;
			if( len < 2 ) return;
			
			int pow2 = 0;
			while( 0 < len )
			{
				pow2++;
				len >>>= 1;
			}
			
			sort( dst, lo, hi, 2 * pow2 );
		}
		
		static void sort( ISort dst, final int lo, int hi, int limit ) {
			
			while( hi > lo )
			{
				int size = hi - lo + 1;
				
				if( size < 17 ) switch( size )
				{
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
						for( int i = lo; i < hi; i++ )
						{
							dst.hold( i + 1 );
							
							int j = i;
							while( lo <= j && dst.compare( j ) < 0 ) dst.copy( j + 1, j-- );
							
							dst.drop( j + 1 );
						}
						return;
				}
				
				if( limit == 0 )
				{
					final int w = hi - lo + 1;
					for( int i = w >>> 1; 0 < i; i-- ) heapify( dst, i, w, lo );
					
					for( int i = w; 1 < i; i-- )
					{
						dst.swap( lo, lo + i - 1 );
						heapify( dst, 1, i - 1, lo );
					}
					return;
				}
				
				limit--;
				
				final int o = lo + (hi - lo >> 1);
				
				if( dst.compare( lo, o ) > 0 ) dst.swap( lo, o );
				if( dst.compare( lo, hi ) > 0 ) dst.swap( lo, hi );
				if( dst.compare( o, hi ) > 0 ) dst.swap( o, hi );
				
				dst.hold( o );
				dst.swap( o, hi - 1 );
				int l = lo, h = hi - 1;
				
				while( l < h )
				{
					while( -1 < dst.compare( ++l ) ) ;
					while( dst.compare( --h ) < 0 ) ;
					
					if( h <= l ) break;
					
					dst.swap( l, h );
				}
				
				if( l != hi - 1 ) dst.swap( l, hi - 1 );
				sort( dst, l + 1, hi, limit );
				hi = l - 1;
			}
		}
		
		static void heapify( ISort dst, int i, final int w, int lo ) {
			dst.hold( --lo + i );
			
			while( i <= w >>> 1 )
			{
				int child = i << 1;
				if( child < w && dst.compare( lo + child, lo + child + 1 ) < 0 ) child++;
				
				if( -1 < dst.compare( lo + child ) ) break;
				
				dst.copy( lo + i, lo + child );
				i = child;
			}
			
			dst.drop( lo + i );
		}
	}
}
