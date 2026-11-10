-- NfA-4: process-scoped tables carry the owning tenant; every port call filters by it.
-- Existing rows are transient (TTL) and stem from the single-tenant operation, hence the backfill.
ALTER TABLE process_step ADD COLUMN tenant_id VARCHAR(128) NOT NULL DEFAULT 'default-tenant';
ALTER TABLE process_step ALTER COLUMN tenant_id DROP DEFAULT;

-- Retention purger: newest step per process.
CREATE INDEX idx_process_step_process_created ON process_step (process_id, created_at);

ALTER TABLE retrieval_result ADD COLUMN tenant_id VARCHAR(128) NOT NULL DEFAULT 'default-tenant';
ALTER TABLE retrieval_result ALTER COLUMN tenant_id DROP DEFAULT;

ALTER TABLE document_type_candidate
    ADD COLUMN tenant_id VARCHAR(128) NOT NULL DEFAULT 'default-tenant';
ALTER TABLE document_type_candidate ALTER COLUMN tenant_id DROP DEFAULT;

ALTER TABLE extraction_result ADD COLUMN tenant_id VARCHAR(128) NOT NULL DEFAULT 'default-tenant';
ALTER TABLE extraction_result ALTER COLUMN tenant_id DROP DEFAULT;

-- One row per tenant and process: process ids of different tenants never clash.
ALTER TABLE document_type_candidate DROP CONSTRAINT document_type_candidate_pkey;
ALTER TABLE document_type_candidate ADD PRIMARY KEY (tenant_id, process_id);

ALTER TABLE extraction_result DROP CONSTRAINT extraction_result_pkey;
ALTER TABLE extraction_result ADD PRIMARY KEY (tenant_id, process_id);

-- Saga state: tenant_id is NOT NULL since V14, only the key changes.
ALTER TABLE process_finalization DROP CONSTRAINT process_finalization_pkey;
ALTER TABLE process_finalization ADD PRIMARY KEY (tenant_id, process_id);
