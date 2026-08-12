# Oyuki Market Square implementation

Added MARKET_AGENT and future MARKET_SUPERVISOR roles, database-driven states/LGAs/markets, market seller mapping, procurement requests/items, purchase evidence, quality fields, handover records, market price updates with approval status, configurable commission fields, Market Directory frontend and Market Agent dashboard.

## Core APIs
- GET /api/market-directory/states|lgas|markets
- POST /api/admin/markets/states|lgas and POST /api/admin/markets
- GET /api/market-agent/procurements
- PATCH /api/market-agent/procurements/{id}/accept
- PATCH /api/market-agent/procurements/{id}/ready
- POST /api/market-agent/prices

Hibernate ddl-auto=update can create the new tables. Production should later migrate these with Flyway/Liquibase.
