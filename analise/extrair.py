import re, csv, sys
src = open(sys.argv[1], encoding="utf-8").read()
pat = re.compile(r"nova_solicitacao\('(\w+)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'(\w+)',\s*v_cat_(\w+),\s*v_st_(\w+),\s*(\d+),\s*(NULL|\d+)\)", re.S)
rows = pat.findall(src)
pri = {"BAIXA":1,"MEDIA":2,"ALTA":3}
st = {"recebido":1,"analise":2,"andamento":3,"concluido":4}
with open("dados.csv","w",newline="",encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["protocolo","titulo","bairro","prioridade","prioridade_num","categoria","status","status_ordem","dias_aberta","dias_para_concluir","tam_descricao"])
    for p,t,d,b,pr,c,s,da,dc in rows:
        w.writerow([p,t,b,pr,pri[pr],c,s,st[s],int(da),"" if dc=="NULL" else int(dc),len(d)])
print(len(rows))
