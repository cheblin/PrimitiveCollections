package org.unirail.collections;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;

/**
 * A lock-free, bounded object pool with false sharing prevention.
 */
public final class Pool< T > {
	
	private final AtomicReferenceArray< T > items;
	private final int                       mask;
	private final Supplier< ? extends T  >             factory;
	
	// We use a structured padding approach because the JVM reorders fields within the same class.
	private final PaddedAtomicInteger readIndex  = new PaddedAtomicInteger();
	private final PaddedAtomicInteger writeIndex = new PaddedAtomicInteger();
	
	public Pool( int powerOf2, Supplier< ? extends T  > factory ) {
		if( powerOf2 < 1 || powerOf2 > 30 ) throw new IllegalArgumentException( "powerOf2 must be between 1 and 30" );
		if( factory == null ) throw new NullPointerException( "factory cannot be null" );
		
		int capacity = 1 << powerOf2;
		this.items   = new AtomicReferenceArray<>( capacity );
		this.mask    = capacity - 1;
		this.factory = factory;
	}
	
	public T acquire() {
		// Optimistic check
		if( readIndex.get() == writeIndex.get() ) return factory.get();
		
		while( true ) {
			int read  = readIndex.get();
			int write = writeIndex.get();
			
			// 1. Check Empty
			if( read == write ) return factory.get();
			
			// 2. Claim Read Cursor
			if( !readIndex.compareAndSet( read, read + 1 ) ) continue;
			
			// Atomically swap the item out. 
			// AtomicReferenceArray.getAndSet is the Java equivalent of Interlocked.Exchange
			T item = items.getAndSet( read & mask, null );
			
			return ( item != null ) ?
			       item :
			       factory.get();
		}
	}
	
	public void release( T item ) {
		if( item == null ) return;
		
		while( true ) {
			int write = writeIndex.get();
			int read  = readIndex.get();
			
			// 1. Check Full
			// Using unsigned comparison correctly handles index wrap-around (overflow)
			if( Integer.compareUnsigned( write - read, items.length() ) >= 0 ) return;
			
			// 2. Claim Write Cursor
			if( !writeIndex.compareAndSet( write, write + 1 ) ) continue;
			
			// Direct write. The read index's volatile access/CAS provides the necessary barriers.
			items.set( write & mask, item );
			return;
		}
	}
	
	public int capacity() {
		return items.length();
	}
	
	public int count() {
		// In two's complement, (write - read) is correct even if write < read due to overflow
		return Math.max( 0, writeIndex.get() - readIndex.get() );
	}
	
	/**
	 * Prevents False Sharing.
	 * We use a hierarchy to ensure the 'value' field is not reordered
	 * with the padding fields by the JVM.
	 */
	static class Padding {
		protected long p1, p2, p3, p4, p5, p6, p7, p8;
	}
	
	static class Value extends Padding {
		protected final AtomicInteger index = new AtomicInteger();
	}
	
	private static final class PaddedAtomicInteger extends Value {
		private long p9, p10, p11, p12, p13, p14, p15, p16;
		
		public int get() { return index.get(); }
		
		public boolean compareAndSet( int expect, int update ) {
			return index.compareAndSet( expect, update );
		}
	}
}