package com.mottu.ops.infrastructure.adapter.out.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Component
public class PerceptualHashService {

    private static final Logger log = LoggerFactory.getLogger(PerceptualHashService.class);

    public String computeDHash(byte[] imageBytes, String sha256Fallback) {
        if (imageBytes == null || imageBytes.length == 0) {
            return (sha256Fallback != null && sha256Fallback.length() >= 16)
                    ? sha256Fallback.substring(0, 16).toLowerCase()
                    : "0000000000000000";
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                return (sha256Fallback != null && sha256Fallback.length() >= 16)
                        ? sha256Fallback.substring(0, 16).toLowerCase()
                        : "0000000000000000";
            }

            // Resize to 9x8 grayscale
            BufferedImage small = new BufferedImage(9, 8, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = small.createGraphics();
            g.drawImage(image.getScaledInstance(9, 8, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();

            long hash = 0L;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int left = small.getRaster().getSample(x, y, 0);
                    int right = small.getRaster().getSample(x + 1, y, 0);
                    if (left > right) {
                        hash |= (1L << (y * 8 + x));
                    }
                }
            }

            return String.format("%016x", hash);
        } catch (Exception e) {
            log.debug("Could not compute visual dHash via ImageIO: {}, using SHA fallback", e.getMessage());
            return (sha256Fallback != null && sha256Fallback.length() >= 16)
                    ? sha256Fallback.substring(0, 16).toLowerCase()
                    : "0000000000000000";
        }
    }
}
