package com.flomio.smartcartlib.util;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Locale;

public class Format {
    public static String ip(byte[] ip) {
        return String.format(Locale.US, "%d.%d.%d.%d",
                ip[0] & 0xff,
                ip[1] & 0xff,
                ip[2] & 0xff,
                ip[3] & 0xff
                );
    }

    public static String money(BigDecimal bd) {
        bd = bd.setScale(2, BigDecimal.ROUND_DOWN);

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        df.setGroupingUsed(true);

        return df.format(bd);
    }

    public static String decodeUTF8(byte[] bytes) {
        return new String(bytes, Charset
                .forName("utf8"));
    }
}
