"""Render a fresh code-drawn green icon set for the Swing forms (no old pixels reused)."""
from pathlib import Path
from PIL import Image, ImageDraw
import math

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT/'src/resources/images/ui'
DEST.mkdir(parents=True,exist_ok=True)


def draw_icon(kind, size, color='#145A32'):
    w,h=size
    im=Image.new('RGBA',(w*3,h*3))
    d=ImageDraw.Draw(im)
    unit=min(w,h)*3/24
    ox=(w*3-24*unit)/2;oy=(h*3-24*unit)/2
    def points(pts):return [(round(ox+x*unit),round(oy+y*unit)) for x,y in pts]
    def box(coords):
        x1,y1,x2,y2=coords
        return (round(ox+x1*unit),round(oy+y1*unit),round(ox+x2*unit),round(oy+y2*unit))
    stroke=max(2,round(1.6*unit))
    def line(pts,fill=color,width=stroke):d.line(points(pts),fill=fill,width=width,joint='curve')
    def rect(b,fill=None,outline=color,radius=2):d.rounded_rectangle(box(b),radius=round(radius*unit),fill=fill,outline=outline,width=stroke)
    def circle(b,fill=None,outline=color):d.ellipse(box(b),fill=fill,outline=outline,width=stroke)
    if kind in ('home',):
        line([(3,11),(12,3),(21,11)]);line([(6,9),(6,21),(18,21),(18,9)]);rect((10,14,14,21),radius=0)
    elif kind in ('orders','basket','box','clipboard','order'):
        rect((4,7,20,22));d.arc(box((8,2,16,12)),180,360,fill=color,width=stroke)
        line([(8,13),(16,13)]);line([(8,17),(13,17)])
    elif kind in ('track',):
        circle((4,2,20,18));circle((9,7,15,13));line([(6,15),(12,22),(18,15)])
    elif kind in ('history',):
        d.arc(box((4,4,21,21)),35,330,fill=color,width=stroke);line([(3,3),(3,9),(9,9)]);line([(12,7),(12,13),(16,15)])
    elif kind in ('services','clothes','shirt','clothes_small'):
        line([(8,4),(4,6),(1,11),(5,14),(7,11),(7,22),(17,22),(17,11),(19,14),(23,11),(20,6),(16,4)])
        d.arc(box((8,1,16,9)),0,180,fill=color,width=stroke)
    elif kind in ('hanger','logo','logo_small','logo_white'):
        d.arc(box((9,2,15,10)),175,385,fill=color,width=stroke)
        line([(12,9),(2,17),(2,19),(22,19),(22,17),(12,10)])
        if kind.startswith('logo'):
            d.ellipse(box((16,3,23,7)),fill=color)
    elif kind in ('profile','profile_big','user','user_small','user_header'):
        circle((8,2,16,10));d.arc(box((3,12,21,29)),180,360,fill=color,width=stroke)
    elif kind in ('mail',):
        rect((2,5,22,19));line([(3,6),(12,13),(21,6)])
    elif kind in ('phone',):
        line([(5,2),(2,5),(4,12),(12,20),(19,22),(22,19),(18,15),(15,17),(8,10),(10,7),(5,2)])
    elif kind in ('lock',):
        rect((5,10,19,22));d.arc(box((7,1,17,17)),180,360,fill=color,width=stroke);circle((11,14,13,16),fill=color);line([(12,16),(12,18)])
    elif kind in ('eye',):
        line([(1,12),(6,7),(12,5),(18,7),(23,12),(18,17),(12,19),(6,17),(1,12)]);circle((9,9,15,15))
    elif kind in ('search',):
        circle((3,3,16,16));line([(15,15),(22,22)])
    elif kind in ('report',):
        line([(3,3),(3,21),(22,21)]);rect((7,13,10,19),fill=color,radius=0);rect((13,8,16,19),fill=color,radius=0);rect((19,4,22,19),fill=color,radius=0)
    elif kind in ('receipt','receipt_small'):
        line([(5,2),(19,2),(19,22),(16,20),(13,22),(10,20),(7,22),(5,20),(5,2)])
        line([(8,7),(16,7)]);line([(8,11),(16,11)]);line([(8,15),(13,15)])
    elif kind in ('payment','cash'):
        rect((2,5,22,19));circle((9,8,15,16));line([(5,9),(6,9)]);line([(18,15),(19,15)])
    elif kind in ('bank',):
        line([(2,8),(12,2),(22,8),(2,8)]);line([(2,22),(22,22)])
        for x in (5,12,19):line([(x,10),(x,19)])
    elif kind in ('gcash',):
        circle((2,2,22,22));d.arc(box((7,6,17,18)),50,320,fill=color,width=stroke);line([(12,12),(18,12),(18,16)])
    elif kind in ('scale',):
        rect((3,3,21,22));d.arc(box((7,5,17,15)),180,360,fill=color,width=stroke);line([(12,10),(15,7)])
    elif kind in ('iron',):
        line([(3,17),(8,9),(21,9),(22,20),(2,20),(3,17)]);line([(10,8),(11,4),(18,4),(20,8)])
    elif kind in ('logout',):
        line([(10,3),(3,3),(3,21),(10,21)]);line([(9,12),(22,12)]);line([(17,7),(22,12),(17,17)])
    elif kind in ('step_done','check_on'):
        circle((1,1,23,23),fill='#1E8E3E',outline='#1E8E3E');line([(6,12),(10,16),(18,7)],fill='#FFFFFF')
    elif kind in ('step_current','radio_on'):
        circle((1,1,23,23),outline='#1E8E3E');circle((5,5,19,19),fill='#1E8E3E',outline='#1E8E3E')
    elif kind in ('step_pending','radio_off','check_off'):
        circle((2,2,22,22),outline='#A7B6AA')
    else:
        circle((2,2,22,22));line([(12,10),(12,17)]);circle((11,6,13,8),fill=color)
    return im.resize((w,h),Image.Resampling.LANCZOS)


