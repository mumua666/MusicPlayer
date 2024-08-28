package edu.szu.musicplayer;

import java.io.File;

public class PlayingFile {
    private File currentPlayingFile;
    private boolean isLoveSong = false;

    public PlayingFile(File currentPlayingFile, boolean isLoveSong) {
        this.currentPlayingFile = currentPlayingFile;
        this.isLoveSong = isLoveSong;
    }

    public PlayingFile() {

    }

    public boolean getStatus() {
        return isLoveSong;
    }

    public File getFile() {
        return currentPlayingFile;
    }

    public void setFile(File file) {
        this.currentPlayingFile = file;
    }

    public void setStatus(boolean status) {
        this.isLoveSong = status;
    }
}
