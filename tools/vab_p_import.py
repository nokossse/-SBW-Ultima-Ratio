"""Imports Downloads/vab-p.* for the mod and writes the VAB P vehicle JSONs. Rerun it after every Blockbench export.

Fixes the export for SBW: barrrel -> barrel, cannon-recoil -> cannon (animated by the fire clip), the right-hand wheel names
(the export has R0Turn at the rear and R1 at the front, SBW steers the "0" ones) and drops the reference `hitbox` bone
(transcribed into the OBBs below, see memory sbw-model-baker-folding). Run from -SBW-Ultima-Ratio/."""
import copy, json, shutil

SCALE = 1.15  # the model was scaled up by this much in Blockbench; the hand-tuned numbers below follow it
FRONT_SEAT_DROP = 0.35  # driver and gunner sit this much lower than the seat bone bottom + 0.15
SRC = 'C:/Users/Administrator/Downloads/'
RES = 'src/main/resources/'
ASSETS = RES + 'assets/ultimaratio/'
DATA = RES + 'data/ultimaratio/'
RENAME = {'barrrel': 'barrel', 'cannon-recoil': 'cannon', 'wheelR0Turn': 'wheelR1', 'wheelR1': 'wheelR0Turn',
          'hubR0': 'hubR1', 'hubR1': 'hubR0'}

def rename(name):
    return RENAME.get(name, name)

def r(v):
    return round(v, 4)

geo = json.load(open(SRC + 'vab-p.geo.json'))
g = geo['minecraft:geometry'][0]
hits = {b['name']: b['cubes'] for b in g['bones'] if b['name'].startswith('hitbox')}
g['bones'] = [b for b in g['bones'] if not b['name'].startswith('hitbox')]
for bone in g['bones']:
    bone['name'] = rename(bone['name'])
    if 'parent' in bone:
        bone['parent'] = rename(bone['parent'])
json.dump(geo, open(ASSETS + 'models/bedrock/vehicle/vab_p.geo.json', 'w'), indent=2)
shutil.copy(SRC + 'vab-p.png', ASSETS + 'textures/bedrock/vehicle/vab_p.png')

# MG recoil and muzzle flash, same clips as the Serval/Griffon (the baker keeps `cannon` and `flare` because they are animated).
anim = json.load(open(ASSETS + 'animations/bedrock/vehicle/serval.animation.json'))
anim['animations'] = {k: v for k, v in anim['animations'].items() if k.startswith('animation.machine_gun') or k == 'animation.rams'}
json.dump(anim, open(ASSETS + 'animations/bedrock/vehicle/vab_p.animation.json', 'w'), indent=2)

json.dump({
    'Animation': 'ultimaratio:animations/bedrock/vehicle/vab_p.animation.json',
    'Models': [{'Model': 'ultimaratio:models/bedrock/vehicle/vab_p.geo.json',
                'Texture': 'ultimaratio:textures/bedrock/vehicle/vab_p.png'}],
}, open(ASSETS + 'sbw/vehicles/vab_p.json', 'w'), indent=2)

# geo px -> vehicle blocks: (x/16, y/16, -z/16)
bones = {b['name']: b for b in g['bones']}
def blocks(p):
    return [r(p[0] / 16), r(p[1] / 16), r(-p[2] / 16)]
def sub(a, b):
    return [a[i] - b[i] for i in range(3)]
turret = bones['turret']['pivot']
barrel = bones['barrel']['pivot']
def box(cubes_of, cs=None):
    cs = cs or [c for n in cubes_of for c in bones[n]['cubes']]
    lo = [min(c['origin'][i] for c in cs) for i in range(3)]
    hi = [max(c['origin'][i] + c['size'][i] for c in cs) for i in range(3)]
    return [(lo[i] + hi[i]) / 2 for i in range(3)], [(hi[i] - lo[i]) / 32 for i in range(3)]

serval = json.load(open(DATA + 'sbw/vehicles/serval.json'))
dsa = json.load(open(DATA + 'sbw/vehicles/serval_dsa.json'))
d = copy.deepcopy(serval)
d['MaxHealth'] = 200
d['TurretPos'] = blocks(turret)
d['BarrelPos'] = [r(v) for v in (lambda s: [s[0] / 16, s[1] / 16, -s[2] / 16])(sub(barrel, turret))]
d['TurretTurnSpeed'] = [12, 12]
d['TurretPitchRange'] = [-10, 60]
d['Mass'] = 13
d['EngineInfo']['Buoyancy'] = 0.045  # amphibious; LAV-150 is 0.052, lower = sits deeper

# 0 driver (left), 1 turret gunner (right, under the turret), then 2 hidden bench seats per side (passengers 3-6).
# Seat position = centre of the seat bone, y = its bottom + 0.15 (same rule as the Serval).
def seat_pos(name):
    c, h = box([name])
    return [r(c[0] / 16), r(c[1] / 16 - h[1] + 0.15 - FRONT_SEAT_DROP), r(-c[2] / 16)]
