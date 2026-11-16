package util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyFormat {
    private CurrencyFormat() { }
    public static String peso(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(2); format.setMaximumFractionDigits(2);
        return "\u20b1" + format.format(amount);
    }
}
