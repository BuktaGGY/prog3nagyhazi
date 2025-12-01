package nagyhazi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.*;
import java.time.Duration;

public class MediaPlayerTest {
    private MediaFile testFile;
    private MediaPlayerController testController;
    private ClipboardHandler testClipboardHandler; 

    @BeforeEach // minden teszt előtt lefut
    void setup(){
        testController = new MediaPlayerController();
        testClipboardHandler = new ClipboardHandler();
        
        Path path = Paths.get("test_music.mp3"); 
        Duration duration = Duration.ofMinutes(2).plusSeconds(34); // 02:34
        long size = 5 * 1024 * 1024; // 5 MB

        testFile = new MediaFile("test_music.mp3", path, duration, size, MediaFile.MediaType.MP3);
    }

    @Test
    @DisplayName("01. Időformázás (MM:SS)")
    void testDurationFormatting(){
        assertEquals("02:34", testFile.getFormattedDuration(), "Hibás időformázás.");
    }

    @Test
    @DisplayName("02. Fájlméret formázás (MB)")
    void testFileSizeFormatting(){
        assertEquals("5.0 MB", testFile.getFormattedFileSize(), "Hibás méretformázás.");
    }
    
    @Test
    @DisplayName("03. Kis fájlméret formázás (B)")
    void testSmallFileSizeFormatting() {
        testFile.setFileSize(500);
        assertEquals("500 B", testFile.getFormattedFileSize(), "Hibás bájt formázás.");
    }

    @Test
    @DisplayName("04. toString metódus")
    void testToString() {
        assertEquals("test_music.mp3", testFile.toString());
    }

    @Test
    @DisplayName("05. Setter és Getter működése")
    void testSetters() {
        testFile.setFileName("uj_nev.wav");
        assertEquals("uj_nev.wav", testFile.getFileName());
        
        testFile.setMediaType(MediaFile.MediaType.WAV);
        assertEquals(MediaFile.MediaType.WAV, testFile.getMediaType());
    }
    
    @Test
    @DisplayName("06. Könyvtár üres indításkor")
    void testInitialLibraryState() {
        assertTrue(testController.getMediaLibrary().isEmpty(), "A könyvtárnak üresnek kell lennie.");
    }

    @Test
    @DisplayName("07. Fájl hozzáadása")
    void testAddMediaFile() {
        testController.addMediaFile(testFile);
        assertEquals(1, testController.getMediaLibrary().size());
        assertEquals(testFile, testController.getMediaLibrary().get(0));
    }
    
    @Test
    @DisplayName("08. Fájl törlése (Logikai)")
    void testDeleteMediaFile() {
        testController.addMediaFile(testFile);
        testController.deleteMedia(testFile);
        assertTrue(testController.getMediaLibrary().isEmpty(), "A listából törlődnie kellett volna az elemnek.");
    }

    @Test
    @DisplayName("09. CurrentMedia alapállapota")
    void testCurrentMediaNull() {
        assertNull(testController.getCurrentMedia(), "Induláskor nem lehet kiválasztott zene.");
    }

    @Test
    @DisplayName("10. Hangerő korlátozás")
    void testVolumeLimits() {
        testController.setVolume(200);
        assertEquals(100, testController.getVolume());
        
        testController.setVolume(-50);
        assertEquals(0, testController.getVolume());
    }

    @Test
    @DisplayName("11. Lejátszó állapot (isPlaying) alapértelmezés")
    void testIsPlayingDefault() {
        // Ez a teszt ellenőrzi az AudioEngine állapotát a Controlleren keresztül
        // anélkül, hogy grafikus felületet igényelne.
        assertFalse(testController.isPlaying(), "Induláskor nem játszhat semmi.");
        assertFalse(testController.isPaused(), "Induláskor nem lehet szüneteltetve.");
    }

    @Test
    @DisplayName("12. WAV típus kezelése")
    void testWavMediaType() {
        // Ellenőrizzük, hogy ha WAV-ként hozzuk létre, akkor az is marad-e
        MediaFile wavFile = new MediaFile("test.wav", Paths.get("test.wav"), Duration.ZERO, 1000, MediaFile.MediaType.WAV);
        assertEquals(MediaFile.MediaType.WAV, wavFile.getMediaType(), "A WAV típus beállítása nem sikerült.");
    }

