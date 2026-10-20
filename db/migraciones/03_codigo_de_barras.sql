-- =====================================================================
-- Camelot · Migración 03 · Código de barras en productos.
-- Córrela en Supabase ANTES de desplegar la versión con "Importar productos".
-- (Si en Render tienes SPRING_JPA_HIBERNATE_DDL_AUTO=validate, sin esto la app no arranca.)
-- Segura de correr más de una vez. No borra datos.
-- =====================================================================

ALTER TABLE public.producto ADD COLUMN IF NOT EXISTS codigo_barras varchar(32);

CREATE INDEX IF NOT EXISTS idx_producto_tienda_codigo
  ON public.producto (tienda_id, codigo_barras);