S = SCALE
d['Seats'] = []
for name, side in (('seat1', 1), ('seat2', -1)):
    pos = seat_pos(name)
    seat = {'Transform': 'Vehicle', 'Position': pos, 'IsEnclosed': True, 'RotateWithVehicle': True,
            'MinPitch': -60, 'MaxPitch': 60, 'MinYaw': -95, 'MaxYaw': 95,
            'DismountInfo': {'Transform': 'Vehicle', 'Position': [r(side * 2.2), 0.3, pos[2]]}}
    if side < 0:
        seat.update({'HidePassenger': False, 'MinPitch': -10, 'MaxPitch': 60, 'Sensitivity': [0.55, 0.7, 1],
                     'Weapons': ['MachineGun'],
                     'CameraPos': {'Transform': 'Barrel', 'UseFixedCameraPos': True, 'Position': [0.0, r(0.32 * S), r(0.2 * S)],
                                   'ZoomPosition': [0.0, r(0.2 * S), r(0.7 * S)], 'Direction': 'Barrel'}})
    d['Seats'].append(seat)
for side in (1, -1):
    for z in (0.0, -0.69):
        d['Seats'].append({
            'HidePassenger': True, 'Transform': 'Vehicle', 'Position': [r(side * 0.85 * S), r(1.15 * S), r(z * S)], 'IsEnclosed': True,
            'RotateWithVehicle': True, 'Orientation': 90 * side, 'MinPitch': -45, 'MaxPitch': 45,
            'MinYaw': -50, 'MaxYaw': 50,
            'DismountInfo': {'Transform': 'Vehicle', 'Position': [r(side * 0.5 * S), 0.3, r(-3.7 * S)]}})

mg = copy.deepcopy(dsa['Weapons']['MachineGun'])
flare = bones['flare']['pivot']
mg['ShootPos']['Positions'] = [[r(v) for v in (lambda s: [s[0] / 16, s[1] / 16, -s[2] / 16])(sub(flare, barrel))]]
d['Weapons'] = {'MachineGun': mg}

tc, th = box(['turret'])
bc, bh = box(['gun', 'cannon'])
def rel(c, origin):
    return [r(v) for v in (lambda s: [s[0] / 16, s[1] / 16, -s[2] / 16])(sub(c, origin))]
d['OBB'] = []
# hitbox = hull Collision; hitbox2 cubes = extra hit zones, the last one (front, x 40 wide) is the engine bay.
for k, cube in enumerate(hits['hitbox'] + hits.get('hitbox2', [])):
    c, h = box(None, [cube])
    obb = {'Size': [r(v) for v in h], 'Position': blocks(c)}
    if k == 0:
        obb = {'Part': 'Collision', **obb}
    d['OBB'].append(obb)
if 'hitbox2' in hits:
    d['OBB'][-1] = {'Part': 'MainEngine', **d['OBB'][-1]}
d['OBB'] += [
    {'Part': 'Turret', 'Transform': 'Turret', 'Rotation': 'Turret', 'Size': [r(v) for v in th], 'Position': rel(tc, turret)},
    {'Transform': 'Barrel', 'Rotation': 'Barrel', 'Size': [r(v) for v in bh], 'Position': rel(bc, barrel)},
]
wheels = {n: bones[n]['pivot'] for n in ('wheelL0Turn', 'wheelL1', 'wheelR0Turn', 'wheelR1')}
for n, p in wheels.items():
    pos = blocks(p)
    d['OBB'].append({'Part': 'WheelLeft' if n[5] == 'L' else 'WheelRight', 'Size': [r(0.22 * SCALE), r(0.59 * SCALE), r(0.59 * SCALE)], 'Position': pos})
d['TerrainCompat'] = [[b[0], 0.0, b[2]] for b in (blocks(p) for p in wheels.values())]
d['ThirdPersonCameraPos'] = [0.0, r(1.7 * SCALE), r(5.0 * SCALE)]
json.dump(d, open(DATA + 'sbw/vehicles/vab_p.json', 'w'), indent=2)

recipe = json.load(open(DATA + 'recipes/serval.json'))
recipe['inputs'] = ['8 #forge:storage_blocks/steel', 'superbwarfare:light_armament_module', 'superbwarfare:medium_battery_pack',
                    '4 superbwarfare:wheel', 'superbwarfare:large_motor', '12 superbwarfare:rifle_ammo']
recipe['result']['entity'] = 'ultimaratio:vab_p'
json.dump(recipe, open(DATA + 'recipes/vab_p.json', 'w'), indent=2)
print(len(g['bones']), 'bones;', len(d['Seats']), 'seats')
