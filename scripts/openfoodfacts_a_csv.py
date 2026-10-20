#!/usr/bin/env python3
"""
Descarga productos REALES vendidos en México desde Open Food Facts (base abierta y gratuita)
y genera un CSV listo para cargar en Camelot con SemillaProductos.

Uso (en tu laptop, necesita internet; solo usa la librería estándar de Python):
    python3 scripts/openfoodfacts_a_csv.py                 # ~25 productos por categoría
    python3 scripts/openfoodfacts_a_csv.py --por-categoria 50 --salida productos_off.csv

Luego, para cargarlos en la tienda con id 5:
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--camelot.semilla.tienda-id=5 --camelot.semilla.archivo=file:productos_off.csv"

OJO: Open Food Facts NO tiene precios. Se pone un precio de referencia por subcategoría
(columna 'precio') para que la tienda no salga vacía; revísalos y ajústalos en el catálogo.
Los datos de Open Food Facts tienen licencia ODbL: si los publicas, menciona la fuente.
"""
import argparse
import json
import sys
import time
import urllib.parse
import urllib.request

API = "https://world.openfoodfacts.org/api/v2/search"
USER_AGENT = "Camelot-Abarrotes/1.0 (proyecto escolar UAM; contacto en el README)"

# categoría OFF  →  (categoría Camelot, subcategoría, precio referencia MXN, stock inicial)
CATEGORIAS = {
    "en:sodas":                    ("Bebidas", "Refrescos", 22, 24),
    "en:fruit-juices":             ("Bebidas", "Jugos", 18, 24),
    "en:waters":                   ("Bebidas", "Agua", 14, 36),
    "en:milks":                    ("Lácteos", "Leche", 30, 24),
    "en:yogurts":                  ("Lácteos", "Yoghurt", 25, 12),
    "en:cheeses":                  ("Lácteos", "Quesos", 60, 8),
    "en:breads":                   ("Panadería", "Pan", 50, 10),
    "en:cakes":                    ("Panadería", "Pastelitos", 20, 20),
    "en:chips-and-fries":          ("Botanas", "Papas", 20, 24),
    "en:salty-snacks":             ("Botanas", "Frituras", 18, 24),
    "en:biscuits":                 ("Galletas", "Galletas", 25, 20),
    "en:chocolates":               ("Dulces", "Chocolates", 18, 30),
    "en:candies":                  ("Dulces", "Dulces", 8, 40),
    "en:breakfast-cereals":        ("Desayuno", "Cereales", 65, 8),
    "en:coffees":                  ("Desayuno", "Café", 90, 6),
    "en:pastas":                   ("Abarrotes", "Pastas", 13, 30),
    "en:instant-noodles":          ("Abarrotes", "Sopas instantáneas", 17, 36),
    "en:rices":                    ("Abarrotes", "Granos", 35, 15),
    "en:beans":                    ("Abarrotes", "Granos", 40, 15),
    "en:vegetable-oils":           ("Abarrotes", "Aceites", 55, 10),
    "en:sauces":                   ("Abarrotes", "Salsas", 25, 15),
    "en:canned-foods":             ("Enlatados", "Enlatados", 25, 20),
    "en:tortillas":                ("Tortillería", "Tortillas", 30, 10),
}


def pedir(categoria, cuantos, pagina=1):
    params = {
        "countries_tags_en": "mexico",
        "categories_tags_en": categoria.removeprefix("en:"),
        "fields": "code,product_name_es,product_name,brands,quantity,image_front_url",
        "sort_by": "unique_scans_n",   # los más escaneados = los más comunes
        "page_size": str(cuantos),
        "page": str(pagina),
    }
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r).get("products", [])


def limpiar(s):
    return " ".join((s or "").replace(";", ",").split())


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--por-categoria", type=int, default=25)
    ap.add_argument("--salida", default="productos_off.csv")
    args = ap.parse_args()

    vistos = set()
    filas = []
    for cat_off, (cat, sub, precio, stock) in CATEGORIAS.items():
        try:
            productos = pedir(cat_off, args.por_categoria)
        except Exception as e:  # noqa: BLE001
            print(f"  ! {cat_off}: {e}", file=sys.stderr)
            continue
        nuevos = 0
        for p in productos:
            nombre = limpiar(p.get("product_name_es") or p.get("product_name"))
            marca = limpiar((p.get("brands") or "").split(",")[0])
            cantidad = limpiar(p.get("quantity"))
            if not nombre:
                continue
            if marca and marca.lower() not in nombre.lower():
                nombre = f"{nombre} {marca}"
            if cantidad and cantidad.lower() not in nombre.lower():
                nombre = f"{nombre} {cantidad}"
            nombre = nombre[:120]
            clave = nombre.lower()
            if clave in vistos:
                continue
            vistos.add(clave)
            costo = round(precio * 0.78, 2)
            imagen = p.get("image_front_url") or ""
            filas.append(f"{nombre};{cat};{sub};{precio:.2f};{costo:.2f};{stock};;;{imagen}")
            nuevos += 1
        print(f"  {cat_off:<24} {nuevos:>3} productos")
        time.sleep(1)  # OFF pide no saturar su API

    with open(args.salida, "w", encoding="utf-8") as f:
        f.write("# Generado desde Open Food Facts (ODbL). Precios de referencia: ajústalos.\n")
        f.write("# nombre;categoria;subcategoria;precio;costo_proveedor;stock;unidad;cantidad;imagen_url\n")
        f.write("\n".join(filas) + "\n")
    print(f"\nListo: {len(filas)} productos en {args.salida}")


if __name__ == "__main__":
    main()
