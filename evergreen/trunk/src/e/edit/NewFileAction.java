package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
 * An action for the operation of creating and opening a new file.
 */
public class NewFileAction extends ETextAction {
    private static final FilenameChooserField filenameField = new FilenameChooserField(JFileChooser.FILES_AND_DIRECTORIES);
    
    public NewFileAction() {
        super("_New File...", GuiUtilities.makeKeyStroke("N", false));
        GnomeStockIcon.useStockIcon(this, "gtk-new");
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window != null) {
            filenameField.setPathname(window.getContext());
        } else {
            filenameField.setPathname(Evergreen.getInstance().getCurrentWorkspace().getRootDirectory());
        }

        do {
            FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), "New File");
            form.getFormPanel().addRow("Filename:", filenameField);
            boolean okay = form.show("Create");
            
            if (okay == false) {
                return;
            }
        } while (createNewFile(filenameField.getPathname()) == false);
    }

    private boolean createNewFile(String filename) {
        Evergreen editor = Evergreen.getInstance();
        File newFile = FileUtilities.fileFromString(filename);
        try {
            if (newFile.isAbsolute() == false) {
                // A new file, if no absolute path is specified, should be
                // created relative to the root of the current workspace.
                newFile = FileUtilities.fileFromParentAndString(editor.getCurrentWorkspace().getCanonicalRootDirectory(), filename);
            }
            File directory = newFile.getParentFile();
            if (directory.exists() == false) {
                boolean createDirectory = editor.askQuestion("Create directory?", "The directory \"" + directory + "\" doesn't exist. We can either create the directory for you, or you can go back and re-type the filename.", "Create");
                if (createDirectory == false) {
                    return false;
                }
                directory.mkdirs();
            }
            boolean created = newFile.createNewFile();
            if (created) {
                fillWithInitialContents(newFile);
            } else {
                editor.showAlert("Couldn't create new file", "File \"" + newFile + "\" already exists.");
            }
            editor.openFile(newFile.toString());
            return true;
        } catch (IOException ex) {
            editor.showAlert("Couldn't create new file", "Failed to create file \"" + newFile + "\": " + ex.getMessage() + ".");
            return false;
        }
    }
    
    private void fillWithInitialContents(File file) {
        // FIXME: we could use FileType.guessFileTypeByFilename to allow the user to specify scripts that override a single type. YAGNI.
        String defaultBoilerplateGenerator = FileUtilities.findOnPath("evergreen-boilerplate-generator").toString();
        String boilerplateGenerator = Parameters.getParameter("boilerplateGenerator", defaultBoilerplateGenerator);
        if (boilerplateGenerator == null) {
            return;
        }
        
        String command = "'" + boilerplateGenerator + "' '" + file.toString() + "'";
        ArrayList<String> lines = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(null, ProcessUtilities.makeShellCommandArray(command), lines, lines);
        
        String result = StringUtilities.writeFile(file, StringUtilities.join(lines, "\n"));
        if (result != null) {
            Evergreen.getInstance().showAlert("Couldn't fill new file", "There was a problem filling \"" + file + "\" with initial content: " + result + ".");
        }
    }
}
