# System Testing

## XAMPP database health verification — November 15, 2026

The live `metric_laundry` database on XAMPP MariaDB 10.4.32, port 3306, was checked
without changing application records. All five InnoDB tables returned `OK` from
`CHECK TABLE`; all six foreign-key constraints are present. Customer, staff and
admin sample credentials authenticated through the application's JDBC/password
code. Incorrect passwords were rejected. Existing records had no orphaned orders,
missing payment rows, missing initial history, inconsistent payment amounts or
weight/rate/total discrepancies. The live snapshot contained four users, three
services, one order, one payment and one status-history row.

`checks/DatabaseHealthChecks.java` completed 22 live read-only assertions and 35
assertions in a separately created database named
`metric_laundry_verify_20261115152524_4bbcb5b5`. The isolated checks exercised schema
creation and repeated startup, registration, duplicate email rejection, password
hashing, profile/password changes, customer order isolation, all three payment
methods, staff status changes and payment verification, retained historical rates,
staff restrictions on rates, foreign-key protection and transaction rollback when
the payment insertion fails. The temporary database was removed, and its absence
was confirmed. Live row counts were unchanged after verification.

Two additional boundary probes reproduced application issues; they are not server
or table-corruption failures, and no production code was changed during this check:

1. **Weight precision:** an order entered as 2.333 kg at 35.00 saves a total of 81.66
   but stores 2.33 kg, whose recalculated total is 81.55. Normalize/validate weight
   to the database precision before computing and persisting the charge.
2. **Disabled services:** `Session.placeOrder` accepts a service whose availability
   has been switched off. Revalidate availability when saving the order.

To rerun outside the sandbox, compile the check against the built application's
classpath, then run `laundry.DatabaseHealthChecks live` and
`laundry.DatabaseHealthChecks isolated` as separate JVM processes. The isolated
mode requires create/drop privileges and only drops its own newly named database.
The live mode performs reads and authentication only; it does not run schema setup.

## Shared green design verification — November 15, 2026

All 13 active forms now use the Home design. The duplicate blue `DashboardForm`
and its form metadata were removed. The old icon directory is archived under
`target/style-migration/original-icons`; application resources use the new green
and white drawings instead.

The 39 production Java sources and three verification classes compiled against
Java 17. All 12 restyled forms were constructed and rendered at 1280 x 760, in
addition to the Customer Dashboard's populated/empty/error checks. Screens were
visually inspected for clipping, contrast, control outlines and shared navigation.
Tracking was checked with an in-memory order fixture and now uses six stages.
Staff-only accounts still hide Manage Services and Reports. Database checks were
read-only; no customer, order, payment or service record was changed.

`checks/check_form_metadata.py` verifies field types, event handler definitions,
resource paths, shared dimensions and absence of the former blue palette in every
form. `checks/ScreenThemeChecks.java` renders the 12 existing screens using the live
database for reads and isolated in-memory user/order fixtures. Generated previews
are stored in `target/review`.

The previous Windows sandbox limitation on an ordinary Maven run still applies;
the compiler API verification used actual dependency classes. NetBeans Design-tab
opening and a manual interactive click-through remain checks to perform in the IDE.

## Customer Dashboard redesign verification — November 15, 2026

The module 1 redesign was verified separately from the historical scenarios below:

- All 38 production Java files and two standalone verification classes compiled
  against the Java 17 API.
- The new form XML's 52 component fields, 13 event bindings and six image references
  were checked against the Java source and packaged resource paths.
- Offline Swing checks passed for populated, empty and database-error states,
  background DAO reads, EDT callbacks, peso formatting, legacy status mapping,
  refresh recovery, disabled checkout after errors, and progress/chart validation.
- Populated and empty dashboards were rendered at 1280 x 760 and visually inspected.
  Populated preview records are test fixtures; they are never inserted into MySQL.
- The live DAO passed a read-only query against the existing database: three services
  and zero orders for customer 1. No schema or data was changed.

The normal Maven attempt was blocked by this Windows sandbox denying Java's
`Path.toRealPath` access to dependency paths. Compilation and the checks were
completed with the JDK compiler API and an isolated verification class loader, using
the actual cached FlatLaf and MySQL classes. This does not establish that a full
Maven build passed. Opening the new form in NetBeans Design and manually clicking
through the legacy navigation destinations remain IDE checks for sign-off.

See `SETUP-REDESIGN.md` and `tools/check-dashboard.ps1` for the ordinary local workflow.

## Historical system scenarios

This file covers the Chapter I specific objective "Test the main system functions and document any issues found during testing" and methodology step 5, System Testing.

The functions were tested with an automated scenario run on November 9, 2026, against a separate MySQL test database (`metric_laundry_test`, XAMPP MariaDB 10.4). The test database is created fresh for each run and deleted afterwards. All 36 test cases passed.

