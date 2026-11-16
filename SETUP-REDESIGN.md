# Metric Laundry: shared green design

The shared green design is applied to all 13 active JFrame forms. Run the normal
`laundry.LoginForm` entry point and log in as a customer to see the new dashboard.
Staff login opens the restyled staff dashboard with its dark-green sidebar. Login,
registration, service selection, weight entry, order summary, tracking, payment,
profile, manage orders, manage services and reports now use Home's palette, rounded
components, typography, artwork and 1280 x 760 content size. Their `.form` metadata
is updated alongside Java so the old blue layout does not return when edited.
The duplicate old `DashboardForm` has been removed; Home is `CustomerDashboard`.

## Run in NetBeans

1. Install JDK 17 or newer. In **Tools > Java Platforms**, register the JDK. Select
   that platform for this Maven project under **Project Properties > Build > Compile**
   (the exact property-page label varies by NetBeans version).
2. Open this folder with **File > Open Project**. The project already declares
   FlatLaf `3.4` and MySQL Connector/J `8.4.0` in `pom.xml`. NetBeans downloads Maven
   dependencies automatically; do not add duplicate JARs to the source folder.
3. Start MySQL in XAMPP. The existing `src/laundry/database.sql` initializes the
   existing database on application startup. For a manual import, select **Import**
   in phpMyAdmin and choose that script.
4. Database defaults remain `localhost:3306`, database `metric_laundry`, user `root`,
   and an empty password. Configure your own connection with JVM options such as:

   ```text
   -Dlaundry.db.host=localhost:3306 -Dlaundry.db.name=metric_laundry -Dlaundry.db.user=your_user -Dlaundry.db.password=your_password
   ```

   Set these in NetBeans run VM options. Keep local credentials out of source control.
5. **Clean and Build**, then **Run Project**. The main class remains
   `laundry.LoginForm`. For terminal use:

   ```text
   mvn compile exec:java
   ```

The dashboard requires a customer login; running its Java file redirects to login.
It does not automatically impersonate the sample customer or admin.

## Edit the dashboard visually

Open `src/view/customer/CustomerDashboard.java` and select **Design**. Its paired
`CustomerDashboard.form` contains every layout component, including the custom
panels, buttons, timeline, table, image labels and event bindings. Build once before
opening Design so NetBeans can load the custom classes.

The content area is 1280 x 760; the OS title bar and borders add to the outer size.
The window is centered, fixed in size and has the original hanger-and-leaf icon.
The form uses Null Layout, matching the existing project and allowing individual
components to be positioned in the GUI Builder. Runtime data fills the designer's
labels; records and prices never come from designer sample text.

The other 12 forms are still located under `src/laundry`. They now have the same
white customer sidebar, dark-green staff sidebar, green gradient headers, rounded
cards and buttons, replacement green icons, and matching window branding.
Tracking uses the same six-stage `StepProgressBar` as Home. `Ui` shares the palette
with selection states, tables, status badges and receipt HTML.

`tools/restyle_swing_forms.py` performed the form migration and can regenerate Java
layouts from the current form XML. It preserves business methods and event handlers.
Its theme marker prevents repeated resizing. As with the Home generator, prefer
normal NetBeans Design editing after this migration.

Layout regeneration is optional. `tools/generate_customer_dashboard.py` creates both
the Java layout and the form metadata from the same component tree. Its business
logic template is `tools/CustomerDashboard.behavior`. If you change layout or event
handlers directly in NetBeans, keep those changes in the generated files and do not
run the generator over them unless you first update the generator/template too.

## Add the reusable components to the NetBeans Palette

1. Build this project. Open a Swing form in Design.
2. Choose **Tools > Palette > Swing/AWT Components** (or **Palette Manager** from
   the palette context menu).
3. Choose **Add from Project**, select this project and then select
   `components.RoundedPanel`, `components.RoundedButton`,
   `components.StepProgressBar` and `components.LineChartPanel`.
4. Create a **Metric Laundry** category and finish. These are public JavaBeans with
   no-argument constructors and editable properties such as `cornerRadius`,
   `gradientEnd`, `shadow`, `vertical` and `currentStep`.
5. If your NetBeans version only offers **Add from JAR**, use **Clean and Build**,
   select `target/laundry-1.0-SNAPSHOT.jar`, and choose the same four classes.

