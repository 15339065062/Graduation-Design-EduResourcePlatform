package com.edu.util;

import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageProcessorTest {
    @Test
    public void processAndSaveResizesAndOutputsJpeg() throws Exception {
        File in = File.createTempFile("img_in_", ".png");
        File out = File.createTempFile("img_out_", ".jpg");
        in.deleteOnExit();
        out.deleteOnExit();

        BufferedImage img = new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", in);

        ImageProcessor.ProcessedImage result = ImageProcessor.processAndSave(in, "image/png", out, 1080, 1080);

        Assert.assertTrue(out.exists());
        Assert.assertTrue(result.fileSize() > 0);
        Assert.assertEquals("image/jpeg", result.mimeType());
        Assert.assertTrue(result.width() <= 1080);
        Assert.assertTrue(result.height() <= 1080);
    }
}

