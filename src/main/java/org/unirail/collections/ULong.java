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
 * Utility interface for handling unsigned 64-bit integer (long) values in Java.
 * <p>
 * Java's {@code long} primitive type is signed.  When you need to treat a {@code long} as unsigned,
 * especially for operations like division, remainder, comparison, and string conversion, you need
 * to use specific methods provided by the {@link Long} class. This interface provides static methods
 * that encapsulate these unsigned operations, making it easier to work with unsigned longs.
 * <p>
 * The following points are important to remember when working with unsigned longs in Java:
 * <ul>
 *     <li>Java represents {@code long} as a signed 64-bit two's complement integer.</li>
 *     <li>When interpreting a {@code long} as unsigned, the range shifts from
 *         {@code -2^63} to {@code 2^63 - 1} (signed) to {@code 0} to {@code 2^64 - 1} (unsigned).</li>
 *     <li>Values that are negative when interpreted as signed longs become very large positive values
 *         when interpreted as unsigned longs. For example, {@code -1L} (signed) is {@code 2^64 - 1} (unsigned).</li>
 *     <li>Java provides methods in the {@link Long} class (like {@link Long#divideUnsigned},
 *         {@link Long#compareUnsigned}, {@link Long#toUnsignedString}, etc.) to perform operations
 *         treating {@code long} values as unsigned.</li>
 * </ul>
 * <p>
 * This interface aims to group these utility methods for cleaner and more organized code when
 * dealing with unsigned long values.
 */
public interface ULong {
	/**
	 *  {@code -1L}: Represents the maximum unsigned long value (2^64 - 1).
	 *  <p>
	 *  {@code Long.MIN_VALUE}: Represents the smallest signed long value (-2^63), which is a large unsigned long value.
	 *  <p>
	 *  {@code Long.MAX_VALUE}: Represents the largest signed long value (2^63 - 1), which is in the middle of the unsigned long range.
	 *  <p>
	 *  {@code 0L}: Represents zero in both signed and unsigned interpretations.
	 *  <p>
	 *  This comment clarifies the relationship between signed and unsigned interpretations of {@code long}
	 *  values, particularly highlighting how the typical signed long boundaries relate to the unsigned range.
	 *  It serves as a reminder of the nuances when working with unsigned longs in Java using signed primitives.
	 */
	// Reminder of unsigned long value representation within Java's signed long type:
	//         -1L (signed)  ->  Maximum unsigned long value (2^64 - 1)
	//          ^
	//     Long.MIN_VALUE (signed) -> Large unsigned long value, but not the maximum
	//          ^
	//     Long.MAX_VALUE (signed) -> Mid-range unsigned long value
	//          ^
	//          0L (signed)   ->  Zero unsigned long value
	
	
	/**
	 * Method to divide two unsigned long values.
	 *
	 * @param dividend The value to be divided.
	 * @param divisor  The value to divide by.
	 * @return The result of the division.
	 */
	static long divide( long dividend, long divisor ) { return Long.divideUnsigned( dividend, divisor ); }
	
	/**
	 * Calculates the remainder of the division of two {@code long} values, treating them as unsigned.
	 * <p>
	 * This method uses {@link Long#remainderUnsigned(long, long)} to perform unsigned remainder calculation.
	 *
	 * @param dividend The unsigned dividend.
	 * @param divisor  The unsigned divisor.
	 * @return The unsigned remainder of the division.
	 * @throws ArithmeticException if the divisor is zero.
	 * @see Long#remainderUnsigned(long, long)
	 */
	static long remainder( long dividend, long divisor ) { return Long.remainderUnsigned( dividend, divisor ); }
	
	/**
	 * Parses a string argument as an unsigned {@code long} in base 10.
	 * <p>
	 * This method uses {@link Long#parseUnsignedLong(String)} with radix 10.
	 *
	 * @param string The string to be parsed.
	 * @return The unsigned {@code long} represented by the string argument.
	 * @throws NumberFormatException if the string does not contain a parsable unsigned {@code long}.
	 * @see Long#parseUnsignedLong(String)
	 */
	static long parse( String string ) { return Long.parseUnsignedLong( string, 10 ); }
	
	/**
	 * Compares two {@code long} values, treating them as unsigned.
	 * <p>
	 * This method uses {@link Long#compareUnsigned(long, long)} for unsigned comparison.
	 *
	 * @param if_bigger_plus  The first unsigned {@code long} to compare.
	 * @param if_bigger_minus The second unsigned {@code long} to compare.
	 * @return 0 if {@code value1 == value2}; a value less than 0 if {@code value1 < value2} as unsigned values;
	 * and a value greater than 0 if {@code value1 > value2} as unsigned values.
	 * @see Long#compareUnsigned(long, long)
	 */
	static int compare( long if_bigger_plus, long if_bigger_minus ) { return Long.compareUnsigned( if_bigger_plus, if_bigger_minus ); }
	
	
	/**
	 * Converts an unsigned {@code long} to its string representation in the specified radix.
	 * <p>
	 * If the radix is outside the range from {@link Character#MIN_RADIX} to {@link Character#MAX_RADIX} inclusive,
	 * then the radix {@code 10} is used.
	 * <p>
	 *
	 * @param ulong The unsigned {@code long} value to convert.
	 * @param radix The radix to use in the string representation.
	 * @return The string representation of the unsigned {@code long} value in the specified radix.
	 */
	static String toString( long ulong, int radix ) {
		if( 0 <= ulong ) return Long.toString( ulong, radix );
		final long quotient = ( ulong >>> 1 ) / radix << 1;
		final long rem      = ulong - quotient * radix;
		return rem < radix ?
				Long.toString( quotient, radix ) + Long.toString( rem, radix ) :
				Long.toString( quotient + 1, radix ) + Long.toString( rem - radix, radix );
	}
}