-- History snapshots keep the statement shown to the student; same lead-in rule as curriculum V6
WITH cleaned AS (
    SELECT id,
           regexp_replace(
               exercise_statement,
               '^\s*(de acuerdo (con|a)|seg[uú]n|conforme a|con base en|bas[aá]ndote en|tomando en cuenta|en el contexto de)\s+(lo que (indica|dice|se[nñ]ala|presenta)\s+)?(el|la|los|las)\s+(material(es)?|texto|contenido|documento|s[ií]labo|unidad|lectura|apuntes?)\y[^,?¿]*,\s*',
               '',
               'i') AS direct
    FROM progress.exercise_attempts
)
UPDATE progress.exercise_attempts e
SET exercise_statement = CASE
        WHEN left(c.direct, 1) = '¿' THEN '¿' || upper(substr(c.direct, 2, 1)) || substr(c.direct, 3)
        ELSE upper(left(c.direct, 1)) || substr(c.direct, 2)
    END
FROM cleaned c
WHERE e.id = c.id
  AND c.direct <> e.exercise_statement
  AND length(c.direct) > 0;
