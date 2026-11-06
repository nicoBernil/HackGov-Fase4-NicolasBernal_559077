# HackGov — Análise estatística (Visão de Futuro, Parte 3)

Correlação e regressão sobre os 20 registros de `hackgov-backend/src/main/resources/db/dados_teste_estatisticas.sql`.
Os mesmos cálculos rodam dentro do sistema em `GET /api/estatisticas/avancadas` (`AnaliseEstatisticaService`).

```
pip install pandas scipy matplotlib
python extrair.py ../hackgov-backend/src/main/resources/db/dados_teste_estatisticas.sql
python analise.py
python graficos.py
```

Resultado em `resultado_analise.txt`; gráficos em `grafico_*.png`.
