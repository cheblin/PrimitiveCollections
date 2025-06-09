package org.unirail; // Note the package change

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jol.info.GraphLayout;
import org.unirail.collections.*;
import oshi.SystemInfo;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State( Scope.Benchmark )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.NANOSECONDS )
@Warmup( iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS )
@Measurement( iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS )
@Fork( 1 )
//@Fork(0) // Uncomment for local debugging without forking JMH
public class vs_AndroidXBenchmarks { // Class name changed
	
	public static class Data {
		String type;
		int    dataSize; // Used for performance data and total memory size
		
		// Memory specific fields
		String[] memoryLabels = new String[ 2 ]; // Only two implementations per type
		int[][]  memoryData   = new int[ 2 ][ 20 ]; // Store memory usage for 20 points
		int      memoryIndex  = 0; // To track which memory implementation is being added
		
		// Performance specific fields
		long MapInsert = -1, AdHocInsert = -1;
		long MapSearch = -1, AdHocSearch = -1;
		long MapGet = -1, AdHocGet = -1;
		long MapDelete = -1, AdHocDelete = -1;
		
		public Data( String type, int dataSize ) {
			this.type     = type;
			this.dataSize = dataSize;
		}
		
		public void addMemoryFootprint( String label, int[] data ) {
			if( memoryIndex < 2 ) {
				this.memoryLabels[ memoryIndex ] = label;
				this.memoryData[ memoryIndex++ ] = data;
			}
		}
	}
	
	private static final Map< String, Data > reportDataMap = new LinkedHashMap<>();
	
