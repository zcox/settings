/*

PUT /tenants/{tenantId}/settings/display
- headers:
  - auth (for simplicity, assume something else validates/provides these)
    - tenantId
    - userId
  - IP
- body:
  - name: String
  - description: Option[String]
- create a UpdateDisplaySettings command
- handle the command:
  - failure: 
    - error in http response
  - success: 
    - create/update the DisplaySettings using event
      - look up DisplaySettings in DB
        - not exists: create
        - exists: update
    - write event and DisplaySettings to db in tx
      - event to settings_events table
      - DisplaySettings to display_settings table

GET /tenants/{tenantId}/settings/display
- SELECT * FROM display_settings WHERE tenant_id = ? LIMIT 1
- convert to DisplaySettings
- return ^^ in http response, or 404 if not exists

*/