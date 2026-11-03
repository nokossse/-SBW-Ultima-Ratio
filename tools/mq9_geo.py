"""Imports Downloads/mq-9-reaper.* for the mod. Rerun it after every Blockbench export.

The dummies_* bones become SBW's dummy_* ones (weapon index shifted by one), the pusher propeller is renamed move_propeller so SBW's model baker keeps it (Mq9ReaperRenderer spins it) and the
bone name with a space is fixed. Everything else is copied as exported: the gear clips are played by name from
Mq9ReaperEntity, the other bones are folded into BODY by the baker."""
import json, re, shutil

SRC = 'C:/Users/Administrator/Downloads/'
RES = 'src/main/resources/assets/ultimaratio/'
RENAME = {'propellerBase': 'move_propeller', 'kamera': 'move_kamera', 'cam': 'move_cam', 'LIGHRS 1': 'LIGHRS_1'}

def rename(name):
    # dummies_<seat>_<weapon, 1-based>_<n> -> SBW's dummy_<seat>_<weapon, 0-based>_<n> (missile shown under the wing)
    m = re.fullmatch(r'dummies_(\d+)_(\d+)_(\d+)', name)
    return 'dummy_%s_%d_%s' % (m[1], int(m[2]) - 1, m[3]) if m else RENAME.get(name, name)

geo = json.load(open(SRC + 'mq-9-reaper.geo.json'))
for bone in geo['minecraft:geometry'][0]['bones']:
    bone['name'] = rename(bone['name'])
    if 'parent' in bone:
        bone['parent'] = rename(bone['parent'])
# The "ARMEE DE L'AIR" plates are 0.025 px thick, flush with the fuselage: in game the depth buffer cannot tell them
# from it and the text vanishes. Per-face UVs make thickening safe: 0.4 px each side, so they stand 0.4 px proud.
for bone in geo['minecraft:geometry'][0]['bones']:
    for cube in bone.get('cubes', []):
        if cube['size'][0] < 0.1 and cube['size'][1] > 1:
            cube['origin'][0] -= 0.4
            cube['size'][0] += 0.8

json.dump(geo, open(RES + 'models/bedrock/vehicle/mq9_reaper.geo.json', 'w'), indent=2)

shutil.copy(SRC + 'mq-9-reaper.animation.json', RES + 'animations/bedrock/vehicle/mq9_reaper.animation.json')
shutil.copy(SRC + 'MQ-9-france.png', RES + 'textures/bedrock/vehicle/mq9_reaper.png')
print(len(geo['minecraft:geometry'][0]['bones']), 'bones imported')
