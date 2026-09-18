package ch.adeon.apps.docextract.structuring.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class HeadingAwareChunkerTest {

  @Test
  void contextualizes_chunks_with_the_heading_path_they_were_found_under() {
    StructuredContent content =
        new StructuredContent(
            """
            # Invoice

            Some intro paragraph.

            ## Line items

            Item 1 costs 10 CHF.
            """);

    List<DocChunk> chunks = HeadingAwareChunker.chunk(content, new ChunkingConfig(512));

    assertThat(chunks).hasSize(2);
    assertThat(chunks.get(0).headingPath()).containsExactly("Invoice");
    assertThat(chunks.get(0).text()).startsWith("Invoice\n\n").contains("Some intro paragraph.");
    assertThat(chunks.get(1).headingPath()).containsExactly("Invoice", "Line items");
    assertThat(chunks.get(1).text())
        .startsWith("Invoice > Line items\n\n")
        .contains("Item 1 costs 10 CHF.");
  }

  @Test
  void indexes_chunks_in_order() {
    StructuredContent content = new StructuredContent("First paragraph.\n\nSecond paragraph.");

    List<DocChunk> chunks = HeadingAwareChunker.chunk(content, new ChunkingConfig(2));

    assertThat(chunks).extracting(DocChunk::index).containsExactly(0, 1);
  }

  @Test
  void splits_an_oversized_paragraph_by_word_budget() {
    String longParagraph = "word ".repeat(20).strip();
    StructuredContent content = new StructuredContent(longParagraph);

    List<DocChunk> chunks = HeadingAwareChunker.chunk(content, new ChunkingConfig(5));

    assertThat(chunks).hasSize(4);
    assertThat(chunks).allMatch(chunk -> chunk.headingPath().isEmpty());
  }

  @Test
  void flat_text_without_headings_yields_un_contextualized_chunks() {
    StructuredContent content = new StructuredContent("Just plain text, no headings at all.");

    List<DocChunk> chunks = HeadingAwareChunker.chunk(content, new ChunkingConfig(512));

    assertThat(chunks).hasSize(1);
    assertThat(chunks.get(0).headingPath()).isEmpty();
    assertThat(chunks.get(0).text()).isEqualTo("Just plain text, no headings at all.");
  }
}
