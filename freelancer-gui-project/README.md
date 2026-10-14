# Time Management for Freelancers

This is a Java desktop app I built for my Java course project. The idea is simple: freelancers juggle a lot of clients and projects, and it's easy to lose track of how many hours they actually worked and what they should bill. This app keeps all of that in one place.

A freelancer can track their time, manage projects, and send invoices. A client can log in to check their invoices and see how their projects are going. Everything is saved in a database, so nothing disappears when you close the app.

It's built with Java Swing for the interface and plain JDBC for the database. No Maven, no Gradle, no frameworks.

## What it can do

**As a freelancer** you can:

- Track billable time with a live timer (start it, do your work, hit stop and it saves the entry), or just type in hours manually if you forgot to start the timer.
- Create projects for a client, give them a deadline, and update their status as you go (not started, in progress, completed).
- Generate invoices. You can bill hourly (hours worked x your rate) or charge a fixed price for the whole project.
- Send messages to your clients.

Every table has a live search box, and you can click a column header to sort it. Double-click an invoice to see its details, or export it as a text receipt. The overview page shows your hours per project and how much of your invoicing has been paid, as simple charts.

There's a **leaderboard** that ranks every freelancer by the hours they've logged, with a podium for the top three and a switch between "this week" and "all time". Clients can see it too, which helps when choosing who to work with.

The interface is animated: pages fade in, the numbers on the overview count up, the charts and leaderboard bars grow into place, and buttons fade smoothly when you hover over them. Cards have smooth rounded corners with a soft blurred shadow.

A background thread checks the database every 15 seconds. It shows a warning on the overview page and pops up an alert when a project is due within 48 hours or overdue, and it tells freelancers when a client marks an invoice as paid (and tells clients when a new invoice arrives).

A few more things worth knowing:

- **Invoices from logged time.** Pick a project and a date range (or press "This month"), and the hours in that period are added up for you before you generate the invoice. Nothing has to be typed by hand except the rate.
- **PDF invoices.** "Export Invoice" saves a styled PDF with the freelancer, client, project, breakdown and total (or a plain text receipt if you prefer). I wrote a tiny PDF generator myself, so there's no extra library to download.
- **Reports.** Export your timesheet or an earnings report as CSV, which opens straight in Excel.
- **Messaging.** Freelancers and clients can write to each other. There is an inbox and a sent list, timestamps, read receipts ("Delivered" turns into "Read 14:05" once the other person opens it), and a red unread counter on the sidebar.
- **System tray.** If a timer is running and you close or minimise the window, the app hides in the system tray instead of quitting, so the timer keeps going. Double-click the tray icon to bring it back.
- **Dark mode.** There is a Light mode / Dark mode button in the sidebar and on the login screen. The choice is remembered next time you open the app.
- **Safer forms.** The register form checks the email, password length and password match as you type, and the amount and hour fields simply won't accept letters or negative numbers.
- **Access checks.** Every page change and every sensitive action checks the logged-in user's role, so a client can't reach freelancer screens and nobody can change another person's data.

**As a client** you can:

- See all the invoices you've received, look one up by its number, and mark it as paid.
- Check the status of your projects, with a quick summary of how many are finished.
- Read messages from your freelancer.
- Export any invoice as a text receipt, and see a paid-versus-pending chart on the overview page.

You can register as many freelancers and clients as you want. Emails are validated, and the same email can't be registered twice.

Every account has a password. You create one when you register (and confirm it), and you log in with your email and password. Passwords are never stored as plain text. They're salted and hashed with PBKDF2, which comes built into Java, so no extra libraries are needed.

## What you need

- A JDK, version 17 or newer. Make sure it's a full JDK and not just a JRE, because you need `javac`. You can check by running `javac -version`.
- The H2 database jar. H2 is a small embedded database, so there's no server to install. You just drop one jar file into the `lib` folder.

You can get the jar from https://h2database.com/html/download.html (download the platform-independent zip and look in the `bin` folder), or from Maven Central under `com.h2database:h2`.

