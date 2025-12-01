package nagyhazi;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ClipboardHandler {
    private File internalClipboard; //tárolj annak a filenak a referenciáját, amit utoljára akartunk másolni/kivágni
    private boolean isCutOperation;

    //állapotbeállítás csak
    public void copy(File file) {
        this.internalClipboard = file;
        this.isCutOperation = false;
    }

    public void cut(File file) {
        this.internalClipboard = file;
        this.isCutOperation = true;
    }

    
    public String paste(Path destinationDir) {
        Clipboard sysClipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); //Hogy ne csak a programon belül tudjunk másolni, hanem az oprendszer vágólapjáról is pl
        Transferable contents = sysClipboard.getContents(null); //a rendszer vágólapjának pillanatnyi állapota

        try {
            // 1. Próba: Rendszer vágólap (pl. Windows Intézőből)
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) { //ha van rajta file és ez meg is felel nekünk
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) contents.getTransferData(DataFlavor.javaFileListFlavor); //garantalja hogy jo fileokat kapunk csak
                if (files != null && !files.isEmpty()) {
                    for (File file : files) { //lehet több is van a vágólapon
                        if (file.isFile()) {
                            pasteSingleFile(file, destinationDir, false);
                        }
                    }
                    return null; // Sikeres
                }
            }
            
            // 2. Próba: Belső vágólap (a programon belül nyomtad a Copy-t)
            if (internalClipboard != null) {
                pasteSingleFile(internalClipboard, destinationDir, isCutOperation);
                return null; // Sikeres
            }

        } catch (Exception e) {
            return "Error pasting: " + e.getMessage();
        }
        
        return "Clipboard is empty";
    }

    private void pasteSingleFile(File sourceFile, Path destinationDir, boolean isCut) throws IOException {
        Path sourcePath = sourceFile.toPath();
        String originalName = sourceFile.getName();
        
        // Névütközés feloldása (pl. zene.mp3 -> zene (1).mp3)
        Path destination = destinationDir.resolve(originalName); //pontosan melyik fileba másoljunk bele
        String baseName = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf('.')) : originalName; //név extension nélkül
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ""; //extension
        
        int copyIndex = 1;
        while (Files.exists(destination)) { //amíg létezik file az elérési útvonalon
            if (isCut && sourcePath.equals(destination)) {
                isCutOperation = false; 
                internalClipboard = null;
                return; // Ha ugyanoda mozgatjuk, nem csinálunk semmit
            }
            destination = destinationDir.resolve(String.format("%s (%d)%s", baseName, copyIndex++, ext)); //új elérési út létrehozása az új névvel, még nem mozgatunk filet, pl.: zene (1).mp3
        }

        if (isCut) { //ha kivágás van
            Files.move(sourcePath, destination, StandardCopyOption.REPLACE_EXISTING); //felülírjuk ha azonos nevűt, és töröljük a régit
            internalClipboard = null;
            isCutOperation = false;
        } else { //ha másolunk
            Files.copy(sourcePath, destination);
        }
    }
}