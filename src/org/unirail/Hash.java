package org.unirail;

public interface Hash {
	
	static int code( int hash, Object val ) {return hash ^ code( val );}
	
	static int code( int hash, double val ) {return hash ^ code( val );}
	
	static int code( int hash, float val )  {return hash ^ code( val );}
	
	static int code( int hash, long val )   {return hash ^ code( val );}
	
	static int code( int hash, int val )    {return hash ^ code( val );}
	
	static int code( Object val )           {return val == null ? 0x85ebca6b : code( val.hashCode() );}
	
	static int code( double val )           {return code( Double.doubleToLongBits( val ) );}
	
	static int code( float val )            {return code( Float.floatToIntBits( val ) );}
	
	static int code( long val ) {
		val = (val ^ (val >>> 32)) * 0x4cd6944c5cc20b6dL;
		val = (val ^ (val >>> 29)) * 0xfc12c5b19d3259e9L;
		return (int) (val ^ (val >>> 32));
	}
	
	static int code( int val ) {
		val = (val ^ (val >>> 16)) * 0x85ebca6b;
		val = (val ^ (val >>> 13)) * 0xc2b2ae35;
		return val ^ (val >>> 16);
	}
}
