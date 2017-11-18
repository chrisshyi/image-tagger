package fx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import main.DirectoryManager;
import main.Image;
import main.ImageTagManager;
import main.PathExtractor;
import main.wrapper.DirectoryWrapper;
import main.wrapper.ImageWrapper;
import main.wrapper.ItemWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Controller {

    // Use the UI element id specified in the FXML file as the variable name
    // to get a hook on those elements
    @FXML
    private Button chooseDirBtn;
    @FXML
    private Button moveFileBtn;
    @FXML
    private Button openCurDirBtn;
    @FXML
    private Label currentFolderLabel;
    @FXML
    private TreeView<ItemWrapper> imagesTreeView;
    @FXML
    private ImageView imageView;
    @FXML
    private TextField addNewTagField;
    @FXML
    private Label imageNameLabel;
    @FXML
    private ListView<String> availableTagsView;
    @FXML
    private ListView nameHistoryView;
    @FXML
    private ListView<String> currentTagsView;
    @FXML
    private Button revertNameBtn;

    /*
     * The following three fields were used repeatedly in the button
     * EventHandlers, made sense to factor them out
     */
    private Image curSelectedImage;

    private EventHandler<MouseEvent> mouseEvent;
    // private File rootFolder = new File("/home/kevin/Documents");
    private DirectoryManager rootDirectoryManager = new DirectoryManager(null);
    /**
     * The stage this controller is associated with
     */
    private Stage stage;

    public Controller() {
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * This method is automatically called after the FXML file is loaded
     * Used primarily to bind UI elements to event listeners
     */
    @FXML
    public void initialize() {

        chooseDirBtn.setOnAction(event -> {
            File rootDirectory = chooseDirectory("Choose a directory to open");
            if (rootDirectory != null) {
                rootDirectoryManager.setRootFolder(new DirectoryWrapper
                        (rootDirectory));
                refreshGUIElements();
            }
        });

        openCurDirBtn.setOnAction(event -> {
            if (rootDirectoryManager.getRootFolder() != null) {
                rootDirectoryManager.openRootFolder();
            }
        });

        // For displaying the image with a mouse click
        mouseEvent = (MouseEvent event) -> {
            ObservableList<TreeItem<ItemWrapper>> selectedTreeItems =
                    imagesTreeView.getSelectionModel().getSelectedItems();
            if (selectedTreeItems.size() != 0) {
                ItemWrapper clickedObject = selectedTreeItems.get(0).getValue();
                if (clickedObject instanceof ImageWrapper) {
                    curSelectedImage = ((ImageWrapper) clickedObject)
                            .getImage();
                    updateSelectedImageGUI();
                }
            }
        };

        // todo: display no image when picture is moved?
        moveFileBtn.setOnAction(event -> {
            if (curSelectedImage != null) {
                try {
                    String newDirectory = chooseDirectory("Move file to " +
                            "directory").toString();
                    String imageFileName = PathExtractor.getImageFileName
                            (curSelectedImage.getPath().toString());
                    String newPathOfImage = newDirectory + "/" + imageFileName;

                    Files.move(curSelectedImage.getPath(), Paths.get
                            (newPathOfImage));
                    System.out.println(curSelectedImage.getPath().toString());
                    ImageTagManager.getInstance().removeImage
                            (curSelectedImage.getPath().toString());
                    // curSelectedImage.setImageFile(newPathOfImage);
                    ImageTagManager.getInstance().addImage(curSelectedImage);
                    // todo: select new image in TreeView afterwards?
                    curSelectedImage = null;
                    //ImageTagManager.getInstance().saveToFile();
                    refreshGUIElements();
                } catch (IOException | NullPointerException e) {
                    System.out.println("No move");
                }
            }
        });

        revertNameBtn.setOnAction(event -> {
            if (curSelectedImage != null) {
                String chosenName = (String) nameHistoryView
                        .getSelectionModel().getSelectedItems().get(0);
                chosenName = chosenName.substring(chosenName.indexOf("→") +
                        1).trim();
                System.out.println(chosenName);
                curSelectedImage.revertName(chosenName);
                updateSelectedImageGUI();
            }
        });
    }

    private void updateSelectedImageGUI() {
        if (curSelectedImage != null) {
            // Update ImageView.
            String filePath = curSelectedImage.getPath().toString();
            imageView.setImage(new javafx.scene.image.Image
                    ("file:" + filePath));
            // Update TreeView.
            imagesTreeView.refresh();
            // Update label.
            imageNameLabel.setText(curSelectedImage.getImageName());
            updateNameHistory();
            updateCurrentTags();
            updateAvailableTags();
        } else {
            imageView.setImage(null);
        }
    }

    /**
     * Method that allows the user to add new tags by interacting with GUI elements
     * Exposed to the FXML file through the @FXML annotation
     */
    @FXML
    public void addNewTag() {
        String newTagName = addNewTagField.getText();
        if (curSelectedImage != null && newTagName.length() > 0) {
            curSelectedImage.addTag(newTagName);
            addNewTagField.clear();
            updateSelectedImageGUI();
        }
    }

    /**
     * Allows the user to add a tag from the ListView of available tags by interacting with a GUI element
     */
    @FXML
    public void addAvailableTag() {
        ObservableList<String> selectedAvailableTag = availableTagsView
                .getSelectionModel().getSelectedItems();
        if (curSelectedImage != null && selectedAvailableTag.size() != 0) {
            curSelectedImage.addTag(selectedAvailableTag.get(0));
            updateSelectedImageGUI();
        }
    }

    /**
     * Allows user to delete a tag by interacting with a GUI element
     */
    @FXML
    public void deleteTag() {
        ObservableList<String> selectedCurrentTag = currentTagsView
                .getSelectionModel().getSelectedItems();
        if (curSelectedImage != null && selectedCurrentTag.size() != 0) {
            curSelectedImage.deleteTag(selectedCurrentTag.get(0));
            updateSelectedImageGUI();
        }
    }

    @FXML
    private File chooseDirectory(String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        return directoryChooser.showDialog(stage);
    }

    /**
     * Refreshes all GUI elements when something is changed by the user
     */
    private void refreshGUIElements() {
        currentFolderLabel.setText(rootDirectoryManager.getRootFolder()
                .toString());
        populateImageList();
        updateAvailableTags();
        updateSelectedImageGUI();
    }

    // Populates the TreeView with list of all images under current dir.
    private void populateImageList() {
        TreeItem<ItemWrapper> rootFolderNode = new TreeItem<>(
                rootDirectoryManager.getRootFolder());
        ItemWrapper rootImagesList = rootDirectoryManager
                .getAllImagesUnderRoot();
        populateParentNode(rootFolderNode, rootImagesList);
        rootFolderNode.setExpanded(true);
        imagesTreeView.setRoot(rootFolderNode);
        imagesTreeView.refresh();
        imagesTreeView.addEventHandler(javafx.scene.input.MouseEvent
                .MOUSE_CLICKED, mouseEvent);
    }

    /**
     * Clears the current name history ListView and rebuilds it using the selected
     * image's TagManager
     */
    private void updateNameHistory() {
        ObservableList<String> nameHistoryList = FXCollections
                .observableArrayList();
        nameHistoryList.setAll(curSelectedImage.getTagManager()
                .getNameHistory());
        nameHistoryView.setItems(nameHistoryList);
    }

    /**
     * Populates the parentNode using ItemWrapper objects from the
     * parentNodeList
     *
     * @param parentNode     The UI element to be populated
     * @param parentNodeList ItemWrapper containing the data needed to
     *                       populate the parent
     */
    private void populateParentNode(TreeItem<ItemWrapper> parentNode,
                                    ItemWrapper parentNodeList) {
        if (parentNodeList instanceof DirectoryWrapper) {
            for (ItemWrapper wrappedItem : ((DirectoryWrapper)
                    parentNodeList).getChildObjects()) {
                if (wrappedItem instanceof DirectoryWrapper) {
                    String parentPath = wrappedItem.getPath().toString();
                    TreeItem<ItemWrapper> childNode = new TreeItem<>
                            (new DirectoryWrapper(new File(PathExtractor
                                    .getImageFileName(parentPath))));
                    populateParentNode(childNode, wrappedItem);
                    parentNode.getChildren().add(childNode);
                } else {
                    parentNode.getChildren().add(new TreeItem<>(wrappedItem));
                }
            }
        }
    }

    private void updateAvailableTags() {
        ObservableList<String> availableTagsList = FXCollections
                .observableArrayList();
        availableTagsList.setAll(ImageTagManager.getInstance().getListOfTags());
        availableTagsView.setItems(availableTagsList);
    }

    private void updateCurrentTags() {
        ObservableList<String> currentTagsList = FXCollections
                .observableArrayList();
        currentTagsList.setAll(curSelectedImage.getTagManager().getTagNames());
        currentTagsView.setItems(currentTagsList);
    }

}