SIZES = {
    'bank':40, 'basket':56, 'box':24, 'cash':40, 'check_off':30, 'check_on':30,
    'clipboard':56, 'clothes':52, 'clothes_small':44, 'eye':20, 'gcash':40,
    'hanger':52, 'home':22, 'home_active':22, 'info':22, 'iron':52, 'lock':20,
    'logo':64, 'logout':22, 'logout_active':22, 'logo_small':30, 'mail':20,
    'orders':22, 'orders_active':22, 'phone':20, 'profile':22, 'profile_active':22,
    'profile_big':56, 'radio_off':24, 'radio_on':24, 'receipt':24, 'receipt_small':18,
    'report':22, 'report_active':22, 'scale':40, 'search':20, 'services':22,
    'services_active':22, 'shirt':52, 'step_current':36, 'step_done':36,
    'step_pending':36, 'user_header':24, 'user_small':20,
}
for name,size in SIZES.items():
    kind=name.removesuffix('_active')
    draw_icon(kind,(size,size),'#1E8E3E' if '_active' in name else '#145A32').save(DEST/(name+'.png'))

for kind in ('home','orders','services','track','payment','history','profile','report','info','logout'):
    for suffix,color in (('', '#145A32'),('_white','#FFFFFF')):
        draw_icon(kind,(20,20),color).save(DEST/f'{kind}{suffix}.png')
draw_icon('logo',(64,64),'#FFFFFF').save(DEST/'logo_white.png')
draw_icon('logo',(96,96),'#1E8E3E').save(DEST/'logo.png')
draw_icon('logo',(32,32),'#1E8E3E').save(DEST/'logo_small.png')
print(f'Rendered {len(list(DEST.glob("*.png")))} fresh green/white UI assets')