## Running it

Clone the repo and put the H2 jar in the `lib` folder:

```
git clone https://github.com/<your-username>/<your-repo>.git
cd <your-repo>
```

Then, from the main project folder (the one that has `app` and `lib` inside it), compile and run.

On Windows:

```
javac -cp "lib/*" app\*.java
java -cp ".;lib/*" app.MainApp
```

On Mac or Linux:

```
javac -cp "lib/*" app/*.java
java -cp ".:lib/*" app.MainApp
```

There's also a `compile_and_run.bat` you can double-click on Windows if you don't feel like typing the commands.

The first time you run it, a file called `freelancer_db.mv.db` gets created in the project folder. That's your database. If you ever want to start fresh, just delete that file.

## Trying it out

If you're not sure where to start, this is a good first walkthrough:

1. Register one freelancer and one client from the home screen. You'll pick a password for each (at least 6 characters).
2. Log in as the freelancer and create a project for that client (Projects tab, then New Project).
3. Go to the Time Tracking tab, start the timer, wait a few seconds, and stop it.
4. Go to Invoices, pick the project, choose hourly or fixed, enter a rate or amount, and generate the invoice.
5. Log out, then log back in as the client using their email and password, and you should see the invoice, the project, and any messages.

## What is in the repository

- `app/` holds all the Java source (package `app`).
- `lib/` is where you put the H2 jar.
- `database/schema.sql` is the full database schema, with a few example queries. It works on H2 and MySQL.
- `docs/` is a **working browser version of the app** (`index.html`, `style.css`, `app.js`). It runs on sample data, with the timer, charts, invoices, leaderboard, messages and dark mode. Publish it with GitHub Pages (Settings, Pages, deploy from the `main` branch and the `/docs` folder) and anyone can try the app without installing Java.
- `database/sample_data.sql` holds demo accounts, projects, logged time, invoices and messages. `scripts/make_demo_data.py` generates it.
- `compile_and_run.bat` (Windows) and `run.sh` (Mac and Linux) compile and start the app for you.

## Trying it with demo data

If you don't want to type in data by hand, load the demo data once:

1. Run the app once so it creates the tables, then close it (the database file can only be open in one place).
2. Load the file (replace the jar name with yours):
   ```
   java -cp "lib/h2-2.2.224.jar" org.h2.tools.RunScript -url jdbc:h2:./freelancer_db -user sa -script database/sample_data.sql
   ```
3. Start the app and log in with `sara@example.com` (a freelancer) or `acme@example.com` (a client). Every demo account uses the password `demo1234`.

To make fresh demo data with today's dates, run `python scripts/make_demo_data.py`.

## How the code is organised

Everything lives in one package called `app`. Roughly:

- **Screens:** `MainApp` starts things up, `MainFrame` is the window, and `HomePanel`, `FreelancerDashboardPanel` and `ClientDashboardPanel` are the actual screens. Both dashboards share a sidebar layout from `DashboardShell`, and `Theme` and `RoundedButton` hold the colours, fonts and custom-painted buttons so the whole app looks consistent.
- **Model classes:** `User` is an abstract class that `Freelancer` and `Client` extend. `Invoice` is also abstract, with `HourlyInvoice` and `FixedPriceInvoice` underneath it. Then there are `Project`, `TimeEntry` and `Message`.
- **Database classes:** `DBConnection` handles the JDBC connection and creates the tables. There's a generic `Dao<T>` interface, a `BaseDao` with the shared JDBC code, and one DAO per table (`UserDao`, `ProjectDao`, `TimeEntryDao`, `InvoiceDao`, `MessageDao`).
- **Everything else:** `BillingService` has the rules for logging time and creating invoices, `StopwatchThread` and `DeadlineMonitor` are the two background threads, `Session` and `AccessDeniedException` enforce who can do what, `SimplePdf` writes the PDF invoices, `ReportExporter` writes the CSV reports, `ActivityMonitor` is the background alert thread, `TrayManager` handles the system tray, `MessagesPage` is the two-way inbox, `Badge` and `EmptyState` draw the status pills and the empty-screen graphics, `StyledComboBoxUI` and `ThinScrollBarUI` restyle dropdowns and scroll bars, `LeaderboardService`, `LeaderboardPage` and `LeaderboardPanel` build and draw the ranking, `Animator` drives all the animations (with `AnimatedNumberLabel` for the counting numbers), `RoundedPanel` draws the blurred-shadow cards, `InvoiceExporter` saves receipts to a file, `BarChartPanel` and `DonutChartPanel` draw the charts, `ToastGlassPane` shows the small fading notifications, `Validator` checks user input, `PasswordUtil` hashes and checks passwords, and `Ui` has a few small helper methods.

