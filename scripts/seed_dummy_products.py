#!/usr/bin/env python3
"""Generate a repeatable, additive local catalog seed. Does not connect to the database."""
from pathlib import Path
from uuid import UUID, uuid5
from html import escape
import re
import argparse

ROOT = Path(__file__).resolve().parents[1]
NAMESPACE = UUID('7ca044ae-f38b-44fa-84ec-995d60e325d1')
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--schema', default='kiditz_outdoor')
parser.add_argument('--media-base-url', default='http://localhost:8080/media')
parser.add_argument('--media-dir', type=Path)
parser.add_argument('--output', type=Path, default=ROOT/'scripts/seed_dummy_products.sql')
args = parser.parse_args()
if not re.fullmatch(r'[a-z_][a-z0-9_]*', args.schema):
 parser.error('--schema must be a lowercase PostgreSQL identifier')
SCHEMA = args.schema
GROUPS = [
 ('Tenda', 'tenda', 50000, ['Dome 2 Orang','Dome 4 Orang','Family 6 Orang','Ultralight 1 Orang','Double Layer 3 Orang']),
 ('Carrier', 'carrier', 35000, ['Hiking 40 Liter','Trekking 60 Liter','Expedition 80 Liter','Daypack 25 Liter','Women Fit 50 Liter']),
 ('Sleeping Bag', 'sleeping-bag', 15000, ['Polar Hangat','Mummy Ultralight','Double Camping','Bulu Sintetis','Compact Travel']),
 ('Matras', 'matras', 10000, ['Foam Lipat','Inflatable Single','Aluminium Thermal','Self Inflating','Double Comfort']),
 ('Kompor', 'kompor', 20000, ['Portable Mini','Windproof Camping','Double Burner','Ultralight Titanium','Paket Cooking Set']),
 ('Peralatan Masak', 'peralatan-masak', 15000, ['Nesting 2 Orang','Nesting 4 Orang','Panci Camping','Teko Kopi Outdoor','Grill Portable']),
 ('Lampu', 'lampu', 10000, ['Headlamp LED','Lentera Rechargeable','String Light Camping','Senter Waterproof','Lentera Solar']),
 ('Furniture Camping', 'furniture-camping', 20000, ['Kursi Lipat','Meja Aluminium','Kursi Recliner','Bangku Portable','Meja Picnic']),
 ('Aksesori Hiking', 'aksesori-hiking', 15000, ['Trekking Pole Sepasang','Gaiter Waterproof','Raincoat Poncho','Dry Bag 20 Liter','Hammock Parasut']),
 ('Paket Camping', 'paket-camping', 100000, ['Solo Explorer','Duo Weekend','Family Adventure','Group Summit','Glamping Comfort']),
]
ART = {
 'tenda':'<path d="M80 260 240 90 400 260Z" fill="#e5a54c"/><path d="M160 260 240 150 320 260Z" fill="#294c43"/><path d="M240 90v170" stroke="#fff" stroke-width="5"/>',
 'carrier':'<rect x="160" y="105" width="160" height="170" rx="35" fill="#51836c"/><rect x="185" y="185" width="110" height="65" rx="15" fill="#355b4a"/><path d="M200 110V90h80v20M155 140h-20v95h25M325 140h20v95h-25" fill="none" stroke="#294c43" stroke-width="12"/>',
 'sleeping-bag':'<path d="M180 110Q240 65 300 110v110q0 70-60 70t-60-70Z" fill="#d98b59"/><ellipse cx="240" cy="120" rx="40" ry="23" fill="#f9e4c5"/><path d="M278 155v95" stroke="#6d4537" stroke-width="5"/>',
 'matras':'<rect x="120" y="125" width="240" height="140" rx="22" fill="#6b9879"/><path d="M150 140v110m35-110v110m35-110v110m35-110v110m35-110v110m35-110v110" stroke="#c9dfc6" stroke-width="5"/>',
 'kompor':'<rect x="180" y="200" width="120" height="70" rx="14" fill="#668a76"/><path d="M240 100q-60 65 0 85 60-25 0-85Z" fill="#efab54"/><path d="M160 200h160m-110 0-30-25m90 25 30-25" stroke="#314f43" stroke-width="10"/><circle cx="240" cy="235" r="14" fill="#dcebd8"/>',
 'peralatan-masak':'<path d="M150 170h180v65q0 35-35 35H185q-35 0-35-35Z" fill="#73918a"/><path d="M140 170h200m-150-15h100m-55 0v-20h20v20M150 195h-25v35h25m180-35h25v35h-25" fill="none" stroke="#3d5c51" stroke-width="10"/>',
 'lampu':'<path d="M205 110q35-55 70 0" fill="none" stroke="#456858" stroke-width="10"/><rect x="180" y="125" width="120" height="130" rx="20" fill="#f0b65f"/><path d="M175 125h130m-130 135h130m-95-115v95m60-95v95" stroke="#456858" stroke-width="12"/>',
 'furniture-camping':'<path d="M160 120h160l-15 95H175Z" fill="#b77b50"/><path d="m175 215 130 60m0-60-130 60M175 215h130" fill="none" stroke="#496258" stroke-width="12"/>',
 'aksesori-hiking':'<path d="m185 100 85 170m25-170-85 170" stroke="#4d7663" stroke-width="10"/><path d="m175 92 20-10m90 0 20 10m-50 165 25-10m-80 0 25 10" stroke="#d49650" stroke-width="16"/>',
 'paket-camping':'<path d="M90 260 220 105 350 260Z" fill="#d6a24f"/><path d="m160 260 60-95 60 95Z" fill="#426c55"/><rect x="325" y="150" width="70" height="110" rx="16" fill="#648f77"/><circle cx="130" cy="115" r="25" fill="#e9c76f"/>',
}

