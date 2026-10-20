#!/usr/bin/env python3
"""
Descarga los productos MÁS COMUNES que se venden en México desde Open Food Facts
(base abierta y gratuita) y genera archivos CSV listos para subir en Camelot:
    Mi tienda → Catálogo y precios → Importar (Excel / CSV)

Uso (en tu computadora, con internet; solo usa la librería estándar de Python 3.9+):
    python scripts/openfoodfacts_a_csv.py                     # 1000 productos
    python scripts/openfoodfacts_a_csv.py --cuantos 1500
    python scripts/openfoodfacts_a_csv.py --cuantos 300 --salida mi_catalogo

Genera mi_catalogo_1.csv, mi_catalogo_2.csv... (máximo 1900 productos por archivo,
porque Camelot importa hasta 2000 por archivo).

IMPORTANTE
- Se ordenan por "más escaneados" en Open Food Facts = los más comunes en tiendas.
- Open Food Facts NO tiene precios. Se pone un precio de REFERENCIA por categoría solo
  para que se puedan importar; revísalos (la vista previa te deja ver todo antes de guardar).
- Stock y costo se dejan vacíos (no se inventa inventario).
- Open Food Facts es sobre todo comida y bebida. Limpieza e higiene casi no vienen.
- Licencia de los datos: ODbL (menciona "Datos de Open Food Facts" si los publicas).
- La API de búsqueda permite ~10 consultas por minuto; el script espera entre páginas.
"""
import argparse
import csv
import json
import re
import sys
import time
import urllib.parse
import urllib.request

API = "https://world.openfoodfacts.org/api/v2/search"
USER_AGENT = "Camelot-Abarrotes/1.1 (proyecto escolar UAM; https://github.com/saumnty/Camelot)"
POR_PAGINA = 100
MAX_POR_ARCHIVO = 1900
ESPERA_SEG = 7  # ~8-9 consultas por minuto

# Etiqueta de categoría de OFF (en inglés) → (categoría, subcategoría, precio de referencia MXN).
# Se revisan en este orden: las más específicas primero.
CATEGORIAS = [
    ("en:sodas",                 "Bebidas", "Refrescos", 22),
    ("en:energy-drinks",         "Bebidas", "Energizantes", 35),
    ("en:sports-drinks",         "Bebidas", "Hidratantes", 24),
    ("en:fruit-juices",          "Bebidas", "Jugos", 18),
    ("en:fruit-based-beverages", "Bebidas", "Jugos", 18),
    ("en:waters",                "Bebidas", "Agua", 14),
    ("en:coffees",               "Desayuno", "Café", 90),
    ("en:teas",                  "Desayuno", "Té", 35),
    ("en:beers",                 "Bebidas", "Cerveza", 25),
    ("en:alcoholic-beverages",   "Bebidas", "Vinos y licores", 180),
    ("en:milks",                 "Lácteos", "Leche", 30),
    ("en:yogurts",               "Lácteos", "Yoghurt", 25),
    ("en:cheeses",               "Lácteos", "Quesos", 60),
    ("en:creams",                "Lácteos", "Crema", 38),
    ("en:butters",               "Lácteos", "Mantequilla", 45),
    ("en:eggs",                  "Huevo", "Huevo", 70),
    ("en:breads",                "Panadería", "Pan", 50),
    ("en:cakes",                 "Panadería", "Pastelitos", 20),
    ("en:tortillas",             "Tortillería", "Tortillas", 30),
    ("en:chips-and-fries",       "Botanas", "Papas", 20),
    ("en:crisps",                "Botanas", "Papas", 20),
    ("en:salty-snacks",          "Botanas", "Frituras", 18),
    ("en:nuts",                  "Botanas", "Cacahuates y semillas", 25),
    ("en:biscuits",              "Galletas", "Galletas", 25),
    ("en:cookies",               "Galletas", "Galletas", 25),
    ("en:chocolates",            "Dulces", "Chocolates", 18),
    ("en:candies",               "Dulces", "Dulces", 8),
    ("en:chewing-gum",           "Dulces", "Chicles", 15),
    ("en:breakfast-cereals",     "Desayuno", "Cereales", 65),
    ("en:cocoa-and-its-products","Desayuno", "Chocolate en polvo", 45),
    ("en:instant-noodles",       "Abarrotes", "Sopas instantáneas", 17),
    ("en:pastas",                "Abarrotes", "Pastas", 13),
    ("en:rices",                 "Abarrotes", "Granos", 35),
    ("en:beans",                 "Abarrotes", "Granos", 40),
    ("en:legumes",               "Abarrotes", "Granos", 35),
    ("en:flours",                "Abarrotes", "Harinas", 30),
    ("en:sugars",                "Abarrotes", "Azúcar", 33),
    ("en:salts",                 "Abarrotes", "Condimentos", 16),
    ("en:vegetable-oils",        "Abarrotes", "Aceites", 55),
    ("en:mayonnaises",           "Abarrotes", "Aderezos", 45),
    ("en:hot-sauces",            "Abarrotes", "Salsas", 22),
    ("en:sauces",                "Abarrotes", "Salsas", 25),
    ("en:condiments",            "Abarrotes", "Condimentos", 25),
    ("en:bouillon-cubes",        "Abarrotes", "Condimentos", 30),
    ("en:canned-fishes",         "Enlatados", "Atún y sardina", 28),
    ("en:canned-vegetables",     "Enlatados", "Verduras", 22),
    ("en:canned-foods",          "Enlatados", "Enlatados", 25),
    ("en:jams",                  "Abarrotes", "Mermeladas", 40),
    ("en:spreads",               "Abarrotes", "Untables", 45),
    ("en:frozen-foods",          "Congelados", "Congelados", 60),
    ("en:meats",                 "Salchichonería", "Carnes frías", 45),
    ("en:sausages",              "Salchichonería", "Salchichas", 45),
    ("en:baby-foods",            "Bebé", "Alimento infantil", 30),
    ("en:snacks",                "Botanas", "Botanas", 18),
    ("en:beverages",             "Bebidas", "Bebidas", 20),
]
OTROS = ("Abarrotes", "Otros", 25)

