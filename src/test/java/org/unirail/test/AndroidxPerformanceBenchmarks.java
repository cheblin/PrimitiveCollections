package org.unirail.test;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.unirail.collections.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@State( Scope.Benchmark )
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.NANOSECONDS )
@Warmup( iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS )
@Measurement( iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS )
@Fork( 1 )
public class AndroidxPerformanceBenchmarks {
	
	public static void main( String[] args ) throws RunnerException, IOException {
		int byteDataSize = 255;
		int dataSize     = 1000;
		reportDataMap.put( "Byte", new Data( "Byte", byteDataSize ) );
		reportDataMap.put( "Short", new Data( "Short", dataSize ) );
		reportDataMap.put( "Int", new Data( "Int", dataSize ) );
		reportDataMap.put( "Long", new Data( "Long", dataSize ) );
		reportDataMap.put( "Int-Boolean", new Data( "Int-Boolean", dataSize ) );
		
		Options opt = new OptionsBuilder()
				.include( AndroidxPerformanceBenchmarks.class.getSimpleName() )
				.build();
		
		Collection< RunResult > results = new Runner( opt ).run();
		processResults( results );
		generateHtmlReportFile();
	}
	
	private static void processResults( Collection< RunResult > results ) {
		for( RunResult result : results ) {
			String benchmarkName = result.getParams().getBenchmark();
			String methodName    = benchmarkName.substring( benchmarkName.lastIndexOf( '.' ) + 1 );
			long   scoreNs       = ( long ) result.getPrimaryResult().getScore();
			
			String[] parts = methodName.split( "_" );
			if( parts.length != 3 ) {
				System.err.println( "Skipping invalid benchmark name: " + methodName );
				continue;
			}
			
			String type = parts[ 0 ].equals( "IntBool" ) ?
					"Int-Boolean" :
					parts[ 0 ];
			String structure = parts[ 1 ];
			String operation = parts[ 2 ];
			
			Data data = reportDataMap.get( type );
			if( data == null ) {
				System.err.println( "No data entry for type: " + type );
				continue;
			}
			
			if( structure.equals( "Map" ) ) switch( operation ) {
				case "Insert":
					data.MapInsert = scoreNs;
					break;
				case "Search":
					data.MapSearch = scoreNs;
					break;
				case "Delete":
					data.MapDelete = scoreNs;
					break;
				case "Get":
					data.MapGet = scoreNs;
					break;
			}
			else if( structure.equals( "AdHoc" ) ) switch( operation ) {
				case "Insert":
					data.AdHocInsert = scoreNs;
					break;
				case "Search":
					data.AdHocSearch = scoreNs;
					break;
				case "Delete":
					data.AdHocDelete = scoreNs;
					break;
				case "Get":
					data.AdHocGet = scoreNs;
					break;
			}
		}
	}
	
