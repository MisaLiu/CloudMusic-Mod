package fengliu.cloudmusic.music163;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 滚动歌词
 */
public class Lyric {
    private final long[] times;
    private final String[] texts;
    private final String[] trans;
    private volatile String[] toLyric = {};
    private int lastIndex = -1;

    /**
     * 将歌词时间字符串转换为毫秒
     * @param n 歌词时间字符串
     * @return 歌词时间毫秒
     */
    public static long timeStrToTime(String n){
        try{
            String[] timeStr = n.split(":");
            String[] secondStr = timeStr[1].split("\\.");

            int minute = Integer.parseInt(timeStr[0]) * 60 * 1000;
            int second = Integer.parseInt(secondStr[0]) * 1000;
            int frac = Integer.parseInt(secondStr[1]);
            if (secondStr[1].length() <= 2) {
                frac *= 10;
            }
            return minute + second + frac;
        }catch (Exception err){
            return 0;
        }
    }

    /**
     * 处理歌词字符串
     * @param lyric 歌词字符串
     * @return 歌词 Map
     */
    public static Map<Long, String> lyricToMap(String lyric){
        Map<Long, String> lyricMap = new LinkedHashMap<>();
        for (String lyricRow : lyric.split("\n")) {
            try {
                String[] lyricRows = lyricRow.substring(1).split("]", 2);
                if (lyricRows.length < 2){
                    continue;
                }

                lyricMap.put(timeStrToTime(lyricRows[0]), lyricRows[1]);
            }catch(Exception err){
            }
        }
        return lyricMap;
    }

    public Lyric(JsonObject data){
        String lyric = data.getAsJsonObject("lrc").get("lyric").getAsString();
        if (lyric.equals("")) {
            this.times = new long[0];
            this.texts = new String[0];
            this.trans = new String[0];
            return;
        }

        Map<Long, String> lyricMap = lyricToMap(lyric);
        Map<Long, String> tlyricMap;
        if (data.has("tlyric")) {
            String tlyric = data.getAsJsonObject("tlyric").get("lyric").getAsString();
            tlyricMap = tlyric.isEmpty() ? new LinkedHashMap<>() : lyricToMap(tlyric);
        } else {
            tlyricMap = new LinkedHashMap<>();
        }

        this.times = new long[lyricMap.size()];
        this.texts = new String[lyricMap.size()];
        this.trans = new String[lyricMap.size()];
        int i = 0;
        for (Map.Entry<Long, String> entry : lyricMap.entrySet()) {
            times[i] = entry.getKey();
            texts[i] = entry.getValue();
            trans[i] = tlyricMap.get(entry.getKey());
            i++;
        }
    }

    /**
     * 根据播放进度更新当前歌词行
     * @param playingProgress 播放进度 (毫秒)
     */
    public void update(long playingProgress) {
        int index = -1;
        for (int i = 0; i < times.length && times[i] <= playingProgress; i++) {
            index = i;
        }

        if (index > lastIndex) {
            if (trans[index] != null) {
                this.toLyric = new String[]{texts[index], trans[index]};
            } else {
                this.toLyric = new String[]{texts[index]};
            }
            lastIndex = index;
        }
    }

    /**
     * 获取当前滚动到的歌词
     * @return 歌词
     */
    public String[] getToLyric() {
        return toLyric;
    }
}
