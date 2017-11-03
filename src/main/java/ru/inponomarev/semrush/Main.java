package ru.inponomarev.semrush;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Main {

    private static boolean isInFooter(Element e) {
        for (Element parent : e.parents()) {
            /*Если <p> находится внутри <footer>, либо внутри элемента, id которого
            содержит слово footer -- то такой <p> нам не нужен.
             */
            if ("footer".equals(parent.tagName())
                    || parent.id().contains("footer"))
                return true;
        }
        return false;
    }


    public static void parsePage(URL url, PrintWriter osw) throws IOException {
        Document d = Jsoup.parse(url, 10000);
        for (Element e : d.getAllElements()) {

            String innerText = e.text().trim();
            /*
            Используем самую грубую эвристику: извлекаем <h1> -- главный заголовок -- и <p>.
             */
            if (!innerText.isEmpty()) {
                if ("p".equalsIgnoreCase(e.tagName())) {
                    if (isInFooter(e)) {
                        //System.out.printf("footer: %s", innerText);
                        continue;
                    }
                    osw.println(innerText);
                } else if ("h1".equalsIgnoreCase(e.tagName())) {
                    StringBuilder l = new StringBuilder();
                    l.append('#');
                    l.append(' ');
                    l.append(innerText);
                    osw.println(l.toString());
                }
                osw.println();
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

