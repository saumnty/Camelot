-- =====================================================================
-- Camelot · Migración 02 · Córrela en Supabase DESPUÉS del primer
-- despliegue en Render (cuando Hibernate ya creó las tablas nuevas:
-- configuracion_tienda, notificacion, cliente_tienda, movimiento_fiado, pago).
--
-- Es segura de correr más de una vez. No borra datos.
-- =====================================================================

BEGIN;

-- 1) Cada dueño (tienda.owner_id) queda como ADMIN de su tienda.
--    Antes crear una tienda no te hacía admin de ella.
INSERT INTO public.tienda_usuario (tienda_id, usuario_id, rol, estado, created_at)
SELECT t.id, t.owner_id, 'ADMIN', 'ACTIVO', now()
FROM public.tienda t
WHERE t.owner_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM public.tienda_usuario tu
    WHERE tu.tienda_id = t.id AND tu.usuario_id = t.owner_id
  );

-- 2) Configuración por tienda: cada tienda arranca con los valores que tenía
--    la configuración global vieja (IVA, moneda, auto-confirmar).
--    RFC, dirección, etc. NO se copian: eran de una sola tienda; cada dueño los captura.
INSERT INTO public.configuracion_tienda (
    tienda_id, iva, precios_incluyen_iva, moneda, auto_confirmar_si_hay_stock,
    acepta_tarjeta, acepta_efectivo, acepta_fiado, permite_reembolsos,
    permite_clientes_frecuentes, updated_at)
SELECT t.id,
       COALESCE(cs.iva, 16.0),
       true,
       COALESCE(NULLIF(cs.moneda, ''), 'MXN'),
       COALESCE(cs.auto_confirmar_si_hay_stock, false),
       true, true, false, true, true, now()
FROM public.tienda t
LEFT JOIN public.configuracion_sistema cs ON cs.id = 1
WHERE NOT EXISTS (SELECT 1 FROM public.configuracion_tienda ct WHERE ct.tienda_id = t.id);

COMMIT;

-- 3) (OPCIONAL) La libreta de clientes vieja era global. Ahora cada tienda tiene la suya
--    y las filas sin tienda ya no se ven. Si eran de tu tienda, asígnalas (cambia el 1):
--
--   UPDATE public.clientes SET tienda_id = 1 WHERE tienda_id IS NULL;

-- 4) (OPCIONAL, cuando ya no la necesites) tablas viejas que el código ya no usa:
--
--   DROP TABLE IF EXISTS public.ventas;
--   DROP TABLE IF EXISTS public.configuracion_sistema;
