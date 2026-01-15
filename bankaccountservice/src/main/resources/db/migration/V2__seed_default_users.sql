INSERT INTO accounts
(id, userid, accountnumber, accounttype, balance)
VALUES
(
  1,
  1,
  'AC11704370000000111',
  'SAVINGS',
  15000
),
(
  2,
  2,
  'AC21704370000000222',
  'SAVINGS',
  22000
),
(
  3,
  3,
  'AC31704370000000333',
  'CURRENT',
  25810
);

SELECT setval(
  pg_get_serial_sequence('accounts', 'id'),
  (SELECT MAX(id) FROM accounts)
);



