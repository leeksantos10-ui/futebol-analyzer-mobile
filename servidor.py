import sqlite3, json, os
from pathlib import Path
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs

BASE = Path(__file__).resolve().parent
DB = str(BASE / "banco_jogos_15323.sqlite")
PORT = int(os.environ.get("PORT", "8005"))

# Métricas oficiais do banco V5 (as métricas antigas de força não são usadas).
METRICS = {
    "LG Score Casa":"LG Score Casa", "LG Score Visitante":"LG Score Visitante", "H-Score":"H-Score",
    "Média Gols Feitos Casa":"Media Gols Feitos Casa", "Média Gols Feitos Visitante":"Media Gols Feitos Visitante",
    "CV Gols Feitos Casa":"CV Gols Feitos Casa", "CV Gols Feitos Visitante":"CV Gols Feitos Visitante",
    "Média Gols Sofridos Casa":"Media Gols Sofridos Casa", "Média Gols Sofridos Visitante":"Media Gols Sofridos Visitante",
    "CV Gols Sofridos Casa":"CV Gols Sofridos Casa", "CV Gols Sofridos Visitante":"CV Gols Sofridos Visitante",
    "Média Gols Marcados HT Casa":"Media Gols Marcados HT Casa", "Média Gols Marcados HT Visitante":"Media Gols Marcados HT Visitante",
    "Média Gols Sofridos HT Casa":"Media Gols Sofridos HT Casa", "Média Gols Sofridos HT Visitante":"Media Gols Sofridos HT Visitante",
    "Média Custo do Gol 2,0 Casa":"Media Custo do Gol 2,0 Casa", "Média Custo do Gol 2,0 Visitante":"Media Custo do Gol 2,0 Visitante",
    "CV Custo do Gol 2,0 Casa":"CV Custo do Gol 2,0 Casa", "CV Custo do Gol 2,0 Visitante":"CV Custo do Gol 2,0 Visitante",
    "Jogos Over 1,5 FT Casa":"Jogos Over 1,5 FT Casa", "Jogos Over 1,5 FT Visitante":"Jogos Over 1,5 FT Visitante",
    "Jogos Over 2,5 FT Casa":"Jogos Over 2,5 FT Casa", "Jogos Over 2,5 FT Visitante":"Jogos Over 2,5 FT Visitante",
    "Pontos Casa":"Pontos Casa", "Pontos Visitante":"Pontos Visitante",
    "Odd Casa":"Odd Casa", "Odd Empate":"Odd Empate", "Odd Visitante":"Odd Visitante",
    "1X ODD":"1x ODD", "2X ODD":"2X ODD",
    "Força Custo do Gol Casa":"FORÇA CUSTO DO GOL CASA",
    "Força Custo do Gol Visitante":"FORÇA CUSTO DO GOL VISITANTE",
    "Força Ofensiva FT Casa":"FORÇA OFENSIVA FT CASA",
    "Força Ofensiva FT Visitante":"FORÇA OFENSIVA VISITAN FT",
    "Força Defensiva FT Casa":"FORÇA DEFENSIVA CASA FT",
    "Força Defensiva FT Visitante":"FORÇA DEFENSIVA VISITAN FT",
    "Força Ofensiva HT Casa":"FORÇA OFENSIVA CASA HT",
    "Força Ofensiva HT Visitante":"FORÇA OFENSIVA VISITAN HT",
    "Força Defensiva HT Casa":"FORÇA DEFENSIVA CASA HT",
    "Força Defensiva HT Visitante":"FORÇA DEFENSIVA VISITAN HT",
    "CV Combinado Custo do Gol":"CV COMBINADO CUSTO DO GOL",
    "CV Combinado Gols Feitos":"CV COMBINADO GOLS FEITO",
    "CV Combinado Gols Sofridos":"CV COMBINADO GOLS SOFRIDOS"
}

MARKETS = {
    "Over 0,5 FT": ("Over 0,5 FT", lambda h,a: h+a>=1),
    "Over 1,5 FT": ("Over 1,5 FT", lambda h,a: h+a>=2),
    "Over 2,5 FT": ("Over 2,5 FT", lambda h,a: h+a>=3),
    "BTTS Sim FT": ("BTTS Sim FT", lambda h,a: h>=1 and a>=1),
    "Casa": ("Odd Casa", lambda h,a: h>a),
    "Empate": ("Odd Empate", lambda h,a: h==a),
    "Visitante": ("Odd Visitante", lambda h,a: a>h),
    "1X": ("1x ODD", lambda h,a: h>=a),
    "2X": ("2X ODD", lambda h,a: a>=h)
}

def fnum(v):
    try:
        if v is None or v=="": return None
        return float(str(v).replace(",","."))
    except: return None

def cmp(v,op,x):
    if v is None or x is None:return False
    return {">":v>x,">=":v>=x,"<":v<x,"<=":v<=x,"=":abs(v-x)<1e-12}.get(op,False)

def stats(rows,stake):
    bal=0; peak=0; dd=0; ddp=0; g=r=0; odds=[]
    for row in rows:
        odd=fnum(row["odd"])
        if odd is None or odd<=0: continue
        if row["green"]:
            g+=1; bal += stake*(odd-1)
        else:
            r+=1; bal -= stake
        peak=max(peak,bal); d=peak-bal; dd=max(dd,d)
        if peak>0: ddp=max(ddp,d/peak*100)
        odds.append(odd)
    n=g+r
    return {"entries":n,"greens":g,"reds":r,"winrate":g/n*100 if n else 0,
            "avgodd":sum(odds)/len(odds) if odds else 0,"lp":bal,
            "roi":bal/(n*stake)*100 if n else 0,"maxdd":dd,"maxddpct":ddp}

