# Crave — Food Ordering & Delivery Backend

Crave is a RESTful backend for a food ordering and delivery platform. It supports customers, restaurant partners, and delivery partners, with order management, online payments, live delivery updates, and machine-learning-powered restaurant recommendations.

## Features

### Authentication & Accounts
- Customer, restaurant, and delivery-partner registration and login.
- Email OTP verification for new accounts, with OTP resend support.
- Google and Facebook sign-in through Supabase Auth.
- Password recovery, password updates, session refresh, and logout.
- HTTP-only cookie-based sessions, with bearer-token support for authenticated API requests.
- Role-aware account access and profile management.
- Account deletion for the signed-in user.

### Restaurant Discovery & Menus
- Browse approved restaurants and retrieve restaurant details.
- Search restaurants and filter by open status.
- Browse available menu items by restaurant.
- Search menu items and filter by food category.
- Restaurant profile details including cuisine, address, opening status, and image URL.
- Restaurant partners can manage their restaurant profile and food-item listings, including availability.

### Ordering
- Place orders with multiple food items and quantities.
- Retrieve an order history or details for an individual order.
- Track order status and view preparation-time information.
- Store order totals, item prices, quantities, delivery address, and customer notes.
- Discover available delivery orders and accept delivery assignments.
- Delivery partners can view assigned orders and update delivery status.

### Payments
- Cash on Delivery and Razorpay payment options.
- Create Razorpay payment orders.
- Verify payment signatures through the backend.
- Razorpay webhook handling for payment events.

### Machine-Learning Integration
- Integrated REST client for a separately hosted ML API.
- Restaurant recommendations with optional cuisine, location, maximum-price, minimum-rating, and result-limit filters.
- ML service health endpoint exposed through the backend.
- A customer-focused recommendation flow that can use cuisines inferred from previous orders.

### Delivery Tracking & Notifications
- Order tracking response includes delivery-partner assignment and available location coordinates.
- WebSocket support for real-time delivery-partner notifications.
- Authenticated WebSocket connections for active delivery-partner accounts.

### Customer Profile & Addresses
- Retrieve the signed-in customer's profile.
- Create, list, retrieve, update, delete, and set a default delivery address.
- Retrieve favourite restaurants based on previous ordering activity.

### API Documentation
- OpenAPI documentation and Swagger UI are available through Springdoc.

## Technology Stack

- **Language:** Java 25
- **Framework:** Spring Boot
- **API:** Spring MVC REST endpoints
- **Persistence:** Spring Data JPA and PostgreSQL
- **Authentication:** Supabase Auth, Spring Security, JWT
- **Payments:** Razorpay Java SDK
- **Real-time communication:** Spring WebSocket with STOMP
- **API documentation:** Springdoc OpenAPI and Swagger UI
- **Build:** Maven

## Architecture

The backend is organized into controllers, services, repositories, models, DTOs, configuration, and ML integration components. REST controllers expose application functionality, service classes coordinate business logic, repositories handle persistence, and the ML client communicates with the external recommendation service.

## API Areas

| Area | Base path |
|---|---|
| Authentication | `/api/auth` |
| Account management | `/api/account` |
| Customer profile | `/api/customer` |
| Restaurant discovery and menus | `/api/customer/restaurants` |
| Customer orders and tracking | `/api/customer/orders` |
| Customer delivery addresses | `/api/customer/addresses` |
| Restaurant menu management | `/api/restaurant/me/food-items` |
| Delivery partner orders | `/api/delivery/me/orders` |
| ML recommendations and health | `/api/customer/ml` |
| WebSocket endpoint | `/ws` |

## Integrations

- **Supabase:** Authentication and PostgreSQL-backed application data.
- **Razorpay:** Online payment order creation and payment verification.
- **ML recommendation service:** Restaurant recommendation and health APIs.
- **WebSocket/STOMP:** Delivery-partner notification channel.

## API Documentation

The backend exposes OpenAPI documentation and Swagger UI through its Springdoc configuration.
