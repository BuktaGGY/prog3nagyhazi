package nagyhazi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.*;
import java.time.Duration;

public class MediaPlayerTest {
    private MediaFile testFile;
    private MediaPlayerController testController;

    @BeforeEach //minden teszt elott lefut
    void setup(){
        testController = new MediaPlayerController();

        Path path = Paths.get("test_music.mp3");
        Duration duration = Duration.ofMinutes(2).plusSeconds(34);

        long size = 5 * 1024 * 1024; //5 mb

        testFile = new MediaFile("test_music.mp3", path, duration, size, MediaFile.MediaType.MP3);
    }

    @Test
    @DisplayName("idoformazas mukodese")
    void testDurationFormatting(){
        assertEquals("02:34", testFile.getFormattedDuration(), "nem megfelelo a formazas");
    }

    @Test
    @DisplayName("fajlmeret formazas")
    void testFileSizeFormatting(){
        assertEquals("5.0 MB", testFile.getFormattedFileSize(), "nem megfelelo a formazas");
    }

    @Test
    @DisplayName("file torles")
    void testDeleteFile(){
        testController.addMediaFile(testFile);
        assertFalse(testController.getMediaLibrary().isEmpty());

        //csak listalogikat tesztelunk, filemuveletet nem
        testController.deleteMedia(testFile);
        assertTrue(testController.getMediaLibrary().isEmpty(), "a filet nem sikerult torolni");
    }

    @Test
    @DisplayName("hangero szabalyozas")
    void testVolumeLimits() {
        // default
        assertEquals(80, testController.getVolume());

        // normal
        testController.setVolume(50);
        assertEquals(50, testController.getVolume());

        // tul nagy ertek
        testController.setVolume(150);
        assertEquals(100, testController.getVolume(), "a hangerő nem lehet nagyobb 100-nal");

        // tul kicsi ertek
        testController.setVolume(-10);
        assertEquals(0, testController.getVolume(), "a hangero nem lehet negativ");
    }
}
