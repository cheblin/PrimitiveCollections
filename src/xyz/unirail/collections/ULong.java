package xyz.unirail.collections;

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
	static long divide( long dividend, long divisor ) { return Long.divideUnsigned( dividend, divisor ); }
	
	static long remainder( long dividend, long divisor )            { return Long.remainderUnsigned( dividend, divisor ); }
	
	static long parse( String string )                              { return Long.parseUnsignedLong( string, 10 ); }
	
	static int compare( long if_bigger_plus, long if_bigger_minus ) { return Long.compareUnsigned( if_bigger_plus, if_bigger_minus ); }
	
	static String toString( long ulong, int radix ) {//This is the most efficient method to obtain the string representation of an unsigned long in Java.
		
		if( 0 <= ulong ) return Long.toString( ulong, radix );
		final long quotient = (ulong >>> 1) / radix << 1;
		final long rem      = ulong - quotient * radix;
		return rem < radix ?
		       Long.toString( quotient, radix ) + Long.toString( rem, radix ) :
		       Long.toString( quotient + 1, radix ) + Long.toString( rem - radix, radix );
	}
}
