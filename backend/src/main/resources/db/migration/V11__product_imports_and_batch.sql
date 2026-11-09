-- Spring Batch JDBC metadata, compatible with Spring Batch 6 PostgreSQL schema.
CREATE TABLE public.batch_job_instance (job_instance_id BIGINT PRIMARY KEY, version BIGINT, job_name VARCHAR(100) NOT NULL, job_key VARCHAR(32) NOT NULL, UNIQUE(job_name,job_key));
CREATE TABLE public.batch_job_execution (job_execution_id BIGINT PRIMARY KEY, version BIGINT, job_instance_id BIGINT NOT NULL REFERENCES public.batch_job_instance, create_time TIMESTAMP NOT NULL, start_time TIMESTAMP, end_time TIMESTAMP, status VARCHAR(10), exit_code VARCHAR(2500), exit_message VARCHAR(2500), last_updated TIMESTAMP);
CREATE TABLE public.batch_job_execution_params (job_execution_id BIGINT NOT NULL REFERENCES public.batch_job_execution, parameter_name VARCHAR(100) NOT NULL, parameter_type VARCHAR(100) NOT NULL, parameter_value VARCHAR(2500), identifying CHAR(1) NOT NULL);
CREATE TABLE public.batch_step_execution (step_execution_id BIGINT PRIMARY KEY, version BIGINT NOT NULL, step_name VARCHAR(100) NOT NULL, job_execution_id BIGINT NOT NULL REFERENCES public.batch_job_execution, create_time TIMESTAMP NOT NULL, start_time TIMESTAMP, end_time TIMESTAMP, status VARCHAR(10), commit_count BIGINT, read_count BIGINT, filter_count BIGINT, write_count BIGINT, read_skip_count BIGINT, write_skip_count BIGINT, process_skip_count BIGINT, rollback_count BIGINT, exit_code VARCHAR(2500), exit_message VARCHAR(2500), last_updated TIMESTAMP);
CREATE TABLE public.batch_step_execution_context (step_execution_id BIGINT PRIMARY KEY REFERENCES public.batch_step_execution, short_context VARCHAR(2500) NOT NULL, serialized_context TEXT);
CREATE TABLE public.batch_job_execution_context (job_execution_id BIGINT PRIMARY KEY REFERENCES public.batch_job_execution, short_context VARCHAR(2500) NOT NULL, serialized_context TEXT);
CREATE SEQUENCE public.batch_step_execution_seq;
CREATE SEQUENCE public.batch_job_execution_seq;
CREATE SEQUENCE public.batch_job_instance_seq;

CREATE TABLE public.product_imports (
 id UUID PRIMARY KEY, tenant VARCHAR(255) NOT NULL, user_id UUID NOT NULL,
 status VARCHAR(32) NOT NULL, total INTEGER NOT NULL, processed INTEGER NOT NULL DEFAULT 0,
 succeeded INTEGER NOT NULL DEFAULT 0, skipped INTEGER NOT NULL DEFAULT 0, failed INTEGER NOT NULL DEFAULT 0,
 message VARCHAR(255), version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX product_import_one_active ON public.product_imports(tenant) WHERE status IN ('QUEUED','RUNNING');
CREATE INDEX product_import_owner ON public.product_imports(tenant,user_id,created_at DESC);
CREATE TABLE public.product_import_rows (
 import_id UUID NOT NULL REFERENCES public.product_imports ON DELETE CASCADE,
 row_number INTEGER NOT NULL, status VARCHAR(16) NOT NULL, message VARCHAR(255),
 PRIMARY KEY(import_id,row_number)
);
