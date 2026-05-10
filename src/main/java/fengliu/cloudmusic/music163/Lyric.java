package fengliu.cloudmusic.music163;

import com.google.gson.JsonObject;
import fengliu.cloudmusic.command.MusicCommand;
import fengliu.cloudmusic.util.MusicPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 滚动歌词
 */
public class Lyric implements Runnable{
    private final Map<Long, String> lyric;
    private final Map<Long, String> tlyric;
    private boolean loopIn = true;
    private boolean load = true;
    private volatile String[] toLyric = {};

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
            int millisecond = Integer.parseInt(secondStr[1]) * 10;
            return minute + second + millisecond;
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
        if(lyric.equals("")){
            this.lyric = new LinkedHashMap<>();
            this.tlyric = this.lyric;
            return;
        }

        this.lyric = lyricToMap(lyric);
        if(!data.has("tlyric")){
            this.tlyric = new LinkedHashMap<>();
            return;
        }

        String tlyric = data.getAsJsonObject("tlyric").get("lyric").getAsString();
        if(tlyric.equals("")){
            this.tlyric = new LinkedHashMap<>();
            return;
        }

        this.tlyric = lyricToMap(data.getAsJsonObject("tlyric").get("lyric").getAsString());
    }

    @Override
    public void run() {
        if (this.lyric.isEmpty()) {
            return;
        }

        MusicPlayer player = MusicCommand.getPlayer();
        long[] times = new long[this.lyric.size()];
        String[] texts = new String[this.lyric.size()];
        String[] trans = new String[this.lyric.size()];
        int i = 0;
        for (Map.Entry<Long, String> entry : this.lyric.entrySet()) {
            times[i] = entry.getKey();
            texts[i] = entry.getValue();
            trans[i] = this.tlyric.get(entry.getKey());
            i++;
        }

        while (this.loopIn) {
            synchronized (this) {
                while (!this.load) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }

            long time = player.getPlayingProgress();
            int index = -1;
            for (int j = 0; j < times.length && times[j] <= time; j++) {
                index = j;
            }

            if (index >= 0) {
                String t = trans[index];
                if (t != null) {
                    this.toLyric = new String[]{texts[index], t};
                } else {
                    this.toLyric = new String[]{texts[index]};
                }
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * 获取当前滚动到的歌词
     * @return 歌词
     */
    public String[] getToLyric() {
        return toLyric;
    }

    /**
     * 开始歌词滚动
     */
    public void start(){
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("CloudMusicLyric thread");
        thread.start();
    }

    /**
     * 退出歌词滚动
     */
    public void exit(){
        this.loopIn = false;
        synchronized(this){
            this.load = true;
            notifyAll();
        }
    }

    /**
     * 暂停歌词滚动
     */
    public void stop(){
        synchronized(this){
            this.load = false;
            notifyAll();
        }
    }

    /**
     * 继续歌词滚动
     */
    public void continues(){
        synchronized(this){
            this.load = true;
            notifyAll();
        }
    }
}
