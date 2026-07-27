-- Derived views. Dropped and recreated on every migrate, so this file is the single
-- source of truth for them and editing it is enough to deploy a change.
--
-- The central idea: CHARGES ARE DERIVED, never stored. Their key is (doc_id, item),
-- which comes from the mirror — so re-syncing cannot double-count, and a booking that
-- gets cancelled makes its charges disappear automatically. That is precisely the bug
-- the mobile app has (it increments payments.amount on booking and never decrements).

DROP VIEW IF EXISTS v_payment_canonical;
DROP VIEW IF EXISTS v_charge_effective;
DROP VIEW IF EXISTS v_charge;
DROP VIEW IF EXISTS v_charge_item;

-- One row per billable thing a volunteer consumed. Breakfast is never persisted by the
-- app, so it can never appear here.
CREATE VIEW v_charge_item AS
  SELECT doc_id, user_id, service_date, 'lunch' AS item, 1 AS qty
    FROM fs_booking WHERE deleted_at IS NULL AND has_lunch = 1
  UNION ALL
  SELECT doc_id, user_id, service_date, 'dinner', 1
    FROM fs_booking WHERE deleted_at IS NULL AND has_dinner = 1
  UNION ALL
  SELECT doc_id, user_id, service_date, 'sleep', 1
    FROM fs_booking WHERE deleted_at IS NULL AND sleep = 1;

-- Resolve each item against the price in force ON THE DAY OF SERVICE, preferring the
-- most specific rule. LEFT JOIN, not INNER: a missing price must show up as an unpriced
-- charge in the data-quality panel, never silently vanish into a free meal.
CREATE VIEW v_charge AS
  SELECT
    ci.doc_id,
    ci.user_id,
    ci.service_date,
    ci.item,
    ci.qty,
    COALESCE(pr.unit_price_cents, 0)          AS unit_price_cents,
    ci.qty * COALESCE(pr.unit_price_cents, 0) AS amount_cents,
    (pr.id IS NOT NULL)                       AS has_price
  FROM v_charge_item ci
  LEFT JOIN fs_user u ON u.uid = ci.user_id
  LEFT JOIN price_rule pr ON pr.id = (
    SELECT p.id
      FROM price_rule p
     WHERE p.item = ci.item
       AND (p.volunteer_type IS NULL OR p.volunteer_type = u.volunteer_type)
       AND (p.is_member      IS NULL OR p.is_member      = COALESCE(u.is_member, 0))
       AND p.valid_from <= ci.service_date
       AND (p.valid_to IS NULL OR ci.service_date < p.valid_to)
     ORDER BY ((p.volunteer_type IS NOT NULL) + (p.is_member IS NOT NULL)) DESC,
              p.valid_from DESC,
              p.id DESC
     LIMIT 1
  );

-- Once a period is closed its charges are frozen, so a later price edit cannot rewrite
-- an invoice that has already gone out. A booking deleted after the close keeps its
-- locked charge — correct, the meal was eaten — and shows up in the post-close report.
CREATE VIEW v_charge_effective AS
  SELECT doc_id, user_id, service_date, item, qty, unit_price_cents, amount_cents,
         1 AS locked, 1 AS has_price
    FROM charge_locked
  UNION ALL
  SELECT c.doc_id, c.user_id, c.service_date, c.item, c.qty, c.unit_price_cents,
         c.amount_cents, 0 AS locked, c.has_price
    FROM v_charge c
   WHERE NOT EXISTS (
     SELECT 1 FROM charge_locked cl
      WHERE cl.doc_id = c.doc_id AND cl.item = c.item
   );

-- The app queries payments with .firstOrNull(), which returns Firestore's implicit
-- __name__ order — the lexicographically smallest doc id. When duplicates exist (and
-- they can, addPayment is a non-transactional read-modify-write) that is the row the
-- phone actually shows, so it is the only row write-back may touch.
CREATE VIEW v_payment_canonical AS
  SELECT p.*,
         (p.doc_id = (
           SELECT MIN(p2.doc_id) FROM fs_payment p2
            WHERE p2.user_id = p.user_id
              AND p2.year    = p.year
              AND p2.month   = p.month
              AND p2.deleted_at IS NULL
         )) AS is_canonical
    FROM fs_payment p
   WHERE p.deleted_at IS NULL;
