package com.edu.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;

public class ImageProcessor {
    public record ProcessedImage(int width, int height, String mimeType, long fileSize) {}

    public static ProcessedImage processAndSave(File inputFile, String inputMimeType, File outputFile, int maxWidth, int maxHeight) throws IOException {
        BufferedImage original = ImageIO.read(inputFile);
        if (original == null) {
            throw new IOException("Unsupported image");
        }

        int ow = original.getWidth();
        int oh = original.getHeight();

        double scale = Math.min((double) maxWidth / ow, (double) maxHeight / oh);
        scale = Math.min(scale, 1.0);
        int tw = Math.max(1, (int) Math.round(ow * scale));
        int th = Math.max(1, (int) Math.round(oh * scale));

        BufferedImage target = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, tw, th, null);
        g.dispose();

        outputFile.getParentFile().mkdirs();

        String format = "jpg";
        String mime = "image/jpeg";
        writeJpeg(target, outputFile, 0.82f);

        long size = Files.size(outputFile.toPath());
        return new ProcessedImage(tw, th, mime, size);
    }

    public static String detectImageMimeType(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = fis.readNBytes(12);
            if (header.length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            if (header.length >= 8
                    && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
                return "image/png";
            }
        }
        String probe = Files.probeContentType(file.toPath());
        return probe;
    }

    private static void writeJpeg(BufferedImage image, File outputFile, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(Math.max(0.1f, Math.min(quality, 0.95f)));
        }
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}

