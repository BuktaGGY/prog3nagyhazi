package nagyhazi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


//CONTROLLER, MEGKAPJA A KÉRÉST ÉS UTASTJA A MEGFELELŐ PROGRAMRÉSZT
public class MediaPlayerController {
    private List<MediaFile> mediaLibrary;
    private MediaFile currentMedia;
    
    // A logikai részek kiszervezve:
    private AudioEngine audioEngine;
    private ClipboardHandler clipboardHandler;

    public MediaPlayerController() { //inicializalas
        this.mediaLibrary = new ArrayList<>();
        this.audioEngine = new AudioEngine();
        this.clipboardHandler = new ClipboardHandler();
    }

    // --- LEJÁTSZÁS KEZELÉS ---

    public boolean playMedia(MediaFile mediaFile) { //igaz, ha le tudott jatszani valamit, hamis, ha nem
        if (!Files.exists(mediaFile.getFilePath())) {
            showError("File not found: " + mediaFile.getFilePath());
            return false;
        }

        try {
            audioEngine.play(mediaFile.getFilePath().toFile()); 
            
            currentMedia = mediaFile; // Megjegyezzük, mi megy éppen
            return true;
        } catch (Exception e) {
            showError("Error playing file: " + e.getMessage());
            return false;
        }
    }

    public void pauseMedia() { audioEngine.pause(); }
    public void resumeMedia() { audioEngine.resume(); }
    public void stopCurrentMedia() { 
        audioEngine.stop(); 
        currentMedia = null; //azért kell, mert nincs külön pause a Clip-nél
    }
    
    public void seekTo(long positionInMillis) { audioEngine.seekTo(positionInMillis); }
    public void setVolume(int percent) { audioEngine.setVolume(percent); }
    public int getVolume() { return audioEngine.getVolume(); }
    
    public long getCurrentPosition() { return audioEngine.getCurrentPosition(); }
    public long getDuration() { return audioEngine.getDuration(); }
    public boolean isPlaying() { return audioEngine.isPlaying(); }
    public boolean isPaused() { return audioEngine.isPaused(); }

    // --- KÖNYVTÁR ÉS FÁJL MŰVELETEK ---

    public void addMediaFile(MediaFile file) { mediaLibrary.add(file); }
    public List<MediaFile> getMediaLibrary() { return mediaLibrary; }
    public MediaFile getCurrentMedia() { return currentMedia; }

    public boolean deleteMedia(MediaFile mediaFile) {
        try {
            if (currentMedia == mediaFile) stopCurrentMedia();
            Files.deleteIfExists(mediaFile.getFilePath());
            return mediaLibrary.remove(mediaFile);
        } catch (IOException e) {
            showError("Error deleting file: " + e.getMessage());
            return false;
        }
    }

    public boolean renameMedia(MediaFile mediaFile, String newName) {
        try {
            Path newPath = mediaFile.getFilePath().resolveSibling(newName); //új elérési út az új névvel
            Files.move(mediaFile.getFilePath(), newPath, StandardCopyOption.REPLACE_EXISTING); //fizikai áthelyezés (erre van visszavezetve az átnevezés), magától törlődik a régi
            mediaFile.setFileName(newName);
            mediaFile.setFilePath(newPath);
            return true;
        } catch (IOException e) {
            showError("Error renaming file: " + e.getMessage());
            return false;
        }
    }

    // --- VÁGÓLAP KEZELÉS ---

    public void copyMedia(MediaFile mediaFile) {
        clipboardHandler.copy(mediaFile.getFilePath().toFile());
    }

    public void cutMedia(MediaFile mediaFile) {
        clipboardHandler.cut(mediaFile.getFilePath().toFile());
    }

    public boolean pasteMedia(Path destinationDir) {
        String error = clipboardHandler.paste(destinationDir);
        if (error != null) {
            showError(error);
            return false;
        }
        return true;
    }

    private void showError(String msg) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE));
    }
}