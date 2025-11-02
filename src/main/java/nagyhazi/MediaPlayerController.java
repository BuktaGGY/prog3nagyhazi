package nagyhazi;

// Importok letisztítva: BufferedInputStream, FileInputStream, SourceDataLine eltávolítva
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

// ÚJ IMPORT-ok a rendszer vágólaphoz
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class MediaPlayerController {
    private List<MediaFile> mediaLibrary;
    private MediaFile currentMedia;
    private Clip audioClip;
    private File clipboard;
    private boolean isCutOperation;
    private boolean isPaused = false;
    
    public MediaPlayerController() {
        this.mediaLibrary = new ArrayList<>();
        this.clipboard = null;
        this.isCutOperation = false;
    }
    
    public boolean playMedia(MediaFile mediaFile) {
        stopCurrentMedia();
        isPaused = false;
        
        try {
            // Check if file exists
            if (!Files.exists(mediaFile.getFilePath())) {
                showErrorDialog("File not found: " + mediaFile.getFilePath());
                return false;
            }
            
            String fileName = mediaFile.getFileName().toLowerCase();
            
            if (fileName.endsWith(".mp3")) {
                return playMP3File(mediaFile);
            } else if (fileName.endsWith(".wav") || fileName.endsWith(".au") || fileName.endsWith(".aiff")) {
                return playSupportedAudioFile(mediaFile);
            } else {
                showErrorDialog("Unsupported file format: " + fileName);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error playing media: " + e.getMessage());
            showErrorDialog("Error playing: " + mediaFile.getFileName() + "\n" + e.getMessage());
            return false;
        }
    }
    
    private boolean playMP3File(MediaFile mediaFile) {
        try {
            // Ez a rész csak akkor működik, ha az MP3 SPI (pl. mp3spi.jar) a classpath-on van
            File audioFile = mediaFile.getFilePath().toFile();
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat baseFormat = audioInputStream.getFormat();
            
            // Konvertálás PCM formátumra, amit a Clip kezelni tud
            AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false
            );
            
            AudioInputStream decodedInputStream = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);
            
            // Clip létrehozása és megnyitása
            DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
            audioClip = (Clip) AudioSystem.getLine(info);
            audioClip.open(decodedInputStream);
            
            // Listener a lejátszás végére
            audioClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && !isPaused) {
                    System.out.println("Playback finished: " + mediaFile.getFileName());
                    currentMedia = null;
                }
            });
            
            audioClip.start();
            currentMedia = mediaFile;
            
            System.out.println("Now playing MP3: " + mediaFile.getFileName());
            // A felugró ablak (showInfoDialog) eltávolítva, mert a státuszbár jobb erre
            return true;
            
        } catch (UnsupportedAudioFileException e) {
            // Ez a hiba akkor jön, ha nincs MP3 SPI telepítve
            System.err.println("MP3 format not supported by this Java installation");
            showErrorDialog("Cannot play MP3 file: " + mediaFile.getFileName() + 
                          "\nYour Java installation does not support MP3 playback.\n" +
                          "Please add MP3 SPI libraries (e.g., mp3spi.jar and jl.jar) to your project.");
            return false;
        } catch (Exception e) {
            // Egyéb hibák (pl. fájl olvasás)
            System.err.println("Error playing MP3: " + e.getMessage());
            showErrorDialog("Error playing MP3 file: " + mediaFile.getFileName() + "\n" + e.getMessage());
            return false;
        }
    }
    
    
    
    private boolean playSupportedAudioFile(MediaFile mediaFile) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(mediaFile.getFilePath().toFile());
            audioClip = AudioSystem.getClip();
            audioClip.open(audioInputStream);
            
            // Add playback completion listener
            audioClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && !isPaused) {
                    System.out.println("Playback finished: " + mediaFile.getFileName());
                    currentMedia = null;
                }
            });
            
            audioClip.start();
            currentMedia = mediaFile;
            
            System.out.println("Now playing: " + mediaFile.getFileName());
            return true;
            
        } catch (Exception e) {
            System.err.println("Error playing audio file: " + e.getMessage());
            showErrorDialog("Error playing: " + mediaFile.getFileName());
            return false;
        }
    }
    
    
    private void showErrorDialog(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, "Playback Error", JOptionPane.ERROR_MESSAGE);
        });
    }
    
    public void pauseMedia() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            isPaused = true;
            System.out.println("Playback paused");
        }
    }
    
    public void resumeMedia() {
        if (audioClip != null && !audioClip.isRunning() && isPaused) {
            audioClip.start();
            isPaused = false;
            System.out.println("Playback resumed");
        }
    }
    
    public void stopCurrentMedia() {
        if (audioClip != null) {
            audioClip.stop();
            audioClip.close();
            audioClip = null;
            isPaused = false;
            System.out.println("Playback stopped");
        }
        currentMedia = null;
    }
    
    public long getCurrentPosition() {
        if (audioClip != null) {
            return audioClip.getMicrosecondPosition() / 1000; // Convert to milliseconds
        }
        return 0;
    }
    
    public long getDuration() {
        if (audioClip != null) {
            return audioClip.getMicrosecondLength() / 1000; // Convert to milliseconds
        }
        return 0;
    }
    
    public boolean isPlaying() {
        return audioClip != null && audioClip.isRunning();
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public boolean deleteMedia(MediaFile mediaFile) {
        try {
            Files.deleteIfExists(mediaFile.getFilePath());
            boolean removed = mediaLibrary.remove(mediaFile);
            if (currentMedia == mediaFile) {
                stopCurrentMedia();
            }
            return removed;
        } catch (IOException e) {
            System.err.println("Error deleting file: " + e.getMessage());
            showErrorDialog("Error deleting file: " + mediaFile.getFileName());
            return false;
        }
    }
    
    public boolean renameMedia(MediaFile mediaFile, String newName) {
        try {
            Path newPath = mediaFile.getFilePath().resolveSibling(newName);
            Files.move(mediaFile.getFilePath(), newPath, StandardCopyOption.REPLACE_EXISTING);
            mediaFile.setFileName(newName);
            mediaFile.setFilePath(newPath);
            return true;
        } catch (IOException e) {
            System.err.println("Error renaming file: " + e.getMessage());
            showErrorDialog("Error renaming file: " + mediaFile.getFileName());
            return false;
        }
    }
    
    public void copyMedia(MediaFile mediaFile) {
        clipboard = mediaFile.getFilePath().toFile();
        isCutOperation = false;
        System.out.println("File copied: " + mediaFile.getFileName());
    }
    
    public void cutMedia(MediaFile mediaFile) {
        clipboard = mediaFile.getFilePath().toFile();
        isCutOperation = true;
        System.out.println("File cut: " + mediaFile.getFileName());
    }
    
    /**
     * MODOSÍTOTT METÓDUS:
     * Beilleszt fájl(oka)t a célmappába.
     * Először a rendszer vágólapját ellenőrzi. Ha ott van(nak) fájl(ok),
     * azt/azokat másolja be.
     * Ha a rendszer vágólapja üres vagy nem fájl(oka)t tartalmaz,
     * akkor a program belső vágólapját (copy/cut) használja.
     * * @param destinationDir A célmappa (pl. a "media" mappa)
     * @return true, ha a beillesztés sikeres volt, egyébként false
     */
    public boolean pasteMedia(Path destinationDir) {
        Clipboard sysClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable clipboardContents = sysClipboard.getContents(null);

        try {
            // 1. Rendszer vágólap ellenőrzése (DataFlavor.javaFileListFlavor)
            if (clipboardContents != null && clipboardContents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                
                @SuppressWarnings("unchecked") // A DataFlavor.javaFileListFlavor garantálja a List<File> típust
                List<File> files = (List<File>) clipboardContents.getTransferData(DataFlavor.javaFileListFlavor);
                
                if (files != null && !files.isEmpty()) {
                    boolean allPasted = true;
                    for (File file : files) {
                        // Csak fájlokat illesztünk be, mappákat nem.
                        // A rendszer vágólapról mindig "Copy" műveletet végzünk (isCut = false)
                        if (file.isFile()) {
                            if (!pasteSingleFile(file, destinationDir, false)) {
                                allPasted = false; // Ha egy is hibára fut, jelezzük
                            }
                        }
                    }
                    return allPasted;
                }
            }

            // 2. Visszaesés a belső vágólapra (ha a rendszer vágólapon nem volt fájl)
            if (this.clipboard != null) {
                // Itt használjuk a belsőleg tárolt 'isCutOperation' állapotot
                return pasteSingleFile(this.clipboard, destinationDir, this.isCutOperation);
            }

        } catch (UnsupportedFlavorException | IOException e) {
            System.err.println("Error reading system clipboard: " + e.getMessage());
            showErrorDialog("Error pasting from clipboard: " + e.getMessage());
            return false;
        }
        
        // 3. Ha sem a rendszer, sem a belső vágólapon nincs semmi
        showErrorDialog("No file in clipboard to paste");
        return false;
    }

    /**
     * ÚJ PRIVÁT METÓDUS:
     * A korábbi 'pasteMedia' logikája, amely egyetlen fájlt kezel,
     * beleértve az átnevezést ütközés esetén és a "Cut" műveletet.
     * * @param sourceFile A forrásfájl (a vágólapról)
     * @param destinationDir A célmappa
     * @param isCut true, ha "Cut" (áthelyezés) művelet, false, ha "Copy" (másolás)
     * @return true, ha a művelet sikeres
     */
    private boolean pasteSingleFile(File sourceFile, Path destinationDir, boolean isCut) {
        if (sourceFile == null) {
            showErrorDialog("No file source to paste");
            return false;
        }
        
        try {
            Path sourcePath = sourceFile.toPath();
            String originalFileName = sourceFile.getName();
            String baseName = "";
            String extension = "";

            // 1. Fájlnév és kiterjesztés szétválasztása
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
                baseName = originalFileName.substring(0, dotIndex);
                extension = originalFileName.substring(dotIndex); // Pl. ".mp3"
            } else {
                baseName = originalFileName;
                extension = "";
            }

            // 2. Egyedi célfájlnév generálása
            Path destination = destinationDir.resolve(originalFileName);
            int copyIndex = 1;

            // 3. Ciklus, amíg egyedi, nem létező fájlnevet nem találunk
            while (Files.exists(destination)) {
                // Ha "Cut" műveletet végzünk ugyanabba a mappába, az értelmetlen
                if (isCut && sourcePath.equals(destination)) {
                    System.out.println("Cut/Paste in the same folder. Operation cancelled.");
                    this.clipboard = null; // Töröljük a vágólapot
                    this.isCutOperation = false;
                    return true;
                }
                String newName = String.format("%s (%d)%s", baseName, copyIndex, extension);
                destination = destinationDir.resolve(newName);
                copyIndex++;
            }

            // 4. A tényleges másolás (vagy "Cut" esetén áthelyezés)
            if (isCut) {
                // Áthelyezés (gyorsabb, mint a copy+delete)
                Files.move(sourcePath, destination, StandardCopyOption.REPLACE_EXISTING);
                this.clipboard = null; // Belső vágólap törlése
                this.isCutOperation = false;
            } else {
                // Másolás
                Files.copy(sourcePath, destination);
            }

            System.out.println("File pasted to: " + destination);
            return true;

        } catch (IOException e) {
            System.err.println("Error in paste operation: " + e.getMessage());
            showErrorDialog("Error pasting file: " + e.getMessage());
            return false;
        }
    }
    
    public List<MediaFile> getMediaLibrary() { 
        return mediaLibrary; 
    }
    
    public MediaFile getCurrentMedia() { 
        return currentMedia; 
    }
    
    public void addMediaFile(MediaFile mediaFile) {
        mediaLibrary.add(mediaFile);
    }
    
    // Method to check supported audio formats
    public String getSupportedFormats() {
        AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
        StringBuilder sb = new StringBuilder("Supported formats: ");
        for (AudioFileFormat.Type type : types) {
            sb.append(type.getExtension()).append(" ");
        }
        return sb.toString();
    }
}