package main;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagTest {
    private Tag tag;
    private Image image;
    private File imageFile;

    @BeforeEach
    void setUp() {
        imageFile = new File("test1.jpg");
        image = new Image(imageFile, "test1");
        tag = new Tag(image, "blah");
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void equals() {
        File sameImageFile = new File("test1.jpg");
        Image sameImage = new Image(sameImageFile, "test1");
        Tag compareTag = new Tag(sameImage, "blah");
        assertEquals(tag, compareTag);
    }

}