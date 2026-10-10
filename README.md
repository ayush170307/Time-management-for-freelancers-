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

There's also a small deadline checker running in the background. Every 30 seconds it looks at your projects and shows a warning at the top of your dashboard if anything is overdue or due within 3 days.

**As a client** you can:

- See all the invoices you've received, look one up by its number, and mark it as paid.
- Check the status of your projects, with a quick summary of how many are finished.
- Read messages from your freelancer.

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

## How the code is organised

Everything lives in one package called `app`. Roughly:

- **Screens:** `MainApp` starts things up, `MainFrame` is the window, and `HomePanel`, `FreelancerDashboardPanel` and `ClientDashboardPanel` are the actual screens.
- **Model classes:** `User` is an abstract class that `Freelancer` and `Client` extend. `Invoice` is also abstract, with `HourlyInvoice` and `FixedPriceInvoice` underneath it. Then there are `Project`, `TimeEntry` and `Message`.
- **Database classes:** `DBConnection` handles the JDBC connection and creates the tables. There's a generic `Dao<T>` interface, a `BaseDao` with the shared JDBC code, and one DAO per table (`UserDao`, `ProjectDao`, `TimeEntryDao`, `InvoiceDao`, `MessageDao`).
- **Everything else:** `BillingService` has the rules for logging time and creating invoices, `StopwatchThread` and `DeadlineMonitor` are the two background threads, `Validator` checks user input, `PasswordUtil` hashes and checks passwords, and `Ui` has a few small helper methods.

The database has five tables: `users` (including the password hash), `projects`, `time_entries`, `invoices` and `messages`. They're created automatically on startup, so there's no setup script to run.

## Java concepts used

Since this is a course project, here's where the main concepts show up:

- **Inheritance and polymorphism:** `Freelancer` and `Client` extend `User`, and both invoice types extend `Invoice`. Each invoice type calculates its total differently by overriding `calculateTotal()`.
- **Interfaces and abstract classes:** `Billable`, `Closable`, and the generic `Dao<T>` are interfaces. `User` and `Invoice` are abstract.
- **Security basics:** passwords are salted and hashed (PBKDF2), compared in constant time, and the login error message is the same whether the email or the password was wrong.
- **Exception handling:** I made two custom checked exceptions, `InvalidInputException` for bad user input and `DatabaseException` for anything that goes wrong with the database.
- **Collections and generics:** lists, `Optional`, an `EnumMap` for the project summary, and the generic DAO classes.
- **Multithreading and synchronization:** the stopwatch thread uses a synchronized counter, the deadline monitor polls in the background, and invoice generation is locked so two threads can't create an invoice at the same time.
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

- **Updating from an older version of the project:** the users table now has a password column. Delete the old `freelancer_db.mv.db` file once so the database gets recreated, then register your accounts again.
- **`javac` not found:** you probably have a JRE instead of a JDK, or it isn't on your PATH.
- **`Invalid filename: app\*.java`:** you're in the wrong folder. Make sure you're in the folder that directly contains `app` and `lib`, not inside `app` itself.
- **"Startup failed" or "No suitable driver" when the app opens:** the H2 jar is missing from `lib`, or the classpath is missing `lib/*`.
- **`Could not find or load main class app.MainApp`:** run `java` from the project root and use the full name `app.MainApp`.
- **VS Code shows "non-project file" warnings:** open the project root folder in VS Code instead of opening single files.

## Author

Made by Ayush.

I haven't added a license yet. If you want to reuse any of this, feel free to open an issue and ask.