	// --- Main Method (Orchestrator) ---
	public static void main( String[] args ) throws RunnerException, IOException {
		int defaultDataSize = 1000;
		int byteDataSize    = 255;
		
		// Initialize Data objects for each map type
		reportDataMap.put( "Byte", new Data( "Byte", byteDataSize ) );
		reportDataMap.put( "Short", new Data( "Short", defaultDataSize ) );
		reportDataMap.put( "Int", new Data( "Int", defaultDataSize ) );
		reportDataMap.put( "Long", new Data( "Long", defaultDataSize ) );
		reportDataMap.put( "Int-Boolean", new Data( "Int-Boolean", defaultDataSize ) );
		
		System.out.println( "--- Starting Memory Footprint Benchmarks ---" );
		measureAllMemoryMaps( defaultDataSize, byteDataSize );
		System.out.println( "--- Memory Footprint Benchmarks Complete ---" );
		
		System.out.println( "\n--- Starting Performance Benchmarks (JMH) ---" );
		Options opt = new OptionsBuilder()
				.include( vs_AndroidXBenchmarks.class.getSimpleName() + "\\.a.*" ) // Updated class name and prefix 'a'
				.build();
		
		Collection< RunResult > results = new Runner( opt ).run();
		processPerformanceResults( results );
		System.out.println( "--- Performance Benchmarks Complete ---" );
		
		String htmlReport = generateHtmlReport( new ArrayList<>( reportDataMap.values() ) );
		File   reportFile = new File( "vs_AndroidX_report.html" ); // Output file name
		try( FileWriter writer = new FileWriter( reportFile ) ) {
			writer.write( htmlReport );
			System.out.println( "\nHTML report generated to: " + reportFile.getAbsolutePath() );
			if( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported( Desktop.Action.BROWSE ) ) Desktop.getDesktop().browse( reportFile.toURI() );
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
	
	// --- Memory Benchmark Logic ---
	private static void measureAllMemoryMaps( int dataSize, int byteDataSize ) {
		System.out.println( "Measuring memory footprint for different Map implementations with up to " + dataSize + " elements, at intervals of ~5%.\n" );
		
		measureByteByteMap( byteDataSize );
		measureShortShortMap( dataSize );
		measureIntegerIntegerMap( dataSize );
		measureLongLongMap( dataSize );
		measureIntBooleanMap( dataSize );
	}
	
	// Helper to calculate step size for ~20 points
	private static int calculateStepSize( int dataSize ) {
		return dataSize / ( 19 - ( 0 < dataSize % 18 ?
				1 :
				0 ) );
	}
	
	private static void measureByteByteMap( int dataSize ) {
		int   stepSize       = calculateStepSize( dataSize );
		int[] androidxMemory = new int[ 20 ];
		int[] adhocMemory    = new int[ 20 ];
		
		androidx.collection.MutableIntIntMap androidx = new androidx.collection.MutableIntIntMap();
		ByteByteMap.RW                       adhoc    = new ByteByteMap.RW( 0 );
		
		for( int i = 0, k = 0; i < dataSize; i++ ) {
			if( i % stepSize == 0 || i == dataSize - 1 ) {
				androidxMemory[ k ] = ( int ) GraphLayout.parseInstance( androidx ).totalSize();
				adhocMemory[ k++ ]  = ( int ) GraphLayout.parseInstance( adhoc ).totalSize();
			}
			
			byte v = ( byte ) i;
			androidx.put( v, v ); // MutableIntIntMap accepts int, so byte promotes to int
			adhoc.put( v, v );
		}
		Data data = reportDataMap.get( "Byte" );
		data.addMemoryFootprint( "MutableIntIntMap", androidxMemory );
		data.addMemoryFootprint( "ByteByteMap", adhocMemory );
	}
	
	private static void measureShortShortMap( int dataSize ) {
		int   stepSize       = calculateStepSize( dataSize );
		int[] androidxMemory = new int[ 20 ];
		int[] adhocMemory    = new int[ 20 ];
		
		androidx.collection.MutableIntIntMap androidx = new androidx.collection.MutableIntIntMap();
		ShortShortMap.RW                     adhoc    = new ShortShortMap.RW( 0 );
		
		for( int i = 0, k = 0; i < dataSize; i++ ) {
			if( i % stepSize == 0 || i == dataSize - 1 ) {
				androidxMemory[ k ] = ( int ) GraphLayout.parseInstance( androidx ).totalSize();
				adhocMemory[ k++ ]  = ( int ) GraphLayout.parseInstance( adhoc ).totalSize();
			}
			short v = ( short ) i;
			androidx.put( v, v ); // MutableIntIntMap accepts int, so short promotes to int
			adhoc.put( v, v );
		}
		Data data = reportDataMap.get( "Short" );
		data.addMemoryFootprint( "MutableIntIntMap", androidxMemory );
		data.addMemoryFootprint( "ShortShortMap", adhocMemory );
	}
	
	private static void measureIntegerIntegerMap( int dataSize ) {
		int   stepSize       = calculateStepSize( dataSize );
		int[] androidxMemory = new int[ 20 ];
		int[] adhocMemory    = new int[ 20 ];
		
		androidx.collection.MutableIntIntMap androidx = new androidx.collection.MutableIntIntMap();
		IntIntMap.RW                         adhoc    = new IntIntMap.RW( 0 );
		
		for( int i = 0, k = 0; i < dataSize; i++ ) {
			if( i % stepSize == 0 || i == dataSize - 1 ) {
				androidxMemory[ k ] = ( int ) GraphLayout.parseInstance( androidx ).totalSize();
				adhocMemory[ k++ ]  = ( int ) GraphLayout.parseInstance( adhoc ).totalSize();
			}
			int v = i;
			androidx.put( v, v );
			adhoc.put( v, v );
		}
		Data data = reportDataMap.get( "Int" );
		data.addMemoryFootprint( "MutableIntIntMap", androidxMemory );
		data.addMemoryFootprint( "IntIntMap", adhocMemory );
	}
	
	private static void measureLongLongMap( int dataSize ) {
		int   stepSize       = calculateStepSize( dataSize );
		int[] androidxMemory = new int[ 20 ];
		int[] adhocMemory    = new int[ 20 ];
		
		androidx.collection.MutableLongLongMap androidx = new androidx.collection.MutableLongLongMap();
		LongLongMap.RW                         adhoc    = new LongLongMap.RW( 0 );
		
		for( int i = 0, k = 0; i < dataSize; i++ ) {
			if( i % stepSize == 0 || i == dataSize - 1 ) {
				androidxMemory[ k ] = ( int ) GraphLayout.parseInstance( androidx ).totalSize();
				adhocMemory[ k++ ]  = ( int ) GraphLayout.parseInstance( adhoc ).totalSize();
			}
			long v = i;
			androidx.put( v, v );
			adhoc.put( v, v );
		}
		Data data = reportDataMap.get( "Long" );
		data.addMemoryFootprint( "MutableLongLongMap", androidxMemory );
		data.addMemoryFootprint( "LongLongMap", adhocMemory );
	}
	
	private static void measureIntBooleanMap( int dataSize ) {
		int   stepSize       = calculateStepSize( dataSize );
		int[] androidxMemory = new int[ 20 ];
		int[] adhocMemory    = new int[ 20 ];
		
		androidx.collection.MutableIntIntMap androidx = new androidx.collection.MutableIntIntMap();
		IntBitsMap.RW                        adhoc    = new IntBitsMap.RW( 2, 0, 0 ); // nullValue=2, bitsPerValue=2
		
		int TRUE  = 1;
		int FALSE = 0;
		
		for( int i = 0, k = 0; i < dataSize; i++ ) {
			if( i % stepSize == 0 || i == dataSize - 1 ) {
				androidxMemory[ k ] = ( int ) GraphLayout.parseInstance( androidx ).totalSize();
				adhocMemory[ k++ ]  = ( int ) GraphLayout.parseInstance( adhoc ).totalSize();
			}
			int key = i;
			androidx.put( key, i % 2 ); // AndroidX MutableIntIntMap uses int for boolean values (0 or 1)
			adhoc.put( key, i % 2 == 0 ?
					TRUE :
					FALSE );
		}
		Data data = reportDataMap.get( "Int-Boolean" );
		data.addMemoryFootprint( "MutableIntIntMap", androidxMemory );
		data.addMemoryFootprint( "IntBitsMap", adhocMemory );
	}
	
	// --- Performance Benchmark Logic (JMH integration) ---
	
	// JMH @Param fields - these are used by the JMH runner
	@Param( { "1000" } )
	private int dataSize;
	
	@Param( { "255" } )
	private int byteDataSize;
	
	private byte[]  byteKeys;
	private short[] shortKeys;
	private int[]   intKeys;
	private long[]  longKeys;
	
	@Setup( Level.Trial )
	public void setupBenchmarkKeys() {
		Random rand = new Random();
		byteKeys = new byte[ byteDataSize ];
		for( int i = 0; i < byteDataSize; i++ ) byteKeys[ i ] = ( byte ) i;
		for( int i = byteKeys.length - 1; i > 0; i-- ) {//shuffle
			int  j    = rand.nextInt( i + 1 );
			byte temp = byteKeys[ i ];
			byteKeys[ i ] = byteKeys[ j ];
			byteKeys[ j ] = temp;
		}
		
		shortKeys = new short[ dataSize ];
		for( int i = 0; i < dataSize; i++ ) shortKeys[ i ] = ( short ) i;
		for( int i = shortKeys.length - 1; i > 0; i-- ) {//shuffle
			int   j    = rand.nextInt( i + 1 );
			short temp = shortKeys[ i ];
			shortKeys[ i ] = shortKeys[ j ];
			shortKeys[ j ] = temp;
		}
		
		intKeys = new int[ dataSize ];
		for( int i = 0; i < dataSize; i++ ) intKeys[ i ] = i;
		for( int i = intKeys.length - 1; i > 0; i-- ) {//shuffle
			int j    = rand.nextInt( i + 1 );
			int temp = intKeys[ i ];
			intKeys[ i ] = intKeys[ j ];
			intKeys[ j ] = temp;
		}
		
		longKeys = new long[ dataSize ];
		for( int i = 0; i < dataSize; i++ ) longKeys[ i ] = i;
		for( int i = longKeys.length - 1; i > 0; i-- ) {//shuffle
			int  j    = rand.nextInt( i + 1 );
			long temp = longKeys[ i ];
			longKeys[ i ] = longKeys[ j ];
			longKeys[ j ] = temp;
		}
	}
	
	@State( Scope.Thread )
	public static class MapState {
		androidx.collection.MutableIntIntMap empty_byteMap;
		ByteByteMap.RW                       empty_byteAdHoc;
		
		androidx.collection.MutableIntIntMap empty_shortMap;
		ShortShortMap.RW                     empty_shortAdHoc;
		
		androidx.collection.MutableIntIntMap empty_intMap;
		IntIntMap.RW                         empty_intAdHoc;
		
		androidx.collection.MutableLongLongMap empty_longMap;
		LongLongMap.RW                         empty_longAdHoc;
		
		androidx.collection.MutableIntIntMap empty_intBoolMap;
		IntBitsMap.RW                        empty_intBoolAdHoc;
		
		androidx.collection.MutableIntIntMap byteMap;
		ByteByteMap.RW                       byteAdHoc;
		
		androidx.collection.MutableIntIntMap shortMap;
		ShortShortMap.RW                     shortAdHoc;
		
		androidx.collection.MutableIntIntMap intMap;
		IntIntMap.RW                         intAdHoc;
		
		androidx.collection.MutableLongLongMap longMap;
		LongLongMap.RW                         longAdHoc;
		
		androidx.collection.MutableIntIntMap intBoolMap;
		IntBitsMap.RW                        intBoolAdHoc;
		
		byte[]  byteKeys;
		short[] shortKeys;
		int[]   intKeys;
		long[]  longKeys;
		
		@Setup( Level.Trial )
		public void setupTrial( vs_AndroidXBenchmarks benchmark ) { // Updated parameter type
			this.byteKeys  = benchmark.byteKeys;
			this.shortKeys = benchmark.shortKeys;
			this.intKeys   = benchmark.intKeys;
			this.longKeys  = benchmark.longKeys;
		}
		
		@Setup( Level.Invocation )
		public void setupIteration() {
			empty_byteMap   = new androidx.collection.MutableIntIntMap();
			empty_byteAdHoc = new ByteByteMap.RW( 0 );
			
			empty_shortMap   = new androidx.collection.MutableIntIntMap();
			empty_shortAdHoc = new ShortShortMap.RW( 0 );
			
			empty_intMap   = new androidx.collection.MutableIntIntMap();
			empty_intAdHoc = new IntIntMap.RW( 0 );
			
			empty_longMap   = new androidx.collection.MutableLongLongMap();
			empty_longAdHoc = new LongLongMap.RW( 0 );
			
			empty_intBoolMap   = new androidx.collection.MutableIntIntMap();
			empty_intBoolAdHoc = new IntBitsMap.RW( 2, 0, 0 );
			
			byteMap   = new androidx.collection.MutableIntIntMap();
			byteAdHoc = new ByteByteMap.RW( 0 );
			
			shortMap   = new androidx.collection.MutableIntIntMap();
			shortAdHoc = new ShortShortMap.RW( 0 );
			
			intMap   = new androidx.collection.MutableIntIntMap();
			intAdHoc = new IntIntMap.RW( 0 );
			
			longMap   = new androidx.collection.MutableLongLongMap();
			longAdHoc = new LongLongMap.RW( 0 );
			
			intBoolMap   = new androidx.collection.MutableIntIntMap();
			intBoolAdHoc = new IntBitsMap.RW( 2, 0, 0 );
			
			for( byte key : byteKeys ) byteMap.put( key, key );
			for( byte key : byteKeys ) byteAdHoc.put( key, key );
			
			for( short key : shortKeys ) shortMap.put( key, key );
			for( short key : shortKeys ) shortAdHoc.put( key, key );
			
			for( int key : intKeys ) intMap.put( key, key );
			for( int key : intKeys ) intAdHoc.put( key, key );
			
			for( long key : longKeys ) longMap.put( key, key );
			for( long key : longKeys ) longAdHoc.put( key, key );
			
			for( int key : intKeys ) intBoolMap.put( key, key % 2 );
			for( int key : intKeys )
				intBoolAdHoc.put( key, key % 2 == 0 ?
						1 :
						0 );
		}
	}
	
	// Static fields for JMH blackhole to prevent dead code elimination
	static byte    by; // AdHoc Byte result
	static short   s;  // AdHoc Short result
	static int     i;  // AndroidX Map results for Byte, Short, Int, Int-Bool, AdHoc Int result
	static long    l;  // Long results
	static boolean b;  // Search result / AdHoc Int-Bool result
	
	// --- Performance Benchmarks (JMH methods) ---
	// Byte Benchmarks
	@Benchmark public void aByte_Map_Insert( MapState state ) { for( byte key : state.byteKeys ) state.empty_byteMap.put( key, key ); }
	
	@Benchmark public void aByte_Map_Search( MapState state )   { for( byte key : state.byteKeys ) b = state.byteMap.containsKey( key ); }
	
	@Benchmark public void aByte_Map_Get( MapState state )      { for( byte key : state.byteKeys ) if( state.byteMap.containsKey( key ) ) i = state.byteMap.get( key ); } // AndroidX returns int
	
	@Benchmark public void aByte_Map_Delete( MapState state )   { for( byte key : state.byteKeys ) state.byteMap.remove( key ); }
	
	@Benchmark public void aByte_AdHoc_Insert( MapState state ) { for( byte key : state.byteKeys ) state.empty_byteAdHoc.put( key, key ); }
	
	@Benchmark public void aByte_AdHoc_Search( MapState state ) { for( byte key : state.byteKeys ) b = state.byteAdHoc.containsKey( key ); }
	
	@Benchmark public void aByte_AdHoc_Get( MapState state )    { for( byte key : state.byteKeys ) { long token = state.byteAdHoc.tokenOf( key ); if( token != -1 ) by = state.byteAdHoc.value( token ); } }
	
	@Benchmark public void aByte_AdHoc_Delete( MapState state ) { for( byte key : state.byteKeys ) state.byteAdHoc.remove( key ); }
	
	// Short Benchmarks
	@Benchmark public void aShort_Map_Insert( MapState state ) { for( short key : state.shortKeys ) state.empty_shortMap.put( key, key ); }
	
	@Benchmark public void aShort_Map_Search( MapState state )   { for( short key : state.shortKeys ) b = state.shortMap.containsKey( key ); }
	
	@Benchmark public void aShort_Map_Get( MapState state )      { for( short key : state.shortKeys ) if( state.shortMap.containsKey( key ) ) i = state.shortMap.get( key ); } // AndroidX returns int
	
	@Benchmark public void aShort_Map_Delete( MapState state )   { for( short key : state.shortKeys ) state.shortMap.remove( key ); }
	
	@Benchmark public void aShort_AdHoc_Insert( MapState state ) { for( short key : state.shortKeys ) state.empty_shortAdHoc.put( key, key ); }
	
	@Benchmark public void aShort_AdHoc_Search( MapState state ) { for( short key : state.shortKeys ) b = state.shortAdHoc.containsKey( key ); }
	
	@Benchmark public void aShort_AdHoc_Get( MapState state )    { for( short key : state.shortKeys ) { long token = state.shortAdHoc.tokenOf( key ); if( token != -1 ) s = state.shortAdHoc.value( token ); } }
	
	@Benchmark public void aShort_AdHoc_Delete( MapState state ) { for( short key : state.shortKeys ) state.shortAdHoc.remove( key ); }
	
	// Int Benchmarks
	@Benchmark public void aInt_Map_Insert( MapState state ) { for( int key : state.intKeys ) state.empty_intMap.put( key, key ); }
	
	@Benchmark public void aInt_Map_Search( MapState state )   { for( int key : state.intKeys ) b = state.intMap.containsKey( key ); }
	
	@Benchmark public void aInt_Map_Get( MapState state )      { for( int key : state.intKeys ) if( state.intMap.containsKey( key ) ) i = state.intMap.get( key ); }
	
	@Benchmark public void aInt_Map_Delete( MapState state )   { for( int key : state.intKeys ) state.intMap.remove( key ); }
	
	@Benchmark public void aInt_AdHoc_Insert( MapState state ) { for( int key : state.intKeys ) state.empty_intAdHoc.put( key, key ); }
	
	@Benchmark public void aInt_AdHoc_Search( MapState state ) { for( int key : state.intKeys ) b = state.intAdHoc.containsKey( key ); }
	
	@Benchmark public void aInt_AdHoc_Get( MapState state )    { for( int key : state.intKeys ) { long token = state.intAdHoc.tokenOf( key ); if( token != -1 ) i = state.intAdHoc.value( token ); } }
	
	@Benchmark public void aInt_AdHoc_Delete( MapState state ) { for( int key : state.intKeys ) state.intAdHoc.remove( key ); }
	
	// Long Benchmarks
	@Benchmark public void aLong_Map_Insert( MapState state ) { for( long key : state.longKeys ) state.empty_longMap.put( key, key ); }
	
	@Benchmark public void aLong_Map_Search( MapState state )   { for( long key : state.longKeys ) b = state.longMap.containsKey( key ); }
	
	@Benchmark public void aLong_Map_Get( MapState state )      { for( long key : state.longKeys ) if( state.longMap.containsKey( key ) ) l = state.longMap.get( key ); }
	
	@Benchmark public void aLong_Map_Delete( MapState state )   { for( long key : state.longKeys ) state.longMap.remove( key ); }
	
	@Benchmark public void aLong_AdHoc_Insert( MapState state ) { for( long key : state.longKeys ) state.empty_longAdHoc.put( key, key ); }
	
	@Benchmark public void aLong_AdHoc_Search( MapState state ) { for( long key : state.longKeys ) b = state.longAdHoc.containsKey( key ); }
	
	@Benchmark public void aLong_AdHoc_Get( MapState state )    { for( long key : state.longKeys ) { long token = state.longAdHoc.tokenOf( key ); if( token != -1 ) l = state.longAdHoc.value( token ); } }
	
	@Benchmark public void aLong_AdHoc_Delete( MapState state ) { for( long key : state.longKeys ) state.longAdHoc.remove( key ); }
	
	// Int-Boolean Benchmarks
	@Benchmark public void aIntBool_Map_Insert( MapState state ) { for( int key : state.intKeys ) state.empty_intBoolMap.put( key, key % 2 ); }
	
	@Benchmark public void aIntBool_Map_Search( MapState state )   { for( int key : state.intKeys ) b = state.intBoolMap.containsKey( key ); }
	
	@Benchmark public void aIntBool_Map_Get( MapState state )      { for( int key : state.intKeys ) if( state.intBoolMap.containsKey( key ) ) i = state.intBoolMap.get( key ); } // AndroidX returns int
	
	@Benchmark public void aIntBool_Map_Delete( MapState state )   { for( int key : state.intKeys ) state.intBoolMap.remove( key ); }
	
	@Benchmark public void aIntBool_AdHoc_Insert( MapState state ) {
		                                                               for( int key : state.intKeys )
			                                                               state.empty_intBoolAdHoc.put( key, key % 2 == 0 ?
					                                                               1 :
					                                                               0 );
	                                                               }
	
	@Benchmark public void aIntBool_AdHoc_Search( MapState state ) { for( int key : state.intKeys ) b = state.intBoolAdHoc.containsKey( key ); }
	
	@Benchmark public void aIntBool_AdHoc_Get( MapState state )    { for( int key : state.intKeys ) { long token = state.intBoolAdHoc.tokenOf( key ); if( token != -1 ) b = state.intBoolAdHoc.value( token ) == 1; } }
	
	@Benchmark public void aIntBool_AdHoc_Delete( MapState state ) { for( int key : state.intKeys ) state.intBoolAdHoc.remove( key ); }
	
	// --- Performance Results Processing ---
	private static void processPerformanceResults( Collection< RunResult > results ) {
		for( RunResult result : results ) {
			String benchmarkName = result.getParams().getBenchmark();
			String methodName    = benchmarkName.substring( benchmarkName.lastIndexOf( '.' ) + 1 );
			long   scoreNs       = ( long ) result.getPrimaryResult().getScore();
			
			String[] parts = methodName.split( "_" );
			if( parts.length != 3 ) {
				System.err.println( "Skipping invalid benchmark name: " + methodName );
				continue;
			}
			
			String typeStr = parts[ 0 ];
			// Map JMH type string to Data type string (remove 'a' prefix)
			String type = switch( typeStr ) {
				case "aByte" -> "Byte";
				case "aShort" -> "Short";
				case "aInt" -> "Int";
				case "aLong" -> "Long";
				case "aIntBool" -> "Int-Boolean";
				default -> {
					System.err.println( "Unknown type string: " + typeStr );
					yield null;
				}
			};
			if( type == null ) continue;
			
			String structure = parts[ 1 ];
			String operation = parts[ 2 ];
			
			Data data = reportDataMap.get( type );
			if( data == null ) {
				System.err.println( "No Data entry for type: " + type );
				continue;
			}
			
			if( structure.equals( "Map" ) ) { // AndroidX maps
				switch( operation ) {
					case "Insert":
						data.MapInsert = scoreNs; break;
					case "Search":
						data.MapSearch = scoreNs; break;
					case "Delete":
						data.MapDelete = scoreNs; break;
					case "Get":
						data.MapGet = scoreNs; break;
				}
			}
			else if( structure.equals( "AdHoc" ) ) {
				switch( operation ) {
					case "Insert":
						data.AdHocInsert = scoreNs; break;
					case "Search":
						data.AdHocSearch = scoreNs; break;
					case "Delete":
						data.AdHocDelete = scoreNs; break;
					case "Get":
						data.AdHocGet = scoreNs; break;
				}
			}
		}
	}
	
	// --- HTML Report Generation ---
	public static String generateHtmlReport( List< Data > dataList ) {
		StringBuilder htmlBuilder = new StringBuilder();
		
		htmlBuilder.append( "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" )
		           .append( "<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>vs AndroidX Benchmarks Report</title>\n" )
		           .append( "<style>\n" )
		           .append( "body { background-color: #1e1e1e; color: #f8f8f2; font-family: monospace; line-height: 1.6; margin: 0; display: flex; justify-content: center; min-height: 100vh; padding: 20px 0; box-sizing: border-box; }\n" )
		           .append( ".report-body-wrapper { width: 95vw; max-width: 1200px; margin: 0 auto; box-sizing: border-box; }\n" )
		           .append( "header { text-align: center; margin-bottom: 20px; }\n" )
		           .append( "h1 { color: #bd93f9; margin-bottom: 5px; }\n" )
		           .append( "p { color: #f8f8f2; margin: 0 auto 20px auto; text-align: center; }\n" )
		           .append( ".report-section { display: flex; flex-direction: column; gap: 40px; margin-bottom: 40px; }\n" )
		           .append( ".chart-pair-container { display: flex; flex-wrap: wrap; justify-content: center; gap: 20px; width: 100%; margin: 0 auto; }\n" )
		           .append( ".chart-wrapper { flex: 1 1 calc(50% - 10px); min-width: 300px; background: #282a36; padding: 15px; border-radius: 5px; box-sizing: border-box; }\n" )
		           .append( ".chart-wrapper h2 { color: #50fa7b; text-align: center; margin-top: 0; }\n" )
		           .append( ".canvas-container { position: relative; width: 100%; }\n" )
		           .append( "table { width: 100%; border-collapse: collapse; margin-top: 30px; background-color: #282a36; display: none; /* Hide tables, rely on charts */ }\n" )
		           .append( "th, td { border: 1px solid #44475a; padding: 10px; text-align: left; }\n" )
		           .append( "th { background-color: #44475a; color: #f8f8f2; font-weight: bold; }\n" )
		           .append( ".androidx-label { color: #ff79c6; }\n" ) // AndroidX color
		           .append( ".adhoc-label { color: #8be9fd; }\n" )
		           .append( "</style>\n" )
		           .append( "<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n" )
		           .append( "</head>\n<body>\n" )
		           .append( "<div class=\"report-body-wrapper\">\n" ) // The main resizable container
		           .append( "<header>\n" )
		           .append( "<h1>vs AndroidX Benchmarks Report</h1>\n" ) // Report title
		           .append( "<p>CPU: " ).append( new SystemInfo().getHardware().getProcessor().getProcessorIdentifier().getName().trim() )
		           .append( ", Java VM: " ).append( System.getProperty( "java.vm.vendor" ) + " " + System.getProperty( "java.vm.version" ) )
		           .append( "</p>\n" )
		           .append( "<p>This report compares memory usage (in bytes) and performance (ns/op) between " )
		           .append( "<a href=\"https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:collection/collection/src/commonMain/kotlin/androidx/collection/\">Maps from AndroidX Collection</a> and AdHoc maps." ) // AndroidX link and description
		           .append( " Memory measurements show growth at 20 intervals (from 5% to 100% fill). " )
		           .append( "Performance measurements show average time for Insert, Search, Get, and Delete operations." )
		           .append( "<br><strong>Lower values indicate better efficiency.</strong></p>\n" )
		           .append( "</header>\n" );
		
		
		htmlBuilder.append( "<main class=\"report-section\">\n" );
		
		for( Data data : dataList ) {
			String typeLowercase = data.type.toLowerCase().replace( " ", "-" );
			
			// Determine specific map labels for chart legends
			String androidxLabel;
			String adhocLabel;
			switch( data.type ) {
				case "Byte":
					androidxLabel = "MutableIntIntMap"; adhocLabel = "ByteByteMap"; break;
				case "Short":
					androidxLabel = "MutableIntIntMap"; adhocLabel = "ShortShortMap"; break;
				case "Int":
					androidxLabel = "MutableIntIntMap"; adhocLabel = "IntIntMap"; break;
				case "Long":
					androidxLabel = "MutableLongLongMap"; adhocLabel = "LongLongMap"; break;
				case "Int-Boolean":
					androidxLabel = "MutableIntIntMap"; adhocLabel = "IntBitsMap"; break;
				default:
					androidxLabel = "AndroidX Map"; adhocLabel = "AdHoc Map"; break;
			}
			
			htmlBuilder.append( "<div class=\"chart-pair-container\">\n" )
			           // Performance Chart (Left)
			           .append( "<div class=\"chart-wrapper\">\n" )
			           .append( "<h2>" ).append( data.type ).append( " Performance</h2>\n" )
			           .append( "<div class=\"canvas-container\"><canvas id=\"perf-chart-" ).append( typeLowercase ).append( "\"></canvas></div>\n" )
			           .append( "</div>\n" )
			           // Memory Chart (Right)
			           .append( "<div class=\"chart-wrapper\">\n" )
			           .append( "<h2>" ).append( data.type ).append( " Memory Growth</h2>\n" )
			           .append( "<div class=\"canvas-container\"><canvas id=\"mem-chart-" ).append( typeLowercase ).append( "\"></canvas></div>\n" )
			           .append( "</div>\n" )
			           .append( "</div>\n" );
		}
		htmlBuilder.append( "</main>\n" );
		
		htmlBuilder.append( "<script>\n" );
		for( Data data : dataList ) {
			String typeLowercase = data.type.toLowerCase().replace( " ", "-" );
			String perfChartId   = "perf-chart-" + typeLowercase;
			String memChartId    = "mem-chart-" + typeLowercase;
			
			// Determine specific map labels for chart legends (re-defining for clarity in JS)
			String androidxLabel;
			String adhocLabel;
			switch( data.type ) {
				case "Byte":
					androidxLabel = "MutableIntIntMap"; adhocLabel = "ByteByteMap"; break;
				case "Short":
					androidxLabel = "MutableIntIntMap"; adhocLabel = "ShortShortMap"; break;
				case "Int":
					androidxLabel = "MutableIntIntMap"; adhocLabel = "IntIntMap"; break;
				case "Long":
					androidxLabel = "MutableLongLongMap"; adhocLabel = "LongLongMap"; break;
				case "Int-Boolean":
					androidxLabel = "MutableIntIntMap"; adhocLabel = "IntBitsMap"; break;
				default:
					androidxLabel = "AndroidX Map"; adhocLabel = "AdHoc Map"; break;
			}
			
			// --- Performance Chart Script ---
			htmlBuilder.append( "new Chart(document.getElementById('" ).append( perfChartId ).append( "').getContext('2d'), {\n" )
			           .append( "type: 'bar',\n" )
			           .append( "data: {\n" )
			           .append( "labels: ['Insert', 'Search', 'Get', 'Delete'],\n" )
			           .append( "datasets: [{\n" )
			           .append( "label: '" ).append( androidxLabel ).append( "',\n" )
			           .append( "data: [" ).append( data.MapInsert ).append( ", " ).append( data.MapSearch ).append( ", " )
			           .append( data.MapGet ).append( ", " ).append( data.MapDelete ).append( "],\n" )
			           .append( "backgroundColor: '#ff79c6',\n" )
			           .append( "borderColor: '#ff79c6',\n" )
			           .append( "borderWidth: 1\n" )
			           .append( "}, {\n" )
			           .append( "label: '" ).append( adhocLabel ).append( "',\n" )
			           .append( "data: [" ).append( data.AdHocInsert ).append( ", " ).append( data.AdHocSearch ).append( ", " )
			           .append( data.AdHocGet ).append( ", " ).append( data.AdHocDelete ).append( "],\n" )
			           .append( "backgroundColor: '#8be9fd',\n" )
			           .append( "borderColor: '#8be9fd',\n" )
			           .append( "borderWidth: 1\n" )
			           .append( "}]\n" )
			           .append( "},\n" )
			           .append( "options: {\n" )
			           .append( "responsive: true,\n" )
			           .append( "maintainAspectRatio: true, /* Chart.js default: let it manage height based on width */\n" )
			           .append( "scales: {\n" )
			           .append( "y: { beginAtZero: true, title: { display: true, text: 'Time (ns/op) - Lower is Better', color: '#f8f8f2' }, ticks: { color: '#f8f8f2' }, grid: { color: '#44475a' } },\n" )
			           .append( "x: { ticks: { color: '#f8f8f2' }, grid: { color: '#44475a' } }\n" )
			           .append( "},\n" )
			           .append( "plugins: {\n" )
			           .append( "legend: { labels: { color: '#f8f8f2' } },\n" )
			           .append( "title: { display: false }\n" ) // Title handled by h2 in wrapper
			           .append( "}\n" )
			           .append( "}\n" )
			           .append( "});\n" );
			
			// --- Memory Chart Script ---
			StringBuilder labels = new StringBuilder( "[" );
			for( int k = 0; k < 20; k++ ) {
				labels.append( "'" ).append( ( k + 1 ) * 5 ).append( "%'" );
				if( k < 19 ) labels.append( "," );
			}
			labels.append( "]" );
			
			htmlBuilder.append( "new Chart(document.getElementById('" ).append( memChartId ).append( "').getContext('2d'), {\n" )
			           .append( "type: 'line',\n" )
			           .append( "data: {\n" )
			           .append( "labels: " ).append( labels ).append( ",\n" )
			           .append( "datasets: [{\n" )
			           .append( "label: '" ).append( androidxLabel ).append( "',\n" )
			           .append( "data: [" ).append( Arrays.toString( data.memoryData[ 0 ] ).replaceAll( "[\\[\\]]", "" ) ).append( "],\n" )
			           .append( "borderColor: '#ff79c6',\n" )
			           .append( "backgroundColor: '#ff79c6',\n" )
			           .append( "fill: false,\n" )
			           .append( "borderWidth: 2\n" )
			           .append( "}, {\n" )
			           .append( "label: '" ).append( adhocLabel ).append( "',\n" )
			           .append( "data: [" ).append( Arrays.toString( data.memoryData[ 1 ] ).replaceAll( "[\\[\\]]", "" ) ).append( "],\n" )
			           .append( "borderColor: '#8be9fd',\n" )
			           .append( "backgroundColor: '#8be9fd',\n" )
			           .append( "fill: false,\n" )
			           .append( "borderWidth: 2\n" )
			           .append( "}]\n" )
			           .append( "},\n" )
			           .append( "options: {\n" )
			           .append( "responsive: true,\n" )
			           .append( "maintainAspectRatio: true, /* Chart.js default: let it manage height based on width */\n" )
			           .append( "scales: {\n" )
			           .append( "y: { beginAtZero: true, title: { display: true, text: 'Memory (bytes) - Lower is Better', color: '#f8f8f2' }, ticks: { color: '#f8f8f2' }, grid: { color: '#44475a' } },\n" )
			           .append( "x: { title: { display: true, text: 'Fill Percentage', color: '#f8f8f2' }, ticks: { color: '#f8f8f2' }, grid: { color: '#44475a' } }\n" )
			           .append( "},\n" )
			           .append( "plugins: {\n" )
			           .append( "legend: { labels: { color: '#f8f8f2' } },\n" )
			           .append( "title: { display: false }\n" ) // Title handled by h2 in wrapper
			           .append( "}\n" )
			           .append( "}\n" )
			           .append( "});\n" );
		}
		htmlBuilder.append( "</script>\n" )
		           .append( "</div>\n" ) // Close .report-body-wrapper
		           .append( "</body>\n</html>" );
		
		return htmlBuilder.toString();
	}
}