package ru.inponomarev.semrush;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Main {

    private static boolean isInFooter(Element e) {
        for (Element parent : e.parents()) {
            /*Если <p> находится внутри <footer>, либо внутри элемента, id которого
            содержит слово footer -- то такой <p> нам уже не нужен.
             */
            if ("footer".equals(parent.tagName())
                    || parent.id().contains("footer"))
                return true;
        }
        return false;
    }


    public static void parsePage(URL url, PrintWriter osw) throws IOException {
        Document d = Jsoup.parse(url, 10000);
        int state = 0;
        elementsCycle:
        for (Element e : d.getAllElements()) {
            String innerText = e.text().trim();
            /*
            Используем самую грубую эвристику: извлекаем <h1> (главный заголовок)
            и все следующие за ним <p>.
             */
            if (!innerText.isEmpty())
                switch (state) {
                    case 0: //ищем заголовок
                        if ("h1".equalsIgnoreCase(e.tagName())) {
                            osw.println("# " + innerText);
                            osw.println();
                            state = 1;
                        }
                        break;
                    case 1: //читаем абзацы
                        if ("p".equalsIgnoreCase(e.tagName())) {
                            if (isInFooter(e)) {
                                //System.out.printf("footer: %s", innerText);
                                break elementsCycle;
                            }
                            parseParagraph(osw, e);
                        }
                }
        }
    }

    private static void parseParagraph(PrintWriter osw, Element e) {
        StringBuilder txt = new StringBuilder();
        boolean linkOnly = true;
        for (Node child : e.childNodes()) {
            if (child instanceof TextNode) {
                TextNode tn = (TextNode) child;
                txt.append(tn.getWholeText());
                linkOnly = false;
            } else if (child instanceof Element) {
                Element childe = (Element) child;
                if ("a".equalsIgnoreCase(childe.tagName())) {
                    txt.append("[");
                    txt.append(childe.text());
                    txt.append("](");
                    txt.append(childe.attr("href"));
                    txt.append(")");
                } else if ("b".equalsIgnoreCase(childe.tagName())
                        || "strong".equalsIgnoreCase(childe.tagName())) {
                    txt.append("**");
                    txt.append(childe.text());
                    txt.append("**");
                    linkOnly = false;
                } else if ("i".equalsIgnoreCase(childe.tagName())) {
                    txt.append("*");
                    txt.append(childe.text());
                    txt.append("*");
                    linkOnly = false;
                } else {
                    txt.append(childe.text());
                    linkOnly = false;
                }
            }
        }
        /*
        Абзац, состоящий только из ссылки, скорее всего является элементом навигации.
         */
        if (!linkOnly) {
            osw.println(txt.toString().trim());
            osw.println();
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

