package fx;

import fx.imgur.ImageResponse;
import fx.imgur.ImgurAPI;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import main.*;
import main.Image;
import main.wrapper.DirectoryWrapper;
import main.wrapper.ImageWrapper;
import main.wrapper.ItemWrapper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for JavaFX GUI.
 */
public class Controller {

    private final DirectoryManager rootDirectoryManager = DirectoryManager
            .getInstance();
    private final Service service = new ImgurService();
    // Use the UI element id specified in the FXML file as the variable name
    // to get a hook on those elements
    @FXML
    private Button chooseDirBtn;
    @FXML
    private Button moveFileBtn;
    @FXML
    private Label currentFolderLabel;
    @FXML
    private TreeView<ItemWrapper> imagesTreeView;
    @FXML
    private ImageView imageView;
    @FXML
    private Label imageNameLabel;
    @FXML
    private ListView<String> availableTagsView;
    @FXML
    private ListView<String> nameHistoryView;
    @FXML
    private ListView<String> currentTagsView;
    @FXML
    private Button revertNameBtn;
    @FXML
    private Button deleteTagBtn;
    @FXML
    private Button uploadBtn;
    @FXML
    private Label uploadLabel;
    @FXML
    private Label currentTagsLabel;
    private Stage stage;
    private Image lastSelectedImage;
    private List<Image> curSelectedImages;
    private DirectoryWrapper lastSelectedDirectory;
    private ObservableList<String> availableTagsList = FXCollections
            .observableArrayList();
    private ObservableList<String> currentTagsList = FXCollections
            .observableArrayList();
    private ObservableList<String> nameHistoryList = FXCollections
            .observableArrayList();
    private ObservableList<String> selectedAvailableTags = FXCollections
            .observableArrayList();

    /**
     * Constructor.
     */
    public Controller() {
    }

    /**
     * Adapted from Johnny850807's GitHub repository
     * https://github.com/Johnny850807/Imgur-Picture-Uploading-Example-Using
     * -Retrofit-On-Native-Java
     * on Nov 24th, 2017
     */
    private static ImgurAPI createImgurAPI() {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(ImgurAPI.SERVER)
                .build();
        return retrofit.create(ImgurAPI.class);
    }

/*    private <T extends SelectionModel> void setMultipleSelection(T view) {
        view.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }*/

