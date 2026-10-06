-- V7's backfill used the stale 'pdfbox-fast-track' label from the old chunking-based structuring
-- module; current code (StagePendingEmbeddingsService) writes 'pdfbox', matching
-- docextract.adapters.text-extraction. Realign old rows so document_hash/extraction_source lookups
-- (V10) can still match them.
UPDATE embedding
SET extraction_source = 'pdfbox'
WHERE extraction_source = 'pdfbox-fast-track';
