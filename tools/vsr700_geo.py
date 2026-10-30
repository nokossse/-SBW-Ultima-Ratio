"""Imports Downloads/vsr700.geo.json for the mod: drops the Steve reference, renames the rotors, and gives
every poly_mesh face a flat outward normal, like the Tigre's meshes (one normal per face, no smoothing).

The exported normals point inward on about half the faces and some faces are wound backwards. Per connected
piece of mesh: the winding is made consistent across shared edges, then the piece is oriented outward
(closed piece: by signed volume; open piece: by a ray casting vote). Usage: vsr700_geo.py [UP_WEIGHT=0]"""
import json, math, sys
from collections import defaultdict, deque

SRC = 'C:/Users/Administrator/Downloads/vsr700.geo.json'
DST = 'src/main/resources/assets/ultimaratio/models/bedrock/vehicle/vsr700.geo.json'
UP_WEIGHT = float(sys.argv[1]) if len(sys.argv) > 1 else 0.0

def sub(a, b): return [a[i] - b[i] for i in range(3)]
def cross(a, b): return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
def dot(a, b): return sum(x * y for x, y in zip(a, b))
def unit(v):
    l = math.sqrt(sum(x * x for x in v)) or 1
    return [x / l for x in v]
def key(p): return tuple(round(x, 3) for x in p)

def newell(pts):
    n = [0, 0, 0]
    for i in range(len(pts)):
        a, c = pts[i], pts[(i + 1) % len(pts)]
        n[0] += (a[1] - c[1]) * (a[2] + c[2])
        n[1] += (a[2] - c[2]) * (a[0] + c[0])
        n[2] += (a[0] - c[0]) * (a[1] + c[1])
    return n

def ray_hits_tri(o, d, a, b, c):  # Moeller-Trumbore, forward hits only
    e1, e2 = sub(b, a), sub(c, a)
    h = cross(d, e2)
    det = dot(e1, h)
    if abs(det) < 1e-9:
        return False
    f = 1 / det
    s = sub(o, a)
    u = f * dot(s, h)
    if u < 0 or u > 1:
        return False
    q = cross(s, e1)
    v = f * dot(d, q)
    return v >= 0 and u + v <= 1 and f * dot(e2, q) > 1e-4

def orient(P, polys):
    """Returns one outward unit normal per poly."""
    faces = []
    for poly in polys:
        idx = []
        for v in poly:
            if v[0] not in idx:
                idx.append(v[0])
        faces.append(idx)
    keys = [[key(P[i]) for i in idx] for idx in faces]
    edges = defaultdict(list)  # (a, b) -> faces traversing a->b
    for fi, ks in enumerate(keys):
        for i in range(len(ks)):
            edges[(ks[i], ks[(i + 1) % len(ks)])].append(fi)
    flip = [0] * len(faces)  # 0 unvisited, +1 keep winding, -1 reverse it
    comps = []
    for start in range(len(faces)):
        if flip[start]:
            continue
        flip[start] = 1
        comp, queue = [], deque([start])
        while queue:
            f = queue.popleft()
            comp.append(f)
            ks = keys[f]
            for i in range(len(ks)):
                a, b = ks[i], ks[(i + 1) % len(ks)]
                if flip[f] < 0:
                    a, b = b, a  # effective direction of f on this edge
                for g, same in ([(g, True) for g in edges.get((a, b), []) if g != f] +
                                [(g, False) for g in edges.get((b, a), []) if g != f]):
                    if len(edges.get((a, b), [])) + len(edges.get((b, a), [])) != 2:
                        continue  # non-manifold edge: do not propagate across it
                    want = -1 if same else 1  # a neighbour traversing the same way must be reversed
                    if not flip[g]:
                        flip[g] = want
                        queue.append(g)
        comps.append(comp)
    normals = [None] * len(faces)
    tris = [(P[idx[0]], P[idx[i]], P[idx[i + 1]]) for idx in faces for i in range(1, len(idx) - 1)]
    for comp in comps:
        n_stored = {f: unit(newell([P[i] for i in faces[f]])) for f in comp}
        vol = 0
        for f in comp:
            idx = faces[f]
            for i in range(1, len(idx) - 1):
                vol += flip[f] * dot(P[idx[0]], cross(P[idx[i]], P[idx[i + 1]])) / 6
        used = defaultdict(int)
        for f in comp:
            for i in range(len(keys[f])):
                used[frozenset((keys[f][i], keys[f][(i + 1) % len(keys[f])]))] += 1
        closed = all(c == 2 for c in used.values())
        if closed:
            t = 1 if vol > 0 else -1
        elif len(comp) <= 2:
            t = -1  # lone planes: the exporter's convention (front face = opposite of the right-hand normal)
        else:
            vote = 0
            for f in comp:
                n = [x * flip[f] for x in n_stored[f]]
                centre = [sum(P[i][k] for i in faces[f]) / len(faces[f]) for k in range(3)]
                origin = [centre[k] + n[k] * 0.01 for k in range(3)]
                outward = sum(1 for tr in tris if ray_hits_tri(origin, n, *tr)) % 2 == 0
                vote += 1 if outward else -1
            t = 1 if vote >= 0 else -1
        for f in comp:
            normals[f] = [x * flip[f] * t for x in n_stored[f]]
        print('  piece of %3d faces: %s, reversed %d' % (len(comp), 'closed' if closed else 'open', sum(1 for f in comp if flip[f] * t < 0)))
    return normals

d = json.load(open(SRC))
g = d['minecraft:geometry'][0]
g['description']['identifier'] = 'geometry.vsr700'
drop = {'STEVE', 'Left Leg', 'Right Leg', 'Waist', 'Head', 'Body', 'Right Arm', 'Left Arm', 'hitbox', 'hitbox2', 'hitbox3'}  # hitboxes live in the vehicle JSON
g['bones'] = [b for b in g['bones'] if b['name'] not in drop]
ren = {'move_tailPropeller': 'move_propeller', 'propellerBase': 'move_tailPropeller', 'radar': 'move_radar'}
for b in g['bones']:
    b['name'] = ren.get(b['name'], b['name'])
    if b.get('parent') in ren:
        b['parent'] = ren[b['parent']]

for b in g['bones']:
    m = b.get('poly_mesh')
    if not m:
        continue
    print(b['name'])
    out = orient(m['positions'], m['polys'])
    new = []
    for poly, n in zip(m['polys'], out):
        n = unit([n[0] * (1 - UP_WEIGHT), n[1] * (1 - UP_WEIGHT) + UP_WEIGHT, n[2] * (1 - UP_WEIGHT)])
        new.append([round(x, 5) for x in n])
        for v in poly:
            v[1] = len(new) - 1
    m['normals'] = new

json.dump(d, open(DST, 'w'), indent=1)
print('wrote', DST, 'up weight', UP_WEIGHT)
