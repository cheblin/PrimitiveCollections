package org.unirail.test;

import org.openjdk.jol.info.GraphLayout;
import org.unirail.collections.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MemoryBenchmarks {
	
	static List< Data > reportDataList = new ArrayList<>();
	
	public static void main( String[] args ) {
		int dataSize = 1000; // Number of elements to add to each map
		
		System.out.println( "Measuring memory footprint for different Map implementations with " + dataSize + " elements.\n" );
		
		measureByteByteMap( 255 ); // Reduced dataSize for Byte to 255
		measureShortShortMap( dataSize );
		measureIntegerIntegerMap( dataSize );
		measureLongLongMap( dataSize );
		measureIntBooleanMap( dataSize );
		
		String htmlReport = generateHtmlReport( reportDataList );
		File   reportFile = new File( "memory_report.html" );
		try( FileWriter writer = new FileWriter( reportFile ) ) {
			writer.write( htmlReport );
			System.out.println( "HTML report generated to: " + reportFile.getAbsolutePath() );
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
	
	public static class Data {
		String   type;
		String[] labels = new String[ 6 ]; // Fixed size: 6 entries (3 scenarios × 2 maps)
		int[]    data   = new int[ 6 ];
		int      index  = 0;
		
		public Data( String type ) {
			this.type = type;
			reportDataList.add( this );
		}
		
		public void put( String label, int data ) {
			if( index < 6 ) { // Prevent overflow
				this.labels[ index ] = label;
				this.data[ index ]   = data;
				index++;
			}
		}
	}
	
	private static void measureByteByteMap( int dataSize ) {
		Map< Byte, Byte >  standard          = new HashMap<>();
		Map< Byte, Byte >  standard50        = new HashMap<>();
		Map< Byte, Byte >  standard100       = new HashMap<>();
		ByteByteNullMap.RW nullable_value    = new ByteByteNullMap.RW( 0 );
		ByteByteNullMap.RW nullable_value50  = new ByteByteNullMap.RW( 0 );
		ByteByteNullMap.RW nullable_value100 = new ByteByteNullMap.RW( 0 );
		
		for( int i = 0; i < 255; i++ ) {
			byte v = ( byte ) i;
			standard.put( v, v );
			nullable_value.put( v, v );
			if( i < dataSize / 2 ) {
				standard50.put( v, v );
				nullable_value50.put( v, v );
			}
			else {
				standard50.put( v, null );
				nullable_value50.put( v, null );
			}
			standard100.put( v, null );
			nullable_value100.put( v, null );
		}
		
		Data report = new Data( "Byte" );
		report.put( "Map<Byte, Byte> no nulls", ( int ) GraphLayout.parseInstance( standard ).totalSize() );
		report.put( "ByteByteNullMap no nulls", ( int ) GraphLayout.parseInstance( nullable_value ).totalSize() );
		report.put( "Map<Byte, Byte> half nulls", ( int ) GraphLayout.parseInstance( standard50 ).totalSize() );
		report.put( "ByteByteNullMap half nulls", ( int ) GraphLayout.parseInstance( nullable_value50 ).totalSize() );
		report.put( "Map<Byte, Byte> all nulls", ( int ) GraphLayout.parseInstance( standard100 ).totalSize() );
		report.put( "ByteByteNullMap all nulls", ( int ) GraphLayout.parseInstance( nullable_value100 ).totalSize() );
	}
	
	private static void measureShortShortMap( int dataSize ) {
		Map< Short, Short >  standard          = new HashMap<>();
		Map< Short, Short >  standard50        = new HashMap<>();
		Map< Short, Short >  standard100       = new HashMap<>();
		ShortShortNullMap.RW nullable_value    = new ShortShortNullMap.RW( 0 );
		ShortShortNullMap.RW nullable_value50  = new ShortShortNullMap.RW( 0 );
		ShortShortNullMap.RW nullable_value100 = new ShortShortNullMap.RW( 0 );
		
		for( int i = 0; i < dataSize; i++ ) {
			short v = ( short ) i;
			standard.put( v, v );
			nullable_value.put( v, v );
			if( i < dataSize / 2 ) {
				standard50.put( v, v );
				nullable_value50.put( v, v );
			}
			else {
				standard50.put( v, null );
				nullable_value50.put( v, null );
			}
			standard100.put( v, null );
			nullable_value100.put( v, null );
		}
		
		Data report = new Data( "Short" );
		report.put( "Map<Short, Short> no nulls", ( int ) GraphLayout.parseInstance( standard ).totalSize() );
		report.put( "ShortShortNullMap no nulls", ( int ) GraphLayout.parseInstance( nullable_value ).totalSize() );
		report.put( "Map<Short, Short> half nulls", ( int ) GraphLayout.parseInstance( standard50 ).totalSize() );
		report.put( "ShortShortNullMap half nulls", ( int ) GraphLayout.parseInstance( nullable_value50 ).totalSize() );
		report.put( "Map<Short, Short> all nulls", ( int ) GraphLayout.parseInstance( standard100 ).totalSize() );
		report.put( "ShortShortNullMap all nulls", ( int ) GraphLayout.parseInstance( nullable_value100 ).totalSize() );
	}
	
	private static void measureIntegerIntegerMap( int dataSize ) {
		Map< Integer, Integer > standard          = new HashMap<>();
		Map< Integer, Integer > standard50        = new HashMap<>();
		Map< Integer, Integer > standard100       = new HashMap<>();
		IntIntNullMap.RW        nullable_value    = new IntIntNullMap.RW( 0 );
		IntIntNullMap.RW        nullable_value50  = new IntIntNullMap.RW( 0 );
		IntIntNullMap.RW        nullable_value100 = new IntIntNullMap.RW( 0 );
		
		for( int i = 0; i < dataSize; i++ ) {
			int v = i;
			standard.put( v, v );
			nullable_value.put( v, v );
			if( i < dataSize / 2 ) {
				standard50.put( v, v );
				nullable_value50.put( v, v );
			}
			else {
				standard50.put( v, null );
				nullable_value50.put( v, null );
			}
			standard100.put( v, null );
			nullable_value100.put( v, null );
		}
		
		Data report = new Data( "Int" );
		report.put( "Map<Int, Int> no nulls", ( int ) GraphLayout.parseInstance( standard ).totalSize() );
		report.put( "IntIntNullMap no nulls", ( int ) GraphLayout.parseInstance( nullable_value ).totalSize() );
		report.put( "Map<Int, Int> half nulls", ( int ) GraphLayout.parseInstance( standard50 ).totalSize() );
		report.put( "IntIntNullMap half nulls", ( int ) GraphLayout.parseInstance( nullable_value50 ).totalSize() );
		report.put( "Map<Int, Int> all nulls", ( int ) GraphLayout.parseInstance( standard100 ).totalSize() );
		report.put( "IntIntNullMap all nulls", ( int ) GraphLayout.parseInstance( nullable_value100 ).totalSize() );
	}
	
	private static void measureLongLongMap( int dataSize ) {
		Map< Long, Long >  standard          = new HashMap<>();
		Map< Long, Long >  standard50        = new HashMap<>();
		Map< Long, Long >  standard100       = new HashMap<>();
		LongLongNullMap.RW nullable_value    = new LongLongNullMap.RW( 0 );
		LongLongNullMap.RW nullable_value50  = new LongLongNullMap.RW( 0 );
		LongLongNullMap.RW nullable_value100 = new LongLongNullMap.RW( 0 );
		
		for( int i = 0; i < dataSize; i++ ) {
			long v = i;
			standard.put( v, v );
			nullable_value.put( v, v );
			if( i < dataSize / 2 ) {
				standard50.put( v, v );
				nullable_value50.put( v, v );
			}
			else {
				standard50.put( v, null );
				nullable_value50.put( v, null );
			}
			standard100.put( v, null );
			nullable_value100.put( v, null );
		}
		
		Data report = new Data( "Long" );
		report.put( "Map<Long, Long> no nulls", ( int ) GraphLayout.parseInstance( standard ).totalSize() );
		report.put( "LongLongNullMap no nulls", ( int ) GraphLayout.parseInstance( nullable_value ).totalSize() );
		report.put( "Map<Long, Long> half nulls", ( int ) GraphLayout.parseInstance( standard50 ).totalSize() );
		report.put( "LongLongNullMap half nulls", ( int ) GraphLayout.parseInstance( nullable_value50 ).totalSize() );
		report.put( "Map<Long, Long> all nulls", ( int ) GraphLayout.parseInstance( standard100 ).totalSize() );
		report.put( "LongLongNullMap all nulls", ( int ) GraphLayout.parseInstance( nullable_value100 ).totalSize() );
	}
	
	private static void measureIntBooleanMap( int dataSize ) {
		Map< Integer, Boolean > standard          = new HashMap<>();
		Map< Integer, Boolean > standard50        = new HashMap<>();
		Map< Integer, Boolean > standard100       = new HashMap<>();
		IntBitsMap.RW           nullable_value    = new IntBitsMap.RW( 0, 2, 2 );
		IntBitsMap.RW           nullable_value50  = new IntBitsMap.RW( 0, 2, 2 );
		IntBitsMap.RW           nullable_value100 = new IntBitsMap.RW( 0, 2, 2 );
		
		int NULL  = 2;
		int TRUE  = 1;
		int FALSE = 0;
		
		for( int i = 0; i < dataSize; i++ ) {
			int key = i;
			standard.put( key, i % 2 == 0 );
			nullable_value.put( key, i % 2 == 0 ?
					TRUE :
					FALSE );
			
			if( i < dataSize / 2 ) {
				standard50.put( key, i % 2 == 0 );
				nullable_value50.put( key, i % 2 == 0 ?
						TRUE :
						FALSE );
			}
			else {
				standard50.put( key, null );
				nullable_value50.put( key, NULL );
			}
			standard100.put( key, null );
			nullable_value100.put( key, NULL );
		}
		
		Data report = new Data( "Int-Boolean" );
		report.put( "Map<Int, Boolean> no nulls", ( int ) GraphLayout.parseInstance( standard ).totalSize() );
		report.put( "IntBitsMap no nulls", ( int ) GraphLayout.parseInstance( nullable_value ).totalSize() );
		report.put( "Map<Int, Boolean> half nulls", ( int ) GraphLayout.parseInstance( standard50 ).totalSize() );
		report.put( "IntBitsMap half nulls", ( int ) GraphLayout.parseInstance( nullable_value50 ).totalSize() );
		report.put( "Map<Int, Boolean> all nulls", ( int ) GraphLayout.parseInstance( standard100 ).totalSize() );
		report.put( "IntBitsMap all nulls", ( int ) GraphLayout.parseInstance( nullable_value100 ).totalSize() );
	}
	
	public static String generateHtmlReport( List< Data > dataList ) {
		StringBuilder htmlBuilder = new StringBuilder();
		
		// Enhanced HTML header with better styling
		htmlBuilder.append( "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n" )
		           .append( "<meta charset=\"UTF-8\">\n<title>Memory Footprint Report</title>\n" )
		           .append( "<style>\n" )
		           .append( "body { background-color: #1e1e1e; color: #f8f8f2; font-family: monospace; padding: 20px; line-height: 1.6; }\n" )
		           .append( ".chart-container { width: 80%; margin: 20px auto; background: #282a36; padding: 15px; border-radius: 5px; }\n" )
		           .append( "table { width: 100%; border-collapse: collapse; margin-top: 30px; background-color: #282a36; }\n" )
		           .append( "th, td { border: 1px solid #44475a; padding: 10px; text-align: left; }\n" )
		           .append( "th { background-color: #44475a; color: #f8f8f2; font-weight: bold; }\n" )
		           .append( ".map-label { color: #ff79c6; }\n" )
		           .append( ".custom-label { color: #8be9fd; }\n" )
		           .append( "h1 { color: #bd93f9; }\n" )
		           .append( "h2 { color: #50fa7b; }\n" )
		           .append( "</style>\n" )
		           .append( "<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n" )
		           .append( "</head>\n<body>\n<h1>Memory Footprint Report</h1>\n" );
		
		// Improved description
		htmlBuilder.append( "<p>This report compares memory usage (in bytes) between standard Java HashMap and custom map implementations. " )
		           .append( "Measurements show total memory footprint for different data types under three scenarios: no nulls, half nulls, and all nulls. " )
		           .append( "Lower values indicate better memory efficiency.</p>\n" );
		
		// Chart containers
		for( Data data : dataList ) {
			htmlBuilder.append( "<div class=\"chart-container\">\n" )
			           .append( "<h2>" ).append( data.type ).append( " Maps</h2>\n" )
			           .append( "<canvas id=\"chart-" ).append( data.type.toLowerCase().replace( " ", "-" ) ).append( "\"></canvas>\n" )
			           .append( "</div>\n" );
		}
		
		// Enhanced charts with multiple datasets (inspired by first generator)
		htmlBuilder.append( "<script>\n" );
		for( Data data : dataList ) {
			String chartId = "chart-" + data.type.toLowerCase().replace( " ", "-" );
			
			String standardLabel;
			String customLabel;
			switch( data.type ) {
				case "Byte":
					standardLabel = "HashMap<Byte, Byte>";
					customLabel = "ByteByteNullMap";
					break;
				case "Short":
					standardLabel = "HashMap<Short, Short>";
					customLabel = "ShortShortNullMap";
					break;
				case "Int":
					standardLabel = "HashMap<Integer, Integer>";
					customLabel = "IntIntNullMap";
					break;
				case "Long":
					standardLabel = "HashMap<Long, Long>";
					customLabel = "LongLongNullMap";
					break;
				case "Int-Boolean":
					standardLabel = "HashMap<Integer, Boolean>";
					customLabel = "IntBitsMap";
					break;
				default:
					standardLabel = "HashMap<?, ?>";
					customLabel = "CustomMap";
			}
			
			htmlBuilder.append( "new Chart(document.getElementById('" ).append( chartId ).append( "').getContext('2d'), {\n" )
			           .append( "type: 'bar',\n" )
			           .append( "data: {\n" )
			           .append( "labels: ['No entries with null values.', 'Half of the entries have null values.', 'All entries have null values.'],\n" )
			           .append( "datasets: [{\n" )
			           .append( "label: '" ).append( standardLabel ).append( "',\n" )
			           .append( "data: [" ).append( data.data[ 0 ] ).append( ", " ).append( data.data[ 2 ] ).append( ", " )
			           .append( data.data[ 4 ] ).append( "],\n" )
			           .append( "backgroundColor: '#ff79c6',\n" )
			           .append( "borderColor: '#ff79c6',\n" )
			           .append( "borderWidth: 1\n" )
			           .append( "}, {\n" )
			           .append( "label: '" ).append( customLabel ).append( "',\n" )
			           .append( "data: [" ).append( data.data[ 1 ] ).append( ", " ).append( data.data[ 3 ] ).append( ", " )
			           .append( data.data[ 5 ] ).append( "],\n" )
			           .append( "backgroundColor: '#8be9fd',\n" )
			           .append( "borderColor: '#8be9fd',\n" )
			           .append( "borderWidth: 1\n" )
			           .append( "}]\n" )
			           .append( "},\n" )
			           .append( "options: {\n" )
			           .append( "responsive: true,\n" )
			           .append( "scales: {\n" )
			           .append( "y: { beginAtZero: true, title: { display: true, text: 'Memory (bytes) - Lower is Better', color: '#f8f8f2' }, ticks: { color: '#f8f8f2' }, grid: { color: '#44475a' } },\n" )
			           .append( "x: { ticks: { color: '#f8f8f2' }, grid: { color: '#44475a' } }\n" )
			           .append( "},\n" )
			           .append( "plugins: {\n" )
			           .append( "legend: { labels: { color: '#f8f8f2' } },\n" )
			           .append( "title: { display: true, text: '" ).append( data.type ).append( " Memory Usage', color: '#f8f8f2', font: { size: 16 } }\n" )
			           .append( "}\n" )
			           .append( "}\n" )
			           .append( "});\n" );
		}
		htmlBuilder.append( "</script>\n</body>\n</html>" );
		
		return htmlBuilder.toString();
	}
}