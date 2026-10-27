-- public.event_publication definition

-- Drop table

-- DROP TABLE public.event_publication;

CREATE TABLE public.event_publication
(
    id                     uuid         NOT NULL,
    completion_attempts    int4         NOT NULL,
    completion_date        timestamptz(6) NULL,
    event_type             varchar(255) NOT NULL,
    last_resubmission_date timestamptz(6) NULL,
    listener_id            varchar(255) NOT NULL,
    publication_date       timestamptz(6) NOT NULL,
    serialized_event       varchar(255) NOT NULL,
    status                 varchar(255) NULL,
    CONSTRAINT event_publication_pkey PRIMARY KEY (id),
    CONSTRAINT event_publication_status_check CHECK (((status)::text = ANY ((ARRAY['PUBLISHED':: character varying, 'PROCESSING':: character varying, 'COMPLETED':: character varying, 'FAILED':: character varying, 'RESUBMITTED':: character varying])::text[])
) )
);