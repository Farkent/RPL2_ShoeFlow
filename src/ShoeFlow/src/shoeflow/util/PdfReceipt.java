package shoeflow.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import shoeflow.model.CartItem;
import shoeflow.model.Sale;

/**
 * Pembuat struk digital berformat PDF TANPA library tambahan (PDF ditulis manual, font Courier).
 * Lebar kertas ~80mm (struk thermal). File disimpan di folder struk/.
 */
public final class PdfReceipt {
    private static final int W = 40;   // jumlah karakter per baris

    private PdfReceipt() { }

    public static File create(Sale sale, List<CartItem> items) throws IOException {
        File dir = new File("struk");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, sale.getInvoiceNo() + ".pdf");
        writePdf(out, buildLines(sale, items));
        return out;
    }

    /** Menyusun isi struk per baris (juga bisa dipakai untuk preview teks). */
    public static List<String> buildLines(Sale s, List<CartItem> items) {
        List<String> l = new ArrayList<>();
        String line = repeat('-', W);
        l.add(center("SHOEFLOW"));
        l.add(center("Struk Penjualan"));
        l.add(line);
        l.add("No    : " + s.getInvoiceNo());
        l.add("Tgl   : " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(s.getCreatedAt()));
        l.add("Kasir : " + s.getCashierName());
        l.add(line);
        for (CartItem i : items) {
            l.add(cut(i.getProductName() + " " + i.getSize() + "/" + i.getColor(), W));
            l.add(lr("  " + i.getQty() + " x " + Rupiah.format(i.getUnitPrice()), Rupiah.format(i.getSubtotal())));
        }
        l.add(line);
        l.add(lr("TOTAL", Rupiah.format(s.getTotal())));
        l.add(lr("Metode", "tunai".equals(s.getPaymentMethod()) ? "Tunai" : "Transfer Bank"));
        l.add(lr("Dibayar", Rupiah.format(s.getAmountPaid())));
        l.add(lr("Kembali", Rupiah.format(s.getChangeAmount())));
        l.add(line);
        l.add(center("Terima kasih"));
        l.add(center("Barang yang sudah dibeli"));
        l.add(center("tidak dapat ditukar"));
        return l;
    }

    // ---------------------------------------------------------- penulis PDF minimal
    private static void writePdf(File file, List<String> lines) throws IOException {
        final int fontSize = 8, leading = 10, margin = 14;
        int width = 240, height = margin * 2 + leading * lines.size();

        StringBuilder cs = new StringBuilder();
        cs.append("BT\n/F1 ").append(fontSize).append(" Tf\n").append(leading).append(" TL\n");
        cs.append(margin).append(' ').append(height - margin - fontSize).append(" Td\n");
        for (String ln : lines) cs.append('(').append(escape(ln)).append(") Tj T*\n");
        cs.append("ET\n");
        byte[] content = cs.toString().getBytes(StandardCharsets.ISO_8859_1);

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        put(bo, "%PDF-1.4\n");
        obj(bo, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        obj(bo, offsets, 2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        obj(bo, offsets, 3, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + width + " " + height
                + "] /Contents 5 0 R /Resources << /Font << /F1 4 0 R >> >> >>");
        obj(bo, offsets, 4, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>");
        offsets.add(bo.size());
        put(bo, "5 0 obj\n<< /Length " + content.length + " >>\nstream\n");
        bo.write(content);
        put(bo, "\nendstream\nendobj\n");

        int xref = bo.size();
        put(bo, "xref\n0 6\n0000000000 65535 f \n");
        for (int off : offsets) put(bo, String.format("%010d 00000 n \n", off));
        put(bo, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");

        try (FileOutputStream fo = new FileOutputStream(file)) { bo.writeTo(fo); }
    }

    private static void obj(ByteArrayOutputStream bo, List<Integer> offsets, int n, String body) throws IOException {
        offsets.add(bo.size());
        put(bo, n + " 0 obj\n" + body + "\nendobj\n");
    }

    private static void put(ByteArrayOutputStream bo, String s) throws IOException {
        bo.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c < 32 || c > 126) c = '?';
            if (c == '\\' || c == '(' || c == ')') sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }

    private static String repeat(char c, int n) { StringBuilder sb = new StringBuilder(); for (int i = 0; i < n; i++) sb.append(c); return sb.toString(); }
    private static String cut(String s, int n) { return s.length() <= n ? s : s.substring(0, n); }
    private static String center(String s) { int p = Math.max(0, (W - s.length()) / 2); return repeat(' ', p) + s; }
    private static String lr(String left, String right) {
        int gap = Math.max(1, W - left.length() - right.length());
        return left + repeat(' ', gap) + right;
    }
}
