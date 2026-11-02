package shoeflow.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/** Helper format & parsing mata uang Rupiah, contoh: Rp1.250.000 */
public class Rupiah {
    private static final DecimalFormat FMT;
    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        FMT = new DecimalFormat("#,##0", s);
    }

    public static synchronized String format(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return (v.signum() < 0 ? "-" : "") + "Rp" + FMT.format(v.abs());
    }

    /** Mengambil angka saja dari teks input; kosong -> 0. Tanda minus otomatis dibuang (anti angka negatif). */
    public static BigDecimal parse(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? BigDecimal.ZERO : new BigDecimal(digits);
    }
}
