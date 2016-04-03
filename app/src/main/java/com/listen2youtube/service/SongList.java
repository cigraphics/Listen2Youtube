package com.listen2youtube.service;

import com.listen2youtube.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by khang on 30/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class SongList {
    List<SongInfo> songInfoList = new ArrayList<>();
    int lastPlayedPosition = -1;

    /**
     * @return next song, null mean all song were played
     */
    public SongInfo nextSong() {
        if (songInfoList.size() == 0)
            return null;
        boolean isLoop = Settings.isRepeat(),
                isRandom = Settings.isShuffle();
        Random mRandom = new Random();
        if (!isLoop && !isRandom && lastPlayedPosition >= songInfoList.size() - 1)
            return null;
        if (isRandom) {
            lastPlayedPosition = mRandom.nextInt(songInfoList.size());
            return songInfoList.get(lastPlayedPosition);
        } else {
            if (lastPlayedPosition >= songInfoList.size() - 1 && isLoop)
                return songInfoList.get(lastPlayedPosition = 0);
            else
                return songInfoList.get(++lastPlayedPosition);
        }
    }
}
