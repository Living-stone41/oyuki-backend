OYUKI HTML + SPRING BOOT INTEGRATION

CONNECTED HTML PAGES
- login.html
- register.html + OTP verification
- forgot-password.html
- index.html
- shop.html
- meals.html
- kitchens.html
- kitchen-detail.html
- product.html
- cart.html
- checkout.html
- customer.html
- tracking.html

BACKEND PATCHES REQUIRED
1. Copy backend-patches/SecurityConfig.java to:
   src/main/java/com/oyuki/security/SecurityConfig.java
   This permits GET /api/marketplace/products/** and GET /uploads/**.

2. Copy backend-patches/CorsConfig.java to:
   src/main/java/com/oyuki/config/CorsConfig.java
   This allows VS Code Live Server on any localhost port.

3. Copy backend-patches/OrderResponse.java to:
   src/main/java/com/oyuki/order/dto/OrderResponse.java
   This exposes couponCode and discountAmount to the frontend.

RUN
1. Start Spring Boot on http://localhost:8080
2. Open this frontend folder with VS Code.
3. Start Live Server. The backend allows localhost/127.0.0.1 ports.
4. Do not double-click index.html because fetch/CORS behaviour is more reliable through Live Server.

NOT YET MIGRATED
- seller.html
- kitchen.html
- admin.html
Their old demo scripts remain for now. The customer storefront no longer uses localStorage data.
