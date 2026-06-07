package com.example.kfmusic.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcParser {
    public static class LrcLine implements Comparable<LrcLine> {
        public final long timestamp;
        public final String text;

        public LrcLine(long timestamp, String text) {
            this.timestamp = timestamp;
            this.text = text;
        }

        @Override
        public int compareTo(LrcLine o) {
            return Long.compare(this.timestamp, o.timestamp);
        }
    }

    private static final Pattern PATTERN_TIME = Pattern.compile("\\[(\\d+):(\\d+)\\.(\\d+)\\]");

    public static List<LrcLine> parse(InputStream is) {
        List<LrcLine> lines = new ArrayList<>();
        if (is == null) return lines;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                Matcher matcher = PATTERN_TIME.matcher(line);
                int lastMatchEnd = 0;
                List<Long> times = new ArrayList<>();
                while (matcher.find()) {
                    long min = Long.parseLong(matcher.group(1));
                    long sec = Long.parseLong(matcher.group(2));
                    long ms = Long.parseLong(matcher.group(3));
                    if (matcher.group(3).length() == 2) {
                        ms = ms * 10;
                    }
                    long time = min * 60000 + sec * 1000 + ms;
                    times.add(time);
                    lastMatchEnd = matcher.end();
                }
                if (!times.isEmpty()) {
                    String content = line.substring(lastMatchEnd).trim();
                    for (long t : times) {
                        lines.add(new LrcLine(t, content));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(lines);
        return lines;
    }
}