	private static void generateHtmlReportFile() throws IOException {
		String htmlReport = generateHtmlReport( new ArrayList<>( reportDataMap.values() ) );
		File   reportFile = new File( "androidx_performance_report.html" );
		try( FileWriter writer = new FileWriter( reportFile ) ) {
			writer.write( htmlReport );
			System.out.println( "HTML report generated to: " + reportFile.getAbsolutePath() );
		}
	}
	public static String generateHtmlReport(List<Data> dataList) {
    StringBuilder htmlBuilder = new StringBuilder();
    
    htmlBuilder.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
               .append("<meta charset=\"UTF-8\">\n<title>Performance Report</title>\n")
               .append("<style>\n")
               .append("body { background-color: #1e1e1e; color: #f8f8f2; font-family: monospace; padding: 20px; line-height: 1.6; margin: 0 auto; max-width: 1024px; }\n")
               .append(".chart-container { width: 100%; max-width: 984px; margin: 20px auto; background: #282a36; padding: 15px; border-radius: 5px; }\n")
               .append("table { width: 100%; border-collapse: collapse; margin-top: 30px; background-color: #282a36; }\n")
               .append("th, td { border: 1px solid #44475a; padding: 10px; text-align: left; }\n")
               .append("th { background-color: #44475a; color: #f8f8f2; font-weight: bold; }\n")
               .append(".map-label { color: #ff79c6; }\n")
               .append(".adhoc-label { color: #8be9fd; }\n")
               .append("h1 { color: #bd93f9; }\n")
               .append("h2 { color: #50fa7b; }\n")
               .append("</style>\n")
               .append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n")
               .append("</head>\n<body>\n")
               .append("<h1>Performance Report - CPU: ")
               .append(new oshi.SystemInfo().getHardware().getProcessor().getProcessorIdentifier().getName().trim())
               .append(", Java VM: ").append(System.getProperty("java.vm.vendor") + " " + System.getProperty("java.vm.version"))
               .append("</h1>\n");
    
    htmlBuilder.append("<p>This report compares the performance of <a href=\"https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:collection/collection/src/commonMain/kotlin/androidx/collection/\">Maps from Androidx collection</a> vs AdHoc maps. ")
               .append("Measurements show average time per operation (ns/op) for insertion, search, get, and deletion across different data types and sizes. ")
               .append("Lower values indicate better performance.</p>\n");
    
    for(Data data : dataList)
        htmlBuilder.append("<div class=\"chart-container\">\n")
                   .append("<h2>").append(data.type).append(" Maps</h2>\n")
                   .append("<canvas id=\"chart-").append(data.type.toLowerCase().replace(" ", "-")).append("\"></canvas>\n")
                   .append("</div>\n");
    
    htmlBuilder.append("<script>\n");
    for(Data data : dataList) {
        String chartId = "chart-" + data.type.toLowerCase().replace(" ", "-");
        
        String androidxLabel;
        String adhocLabel;
        switch(data.type) {
            case "Byte":
                androidxLabel = "MutableIntIntMap";
                adhocLabel = "ByteByteNullMap.RW";
                break;
            case "Short":
                androidxLabel = "MutableIntIntMap";
                adhocLabel = "ShortShortNullMap.RW";
                break;
            case "Int":
                androidxLabel = "MutableIntIntMap";
                adhocLabel = "IntIntNullMap.RW";
                break;
            case "Long":
                androidxLabel = "MutableLongLongMap";
                adhocLabel = "LongLongNullMap.RW";
                break;
            case "Int-Boolean":
                androidxLabel = "MutableIntIntMap";
                adhocLabel = "IntBitsMap.RW";
                break;
            default:
                androidxLabel = "XXX";
                adhocLabel = "XXX";
        }
        
        htmlBuilder.append("new Chart(document.getElementById('").append(chartId).append("').getContext('2d'), {\n")
                   .append("type: 'bar',\n")
                   .append("data: {\n")
                   .append("labels: ['Insert', 'Search', 'Get', 'Delete'],\n")
                   .append("datasets: [{\n")
                   .append("label: '").append(androidxLabel).append("',\n")
                   .append("data: [").append(data.MapInsert).append(", ").append(data.MapSearch).append(", ")
                   .append(data.MapGet).append(", ").append(data.MapDelete).append("],\n")
                   .append("backgroundColor: '#ff79c6',\n")
                   .append("borderColor: '#ff79c6',\n")
                   .append("borderWidth: 1\n")
                   .append("}, {\n")
                   .append("label: '").append(adhocLabel).append("',\n")
                   .append("data: [").append(data.AdHocInsert).append(", ").append(data.AdHocSearch).append(", ")
                   .append(data.AdHocGet).append(", ").append(data.AdHocDelete).append("],\n")
                   .append("backgroundColor: '#8be9fd',\n")
                   .append("borderColor: '#8be9fd',\n")
                   .append("borderWidth: 1\n")
                   .append("}]\n")
                   .append("},\n")
                   .append("options: {\n")
                   .append("responsive: true,\n")
                   .append("scales: {\n")
                   .append("y: { beginAtZero: true, title: { display: true, text: 'Time (ns/op) - Lower is Better', color: '#f8f8f2' }, ticks: { color: '#f8f8f2' }, grid: { color: '#44475a' } },\n")
                   .append("x: { ticks: { color: '#f8f8f2' }, grid: { color: '#44475a' } }\n")
                   .append("},\n")
                   .append("plugins: {\n")
                   .append("legend: { labels: { color: '#f8f8f2' } },\n")
                   .append("title: { display: true, text: '").append(data.type).append(" Performance', color: '#f8f8f2', font: { size: 16 } }\n")
                   .append("}\n")
                   .append("}\n")
                   .append("});\n");
    }
    htmlBuilder.append("</script>\n</body>\n</html>");
    
    return htmlBuilder.toString();
}
	public static class Data {
		String type;
		int    dataSize;
		long   MapInsert;
		long   AdHocInsert;
		long   MapSearch;
		long   AdHocSearch;
		long   MapGet;
		long   AdHocGet;
		long   MapDelete;
		long   AdHocDelete;
		
