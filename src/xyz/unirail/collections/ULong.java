package xyz.unirail.collections;

/**
 * Methods for handling unsigned long values in Java.
 */
public interface ULong {
	//         -1
	//          ^
	//     Long.MIN_VALUE
	//          ^
	//     Long.MAX_VALUE
	//          ^
	//          0
	//This code serves as a reminder of the specific functionalities provided in Java for handling unsigned long values.
	//All other operations remain the same as those for signed long values.
	
	/**
	 Method to divide two unsigned long values.
	 
	 @param dividend The value to be divided.
	 @param divisor  The value to divide by.
	 @return The result of the division.
	 */
	static long divide( long dividend, long divisor ) { return Long.divideUnsigned( dividend, divisor ); }
	
	/**
	 Method to get the remainder of dividing two unsigned long values.
	 
	 @param dividend The value to be divided.
	 @param divisor  The value to divide by.
	 @return The remainder of the division.
	 */
	static long remainder( long dividend, long divisor ) { return Long.remainderUnsigned( dividend, divisor ); }
	
	/**
	 Method to parse an unsigned long from a string.
	 
	 @param string The string to parse.
	 @return The parsed unsigned long value.
	 */
	static long parse( String string ) { return Long.parseUnsignedLong( string, 10 ); }
	
	/**
	 Method to compare two unsigned long values.
	 
	 @param if_bigger_plus  The first value to compare.
	 @param if_bigger_minus The second value to compare.
	 @return The result of the comparison.
	 */
	static int compare( long if_bigger_plus, long if_bigger_minus ) { return Long.compareUnsigned( if_bigger_plus, if_bigger_minus ); }
	
	/**
	 Method to convert an unsigned long to a string.
	 
	 @param ulong The unsigned long value to convert.
	 @param radix The radix to use in the conversion.
	 @return The string representation of the unsigned long value.
	 */
	static String toString( long ulong, int radix ) {
		if( 0 <= ulong ) return Long.toString( ulong, radix );
		final long quotient = (ulong >>> 1) / radix << 1;
		final long rem      = ulong - quotient * radix;
		return rem < radix ?
		       Long.toString( quotient, radix ) + Long.toString( rem, radix ) :
		       Long.toString( quotient + 1, radix ) + Long.toString( rem - radix, radix );
	}
}