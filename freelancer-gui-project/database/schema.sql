-- =====================================================================
-- Time Management for Freelancers: database schema
-- Works on H2 (the default) and MySQL. The app creates these tables
-- automatically on start-up (see DBConnection.java); this file documents
-- the structure and lets you create the database by hand.
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    role          VARCHAR(20)  NOT NULL,          -- FREELANCER or CLIENT
    password_hash VARCHAR(200) NOT NULL           -- PBKDF2: iterations:salt:hash
);

CREATE TABLE IF NOT EXISTS projects (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    description   VARCHAR(500),
    deadline      DATE         NOT NULL,
    status        VARCHAR(20)  NOT NULL,          -- NOT_STARTED, IN_PROGRESS, COMPLETED
    freelancer_id INT          NOT NULL,
    client_id     INT          NOT NULL,
    FOREIGN KEY (freelancer_id) REFERENCES users(id),
    FOREIGN KEY (client_id)     REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS time_entries (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT       NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time   TIMESTAMP NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS invoices (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    project_id    INT         NOT NULL,
    freelancer_id INT         NOT NULL,
    client_id     INT         NOT NULL,
    invoice_type  VARCHAR(10) NOT NULL,           -- HOURLY or FIXED
    hours         DOUBLE      NOT NULL,
    rate          DOUBLE      NOT NULL,           -- rate per hour, or the flat fee for FIXED
    status        VARCHAR(10) NOT NULL,           -- PENDING or PAID
    created_date  DATE        NOT NULL,
    FOREIGN KEY (project_id)    REFERENCES projects(id),
    FOREIGN KEY (freelancer_id) REFERENCES users(id),
    FOREIGN KEY (client_id)     REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS messages (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    sender_id   INT           NOT NULL,
    receiver_id INT           NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    sent_at     TIMESTAMP     NOT NULL,
    read_at     TIMESTAMP     NULL,               -- NULL until the receiver opens the message
    FOREIGN KEY (sender_id)   REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
);

-- ---------------------------------------------------------------------
-- Handy queries (the same ideas the app uses)
-- ---------------------------------------------------------------------

-- Hours logged per freelancer (the leaderboard)
-- H2:    DATEDIFF('SECOND', t.start_time, t.end_time)
-- MySQL: TIMESTAMPDIFF(SECOND, t.start_time, t.end_time)
SELECT u.name,
       ROUND(SUM(DATEDIFF('SECOND', t.start_time, t.end_time)) / 3600.0, 2) AS hours
FROM   time_entries t
JOIN   projects p ON t.project_id = p.id
JOIN   users u    ON p.freelancer_id = u.id
GROUP  BY u.name
ORDER  BY hours DESC;

-- Money still owed, per client
SELECT c.name,
       SUM(CASE WHEN i.invoice_type = 'FIXED' THEN i.rate ELSE i.hours * i.rate END) AS amount_due
FROM   invoices i
JOIN   users c ON i.client_id = c.id
WHERE  i.status = 'PENDING'
GROUP  BY c.name;

-- Unread messages for one user
SELECT COUNT(*) FROM messages WHERE receiver_id = 1 AND read_at IS NULL;