    @Test
    @DisplayName("13. Nulla méretű fájl formázása")
    void testZeroFileSize() {
        // Mi történik, ha egy fájl 0 bájtos?
        testFile.setFileSize(0);
        assertEquals("0 B", testFile.getFormattedFileSize(), "A 0 bájtos fájl formázása hibás.");
    }

    @Test
    @DisplayName("14. Nem létező fájl lejátszása")
    void testPlayNonExistentFile() {
        // Olyan fájlt próbálunk lejátszani, ami nincs a lemezen.
        // A playMedia metódusnak false-al kell visszatérnie (Files.exists ellenőrzés miatt).
        Path ghostPath = Paths.get("nem_letezo_fajl.mp3");
        MediaFile ghostFile = new MediaFile("ghost", ghostPath, Duration.ZERO, 0, MediaFile.MediaType.MP3);
        
        boolean result = testController.playMedia(ghostFile);
        assertFalse(result, "A nem létező fájl lejátszásának el kell hasalnia.");
    }

    @Test
    @DisplayName("15. Nem létező fájl átnevezése")
    void testRenameNonExistentFile() {
        // Olyan fájlt próbálunk átnevezni, ami fizikailag nincs ott.
        // A renameMedia metódusnak false-al kell visszatérnie (kivételkezelés miatt).
        Path ghostPath = Paths.get("nem_letezo_fajl.mp3");
        MediaFile ghostFile = new MediaFile("ghost", ghostPath, Duration.ZERO, 0, MediaFile.MediaType.MP3);
        
        boolean result = testController.renameMedia(ghostFile, "uj_nev.mp3");
        assertFalse(result, "Nem létező fájl átnevezése nem sikerülhet.");
    }

    @Test
    @DisplayName("16. Komplex Életciklus: Létrehozás -> Átnevezés -> Törlés")
    void testComplexFileLifecycle() throws java.io.IOException {
        // 1. ELŐKÉSZÜLET: Létrehozunk egy igazi, fizikai fájlt a teszthez
        // (Ez azért kell, hogy a rename és delete műveletek tényleg lefussanak)
        Path tempPath = Paths.get("temp_test_lifecycle.wav");
        if (!Files.exists(tempPath)) {
            Files.createFile(tempPath);
        }

        // Létrehozunk hozzá egy MediaFile objektumot
        MediaFile realFile = new MediaFile("temp_test_lifecycle.wav", tempPath, Duration.ZERO, 0, MediaFile.MediaType.WAV);

        // 2. HOZZÁADÁS
        testController.addMediaFile(realFile);
        assertEquals(1, testController.getMediaLibrary().size(), "A listának tartalmaznia kell az új fájlt.");

        // 3. ÁTNEVEZÉS TESZT (A legkritikusabb rész)
        String newName = "renamed_test_file.wav";
        Path expectedNewPath = Paths.get(newName);
        
        // Végrehajtjuk az átnevezést
        boolean renameSuccess = testController.renameMedia(realFile, newName);

        // Ellenőrzések:
        assertTrue(renameSuccess, "Az átnevezésnek sikerülnie kell.");
        assertEquals(newName, realFile.getFileName(), "A memóriában lévő névnek frissülnie kell.");
        assertEquals(expectedNewPath.toAbsolutePath(), realFile.getFilePath().toAbsolutePath(), "A memóriában lévő útvonalnak frissülnie kell.");
        
        // Fájlrendszer ellenőrzése:
        assertTrue(Files.exists(expectedNewPath), "Az új nevű fájlnak léteznie kell a lemezen.");
        assertFalse(Files.exists(tempPath), "A régi nevű fájlnak el kell tűnnie a lemezről.");

        // 4. TÖRLÉS TESZT
        // Most töröljük az átnevezett fájlt
        boolean deleteSuccess = testController.deleteMedia(realFile);

        assertTrue(deleteSuccess, "A törlésnek sikerülnie kell.");
        assertTrue(testController.getMediaLibrary().isEmpty(), "A listának üresnek kell lennie.");
        assertFalse(Files.exists(expectedNewPath), "A fájlnak fizikailag is törlődnie kellett.");

        // Biztonsági takarítás (ha bármi hiba történt volna közben)
        Files.deleteIfExists(tempPath);
        Files.deleteIfExists(expectedNewPath);
    }
}