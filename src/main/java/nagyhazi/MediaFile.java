package nagyhazi;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.Duration;

/**
 *
 * @author bukta
 */

public class MediaFile implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String fileName;
    private Path filePath;
    private Duration duration;
    private long fileSize;
    private MediaType mediaType;
    
    public enum MediaType {
        MP3, WAV
    }
    
    public MediaFile(String fileName, Path filePath, Duration duration, long fileSize, MediaType mediaType) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.duration = duration;
        this.fileSize = fileSize;
        this.mediaType = mediaType;
    }
    
    // Getters and setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public Path getFilePath() { return filePath; }
    public void setFilePath(Path filePath) { this.filePath = filePath; }
    
    public Duration getDuration() { return duration; }
    public void setDuration(Duration duration) { this.duration = duration; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
    
    public String getFormattedDuration() {
        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        else if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        else return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }
    
    @Override
    public String toString() {
        return fileName;
    }
}