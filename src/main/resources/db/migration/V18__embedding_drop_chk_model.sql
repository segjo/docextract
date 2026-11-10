-- Drops V7's chk_model CHECK constraint, which pinned embedding.embedding_model to a single
-- hard-coded value and thereby blocked ReindexService/ADR-012's multi-model re-indexing scenario
-- (an APPROVED row of a superseded model is flagged STALE, not deleted, so a second model's rows
-- must coexist). V7 itself is left unchanged (immutable migration history); this only removes the
-- now-contradictory constraint going forward.
--
-- OFFEN (reported, not fixed — outside this migration's narrow scope): the embedding column is
-- vector(1024), a fixed dimension pinned in V3 to the Qwen3-Embedding-0.6B model's output size. A
-- second embedding_model with a different vector dimension cannot be inserted into this column at
-- all (pgvector enforces the dimension at the type level) — dropping chk_model alone does not make
-- multi-model re-indexing with a different dimension possible; a dimension change still needs its
-- own migration (as V3's comment already says). embedding.status has no CHECK constraint (plain
-- VARCHAR(16)), so STALE (ReindexService#reindex) was already a structurally allowed value before
-- this migration and remains so.
ALTER TABLE embedding
    DROP CONSTRAINT chk_model;
