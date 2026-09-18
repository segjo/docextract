package ch.adeon.apps.docextract.structuring.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits {@link StructuredContent} into token-budgeted, heading-contextualized {@link DocChunk}s
 * (ADR-007). This is a heuristic stand-in for docling's native {@code HybridChunker}: it tracks
 * Markdown {@code #}-headings to build a heading path per paragraph, then greedily packs paragraphs
 * into chunks up to {@link ChunkingConfig#maxTokens()} (approximated by word count). Plain-text
 * input (no headings, e.g. the PDFBox fast-track) simply yields flat, un-contextualized chunks.
 * Chunks are contextualized by prefixing the heading path, mirroring docling's {@code
 * contextualize()}.
 */
public final class HeadingAwareChunker {

  private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");

  private HeadingAwareChunker() {}

  public static List<DocChunk> chunk(StructuredContent content, ChunkingConfig config) {
    List<DocChunk> chunks = new ArrayList<>();
    Deque<String> headingStack = new ArrayDeque<>();
    List<String> currentHeadingPath = List.of();
    StringBuilder paragraph = new StringBuilder();
    List<String> pendingParagraphs = new ArrayList<>();
    List<String> pendingHeadingPath = List.of();

    for (String line : content.text().split("\n", -1)) {
      Matcher matcher = HEADING.matcher(line.strip());
      if (matcher.matches()) {
        flushParagraph(paragraph, pendingParagraphs, pendingHeadingPath);
        flushSection(pendingParagraphs, pendingHeadingPath, chunks, config);
        pendingHeadingPath = List.of();

        int level = matcher.group(1).length();
        String title = matcher.group(2).strip();
        while (headingStack.size() >= level) {
          headingStack.removeLast();
        }
        headingStack.addLast(title);
        currentHeadingPath = List.copyOf(headingStack);
        continue;
      }
      if (line.isBlank()) {
        flushParagraph(paragraph, pendingParagraphs, pendingHeadingPath);
        pendingHeadingPath = currentHeadingPath;
        continue;
      }
      if (paragraph.length() > 0) {
        paragraph.append(' ');
      }
      paragraph.append(line.strip());
      pendingHeadingPath = currentHeadingPath;
    }
    flushParagraph(paragraph, pendingParagraphs, pendingHeadingPath);
    flushSection(pendingParagraphs, pendingHeadingPath, chunks, config);

    return reindex(chunks);
  }

  private static void flushParagraph(
      StringBuilder paragraph, List<String> pendingParagraphs, List<String> headingPath) {
    if (paragraph.length() == 0) {
      return;
    }
    pendingParagraphs.add(paragraph.toString());
    paragraph.setLength(0);
  }

  /**
   * Packs the paragraphs collected for one heading section into one or more chunks, splitting a
   * single oversized paragraph by word budget as a last resort.
   */
  private static void flushSection(
      List<String> paragraphs,
      List<String> headingPath,
      List<DocChunk> chunks,
      ChunkingConfig config) {
    if (paragraphs.isEmpty()) {
      return;
    }
    StringBuilder current = new StringBuilder();
    int currentTokens = 0;
    for (String p : paragraphs) {
      int paragraphTokens = wordCount(p);
      if (currentTokens > 0 && currentTokens + paragraphTokens > config.maxTokens()) {
        chunks.add(contextualize(current.toString(), headingPath));
        current.setLength(0);
        currentTokens = 0;
      }
      if (paragraphTokens > config.maxTokens()) {
        for (String piece : splitByWordBudget(p, config.maxTokens())) {
          chunks.add(contextualize(piece, headingPath));
        }
        continue;
      }
      if (current.length() > 0) {
        current.append("\n\n");
      }
      current.append(p);
      currentTokens += paragraphTokens;
    }
    if (current.length() > 0) {
      chunks.add(contextualize(current.toString(), headingPath));
    }
    paragraphs.clear();
  }

  private static List<String> splitByWordBudget(String text, int maxTokens) {
    String[] words = text.split("\\s+");
    List<String> pieces = new ArrayList<>();
    StringBuilder piece = new StringBuilder();
    int count = 0;
    for (String word : words) {
      if (count > 0 && count >= maxTokens) {
        pieces.add(piece.toString());
        piece.setLength(0);
        count = 0;
      }
      if (piece.length() > 0) {
        piece.append(' ');
      }
      piece.append(word);
      count++;
    }
    if (piece.length() > 0) {
      pieces.add(piece.toString());
    }
    return pieces;
  }

  private static int wordCount(String text) {
    String trimmed = text.strip();
    return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
  }

  private static DocChunk contextualize(String text, List<String> headingPath) {
    String contextualized =
        headingPath.isEmpty() ? text : String.join(" > ", headingPath) + "\n\n" + text;
    return new DocChunk(-1, headingPath, contextualized);
  }

  private static List<DocChunk> reindex(List<DocChunk> chunks) {
    List<DocChunk> result = new ArrayList<>(chunks.size());
    for (int i = 0; i < chunks.size(); i++) {
      DocChunk c = chunks.get(i);
      result.add(new DocChunk(i, c.headingPath(), c.text()));
    }
    return result;
  }
}
