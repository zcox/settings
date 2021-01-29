# Overview
- Settings
    - Various types of settings that control the behavior of the product
    - Some settings look like a tuple; others look like a list
- Values of settings
    - Each setting has a default value
    - The product is multi-tenant, each tenant has different settings values
- Changes to settings
    - Retain history of all changes to settings
    - Record context of each change: who, when, where, why, etc

# Goals/Requirements
- Log of all changes
- Efficient queries for current values
- Read-after-write consistency

## Example
- Postgres tables
  - Postgres is the system-of-record
  - Service receives commands from clients, validates them, records events, uses events to determine state changes
  - This is essentially the Outbox Pattern
  - Events table
    - This is the full event log
    - Each row is one event, with context
  - State tables
    - One table per unit of state
    - State is some projection of events
    - Could be completely recreated from all events
    - Could be read during command validation, or to answer client queries
  - Events and state tables are written in a single transaction
    - State tables have read-after-write consistency with event log
- Kafka topics
  - Debezium produces a changelog topic for each table
  - Events topic
    - Contains one record per event
    - Can be used for any event-sourcing purpose, e.g. update some materialized view (with eventual consistency)
  - State topics
    - One topic per state table
    - One record per insert/update/delete to state table
    - May be more useful to process state change topic, depending on use case
    - Could enable compaction, and use to populate some other view

```
#run all components:
docker-compose up --build -d

#verify everything is running using:
docker-compose ps

#after kafka connect starts (check its logs using: `docker-compose logs -f connect`), run the postgres source connectors:
./run-basic-connector.sh

#use psql to insert example rows, see below section

#view contents of topics (run in confluent download dir)
bin/kafka-console-consumer --bootstrap-server localhost:9092 --property print.key=true --formatter io.confluent.kafka.formatter.AvroMessageFormatter --property schema.registry.url=http://localhost:8081 --topic postgres0.public.settings_events --from-beginning

bin/kafka-console-consumer --bootstrap-server localhost:9092 --property print.key=true --formatter io.confluent.kafka.formatter.AvroMessageFormatter --property schema.registry.url=http://localhost:8081 --topic postgres0.public.display_settings --from-beginning
```

## Postgres

Connect to postgres0 using: `PGPASSWORD=postgres psql -h localhost -p 5432 -U postgres -d postgres`

```
BEGIN;

INSERT INTO settings_events VALUES ('546566cd-50a6-4f4d-b1c9-526f3dbbde92', now(), '{"DisplaySettingsUpdated":{"eventId":"546566cd-50a6-4f4d-b1c9-526f3dbbde92","timestamp":"2021-01-28T23:11:43.615678Z","userId":"f4ccfbdd-05da-4730-90ea-506978eb5287","ip":"97.125.16.244","tenantId":"7701a599-760f-4fc3-be32-513efb4b4b95","name":"Dog Waterproofing","description":"We waterproof dogs!"}}'::jsonb);

INSERT INTO display_settings VALUES ('7701a599-760f-4fc3-be32-513efb4b4b95', 'Dog Waterproofing', 'We waterproof dogs!');

COMMIT;
```

## Topic Records

`settings_events`:

```json
{
  "before": null,
  "after": {
    "postgres0.public.settings_events.Value": {
      "event_id": "546566cd-50a6-4f4d-b1c9-526f3dbbde92",
      "created": "2021-01-28T23:16:31.379404Z",
      "event": "{\"DisplaySettingsUpdated\": {\"ip\": \"97.125.16.244\", \"name\": \"Dog Waterproofing\", \"userId\": \"f4ccfbdd-05da-4730-90ea-506978eb5287\", \"eventId\": \"546566cd-50a6-4f4d-b1c9-526f3dbbde92\", \"tenantId\": \"7701a599-760f-4fc3-be32-513efb4b4b95\", \"timestamp\": \"2021-01-28T23:11:43.615678Z\", \"description\": \"We waterproof dogs!\"}}"
    }
  },
  "source": {
    "version": "1.2.0.Final",
    "connector": "postgresql",
    "name": "postgres0",
    "ts_ms": 1611875818237,
    "snapshot": {
      "string": "false"
    },
    "db": "postgres",
    "schema": "public",
    "table": "settings_events",
    "txId": {
      "long": 492
    },
    "lsn": {
      "long": 24626728
    },
    "xmin": null
  },
  "op": "c",
  "ts_ms": {
    "long": 1611875818599
  },
  "transaction": null
}
```

`display_settings`:

```json
{
  "before": null,
  "after": {
    "postgres0.public.display_settings.Value": {
      "tenant_id": "7701a599-760f-4fc3-be32-513efb4b4b95",
      "name": "Dog Waterproofing",
      "description": {
        "string": "We waterproof dogs!"
      }
    }
  },
  "source": {
    "version": "1.2.0.Final",
    "connector": "postgresql",
    "name": "postgres0",
    "ts_ms": 1611875818237,
    "snapshot": {
      "string": "false"
    },
    "db": "postgres",
    "schema": "public",
    "table": "display_settings",
    "txId": {
      "long": 492
    },
    "lsn": {
      "long": 24627368
    },
    "xmin": null
  },
  "op": "c",
  "ts_ms": {
    "long": 1611875818624
  },
  "transaction": null
}
```