| ID | Function | Test case | Expected result | Result |
|---|---|---|---|---|
| T00 | Database setup | Start the app with no database | `metric_laundry` database and its 5 tables are created | Pass |
| T01 | Login | Wrong password | Login refused | Pass |
| T02 | Login | Customer email (any letter case) and correct password | Logged in as customer | Pass |
| T03 | Register | Email and phone formats (`juan@`, `9123456789`) | Invalid formats rejected; `09XXXXXXXXX` accepted | Pass |
| T04 | Register | Email that already has an account | Duplicate detected | Pass |
| T05 | Service selection | Service list | Wash and Fold, Dry Cleaning, Ironing | Pass |
| T06 | Price calculation | 5 kg Wash and Fold at ₱25.00/kg | ₱125.00 | Pass |
| T07 | Price calculation | 2.333 kg × ₱25.00 | Rounded to ₱58.33 | Pass |
| T08 | Weight entry | Text such as `abc` or `NaN` | Rejected | Pass |
| T09 | Order summary | Expected completion | Order date + service turnaround | Pass |
| T10 | Order confirmation | Place a Cash order | Status Received, payment Unpaid, amount due ₱125.00 | Pass |
| T11 | Claim stub | Unpaid order document | "Claim Stub", not a receipt | Pass |
| T12 | Payment-method recording | GCash order with reference no. | Pending Verification, reference stored | Pass |
| T13 | Staff login | Staff account | Logged in with staff role | Pass |
| T14 | Status update | Staff set the status to Washing | Received done, Washing in progress, Drying pending | Pass |
| T15 | Payment verification | Staff mark the order Paid | Amount due ₱0.00, payment time recorded, "Payment Receipt" | Pass |
| T16 | Order tracking | Status Ready for Pickup | All four tracking steps complete | Pass |
| T17 | Rates | Change the Wash and Fold rate to ₱30 | Existing order stays ₱125.00 at ₱25/kg | Pass |
| T18 | Rates | Mark Ironing as not offered | Service unavailable to customers | Pass |
| T19 | Record keeping | Read records back from MySQL | Orders, status, payment, note and rates kept | Pass |
| T20 | Record retrieval | Open Manage Orders | All orders listed | Pass |
| T21 | Record retrieval | Search "001" | Only order #001 shown | Pass |
| T22 | Sales summary | All Time | Collected ₱125.00, unpaid / to verify ₱120.00 | Pass |
| T23 | Order history | Customer opens My Orders | Only their orders, newest first | Pass |
| T24 | Order tracking | Customer views the updated order | Shows Ready for Pickup and the staff note | Pass |
| T25 | Staff dashboard | Open the dashboard after the orders above | 2 orders today, 1 payment to verify, 1 ready, sales today ₱125.00 | Pass |
| T26 | Staff dashboard | Needs Attention list | GCash order ("Verify payment") listed before the order awaiting pickup | Pass |
| T27 | Staff dashboard | Open an order from the dashboard | That order is selected in Manage Orders | Pass |
| T28 | Dashboard graphs | Sales – Last 7 Days and Active Orders by Status | Today's bar ₱125.00; 1 Received and 1 Ready for Pickup | Pass |
| T29 | Sales Summary graph | Sales Collected by Service | Wash and Fold ₱125.00; Dry Cleaning and Ironing ₱0.00 | Pass |
| T30 | Security | Register a new customer | Password stored as a PBKDF2 hash, not plain text; login works | Pass |
| T31 | Status and payment records | Staff updates order #1 (Washing, then Ready and Paid) | 4 status-history rows (3 recorded by staff); payment verified by staff | Pass |
| T32 | Data integrity | Check the table relationships | 6 foreign keys link orders, payments, history, users and services | Pass |
| T33 | Admin role | Staff try to change a service rate | Refused; rate unchanged | Pass |
| T34 | Admin role | Admin logs in | Admin role with staff panel access | Pass |
| T35 | Admin role | Staff open the staff panel | "STAFF PANEL"; Services & Rates and Sales Summary hidden (admin sees "ADMIN PANEL" and all items) | Pass |

## Issues found during testing (fixed)

| Issue | Fix |
|---|---|
| The order-count text on the Profile screen was cut off. | Shown as a badge instead. |
| The Manage Orders table columns were too narrow to read. | Removed the Phone column. Phone search still works, and the phone appears in the order details. |
| The Services & Rates notice text was cut off. | Shortened the notice. |
| Needs Attention was sorted by due date only, so "Awaiting pickup" appeared above "Verify payment" (found by T26). | Sorted by urgency first, then by due date. |
| The date on the staff dashboard was cut off at the right edge. | Added right padding. |
| A gridline showed through the bottom of the graph bars. | Bars are drawn with flat bottoms on the baseline. |

## Manual checks to perform in NetBeans

The tests above cover the system's logic and screen contents. Before submission, also test these by hand:

1. Open each `.form` in the **Design** tab and confirm it loads.
2. Click through the full flow: customer login → place an order with GCash → log out → staff login → check the order appears under Needs Attention on the dashboard → verify payment → set Ready for Pickup → log in as the customer → **Refresh** under My Orders.
3. Close and reopen the app, and confirm that orders are still there. Also check the tables in phpMyAdmin (`metric_laundry`).
4. Stop MySQL in XAMPP and start the app. It should show a message asking you to start MySQL.
