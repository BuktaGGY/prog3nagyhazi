package nagyhazi;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
// ÚJ IMPORT-ok a JSlider és a ChangeListener számára
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class MediaPlayerApp extends JFrame {
    private MediaPlayerController controller;
    private JTable mediaTable;
    private DefaultTableModel tableModel;
    private JButton playPauseButton, stopButton;
    private javax.swing.Timer statusTimer; // Időzítő a gomb állapotának frissítéséhez
    private JLabel statusLabel;
    private JScrollPane scrollPane;
    private JProgressBar progressBar;
    
    private JSlider volumeSlider;
    
    private final String MEDIA_FOLDER = "media";
    private Action deleteAction, copyAction, pasteAction, cutAction;
    
    public MediaPlayerApp() {
        controller = new MediaPlayerController();

        initializeUI();
        createKeyboardShortcuts();
        loadMediaFromFolder();
        startStatusTimer();
    }
    
    private void initializeUI() {
        setTitle("Media Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        createMenuBar();
        createMainPanel();
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem openItem = new JMenuItem("Open Files");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> openMediaFile());
        
        JMenuItem refreshItem = new JMenuItem("Refresh Media Folder");
        refreshItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        refreshItem.addActionListener(e -> loadMediaFromFolder());
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(openItem);
        fileMenu.add(refreshItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }
    
    private void createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Control panel (kommentben marad, ahogy nálad is volt)
        // JPanel controlPanel = new JPanel();
        
        // Gomb létrehozása szöveggel
        playPauseButton = new JButton("Play"); 
        
        // Ikonos beállítások eltávolítva
        // if (playIcon != null) { ... }
        // playPauseButton.setPreferredSize(new Dimension(60, 60));
        // playPauseButton.setBorderPainted(false);
        // playPauseButton.setContentAreaFilled(false);
        
        stopButton = new JButton("Stop");
        
        
        // Új, okosabb ActionListener
        playPauseButton.addActionListener(e -> handlePlayPause()); 
        
        // A Stop gombnak is frissítenie kell a Play gomb szövegét
        stopButton.addActionListener(e -> {
            controller.stopCurrentMedia();
            updatePlayPauseButton(); // <<< Ezt add hozzá
        });
        
        // Status panel
        statusLabel = new JLabel("Ready");
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);

        // Progress bar létrehozása
        progressBar = new JProgressBar(0, 100); // Kezdeti min/max
        progressBar.setStringPainted(true);
        progressBar.setString("00:00 / 00:00");
        
        // --- ÚJ RÉSZ: Hangerő-szabályzó ---
        volumeSlider = new JSlider(0, 100, 80); // Min, Max, Kezdőérték (80%)
        volumeSlider.setToolTipText("Volume");
        
        // Listener, ami figyeli a csúszka változását
        volumeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // Csak akkor frissíts, ha a felhasználó "elengedte" a csúszkát
                // (vagy ha nem kattintva állítja). Ez megakadályozza a túl sok eseményt.
                //if (volumeSlider.getValueIsAdjusting()) { ezt még eldöntöm hogy lesz ha nagyon nem tetszik neki hogy sok az event akkor kikommentelem
                    int volumePercent = volumeSlider.getValue();
                    controller.setVolume(volumePercent);
                //}
            }
        });

        
        // A lejátszás vezérlő gombokat külön panelbe tesszük
        JPanel bottomControlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomControlsPanel.add(playPauseButton);
        bottomControlsPanel.add(stopButton);
        bottomControlsPanel.add(new JLabel("Volume:"));
        bottomControlsPanel.add(volumeSlider); 

        // Létrehozunk egy új panelt a déli (SOUTH) részre
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(bottomControlsPanel, BorderLayout.NORTH); // Gombok fent
        southPanel.add(progressBar, BorderLayout.CENTER);      // Progress bar középen
        southPanel.add(statusPanel, BorderLayout.SOUTH);       // Státusz szöveg lent
        
        // Table setup
        String[] columnNames = {"File Name", "Type", "Duration", "Size"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        mediaTable = new JTable(tableModel);
        mediaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Context menus
        JPopupMenu tableContextMenu = createTableContextMenu();
        JPopupMenu backgroundContextMenu = createBackgroundContextMenu();
        mediaTable.setComponentPopupMenu(tableContextMenu);
        
        scrollPane = new JScrollPane(mediaTable);
        scrollPane.setComponentPopupMenu(backgroundContextMenu);
        
        //mainPanel.add(controlPanel, BorderLayout.NORTH); // Kommentelve marad
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private void createKeyboardShortcuts() {
        deleteAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { deleteSelectedMedia(); }
        };
        copyAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { copySelectedMedia(); }
        };
        pasteAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { pasteMedia(); }
        };
        cutAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { cutSelectedMedia(); }
        };
        
        // Table shortcuts
        mediaTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        mediaTable.getActionMap().put("delete", deleteAction);
        mediaTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy");
        mediaTable.getActionMap().put("copy", copyAction);
        mediaTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "paste");
        mediaTable.getActionMap().put("paste", pasteAction);
        mediaTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), "cut");
        mediaTable.getActionMap().put("cut", cutAction);
        
        // Global paste shortcut
        getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "pasteGlobal");
        getRootPane().getActionMap().put("pasteGlobal", pasteAction);
    }
    

    //Jobb klikk menu
    private JPopupMenu createTableContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem playItem = new JMenuItem("Play");
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem cutItem = new JMenuItem("Cut");
        JMenuItem pasteItem = new JMenuItem("Paste");
        
        playItem.addActionListener(e -> handlePlayPause());
        deleteItem.addActionListener(e -> deleteSelectedMedia());
        renameItem.addActionListener(e -> renameSelectedMedia());
        copyItem.addActionListener(e -> copySelectedMedia());
        cutItem.addActionListener(e -> cutSelectedMedia());
        pasteItem.addActionListener(e -> pasteMedia());
        
        deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
        
        menu.add(playItem);
        menu.addSeparator();
        menu.add(deleteItem);
        menu.add(renameItem);
        menu.addSeparator();
        menu.add(copyItem);
        menu.add(cutItem);
        menu.add(pasteItem);
        
        return menu;
    }
    
    //Jobbklikk menu
    private JPopupMenu createBackgroundContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem pasteItem = new JMenuItem("Paste");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        JMenuItem openItem = new JMenuItem("Open Files");
        
        pasteItem.addActionListener(e -> pasteMedia());
        refreshItem.addActionListener(e -> loadMediaFromFolder());
        openItem.addActionListener(e -> openMediaFile());
        
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
        refreshItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        
        menu.add(pasteItem);
        menu.addSeparator();
        menu.add(refreshItem);
        menu.add(openItem);
        
        return menu;
    }
    
    private void loadMediaFromFolder() {
        controller.getMediaLibrary().clear();
        tableModel.setRowCount(0);
        
        File mediaDir = new File(MEDIA_FOLDER);
        if (!mediaDir.exists()) {
            statusLabel.setText("Media folder not found: " + MEDIA_FOLDER);
            if (mediaDir.mkdirs()) {
                JOptionPane.showMessageDialog(this, "Created media folder. Add WAV files to: " + mediaDir.getAbsolutePath());
            }
            return;
        }
        
        File[] mediaFiles = mediaDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".wav") || 
            name.toLowerCase().endsWith(".mp3") ||
            name.toLowerCase().endsWith(".mp4")
        );
        
        if (mediaFiles == null || mediaFiles.length == 0) {
            statusLabel.setText("No media files found");
            JOptionPane.showMessageDialog(this, "No media files found. Please add WAV, MP3, or MP4 files to the media folder.");
            return;
        }
        
        int loadedCount = 0;
        for (File file : mediaFiles) {
            if (addMediaFile(file.toPath())) {
                loadedCount++;
            }
        }
        
        statusLabel.setText("Loaded " + loadedCount + " media files");
    }
    
    private Duration getWavDuration(Path filePath) {
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(filePath.toFile());
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            float frameRate = format.getFrameRate();
            
            if (frames != AudioSystem.NOT_SPECIFIED && frameRate != AudioSystem.NOT_SPECIFIED) {
                double durationInSeconds = frames / frameRate;
                return Duration.ofSeconds((long) durationInSeconds);
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            System.err.println("Error reading WAV duration: " + e.getMessage());
        } finally {
            if (audioInputStream != null) {
                try {
                    audioInputStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing audio stream: " + e.getMessage());
                }
            }
        }
        return Duration.ofMinutes(3).plusSeconds(30); 
    }

    
    private Duration getMP3Duration(Path filePath) {
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(filePath.toFile());
            Map<String, Object> properties = fileFormat.properties();

            if(properties.containsKey("duration")){ //keresünk egy "duration" kulcsot és az ahhoz tartozó érték kell
                long microseconds = (Long) properties.get("duration");
                return Duration.ofNanos(microseconds * 1000);
            }
        } catch (Exception e) {
            System.err.println("Error reading mediafile duration +" + e.getMessage());
        }

        //ha nem sikerült, akkor ez legyen default
        return Duration.ofMinutes(3).plusSeconds(30);
    }
    
    private boolean addMediaFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            String lowerName = fileName.toLowerCase();
            MediaFile.MediaType type;
            Duration duration;
            
            if (lowerName.endsWith(".wav")) {
                type = MediaFile.MediaType.WAV;
                duration = getWavDuration(filePath);
            } else if (lowerName.endsWith(".mp3")) {
                type = MediaFile.MediaType.MP3;
                duration = getMP3Duration(filePath); 
            } else {
                return false;
            }
            
            long fileSize = Files.size(filePath);
            MediaFile mediaFile = new MediaFile(fileName, filePath, duration, fileSize, type);
            controller.addMediaFile(mediaFile);
            
            tableModel.addRow(new Object[]{
                mediaFile.getFileName(),
                mediaFile.getMediaType().toString(),
                mediaFile.getFormattedDuration(),
                mediaFile.getFormattedFileSize()
            });
            
            return true;
        } catch (Exception e) {
            System.err.println("Error adding file " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    private void openMediaFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Media Files", "wav", "mp3", "mp4"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (java.io.File selectedFile : fileChooser.getSelectedFiles()) {
                try {
                    // Copy file to media folder
                    Path sourcePath = selectedFile.toPath();
                    Path targetPath = Paths.get(MEDIA_FOLDER, sourcePath.getFileName().toString());
                    
                    // If file already exists in media folder, ask for confirmation
                    if (Files.exists(targetPath)) {
                        int overwrite = JOptionPane.showConfirmDialog(this,
                            "File '" + sourcePath.getFileName() + "' already exists in media folder. Overwrite?",
                            "File Exists", JOptionPane.YES_NO_OPTION);
                        
                        if (overwrite != JOptionPane.YES_OPTION) {
                            continue; // Skip this file
                        }
                    }
                    
                    // Copy the file to media folder
                    Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    
                    // Add the copied file to the library (this automatically updates the table)
                    addMediaFile(targetPath);
                    
                    statusLabel.setText("Added: " + targetPath.getFileName());
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                        "Error copying file '" + selectedFile.getName() + "': " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void handlePlayPause() {
        int selectedRow = mediaTable.getSelectedRow();
        if (selectedRow < 0) {
            // Ha semmi nincs kiválasztva, de valami szünetel, folytatjuk
            if (controller.isPaused()) {
                 controller.resumeMedia();
                 statusLabel.setText("Resumed: " + controller.getCurrentMedia().getFileName());
            } else {
                JOptionPane.showMessageDialog(this, "Válassz ki egy fájlt a lejátszáshoz");
            }
            updatePlayPauseButton();
            return;
        }
        
        MediaFile selectedMedia = controller.getMediaLibrary().get(selectedRow);
        MediaFile currentMedia = controller.getCurrentMedia();
        
        // 1. Eset: Folytatás (A zene szünetel ÉS ugyanaz a szám van kiválasztva)
        if (controller.isPaused() && selectedMedia.equals(currentMedia)) {
            controller.resumeMedia();
            statusLabel.setText("Resumed: " + selectedMedia.getFileName());
        
        // 2. Eset: Szünet (A zene megy ÉS ugyanaz a szám van kiválasztva)
        } else if (controller.isPlaying() && selectedMedia.equals(currentMedia)) {
            controller.pauseMedia();
            statusLabel.setText("Paused: " + selectedMedia.getFileName());
        
        // 3. Eset: Új lejátszás (Másik szám van kiválasztva, vagy semmi sem megy)
        } else {

            boolean success = controller.playMedia(selectedMedia);
            if (success) {
                statusLabel.setText("Now playing: " + selectedMedia.getFileName());
                // Sikeres lejátszáskor a controller már beállította a hangerőt,
                // de a mi csúszkánknak is tudnia kell róla (ha pl.
                // a controllerben van egy alapértelmezett)
                // Ezért lekérdezzük a controllerben tárolt értéket.
                volumeSlider.setValue(controller.getVolume());
            } else {
                statusLabel.setText("Error playing: " + selectedMedia.getFileName());
            }
        }
        
        updatePlayPauseButton(); // Azonnal frissítjük a gombot
    }
    
    /**
     * Elindít egy időzítőt, ami figyeli a lejátszó állapotát
     * (pl. ha magától leáll a zene) és frissíti a gombot.
     */
    private void startStatusTimer() {
        // Vacilálok hogy 250 vagy 125ms legyen, most 250
        statusTimer = new javax.swing.Timer(250, e -> { 
            // Mostantól ezt az egy metódust hívjuk, ami mindent frissít
            SwingUtilities.invokeLater(this::updateUIStatus); 
        });
        statusTimer.setRepeats(true);
        statusTimer.start();
    }
    
    /**
     * Frissíti a Play/Pause gomb szövegét a controller állapota alapján.
     */
    private void updatePlayPauseButton() {
        if (controller.isPlaying()) {
            playPauseButton.setText("Pause"); // Visszaállítva szövegre
        } else {
            // Akkor is "Play" ha szünetel, vagy ha teljesen leállt
            playPauseButton.setText("Play"); // Visszaállítva szövegre
        }
    }
    
    private void deleteSelectedMedia() {
        int selectedRow = mediaTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<MediaFile> library = controller.getMediaLibrary();
            if (selectedRow < library.size()) {
                MediaFile mediaFile = library.get(selectedRow);
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "Delete " + mediaFile.getFileName() + " from library?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
                
                if (confirm == JOptionPane.YES_OPTION && controller.deleteMedia(mediaFile)) {
                    tableModel.removeRow(selectedRow); //táblázatból és
                    statusLabel.setText("Deleted: " + mediaFile.getFileName()); //listából is töröljük
                }
            }
        }
    }
    
    private void renameSelectedMedia() {
        int selectedRow = mediaTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<MediaFile> library = controller.getMediaLibrary();
            if (selectedRow < library.size()) {
                MediaFile mediaFile = library.get(selectedRow);
                String newName = JOptionPane.showInputDialog(this, "Enter new name:", mediaFile.getFileName());
                if (newName != null && !newName.trim().isEmpty() && controller.renameMedia(mediaFile, newName)) {
                    tableModel.setValueAt(newName, selectedRow, 0);
                    statusLabel.setText("Renamed to: " + newName);
                }
            }
        }
    }
    
    private void copySelectedMedia() {
        int selectedRow = mediaTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<MediaFile> library = controller.getMediaLibrary();
            if (selectedRow < library.size()) {
                MediaFile mediaFile = library.get(selectedRow);
                controller.copyMedia(mediaFile);
                statusLabel.setText("Copied: " + mediaFile.getFileName());
            }
        }
    }
    
    private void cutSelectedMedia() {
        int selectedRow = mediaTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<MediaFile> library = controller.getMediaLibrary();
            if (selectedRow < library.size()) {
                MediaFile mediaFile = library.get(selectedRow);
                controller.cutMedia(mediaFile);
                statusLabel.setText("Cut: " + mediaFile.getFileName());
            }
        }
    }
    
    private void pasteMedia() {
        // Közvetlenül megadjuk a célmappát a MEDIA_FOLDER konstans alapján
        Path destinationPath = Paths.get(MEDIA_FOLDER);
        
        // Meghívjuk a controller pasteMedia metódusát a fix elérési úttal
        if (controller.pasteMedia(destinationPath)) {
            statusLabel.setText("File pasted successfully");
            loadMediaFromFolder(); // Töltsük újra a listát, hogy megjelenjen az új fájl
        } else {
            // A controller már mutat hibaüzenetet, ha baj van
            statusLabel.setText("Paste failed");
        }
    }
    /**
     * Ez az új "gyűjtő" metódus frissíti az összes UI elemet,
     * ami a lejátszás állapotától függ.
     */
    private void updateUIStatus() {
        updatePlayPauseButton(); 
        updateProgressBar();
    }

    /**
     * Frissíti a progress bar állapotát és szövegét.
     */
    private void updateProgressBar() {
        // Csak akkor frissítünk, ha a zene megy vagy szünetel
        if (controller.isPlaying() || controller.isPaused()) {
            long current = controller.getCurrentPosition();
            long duration = controller.getDuration();
            
            if (duration > 0) {
                progressBar.setMaximum((int) duration);
                progressBar.setValue((int) current);
                progressBar.setString(formatTime(current) + " / " + formatTime(duration));
            }
        } else {
            // Ha semmi sem megy, nullázzuk a bart
            progressBar.setValue(0);
            progressBar.setMaximum(100); // Alapértelmezett max
            progressBar.setString("00:00 / 00:00");
        }
    }
    
    /**
     * Segédfüggvény: Milliszekundumokat alakít MM:SS formátumú Stringgé.
     * @param millis A millimásodpercek száma
     * @return Formázott idő string
     */
    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MediaPlayerApp().setVisible(true);
        });
    }
}