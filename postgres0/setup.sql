CREATE TABLE settings_events(
    event_id uuid PRIMARY KEY,
    created timestamp with time zone NOT NULL,
    event jsonb NOT NULL
);

CREATE TABLE display_settings(
    tenant_id uuid PRIMARY KEY,
    name text NOT NULL,
    description text
);

-- CREATE TABLE list_settings(
--     tenant_id uuid NOT NULL,
--     item_id uuid NOT NULL,
--     name text NOT NULL
-- );
