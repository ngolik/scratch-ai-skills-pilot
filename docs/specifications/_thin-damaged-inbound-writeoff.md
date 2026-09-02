# Thin input (not a ready story)

Warehouse problem: inbound goods sometimes arrive damaged. The next shift
can see the delivery, but nobody can see that part of it was unusable, and
finance cannot see the write-off amount. People send photos and numbers
in chat.

Wanted: when a delivery has damaged goods, the operator can record that
damage against that delivery and attach a write-off money amount, so the
warehouse and finance share one picture — not two side channels.

Two facts, two audiences (do not collapse into “one record”):
- warehouse sees the delivery is damaged (and optional remark);
- finance sees a write-off amount tied to that same delivery.

Not thought through yet: is the amount optional, who is the submitting
user, can you record write-off without a delivery id, list vs single
record, max remark length, relation to already-shipped “waiting + wait-cost”
(this is damage, not waiting).

No service names. No AC. No tracker.