		public Data( String type, int dataSize ) {
			this.type     = type;
			this.dataSize = dataSize;
		}
	}
	
	private static final Map< String, Data > reportDataMap = new LinkedHashMap<>();
	
	@Param( { "1000" } )
	private int dataSize;
	
	@Param( { "255" } )
	private int byteDataSize;
	
	private byte[]  byteKeys;
	private short[] shortKeys;
	private int[]   intKeys;
	private long[]  longKeys;
	
	@Setup( Level.Trial )
	public void setup() {
		byteKeys = new byte[ byteDataSize ];
		for( int i = 0; i < byteDataSize; i++ ) byteKeys[ i ] = ( byte ) ( byteDataSize - 1 - i );
		
		shortKeys = new short[ dataSize ];
		for( int i = 0; i < dataSize; i++ ) shortKeys[ i ] = ( short ) ( dataSize - 1 - i );
		
		intKeys = new int[ dataSize ];
		for( int i = 0; i < dataSize; i++ ) intKeys[ i ] = dataSize - 1 - i;
		
		longKeys = new long[ dataSize ];
		for( int i = 0; i < dataSize; i++ ) longKeys[ i ] = dataSize - 1 - i;
	}
	
	@State( Scope.Thread )
	public static class MapState {
		androidx.collection.MutableIntIntMap  empty_byteMap;
		ByteByteNullMap.RW    empty_byteAdHoc;
		
		androidx.collection.MutableIntIntMap  empty_shortMap;
		ShortShortNullMap.RW    empty_shortAdHoc;
		
		androidx.collection.MutableIntIntMap  empty_intMap;
		IntIntNullMap.RW            empty_intAdHoc;
		
		androidx.collection.MutableLongLongMap empty_longMap;
		LongLongNullMap.RW    empty_longAdHoc;
		
		androidx.collection.MutableIntIntMap empty_intBoolMap;
		IntBitsMap.RW               empty_intBoolAdHoc;
		
		androidx.collection.MutableIntIntMap  byteMap;
		ByteByteNullMap.RW    byteAdHoc;
		
		androidx.collection.MutableIntIntMap  shortMap;
		ShortShortNullMap.RW    shortAdHoc;
		
		androidx.collection.MutableIntIntMap  intMap;
		IntIntNullMap.RW            intAdHoc;
		
		androidx.collection.MutableLongLongMap longMap;
		LongLongNullMap.RW    longAdHoc;
		
		androidx.collection.MutableIntIntMap intBoolMap;
		IntBitsMap.RW               intBoolAdHoc;
		
		byte[]  byteKeys;
		short[] shortKeys;
		int[]   intKeys;
		long[]  longKeys;
		
		@Setup( Level.Trial )
		public void setupTrial( AndroidxPerformanceBenchmarks benchmark ) {
			this.byteKeys  = benchmark.byteKeys;
			this.shortKeys = benchmark.shortKeys;
			this.intKeys   = benchmark.intKeys;
			this.longKeys  = benchmark.longKeys;
		}
		