The database has five tables: `users` (including the password hash), `projects`, `time_entries`, `invoices` and `messages`. They're created automatically on startup, so there's no setup script to run.

## Java concepts used

Since this is a course project, here's where the main concepts show up:

- **Inheritance and polymorphism:** `Freelancer` and `Client` extend `User`, and both invoice types extend `Invoice`. Each invoice type calculates its total differently by overriding `calculateTotal()`.
- **Interfaces and abstract classes:** `Billable`, `Closable`, and the generic `Dao<T>` are interfaces. `User` and `Invoice` are abstract.
- **Security basics:** passwords are salted and hashed (PBKDF2, which ships with Java; BCrypt would need an extra library), compared in constant time, and the login error message is the same whether the email or the password was wrong. A `Session` class checks the user's role on every page change and before every sensitive action.
- **Exception handling:** I made two custom checked exceptions, `InvalidInputException` for bad user input and `DatabaseException` for anything that goes wrong with the database.
- **Collections and generics:** lists, `Optional`, an `EnumMap` for the project summary, a `LinkedHashMap` plus a custom comparator for the leaderboard, and the generic DAO classes.
- **Multithreading and synchronization:** the stopwatch thread uses a synchronized counter, the deadline monitor polls in the background, and invoice generation is locked so two threads can't create an invoice at the same time.
- **Why plain JDBC and not Hibernate:** the course marks JDBC connectivity and database classes directly, so I kept a hand-written DAO layer. It is still easy to move to MySQL (see below).
- **JDBC:** prepared statements, joins, generated keys, and try-with-resources for closing connections.

## Using MySQL instead

H2 was just the easiest option for me, but the code is plain JDBC, so switching is easy. Open `app/DBConnection.java` and change these three lines:

```java
private static final String URL = "jdbc:mysql://localhost:3306/freelancer_db";
private static final String USER = "root";
private static final String PASSWORD = "your_password";
```

You'll need to create the `freelancer_db` database yourself first, and put the MySQL Connector/J jar in `lib` instead of the H2 one.

## If something goes wrong

These are the problems I ran into while setting this up, so hopefully they save you some time.

- **Messages table:** older databases get the new `read_at` column added automatically on start-up, so you do not need to delete anything for read receipts.
- **Updating from a very old version of the project:** the users table now has a password column. Delete the old `freelancer_db.mv.db` file once so the database gets recreated, then register your accounts again.
- **`javac` not found:** you probably have a JRE instead of a JDK, or it isn't on your PATH.
- **`Invalid filename: app\*.java`:** you're in the wrong folder. Make sure you're in the folder that directly contains `app` and `lib`, not inside `app` itself.
- **"Startup failed" or "No suitable driver" when the app opens:** the H2 jar is missing from `lib`, or the classpath is missing `lib/*`.
- **`Could not find or load main class app.MainApp`:** run `java` from the project root and use the full name `app.MainApp`.
- **VS Code shows "non-project file" warnings:** open the project root folder in VS Code instead of opening single files.

## Author

Made by Ayush.

I haven't added a license yet. If you want to reuse any of this, feel free to open an issue and ask.
