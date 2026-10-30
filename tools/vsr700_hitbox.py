"""Writes the VSR700 OBBs from the hitbox* bones of Downloads/vsr700.geo.json (geo px -> vehicle metres:
(x/16, y/16, -z/16), Size = half extents). The box on the rotor head is the MainEngine part."""
import json

SRC = 'C:/Users/Administrator/Downloads/vsr700.geo.json'
VEHICLE = 'src/main/resources/data/ultimaratio/sbw/vehicles/vsr700.json'
ENGINE_ORIGIN = [-4, 34, -21]  # rotor head cube

geo = json.load(open(SRC))
obbs = []
for bone in geo['minecraft:geometry'][0]['bones']:
    if not bone['name'].startswith('hitbox'):
        continue
    for cube in bone.get('cubes', []):
        o, s = cube['origin'], cube['size']
        obb = {
            'Size': [round(s[0] / 32, 4), round(s[1] / 32, 4), round(s[2] / 32, 4)],
            'Position': [round((o[0] + s[0] / 2) / 16, 4), round((o[1] + s[1] / 2) / 16, 4), round(-(o[2] + s[2] / 2) / 16, 4)],
        }
        if o == ENGINE_ORIGIN:
            obb['Part'] = 'MainEngine'
        obbs.append(obb)

vehicle = json.load(open(VEHICLE, encoding='utf-8'))
vehicle['OBB'] = obbs
json.dump(vehicle, open(VEHICLE, 'w', encoding='utf-8'), indent=2)
print(len(obbs), 'OBBs,', sum(1 for o in obbs if 'Part' in o), 'MainEngine')
