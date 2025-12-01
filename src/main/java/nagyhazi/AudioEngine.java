package nagyhazi;

import java.io.File;
import javax.sound.sampled.*;


//ZENELEJÁTSZÁS
public class AudioEngine {
    private Clip audioClip; //Clip -> teljes hangfilet betólti a memoriába lejátszás előtt, könnyű pl: benne tekerni (seek)
    private boolean isPaused = false;
    private int currentVolumePercent = 80;

    public void play(File file) throws Exception {
        stop(); // Előző leállítása
        
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        AudioFormat baseFormat = audioInputStream.getFormat();

        // MP3 esetén dekódolás PCM-re, ezt tudja kezelni a program, azzal, hogy MP3 nem tud semmit csinalni
        //Ezt az MP3SPI dekóderrel csináljuk
        //WAV-ra nem kell külön, mert a java tudja kezelni
        if (file.getName().toLowerCase().endsWith(".mp3")) {
            AudioFormat decodedFormat = new AudioFormat( //átalakítjuk MP3-rol PCM_SIGNED-ra
                AudioFormat.Encoding.PCM_SIGNED, //ez a kicsomagolt, nyers hang, ettől lefele az MP3 formátum tulajdonságait adjuk át
                baseFormat.getSampleRate(), //ugyan az a mintavételezési frekvencia
                16, //PCM hang bitmélysége 16
                baseFormat.getChannels(), //ha mono -> 1, ha stereo -> 2
                baseFormat.getChannels() * 2, //csatorna * sample mérete, mivel sample 16bit = 2 Byte
                baseFormat.getSampleRate(), //másodpercenként hány frame kerül lejátszásra
                false
            ); 
            audioInputStream = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);
            baseFormat = decodedFormat; // Frissítjük a formátumot
        }

        DataLine.Info info = new DataLine.Info(Clip.class, baseFormat); //"igénylőlap", oprendzsertől kérünk egy Clip típusú eszközt
        audioClip = (Clip) AudioSystem.getLine(info); //maga az igénylés, olyan audioeszközt kérünk, ami megfelel az elvárásainknak (info), Line-t adna vissza amit castolunk Clip-re
        audioClip.open(audioInputStream); //betöltjük a (már dekódolt) hangfilet a memóriába
        
        applyVolume(); // Hangerő beállítása indítás előtt

        audioClip.start();
        isPaused = false;
    }

    public void pause() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            isPaused = true;
        }
    }

    public void resume() {
        if (audioClip != null && !audioClip.isRunning() && isPaused) {
            audioClip.start();
            isPaused = false;
        }
    }

    public void stop() {
        if (audioClip != null) {
            audioClip.stop();
            audioClip.close();
            audioClip = null;
        }
        isPaused = false;
    }

    public void seekTo(long positionInMillis) {
        if (audioClip != null && audioClip.isOpen()) {
            long microSeconds = positionInMillis * 1000;
            if (microSeconds > audioClip.getMicrosecondLength()) { //ha tullepnenk, nem lehet csuszka miatt de a biztonság kedvéért
                microSeconds = audioClip.getMicrosecondLength();
            }
            audioClip.setMicrosecondPosition(microSeconds);
        }
    }

    public void setVolume(int percent) { //azért van, hogy ne csak egyes zenékre tudjunk hangerőt állítani, hanem pl lejátszás előtt
        this.currentVolumePercent = Math.max(0, Math.min(100, percent));
        applyVolume();
    }
    
    public int getVolume() {
        return currentVolumePercent;
    }

    private void applyVolume() {
        if (audioClip != null && audioClip.isOpen()) { //Ha éppen játszunk le zenét
            try {
                FloatControl gainControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN); //Clip objektumot tudjuk kontrollálni, most éppen a hangerőt
                float minDB = gainControl.getMinimum();
                float maxDB = gainControl.getMaximum();
                float dbRange = maxDB - minDB;
                
                float gain;
                if (currentVolumePercent == 0) {
                    gain = minDB;
                } else {
                    gain = (dbRange * (currentVolumePercent / 100.0f)) + minDB; //csak matek, hogyan valtjuk at a szazalekot hangerőre
                }
                gain = Math.max(minDB, Math.min(maxDB, gain)); //A hangerő nem lehet kisebb a minimumnál
                gainControl.setValue(gain); //beállítjuk a hangerőt
            } catch (Exception e) {
                System.err.println("Volume control not supported: " + e.getMessage());
            }
        }
    }

    public long getCurrentPosition() {
        return (audioClip != null) ? audioClip.getMicrosecondPosition() / 1000 : 0; //pozicio
    }

    public long getDuration() {
        return (audioClip != null) ? audioClip.getMicrosecondLength() / 1000 : 0; //hossz
    }

    public boolean isPlaying() {
        return audioClip != null && audioClip.isRunning(); //eppen lejatszodik-e valami
    }

    public boolean isPaused() {
        return isPaused; //meg van e állítva
    }
}