import math, os, struct, wave, zlib
from pathlib import Path
ROOT = Path('app/src/main/assets')
ROOT.mkdir(parents=True, exist_ok=True)
def png(path,w,h,fn,bg=(0,0,0,0),scale=2):
    W,H=w*scale,h*scale; pix=[[list(bg) for _ in range(W)] for _ in range(H)]
    def blend(x,y,c):
        if 0<=x<W and 0<=y<H:
            a=c[3]/255; old=pix[y][x]
            pix[y][x]=[int(c[i]*a+old[i]*(1-a)) for i in range(3)]+[min(255,int(c[3]+old[3]*(1-a)))]
    def rect(x0,y0,x1,y1,c):
        for y in range(max(0,int(y0*scale)),min(H,int(y1*scale))):
            for x in range(max(0,int(x0*scale)),min(W,int(x1*scale))): blend(x,y,c)
    def circle(cx,cy,r,c):
        rr=(r*scale)**2
        for y in range(int((cy-r)*scale),int((cy+r+1)*scale)):
            for x in range(int((cx-r)*scale),int((cx+r+1)*scale)):
                if (x-cx*scale)**2+(y-cy*scale)**2<=rr: blend(x,y,c)
    def poly(points,c):
        pts=[(int(x*scale),int(y*scale)) for x,y in points]; minx=max(0,min(x for x,_ in pts)); maxx=min(W-1,max(x for x,_ in pts)); miny=max(0,min(y for _,y in pts)); maxy=min(H-1,max(y for _,y in pts))
        for y in range(miny,maxy+1):
            xs=[]
            for i in range(len(pts)):
                x1,y1=pts[i]; x2,y2=pts[(i+1)%len(pts)]
                if (y1<=y<y2) or (y2<=y<y1): xs.append(x1+(y-y1)*(x2-x1)/(y2-y1))
            xs.sort()
            for a,b in zip(xs[0::2],xs[1::2]):
                for x in range(max(0,int(a)),min(W,int(b)+1)): blend(x,y,c)
    fn(rect,circle,poly); out=bytearray()
    for y in range(h):
        out.extend(b'\x00')
        for x in range(w):
            rs=gs=bs=aa=0
            for yy in range(scale):
                for xx in range(scale): q=pix[y*scale+yy][x*scale+xx]; rs+=q[0]; gs+=q[1]; bs+=q[2]; aa+=q[3]
            n=scale*scale; out.extend(bytes((rs//n,gs//n,bs//n,aa//n)))
    def chunk(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(out),9))+chunk(b'IEND',b''))
def chicken(path,body,comb,crown=False,ice=False,frame=0):
    def draw(rect,circle,poly):
        circle(180,300,70,(60,45,30,45)); circle(180,185,105,(*body,255)); circle(100,145,75,(*body,255)); circle(80,130,13,(45,38,30,255)); circle(86,128,4,(255,255,255,255)); poly([(40,190),(5,165),(35,155)],(226,145,54,255)); poly([(70,72),(95,35),(115,75)],(*comb,255)); poly([(110,72),(137,38),(145,85)],(*comb,255))
        if crown: poly([(120,52),(135,15),(150,45),(170,10),(178,52)],(239,190,58,255))
        if ice: poly([(250,100),(285,50),(275,125),(315,100),(285,155)],(112,190,231,220))
        circle(175+(8 if frame else -4),205,48,(232,226,208,255)); rect(170,285,180,330,(216,157,72,255)); rect(210,282,220,330,(216,157,72,255))
        if frame: poly([(180,250),(235,270),(185,282)],(237,170,74,255))
    png(path,360,360,draw)
def enemy(path,kind):
    colors={'rat':(142,128,114,255),'fox':(224,133,65,255),'wolf':(126,135,145,255),'bear':(150,103,66,255),'eagle':(105,110,120,255)}
    def draw(rect,circle,poly):
        c=colors[kind]; circle(180,185,100,c); circle(118,145,50,c); circle(105,132,10,(40,35,30,255)); circle(130,132,8,(40,35,30,255))
        if kind in ('fox','wolf'): poly([(80,115),(95,48),(140,90)],c); poly([(180,90),(225,50),(235,125)],c)
        if kind=='bear': circle(85,82,32,c); circle(225,82,32,c)
        if kind=='eagle': poly([(70,180),(10,135),(80,210)],(85,91,104,255)); poly([(290,180),(350,135),(280,210)],(85,91,104,255))
        poly([(108,165),(82,176),(108,184)],(232,167,64,255)); rect(135,265,150,318,(75,64,55,255)); rect(205,265,220,318,(75,64,55,255))
    png(path,320,320,draw,bg=(0,0,0,0))
def ui(path,kind):
    def draw(rect,circle,poly):
        if kind=='egg': circle(96,96,62,(245,220,166,255)); circle(78,75,13,(255,245,215,180))
        elif kind=='heart': circle(70,72,32,(221,91,91,255)); circle(122,72,32,(221,91,91,255)); poly([(38,75),(154,75),(96,158)],(221,91,91,255))
        else: circle(96,96,66,(229,155,66,255)); circle(96,96,46,(255,248,225,255)); rect(58,90,134,102,(229,155,66,255))
    png(path,192,192,draw,bg=(0,0,0,0))
def start_menu():
    def draw(rect,circle,poly):
        rect(0,0,1600,900,(196,226,172,255)); rect(0,590,1600,900,(120,170,95,255)); circle(1360,150,95,(251,211,106,255)); circle(1140,160,120,(255,255,255,90)); circle(1260,130,90,(255,255,255,80)); rect(590,355,1010,630,(231,194,125,255)); poly([(540,365),(800,170),(1060,365)],(178,89,73,255)); rect(700,495,900,630,(108,83,64,255))
        for x in range(60,1550,120): rect(x,610,x+22,765,(222,189,132,255)); rect(x,665,x+95,680,(222,189,132,255))
        for x in (250,420,1110,1260): circle(x,630,70,(250,246,230,255)); circle(x-45,600,45,(250,246,230,255)); circle(x-52,593,7,(40,35,30,255)); poly([(x-12,620),(x-65,600),(x-15,602)],(226,145,54,255)); poly([(x+5,556),(x+25,532),(x+35,558)],(196,76,61,255))
    png(ROOT/'start_menu.png',1600,900,draw,bg=(20,20,20,255),scale=1)
start_menu()
for t,body,comb,crown,ice in [('normal',(250,246,230),(196,76,61),False,False),('fast',(242,242,242),(210,82,70),False,False),('power',(255,237,196),(190,61,50),False,False),('ice',(238,250,255),(109,184,218),False,True),('king',(252,241,199),(194,87,54),True,False)]:
    chicken(ROOT/f'chicken_{t}_0.png',body,comb,crown,ice,0); chicken(ROOT/f'chicken_{t}_1.png',body,comb,crown,ice,1)
for k in ('rat','fox','wolf','bear','eagle'): enemy(ROOT/f'enemy_{k}.png',k)
for k in ('egg','heart','wave'): ui(ROOT/f'ui_{k}.png',k)
def tone(path,seconds,freqs,amp=0.24,rate=22050):
    n=int(seconds*rate); frames=bytearray()
    for i in range(n):
        t=i/rate; env=min(1,t*40)*min(1,(seconds-t)*35); f=freqs[int((t/seconds)*len(freqs))%len(freqs)] if isinstance(freqs,list) else freqs; v=math.sin(2*math.pi*f*t)*amp*env; frames+=struct.pack('<h',int(v*32767))
    with wave.open(str(path),'wb') as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(rate); w.writeframes(frames)
tone(ROOT/'click.wav',.08,760,.25); tone(ROOT/'hit.wav',.14,[180,120,90],.24); tone(ROOT/'wave.wav',.8,[392,494,587,784],.20)
rate=22050; seconds=8; notes=[262,330,392,523,392,330,294,349]; frames=bytearray()
for i in range(int(rate*seconds)):
    t=i/rate; note=notes[int(t*2)%len(notes)]; env=.13+.05*math.sin(2*math.pi*t/4); v=(math.sin(2*math.pi*note*t)*.18+math.sin(2*math.pi*note*2*t)*.045)*env; frames+=struct.pack('<h',int(v*32767))
with wave.open(str(ROOT/'bgm.wav'),'wb') as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(rate); w.writeframes(frames)
print('Generated',len(list(ROOT.iterdir())),'assets')
