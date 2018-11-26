package Done.md2html;

import java.io.*;
import java.util.*;

public class Md2Html {
    private static Map<String, String> tags = new HashMap<>();

    static {
        tags.put("*", "em");
        tags.put("**", "strong");
        tags.put("_", "em");
        tags.put("__", "strong");
        tags.put("--", "s");
        tags.put("`", "code");
        tags.put("++", "u");
        tags.put("~", "mark");
    }

    private static Map<String, String> codeSymbols = new HashMap<>();

    static {
        codeSymbols.put("<", "&lt;");
        codeSymbols.put(">", "&gt;");
        codeSymbols.put("&", "&amp;");
    }

    private static String lineSep = System.lineSeparator();
    private static TreeSet<String> openedTags = new TreeSet<>();
    private static int cur;
    private static int previous = ' ';
    private static BufferedReader reader;
    private static BufferedWriter writer;
    private static boolean newParagOrHead;
    private static boolean isBeginingOfFile;
    private static boolean isCode;

    private static boolean ifTag(char c) {
        return c == '*' || c == '-' || c == '_' || c == '`' || c == '+' || c == '~';
    }

    private static String createTag(String tag, boolean ifOpen) {
        return (ifOpen ? "<" : "</") + tags.getOrDefault(tag, tag) + ">";
    }

    private static boolean isLineSep(char cur, char nxt) {
        return lineSep.length() == 1 && String.valueOf(nxt).equals(lineSep) ||
                lineSep.length() == 2 && (String.valueOf(cur) + String.valueOf(nxt)).equals(lineSep);
    }

    private static boolean isLineSepSymbol(char c) {
        return c == '\n' || c == '\r';
    }

    private static void startBlock(String tag) throws IOException {
        newParagOrHead = false;
        writer.write(createTag(tag, true));
        openedTags.add(tag);
    }

    private static void endBlock() throws IOException {
        if (openedTags.contains("p") || !openedTags.isEmpty() && openedTags.last().charAt(0) == 'h') {
            writer.write(createTag(openedTags.pollLast(), false));
            writer.write(lineSep);
            newParagOrHead = true;
        }
    }

    private static void writeTag(StringBuilder tagToAdd) throws IOException {
        if (openedTags.contains(tagToAdd.toString())) {
            writer.write(createTag(tagToAdd.toString(), false));
            openedTags.remove(tagToAdd.toString());
        } else {
            writer.write(createTag(tagToAdd.toString(), true));
            openedTags.add(tagToAdd.toString());
        }
    }

    private static void readEmptyLines() throws IOException {
        cur = reader.read();
        while (isLineSepSymbol((char) cur)) {
            cur = reader.read();
        }
        if (!isBeginingOfFile) {
            endBlock();
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Wrong amount of arguments");
        } else {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "utf8"))) {
                try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "utf8"))) {
                    reader = in;
                    writer = out;
                    newParagOrHead = true;
                    isBeginingOfFile = true;
                    isCode = false;
                    readEmptyLines();
                    while (cur != -1) {
                        if (newParagOrHead) {
                            if ((char) cur == '#') {
                                StringBuilder header = new StringBuilder();
                                while ((char) cur == '#') {
                                    header.append((char) cur);
                                    cur = reader.read();
                                }
                                if (Character.isWhitespace(cur)) {
                                    startBlock("h" + header.length());
                                } else {
                                    startBlock("p");
                                    writer.write(header.toString());
                                    continue;
                                }
                            } else {
                                startBlock("p");
                                continue;
                            }
                        } else {
                            if (ifTag((char) cur)) {
                                int temp = cur;
                                cur = reader.read();
                                StringBuilder tagToAdd = new StringBuilder();
                                if ((temp == '-' || temp == '+') && temp != cur) {
                                    writer.write((char) temp);
                                } else {
                                    tagToAdd.append((char) temp);
                                    if (temp == cur) {
                                        tagToAdd.append((char) cur);
                                        cur = reader.read();
                                    }
                                    if (tagToAdd.toString().equals("`")) {
                                        isCode = !isCode;
                                    }
                                    if (Character.isWhitespace(previous) && Character.isWhitespace(cur)) {
                                        writer.write(tagToAdd.toString());
                                    } else {
                                        writeTag(tagToAdd);
                                    }
                                }
                                continue;
                            } else if (isLineSep((char) previous, (char) cur)) {
                                previous = cur;
                                cur = reader.read();
                                if (cur == -1) {
                                    break;
                                }
                                if (isLineSep((char) previous, (char) cur)) {
                                    readEmptyLines();
                                    endBlock();
                                    continue;
                                } else {
                                    writer.write(lineSep);
                                    continue;
                                }
                            } else {
                                if ((char) cur == '\\') {
                                    cur = reader.read();
                                    if (cur == -1) {
                                        break;
                                    }
                                }
                                if (isCode) {
                                    writer.write(codeSymbols.getOrDefault(Character.toString((char) cur), Character.toString((char) cur)));
                                } else {
                                    writer.write((char) cur);
                                }
                            }
                        }
                        isBeginingOfFile = false;
                        previous = cur;
                        cur = reader.read();
                    }
                    endBlock();
                } catch (FileNotFoundException e) {
                    System.out.println("Can't write to output file");
                }
            } catch (FileNotFoundException e) {
                System.out.print("Input file not found");
            } catch (UnsupportedEncodingException e) {
                System.out.print("Unsupported encoding");
            } catch (IOException e) {
                System.out.print("Error while converting");
            }
        }
    }
}