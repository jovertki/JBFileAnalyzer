package analyzer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;


interface SearchStrategy {
    public boolean findSubstr(String text, String pattern);
}

class KMPSearch implements SearchStrategy {

    int[] prefixFunction(String str) {
        char[] s = str.toCharArray();
        int n = (int)s.length;
        int[] pi = new int[n];
        for (int i = 1; i < n; i++) {
            int j = pi[i-1];
            while (j > 0 && s[i] != s[j])
                j = pi[j-1];
            if (s[i] == s[j])
                j++;
            pi[i] = j;
        }
        return pi;
    }

    @Override
    public boolean findSubstr(String txt, String pat){
        int i = 0;
        int j = 0;
        int M = pat.length();
        int N = txt.length();
        int[] lps = prefixFunction(pat);
        while (i < N) {
            if (pat.charAt(j) == txt.charAt(i)) {
                j++;
                i++;
            }
            if (j == M) {
                return true;
            }

            // mismatch after j matches
            else if (i < N && pat.charAt(j) != txt.charAt(i)) {
                // Do not match lps[0..lps[j-1]] characters,
                // they will match anyway
                if (j != 0)
                    j = lps[j - 1];
                else
                    i = i + 1;
            }
        }

        //  System.out.println(Arrays.toString(prefix));
        return false;

    }

}

class Analyzer {
    String pattern;
    String fileType;
    File[] files;
    //String fileContent;
    SearchStrategy algorithm;

    Analyzer(String[] args) {
        this.algorithm = new KMPSearch();
        //this.pattern = "ABBA";
        this.pattern = args[1];
        this.fileType = args[2];
        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(new File(args[1]).toPath());
        } catch (RuntimeException | IOException e) {
            System.out.println(e.getMessage());
        }
        files = new File(args[0]).listFiles();
        //this.fileContent = new String(data, StandardCharsets.UTF_8);
    }

    public String execute() {
        if (algorithm.findSubstr(fileContent, pattern)) {
            return fileType;
        } else {
            return "Unknown file type";
        }
    }
}

public class Main {
    String pattern;
    String fileType;
    String fileContent;
    SearchStrategy algorithm;



    public static void main(String[] args) {
        if (args.length != 3) {
            //ERROR
            return;
        }
        Analyzer a = new Analyzer(args);
        long begin = System.nanoTime();
        System.out.println(a.execute());
        double diff = (System.nanoTime() - (double) begin) / 1_000_000_000;
        System.out.printf("It took %f seconds\n", diff);
    }
}