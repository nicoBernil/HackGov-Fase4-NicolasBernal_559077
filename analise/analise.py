"""HackGov - Parte 3: correlacao e regressao sobre os dados de teste.
Uso: python extrair.py dados_teste_estatisticas.sql && python analise.py && python graficos.py
"""
import pandas as pd
from scipy import stats

df = pd.read_csv("dados.csv")
concluidas = df.dropna(subset=["dias_para_concluir"])
abertas = df[df.dias_para_concluir.isna()]

def analisar(nome, x, y):
    r, p = stats.pearsonr(x, y)
    rho, p_s = stats.spearmanr(x, y)
    lr = stats.linregress(x, y)
    print(f"{nome}\n  n={len(x)}  Pearson r={r:.3f} (p={p:.3f})  Spearman rho={rho:.3f} (p={p_s:.3f})")
    print(f"  regressao: y = {lr.intercept:.3f} + {lr.slope:.3f}x   R2={lr.rvalue**2:.3f}\n")
    return lr

a = analisar("A) prioridade x dias para concluir (concluidas)", concluidas.prioridade_num, concluidas.dias_para_concluir)
for nivel, n in [("BAIXA", 1), ("MEDIA", 2), ("ALTA", 3)]:
    print(f"  previsao {nivel}: {a.intercept + a.slope * n:.1f} dias")
print()
b = analisar("B) dias em aberto x etapa do fluxo (abertas)", abertas.dias_aberta, abertas.status_ordem)
atraso = (b.intercept + b.slope * abertas.dias_aberta) - abertas.status_ordem
print("  paradas (>= 0,5 etapa atras do esperado):", list(abertas.protocolo[atraso >= 0.5]), "\n")
analisar("C) tamanho da descricao x prioridade (todas)", df.tam_descricao, df.prioridade_num)
