OYUKI ADMIN SUBDOMAIN PACKAGE

Upload all files and folders in this ZIP into the document root for:
https://admin.oyukimarketplace.com

Recommended document root:
public_html/admin

Required Spring Boot CORS origin:
https://admin.oyukimarketplace.com

The package connects to:
https://illustrious-nurturing-production-8169.up.railway.app/api

Expected backend endpoints:
POST /api/auth/login
GET /api/admin/users
GET /api/admin/users/statistics
PATCH /api/admin/users/{id}/status
GET /api/admin/applications/pending
GET /api/admin/applications/{userId}
PATCH /api/admin/applications/{userId}/approve
PATCH /api/admin/applications/{userId}/reject
GET /api/admin/orders
GET /api/admin/orders/{id}
GET /api/admin/payments
PATCH /api/admin/payments/{id}/confirm
PATCH /api/admin/payments/{id}/reject
GET /api/admin/payments/{id}/receipt
