# HANDOFF — 16/09/2026 — Legacy+ v2

## Estado

- Experimento isolado em `legacyfeel`; NuvenClub foi usado somente como referência.
- Laboratório Paper 26.2 permanece em `127.0.0.1:25575`.
- Mod final está instalado no Lunar Fabric 26.2 e abre até o menu.
- Plugin v2 foi copiado para o quick lab e será carregado no próximo reinício.

## Implementado

- combate, bloqueio, blockhit, animações 1.7, áudio da vara, tint e fogo baixo;
- câmera e pose de agachamento clássicas;
- handshake v2 compatível com v1;
- perfis `CLASSIC_PARITY`, `MODERN_LEGACY_COMBAT`, `SKYWARS_LAB` e `VANILLA_SAFE`;
- `rulesetId`, capabilities, indicador Legacy+ e simulador `/legacyfeel policy`;
- perfil e versões do handshake nos CSVs de combate;
- correção opt-in da previsão de blocos para backend 1.8.8;
- atraso de ACK restrito a sequências `use item on`, sem afetar mineração;
- timeout configurável de 1–10 ticks, padrão quatro, com rollback vanilla.

## Verificado

- plugin: testes e build Java 25 passaram;
- negociação v1/v2 e rejeição de versões inválidas têm testes unitários;
- mod Fabric compilou com Loom 1.17.21;
- Lunar 26.2 iniciou sem erro de mixin e sem conexão ao servidor;
- smoke server descartável abriu em `127.0.0.1:25577` com os sete plugins e
  ficou escutando; nenhum cliente conectou;
- quick lab ativo na 25575 não foi interrompido.

## Pesquisa de blocos fantasmas

- ViaVersion issue #3643 continua aberta;
- provider atual ainda sintetiza ACK em um tick para backend 1.8.8;
- configurações de block connections não tratam a ordem desse ACK;
- fontes e limites registrados em `RESEARCH-PVP-LEGACY.md` e `PROTOCOL.md`.

## Pendente de teste humano

- handshake visual dentro do jogo;
- ponte ninja e torre vertical com correção ligada/desligada;
- matriz de 0/50/100/200 ms e colocações rejeitadas;
- comparar 1.8.9 e 26.x no mesmo mapa;
- conferir Grim/fast-place e ajustar o padrão de quatro ticks com evidência;
- troca de backend real pelo Velocity.

## Próximos passos

1. Reiniciar o quick lab quando for conveniente e testar `/legacyfeel policy classic`.
2. Executar a matriz de ponte de `TESTING.md`, sem mudar pacotes C2S.
3. A partir dos resultados, fixar atraso por latência ou manter quatro ticks.
4. Criar o esqueleto separado do SkyWars Laboratório 26.x.
