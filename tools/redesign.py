# -*- coding: utf-8 -*-
"""One-off redesign pass over www/index.html (v1.4.0):
readable colours + bigger text, new chrome, sanctity warning, bug fixes."""
import re, colorsys, io, sys
P='www/index.html'
s=io.open(P,encoding='utf8').read()
assert 'kd-warn' not in s, 'already applied'

# ---------- split off the splash (keep as is) ----------
a=s.index('/* ── SPLASH SCREEN'); b=s.index('/* modal name display */')
c=s.index('<!-- ══ SPLASH SCREEN'); d=s.index('<!-- ══ END SPLASH')
KEEP=[(a,b),(c,d)]
def protected(i): return any(x<=i<y for x,y in KEEP)

# ---------- 1. readable text colours ----------
BG=(0x0f,0x0d,0x18)
def lum(rgb):
    f=lambda v:(v/255/12.92) if v/255<=0.03928 else ((v/255+0.055)/1.055)**2.4
    r,g,b_=rgb; return 0.2126*f(r)+0.7152*f(g)+0.0722*f(b_)
def contrast(c1,c2):
    l1,l2=sorted((lum(c1),lum(c2)),reverse=True); return (l1+0.05)/(l2+0.05)
def fix_color(hexv):
    h=hexv.lstrip('#')
    if len(h)==3: h=''.join(ch*2 for ch in h)
    if len(h)!=6: return hexv
    rgb=tuple(int(h[i:i+2],16) for i in (0,2,4))
    L=lum(rgb)
    if L<0.012: return hexv                      # dark text on gold buttons
    hh,ll,ss=colorsys.rgb_to_hls(*[v/255 for v in rgb])
    hue=hh*360
    if not (15<=hue<=65) and ss>0.25: return hexv  # coloured (sefira etc.) - keep
    if ss>0.72 and ll>0.40: return hexv            # bright gold accents - keep
    target=7.5
    if contrast(rgb,BG)>=target: return hexv
    ss=min(ss,0.42)
    while ll<0.95:
        ll+=0.01
        n=tuple(round(v*255) for v in colorsys.hls_to_rgb(hh,ll,ss))
        if contrast(n,BG)>=target: break
    return '#%02x%02x%02x'%n
cnt=[0]
def col_sub(m):
    if protected(m.start()): return m.group(0)
    new=fix_color(m.group(2))
    if new!=m.group(2): cnt[0]+=1
    return m.group(1)+new
# "color:#xxx" but not border-color / background-color / stop-color, and not quoted data values
s2=re.sub(r'((?<![-\w])color\s*:\s*)(#[0-9a-fA-F]{3,6})\b',col_sub,s)
s2=re.sub(r'(\.style\.color\s*=\s*[\'"])(#[0-9a-fA-F]{3,6})\b',col_sub,s2)
assert len(s2)==len(s) or True
print('colours fixed:',cnt[0])

# offsets may have shifted slightly (3-digit hex) -> recompute protected ranges
def ranges(t):
    a=t.index('/* ── SPLASH SCREEN'); b=t.index('/* modal name display */')
    c=t.index('<!-- ══ SPLASH SCREEN'); d=t.index('<!-- ══ END SPLASH')
    return [(a,b),(c,d)]
s=s2; KEEP=ranges(s)

# ---------- 2. letter-spacing on small labels (hurts Hebrew legibility) ----------
def style_sub(m):
    if protected(m.start()): return m.group(0)
    body=m.group(2)
    fs=re.search(r'font-size:\s*([0-9.]+)px',body)
    if fs and float(fs.group(1))<=14:
        body=re.sub(r'letter-spacing:\s*[1-9][0-9.]*px','letter-spacing:.3px',body)
    return m.group(1)+body+m.group(3)
s=re.sub(r'(style=")([^"]*)(")',style_sub,s); KEEP=ranges(s)
s=re.sub(r'(\{)([^{}]*letter-spacing[^{}]*)(\})',style_sub,s); KEEP=ranges(s)

# ---------- 3. bigger text ----------
MAP={8:12,9:12.5,9.5:12.5,10:13,11:14,11.5:14,12:15,12.5:15,13:16,14:17,15:17.5,16:18,17:19}
fc=[0]
def fs_sub(m):
    if protected(m.start()): return m.group(0)
    v=float(m.group(2))
    if v in MAP:
        fc[0]+=1; n=MAP[v]
        return m.group(1)+(('%g'%n))+'px'
    return m.group(0)
s=re.sub(r'(font-size:\s*)([0-9.]+)px',fs_sub,s); KEEP=ranges(s)
print('font sizes raised:',fc[0])

# ---------- 4. fonts: bundled, no internet needed ----------
imp="@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+Hebrew:wght@400;600;700;900&display=swap');"
assert imp in s
s=s.replace(imp,"""@font-face{font-family:'Noto Serif Hebrew';font-style:normal;font-weight:100 900;font-display:swap;src:url('fonts/noto-serif-hebrew.woff2') format('woff2');unicode-range:U+0590-05FF,U+200C-2010,U+20AA,U+25CC,U+FB1D-FB4F}
@font-face{font-family:'Noto Serif Hebrew';font-style:normal;font-weight:100 900;font-display:swap;src:url('fonts/noto-serif-latin.woff2') format('woff2');unicode-range:U+0000-00FF,U+2013-2014,U+2018-201D,U+2022,U+2026}""")

# ---------- 5. new chrome CSS ----------
CSS=io.open('tools/redesign.css',encoding='utf8').read()
k=s.index('</style>'); s=s[:k]+CSS+'\n'+s[k:]

