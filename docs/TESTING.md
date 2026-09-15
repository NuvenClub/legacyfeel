# TESTING — 15/09/2026

## Executado nesta sessão

- [x] Fontes 26.2 gerados por Loom; log: evidence/genSources-local.log.
- [x] Fontes 1.8.9 gerados por Legacy Loom; log: evidence/genSources-1.8.9.log.
- [x] Java25 e Gradle9.5.1: SHA-256 dos downloads conferidos.
- [x] Alvos de mixin conferidos por leitura dos fontes locais.
- [x] Consulta do Docker: daemon indisponível, nenhum servidor iniciado.

## Não executado

Todos os testes de gameplay, cliente, proxy, handshake, anticheat, FPS, Lunar e Feather abaixo estão pendentes. Não existe jar funcional. As fórmulas/confirmações de código não equivalem a testes em jogo. O checklist original está preservado; aplicar as correções de APPROVAL.md (notadamente dano bloqueado e pacotes) antes de executá-lo.

## 8. Plano de testes (preencher `docs/TESTING.md`)

### 8.1 Por feature do mod (repetir para cada # da §5.7)

- [ ] Toggle liga/desliga em runtime, sem reiniciar o jogo, efeito visível imediato.
- [ ] Preset `1.8` e `Vanilla` alternam corretamente.
- [ ] Gravação de 5 s (GIF/vídeo, `testserver/evidence/`) lado a lado: vanilla × mod × 1.8.9 real quando a animação existe na 1.8.9.
- [ ] **Teste de não-interferência:** com um teste automatizado ou log de debug, comparar `player.position()`, `player.getEyeHeight()`, `player.getDeltaMovement()` e a sequência de pacotes C2S (via logger de rede em dev) em um roteiro fixo de 30 s (agachar/levantar 10x, trocar slot 10x, 20 cliques bloqueando, 10 pulos) com mod **ligado** e **desligado**. Os valores devem ser idênticos tick a tick; os pacotes, idênticos em tipo e ordem.
- [ ] Zero flags no Grim em 10 min de: bridging agachado, w-tap, blockhit contínuo, troca rápida de slot, tomar dano em sequência, cair de altura.

### 8.2 Combate no servidor

- [ ] Log de debug do KB mostra vetores dentro do esperado; comparar com o KB da 1.8.9 (medir deslocamento em blocos após 1 hit parado e 1 hit em sprint, em superfície plana).
- [ ] Luta 1.8.9 × 26.2 vanilla × 26.2 mod: os três recebem o mesmo KB e o mesmo dano para o mesmo hit (log).
- [ ] Bloqueio: dano reduzido a 50% em todos os clientes; cliente 1.8.9 vê a espada bloqueando (o Via traduz o uso de item) — anotar se a pose aparece.
- [ ] Sem sweep: acertar dois alvos adjacentes não danifica o segundo.
- [ ] Sem cooldown: 2 hits em 1 s registram (limitados pelos 20 ticks de invulnerabilidade com regra dos 10 ticks).
- [ ] Grim: 10 min de luta real entre dois clientes sem flags de velocity/antikb (o KB custom é aplicado pelo servidor, então não deve flagar; se flagar, é bug nosso ou configuração do Grim — investigar antes de exemptar qualquer coisa).

### 8.3 Compatibilidade do mod

- [ ] Sodium; Iris com shader ativo; Mod Menu; Simple Voice Chat.
- [ ] Lunar Client add-on Fabric com "Use Lunar Features" **ligado** e **desligado** — anotar feature por feature o que o Lunar sobrescreve (esperado: algumas; ver aviso do Animatium).
- [ ] Feather Client e Modrinth App.
- [ ] Sem Fabric API instalada → erro claro do Loader, não crash silencioso.
- [ ] Servidor **sem** o plugin: mod entra em modo "sem suporte", nenhuma exceção no log, todas as features locais funcionam.

### 8.4 Performance

- [ ] FPS médio em `arena_flat` com 12 entidades jogador visíveis, mod ligado × desligado, 60 s cada; diferença ≤ 2%.
- [ ] Nenhuma alocação por frame nos mixins (verificar com o profiler da IDE ou `-XX:+UnlockDiagnosticVMOptions` + sampler simples).

### 8.5 Handshake

- [ ] `HELLO` chega e `WELCOME` volta em < 200 ms local.
- [ ] `POLICY` desliga uma feature e a tela de config mostra o motivo.
- [ ] Cliente 1.8.9 e vanilla nunca recebem payload nosso (verificar com log do Via/console).
- [ ] Reconexão via proxy para outro backend reenvia `HELLO`.
- [ ] Payload malformado (fuzz simples: 50 JSONs inválidos) não derruba o servidor nem o cliente.

### 8.6 QoL (§6.6) — repetir em cada um dos três clientes: 1.8.9 via Via, 26.2 vanilla, 26.2 + mod

- [ ] Clique direito segurando escudo (mão principal e secundária): **nada** acontece; no vanilla 26.2 o escudo não fica "levantado fantasma" nem o movimento cai para ×0,2 (armadilha 2 da §6.6.1).
- [ ] Agachar com escudo na mão: hit frontal de espada → dano bloqueado e som de bloqueio; hit por trás → dano normal; machado frontal → escudo desativado como no vanilla. Soltar o agachar → volta a tomar dano no tick seguinte.
- [ ] Observador 26.2 vê o escudo erguido do jogador agachado; anotar o que o observador 1.8.9 vê (ViaRewind).
- [ ] Atacar enquanto agacha com escudo: resultado idêntico nos três clientes, conforme a decisão do item 4 da §6.6.1 (log de dano).
- [ ] Grim: 5 min alternando agachar/levantar com escudo (≥ 200 vezes) + spam de clique direito no escudo → zero flags. Se flagar, investigar primeiro a prioridade do descarte do `RELEASE_USE_ITEM`.
- [ ] `POLICY` com `rules.shieldOnSneak: false` → o mod para de espelhar na hora e o servidor volta ao escudo vanilla (clique direito ativa).
- [ ] Troca de armadura: com peitoral equipado, clique direito com outro peitoral na mão → troca em 1 clique nos três clientes (no 1.8.9, conferir que os dois slots atualizam via `SetSlot`); repetir com capacete, calça, bota e elytra × peitoral.
- [ ] Troca de armadura continua funcionando **com todos os módulos ligados** (regressão contra cancelamento genérico de `PlayerInteractEvent`).

---