`ShadowBorder` is assigned using Java code; `StatusBadgeRenderer`, `ButtonRenderer`
and `ButtonEditor` are JTable renderers/editors and are configured on table columns,
rather than dropped onto a form. `ButtonEditor` accepts an `IntConsumer` callback
with the model row, so actions continue to target the correct record when sorted.
Palette registration is per NetBeans installation; the project supplies the beans
and instructions without modifying your IDE's private configuration.

## Images and the design system

The six brand PNG assets live in `src/resources/images`: `logo.png`, `basket.png`,
`washing.png`, `dry-cleaning.png`, `folding.png`, and `leaves.png`. Maven packages
them automatically. Use the absolute classpath prefix `/resources/images/` from
Java or choose **Image Within Project** in a label's NetBeans icon property. To
recreate these original drawings, run `tools/generate_brand_assets.py` using Python
and Pillow. Neither Python nor Pillow is needed to build or run the application.
The fresh navigation/control icon set lives in `src/resources/images/ui`; recreate
it with `tools/generate_ui_icons.py`. The old blue icon directory is archived under
`target/style-migration/original-icons` and is no longer part of application sources.

`components.Theme` centralizes the requested greens, warning/success colors, spacing
conventions and Segoe UI/SansSerif font selection. `Theme.install()` initializes
FlatLaf Light before windows are created, following the
[FlatLaf setup documentation](https://www.formdev.com/flatlaf/).

If maintaining a separate non-Maven NetBeans project, add `flatlaf-3.4.jar` and
`mysql-connector-j-8.4.0.jar` through **Project Properties > Libraries > Add JAR/Folder**.
For the future admin module, the requested BCrypt dependency is
`org.mindrot:jbcrypt:0.4` / `jbcrypt-0.4.jar`. It is intentionally not used to replace
the existing customer PBKDF2 hashes in this dashboard delivery. Admin authentication
and hash migration belong to the admin module, with compatibility checks.

## Behavior and compatibility

* Customer login and all existing Home links open the new dashboard.
* Home, Refresh and F5 read the latest services and the customer's five latest orders.
  A background `SwingWorker` keeps JDBC off the Swing event dispatch thread.
* The DAO uses PreparedStatements, scopes orders to the logged-in customer ID, and
  reads a consistent database snapshot. The controller handles errors and cancels
  pending callbacks when the frame closes or a new refresh starts.
* Order Now opens the existing weight-entry flow for that service. Unavailable
  services are disabled. View All opens the existing service selection or order list.
* Track Order and Order History open the restyled order-tracking/history screen. Payment opens
  My Orders with an explanation of the current payment workflow. About Us displays
  the supplied location and hours. The application keeps its existing workflow and
  class names while presenting the new visual design throughout.
* Empty records and database errors have explicit messages; the app does not insert
  fake dashboard orders or show fictional completion times.
* Current database service names, rates and per-kilogram units are displayed honestly.
  The requested Washing/Folding catalog, per-piece dry-cleaning billing, daily
  `ML-YYYYMMDD###` / `RC-YYYYMMDD###` numbers and seven-table schema require a
  coordinated migration with order/payment modules. This delivery retains existing
  order numbers and data. It only maps legacy `Claimed` to the visible label `Completed`.
* The six-step timeline derives Order Placed from the creation time and the remaining
  stages from actual status history. Legacy Received remains the first persisted status.
  Current steps show **In Progress**; completed steps show recorded timestamps.

## Validation

`checks/DashboardChecks.java` covers populated, empty and failing DAO results, EDT
callbacks, background reads, peso formatting, legacy status mapping, disabled checkout
after errors, and progress/chart validation. It also renders populated and empty
dashboard PNGs into `target/review/` for visual review. Run it using:

```powershell
./tools/check-dashboard.ps1
```

This offline check uses an injected DAO; it never modifies your MySQL database.
The normal Maven build compiles the production source; the standalone checks live
outside `src` so no verification fixtures are shipped with the application.

Before sign-off in your IDE, open the new form in Design and click through login,
Order Now, View All, My Orders, Refresh, About Us and logout against your local MySQL.
XML/source consistency and rendered Swing checks cannot confirm the IDE's Design tab.

To render all restyled screens against an existing local database without writing
records, run `./tools/check-dashboard.ps1 -IncludeDatabaseScreens`. That optional
check uses in-memory preview users/orders and read-only queries, and also verifies
that admin-only navigation remains hidden from staff.
