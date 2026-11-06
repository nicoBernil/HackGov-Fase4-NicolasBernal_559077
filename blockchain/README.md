# HackGov — Smart Contract (Visão de Futuro, Parte 4)

`src/HackGovRegistro.sol` ancora na blockchain o **hash** de cada abertura e de cada
mudança de status do HackGov, e torna público o prazo (SLA) de cada prioridade.
Nenhum dado pessoal vai para a blockchain.

## Demonstração no Remix (sem instalar nada)

1. Abra https://remix.ethereum.org, crie `HackGovRegistro.sol` e cole o conteúdo de `src/HackGovRegistro.sol`.
2. Aba **Solidity Compiler** → versão `0.8.26` → **Compile**.
3. Aba **Deploy & Run** → ambiente **Remix VM** → **Deploy**. A conta que fez o deploy é a prefeitura e já é gestora.
4. `registrarSolicitacao`:
   - protocolo: `0x107b446b1b6d6894851d17c9b9702281b27055b1c11098f59351f462a5213605` (keccak256 de "HG202600028")
   - hashConteudo: `0xe96b14f9b6d99c1340a0316a022044ee458da6a1c892c65da3e2f657db97e8ca`
   - prioridade: `2` (ALTA)
5. `alterarStatus`:
   - protocolo: o mesmo do passo 4
   - novoStatus: `2` (Em Análise)
   - hashHistorico: `0x04db8e6ed21a93382a4a4bf2826af5848cb3f8b01665bdf2898661bd17e451c8`
6. `verificarAlteracao` com índice `0`:
   - com o hash do passo 5 → `true`
   - com `0x56c5982e8f492800dabecaa40e6165f932c8f876d3b1fcf70c70c17aafb9304a` (linha adulterada: gestor trocado) → `false`
7. Troque para outra conta no Remix e tente `alterarStatus`: a transação é revertida com `SomenteGestor`.

## Testes automatizados (Foundry)

```
forge install foundry-rs/forge-std --no-git
forge test -vv
```

10 testes: registro com prazo, protocolo duplicado, permissão de gestor e de prefeitura,
detecção de adulteração, "desfazer" sem apagar, status inválido, atraso público e ajuste de prazo.
