INSERT INTO accounts
(id, userid, accountnumber, accounttype, balance)
VALUES
(
  1,
  1,
  'AC11768471543584479',
  'SAVINGS',
  15000
),
(
  2,
  2,
  'AC21768471543584044',
  'SAVINGS',
  22000
),
(
  3,
  3,
  'AC31768471543584173',
  'CURRENT',
  25810
);

SELECT setval(
  pg_get_serial_sequence('accounts', 'id'),
  (SELECT MAX(id) FROM accounts)
);