    /**
     * Sets the JavaFX stage for this controller
     *
     * @param stage the stage
     */
    void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * This method is automatically called after the FXML file is loaded
     * Used to bind UI elements to event listeners
     */
    @FXML
    public void initialize() {
        /* Set ListView's for multiple selection. */
        // todo: refactor into setMultipleSelection method
        availableTagsView.getSelectionModel().setSelectionMode(SelectionMode
                .MULTIPLE);
        currentTagsView.getSelectionModel().setSelectionMode(SelectionMode
                .MULTIPLE);
        imagesTreeView.getSelectionModel().setSelectionMode(SelectionMode
                .MULTIPLE);
        /* Only visible when an Image is in the process of being uploaded to
        Imgur. */
        uploadLabel.setVisible(false);

        if (DirectoryManager.getInstance().readLastDir()) {
            refreshGUIElements();
        } else {
            updateAvailableTags();
        }

        availableTagsView.setItems(availableTagsList);
        currentTagsView.setItems(currentTagsList);
        /* Don't know how to fix this yellow error. The suggested fix
        fort this is the same as the fix for casting from an unknown
        Serialized object (see ImageTagManager's yellow errors for more
        clarification).
         */
        nameHistoryView.setItems(nameHistoryList);

        chooseDirBtn.setOnAction(event -> {
            File rootDirectory = chooseDirectory("Choose a directory to open");
            if (rootDirectory != null) {
                rootDirectoryManager.setRootFolder(rootDirectory);
                refreshGUIElements();
            }
        });

        deleteTagBtn.setOnAction(event -> {
            ObservableList<String> selectedTags = currentTagsView.getSelectionModel().getSelectedItems();
            if (selectedTags.size() != 0) {
                boolean confirmDeleteAll = true;
                if (singleDirectorySelected()) {
                    confirmDeleteAll = PopUp.confirmDeleteAll(selectedTags);
                }
                if (confirmDeleteAll) {
                    deleteTag(selectedTags);
                }
            }
            updateSelectedImageGUI();
            updateLastSelectedTreeItem();
            updateCurSelectedImages();
        });

        uploadBtn.setOnAction(event -> {
            /*
             * Adapted from ItachiUchiha's post on StackOverflow
             * https://stackoverflow
             * .com/questions/31607656/how-to-show-and-then-hide-a-label-in
             * -javafx-after-a-task-is-completed
             * retrieved Nov 25, 2017
             */
            /* Only allow the user to upload one picture at a time */
            if (imagesTreeView.getSelectionModel().getSelectedItems().size()
                    == 1) {
                if (PopUp.confirmUpload()) {
                    uploadLabel.setVisible(true);
                    if (!service.isRunning()) {
                        /* Service will take care of uploading the image */
                        service.start();
                    }

                    service.setOnSucceeded(event1 -> {
                        uploadLabel.setVisible(false);
                        service.reset();
                    });
                }
            }
        });

        moveFileBtn.setOnAction(event -> {
            if (lastSelectedImage != null) {
                try {
                    File newDirectoryFile = chooseDirectory("Move file to " +
                            "directory");
                    boolean sameDir = newDirectoryFile.toString().equals
                            (lastSelectedImage.getCurDir());
                    if (!sameDir) {
                        lastSelectedImage.move(newDirectoryFile.toString(),
                                lastSelectedImage.getImageName(), false);
                        /* If the moved image is still under the root
                        directory, find it */
                        if (rootDirectoryManager.isUnderRootDirectory
                                (newDirectoryFile)) {
                            TreeItem<ItemWrapper> movedImage =
                                    selectMovedImage(lastSelectedImage
                                                    .getImageFile()
                                            , imagesTreeView.getRoot());
                            imagesTreeView.getSelectionModel().select
                                    (movedImage);
                        }
                        refreshGUIElements();
                    }
                } catch (NullPointerException e) {
                    PopUp.noDirSelectedPopup();
                    System.out.println("No valid directory was selected.");
                }
            }
        });

        revertNameBtn.setOnAction(event -> {
            if (curSelectedImages != null && nameHistoryView.getSelectionModel
                    ().getSelectedItems().get(0) != null) {
                String chosenName = nameHistoryView
                        .getSelectionModel().getSelectedItems().get(0);
                chosenName = chosenName.substring(chosenName.indexOf("→") +
                        1).trim();
                lastSelectedImage.revertName(chosenName);
                updateSelectedImageGUI();
                updateLastSelectedTreeItem();
                updateAvailableTags();
            }
        });
    }

