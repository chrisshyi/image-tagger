package main.wrapper;

import main.Image;

import java.nio.file.Path;

/**
 * Wrapper class for a Image object
 */
public class ImageWrapper implements ItemWrapper {

    /**
     * The Image object that this wrapper encapsulates.
     */
    private Image image;

    /**
     * Constructs a new ImageWrapper object
     *
     * @param image the image object that this wrapper wraps around
     */
    public ImageWrapper(Image image) {
        this.image = image;
    }

    /**
     * Gets the image wrapped by this object
     *
     * @return the image wrapped by this wrapper
     */
    public Image getImage() {
        return image;
    }

    /**
     * Gets the path of the image this wrapper contains
     *
     * @return the path of the image in this wrapper, as a Path object
     */
    public Path getPath() {
        return this.image.getPath();
    }

    /**
     * Returns the string representation of the image this wrapper contains
     *
     * @return the string representation of the image this wrapper contains
     */
    @Override
    public String toString() {
        return this.image.toString();
    }

    /**
     * An ImageWrapper is equal to this one only if all its fields are equal
     * to this one's
     *
     * @param o the object to compare to
     * @return true if o is equal to this, false if otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageWrapper that = (ImageWrapper) o;

        return image != null ? image.equals(that.image) : that.image
                == null;
    }

    /**
     * Calculates the hash code for this object using its image field
     *
     * @return the hash code for this ImageWrapper
     */
    @Override
    public int hashCode() {
        return image != null ? image.hashCode() : 0;
    }
}
