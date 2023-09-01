package org.unirail.collections;

import java.math.BigInteger;

public interface ULong {
	BigInteger ULONG_MASK = BigInteger.ONE.shiftLeft( Long.SIZE ).subtract( BigInteger.ONE );
	
	static BigInteger toULong( long src ) {
		return src < 0L ? BigInteger.valueOf( src ).and( ULONG_MASK ) : BigInteger.valueOf( src );
	}
	
	static String toString( long src ) {
		return src < 0L ? BigInteger.valueOf( src ).and( ULONG_MASK ).toString() : Long.toString( src );
	}
}
