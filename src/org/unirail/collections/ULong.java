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