def ident(key): return str(uuid5(NAMESPACE, key))
def sql(value): return "'" + str(value).replace("'", "''") + "'"
def insert(table, columns, values):
 return f'INSERT INTO {table} ({columns}) VALUES ({values}) ON CONFLICT DO NOTHING;'

statements = [f'-- Local dummy data for {SCHEMA}; additive and repeatable.',
 'BEGIN;', f'SET LOCAL search_path TO {SCHEMA}, public;',
 f"SELECT pg_advisory_xact_lock(hashtext('sewain:dummy-products:{SCHEMA}'));",
 insert('tags', 'id,name,slug', ','.join(map(sql,[ident('tag'), 'Data Dummy', 'dummy-data-outdoor'])))]
images = args.media_dir or ROOT / 'backend/upload/media' / SCHEMA / 'dummy-products'
images.mkdir(parents=True, exist_ok=True)
for category_index,(category,key,fee,models) in enumerate(GROUPS):
 type_id=ident('type:'+key)
 statements.append(insert('product_types','id,name,slug',','.join(map(sql,[type_id,category,'dummy-'+key]))))
 # Slug lookup respects existing categories/tags when this seed is run again.
 type_ref=f'(SELECT id FROM product_types WHERE slug={sql("dummy-"+key)})'
 for model_index,model in enumerate(models):
  number=category_index*5+model_index+1
  name=f'{category} {model}'
  slug='dummy-'+re.sub('[^a-z0-9]+','-',name.lower()).strip('-')
  product_id,variant_id=ident('product:'+slug),ident('variant:'+slug)
  price=fee+model_index*5000
  url=f'{args.media_base_url.rstrip("/")}/{SCHEMA}/dummy-products/{slug}.svg'
  image=f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 480 400"><rect width="480" height="400" fill="#f1f0e8"/><circle cx="240" cy="185" r="130" fill="#e2e7d8"/>{ART[key]}<text x="240" y="325" text-anchor="middle" font-family="sans-serif" font-size="20" fill="#294c43">{escape(category)}</text><text x="240" y="355" text-anchor="middle" font-family="sans-serif" font-size="14" fill="#607364">{escape(model)}</text><text x="455" y="30" text-anchor="end" font-family="sans-serif" font-size="11" fill="#607364">DEMO {number:02}</text></svg>'''
  (images/(slug+'.svg')).write_text(image+'\n')
  description=f'<p>{name}, perlengkapan untuk aktivitas outdoor dan camping. Paket sewa tersedia untuk 1 hari, 3 hari, dan 7 hari.</p><p>Data dummy untuk pengembangan storefront. Gambar merupakan ilustrasi, bukan foto produk.</p>'
  statements.append(insert('products','id,name,slug,media,description,status,product_type_id',','.join(map(sql,[product_id,name,slug,url,description,'ACTIVE']))+','+type_ref))
  statements.append(insert('product_tags','product_id,tag_id',sql(product_id)+",(SELECT id FROM tags WHERE slug='dummy-data-outdoor')"))
  statements.append(insert('product_variants','id,product_id,name,sku,media_list,buy_price,deposit_amount,is_active',','.join(map(sql,[variant_id,product_id,'Standar',f'DUMMY-OUT-{number:03}']))+f',ARRAY[{sql(url)}]::text[],{price*30},{price*2},TRUE'))
  for days,multiplier in [(1,1),(3,2.5),(7,5)]:
   statements.append(insert('variant_rental_plans','id,variant_id,duration_unit,duration_value,rental_fee,is_active',','.join(map(sql,[ident(f'plan:{slug}:{days}'),variant_id,'DAY']))+f',{days},{int(price*multiplier)},TRUE'))
  for unit in range(1,4+model_index):
   barcode=f'DUMMY-OUT-{number:03}-{unit:02}'
   statements.append(insert('inventory_units','id,variant_id,sku_barcode,serial_number,status,condition_notes',','.join(map(sql,[ident('unit:'+barcode),variant_id,barcode,'SN-'+barcode,'AVAILABLE','Data dummy; unit siap disewa untuk demo.']))))
statements.append('COMMIT;')
output=args.output
output.write_text('\n'.join(statements)+'\n')
print(f'Generated {output}: 50 products, 50 variants, 150 rental plans, 250 inventory units, 50 SVG illustrations.')
