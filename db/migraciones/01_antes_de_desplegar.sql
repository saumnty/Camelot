-- =====================================================================
-- Camelot · Migración 01 · Córrela en Supabase (SQL Editor) ANTES de
-- hacer merge / desplegar en Render.
--
-- Es segura de correr más de una vez. No borra datos.
-- =====================================================================

BEGIN;

-- 1) compra.estado: agregar el estado REEMBOLSADA.
--    Hibernate creó un CHECK con los estados viejos y no lo actualiza solo;
--    sin esto, reembolsar una compra daría error.
DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT conname
    FROM pg_constraint
    WHERE conrelid = 'public.compra'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) ILIKE '%CANCELADA%'
      AND pg_get_constraintdef(oid) NOT ILIKE '%REEMBOLSADA%'
  LOOP
    EXECUTE format('ALTER TABLE public.compra DROP CONSTRAINT %I', r.conname);
  END LOOP;
END $$;

ALTER TABLE public.compra DROP CONSTRAINT IF EXISTS compra_estado_check;
ALTER TABLE public.compra ADD CONSTRAINT compra_estado_check
  CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'REEMBOLSADA'));

-- 2) tienda_solicitud_admin: quitar UNIQUE(tienda_id, solicitante_id, estado).
--    Con él, rechazar dos veces al mismo usuario en la misma tienda tronaba.
--    Se reemplaza por "solo una solicitud PENDIENTE por usuario y tienda".
DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT c.conname
    FROM pg_constraint c
    WHERE c.conrelid = 'public.tienda_solicitud_admin'::regclass
      AND c.contype = 'u'
      AND (SELECT array_agg(a.attname::text ORDER BY a.attname)
           FROM unnest(c.conkey) k JOIN pg_attribute a
             ON a.attrelid = c.conrelid AND a.attnum = k)
          = ARRAY['estado', 'solicitante_id', 'tienda_id']
  LOOP
    EXECUTE format('ALTER TABLE public.tienda_solicitud_admin DROP CONSTRAINT %I', r.conname);
  END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_solicitud_admin_pendiente
  ON public.tienda_solicitud_admin (tienda_id, solicitante_id)
  WHERE estado = 'PENDIENTE';

COMMIT;
