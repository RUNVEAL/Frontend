import sys, numpy as np
from PIL import Image
from figma_fx import render_on_bg
ref = Image.open('/root/.claude/uploads/626969e2-90a6-535a-8f45-bc958c7b4107/4d9fd547-image.png').convert('RGB')
H0 = [(42,153),(47,122),(88,108),(129,119),(141,158),(110,190),(62,184)]
cx = 91.5
scale = float(sys.argv[1]) if len(sys.argv)>1 else 1.0
fa = len(sys.argv)>2 and sys.argv[2]=='fa'
y0,y1 = 70, 230
W = 1180
canvas = np.ones((y1-y0, W, 3), np.float32)*14/255
for lvl in range(7):
    dx = 145.2*lvl
    poly = [((x+dx), y-y0) for x,y in H0]
    # render in local window
    x0 = int(cx+dx-72); 
    loc = [(x-x0, y) for x,y in poly]
    img = render_on_bg([(loc, [])], 144, y1-y0, lvl, scale=scale, inner_uses_fill_alpha=fa)
    canvas[:, x0:x0+144] = img
out = Image.fromarray((canvas*255).astype(np.uint8))
refc = ref.crop((0,y0,W,y1))
stack = Image.new('RGB', (W, (y1-y0)*2+6), (60,0,0))
stack.paste(refc,(0,0)); stack.paste(out,(0,y1-y0+6))
stack.save(f'calib_{scale}_{"fa" if fa else "geo"}.png')
