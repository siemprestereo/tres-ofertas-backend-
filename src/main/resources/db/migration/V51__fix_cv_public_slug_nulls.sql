UPDATE cvs SET public_slug = UUID() WHERE public_slug IS NULL OR public_slug = '';
