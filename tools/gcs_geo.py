"""Imports Downloads/GCS.* for the mod. Rerun it after every Blockbench export.

The desk is exported 2 blocks wide from x=-8 to x=24 and kept there: in game +x is the placer's right, so the
placed block (centred on x=0) is its left half and the second half lies toward +x, see GcsBlock.otherHalf. The screen bones get
a move_ prefix: SBW's block baker folds every other bone, and GcsBlockEntityRenderer hides them by name."""
import json, shutil

SRC = 'C:/Users/Administrator/Downloads/'
RES = 'src/main/resources/assets/ultimaratio/'
SHIFT = 0

geo = json.load(open(SRC + 'GCS.geo.json'))
for bone in geo['minecraft:geometry'][0]['bones']:
    if bone['name'].startswith('scr_'):
        bone['name'] = 'move_' + bone['name']
    if bone.get('parent', '').startswith('scr_'):
        bone['parent'] = 'move_' + bone['parent']
for bone in geo['minecraft:geometry'][0]['bones']:
    bone['pivot'][0] -= SHIFT
    for cube in bone.get('cubes', []):
        cube['origin'][0] -= SHIFT
        if 'pivot' in cube:
            cube['pivot'][0] -= SHIFT

json.dump(geo, open(RES + 'models/bedrock/block/gcs.geo.json', 'w'), indent=2)
shutil.copy(SRC + 'GCS.png', RES + 'textures/bedrock/block/gcs.png')
print(len(geo['minecraft:geometry'][0]['bones']), 'bones imported')
