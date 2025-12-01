package nagyhazi;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class MediaMetadataReader {

    // Privát konstruktor, mert ez egy utility osztály (csak statikus metódusai vannak)
    private MediaMetadataReader() {}

    public static Duration getDuration(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        try {
            if (fileName.endsWith(".wav")) {
                return getWavDuration(filePath);
            } else if (fileName.endsWith(".mp3")) {
                return getMP3Duration(filePath);
            }
        } catch (Exception e) {
            System.err.println("Error reading duration for " + fileName + ": " + e.getMessage());
        }
        // Default érték hiba esetén
        return Duration.ofMinutes(3).plusSeconds(30);
    }

    private static Duration getWavDuration(Path filePath) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(filePath.toFile())) {
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            float frameRate = format.getFrameRate();
            
            if (frames != AudioSystem.NOT_SPECIFIED && frameRate != AudioSystem.NOT_SPECIFIED) {
                double durationInSeconds = frames / frameRate; //csak matek, a hossz az összes frame / frame másodpercenként
                return Duration.ofSeconds((long) durationInSeconds);
            }
        }
        return Duration.ZERO;
    }

    private static Duration getMP3Duration(Path filePath) throws IOException, UnsupportedAudioFileException { //mp3spi függőség kell
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(filePath.toFile());
        Map<String, Object> properties = fileFormat.properties();
        if (properties.containsKey("duration")) { //megkeressük, hogy létezik e érték a duration kulcshoz
            long microseconds = (Long) properties.get("duration");
            return Duration.ofNanos(microseconds * 1000);
        }
        return Duration.ZERO;
    }
}