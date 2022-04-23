package analyzer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;


interface SearchStrategy {
    boolean findSubstr(String text, String pattern);
}

class RKSearch implements SearchStrategy {

    long a = 3;
    long m = 127;


    long findHash(String str) {
        char[] chars = str.toCharArray();
        long out = 0;
        for (int i = 0; i < chars.length; i++) {
            out += chars[i] * (long) Math.pow(a, i);
        }
        return out % m;
    }

    long findNextHash(char newC, char oldC, long hash, int pLen) {
        long out = ((hash - oldC * (long) Math.pow(a, (pLen - 1))) * a + newC) % m;
        out = (out % m + m) % m;
        return out;
    }

    @Override
    public boolean findSubstr(String text, String pattern) {

        long pHash = findHash(pattern);
        int begin = text.length() - pattern.length();
        if (begin < 0 || text.length() < pattern.length()) {
            return false;
        }
        long tempHash = findHash(text.substring(begin));
        String temp = text.substring(begin);
        if (text.substring(begin).equals(pattern)) {
            return true;
        }
        for (int i = begin - 1; i >= -1; i--) {
            if (tempHash == pHash) {
                if (temp.equals(pattern)) {
                    return true;
                }
            }
            if (i != -1) {
                char prev = temp.charAt(temp.length() - 1);
                temp = text.substring(i, i + pattern.length());
                char next = temp.charAt(0);
                tempHash = findNextHash(next, prev, tempHash, pattern.length());
            }
        }
        return false;
    }
}

class KMPSearch implements SearchStrategy {


    int[] prefixFunction(String str) {
        char[] s = str.toCharArray();
        int n = s.length;
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

class FilePattern implements Comparable<FilePattern>{
    public int priority;
    public String pattern;
    public String fileType;

    FilePattern(int n, String pattern, String type) {
        this.priority = n;
        this.pattern = pattern;
        this.fileType = type;
    }

    @Override
    public int compareTo(FilePattern o) {
        return Integer.compare(priority, o.priority);
    }
}

class FileChecker implements Callable<String> {
    FilePattern[] filePatterns;
    String fileContent;
    SearchStrategy algorithm;
    String fileName;
    String rootName;

    FileChecker(String fileName, SearchStrategy algorithm, FilePattern[] filePatterns, String rootName) {
        this.fileName = fileName;
        this.algorithm = algorithm;
        this.rootName = rootName;
        this.filePatterns = filePatterns;
        byte[] data = new byte[0];
        try {
            File temp = new File(rootName + "/" + fileName);
            if (temp.isFile()) {
                data = Files.readAllBytes(temp.toPath());
            }
        } catch (RuntimeException | IOException e) {
            System.out.println(e.getMessage());
            System.out.println("LOLKEKW");
        }
        this.fileContent = new String(data, StandardCharsets.UTF_8);

    }

    @Override
    public String call() {
        StringBuilder out = new StringBuilder(fileName + ": ");
        String type = null;
        for (FilePattern filePattern : filePatterns) {
            if (algorithm.findSubstr(fileContent, filePattern.pattern)) {
                type = filePattern.fileType;
                break;
            }
        }
        out.append(Objects.requireNonNullElse(type, "Unknown file type"));
        return out.toString();
    }
}

class Utils {
    static public String readFileToString(String fileName) {
        byte[] data = new byte[0];
        try {
            File temp = new File(fileName);
            if (temp.isFile()) {
                data = Files.readAllBytes(temp.toPath());
            }
        } catch (RuntimeException | IOException e) {
            System.out.println(e.getMessage());
            System.out.println("LOLKEKW");
        }
        return new String(data, StandardCharsets.UTF_8);
    }
}

class Analyzer {

    FilePattern[] filePatterns;
    File[] files;
    SearchStrategy algorithm;
    String rootName;

    void initPatterns(String fileName) {
        String[] lines = Utils.readFileToString(fileName).split("\\n");
        FilePattern[] out = new FilePattern[lines.length];
        for (int i = 0; i < lines.length; i ++) {
            String[] temp = lines[i].replaceAll("\"", "").split(";", 0);
            out[i] = new FilePattern(Integer.parseInt(temp[0]), temp[1], temp[2]);
        }

        Arrays.sort(out, Collections.reverseOrder());
        filePatterns = out;
    }



    Analyzer(String[] args) {
        initPatterns(args[1]);
        this.algorithm = new RKSearch();
        //this.algorithm = new KMPSearch();
        //this.pattern = "ABBA";
        this.rootName = args[0];
        files = new File(args[0]).listFiles();
    }

    public void execute() {
        ExecutorService executor = Executors.newFixedThreadPool(files.length);
        List<Callable<String>> callables = new ArrayList<>();

        for (File file : files) {
            callables.add(new FileChecker(file.getName(), algorithm, filePatterns, rootName));
        }
        try {
            List<Future<String>> futures = executor.invokeAll(callables);
            for (Future<String> future : futures) {
                System.out.println(future.get());
            }
        } catch (InterruptedException ignore) {}
        catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}

public class Main {

    public static void main(String[] args) {
        if (args.length != 2) {
            //ERROR
            return;
        }
        Analyzer a = new Analyzer(args);
        a.execute();

    }
}