package utils;

import Segmentation.data.Node;
import com.opencsv.CSVWriter;
import data.Event;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    static String eventListToString(List<Event> events){
        String[] header = {"caseID", "timeStamp", "userID", "targetApp", "eventType", "url", "content", "target.workbookName",
                "target.sheetName", "target.id", "target.class", "target.tagName", "target.type", "target.name",
                "target.value", "target.innerText", "target.checked", "target.href", "target.option", "target.title", "target.innerHTML"
        };
        String str = "";
        for(var event: events){
            str += "\"" + event.getTimestamp() + "\",";
            str += event.payload.containsKey("userID") ? "\"" + event.payload.get("userID") + "\"," : "\"\",";
            str += event.payload.containsKey("targetApp") ? "\"" + event.payload.get("targetApp") + "\"," : "\"\",";
            str += "\"" + event.getEventType() + "\",";

            for(int i = 5; i < header.length; i++)
                if(event.payload.containsKey(header[i]) && !event.payload.get(header[i]).equals("\"\""))
                    str += "\"" + event.payload.get(header[i]) + "\",";
                else
                    str += "\"\",";

            str = str.substring(0, str.lastIndexOf(",")) + "\n";
        }
        return str;
    }

    static void writeDataLineByLine(String filePath, String data) {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filePath),
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.NO_ESCAPE_CHARACTER,
                    CSVWriter.RFC4180_LINE_END);

            String[] headers = {"\"timeStamp\"", "\"userID\"", "\"targetApp\"", "\"eventType\"", "\"url\"",
                    "\"content\"", "\"target.workbookName\"", "\"target.sheetName\"", "\"target.id\"", "\"target.class\"",
                    "\"target.tagName\"", "\"target.type\"", "\"target.name\"", "\"target.value\"", "\"target.innerText\"",
                    "\"target.checked\"", "\"target.href\"", "\"target.option\"", "\"target.title\"", "\"target.innerHTML\""
            };

            writer.writeNext(headers);
            writeActionsValues(writer, data);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void writeActionsValues(CSVWriter writer, String data){
        String[] actions = data.split("\n");

        for (String action : actions) {
            String[] actionValues = action.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            actionValues = Arrays.stream(actionValues)
                    .map(e -> e.replaceAll("\"{2}(([^\"]|\"\")*)\"{2}", "\"\"\"$1\"\"\""))
                    .toArray(String[]::new);
            writer.writeNext(actionValues);
        }
    }

    public static void writeSegments(String filePath, Map<Integer, List<Event>> segments){
        System.out.print("\nSaving segmented log... ");
        long startTime = System.currentTimeMillis();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filePath),
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.NO_ESCAPE_CHARACTER,
                    CSVWriter.RFC4180_LINE_END);

            List<Event> events = new ArrayList<>();
            segments.values().forEach(events::addAll);

            String[] headers = Stream.concat(Stream.of("\"caseID\""),
                    extractAttributes(events).stream().map(el -> "\"" + el + "\"")).toArray(String[]::new);
            writer.writeNext(headers);

            StringBuilder row = new StringBuilder();
            System.out.println(row.toString());
            for(var caseID: segments.keySet())
                for(var event: segments.get(caseID)){
                    for (String header : headers) {
                        switch (header) {
                            case "\"caseID\"":
                                row.append("\"").append(caseID).append("\",");
                                break;
                            case "\"timeStamp\"":
                                row.append("\"").append(event.getTimestamp()).append("\",");
                                break;
                            case "\"eventType\"":
                                row.append("\"").append(event.getEventType()).append("\",");
                                break;
                            default:
                                String attribute = header.replaceAll("^\"(.*)\"$", "$1");
                                row.append(event.payload.containsKey(attribute) ? "\"" + event.payload.get(attribute) + "\"," : ",");
                                break;
                        }
                    }
                    row = new StringBuilder(row.substring(0, row.lastIndexOf(",")) + "\n");
                }
            Utils.writeActionsValues(writer, row.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");
    }

    /* Context attributes analysis */

    public static HashMap<String, List<Event>> groupByEventType(List<Event> events){
        HashMap<String, List<Event>> groupedEvents = new HashMap<>();
        for(var event: events){
            if(!groupedEvents.containsKey(event.getEventType()))
                groupedEvents.put(event.getEventType(), Collections.singletonList(event));
            else
                groupedEvents.put(event.getEventType(), Stream.concat(groupedEvents.get(event.getEventType()).stream(),
                        Stream.of(event)).collect(Collectors.toList()));
        }
        return groupedEvents;
    }

    public static HashMap<String, List<Event>> groupEvents(List<Event> events){
        HashMap<String, List<Event>> groupedEvents = new HashMap<>();
        for(var event: events){
            var key = event.getEventType() + "_" + event.getApplication();
            if(!groupedEvents.containsKey(key))
                groupedEvents.put(key, Collections.singletonList(event));
            else
                groupedEvents.put(key, Stream.concat(groupedEvents.get(key).stream(),
                        Stream.of(event)).collect(Collectors.toList()));
        }

        return groupedEvents;
    }

    public static HashMap<Integer, List<Event>> extractCases(List<Event> events){
        HashMap<Integer, List<Event>> cases = new HashMap<>();
        for(var event: events){
            var caseID = Integer.valueOf(event.getCaseID());
            if(!cases.containsKey(caseID))
                cases.put(caseID, Collections.singletonList(event));
            else
                cases.put(caseID, Stream.concat(cases.get(caseID).stream(),
                        Stream.of(event)).collect(Collectors.toList()));
        }
        return cases;
    }

    public static void setContextAttributes(List<Event> events, List<String> contextAttributes){
        for(var event: events){
            HashMap<String, String> context = new HashMap<>();
            for(var attribute: event.payload.keySet())
                if(contextAttributes.contains(attribute)) {
                    if (attribute.equals("target.id") && event.getApplication().equals("Excel")) {
                        var uniqueColumns = events.stream().map(el -> el.payload.get("target.column")).distinct().collect(Collectors.toList());
                        var uniqueRows = events.stream().map(el -> el.payload.get("target.row")).distinct().collect(Collectors.toList());
                        if (uniqueColumns.size() < uniqueRows.size())
                            attribute = "target.column";
                        else
                            attribute = "target.row";
                    }
                    context.put(attribute, event.payload.get(attribute));
                }
                event.context = new HashMap<>(context);
        }
    }

    public static List<String> toSequence(List<Event> events, List<String> contextAttributes){
        List<String> sequence = new ArrayList<>();
        HashMap<String, List<Event>> groupedEvents = groupByEventType(events);
        for(var group: groupedEvents.keySet())
            Utils.setContextAttributes(groupedEvents.get(group), contextAttributes);
        for(var event: events)
            sequence.add(new Node(event.getEventType(), event.context, 1).toString());
        return sequence;
    }


    public static List<List<String>> toSequences(HashMap<Integer, List<Event>> cases, List<String> contextAttributes){
        List<List<String>> sequences = new ArrayList<>();

        List<Event> events = new ArrayList<>();
        cases.values().forEach(events::addAll);

        HashMap<String, List<Event>> groupedEvents = groupEvents(events);
        for(var group: groupedEvents.keySet())
            Utils.setContextAttributes(groupedEvents.get(group), contextAttributes);
        for(var caseID: cases.keySet()){
            List<String> sequence = new ArrayList<>();
            for(var event: cases.get(caseID))
                sequence.add(new Node(event.getEventType(), event.context, 1).toString());
            sequences.add(sequence);
        }
        return sequences;
    }

    public static List<String> toSequence(List<Event> events){
        List<String> sequence = new ArrayList<>();
        for(var event: events)
            sequence.add(new Node(event.getEventType(), event.context, 1).toString());
        return sequence;
    }

    /* Summary */

    /*
    public static double getEditDistance(HashMap<Integer, List<Event>> discoveredSegments,  HashMap<Integer, List<Event>> originalTraces){
        List<Double> editDistances = new ArrayList<>();
        for(var caseID: discoveredSegments.keySet()){
            Pattern pattern = new Pattern(toSequence(discoveredSegments.get(caseID)));
            List<List<Event>> coveredTraces = new ArrayList<>();
            int startIdx = discoveredSegments.get(caseID).get(0).getID();
            int endIdx = discoveredSegments.get(caseID).get(discoveredSegments.get(caseID).size()-1).getID();
            for(var trace: originalTraces.keySet()){
                var ids = originalTraces.get(trace).stream().map(el -> el.getID()).filter(el -> el >= startIdx &&
                        el <= endIdx).collect(Collectors.toList());
                if(ids.size() > 0){
                    if(ids.contains(endIdx)){
                        coveredTraces.add(originalTraces.get(trace));
                        break;
                    }
                    else
                        coveredTraces.add(originalTraces.get(trace));
                }
            }
            double editDistance = Double.MAX_VALUE;
            for(int i = 0; i < coveredTraces.size(); i++){
                var trace = toSequence(coveredTraces.get(i));
                var dist = (double)pattern.LevenshteinDistance(pattern.getPattern(), trace)/Math.max(discoveredSegments.get(caseID).size(),
                        trace.size());
                if(dist < editDistance)
                    editDistance = dist;
            }
            editDistances.add(editDistance);
        }
        var meanEditDistance = editDistances.stream().mapToDouble(d -> d).average().orElse(0.0);
        return meanEditDistance;
    }

    public static void getSummary(List<Pattern> patterns, List<List<String>> groundTruth, List<Event> events){
        int i = 1;
        for(var pattern: patterns){
            pattern.assignClosestMatch(groundTruth);
            pattern.computeConfusionMatrix(events);
            System.out.println("\nPattern " + i + ":\n" + pattern + "\n" + pattern.getClosestMatch());
            System.out.println("Length = " + pattern.getLength());
            System.out.printf("Sup = %.2f\n", pattern.getRelativeSupport());
            System.out.printf("Coverage = %.2f\n", pattern.getCoverage());
            System.out.printf("Precision = %.3f\n", pattern.calculatePrecision());
            System.out.printf("Recall = %.3f\n", pattern.calculateRecall());
            System.out.printf("Accuracy = %.3f\n", pattern.calculateAccuracy());
            System.out.printf("F-score = %.3f\n", pattern.calculateFScore());
            System.out.printf("Jaccard = %.3f\n", pattern.calculateJaccard(groundTruth, events));
            i++;
        }
        System.out.println("\nOverall results:\n");
        System.out.printf("Average length = %.2f\n", patterns.stream().mapToInt(Pattern::getLength).average().orElse(0.0));
        System.out.printf("Average support = %.2f\n", patterns.stream().mapToDouble(Pattern::getRelativeSupport).average().orElse(0.0));
        System.out.printf("Total coverage = %.2f\n", patterns.stream().mapToDouble(Pattern::getCoverage).sum());
        System.out.printf("Average coverage = %.2f\n", patterns.stream().mapToDouble(Pattern::getCoverage).average().orElse(0.0));
        System.out.printf("Average precision = %.3f\n", patterns.stream().mapToDouble(Pattern::getPrecision).average().orElse(0.0));
        System.out.printf("Average recall = %.3f\n", patterns.stream().mapToDouble(Pattern::getRecall).average().orElse(0.0));
        System.out.printf("Average accuracy = %.3f\n", patterns.stream().mapToDouble(Pattern::getAccuracy).average().orElse(0.0));
        System.out.printf("Average f-score = %.3f\n", patterns.stream().mapToDouble(Pattern::getFscore).average().orElse(0.0));
        System.out.printf("Average Jaccard = %.3f\n", patterns.stream().mapToDouble(Pattern::getJaccard).average().orElse(0.0));
    }*/

    public static List<String>  extractAttributes(List<Event> events){
        List<String> attributes = new ArrayList<>();
        for(int i = 0; i < events.size(); i++){
            for(String attr: events.get(i).getAttributes())
                if(!attributes.contains(attr))
                    attributes.add(attr);
        }
        return attributes;
    }

    public static List<String> extractContextAttributes(String filePath){
        List<String> contextAttributes = new ArrayList<>();
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(filePath));
            JSONObject jsonObject = (JSONObject) obj;
            JSONArray context = (JSONArray) jsonObject.get("context");

            if (context != null) {
                for (int i = 0; i < context.size(); i++){
                    contextAttributes.add(context.get(i).toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contextAttributes;
    }


}