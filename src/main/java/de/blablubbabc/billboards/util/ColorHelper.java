package de.blablubbabc.billboards.util;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorHelper {
    private static final Pattern startWithColor = Pattern.compile("^(&[LMNKOlmnko])+");
    private static final Pattern gradientPattern = Pattern.compile("\\{(#[ABCDEFabcdef0123456789]{6}):(#[ABCDEFabcdef0123456789]{6}):(.*?)}");
    private static final Pattern hexPattern = Pattern.compile("&(#[ABCDEFabcdef0123456789]{6})");

    public static boolean t(CommandSender sender, String... msg) {
        sender.sendMessage(ColorHelper.parseColor(String.join("\n&r", msg)));
        return true;
    }

    public static boolean t(CommandSender sender, List<String> msg) {
        sender.sendMessage(ColorHelper.parseColor(String.join("\n&r", msg)));
        return true;
    }

    public static TextComponent bungee(String s) {
        return new TextComponent(parseColor(s));
    }

    @SuppressWarnings({"deprecation"})
    public static HoverEvent hover(String s) {
        return new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[]{ bungee(s) }
        );
    }

    public static HoverEvent hover(List<String> s) {
        return hover(String.join("\n", s));
    }

    public static List<String> parseColor(List<String> s) {
        return Lists.newArrayList(parseColor(String.join("\n", s)).split("\n"));
    }

    public static String parseColor(String s) {
        String fin = parseHexText(s);
        fin = parseGradientText(fin);
        return fin.replace("&", "§");
    }

    public static String parseHexText(String s) {
        return String.join("", split(hexPattern, s, regexResult -> {
            if (!regexResult.isMatched) return regexResult.text;
            String hex = regexResult.text.substring(1);
            return parseHex(hex);
        }));
    }

    public static String parseGradientText(String s) {
        return String.join("", split(gradientPattern, s, regexResult -> {
            if (!regexResult.isMatched) return regexResult.text;
            String[] args = regexResult.text.substring(1, regexResult.text.length() - 1).split(":", 3);
            String extra = "";
            Matcher m = startWithColor.matcher(args[2]);
            if (m.find()) {
                extra = ChatColor.translateAlternateColorCodes('&', m.group());
            }
            return parseGradient(m.replaceAll(""), extra, args[0], args[1]);
        }));
    }

    /**
     * 生成 Minecraft 1.16+ 渐变颜色文字
     *
     * @param s           字符串
     * @param extraFormat 额外样式
     * @param startHex    开始颜色 (#XXXXXX)
     * @param endHex      结束颜色 (#XXXXXX)
     * @return 渐变文字
     */
    public static String parseGradient(String s, String extraFormat, String startHex, String endHex) {
        s = s.replaceAll("[&§].", "");
        int color1 = hex(startHex);
        int color2 = hex(endHex);
        int[] colors = createGradient(color1, color2, s.length());
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < colors.length; i++) {
            result.append(hexToMc(colors[i])).append(extraFormat).append(s.charAt(i));
        }
        return result.toString();
    }

    /**
     * 生成 Minecraft 1.16+ 16进制颜色代码
     *
     * @param hex 16进制颜色 (#XXXXXX)
     * @return 颜色代码
     */
    public static String parseHex(String hex) {
        StringBuilder result = new StringBuilder("§x");
        for (char c : hex.substring(1, hex.length() - 1).toLowerCase().toCharArray()) {
            result.append('§').append(c);
        }
        result.append("§F");
        return result.toString();
    }

    public static int[] createGradient(int startHex, int endHex, int step) {
        if (step == 1) return new int[]{startHex};

        int[] colors = new int[step];
        int[] start = hexToRGB(startHex);
        int[] end = hexToRGB(endHex);

        int stepR = (end[0] - start[0]) / (step - 1);
        int stepG = (end[1] - start[1]) / (step - 1);
        int stepB = (end[2] - start[2]) / (step - 1);

        for (int i = 0; i < step; i++) {
            colors[i] = rgbToHex(
                    start[0] + stepR * i,
                    start[1] + stepG * i,
                    start[2] + stepB * i
            );
        }
        return colors;
    }

    public static String hexToMc(int hex) {
        return parseHex(hex(hex));
    }

    public static int hex(String hex) {
        return Integer.parseInt(hex.substring(1), 16);
    }

    public static String hex(int hex) {
        return "#" + String.format("%06x", hex);
    }

    public static int[] hexToRGB(int hex) {
        return new int[]{
                (hex >> 16) & 0xff,
                (hex >> 8) & 0xff,
                hex & 0xff
        };
    }

    public static int rgbToHex(int r, int g, int b) {
        return (r << 16) + (g << 8) + b;
    }

    public static <T> List<T> split(Pattern regex, String s, Function<RegexResult, T> transform) {
        List<T> list = new ArrayList<>();
        int index = 0;
        Matcher m = regex.matcher(s);
        while (m.find()) {
            int first = m.start();
            int last = m.end();
            if (first > index) {
                T value = transform.apply(new RegexResult(null, s.substring(index, first)));
                if (value != null) list.add(value);
            }
            T value = transform.apply(new RegexResult(m.toMatchResult(), s.substring(first, last)));
            if (value != null) list.add(value);
            index = last;
        }
        if (index < s.length()) {
            T value = transform.apply(new RegexResult(null, s.substring(index)));
            if (value != null) list.add(value);
        }
        return list;
    }

    public static class RegexResult {
        public final MatchResult result;
        public final boolean isMatched;
        public final String text;

        public RegexResult(MatchResult result, String text) {
            this.result = result;
            this.isMatched = result != null;
            this.text = text;
        }
    }
}
