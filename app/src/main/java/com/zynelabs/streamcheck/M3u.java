package com.zynelabs.streamcheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Minimal streaming M3U parser — handles #EXTINF with group-title + display name.
public class M3u {

    public static class Channel {
        public String name;
        public String group;
        public String url;
        public boolean checked;
        public boolean alive;
        public int ms;
    }

    public static List<Channel> parse(BufferedReader br) throws IOException {
        List<Channel> out = new ArrayList<>();
        String line;
        String pendingName = null;
        String pendingGroup = "";
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXTINF")) {
                pendingGroup = attr(line, "group-title");
                int ci = line.lastIndexOf(',');
                pendingName = ci >= 0 ? line.substring(ci + 1).trim() : "Unknown";
                if (pendingName.isEmpty()) pendingName = "Unknown";
            } else if (line.charAt(0) == '#') {
                continue;
            } else if (line.startsWith("http://") || line.startsWith("https://")) {
                Channel ch = new Channel();
                ch.name = pendingName != null ? pendingName : line;
                ch.group = pendingGroup;
                ch.url = line;
                out.add(ch);
                pendingName = null;
                pendingGroup = "";
            }
            // non-http(s) urls (rtmp, etc.) are skipped in v1
        }
        return out;
    }

    static String attr(String line, String key) {
        String k = key + "=\"";
        int i = line.indexOf(k);
        if (i < 0) return "";
        int j = line.indexOf('"', i + k.length());
        return j < 0 ? "" : line.substring(i + k.length(), j);
    }

    // Build a cleaned playlist containing only alive channels.
    public static String buildClean(List<Channel> alive) {
        StringBuilder sb = new StringBuilder("#EXTM3U\n");
        for (Channel ch : alive) {
            sb.append("#EXTINF:-1");
            if (!ch.group.isEmpty()) sb.append(" group-title=\"").append(ch.group).append('"');
            sb.append(',').append(ch.name).append('\n');
            sb.append(ch.url).append('\n');
        }
        return sb.toString();
    }
}
