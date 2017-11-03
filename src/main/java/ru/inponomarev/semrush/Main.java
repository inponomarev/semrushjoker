package ru.inponomarev.semrush;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final Pattern h = Pattern.compile("h(1)");

    public static void parsePage(URL url, PrintWriter osw) throws IOException {
        Document d = Jsoup.parse(url, 10000);
        for (Element e : d.getAllElements()) {

            String innerText = e.text().trim();
            if (!innerText.isEmpty()) {
                if ("p".equals(e.tagName())) {
                    osw.println(innerText);
                    osw.println();
                } else {
                    Matcher m = h.matcher(e.tagName());
                    if (m.matches()) {
                        int level = Integer.parseInt(m.group(1));
                        StringBuilder l = new StringBuilder();
                        for (int i = 0; i < level; i++)
                            l.append('#');
                        l.append(' ');
                        l.append(innerText);
                        osw.println(l.toString());
                        osw.println();
                    }
                }
            }
        }
    }

    public static void main(String... args) throws IOException {
        if (args.length < 1) {
            System.out.println("Please provide a path to links file in command-line argument");
            return;
        }
        File f = new File(args[0]);
        if (!(f.exists() && f.canRead())) {
            System.out.printf("File %s does not exists or cannot be read.%n", args[0]);
            return;
        }

        try (BufferedReader r =
                     new BufferedReader(
                             new InputStreamReader(
                                     new FileInputStream(f)))) {
            String urlLine;
            int i = 0;
            while ((urlLine = r.readLine()) != null) {
                i++;
                URL url = new URL(urlLine);
                File outFile = new File(i + ".md");
                try (PrintWriter pw = new PrintWriter(
                        new OutputStreamWriter(
                        new FileOutputStream(outFile),
                        StandardCharsets.UTF_8)
                        )) {
                    System.out.printf("%s -> %s%n", url, outFile);
                    try {
                        parsePage(url, pw);
                    } catch (SSLHandshakeException e) {
                        System.out.println("Oops, something is wrong with certificate!");
                        e.printStackTrace(pw);
                    }
                }
            }
        }
    }
}

