-- Identical classification inputs must always reuse the same venue setting.
select input_fingerprint
from {{ ref("fct_event_attributes") }}
group by input_fingerprint
having count(distinct venue_setting) > 1