		@Setup( Level.Iteration )
		public void setupIteration() {
			empty_byteMap   = new androidx.collection.MutableIntIntMap();
			empty_byteAdHoc = new ByteByteNullMap.RW( 0 );
			
			empty_shortMap   = new androidx.collection.MutableIntIntMap();
			empty_shortAdHoc = new ShortShortNullMap.RW( 0 );
			
			empty_intMap   = new androidx.collection.MutableIntIntMap();
			empty_intAdHoc = new IntIntNullMap.RW( 0 );
			
			empty_longMap   = new androidx.collection.MutableLongLongMap();
			empty_longAdHoc = new LongLongNullMap.RW( 0 );
			
			empty_intBoolMap   = new androidx.collection.MutableIntIntMap();
			empty_intBoolAdHoc = new IntBitsMap.RW( 0, 2, 2 );
			
			byteMap   = new androidx.collection.MutableIntIntMap();
			byteAdHoc = new ByteByteNullMap.RW( 0 );
			
			shortMap   = new androidx.collection.MutableIntIntMap();
			shortAdHoc = new ShortShortNullMap.RW( 0 );
			
			intMap   = new androidx.collection.MutableIntIntMap();
			intAdHoc = new IntIntNullMap.RW( 0 );
			
			longMap   = new androidx.collection.MutableLongLongMap();
			longAdHoc = new LongLongNullMap.RW( 0 );
			
			intBoolMap   = new androidx.collection.MutableIntIntMap();
			intBoolAdHoc = new IntBitsMap.RW( 0, 2, 2 );
			
			for( byte key : byteKeys ) byteMap.put( key, key );
			for( byte key : byteKeys ) byteAdHoc.put( key, key );
			
			for( short key : shortKeys ) shortMap.put( key, key );
			for( short key : shortKeys ) shortAdHoc.put( key, key );
			
			for( int key : intKeys ) intMap.put( key, key );
			for( int key : intKeys ) intAdHoc.put( key, key );
			
			for( long key : longKeys ) longMap.put( key, key );
			for( long key : longKeys ) longAdHoc.put( key, key );
			
			for( int key : intKeys ) intBoolMap.put( key, key % 2  );
			for( int key : intKeys )
				intBoolAdHoc.put( key, key % 2 == 0 ?
						1 :
						0 );
		}
	}
	
	static byte    by;
	static short   s;
	static int     i;
	static long    l;
	static boolean b;
	
	// Byte Benchmarks
	@Benchmark public void Byte_Map_Insert( MapState state ) {
		for( byte key : byteKeys ) state.empty_byteMap.put( key, key );
	}
	
	@Benchmark public void Byte_Map_Search( MapState state ) {
		for( byte key : byteKeys ) b = state.byteMap.containsKey( key );
	}
	
	@Benchmark public void Byte_Map_Get( MapState state ) {
		for( byte key : byteKeys )
			if( state.byteMap.containsKey( key ) )
				i = state.byteMap.get( key );
	}
	
	@Benchmark public void Byte_Map_Delete( MapState state ) {
		for( byte key : byteKeys ) state.byteMap.remove( key );
	}
	
	@Benchmark public void Byte_AdHoc_Insert( MapState state ) {
		for( byte key : byteKeys ) state.empty_byteAdHoc.put( key, key );
	}
	
	@Benchmark public void Byte_AdHoc_Search( MapState state ) {
		for( byte key : byteKeys ) b = state.byteAdHoc.containsKey( key );
	}
	
	@Benchmark public void Byte_AdHoc_Get( MapState state ) {
		for( byte key : byteKeys ) {
			long token = state.byteAdHoc.tokenOf( key );
			if( token != -1 ) by = state.byteAdHoc.value( token );
		}
	}
	
	@Benchmark public void Byte_AdHoc_Delete( MapState state ) {
		state.byteAdHoc.remove( ( byte ) 0 );
		for( byte key : byteKeys ) state.byteAdHoc.remove( key );
	}
	
	// Short Benchmarks
	@Benchmark public void Short_Map_Insert( MapState state ) {
		for( short key : shortKeys ) state.empty_shortMap.put( key, key );
	}
	
	@Benchmark public void Short_Map_Search( MapState state ) {
		for( short key : shortKeys ) b = state.shortMap.containsKey( key );
	}
	
	@Benchmark public void Short_Map_Get( MapState state ) {
		for( short key : shortKeys )
			if( state.shortMap.containsKey( key ) )
				i = state.shortMap.get( key );
	}
	
	@Benchmark public void Short_Map_Delete( MapState state ) {
		for( short key : shortKeys ) state.shortMap.remove( key );
	}
	
	@Benchmark public void Short_AdHoc_Insert( MapState state ) {
		for( short key : shortKeys ) state.empty_shortAdHoc.put( key, key );
	}
	
	@Benchmark public void Short_AdHoc_Search( MapState state ) {
		for( short key : shortKeys ) b = state.shortAdHoc.containsKey( key );
	}
	
	@Benchmark public void Short_AdHoc_Get( MapState state ) {
		for( short key : shortKeys ) {
			long token = state.shortAdHoc.tokenOf( key );
			if(  token !=-1 )
				s = state.shortAdHoc.value( token );
		}
	}
	
	@Benchmark public void Short_AdHoc_Delete( MapState state ) {
		for( short key : shortKeys ) state.shortAdHoc.remove( key );
	}
	
