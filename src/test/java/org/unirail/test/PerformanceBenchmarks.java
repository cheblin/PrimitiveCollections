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

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PerformanceBenchmarks {

    public static void main(String[] args) throws RunnerException, IOException {
        int byteDataSize = 255;
        int dataSize = 1000;
        reportDataMap.put("Byte", new Data("Byte", byteDataSize));
        reportDataMap.put("Short", new Data("Short", dataSize));
        reportDataMap.put("Int", new Data("Int", dataSize));
        reportDataMap.put("Long", new Data("Long", dataSize));
        reportDataMap.put("Int-Boolean", new Data("Int-Boolean", dataSize));

        Options opt = new OptionsBuilder()
                .include( PerformanceBenchmarks.class.getSimpleName())
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        processResults(results);
        generateHtmlReportFile();
    }

    private static void processResults(Collection<RunResult> results) {
        for (RunResult result : results) {
            String benchmarkName = result.getParams().getBenchmark();
            String methodName = benchmarkName.substring(benchmarkName.lastIndexOf('.') + 1);
            long scoreNs = (long) result.getPrimaryResult().getScore();

            String[] parts = methodName.split("_");
            if (parts.length != 3) {
                System.err.println("Skipping invalid benchmark name: " + methodName);
                continue;
            }

            String type = parts[0].equals("IntBool") ? "Int-Boolean" : parts[0];
            String structure = parts[1];
            String operation = parts[2];

            Data data = reportDataMap.get(type);
            if (data == null) {
                System.err.println("No data entry for type: " + type);
                continue;
            }

            if (structure.equals("Map")) {
                switch (operation) {
                    case "Insert": data.MapInsert = scoreNs; break;
                    case "Search": data.MapSearch = scoreNs; break;
                    case "Delete": data.MapDelete = scoreNs; break;
                    case "Get": data.MapGet = scoreNs; break;
                }
            } else if (structure.equals("AdHoc")) {
                switch (operation) {
                    case "Insert": data.AdHocInsert = scoreNs; break;
                    case "Search": data.AdHocSearch = scoreNs; break;
                    case "Delete": data.AdHocDelete = scoreNs; break;
                    case "Get": data.AdHocGet = scoreNs; break;
                }
            }
        }
    }

    private static void generateHtmlReportFile() throws IOException {
        String htmlReport = generateHtmlReport(new ArrayList<>(reportDataMap.values()));
        File reportFile = new File("performance_report.html");
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(htmlReport);
            System.out.println("HTML report generated to: " + reportFile.getAbsolutePath());
        }
    }

    public static String generateHtmlReport(List<Data> dataList) {
    StringBuilder htmlBuilder = new StringBuilder();

    htmlBuilder.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
               .append("<meta charset=\"UTF-8\">\n<title>Performance Report</title>\n")
               .append("<style>\n")
               .append("body { background-color: #1e1e1e; color: #f8f8f2; font-family: monospace; padding: 20px; line-height: 1.6; }\n")
               .append(".chart-container { width: 80%; margin: 20px auto; background: #282a36; padding: 15px; border-radius: 5px; }\n")
               .append("table { width: 100%; border-collapse: collapse; margin-top: 30px; background-color: #282a36; }\n")
               .append("th, td { border: 1px solid #44475a; padding: 10px; text-align: left; }\n")
               .append("th { background-color: #44475a; color: #f8f8f2; font-weight: bold; }\n")
               .append(".map-label { color: #ff79c6; }\n")
               .append(".custom-label { color: #8be9fd; }\n")
               .append("h1 { color: #bd93f9; }\n")
               .append("h2 { color: #50fa7b; }\n")
               .append("</style>\n")
               .append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n")
               .append("</head>\n<body>\n<h1>Performance Report</h1>\n");

    htmlBuilder.append("<p>This report compares the performance of standard Java HashMap against custom map implementations from org.unirail.collections. ")
               .append("Measurements show average time per operation (ns/op) for insertion, search, get, and deletion across different data types and sizes. ")
               .append("Lower values indicate better performance.</p>\n");

    for (Data data : dataList) {
        htmlBuilder.append("<div class=\"chart-container\">\n")
                   .append("<h2>").append(data.type).append(" Maps</h2>\n")
                   .append("<canvas id=\"chart-").append(data.type.toLowerCase().replace(" ", "-")).append("\"></canvas>\n")
                   .append("</div>\n");
    }

    htmlBuilder.append("<script>\n");
    for (Data data : dataList) {
        String chartId = "chart-" + data.type.toLowerCase().replace(" ", "-");

        String standardLabel;
        String customLabel;
        switch (data.type) {
            case "Byte":
                standardLabel = "HashMap<Byte, Byte>";
                customLabel = "ByteByteNullMap.RW";
                break;
            case "Short":
                standardLabel = "HashMap<Short, Short>";
                customLabel = "ShortShortNullMap.RW";
                break;
            case "Int":
                standardLabel = "HashMap<Integer, Integer>";
                customLabel = "IntIntNullMap.RW";
                break;
            case "Long":
                standardLabel = "HashMap<Long, Long>";
                customLabel = "LongLongNullMap.RW";
                break;
            case "Int-Boolean":
                standardLabel = "HashMap<Integer, Boolean>";
                customLabel = "IntBitsMap.RW";
                break;
            default:
                standardLabel = "HashMap<?, ?>";
                customLabel = "CustomMap";
        }

        htmlBuilder.append("new Chart(document.getElementById('").append(chartId).append("').getContext('2d'), {\n")
                   .append("type: 'bar',\n")
                   .append("data: {\n")
                   .append("labels: ['Insert', 'Search', 'Get', 'Delete'],\n")
                   .append("datasets: [{\n")
                   .append("label: '").append(standardLabel).append("',\n")
                   .append("data: [").append(data.MapInsert).append(", ").append(data.MapSearch).append(", ")
                   .append(data.MapGet).append(", ").append(data.MapDelete).append("],\n")
                   .append("backgroundColor: '#ff79c6',\n")
                   .append("borderColor: '#ff79c6',\n")
                   .append("borderWidth: 1\n")
                   .append("}, {\n")
                   .append("label: '").append(customLabel).append("',\n")
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
        int dataSize;
        long MapInsert;
        long AdHocInsert;
        long MapSearch;
        long AdHocSearch;
        long MapGet;
        long AdHocGet;
        long MapDelete;
        long AdHocDelete;

        public Data(String type, int dataSize) {
            this.type = type;
            this.dataSize = dataSize;
        }
    }

    private static final Map<String, Data> reportDataMap = new LinkedHashMap<>();

    @Param({"1000"})
    private int dataSize;

    @Param({"255"})
    private int byteDataSize;

    private byte[] byteKeys;
    private short[] shortKeys;
    private int[] intKeys;
    private long[] longKeys;

    @Setup(Level.Trial)
    public void setup() {
        byteKeys = new byte[byteDataSize];
        for (int i = 0; i < byteDataSize; i++) byteKeys[i] = (byte) i;

        shortKeys = new short[dataSize];
        for (int i = 0; i < dataSize; i++) shortKeys[i] = (short) i;

        intKeys = new int[dataSize];
        for (int i = 0; i < dataSize; i++) intKeys[i] = i;

        longKeys = new long[dataSize];
        for (int i = 0; i < dataSize; i++) longKeys[i] = i;
    }

    @State(Scope.Thread)
    public static class MapState {
        Map<Byte, Byte> byteMap;
        ByteByteNullMap.RW byteAdHoc;

        Map<Short, Short> shortMap;
        ShortShortNullMap.RW shortAdHoc;

        Map<Integer, Integer> intMap;
        IntIntNullMap.RW intAdHoc;

        Map<Long, Long> longMap;
        LongLongNullMap.RW longAdHoc;

        Map<Integer, Boolean> intBoolMap;
        IntBitsMap.RW intBoolAdHoc;

        byte[] byteKeys;
        short[] shortKeys;
        int[] intKeys;
        long[] longKeys;

        @Setup(Level.Trial)
        public void setupTrial( PerformanceBenchmarks benchmark) {
            this.byteKeys = benchmark.byteKeys;
            this.shortKeys = benchmark.shortKeys;
            this.intKeys = benchmark.intKeys;
            this.longKeys = benchmark.longKeys;
        }

        @Setup(Level.Iteration)
        public void setupIteration() {
            byteMap = new HashMap<>();
            byteAdHoc = new ByteByteNullMap.RW(0);

            shortMap = new HashMap<>();
            shortAdHoc = new ShortShortNullMap.RW(0);

            intMap = new HashMap<>();
            intAdHoc = new IntIntNullMap.RW(0);

            longMap = new HashMap<>();
            longAdHoc = new LongLongNullMap.RW(0);

            intBoolMap = new HashMap<>();
            intBoolAdHoc = new IntBitsMap.RW(0, 2, 2);

            for (byte key : byteKeys) byteMap.put(key, key);
            for (byte key : byteKeys) byteAdHoc.put(key, key);

            for (short key : shortKeys) shortMap.put(key, key);
            for (short key : shortKeys) shortAdHoc.put(key, key);

            for (int key : intKeys) intMap.put(key, key);
            for (int key : intKeys) intAdHoc.put(key, key);

            for (long key : longKeys) longMap.put(key, key);
            for (long key : longKeys) longAdHoc.put(key, key);

            for (int key : intKeys) intBoolMap.put(key, key % 2 == 0);
            for (int key : intKeys) intBoolAdHoc.put(key, key % 2 == 0 ? 1 : 0);
        }
    }

    static byte by;
    static short s;
    static int i;
    static long l;
    static boolean b;

    // Byte Benchmarks
    @Benchmark public void Byte_Map_Insert(MapState state) {
        state.byteMap.clear();
        for (byte key : byteKeys) state.byteMap.put(key, key);
    }

    @Benchmark public void Byte_Map_Search(MapState state) {
        for (byte key : byteKeys) b = state.byteMap.containsKey(key);
    }

    @Benchmark public void Byte_Map_Get(MapState state) {
        for (byte key : byteKeys)
            if (state.byteMap.containsKey(key))
                by = state.byteMap.get(key);
    }

    @Benchmark public void Byte_Map_Delete(MapState state) {
        for (byte key : byteKeys) state.byteMap.remove(key);
    }

    @Benchmark public void Byte_AdHoc_Insert(MapState state) {
        state.byteAdHoc.clear();
        for (byte key : byteKeys) state.byteAdHoc.put(key, key);
    }

    @Benchmark public void Byte_AdHoc_Search(MapState state) {
        for (byte key : byteKeys) b = state.byteAdHoc.contains(key);
    }

    @Benchmark public void Byte_AdHoc_Get(MapState state) {
        for (byte key : byteKeys) {
            long token = state.byteAdHoc.tokenOf(key);
            if (state.byteAdHoc.isValid(token))
                by = state.byteAdHoc.value(token);
        }
    }

    @Benchmark public void Byte_AdHoc_Delete(MapState state) {
        
        state.byteAdHoc.remove( ( byte ) 0 );
        
        for (byte key : byteKeys) {
            
            state.byteAdHoc.remove(key);
        }
    }

    // Short Benchmarks
    @Benchmark public void Short_Map_Insert(MapState state) {
        state.shortMap.clear();
        for (short key : shortKeys) state.shortMap.put(key, key);
    }

    @Benchmark public void Short_Map_Search(MapState state) {
        for (short key : shortKeys) b = state.shortMap.containsKey(key);
    }

    @Benchmark public void Short_Map_Get(MapState state) {
        for (short key : shortKeys)
            if (state.shortMap.containsKey(key))
                s = state.shortMap.get(key);
    }

    @Benchmark public void Short_Map_Delete(MapState state) {
        for (short key : shortKeys) state.shortMap.remove(key);
    }

    @Benchmark public void Short_AdHoc_Insert(MapState state) {
        state.shortAdHoc.clear();
        for (short key : shortKeys) state.shortAdHoc.put(key, key);
    }

    @Benchmark public void Short_AdHoc_Search(MapState state) {
        for (short key : shortKeys) b = state.shortAdHoc.contains(key);
    }

    @Benchmark public void Short_AdHoc_Get(MapState state) {
        for (short key : shortKeys) {
            long token = state.shortAdHoc.tokenOf(key);
            if (state.shortAdHoc.isValid(token))
                s = state.shortAdHoc.value(token);
        }
    }

    @Benchmark public void Short_AdHoc_Delete(MapState state) {
        for (short key : shortKeys) state.shortAdHoc.remove(key);
    }

    // Int Benchmarks
    @Benchmark public void Int_Map_Insert(MapState state) {
        state.intMap.clear();
        for (int key : intKeys) state.intMap.put(key, key);
    }

    @Benchmark public void Int_Map_Search(MapState state) {
        for (int key : intKeys) b = state.intMap.containsKey(key);
    }

    @Benchmark public void Int_Map_Get(MapState state) {
        for (int key : intKeys)
            if (state.intMap.containsKey(key))
                i = state.intMap.get(key);
    }

    @Benchmark public void Int_Map_Delete(MapState state) {
        for (int key : intKeys) state.intMap.remove(key);
    }

    @Benchmark public void Int_AdHoc_Insert(MapState state) {
        state.intAdHoc.clear();
        for (int key : intKeys) state.intAdHoc.put(key, key);
    }

    @Benchmark public void Int_AdHoc_Search(MapState state) {
        for (int key : intKeys) b = state.intAdHoc.contains(key);
    }

    @Benchmark public void Int_AdHoc_Get(MapState state) {
        for (int key : intKeys) {
            long token = state.intAdHoc.tokenOf(key);
            if (state.intAdHoc.isValid(token))
                i = state.intAdHoc.value(token);
        }
    }

    @Benchmark public void Int_AdHoc_Delete(MapState state) {
        for (int key : intKeys) state.intAdHoc.remove(key);
    }

    // Long Benchmarks
    @Benchmark public void Long_Map_Insert(MapState state) {
        state.longMap.clear();
        for (long key : longKeys) state.longMap.put(key, key);
    }

    @Benchmark public void Long_Map_Search(MapState state) {
        for (long key : longKeys) b = state.longMap.containsKey(key);
    }

    @Benchmark public void Long_Map_Get(MapState state) {
        for (long key : longKeys)
            if (state.longMap.containsKey(key))
                l = state.longMap.get(key);
    }

    @Benchmark public void Long_Map_Delete(MapState state) {
        for (long key : longKeys) state.longMap.remove(key);
    }

    @Benchmark public void Long_AdHoc_Insert(MapState state) {
        state.longAdHoc.clear();
        for (long key : longKeys) state.longAdHoc.put(key, key);
    }

    @Benchmark public void Long_AdHoc_Search(MapState state) {
        for (long key : longKeys) b = state.longAdHoc.contains(key);
    }

    @Benchmark public void Long_AdHoc_Get(MapState state) {
        for (long key : longKeys) {
            long token = state.longAdHoc.tokenOf(key);
            if (state.longAdHoc.isValid(token))
                l = state.longAdHoc.value(token);
        }
    }

    @Benchmark public void Long_AdHoc_Delete(MapState state) {
        for (long key : longKeys) state.longAdHoc.remove(key);
    }

    // Int-Boolean Benchmarks
    @Benchmark public void IntBool_Map_Insert(MapState state) {
        state.intBoolMap.clear();
        for (int key : intKeys) state.intBoolMap.put(key, key % 2 == 0);
    }

    @Benchmark public void IntBool_Map_Search(MapState state) {
        for (int key : intKeys) b = state.intBoolMap.containsKey(key);
    }

    @Benchmark public void IntBool_Map_Get(MapState state) {
        for (int key : intKeys)
            if (state.intBoolMap.containsKey(key))
                b = state.intBoolMap.get(key);
    }

    @Benchmark public void IntBool_Map_Delete(MapState state) {
        for (int key : intKeys) state.intBoolMap.remove(key);
    }

    @Benchmark public void IntBool_AdHoc_Insert(MapState state) {
        state.intBoolAdHoc.clear();
        for (int key : intKeys) state.intBoolAdHoc.put(key, key % 2 == 0 ? 1 : 0);
    }

    @Benchmark public void IntBool_AdHoc_Search(MapState state) {
        for (int key : intKeys) b = state.intBoolAdHoc.contains(key);
    }

    @Benchmark public void IntBool_AdHoc_Get(MapState state) {
        for (int key : intKeys) {
            long token = state.intBoolAdHoc.tokenOf(key);
            if (state.intBoolAdHoc.isValid(token))
                b = state.intBoolAdHoc.value(token) == 1;
        }
    }

    @Benchmark public void IntBool_AdHoc_Delete(MapState state) {
        for (int key : intKeys) state.intBoolAdHoc.remove(key);
    }
}