package main;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Manages anything related to Tags for an Image.
 */
public class TagManager implements Serializable {
    /**
     * The name history of the image this TagManager is associated with
     */
    private TreeMap<Timestamp, String> nameHistory;

    // todo: can this be deleted? one usage and can recreate tags upon reverting
    /**
     * List of Tags the image has ever had
     */
    private ArrayList<Tag> tagList;
    /**
     * Current tags on the image
     */
    private Set<Tag> currentTags;
    /**
     * The image this TagManager is associated with
     */
    private Image image;

    /**
     * Constructor.
     *
     * @param originalImageName Original name of image without extension.
     * @param image             Image this TagManager will be associated with.
     */
    public TagManager(String originalImageName, Image image) {
        nameHistory = new TreeMap<>();
        nameHistory.put(new Timestamp(System.currentTimeMillis()),
                originalImageName);
        tagList = new ArrayList<>();
        currentTags = new LinkedHashSet<>();
        this.image = image;
    }

    /**
     * Returns the Image associated with this TagManager.
     *
     * @return The Image associated with this TagManager.
     */
    public Image getImage() {
        return image;
    }

    /**
     * Sets the Image associated with this TagManager.
     *
     * @param image The Image to be associated with this TagManager.
     */
    public void setImage(Image image) {
        this.image = image;
    }

    /**
     * Adds new Tag with tagName param to this tag manager.
     *
     * @param tagName Name of tag to add.
     * @return What the new name of the file should be.
     */
    public String addTag(String tagName) {
        Tag tag = new Tag(image, tagName);
        if (!currentTags.contains(tag)) {
            currentTags.add(tag);
            tagList.add(tag);
            String currentName = nameHistory.lastEntry().getValue();
            nameHistory.put(new Timestamp(System.currentTimeMillis()),
                    currentName + " @" + tag.getName());
            LogUtility.getInstance().logAddOrDeleteTag(tag.getName(), image
                    .getImageName(), true);
        }

        return nameHistory.lastEntry().getValue();
    }

    /**
     * Deletes Tag with tagName param to this tag manager.
     *
     * @param tagName Name of tag to delete.
     * @return What the new name of the file should be.
     */
    public String deleteTag(String tagName) {
        Tag tag = new Tag(image, tagName);
        if (currentTags.contains(tag)) {
            currentTags.remove(tag);
            nameHistory.put(new Timestamp(System.currentTimeMillis()),
                    getCurrentName());
            LogUtility.getInstance().logAddOrDeleteTag(tagName, image
                    .getImageName(), false);
        }
        return nameHistory.lastEntry().getValue();
    }

    // Returns current name of Image with its original name and current tags.
    private String getCurrentName() {
        StringBuilder result = new StringBuilder();
        result.append(PathExtractor.getOriginalName((image.getPathString())));
        for (Tag currentTag : currentTags) {
            result.append(" @").append(currentTag.getName());
        }
        return result.toString();
    }

    /**
     * Returns all the names this Image has ever had.
     *
     * @return ArrayList of all the names this Image has ever had.
     */
    public ArrayList<String> getNameHistory() {
        ArrayList<String> result = new ArrayList<>();
        for (Timestamp keys : nameHistory.keySet()) {
            String s = new SimpleDateFormat("MM/dd HH:mm:ss").format(keys);
            result.add(s + "  →  " + nameHistory.get(keys));
        }
        return result;
    }

    /**
     * Returns of the current tags of this image.
     *
     * @return Alphabetically sorted ArrayList of current tags.
     */
    public ArrayList<String> getTagNames() {
        ArrayList<String> result = new ArrayList<>();
        for (Tag tag : currentTags) {
            result.add(tag.getName());
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Reverts the Image to a previous name in nameHistory.
     *
     * @param name Name to be reverted to.
     * @return New name of Image.
     */
    String revertName(String name) {
        if (nameHistory.values().contains(name)) {
            LogUtility.getInstance().logRevertName(nameHistory.lastEntry()
                    .getValue(), name);
            nameHistory.put(new Timestamp(System.currentTimeMillis()), name);
            updateCurrentTags();
        }
        return nameHistory.lastEntry().getValue();
    }

    // Updates set of current tags based on latest name.
    private void updateCurrentTags() {
        String name = nameHistory.lastEntry().getValue();
        ArrayList<String> tags = new ArrayList<>(Arrays.asList(name
                .split("@")));
        for (int i = 0; i < tags.size(); i++) {
            tags.set(i, tags.get(i).trim());
        }
        tags.remove(0);

        currentTags = new LinkedHashSet<>(returnTagsNeeded(tags));
    }

    // Return tags needed from tagList when reverting name.
    private ArrayList<Tag> returnTagsNeeded(ArrayList<String> names) {
        ArrayList<Tag> tags = new ArrayList<>();
        for (String name : names) {
            for (Tag tag : tagList) {
                if (name.equals(tag.getName())) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    /**
     * Object is equal to this TagManager if it is an instance of a
     * TagManager and all its fields are equal.
     *
     * @param o Object to be compared to.
     * @return Whether this and the object are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TagManager that = (TagManager) o;

        if (nameHistory != null ? !nameHistory.equals(that.nameHistory) : that
                .nameHistory != null)
            return false;
        if (tagList != null ? !tagList.equals(that.tagList) : that.tagList !=
                null)
            return false;
        // Ignoring this yellow error for readability.
        if (currentTags != null ? !currentTags.equals(that.currentTags) :
                that.currentTags != null)
            return false;
        return image != null ? image.equals(that.image) : that.image == null;
    }

    /**
     * Returns hash code of this object based on its fields.
     *
     * @return Hashed value.
     */
    @Override
    public int hashCode() {
        int result = nameHistory != null ? nameHistory.hashCode() : 0;
        result = 31 * result + (tagList != null ? tagList.hashCode() : 0);
        result = 31 * result + (currentTags != null ? currentTags.hashCode()
                : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        return result;
    }
}



