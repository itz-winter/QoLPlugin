package com.kelpwing.kelpylandiaplugin.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

public class DurationParser {
    
    // Pattern to match time units like 1y, 1w, 5d, 3h, 30m, 45s
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([ywdhms])");
    
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return -1; // Permanent
        }

        // Handle infinite/permanent keywords
        String lowerInput = input.toLowerCase();
        if (lowerInput.equals("inf") || lowerInput.equals("infinite") || 
            lowerInput.equals("perm") || lowerInput.equals("permanent")) {
            return -1; // Permanent
        }

        // Handle simple numeric input (assume minutes for backward compatibility)
        if (input.matches("\\d+")) {
            long minutes = Long.parseLong(input);
            return minutes * 1000 * 60; // Convert to milliseconds
        }

        long totalMilliseconds = 0;
        
        // Find all time components in the input string
        Matcher matcher = TIME_PATTERN.matcher(input.toLowerCase());
        
        if (!matcher.find()) {
            return -1; // Invalid format
        }
        
        // Reset matcher to find all matches
        matcher.reset();
        
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            char unit = matcher.group(2).charAt(0);
            
            long milliseconds = switch (unit) {
                case 's' -> value * 1000;                           // seconds
                case 'm' -> value * 1000 * 60;                      // minutes
                case 'h' -> value * 1000 * 60 * 60;                 // hours
                case 'd' -> value * 1000 * 60 * 60 * 24;            // days
                case 'w' -> value * 1000 * 60 * 60 * 24 * 7;        // weeks
                case 'y' -> value * 1000 * 60 * 60 * 24 * 365;      // years (365 days)
                default -> 0;
            };
            
            totalMilliseconds += milliseconds;
        }
        
        return totalMilliseconds > 0 ? totalMilliseconds : -1;
    }
    
    /**
     * Get suggestions for tab completion
     */
    public static List<String> getTabCompletions(String partial) {
        List<String> completions = new ArrayList<>();
        
        // If the partial string is empty or just numbers, suggest time unit examples
        if (partial.isEmpty() || partial.matches("\\d*")) {
            completions.add("inf");
            completions.add("permanent");
            completions.add("1m");
            completions.add("5m");
            completions.add("10m");
            completions.add("30m");
            completions.add("1h");
            completions.add("2h");
            completions.add("6h");
            completions.add("12h");
            completions.add("1d");
            completions.add("3d");
            completions.add("7d");
            completions.add("1w");
            completions.add("1y");
        }
        // If it ends with a number, suggest unit completions
        else if (partial.matches(".*\\d+$")) {
            String base = partial;
            completions.add(base + "s");
            completions.add(base + "m");
            completions.add(base + "h");
            completions.add(base + "d");
            completions.add(base + "w");
            completions.add(base + "y");
        }
        // If it has existing time units, suggest compound examples
        else if (partial.matches(".*\\d+[ywhms].*")) {
            // Suggest compound time examples
            if (!partial.contains("s")) completions.add(partial + "30s");
            if (!partial.contains("m")) completions.add(partial + "30m");
            if (!partial.contains("h")) completions.add(partial + "12h");
            if (!partial.contains("d")) completions.add(partial + "7d");
            if (!partial.contains("w")) completions.add(partial + "1w");
            if (!partial.contains("y")) completions.add(partial + "1y");
        }
        
        return completions;
    }
    
    /**
     * Validate if a duration string is valid
     */
    public static boolean isValidDuration(String input) {
        return parseDuration(input) != -1;
    }
    
    /**
     * Format examples for help text
     */
    public static String getExamples() {
        return "Examples: inf, permanent, 30s, 5m, 1h, 2d, 1w, 1y, 1w2d5h30m (compound time)";
    }
}