    /**
     * Returns the TreeItem representing an image in a directory
     *
     * @param imagePath the path of the image file
     * @param directory the directory in which to look for this image
     * @return the TreeItem representing the image, if found. Null if otherwise.
     */
    private TreeItem<ItemWrapper> selectMovedImage(File imagePath,
                                                   TreeItem<ItemWrapper>
                                                           directory) {
        for (TreeItem<ItemWrapper> child : directory.getChildren()) {
            ItemWrapper wrappedVal = child.getValue();
            if (wrappedVal instanceof DirectoryWrapper) {
                TreeItem<ItemWrapper> returnedItem = selectMovedImage(imagePath, child);
                if (returnedItem != null) {
                    return returnedItem;
                }
                /* child wraps an Image object */
            } else {
                ImageWrapper imageWrapper = (ImageWrapper) wrappedVal;
                if (imagePath.equals(imageWrapper.getImage().getImageFile())) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Changes the image displayed on mouse click or key press
     */
    @FXML
    public void changeImageDisplayed() {
        updateLastSelectedTreeItem();
        updateSelectedImageGUI();
        updateCurSelectedImages();
    }

    private void hideTags() {
        if (selectedAvailableTags.size() != 0) {
            for (String availableTagName : selectedAvailableTags) {
                String extractedTagName = extractAvailableTagName
                        (availableTagName);
                ImageTagManager.getInstance().hideThisTag(extractedTagName);
            }
        }
    }

    /**
     * Updates the last selected image based on what's selected in the
     * imagesTreeView
     */
    private void updateLastSelectedTreeItem() {
        TreeItem<ItemWrapper> lastSelectedTreeItem = imagesTreeView
                .getSelectionModel().getSelectedItem();
        if (lastSelectedTreeItem != null) {
            ItemWrapper lastSelectedItemWrapper = lastSelectedTreeItem
                    .getValue();
            if (lastSelectedItemWrapper instanceof ImageWrapper) {
                lastSelectedImage = ((ImageWrapper) lastSelectedItemWrapper)
                        .getImage();
                currentTagsList.setAll(lastSelectedImage.getTagManager()
                        .getTagNames());
            } else {
                lastSelectedDirectory = (DirectoryWrapper) lastSelectedTreeItem.getValue();
                currentTagsList.setAll(getAllTagsInDirectory(lastSelectedDirectory));
            }
        }
    }

    /**
     * Updates the list of currently selected images based on what's selected
     * in the imagesTreeView
     */
    private void updateCurSelectedImages() {
        ObservableList<TreeItem<ItemWrapper>> selectedTreeItems =
                imagesTreeView.getSelectionModel().getSelectedItems();
        if (selectedTreeItems.size() != 0) {
            List<Image> curSelectedImages = new ArrayList<>();
            if (selectedTreeItems.size() == 1) {
                ItemWrapper firstSelectedItem;
                        /* Following try-catch block to address strange JavaFX
                        behavior described in #21. */
                try {
                    firstSelectedItem = selectedTreeItems.get(0).getValue();
                } catch (NullPointerException e) {
                    firstSelectedItem = selectedTreeItems.get(0).getValue();
                }
                if (firstSelectedItem instanceof DirectoryWrapper) {
                    curSelectedImages = getAllImagesUnderDirectory((
                            (DirectoryWrapper) firstSelectedItem));
                    changeCurrentTagsDisplay((DirectoryWrapper)
                            firstSelectedItem);
                } else {
                    Image selectedImage = ((ImageWrapper)
                            firstSelectedItem).getImage();
                    curSelectedImages.add(selectedImage);
                    lastSelectedImage = selectedImage;
                    currentTagsLabel.setText("Current Tags");
                }
            } else {
                for (TreeItem<ItemWrapper> items : selectedTreeItems) {
                    if (items.getValue() instanceof ImageWrapper) {
                        curSelectedImages.add(((ImageWrapper) items
                                .getValue()).getImage());
                    }
                }
            }
            this.curSelectedImages = curSelectedImages;
        }
    }

    /**
     * Change the label and contents of currentTagsView when a directory is
     * selected, so that it displays all the tags in that directory
     *
     * @param directory the directory that's selected
     */
    private void changeCurrentTagsDisplay(DirectoryWrapper directory) {
        currentTagsLabel.setText("Tags in Selected Directory");
        currentTagsView.getItems().setAll(getAllTagsInDirectory(directory));
    }

    private boolean singleDirectorySelected() {
        ObservableList<TreeItem<ItemWrapper>> selectedTreeItems =
                imagesTreeView.getSelectionModel().getSelectedItems();
        if (selectedTreeItems.size() == 1) {
            ItemWrapper selectedItem = selectedTreeItems.get(0).getValue();
            return selectedItem instanceof DirectoryWrapper;
        }
        return false;
    }

    /**
     * Bound to the addToAvailableTagsBtn, lets the user add a tag to the available tags pool,
     * without adding it to any image
     */
    @FXML
    public void addToAvailableTags() {
        String tagName = PopUp.addTagPopup();
        if (tagName != null) {
            addTagToAvailable(tagName);
            updateAvailableTags();
            populateImageList(new ArrayList<>(), true);
        }
    }

    /**
     * Bound to deleteFromAvailableBtn, allows the user to delete(hide) a tag from the pool
     * of available tags without deleting it from any image.
     */
    @FXML
    public void deleteFromAvailableTags() {
        if (selectedAvailableTags.size() > 0) {
            hideTags();
            updateAvailableTags();
        }
    }

    /**
     * Recursively collects the tags of all images in a directory and its
     * subdirectories and puts them
     * in a list
     *
     * @param directory the directory to search in
     * @return a list of all the tags in the directory
     */
    private Set<String> getAllTagsInDirectory(DirectoryWrapper directory) {
        Set<String> tagsList = new HashSet<>();
        for (ItemWrapper item : directory.getChildObjects()) {
            if (item instanceof DirectoryWrapper) {
                tagsList.addAll(getAllTagsInDirectory((DirectoryWrapper) item));
            } else {
                tagsList.addAll(((ImageWrapper) item).getImage().getAllTags());
            }
        }
        return tagsList;
    }

    /**
     * Recursively collects all the images under a directory, including those
     * in subdirectories
     *
     * @param directory the directory to search in
     * @return a list of images under the aforementioned directory
     */
    private List<Image> getAllImagesUnderDirectory(DirectoryWrapper directory) {
        List<Image> images = new ArrayList<>();
        for (ItemWrapper item : directory.getChildObjects()) {
            if (item instanceof DirectoryWrapper) {
                images.addAll(getAllImagesUnderDirectory((DirectoryWrapper)
                        item));
            } else {
                images.add(((ImageWrapper) item).getImage());
            }
        }
        return images;
    }

    /**
     * Updates the GUI if any changes were made to the selected image,
     * e.g. reverting the name, adding/deleting a tag, etc
     */
    private void updateSelectedImageGUI() {
        if (lastSelectedImage != null) {
            // Update ImageView.
            String filePath = lastSelectedImage.getPathString();
            imageView.setImage(new javafx.scene.image.Image
                    ("file:" + filePath));
            // Update TreeView.
            imagesTreeView.refresh();
            // Update label.
            imageNameLabel.setText(lastSelectedImage.getPathString());
            updateNameHistory();
            updateAvailableTags();
        } else {
            imageView.setImage(null);
        }
    }

    /**
     * Extracts the name of a tag by stripping away dashes and spaces
     *
     * @param tagName the tag name to be cleaned up
     * @return the extracted tag name without any dashes or spaces
     */
    private String extractAvailableTagName(String tagName) {
        return tagName.substring(tagName.indexOf("-") + 1).trim();
    }

    /**
     * Method that allows the user to add new tags by interacting with GUI
     * elements
     * Exposed to the FXML file through the @FXML annotation
     */
    @FXML
    public void addNewTag() {
        String newTagName = PopUp.addTagPopup();
        if (curSelectedImages != null && newTagName.trim().length() > 0) {
            for (Image img : curSelectedImages) {
                img.addTag(newTagName);
            }
            updateSelectedImageGUI();
            updateLastSelectedTreeItem();
            updateCurSelectedImages();
        }
    }

    /**
     * Adds a new tag to the available tags pool, without adding it to an actual image
     * 
     * @param newTagName the tag to add
     */
    private void addTagToAvailable(String newTagName) {
        if (newTagName.trim().length() > 0) {
            ImageTagManager.getInstance().addTagToPlaceholder(newTagName);
            // refreshGUIElements();
            updateAvailableTags();
        }
    }

    /**
     * Updates the selected tags in the "Available Tags" ListView
     */
    @FXML
    public void updateSelectedAvailableTags() {
        this.selectedAvailableTags = availableTagsView.getSelectionModel()
                .getSelectedItems();
    }

    /**
     * Allows the user to add a tag from the ListView of available tags by
     * interacting with a GUI element
     */
    @FXML
    public void addAvailableTag() {
        if (curSelectedImages != null && selectedAvailableTags.size() != 0) {
            for (String tagName : selectedAvailableTags) {
                for (Image img : curSelectedImages) {
                    img.addTag(extractAvailableTagName(tagName));
                }
            }
            updateSelectedImageGUI();
            updateLastSelectedTreeItem();
        }
    }

    /**
     * Deletes a list of tags from the currently selected images
     *
     * @param tagNamesToDelete the list of tags to be deleted
     */
    @FXML
    private void deleteTag(ObservableList<String> tagNamesToDelete) {
        if (curSelectedImages != null && tagNamesToDelete.size() != 0) {
            for (String tagName : tagNamesToDelete) {
                int indexOfDash = tagName.indexOf('-');
                if (indexOfDash != -1) {
                    tagName = tagName.substring(indexOfDash + 2);
                }
                for (Image img : curSelectedImages) {
                    img.deleteTag(tagName);
                }
            }
            updateSelectedImageGUI();
            updateLastSelectedTreeItem();
            // refreshGUIElements();
            updateAvailableTags();
        }
    }

    /**
     * Display the OS's file selector so the user can select a directory
     *
     * @param title the title of the file selector window
     * @return the directory that was chosen by the user
     */
    @FXML
    private File chooseDirectory(String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        return directoryChooser.showDialog(stage);
    }

    /**
     * Opens the directory the last selected image is in
     */
    @FXML
    private void openImageDir() {
        if (lastSelectedImage != null) {
            if (Desktop.isDesktopSupported()) {
                new Thread(() -> {
                    try {
                        File imageDir = new File(PathExtractor.getDirectory
                                (lastSelectedImage.getPathString()));
                        Desktop.getDesktop().open(imageDir);
                    } catch (IOException e) {
                        String popupText = "Unable to open directory.";
                        PopUp.errorPopup("Error", popupText);
                        System.out.println("Unable to open directory.");
                    }
                }).start();
            } else {
                String popupText = "The Java awt Desktop API is not supported" +
                        " on this machine.";
                PopUp.errorPopup("Error", popupText);
            }
        }
    }

    /**
     * Opens the rename log for the user to review
     */
    @FXML
    private void viewRenameLog() {
        File renameLogFile = new File(LogUtility.RENAME_LOGGER_NAME + ".txt");
        try {
            Runtime.getRuntime().exec(String.format("gedit %s", renameLogFile
                    .getAbsoluteFile().toString()));
        } catch (IOException e) {
            String popupText = String.format("Unable to open %s.",
                    renameLogFile.toString());
            PopUp.errorPopup("Unable to Open File", popupText);
        }
    }

    /**
     * Refreshes all GUI elements. Should ideally be used once when starting
     * program. updateSelectedImageGUI() should be used for more constant
     * updating.
     */
    private void refreshGUIElements() {
        currentFolderLabel.setText(rootDirectoryManager.getRootFolder()
                .getPath().toString());
        /* Pass in an empty list when just refreshing (no filtering) */
        populateImageList(new ArrayList<>(), true);
        updateAvailableTags();
        updateSelectedImageGUI();
    }

    /**
     * Filters the images displayed in imagesTreeView based on the tags the
     * user has
     * selected from the availableTagsView
     */
    @FXML
    public void filterImagesByTags() {
        if (selectedAvailableTags.size() != 0) {
            List<String> tagNames = new ArrayList<>();
            for (String tag : selectedAvailableTags) {
                tag = extractAvailableTagName(tag);
                if (ImageTagManager.getInstance().getTaggedImageCount(tag) != 0) {
                    tagNames.add(tag);
                }
            }
            populateImageList(tagNames, true);
        }
    }

    /**
     * Shows all images without any tag filtering
     */
    @FXML
    public void showAllImages() {
        populateImageList(new ArrayList<>(), false);
    }

    /**
     * Populates the TreeView with list of all images under current dir.
     *
     * @param tagNames list of tag names to filter images by
     */
    private void populateImageList(List<String> tagNames, boolean
            expandDirectories) {
        TreeItem<ItemWrapper> rootFolderNode = new TreeItem<>(
                rootDirectoryManager.getRootFolder());
        ItemWrapper rootImagesList = rootDirectoryManager
                .getRootFolder();
        populateParentNode(rootFolderNode, rootImagesList, tagNames,
                expandDirectories);
        rootFolderNode.setExpanded(true);
        imagesTreeView.setRoot(rootFolderNode);
        imagesTreeView.refresh();
    }

    /**
     * Clears the current name history ListView and rebuilds it using the
     * selected
     * image's TagManager
     */
    private void updateNameHistory() {
        if (lastSelectedImage != null) {
            nameHistoryList.setAll(lastSelectedImage.getTagManager()
                    .getNameHistory());
        } else {
            nameHistoryView.getItems().clear();
        }
    }

    /**
     * Populates the parentNode using ItemWrapper objects from the
     * parentNodeList
     *
     * @param parentNode     The UI element to be populated
     * @param parentNodeList ItemWrapper containing the data needed to
     *                       populate the parent
     * @param tags           List of tags to filter images by, images
     *                       containing any tag in
     *                       the list will be added to the parentNode
     */
    private void populateParentNode(TreeItem<ItemWrapper> parentNode,
                                    ItemWrapper parentNodeList, List<String>
                                            tags, boolean expandDirectories) {
        if (parentNodeList instanceof DirectoryWrapper) {
            for (ItemWrapper wrappedItem : ((DirectoryWrapper)
                    parentNodeList).getChildObjects()) {
                /* If the wrappedItem is a directory, recurse */
                if (wrappedItem instanceof DirectoryWrapper) {
                    TreeItem<ItemWrapper> childNode = new TreeItem<>
                            (wrappedItem);
                    populateParentNode(childNode, wrappedItem, tags,
                            expandDirectories);
                    if (!childNode.isLeaf()) {
                        parentNode.getChildren().add(childNode);
                        childNode.setExpanded(expandDirectories);
                    }
                    /* If the wrapped item is an image */
                } else {
                    if (((ImageWrapper) wrappedItem).getImage().hasAnyTag(tags)) {
                        parentNode.getChildren().add(new TreeItem<>
                                (wrappedItem));
                    }
                }
            }
        }
    }

    /**
     * Updates the ListView that displays all available tags in the chosen
     * root directory
     */
    private void updateAvailableTags() {
        availableTagsList.setAll(ImageTagManager.getInstance()
                .getAvailableTagsWithCount());
    }

    /**
     * Inner class that takes care of uploading an image to Imgur
     * <p>
     * Adapted from ItachiUchiha's post on StackOverflow
     * https://stackoverflow.com/questions/31607656/how-to-show-and-then-hide
     * -a-label-in-javafx-after-a-task-is-completed
     * retrieved Nov 25, 2017
     */
    class ImgurService extends Service<Void> {
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    /*
                     * Adapted from Johnny850807's GitHub repository
                     * https://github
                     * .com/Johnny850807/Imgur-Picture-Uploading-Example
                     * -Using-Retrofit-On-Native-Java
                     * on Nov 24th, 2017
                     */
                    final ImgurAPI imgurApi = createImgurAPI();
                    try {
                        File image = new File(lastSelectedImage.getPath()
                                .toString());
                        RequestBody request = RequestBody.create(MediaType.parse
                                ("image/*"), image);
                        Call<ImageResponse> call = imgurApi.postImage(request);
                        Response<ImageResponse> res = call.execute();

                        System.out.println("Successful? " + res.isSuccessful());
                        String url = res.body().data.link;

                        Runtime runtime = Runtime.getRuntime();
                        try {
                            runtime.exec("firefox " + url);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                    return null;
                }
            };
        }
    }
}