# ---------- 6. warning bar + warning screen markup ----------
HTML=io.open('tools/redesign.html',encoding='utf8').read()
k=s.index('<div class="header">'); s=s[:k]+HTML+'\n'+s[k:]
# header: simpler, no inline size
old='<div class="header-title" style="font-size:26px">שמות הקודש</div>'
assert old in s; s=s.replace(old,'<div class="header-title">שמות הקודש</div>')

# splash -> when it closes, the warning countdown starts
old="setTimeout(function(){s.style.display='none';},920);\n}"
assert old in s
s=s.replace(old,"setTimeout(function(){s.style.display='none';},920);\n  if(window.kdWarnStart) window.kdWarnStart();\n}")
old="function dismissSplash(){\n  running=false;"
assert old in s
s=s.replace(old,"var splDone=false;\nfunction dismissSplash(){\n  if(splDone) return; splDone=true;\n  running=false;")

JS=io.open('tools/redesign.js',encoding='utf8').read()
k=s.rindex('</script>'); s=s[:k]+JS+'\n'+s[k:]

# active nav button scrolls into view
old="    b.classList.toggle('active', b.getAttribute('onclick').includes(\"'\"+v+\"'\"));\n  });"
assert old in s
s=s.replace(old,old+"\n  const _ab=document.querySelector('.nav-btn.active'); if(_ab&&_ab.scrollIntoView) _ab.scrollIntoView({inline:'center',block:'nearest',behavior:'smooth'});\n  window.scrollTo(0,0);")

# ---------- 7. bug fixes ----------
# 7a. calendar button called a function that does not exist
assert s.count('openNameModal(')==1; s=s.replace('openNameModal(','openModal(')
# 7b. download/share link pointed at v1.0.0 forever
n=s.count('releases/download/v1.0.0/shemot-kodesh.apk'); assert n==2
s=s.replace('releases/download/v1.0.0/shemot-kodesh.apk','releases/latest/download/shemot-kodesh.apk')
# 7c. duplicate nikud input id + duplicate functions
i1=s.index('function saveModalNikud(){'); i2=s.index('function renderModalNotes(){') 
j=s.index('}\n',s.index('function clearModalNikud(){'))+2
assert i2<i1<j; s=s[:i1]+s[j:]
first='<input id="modal-nikud-input" class="nk-input"'; assert s.count(first)==1
assert s.count('id="modal-nikud-input"')==2
k=s.index('id="modal-nikud-input"',s.index(first)+10)
s=s[:k]+'id="modal-nikud-input2"'+s[k+len('id="modal-nikud-input"'):]
# buttons say which field they belong to
k=s.index(first); k2=s.index('saveModalNikud()',k); s=s[:k2]+"saveModalNikud('modal-nikud-input')"+s[k2+len('saveModalNikud()'):]
k=s.index('id="modal-nikud-input2"'); k2=s.index('saveModalNikud()',k); s=s[:k2]+"saveModalNikud('modal-nikud-input2')"+s[k2+len('saveModalNikud()'):]
old="function saveModalNikud() {\n  if(!currentName) return;\n  const id = currentName.id;\n  const inp = document.getElementById('modal-nikud-input');"
assert old in s
s=s.replace(old,"function saveModalNikud(fieldId) {\n  if(!currentName) return;\n  const id = currentName.id;\n  const inp = document.getElementById(fieldId || 'modal-nikud-input');")
old="  const btn = document.querySelector('[onclick=\"saveModalNikud()\"]');\n  if(btn) { btn.textContent = '✅'; setTimeout(()=>{btn.textContent='שמור';},1200); }"
assert old in s
s=s.replace(old,"  ['modal-nikud-input','modal-nikud-input2'].forEach(f=>{const e=document.getElementById(f); if(e) e.value=val;});")
old="  const inp = document.getElementById('modal-nikud-input');\n  if(inp) inp.value = '';\n  renderNames(NAMES_A);\n}"
assert s.count(old)==1
s=s.replace(old,"  ['modal-nikud-input','modal-nikud-input2'].forEach(f=>{const e=document.getElementById(f); if(e) e.value='';});\n  renderNames(NAMES_A);\n  const mnEl = document.getElementById('modal-name');\n  if(mnEl) mnEl.textContent = getDisplayName(currentName);\n}")
old="  const nikudInp = document.getElementById('modal-nikud-input');\n  if(nikudInp) {"
assert old in s; s=s.replace(old,"  const nikudInp = document.getElementById('modal-nikud-input2');\n  if(nikudInp) {")
# 7d. reminder marked active even when scheduling failed
old="  }]}).catch(e=>{alert('שגיאה: '+e.message);return;});\n  localStorage.setItem('dailyReminder','1');"
assert old in s
s=s.replace("  await window.Capacitor.Plugins.LocalNotifications.schedule({notifications:[{\n    id:72,","  let _ok=true;\n  await window.Capacitor.Plugins.LocalNotifications.schedule({notifications:[{\n    id:72,")
s=s.replace(old,"  }]}).catch(e=>{_ok=false;alert('שגיאה: '+e.message);});\n  if(!_ok) return;\n  localStorage.setItem('dailyReminder','1');")
assert '_ok=true' in s
# 7e. notes with "<" broke the note display
old="${note.replace(/\\n/g,'<br>')}"
assert old in s
s=s.replace(old,"${note.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/\\n/g,'<br>')}")
# dead references
s=s.replace('  <link rel="manifest" href="manifest.json">\n','')

io.open(P,'w',encoding='utf8').write(s)
print('done',len(s))
