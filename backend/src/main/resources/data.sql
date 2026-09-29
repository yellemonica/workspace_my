-- Runs on every startup; each table is seeded only while it is empty, so user changes survive restarts.

INSERT INTO students (first_name, last_name, email, date_of_birth, enrollment_date)
SELECT v.first_name, v.last_name, v.email, v.date_of_birth, v.enrollment_date
FROM (VALUES
        ('Ada',     'Lovelace', 'ada.lovelace@example.edu',   DATE '2003-12-10', DATE '2022-09-01'),
        ('Alan',    'Turing',   'alan.turing@example.edu',    DATE '2002-06-23', DATE '2021-09-01'),
        ('Grace',   'Hopper',   'grace.hopper@example.edu',   DATE '2004-12-09', DATE '2023-09-01'),
        ('Linus',   'Torvalds', 'linus.torvalds@example.edu', DATE '2003-12-28', DATE '2022-09-01'),
        ('Barbara', 'Liskov',   'barbara.liskov@example.edu', DATE '2005-11-07', DATE '2024-09-01')
     ) AS v (first_name, last_name, email, date_of_birth, enrollment_date)
WHERE NOT EXISTS (SELECT 1 FROM students);

INSERT INTO courses (code, title, credits, description)
SELECT v.code, v.title, v.credits, v.description
FROM (VALUES
        ('CS101',   'Introduction to Programming', 4, 'Fundamentals of programming with Java.'),
        ('CS201',   'Data Structures',             4, 'Lists, trees, graphs, hashing and their complexity.'),
        ('CS305',   'Databases',                   3, 'Relational modelling, SQL and transactions.'),
        ('MATH110', 'Discrete Mathematics',        3, 'Logic, sets, combinatorics and graph theory.'),
        ('PHYS120', 'Classical Mechanics',         5, 'Newtonian mechanics, energy and momentum.'),
        ('HIST210', 'History of Computing',        2, 'From the Analytical Engine to the internet.')
     ) AS v (code, title, credits, description)
WHERE NOT EXISTS (SELECT 1 FROM courses);

INSERT INTO enrollments (student_id, course_id, enrolled_at)
SELECT s.id, c.id, CURRENT_TIMESTAMP
FROM (VALUES
        ('ada.lovelace@example.edu',   'CS101'),
        ('ada.lovelace@example.edu',   'MATH110'),
        ('alan.turing@example.edu',    'CS201'),
        ('alan.turing@example.edu',    'MATH110'),
        ('alan.turing@example.edu',    'HIST210'),
        ('grace.hopper@example.edu',   'CS101'),
        ('linus.torvalds@example.edu', 'CS305')
     ) AS v (email, code)
JOIN students s ON s.email = v.email
JOIN courses c ON c.code = v.code
WHERE NOT EXISTS (SELECT 1 FROM enrollments);
