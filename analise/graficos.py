import pandas as pd, numpy as np, matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from scipy import stats
AZUL="#0b4f8a"; CINZA="#5b6b7a"; GRADE="#d8e0e8"; VERM="#b3261e"; TXT="#1c2a36"
plt.rcParams.update({"font.family":"DejaVu Sans","font.size":10,"axes.edgecolor":GRADE,"axes.labelcolor":TXT,
  "xtick.color":CINZA,"ytick.color":CINZA,"axes.spines.top":False,"axes.spines.right":False})
df = pd.read_csv("dados.csv"); c = df.dropna(subset=["dias_para_concluir"])

# A
fig, ax = plt.subplots(figsize=(6.4,3.6), dpi=200)
lr = stats.linregress(c.prioridade_num, c.dias_para_concluir)
rng = np.random.default_rng(3); jit = rng.uniform(-0.07,0.07,len(c))
ax.scatter(c.prioridade_num+jit, c.dias_para_concluir, s=46, color=AZUL, edgecolor="white", linewidth=1.2, zorder=3)
xs = np.linspace(0.8,3.2,10); ax.plot(xs, lr.intercept+lr.slope*xs, color=CINZA, lw=2, ls="--", zorder=2)
ax.text(3.2, lr.intercept+lr.slope*3.2+0.4, f"dias = {lr.intercept:.0f} + {lr.slope:.0f} × prioridade\nr = {lr.rvalue:.2f} · R² = {lr.rvalue**2:.2f}", ha="right", va="bottom", color=TXT, fontsize=9)
ax.set_xticks([1,2,3], ["BAIXA","MÉDIA","ALTA"]); ax.set_xlim(0.6,3.4); ax.set_ylim(0,13)
ax.set_ylabel("Dias até concluir"); ax.set_xlabel("Prioridade definida pela IA")
ax.grid(axis="y", color=GRADE, lw=0.8); ax.set_axisbelow(True)
ax.set_title("Quanto maior a prioridade, mais demora para concluir", loc="left", fontsize=11.5, color=TXT, fontweight="bold")
fig.tight_layout(); fig.savefig("grafico_prioridade_tempo.png"); plt.close(fig)

# B
fig, ax = plt.subplots(figsize=(6.4,3.6), dpi=200)
o = df[df.dias_para_concluir.isna()].copy()
lr = stats.linregress(o.dias_aberta, o.status_ordem)
o["esp"] = lr.intercept + lr.slope*o.dias_aberta
alerta = (o.esp - o.status_ordem) >= 0.5
# empilha pontos iguais lado a lado para nao sumirem
o["xj"] = o.dias_aberta + o.groupby(["dias_aberta","status_ordem"]).cumcount()*0.12 - o.groupby(["dias_aberta","status_ordem"]).protocolo.transform("count").sub(1)*0.06
ax.scatter(o.xj[~alerta], o.status_ordem[~alerta], s=46, color=AZUL, edgecolor="white", linewidth=1.2, zorder=3)
ax.scatter(o.xj[alerta], o.status_ordem[alerta], s=70, color=VERM, edgecolor="white", linewidth=1.2, zorder=4, marker="D")
for _,r in o[alerta].iterrows():
    ax.annotate(f"{r.protocolo}: 4 dias e ainda em análise\n(esperado: Em Andamento)", (r.xj, r.status_ordem), xytext=(0,-14), textcoords="offset points", fontsize=8.5, color=VERM, va="top", ha="center")
xs = np.linspace(0.7,5.5,10); ax.plot(xs, lr.intercept+lr.slope*xs, color=CINZA, lw=2, ls="--", zorder=2)
ax.text(0.8, 3.45, f"etapa = {lr.intercept:.2f} + {lr.slope:.2f} × dias\nr = {lr.rvalue:.2f} · R² = {lr.rvalue**2:.2f}", ha="left", va="center", color=TXT, fontsize=9)
ax.set_yticks([1,2,3], ["Recebido","Em Análise","Em Andamento"]); ax.set_ylim(0.6,3.7); ax.set_xlim(0.5,5.8)
ax.set_xticks([1,2,3,4,5])
ax.set_xlabel("Dias desde a abertura (solicitações em aberto)"); ax.grid(axis="y", color=GRADE, lw=0.8); ax.set_axisbelow(True)
ax.set_title("Pedidos abertos avançam ~1 etapa a cada 2 dias", loc="left", fontsize=11.5, color=TXT, fontweight="bold")
fig.tight_layout(); fig.savefig("grafico_idade_etapa.png"); plt.close(fig)
print("ok", list(o[alerta].protocolo))
