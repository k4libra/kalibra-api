-- Students never see the curricular material, so statements must not cite it
-- ("De acuerdo con el material curricular sobre X, ..."). Same rule as the engine's
-- statement sanitizer; only the leading clause is removed and the next letter is capitalized.
WITH cleaned AS (
    SELECT id,
           regexp_replace(
               statement,
               '^\s*(de acuerdo (con|a)|seg[uú]n|conforme a|con base en|bas[aá]ndote en|tomando en cuenta|en el contexto de)\s+(lo que (indica|dice|se[nñ]ala|presenta)\s+)?(el|la|los|las)\s+(material(es)?|texto|contenido|documento|s[ií]labo|unidad|lectura|apuntes?)\y[^,?¿]*,\s*',
               '',
               'i') AS direct
    FROM curriculum.generated_exercises
)
UPDATE curriculum.generated_exercises e
SET statement = CASE
        WHEN left(c.direct, 1) = '¿' THEN '¿' || upper(substr(c.direct, 2, 1)) || substr(c.direct, 3)
        ELSE upper(left(c.direct, 1)) || substr(c.direct, 2)
    END
FROM cleaned c
WHERE e.id = c.id
  AND c.direct <> e.statement
  AND length(c.direct) > 0;
