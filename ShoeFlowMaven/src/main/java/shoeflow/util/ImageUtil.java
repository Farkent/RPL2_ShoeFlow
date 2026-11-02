package shoeflow.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/** Memuat gambar produk (path relatif tersimpan di products.image) dan menyalin gambar baru ke folder images/. */
public final class ImageUtil {
    public static final String DIR = "images";
    private static final Map<String, ImageIcon> CACHE = new HashMap<>();

    private ImageUtil() { }

    public static synchronized ImageIcon load(String path, int w, int h) {
        String key = path + "|" + w + "x" + h;
        ImageIcon cached = CACHE.get(key);
        if (cached != null) return cached;
        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xF1F5F9));
        g.fillRect(0, 0, w, h);
        boolean drawn = false;
        try {
            if (path != null && !path.trim().isEmpty()) {
                File f = new File(path);
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        double s = Math.min((double) w / img.getWidth(), (double) h / img.getHeight());
                        int nw = (int) (img.getWidth() * s), nh = (int) (img.getHeight() * s);
                        g.drawImage(img, (w - nw) / 2, (h - nh) / 2, nw, nh, null);
                        drawn = true;
                    }
                }
            }
        } catch (IOException ignore) { }
        if (!drawn) {   // placeholder bila gambar belum ada
            g.setColor(new Color(0x94A3B8));
            g.setFont(g.getFont().deriveFont(Font.BOLD, Math.max(10, h / 8f)));
            String t = "No Image";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(t, (w - fm.stringWidth(t)) / 2, h / 2 + fm.getAscent() / 3);
        }
        g.dispose();
        ImageIcon icon = new ImageIcon(canvas);
        CACHE.put(key, icon);
        return icon;
    }

    /** Menyalin file gambar ke folder images/ dan mengembalikan path relatif untuk disimpan di database. */
    public static String copyToImages(File src) throws IOException {
        File dir = new File(DIR);
        if (!dir.exists()) dir.mkdirs();
        String name = System.currentTimeMillis() + "_" + src.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        Files.copy(src.toPath(), new File(dir, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
        return DIR + "/" + name;
    }
}
