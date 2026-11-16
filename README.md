# Metric Laundry: Digital Laundry Ordering System

A Java Swing system that follows the paper prototype and the project documentation (Chapters I–II). Every screen is a NetBeans GUI Builder `JFrame` (`.java` + `.form` pair) that you can edit in the **Design** tab. Records are stored in a **MySQL** database (XAMPP). Requires JDK 17 or later.

The redesign adds a green-and-white customer dashboard, FlatLaf Light,
reusable Swing components, and a DAO/controller boundary for dashboard reads.
Customer login opens `view.customer.CustomerDashboard`. See
[SETUP-REDESIGN.md](SETUP-REDESIGN.md) for setup, Palette registration, validation,
and the staged migration boundaries. All 13 active JFrame forms now share Home's
green visual design, including login, registration, ordering, tracking, payment,
profile, and the staff/admin screens. Existing workflows remain connected.

## Before running: start MySQL

1. Open the **XAMPP Control Panel** and click **Start** next to **MySQL**.
2. Run the app. On first run it creates the `metric_laundry` database, its tables and the sample accounts automatically. You can also import `src/laundry/database.sql` in phpMyAdmin.

If MySQL isn't running, the app shows a message asking you to start it. The connection settings are in `src/laundry/Database.java`: `localhost:3306`, user `root`, no password. See [DATABASE.md](DATABASE.md) for the ERD, the data dictionary and useful queries.

## Screens

Customer screens follow the prototype, numbered as in the sketches:

| # | Screen | Form |
|---|---|---|
| 1 | Login (customers and staff) | `LoginForm` (start here) |
| – | Register | `RegisterForm` |
| 2 | Dashboard (redesigned) | `view.customer.CustomerDashboard` |
| 3 | Select Laundry Service | `SelectServiceForm` |
| 4 | Enter Weight and View Price | `WeightForm` |
| 5 | Order Summary / Confirmation | `OrderSummaryForm` |
| 6 | Payment | `PaymentForm` |
| 7 | Order Tracking / My Orders | `MyOrdersForm` |
| – | Profile | `ProfileForm` |

Staff and admin screens (Services & Rates and Sales Summary are **admin only**):

| Screen | Form |
|---|---|
| Staff Dashboard (staff home after login): today's orders and sales, payments to verify, orders ready for pickup, a **Sales – Last 7 Days** bar graph, an **Active Orders by Status** graph, and a **Needs Attention** list (overdue, payment to verify, due today, new, awaiting pickup). Double-click an order to open it in Manage Orders. | `StaffDashboardForm` |
| Manage Orders: search records, update status, verify payments, add notes | `StaffOrdersForm` |
| Services & Rates: the service and pricing list | `StaffServicesForm` |
| Sales Summary: orders and sales per period, with graphs of sales collected by service and orders by status | `StaffReportForm` |

Supporting classes: `BarChart` (the graph component used in the staff forms), `Database` (MySQL connection and setup), `Session` (reads and writes the records), `Passwords` (password hashing), `Order`, `User`, `Service`, `Nav` (switching screens) and `Ui` (formatting, claim stub/receipt). Brand artwork and the new green icons are in `src/resources/images` and `src/resources/images/ui`.

## How the system meets the Statement of the Problem

| Problem (Chapter I-b / II-h) | How the system addresses it |
|---|---|
| 1. Manual recording errors | Customer and order details are captured by forms with validation: required fields, email, 11-digit `09` phone, weight from 0 to 50 kg, rates and turnaround. |
| 2. Difficulty retrieving records | **Manage Orders** searches by order no., customer name or phone and filters by order or payment status. Customers see their own history under **My Orders**. |
| 3. Inconsistent price calculation | Charge = weight × the current rate from the **Services & Rates** list, rounded to centavos. Each order stores the rate it was charged, so later rate changes don't alter it. |
| 4. Delayed order updates | Staff record the status (Received, Washing, Drying, Ready for Pickup, Claimed) with the time of each update. Customers see it on the tracking screen; **Refresh** reloads the latest records. |
| 5. Lost or damaged records | All records are kept in the MySQL database `metric_laundry`: users, services, orders, payments and order status history. Orders and their payments are saved together in one transaction. |
| 6. Payment record confusion | Each order records the payment method, amount due, payment status (Unpaid / Pending Verification / Paid), GCash or bank reference no., and payment time. GCash and Bank Transfer stay *Pending Verification* until staff confirm them. An order can't be marked Claimed until it is Paid. |
| 7. Time-consuming order management | Staff update the status, payment and note of an order from one screen. Totals and expected completion dates are computed automatically. |
| 8. Limited order history and reporting | **Sales Summary** shows total orders and kilograms, sales collected, the unpaid amount, a breakdown by service and by status, for Today, the Last 7 Days, This Month or All Time. |

**Inputs (II-c):** customer name and contact number; service; weight; rate; order date and expected completion date; payment method, amount due and payment status; order status and staff notes.

**Outputs (II-f):** an order confirmation / **claim stub** while unpaid, and a **payment receipt** once staff mark the order Paid. Also price information, status information, stored records and sales summaries.

## Sample accounts

| Role | Email | Password |
|---|---|---|
| Customer | `juan.dcruz@email.com` | `password123` |
| Staff | `staff@metriclaundry.com` | `staff123` |
| Admin (owner) | `admin@metriclaundry.com` | `admin123` |

**Roles:** *Staff* see the Dashboard and Manage Orders, where they update statuses and verify payments. *Admin* also gets **Services & Rates** and **Sales Summary**. The sidebar shows "ADMIN PANEL" or "STAFF PANEL", and admin-only screens and rate changes are blocked for staff. Change the admin password before real use. Staff and admin accounts are added in the database, not through Register.

New customers can sign up with **Register here**.

Starting rates (staff can change them): Wash and Fold ₱25/kg with 1-day turnaround, Dry Cleaning ₱60/kg with 3-day turnaround, Ironing ₱30/kg with 1-day turnaround. These are sample values. Set the actual rates from Metric Laundry's price list. For example, 5 kg of Wash and Fold costs ₱125.00.

## Limitations (as stated in Chapter I-e)

- No automatic online payment. A GCash or bank payment is only a recorded reference no. until staff verify it.
- No delivery scheduling, GPS tracking, SMS notifications, inventory or multi-branch management.
- Tracking is only as current as staff updates. Charges depend on correct weights and rates. There are no extra fees, discounts or minimum charges.
- All services are charged per kilogram.
- The database runs on the local XAMPP MySQL server. To use the app from several computers, point `Database.HOST` at one shared MySQL server.

## Open in NetBeans

1. **File > Open Project** and select this `laundry` folder.
2. Open any form under **Source Packages > laundry** and click **Design**.
3. Run the project (F6). It starts at `LoginForm`.

The graphs are a custom component, `laundry.BarChart`. Build the project once (F11) so the Design tab can draw them; until then NetBeans shows a placeholder. You can set a graph's `title`, `subtitle` and `horizontal` in the Properties window.

Screens use **Null Layout**, so you can drag components anywhere in the designer. Names, prices and other record values are filled in by each form's constructor; the designer shows sample text. Customer screens run on their own with Shift+F6 as the sample customer. Staff screens run on their own as the admin account.

To start over with the sample records, run `DROP DATABASE metric_laundry;` in phpMyAdmin, then start the app again.

## Run from a terminal

```text
mvn compile exec:java
```

Maven downloads the MySQL driver (`mysql-connector-j`) automatically. NetBeans does the same when you build the project.

See [TESTING.md](TESTING.md) for the test cases and results.
