package nagyhazi;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;


//CSAK A FELHASZNÁLÓVAL FOGLALKOZIK, NEM TUDJA HOGYAN KELL ZENÉT LEJÁTSZANI
//TOVÁBBÍTJA A KÉRÉST
public class MediaPlayerApp extends JFrame {
    private MediaPlayerController controller;
    private JTable mediaTable;
    private DefaultTableModel tableModel;
    private JButton playPauseButton, stopButton;
    private Timer statusTimer;
    private JLabel statusLabel, timeLabel;
    private JSlider progressSlider, volumeSlider;
    private boolean isSeeking = false;
    
    private static final String MEDIA_FOLDER = "media";
    
    public MediaPlayerApp() { //konstruktor
        controller = new MediaPlayerController();
        initializeUI();
        loadMediaFromFolder();
        startStatusTimer();
    }
    
    private void initializeUI() { //felulet inicializalasa
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
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK)); //CTRL + O
        openItem.addActionListener(e -> openMediaFile());
        
        JMenuItem refreshItem = new JMenuItem("Refresh Media Folder");
        refreshItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)); //F5
        refreshItem.addActionListener(e -> loadMediaFromFolder());
        
        //JMenuItem exitItem = new JMenuItem("Exit"); van egy x gombunk amivel bezárjuk, szóval felesleges
        //exitItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(openItem);
        fileMenu.add(refreshItem);
        fileMenu.addSeparator();
        //fileMenu.add(exitItem);
        menuBar.add(fileMenu); //maga a menü 
        setJMenuBar(menuBar); //az egész menü bár beállítása
    }
    
    private void createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        playPauseButton = new JButton("Play");
        stopButton = new JButton("Stop");
        
        playPauseButton.addActionListener(e -> handlePlayPause());
        stopButton.addActionListener(e -> {
            controller.stopCurrentMedia();
            updatePlayPauseButton();
        });
        
        statusLabel = new JLabel("Ready");
        timeLabel = new JLabel("00:00 / 00:00");
        
        // --- PROGRESS SLIDER ---
        progressSlider = new JSlider(0, 100, 0);
        progressSlider.setToolTipText("Seek");
        progressSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isSeeking = true;
                JSlider slider = (JSlider) e.getSource(); //ez maga a progressSlider
                double percent = (double) e.getX() / (double) slider.getWidth(); //a csuszka hány százalékánál vagyunk
                int range = slider.getMaximum() - slider.getMinimum(); //csúszka tartománya
                int newVal = (int) (slider.getMinimum() + (range * percent));
                
                SwingUtilities.invokeLater(() -> slider.setValue(newVal)); //azért, hogy a csúszka odaugorjon pont ahol felengedtük az egeret, be-
                                                                           //állítjuk a csúszka értékét (szálbiztos)
            }
            @Override
            public void mouseReleased(MouseEvent e) { //amikor felengedjük az egeret
                int seekValue = progressSlider.getValue();
                controller.seekTo(seekValue); //magát a zenét odatekeri
                
                Timer resumeTimer = new Timer(200, evt -> isSeeking = false); //kevés késleltetés a programnak
                resumeTimer.setRepeats(false);
                resumeTimer.start();
                updateUIStatus();
            }
        });

        // --- VOLUME SLIDER ---
        volumeSlider = new JSlider(0, 100, 80);
        volumeSlider.setToolTipText("Volume");
        volumeSlider.addChangeListener(e -> {
             controller.setVolume(volumeSlider.getValue());
        });

        JPanel bottomControlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomControlsPanel.add(playPauseButton);
        bottomControlsPanel.add(stopButton);
        bottomControlsPanel.add(new JLabel("Volume:"));
        bottomControlsPanel.add(volumeSlider);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(timeLabel, BorderLayout.EAST);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(bottomControlsPanel, BorderLayout.NORTH);
        southPanel.add(progressSlider, BorderLayout.CENTER);
        southPanel.add(statusPanel, BorderLayout.SOUTH);
        
        // --- TABLE ---
        tableModel = new DefaultTableModel(new String[]{"File Name", "Type", "Duration", "Size"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; } //ne lehessen módosítani
        };
        mediaTable = new JTable(tableModel);
        mediaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); //egyszerre egyet lehessen kijelölni
        mediaTable.setComponentPopupMenu(createTableContextMenu()); //kontext menü: copy, paste stb
        
        JScrollPane scrollPane = new JScrollPane(mediaTable); //görgető, ha sok zene van
        scrollPane.setComponentPopupMenu(createBackgroundContextMenu()); //jobbklikkes menü ha nem zeneszámra kattintunk
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    //KONTEXT MENÜK
    
    private JPopupMenu createTableContextMenu() { return createContextMenu(true); } //true ha a tablazatba kattintunk
    private JPopupMenu createBackgroundContextMenu() { return createContextMenu(false); } //false ha a hatterbe kattintunk
    
    private JPopupMenu createContextMenu(boolean onTable) { //kontext (jobb klikk) men
        JPopupMenu menu = new JPopupMenu();
        if (onTable) { //csak ha a tablazatba belekattintunk
            JMenuItem play = new JMenuItem("Play");
            play.addActionListener(e -> handlePlayPause());
            menu.add(play);
            
            JMenuItem del = new JMenuItem("Delete");
            del.addActionListener(e -> deleteSelectedMedia());
            menu.add(del);
            
            JMenuItem copy = new JMenuItem("Copy");
            copy.addActionListener(e -> controller.copyMedia(controller.getMediaLibrary().get(mediaTable.getSelectedRow())));
            menu.add(copy);
            
             JMenuItem cut = new JMenuItem("Cut");
            cut.addActionListener(e -> controller.cutMedia(controller.getMediaLibrary().get(mediaTable.getSelectedRow())));
            menu.add(cut);
        }
        JMenuItem paste = new JMenuItem("Paste");
        paste.addActionListener(e -> pasteMedia());
        menu.add(paste);
        return menu;
    }

    //LISTA BETÖLTÉS

    private void loadMediaFromFolder() {
        controller.getMediaLibrary().clear(); //nullázás
        tableModel.setRowCount(0);
        
        File mediaDir = new File(MEDIA_FOLDER); //media mappánk
        if (!mediaDir.exists()) {
            mediaDir.mkdirs(); //ha nincs akkor csinálunk egyet
        }
        
        File[] mediaFiles = mediaDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".mp3")); //megnézi, hogy .wav vagy .mp3 a fileok, a többi nem kell
        
        if (mediaFiles != null) {
            for (File file : mediaFiles) addMediaFile(file.toPath()); //hozzáadás, minden filera külön
            statusLabel.setText("Loaded " + mediaFiles.length + " files");
        }
    }

    private boolean addMediaFile(Path filePath) {
        try {
            String lowerName = filePath.getFileName().toString().toLowerCase();
            MediaFile.MediaType type = lowerName.endsWith(".mp3") ? MediaFile.MediaType.MP3 : MediaFile.MediaType.WAV; //ha mp3 akkor ugy irjuk be, ha wav akkor meg ugy
            
            Duration duration = MediaMetadataReader.getDuration(filePath);
            
            MediaFile mediaFile = new MediaFile( //objektum letrehozasa
                filePath.getFileName().toString(),
                filePath,
                duration,
                Files.size(filePath),
                type
            );
            
            controller.addMediaFile(mediaFile); //controllerbe is belerakjuk a filet
            tableModel.addRow(new Object[]{ //táblázathoz hozzáadunk egy sort, ami a file adatit tartalmazza
                mediaFile.getFileName(),
                mediaFile.getMediaType(),
                mediaFile.getFormattedDuration(),
                mediaFile.getFormattedFileSize()
            });
            return true; //siker
        } catch (Exception e) {
            System.err.println("Error adding file: " + e.getMessage());
            return false; //sikertelen
        }
    }

    //BUTTON & PLAYBACK HANDLERS

    private void handlePlayPause() {//!!mindig lefut ha megnyomjuk a PLAY/PAUSE gombot
        int selectedRow = mediaTable.getSelectedRow();
        if (selectedRow < 0) { //ki van e jelölve sor
            if (controller.isPaused()) { //ez azért, ha nincs kijelölve sor és meg van állítva, ne kelljen a sorra kattintani, hogy folytassuk a lejátszást
                controller.resumeMedia();
            }
            updatePlayPauseButton(); //frissítjük a gombot
            return;
        }
        
        MediaFile selectedMedia = controller.getMediaLibrary().get(selectedRow); //kijelölt
        MediaFile currentMedia = controller.getCurrentMedia(); //éppen játszó
        
        if (controller.isPaused() && selectedMedia.equals(currentMedia)) { //ha meg van állítva, és ki van jelölve
            controller.resumeMedia(); //folytassa
            statusLabel.setText("Resumed: " + selectedMedia.getFileName());
        } else if (controller.isPlaying() && selectedMedia.equals(currentMedia)) { //ha játszódik, ls ki van jelölve
            controller.pauseMedia(); //állítsa meg
            statusLabel.setText("Paused: " + selectedMedia.getFileName());
        } else { //ha nem játszódik semmi
            if (controller.playMedia(selectedMedia)) { //nem történik semmi, ha nincs semmi kijelölve
                statusLabel.setText("Playing: " + selectedMedia.getFileName());
            }
        }
        updatePlayPauseButton();
    }
    
    private void startStatusTimer() {
        statusTimer = new Timer(250, e -> SwingUtilities.invokeLater(this::updateUIStatus)); //frissítjük az UI-t, masodpercenként négyszer
                                                                                                    //invokeLater, akkor fusson ha az biztonságos (szálbiztos)
        statusTimer.start();
    }
    
    private void updateUIStatus() {
        updatePlayPauseButton();
        if (!isSeeking && (controller.isPlaying() || controller.isPaused())) {
            long current = controller.getCurrentPosition();
            long duration = controller.getDuration();
            if (duration > 0) {
                progressSlider.setMaximum((int) duration); //maximum beállítása
                progressSlider.setValue((int) current); //pontos érték beállítása
                timeLabel.setText(formatTime(current) + " / " + formatTime(duration)); //ki is írjuk, hogy éppen hol tartunk
            }
        } else if (!controller.isPlaying() && !controller.isPaused()) {
            progressSlider.setValue(0);
        }
    }

    private void updatePlayPauseButton() {//gomb felirat frissítése
        playPauseButton.setText(controller.isPlaying() ? "Pause" : "Play"); //ha játszik akkor play, ha nem akkor pause
    }

    private String formatTime(long millis) {
        long s = millis / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    //MENÜ, SHORTCUT

    private void deleteSelectedMedia() { //törlés
        int row = mediaTable.getSelectedRow();
        if (row >= 0) {
             MediaFile mf = controller.getMediaLibrary().get(row);
             if (controller.deleteMedia(mf)) {
                 tableModel.removeRow(row);
                 statusLabel.setText("Deleted: " + mf.getFileName());
             }
        }
    }

    private void copySelectedMedia() { //másolás
        int selectedRow = mediaTable.getSelectedRow();
        if (selectedRow >= 0) {
            MediaFile mediaFile = controller.getMediaLibrary().get(selectedRow);
            controller.copyMedia(mediaFile);
            statusLabel.setText("Copied: " + mediaFile.getFileName());
        }
    }

    private void cutSelectedMedia() { //vágás
        int selectedRow = mediaTable.getSelectedRow();
        if (selectedRow >= 0) {
            MediaFile mediaFile = controller.getMediaLibrary().get(selectedRow);
            controller.cutMedia(mediaFile);
            statusLabel.setText("Cut: " + mediaFile.getFileName());
        }
    }
    
    

    private void openMediaFile() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : fc.getSelectedFiles()) {
                try {
                    Path dest = Paths.get(MEDIA_FOLDER, f.getName());
                    Files.copy(f.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    addMediaFile(dest);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    }
    
    private void pasteMedia() {
        if (controller.pasteMedia(Paths.get(MEDIA_FOLDER))) {
            loadMediaFromFolder();
            statusLabel.setText("Pasted successfully");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MediaPlayerApp().setVisible(true)); //kirajzolás
    }
}