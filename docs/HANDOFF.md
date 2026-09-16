# HANDOFF — 16/09/2026 — SkyWars Laboratório v1

## Estado

- Experimento isolado em `legacyfeel`; NuvenClub foi usado somente como referência.
- Laboratório Paper 26.2 permanece em `127.0.0.1:25575`.
- Mod final está instalado no Lunar Fabric 26.2 e abre até o menu.
- Plugin v2 foi copiado para o quick lab e será carregado no próximo reinício.
- Primeiro servidor nativo do SkyWars Laboratório preparado separadamente em
  `127.0.0.1:25576`, com pacote local em `127.0.0.1:25578`.

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
- bloqueio obrigatório pelo estado do pacote e comandos `/swlab`;
- kits Domínio, Reverso e Mirage com efeitos, sons e limpeza;
- núcleo 3D do Domínio, troca segura do Reverso e clone `Mannequin` do Mirage;
- quatro prévias de jaula e quatro cosméticos de projétil;
- pacote 26.2 reproduzível e catálogo visual versionado;
- integração do laboratório com LegacyFeel, OldCombatMechanics e PacketEvents;
- inicialização do LegacyFeel corrigida para funcionar sem PacketEvents quando
  a telemetria opcional não estiver instalada.

## Verificado

- plugin: testes e build Java 25 passaram;
- negociação v1/v2 e rejeição de versões inválidas têm testes unitários;
- mod Fabric compilou com Loom 1.17.21;
- Lunar 26.2 iniciou sem erro de mixin e sem conexão ao servidor;
- smoke server descartável abriu em `127.0.0.1:25577` com os sete plugins e
  ficou escutando; nenhum cliente conectou;
- quick lab ativo na 25575 não foi interrompido.
- build dos dois plugins e os testes unitários passaram em Java 25;
- ZIP do pacote foi gerado de forma determinística, com SHA-1
  `05e1c87ce77752532eade3fac9c39d6fb61f2c9d`;
- smoke integrado abriu na porta 25576 com os quatro plugins ativos, respondeu
  a `plugins`, `legacyfeel stats` e `help swlab`, e encerrou limpo;
- nenhum cliente entrou no servidor durante essa validação.

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
- aceite, download e renderização do pacote em cliente 26.2;
- posição e escala dos itens em primeira e terceira pessoa;
- Domínio com dois jogadores, inclusive destruição e remoção de vida máxima;
- Reverso em chão, borda, ar, vazio e perto de blocos;
- percepção, movimento e revelação do clone do Mirage;
- densidade de partículas nos modos completo e reduzido;
- arte final em Blockbench e áudio OGG próprio;
- ciclo de partida com arena, baús, morte, espectador, vitória e reset.

## Próximos passos

1. Reiniciar o quick lab quando for conveniente e testar `/legacyfeel policy classic`.
2. Executar a matriz de ponte de `TESTING.md`, sem mudar pacotes C2S.
3. A partir dos resultados, fixar atraso por latência ou manter quatro ticks.
4. No laboratório 26.x, validar pacote e os três kits com dois jogadores.
5. Corrigir leitura, colisão e limpeza encontradas no teste.
6. Implementar o ciclo mínimo de partida e substituir a arte provisória pelos
   modelos Blockbench e sons próprios aprovados.
