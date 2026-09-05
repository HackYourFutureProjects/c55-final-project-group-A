-- The backend must receive a non-empty, sorted and distinct category array.
-- `Other` is a fallback and must not accompany a more specific category.
select external_event_key, categories
from {{ ref("fct_external_events") }}
where
    categories is null
    or size(categories) = 0
    or size(categories) <> size(array_distinct(categories))
    or categories <> sort_array(categories)
    or (size(categories) > 1 and array_contains(categories, 'Other'))
    or exists (
        categories,
        category -> category not in (
            'Music',
            'Arts & Culture',
            'Theatre & Performance',
            'Family & Kids',
            'Community & Social',
            'Sports & Fitness',
            'Food & Drink',
            'Other'
        )
    )