MEDIDA = re.compile(r"(\d+(?:[.,]\d+)?)\s*(ml|cl|l|lt|kg|g|gr|oz|pzas?|piezas?)\b", re.I)


def pedir(pagina):
    params = {
        "countries_tags_en": "mexico",
        "fields": "code,product_name_es,product_name,brands,quantity,image_front_url,categories_tags",
        "sort_by": "unique_scans_n",
        "page_size": str(POR_PAGINA),
        "page": str(pagina),
    }
    url = API + "?" + urllib.parse.urlencode(params)
    for intento in range(3):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(req, timeout=60) as r:
                return json.load(r).get("products", [])
        except Exception as e:  # noqa: BLE001
            print(f"   ! página {pagina}, intento {intento + 1}: {e}", file=sys.stderr)
            time.sleep(20)
    return []


def limpiar(s):
    return " ".join((s or "").split())


def codigo_valido(c):
    return bool(re.fullmatch(r"\d{8}|\d{12,14}", c or ""))


def clasificar(tags):
    tags = set(tags or [])
    for tag, cat, sub, precio in CATEGORIAS:
        if tag in tags:
            return cat, sub, precio
    return OTROS


def convertir(p):
    """Producto de OFF → fila para Camelot (o None si no sirve)."""
    codigo = re.sub(r"\D", "", p.get("code") or "")
    nombre = limpiar(p.get("product_name_es") or p.get("product_name"))
    if not codigo_valido(codigo) or not nombre:
        return None
    marca = limpiar((p.get("brands") or "").split(",")[0])
    cantidad = limpiar(p.get("quantity"))
    if marca and marca.lower() not in nombre.lower():
        nombre = f"{nombre} {marca}"
    if cantidad and not MEDIDA.search(nombre):
        nombre = f"{nombre} {cantidad}"
    cat, sub, precio = clasificar(p.get("categories_tags"))
    return {
        "codigo_barras": codigo,
        "nombre": nombre[:120],
        "categoria": cat,
        "subcategoria": sub,
        "precio": f"{precio:.2f}",
        "cantidad": cantidad,          # Camelot entiende "600 ml", "1.5 L", "12 pzas"
        "imagen_url": p.get("image_front_url") or "",
    }


COLUMNAS = ["codigo_barras", "nombre", "categoria", "subcategoria", "precio", "cantidad", "imagen_url"]


def escribir(filas, base):
    archivos = []
    for i in range(0, len(filas), MAX_POR_ARCHIVO):
        nombre = f"{base}_{i // MAX_POR_ARCHIVO + 1}.csv"
        with open(nombre, "w", encoding="utf-8-sig", newline="") as f:  # -sig: Excel abre bien los acentos
            w = csv.DictWriter(f, fieldnames=COLUMNAS)
            w.writeheader()
            w.writerows(filas[i:i + MAX_POR_ARCHIVO])
        archivos.append(nombre)
    return archivos


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--cuantos", type=int, default=1000, help="cuántos productos (los más comunes primero)")
    ap.add_argument("--salida", default="productos_off", help="nombre base de los archivos")
    args = ap.parse_args()

    filas, vistos, pagina = [], set(), 1
    paginas_max = args.cuantos // POR_PAGINA * 3 + 5   # margen: muchos vienen sin nombre o código
    print(f"Descargando hasta {args.cuantos} productos de México (≈{ESPERA_SEG}s por página)…")
    while len(filas) < args.cuantos and pagina <= paginas_max:
        productos = pedir(pagina)
        if not productos:
            break
        nuevos = 0
        for p in productos:
            fila = convertir(p)
            if fila and fila["codigo_barras"] not in vistos:
                vistos.add(fila["codigo_barras"])
                filas.append(fila)
                nuevos += 1
                if len(filas) >= args.cuantos:
                    break
        print(f"  página {pagina}: +{nuevos} (total {len(filas)})")
        pagina += 1
        time.sleep(ESPERA_SEG)

    if not filas:
        print("No se descargó nada. ¿Hay internet? Intenta de nuevo en unos minutos.")
        sys.exit(1)

    for a in escribir(filas, args.salida):
        print(f"Listo: {a}")
    otros = sum(1 for f in filas if f["subcategoria"] == "Otros")
    print(f"\n{len(filas)} productos. {otros} quedaron en 'Abarrotes > Otros' (sin categoría clara).")
    print("Súbelos en Camelot: Mi tienda → Catálogo y precios → Importar (Excel / CSV).")
    print("Recuerda: los precios son de referencia; ajústalos.")


if __name__ == "__main__":
    main()
