# Oyuki Market Square - Location Detection

This build adds browser location permission and LGA-based market discovery.

## Flow
1. User opens `home.html` or `markets.html` over HTTPS.
2. Browser asks for geolocation permission.
3. Frontend sends latitude/longitude to `GET /api/market-directory/nearby?lat=...&lng=...`.
4. Backend reverse-geocodes the coordinates and matches the result to an active Oyuki LGA.
5. Every active market stored in that LGA is returned.
6. If permission is denied or the LGA cannot be resolved, the user can select State -> LGA manually.

## Public endpoints
- `GET /api/market-directory/states`
- `GET /api/market-directory/lgas?stateId=...`
- `GET /api/market-directory/markets?lgaId=...`
- `GET /api/market-directory/nearby?lat=...&lng=...`

## Database
The application automatically seeds Lagos State and its 20 LGAs. Markets themselves remain database-driven and are added through the Admin Market API. The location feature returns all ACTIVE markets registered under the detected LGA.

For best results, every market should have latitude and longitude as well as its LGA. Those coordinates are used as a fallback when reverse geocoding is unavailable and also allow the UI to show approximate distance.

## Railway variables (optional)
- `OYUKI_GEOCODING_ENABLED=true`
- `OYUKI_GEOCODING_REVERSE_URL=https://nominatim.openstreetmap.org/reverse`
- `OYUKI_GEOCODING_USER_AGENT=OyukiMarketplace/1.0 (support@oyukimarketplace.com)`

The public website must use HTTPS for browser geolocation on production domains.

## OpenStreetMap public service note
The default reverse-geocoding URL points to the public Nominatim service. The backend identifies itself with a custom User-Agent, caches nearby coordinate lookups, and throttles requests. For substantial production traffic, configure a commercial/self-hosted geocoder instead of relying on the public service.
