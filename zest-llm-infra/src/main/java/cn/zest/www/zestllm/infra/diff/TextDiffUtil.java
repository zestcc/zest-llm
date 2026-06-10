package cn.zest.www.zestllm.infra.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * 行级 unified diff（对标 GitHub PR diff 预览）。
 */
public final class TextDiffUtil {

    private TextDiffUtil() {
    }

    public static String unifiedLineDiff(String before, String after) {
        String[] left = splitLines(before);
        String[] right = splitLines(after);
        int m = left.length;
        int n = right.length;
        int[][] lcs = new int[m + 1][n + 1];
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (left[i].equals(right[j])) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }
        List<String> lines = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < m && j < n) {
            if (left[i].equals(right[j])) {
                lines.add(" " + left[i]);
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                lines.add("-" + left[i]);
                i++;
            } else {
                lines.add("+" + right[j]);
                j++;
            }
        }
        while (i < m) {
            lines.add("-" + left[i++]);
        }
        while (j < n) {
            lines.add("+" + right[j++]);
        }
        return String.join("\n", lines);
    }

    private static String[] splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        return text.split("\n", -1);
    }
}
