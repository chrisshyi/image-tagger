package fx;

import com.sun.corba.se.impl.interceptors.PICurrent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import main.DirectoryManager;

import java.io.File;
import java.util.List;
import main.Picture;

public class Controller {

    // Use the UI element id specified in the FXML file as the variable name
    // to get a hook on those elements
    @FXML
    private Button chooseDirButton;

    @FXML
    private Button moveFileButton;

    @FXML
    private Button openCurDirButton;

    @FXML
    private Label currentFolderLabel;

    @FXML
    private TreeView<String> imagesTreeView;

    // private File rootFolder = new File("/home/kevin/Documents");
    private DirectoryManager rootDirectoryManager = new DirectoryManager
            (null);
    /**
     * The stage this controller is associated with
     */
    private Stage stage;

    public Controller() {
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * This method is automatically called after the FXML file is loaded
     */
    @FXML
    public void initialize() {

        chooseDirButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose a directory to open");
            File rootDirectory = directoryChooser.showDialog(stage);
            if (rootDirectory != null) {
                rootDirectoryManager.setRootFolder(rootDirectory);
                refreshGUIElements();
            }
        });
        openCurDirButton.setOnAction(event -> {
            if (rootDirectoryManager.getRootFolder() != null) {
                rootDirectoryManager.openRootFolder();
            }
        });
        moveFileButton.setOnAction(event -> {
            System.out.print(imagesTreeView.getSelectionModel()
                    .getSelectedItems());

        });
    }

    // Updates all the needed elements when a new directory is selected.
    private void refreshGUIElements() {
        currentFolderLabel.setText(rootDirectoryManager.getRootFolder()
                .toString());
        populateImageList();
    }

    // Populates the TreeView with list of all images under current dir.
    private void populateImageList() {
        TreeItem<String> rootFolderNode = new TreeItem<>(rootDirectoryManager
                .getRootFolder().toString());
        List rootImagesList = rootDirectoryManager.getAllImagesUnderRoot();
        populateParentNode(rootFolderNode, rootImagesList);
        rootFolderNode.setExpanded(true);
        imagesTreeView.setRoot(rootFolderNode);
        imagesTreeView.refresh();
    }

    // Populates parent node with all images under it.
    private void populateParentNode(TreeItem<String> parentNode, List
            imagesList) {
        for (Object o : imagesList) {
           if (o instanceof List) {
                String firstImagePath = (String) ((List) o).get(0);
                TreeItem<String> childNode = new TreeItem<>(getSubdirectoryName
                        (firstImagePath));
                populateParentNode(childNode, (List) o);
                parentNode.getChildren().add(childNode);
            }

            else if (o instanceof Picture){
                TreeItem<String> current = new TreeItem<>(o.toString());
                parentNode.getChildren().add(current);
            }
        }
    }

    // Extracts the last directory from the path name.
    private String getSubdirectoryName(String imagePath) {
        int indexOfLastSlash = imagePath.lastIndexOf('/');
        return imagePath.substring(indexOfLastSlash + 1,
                imagePath.length());
    }



}