def evaluate(strategy,market,stake):
    if market not in MARKETS: raise ValueError("Mercado inválido")
    conn=sqlite3.connect(DB); conn.row_factory=sqlite3.Row
    cols={"Gols Casa FT","Gols Visitante FT","Data","Hora","Confronto","Campeonato",MARKETS[market][0]}
    for rule in strategy.get("rules",[]):
        if METRICS.get(rule.get("metric")): cols.add(METRICS[rule["metric"]])
        if rule.get("compare_mode")=="metric" and METRICS.get(rule.get("rhs_metric")): cols.add(METRICS[rule["rhs_metric"]])
    sql="SELECT "+",".join('"'+c.replace('"','""')+'"' for c in cols)+" FROM jogos"
    rows=conn.execute(sql).fetchall(); conn.close()
    out=[]
    champs=[str(x).strip() for x in strategy.get("championships",[]) if str(x).strip()]
    for row in rows:
        v={c:row[c] for c in cols}
        if champs and str(v.get("Campeonato","")).strip() not in champs: continue
        ok=True
        for rule in strategy.get("rules",[]):
            left=fnum(v.get(METRICS.get(rule.get("metric"))))
            right=fnum(v.get(METRICS.get(rule.get("rhs_metric")))) if rule.get("compare_mode")=="metric" else fnum(rule.get("value"))
            if not cmp(left,rule.get("op"),right): ok=False; break
        if not ok: continue
        h=fnum(v["Gols Casa FT"]); a=fnum(v["Gols Visitante FT"]); odd=fnum(v[MARKETS[market][0]])
        if h is None or a is None or odd is None or odd<=0: continue
        out.append({"data":v["Data"],"hora":v["Hora"],"confronto":v["Confronto"],"campeonato":v["Campeonato"],
                    "odd":odd,"green":bool(MARKETS[market][1](h,a)),"casa":h,"visitante":a})
    valid=[x for x in out if isinstance(x["data"],str) and x["data"] not in ("Data","")]
    valid.sort(key=lambda x:(x["data"],str(x["hora"])))
    allst=stats(out,stake); st=stats(valid,stake)
    bal=0; curve=[]; months={}
    for x in valid:
        bal += stake*(x["odd"]-1) if x["green"] else -stake
        curve.append({"data":x["data"],"banca":bal})
        m=str(x["data"])[:7]; z=months.setdefault(m,{"mes":m,"jogos":0,"green":0,"red":0,"lp":0})
        z["jogos"]+=1
        if x["green"]: z["green"]+=1; z["lp"]+=stake*(x["odd"]-1)
        else: z["red"]+=1; z["lp"]-=stake
    for z in months.values():
        z["winrate"]=z["green"]/z["jogos"]*100 if z["jogos"] else 0
        z["roi"]=z["lp"]/(z["jogos"]*stake)*100 if z["jogos"] else 0
    ms=list(months.values())
    st.update({"all":allst,"curve":curve,"monthly":sorted(ms,key=lambda x:x["mes"]),
               "matches":[{"data":x["data"],"hora":x["hora"],"confronto":x["confronto"],"campeonato":x["campeonato"],"odd":x["odd"],"resultado":"GREEN" if x["green"] else "RED","placar":f'{int(x["casa"])} x {int(x["visitante"])}'} for x in out]})
    return st

class Handler(BaseHTTPRequestHandler):
    def json(self,obj,code=200):
        b=json.dumps(obj,ensure_ascii=False).encode(); self.send_response(code); self.send_header("Content-Type","application/json; charset=utf-8"); self.send_header("Access-Control-Allow-Origin","*"); self.send_header("Content-Length",str(len(b))); self.end_headers(); self.wfile.write(b)
    def do_GET(self):
        u=urlparse(self.path); p=parse_qs(u.query)
        try:
            if u.path=="/api/meta":
                con=sqlite3.connect(DB)
                champs=[r[0] for r in con.execute('SELECT DISTINCT "Campeonato" FROM jogos WHERE "Campeonato" IS NOT NULL AND TRIM("Campeonato")<>"" ORDER BY "Campeonato"').fetchall()]
                total=con.execute("SELECT COUNT(*) FROM jogos").fetchone()[0]; con.close()
                return self.json({"metrics":list(METRICS.keys()),"markets":list(MARKETS.keys()),"championships":champs,"total":total})
            if u.path=="/api/evaluate":
                strategies=json.loads(p.get("strategies",["[]"])[0]); market=p.get("market",["Over 1,5 FT"])[0]; stake=fnum(p.get("stake",["1"])[0]) or 1
                res=[]
                for i,s in enumerate(strategies):
                    x=evaluate(s,market,stake); x["name"]=s.get("name",f"Estratégia {i+1}"); x["id"]=i; res.append(x)
                return self.json({"status":"ok","results":res})
            if u.path in ("/","/index.html"):
                b=(BASE/"index.html").read_bytes(); self.send_response(200); self.send_header("Content-Type","text/html; charset=utf-8"); self.send_header("Content-Length",str(len(b))); self.end_headers(); self.wfile.write(b); return
            self.json({"status":"ok","msg":"Servidor V5 ativo"})
        except Exception as e: self.json({"status":"erro","msg":str(e)},500)

print(f"Banco: {DB}\nServidor Futebol Analyzer: porta {PORT}")
HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