	// Int Benchmarks
	@Benchmark public void Int_Map_Insert( MapState state ) {
		for( int key : intKeys ) state.empty_intMap.put( key, key );
	}
	
	@Benchmark public void Int_Map_Search( MapState state ) {
		for( int key : intKeys ) b = state.intMap.containsKey( key );
	}
	
	@Benchmark public void Int_Map_Get( MapState state ) {
		for( int key : intKeys )
			if( state.intMap.containsKey( key ) )
				i = state.intMap.get( key );
	}
	
	@Benchmark public void Int_Map_Delete( MapState state ) {
		for( int key : intKeys ) state.intMap.remove( key );
	}
	
	@Benchmark public void Int_AdHoc_Insert( MapState state ) {
		for( int key : intKeys ) state.empty_intAdHoc.put( key, key );
	}
	
	@Benchmark public void Int_AdHoc_Search( MapState state ) {
		for( int key : intKeys ) b = state.intAdHoc.containsKey( key );
	}
	
	@Benchmark public void Int_AdHoc_Get( MapState state ) {
		for( int key : intKeys ) {
			long token = state.intAdHoc.tokenOf( key );
			if(  token !=-1 )
				i = state.intAdHoc.value( token );
		}
	}
	
	@Benchmark public void Int_AdHoc_Delete( MapState state ) {
		for( int key : intKeys ) state.intAdHoc.remove( key );
	}
	
	// Long Benchmarks
	@Benchmark public void Long_Map_Insert( MapState state ) {
		for( long key : longKeys ) state.empty_longMap.put( key, key );
	}
	
	@Benchmark public void Long_Map_Search( MapState state ) {
		for( long key : longKeys ) b = state.longMap.containsKey( key );
	}
	
	@Benchmark public void Long_Map_Get( MapState state ) {
		for( long key : longKeys )
			if( state.longMap.containsKey( key ) )
				l = state.longMap.get( key );
	}
	
	@Benchmark public void Long_Map_Delete( MapState state ) {
		for( long key : longKeys ) state.longMap.remove( key );
	}
	
	@Benchmark public void Long_AdHoc_Insert( MapState state ) {
		for( long key : longKeys ) state.empty_longAdHoc.put( key, key );
	}
	
	@Benchmark public void Long_AdHoc_Search( MapState state ) {
		for( long key : longKeys ) b = state.longAdHoc.containsKey( key );
	}
	
	@Benchmark public void Long_AdHoc_Get( MapState state ) {
		for( long key : longKeys ) {
			long token = state.longAdHoc.tokenOf( key );
			if(  token !=-1 )
				l = state.longAdHoc.value( token );
		}
	}
	
	@Benchmark public void Long_AdHoc_Delete( MapState state ) {
		for( long key : longKeys ) state.longAdHoc.remove( key );
	}
	
	// Int-Boolean Benchmarks
	@Benchmark public void IntBool_Map_Insert( MapState state ) {
		for( int key : intKeys ) state.empty_intBoolMap.put( key, key % 2  );
	}
	
	@Benchmark public void IntBool_Map_Search( MapState state ) {
		for( int key : intKeys ) b = state.intBoolMap.containsKey( key );
	}
	
	@Benchmark public void IntBool_Map_Get( MapState state ) {
		for( int key : intKeys )
			if( state.intBoolMap.containsKey( key ) )
				i = state.intBoolMap.get( key );
	}
	
	@Benchmark public void IntBool_Map_Delete( MapState state ) {
		for( int key : intKeys ) state.intBoolMap.remove( key );
	}
	
	@Benchmark public void IntBool_AdHoc_Insert( MapState state ) {
		for( int key : intKeys )
			state.empty_intBoolAdHoc.put( key, key % 2 == 0 ?
					1 :
					0 );
	}
	
	@Benchmark public void IntBool_AdHoc_Search( MapState state ) {
		for( int key : intKeys ) b = state.intBoolAdHoc.containsKey( key );
	}
	
	@Benchmark public void IntBool_AdHoc_Get( MapState state ) {
		for( int key : intKeys ) {
			long token = state.intBoolAdHoc.tokenOf( key );
			if(  token !=-1 )
				b = state.intBoolAdHoc.value( token ) == 1;
		}
	}
	
	@Benchmark public void IntBool_AdHoc_Delete( MapState state ) {
		for( int key : intKeys ) state.intBoolAdHoc.remove( key );
	}
}