# PROMPT MESTRE — Projeto "LegacyFeel"
### Mod Fabric client-side + plugin Paper + servidor de testes para PvP com feeling 1.7.10/1.8.9 em Minecraft 26.2

> **Como usar:** cole este arquivo inteiro como primeira mensagem no Claude Code (ou outro agente), dentro de uma pasta vazia chamada `legacyfeel/`.
> Preencha os campos `[ ]` da seção 0 antes de colar. Tudo o que está marcado **(VERIFICAR)** foi levantado em setembro/2026 e pode ter mudado — a IA deve confirmar antes de usar.
>
> **Revisão 2 (15/09/2026):** incorporadas as duas únicas exceções ao 1:1, inspiradas no Bedrock — escudo ativado ao agachar e troca de armadura por clique direito (§2 "Princípio 1:1", §5.9, §6.6, §8.6) — e corrigidas referências a seções que não existiam.

---

## 0. Parâmetros do projeto (preencha)

| Campo | Valor |
|---|---|
| Nome do mod / id (minúsculo, sem espaço) | `[legacyfeel]` |
| Namespace do canal de rede | `[legacyfeel]` |
| Nome do servidor (para README) | `[SeuServidor]` |
| Versão alvo do Minecraft | `26.2` (ver §2 sobre 26.3) |
| Pacote Java base | `[br.com.seuservidor.legacyfeel]` |
| Licença do projeto | `[MIT / ARR / GPL-3.0 — ver §4.4 antes de decidir]` |
| Idioma dos comentários e docs | pt-BR (nomes de código em inglês) |

---

## 1. Quem você é e como deve agir

Você é o desenvolvedor principal de um projeto com três componentes acoplados: um **mod Fabric client-side**, um **plugin Paper** (backend) e um **ambiente de testes** (Velocity + Paper + anticheat). Você é sênior em Java, Mixin, Fabric Loom, Paper API e protocolo do Minecraft. Você trabalha sozinho neste repositório, mas outros desenvolvedores (humanos e IAs) continuarão o trabalho depois de você — então tudo que você descobre vai para arquivos, não fica só na conversa.

### 1.1 Princípios de conduta (obrigatórios)

1. **Nunca invente nomes de classes, métodos ou campos.** O código do Minecraft muda a cada versão. Antes de escrever qualquer `@Mixin`, abra os fontes decompilados gerados pelo Loom (§4.2) e confirme o nome exato, a assinatura e o que o método faz. Nas explicações, cite caminho do arquivo e linha.
2. **Pesquise antes de codar.** A seção §4 lista o que buscar. Se um link estiver morto ou a informação divergir, registre em `docs/RECON.md` o que encontrou de fato.
3. **Peça aprovação em dois momentos:** (a) após o reconhecimento, com a tabela de mixins prevista; (b) antes de adicionar qualquer dependência fora da lista permitida (§5.2). Fora disso, avance sozinho.
4. **Uma feature por commit**, com mensagem no formato `feat(camera): sneak instantâneo (Camera#tick)`. Após cada feature: diff resumido, o que testou, o que ficou pendente.
5. **Honestidade sobre incerteza.** Se não conseguiu testar algo (ex.: não tem conta Microsoft para abrir o Lunar), diga "não testado" em vez de "funciona". Nunca marque um item do plano de testes como feito sem executá-lo.
6. **Regra de ouro do mod (§5.1) prevalece sobre qualquer pedido de feature.** Se uma feature só é possível violando a regra, pare e proponha a versão puramente visual.
7. **Não copie código de terceiros sem checar a licença** (§4.4). Estude, entenda, reimplemente. Se copiar algo GPL, o projeto inteiro vira GPL — avise o humano antes.
8. **Não redistribua assets da Mojang** (sons/texturas extraídos de versões antigas) dentro do mod. Veja §5.6.
9. **Time-box:** se uma feature travar por mais de ~2 horas de tentativa, documente o bloqueio em `docs/BLOCKERS.md`, deixe o toggle desabilitado por padrão e siga para a próxima.
10. **Ao final de cada sessão**, atualize `docs/HANDOFF.md` (§10) para que a próxima IA retome sem reler a conversa.

### 1.2 Ordem de trabalho

```
Fase 0  Ler este documento inteiro. Criar a estrutura de pastas (§3.2).
Fase 1  Reconhecimento (§4) → escrever docs/RECON.md e docs/MIXINS.md (previsto) → PARAR e pedir aprovação.
Fase 2  Ambiente de testes (§7) → subir Velocity + Paper + Grim + Via localmente → provar que um cliente vanilla 26.2 e um 1.8.9 entram.
Fase 3  Plugin Paper — módulo handshake (§6.2) + detecção de modo (§6.3).
Fase 4  Mod Fabric — esqueleto, config, handshake (§5.3, §5.4, §5.5). Provar handshake ponta a ponta.
Fase 5  Mod Fabric — features visuais na ordem da §5.7, uma por commit, testando contra o Grim a cada uma.
Fase 6  Plugin Paper — módulo de combate legado (§6.4) + módulo QoL (§6.6).
Fase 7  Testes completos (§8), README, empacotamento, HANDOFF.
```

---

## 2. Visão geral do sistema

```
 Jogador 1.8.9 (Lunar/Badlion 1.8)      Jogador 26.2 vanilla       Jogador 26.2 + mod (Lunar Fabric / Feather / Modrinth App)
          │                                     │                                  │
          └──────────────── TCPShield / DNS ────┴──────────────────────────────────┘
                                                │
                                        Velocity (proxy)            ← SEM ViaVersion aqui (exigência do Grim)
                                                │
                                        Paper 26.2 (backend)        ← ViaVersion + ViaBackwards + ViaRewind
                                          ├─ GrimAC + PacketEvents      (aqui, e só aqui)
                                          ├─ LegacyFeel-Server (plugin deste projeto)
                                          │    ├─ handshake   (canal legacyfeel:handshake)
                                          │    ├─ modes       (MOD / VIA_LEGACY / VANILLA_MODERN)  → API para outros plugins
                                          │    ├─ combat      (KB 1.8, sem cooldown, sem sweep, blockhit via blocks_attacks)
                                          │    ├─ items       (espadas com componente blocks_attacks; kits de teste)
                                          │    └─ qol         (§6.6: escudo só ao agachar; troca de armadura por clique direito mantida)
                                          └─ (futuro) plugins de fila/minigame usam LegacyFeelAPI
```

**Princípio central:** todo o combate é calculado no servidor, uma vez, igual para os três tipos de cliente. O mod **só muda o que o jogador vê e ouve**. Assim uma luta entre jogador 1.8.9 e jogador moddado é justa naquilo que o servidor controla, e o mod vende a experiência (bloqueio com espada, câmera rápida, animações antigas, FPS) sem dar vantagem mecânica.

**Princípio 1:1:** na dúvida, o comportamento é o da 1.8.9 (perfil `1.8`) ou o da 1.7.10 (perfil `1.7`), medido nos fontes decompilados dessas versões — não "parecido", não "de memória". As **únicas** exceções deliberadas são as duas melhorias de qualidade de vida da §6.6, inspiradas no Bedrock: (1) escudo ativado **ao agachar**, nunca pelo clique direito; (2) troca de armadura por clique direito quando o slot já está ocupado. As duas foram escolhidas porque não existiam na 1.8.9 (o escudo nem existia) e não tiram nada de quem joga "no modo 1.8.9". Qualquer outra exceção ao 1:1 precisa de aprovação explícita do humano antes de ser codada.

### 2.1 Sobre a versão alvo

- Alvo: **Minecraft 26.2**. O Fabric anunciou que o 26.3 sai em 15/09/2026 e traz mudanças que afetam a maioria dos devs de mods, e a partir do 26.2 o backend gráfico começa a migrar de OpenGL para Vulkan; mods que usam chamadas OpenGL cruas precisam migrar para a API Blaze3D. **Nossos mixins não tocam em GL**, mas isso é um motivo a mais para não fazer render manual.
- **Decisão:** desenvolver em 26.2 (Fabric API, Paper, Grim, Via já estáveis) e só portar para 26.3 quando Fabric API + Paper + Grim + ViaVersion tiverem builds estáveis. Registrar em `docs/PORTING.md` o que mudou quando portar. O guia oficial de porting fica em `https://docs.fabricmc.net/develop/porting/`.

---

## 3. Stack e estrutura do repositório

### 3.1 Versões de referência **(VERIFICAR todas em fabricmc.net/develop e Modrinth antes de fixar)**

| Componente | Versão vista em set/2026 | Onde confirmar |
|---|---|---|
| Minecraft | 26.2 | — |
| Fabric Loader | 0.19.3 / 0.19.4 | github.com/FabricMC/fabric-loader/releases |
| Fabric API | 0.152.2+26.2 ou 0.153.0+26.2 | modrinth.com/mod/fabric-api |
| Fabric Loom | 1.15+ (26.1 pedia Loom 1.15, Gradle 9.4) | fabricmc.net/develop |
| Gradle | 9.4+ | idem |
| Java (Gradle JVM) | 25+ (exigido desde 26.1) | idem |
| IntelliJ IDEA | 2025.3+ (mixins) | idem |
| Mappings | **Mojang official** (`loom.officialMojangMappings()`) — a documentação Fabric 26.x usa nomes Mojang | fabricmc.net/2026/03/14/261.html |
| YACL (config) | versão para 26.2 | modrinth.com/mod/yacl |
| Cloth Config (fallback) | 26.2.155+ | modrinth.com/mod/cloth-config |
| Mod Menu | 20.0.0-beta.3+ | modrinth.com/mod/modmenu |
| Sodium / Iris (só para teste de compat) | versões 26.2 | Modrinth |
| Paper | build 26.2 | papermc.io/downloads |
| Velocity | compatível 1.7.2–26.2 | docs.papermc.io/velocity |
| ViaVersion + ViaBackwards + ViaRewind | builds 26.2 (ViaRewind é o addon que permite clientes 1.8.x/1.7.x) | hangar.papermc.io/ViaVersion |
| GrimAC | suporta 1.8–26.2; Java 17+; exige PacketEvents | github.com/GrimAnticheat/Grim |
| PacketEvents | versão exigida pelo Grim | github.com/retrooper/packetevents |

### 3.2 Estrutura de pastas (crie na Fase 0)

```
legacyfeel/
├── README.md                     ← visão geral + como instalar (jogador) + como desenvolver
├── docs/
│   ├── RECON.md                  ← resultado do reconhecimento (§4), com links e datas
│   ├── MIXINS.md                 ← tabela: mixin → classe/método alvo → tipo de injeção → por que é só render
│   ├── PROTOCOL.md               ← especificação do handshake (§5.5)
│   ├── COMBAT.md                 ← especificação do combate legado (§6.4) com valores
│   ├── TESTING.md                ← plano de testes preenchido (§8)
│   ├── BLOCKERS.md               ← o que travou e por quê
│   ├── PORTING.md                ← notas para 26.3+
│   └── HANDOFF.md                ← estado atual para a próxima IA (§10)
├── mod/                          ← projeto Fabric (Gradle independente)
│   ├── build.gradle / gradle.properties / settings.gradle
│   └── src/client/java/[pacote]/...
├── plugin/                       ← projeto Paper (Gradle independente, paperweight-userdev)
│   └── src/main/java/[pacote]/server/...
├── testserver/                   ← ambiente de testes (§7)
│   ├── docker-compose.yml
│   ├── velocity/  (velocity.toml, forwarding.secret)
│   ├── paper/     (paper-global.yml, spigot.yml, plugins/)
│   └── scripts/   (up.sh, down.sh, logs.sh, install-plugins.sh)
└── reference/                    ← SOMENTE leitura; clones dos mods de referência (§4.3), fora do build
```

---

## 4. Fase 1 — Reconhecimento (o que buscar antes de escrever código)

Registre tudo em `docs/RECON.md` com data e URL. Não avance para código sem terminar esta fase.

### 4.1 Toolchain Fabric

1. Gere um projeto base com o gerador oficial: `https://fabricmc.net/develop/template/` (escolha 26.2, Mojang mappings, **split client/common sources** — o mod é `environment: client`). Se o gerador não oferecer 26.2, use `https://github.com/FabricMC/fabric-example-mod` e ajuste `gradle.properties` com as versões de `https://fabricmc.net/develop/`.
2. Leia a documentação em `https://docs.fabricmc.net/develop/`: seções *Getting Started*, *Mixins* (incl. MixinExtras — o Loader já embute), *Networking* (custom payloads com `StreamCodec`, `PayloadTypeRegistry`, `ClientPlayNetworking`), *Events*, *Rendering* (o que mudou em 26.x).
3. Leia `https://docs.fabricmc.net/develop/porting/` (26.1→26.2) e o post `https://fabricmc.net/2026/03/14/261.html` para entender as mudanças de render/storage recentes.
4. Confirme a licença dos Mojang mappings permite uso em mods (permite; registre a nota).

### 4.2 Fontes do Minecraft (a única forma legítima)

O Minecraft **não é open source**. O que existe é a decompilação local para desenvolvimento de mods, feita pelo Loom:

```bash
cd mod && ./gradlew genSources
# fontes aparecem em .gradle/loom-cache/... ou via "Download Sources" na IDE
```

Use `grep -rn` nesses fontes para localizar cada alvo de mixin. Classes que você vai procurar (nomes Mojang da era 1.21; **podem ter mudado em 26.2 — confirme cada um**):

| Comportamento | Onde procurar (ponto de partida) | O que confirmar |
|---|---|---|
| Suavização da câmera ao agachar | `net.minecraft.client.Camera` → `tick()` e `setup(...)`. Na era 1.21, `Camera#tick` fazia `eyeHeight += (entity.getEyeHeight() - eyeHeight) * 0.5F` e `setup` interpolava `eyeHeightOld→eyeHeight` | Se a suavização ainda está aqui (classe 100% client/render — alvo ideal) |
| Animação de equipar item | `net.minecraft.client.renderer.ItemInHandRenderer` → `tick()` (campos tipo `mainHandHeight`/`oMainHandHeight`, incremento ~0.4/tick) e `renderHandsWithItems` | Nomes dos campos; se a comparação de item mudou (durabilidade/componentes disparando a animação) |
| Swing da mão, pose de bloqueio | `ItemInHandRenderer#renderArmWithItem`, `applyItemArmTransform`, `applyItemArmAttackTransform`; `LivingEntity#getCurrentSwingDuration` (client-side só para render) | Onde a pose `BLOCK` (`ItemUseAnimation.BLOCK`) é renderizada em 1ª pessoa |
| Uso de item como bloqueio | Componente de dados `minecraft:blocks_attacks` (introduzido em 1.21.5): item que bloqueia como escudo e usa animação `BLOCK` | Como o cliente decide renderizar a pose de bloqueio para uma espada com esse componente |
| View bobbing / hurt cam | `net.minecraft.client.renderer.GameRenderer` → `bobView(...)`, `bobHurt(...)`; opções vanilla `bobView`, `damageTiltStrength` | Se `bobView` ainda afeta mão e câmera juntos |
| Flash vermelho ao tomar dano | `LivingEntityRenderer#getOverlayCoords` / `OverlayTexture` com `hurtTime` | Nome atual |
| FOV de sprint | `AbstractClientPlayer#getFieldOfViewModifier` (ou equivalente em 26.x) e opção `fovEffectScale` | Se ainda existe e como é aplicado |
| Modelo em 3ª pessoa agachado | `HumanoidModel#setupAnim` / `PlayerModel`, campo `crouching`; `PlayerRenderer` | Se há interpolação ou é binário (se for binário, feature 2 da §5.7 é dispensável) |
| Penalidade de miss (10 ticks) | `Minecraft#startAttack` → `missTime = 10` ao errar | **Não remover**: a 1.8.9 tinha a mesma penalidade (`leftClickCounter = 10`). Documentar e deixar quieto |
| Estado "usando item" (para o escudo ao agachar, §6.6.1) | `LivingEntity#startUsingItem/stopUsingItem/isUsingItem`, flag em `DATA_LIVING_ENTITY_FLAGS` e `onSyncedDataUpdated` (cliente copia `useItem` ao receber o flag); `Minecraft#handleKeybinds` (se `isUsingItem()` e `keyUse` **não** pressionada → `releaseUsingItem` → pacote `RELEASE_USE_ITEM`); `LocalPlayer#aiStep` (input ×0,2 enquanto usa item) | Confirmar os três comportamentos do cliente: auto-release, cópia do `useItem` ao receber entity data, e redução de movimento. Eles definem por que o espelho do mod é **só render** (§5.9) e o que o servidor precisa descartar (§6.6.1) |
| Troca de armadura por clique direito (§6.6.2) | Componente `minecraft:equippable` (campo `swappable`, padrão `true`; 1.21.2+) e o `use` da peça de armadura — comportamento vanilla desde a 1.19.4 | Confirmar que a troca é feita no servidor e replicada ao cliente por `SetSlot` (então funciona para o cliente 1.8.9 via Via). **Nada a fazer no mod** |
| Registro de canal de plugin | Como o cliente anuncia canais (`minecraft:register`) e limites de tamanho de payload | Para §5.5 |

**Referência da 1.8.9 (recomendado):** para comparar animações "de verdade", monte um segundo projeto Loom apontando para 1.8.9 com mappings do **Legacy Fabric** (`https://legacyfabric.net/`) e rode `genSources`. Classes-alvo na 1.8.9: `EntityRenderer` (bobbing, hurt cam, FOV), `ItemRenderer` (equip progress, swing, block pose), `Minecraft#clickMouse`. Anote em `docs/RECON.md` as fórmulas de cada animação na 1.8.9 para reproduzi-las com fidelidade.

### 4.3 Mods de referência (clonar em `reference/`, ler, não copiar)

| Repositório | O que aprender | Licença |
|---|---|---|
| `https://github.com/Legacy-Visuals-Project/Animatium` | O conjunto mais completo de animações 1.7/1.8 para versões modernas; config em YACL; lista de features (inclui toggle de miss penalty, item positions antigas etc.) | **GPL-3.0 + Minecraft Linking Exception** → copiar código torna nosso projeto GPL |
| `https://github.com/lowercasebtw/old-animations` | Mixins de animações legadas para 1.21+. Atenção: o README alerta que a "mecânica antiga de sneak" pode ser banível em servidores 1.20+ — exatamente o que **não** faremos | verificar |
| `https://github.com/PvPLand/LegacyBlocking` | Mod pequeno: não cancela o swing progress ao bloquear e remove a animação de equipar quando a espada sobe. Modelo limpo de mixin só-render | verificar |
| `https://github.com/Legacy-Visuals-Project/Animatium-Legacy` (ex-OverflowAnimations, 1.8.9) | Referência de como as animações eram na própria 1.8.9 | LGPL-3.0 |

Para cada um, escreva em `docs/RECON.md`: quais classes/métodos eles mixam, o que faremos igual, o que faremos diferente, e por quê.

**Nota sobre Lunar:** o README do Animatium avisa que com Lunar Client algumas features não funcionam e que o Lunar tem prioridade em alguns lugares sobre configurações ligadas/desligadas. Espere o mesmo conosco; isso vai para os testes de compatibilidade (§8.3).

### 4.4 Decisão de licença (apresentar ao humano na aprovação da Fase 1)

- Se o humano quiser o mod **fechado ou MIT**: reimplemente tudo do zero a partir dos fontes decompilados e da leitura dos mods de referência (aprender é permitido; copiar não).
- Se o humano aceitar **GPL-3.0**: pode-se forkar o Animatium, remover o que não interessa e adicionar as features nossas; ganha-se meses de trabalho, mas o código tem de ficar público.
- Em ambos os casos, **os assets da Mojang não podem ser redistribuídos** (§5.6).

### 4.5 Lado servidor

1. Paper: `https://docs.papermc.io/paper/dev/getting-started/` (paperweight-userdev, `paper-plugin.yml`), *Plugin Messaging* (`Messenger`, `PluginMessageListener`, `PlayerRegisterChannelEvent`), *Data Components* (`DataComponentTypes.BLOCKS_ATTACKS`, builder `BlocksAttacks`) **(VERIFICAR nomes no Javadoc 26.2)**, eventos `EntityDamageByEntityEvent`, `EntityKnockbackByEntityEvent` (Paper), `PlayerInteractEvent`, atributos (`Attribute.ATTACK_SPEED`; foi renomeado de `GENERIC_ATTACK_SPEED` em 1.21.3+ — confirmar nome em 26.2). Para a §6.6: `PlayerToggleSneakEvent`, `PlayerSwapHandItemsEvent`, `DataComponentTypes.EQUIPPABLE`, e se `LivingEntity`/`HumanEntity` do Paper 26.2 expõe algo como `startUsingItem` (em 1.21.x havia `getActiveItem`/`clearActiveItem`/`completeUsingActiveItem`, mas não "iniciar uso" — se continuar assim, é NMS via paperweight).
2. Velocity: `https://docs.papermc.io/velocity/` — *modern forwarding*, `velocity.toml`, e `https://docs.papermc.io/velocity/server-compatibility/`.
3. Grim: README/wiki em `https://github.com/GrimAnticheat/Grim` — **ViaVersion deve ficar apenas no backend onde o Grim está**, nunca no proxy; requer PacketEvents; ler como configurar flags/alertas e como consultar violações para os testes (§8.2).
4. ViaVersion/ViaBackwards/ViaRewind: `https://hangar.papermc.io/ViaVersion`, `https://github.com/ViaVersion/ViaVersion`, docs em `https://docs.viaversion.com`. Existe issue aberta (ViaBackwards #1308) sobre erro de `custom_payload` com cliente 26.1.2 em servidor 26.2 — relevante para nosso handshake; verifique se já foi corrigido e, de qualquer forma, **só envie payload para clientes que registraram o canal** (§5.5).
5. API do ViaVersion para saber a versão do protocolo de cada jogador (`Via.getAPI().getPlayerVersion(...)`) — base da detecção de modo (§6.3).
6. Docker: `https://docker-minecraft-server.readthedocs.io/` (imagens `itzg/minecraft-server` e `itzg/mc-proxy`) — confirmar variáveis para `TYPE=PAPER`, `VERSION`, instalação de plugins por URL/Modrinth, e `TYPE=VELOCITY`.

### 4.6 Distribuição (só ler; executar na Fase 7)

- Lunar Client carrega mods Fabric de terceiros pelo add-on Fabric (1.16.5+), arrastando o `.jar` na aba Mods do seletor de versão: `https://www.lunarclient.com/news/how-to-add-your-own-mods-to-lunar-client`.
- Modrinth: publicar como **mod** e como **modpack** (mod + Sodium + servidor pré-adicionado). O formato `.mrpack` é lido por Lunar, Modrinth App, Prism e Feather.

---

## 5. Componente A — Mod Fabric (`mod/`)

### 5.1 Regra de ouro (não negociável)

O mod só altera **renderização, câmera, animação, som, HUD e o canal de handshake**. Ele **nunca** altera:

- posição, velocidade, hitbox, `eyeHeight` real da entidade, gravidade, estado real de sprint/sneak;
- timing, frequência ou conteúdo de pacotes de movimento, ataque, interação ou uso de item;
- reach, cooldown de ataque, dano, knockback, penalidade de miss.

Teste mental antes de cada mixin: *"se eu remover este mixin, algum pacote enviado ao servidor muda?"* Se a resposta for sim, o mixin está errado. Uma flag no Grim causada pelo mod é bug crítico e bloqueia o merge.

Consequência prática para a câmera: a câmera renderizada pode "chegar antes" da altura real do olho por alguns ticks; a mira (raycast) continua usando a altura real. Isso é aceitável e é o mesmo compromisso que os mods de referência fazem.

**Única exceção controlada — "remapeamento de input autorizado pelo servidor" (§5.9 / §6.6):** o servidor é quem implementa os comportamentos de qualidade de vida inspirados no Bedrock (ex.: escudo ativado ao agachar). O mod pode *espelhar* a mesma regra no cliente **apenas para previsão visual**, e **apenas quando o servidor declarou a regra em `rules` do `WELCOME`/`POLICY`**. Nesse espelhamento o mod pode deixar de enviar um pacote que o servidor iria rejeitar de qualquer forma (ex.: `use_item` do escudo por clique direito), mas nunca cria pacote novo, nunca altera timing de nada e nunca produz efeito que o servidor não aplicaria sozinho a um cliente vanilla. Em particular, o espelho **nunca coloca a entidade local em estado real de uso de item** (`startUsingItem`): isso reduz a velocidade de movimento, engole cliques de ataque e dispara o auto-release — é pose de render, não estado (§5.9). Em servidor sem o plugin, esses espelhamentos ficam desligados à força.

### 5.2 Dependências permitidas

Fabric API, YACL (ou Cloth Config se YACL não tiver build 26.2), Mod Menu. Qualquer outra: pedir aprovação. Sodium e Iris entram **só** como `modRuntimeOnly`/`modCompileOnly` para testar compatibilidade, nunca como dependência declarada.

### 5.3 `fabric.mod.json` e Gradle

- `"environment": "client"`, `"depends": { "fabricloader": ">=0.19.3", "minecraft": "~26.2", "fabric-api": "*" }` **(VERIFICAR sintaxe de range para versões 26.x)**.
- `entrypoints.client` → `LegacyFeelClient`; `entrypoints.modmenu` → tela de config.
- `mixins`: `[nome].client.mixins.json` com `"required": true`, `"minVersion": "0.8"`, pacote `[pacote].mixin`, e MixinExtras habilitado.
- Java: usar a versão que o Loom/26.2 exigir (registrar em RECON.md).

### 5.4 Configuração

- Biblioteca: YACL. Arquivo `config/[nome].json`. Recarregável em runtime (nenhuma feature pode exigir reiniciar).
- Grupos: **Câmera**, **Mão e itens**, **Combate (visual)**, **Áudio**, **Servidor/Debug**.
- Cada feature da §5.7 é um toggle independente; parâmetros numéricos onde indicado.
- Presets de um clique: `1.7`, `1.8`, `Vanilla`. Preset apenas define toggles; o jogador pode ajustar depois.
- Item de debug: mostrar no F3 uma linha `LegacyFeel: <modo do servidor> <features ativas>`.
- **Política do servidor:** o servidor pode enviar (§5.5) uma lista de features que ele **força desligadas**. A tela mostra essas opções acinzentadas com o texto "desativado por este servidor". O jogador nunca pode sobrescrever a política enquanto conectado àquele servidor.

### 5.5 Handshake com o servidor (especificar em `docs/PROTOCOL.md`)

- Canal: `[namespace]:handshake` (um só canal; nada mais trafega por ele).
- Implementação Fabric: `CustomPacketPayload` + `StreamCodec` registrados com `PayloadTypeRegistry` (C2S e S2C), envio com `ClientPlayNetworking`. Corpo: JSON UTF-8 compacto (simples de debugar dos dois lados). Limite: manter abaixo de 8 KB.
- Fluxo:
  1. Cliente, em `ClientPlayConnectionEvents.JOIN` (ou equivalente em 26.2), envia `HELLO`.
  2. Servidor responde `WELCOME` (ou não responde, se o plugin não estiver instalado — o mod então opera em modo "servidor sem suporte" e não envia mais nada).
  3. Servidor pode enviar `POLICY` a qualquer momento (ex.: ao trocar de arena via proxy o backend novo manda a política dele).
  4. Cliente reenvia `HELLO` a cada troca de servidor pelo proxy (cada backend tem seu plugin).
- Mensagens (campo `t` = tipo, `v` = versão do protocolo, começa em 1):

```json
// C2S
{"t":"HELLO","v":1,"mod":"1.0.0","mc":"26.2","loader":"fabric","features":{"instantSneakCamera":true,"legacyBlocking":true,"...":false}}
// S2C
{"t":"WELCOME","v":1,"server":"[SeuServidor]","plugin":"1.0.0","rules":{"shieldOnSneak":true,"armorSwap":true}}
{"t":"POLICY","v":1,"forceOff":["exampleFeature"],"rules":{"shieldOnSneak":false},"reason":"ranked"}
```

- `rules` = regras de servidor da §6.6 que o mod pode espelhar (§5.9). Campo ausente ou regra ausente ⇒ tratar como `false`. `POLICY` substitui apenas as chaves que trouxer.

- O mod **detecta** se está no Lunar/Feather apenas de forma passiva e opcional (ex.: `FabricLoader.getInstance().isModLoaded(...)` de ids conhecidos, se existirem). Não faz fingerprinting invasivo; se não souber, manda `"client":"unknown"`. Registrar em RECON.md o que foi encontrado.
- Nunca enviar dados pessoais, hardware ID ou lista de mods completa.

### 5.6 Assets e sons

- Sons antigos de hit/dano são propriedade da Mojang e **não podem ser embutidos** no jar. Estratégia permitida: um resource pack embutido com **apenas** `sounds.json` remapeando eventos para arquivos de som **que ainda existam** nos assets atuais do jogo (verificar se os sons de dano/hit da 1.8 ainda estão no índice de assets 26.2; se não estiverem, a feature de áudio vira "usar sons alternativos livres" ou fica fora de escopo). Documentar a decisão.
- Texturas: nenhuma. O mod não altera texturas.

### 5.7 Features (ordem de implementação; cada uma com toggle)

| # | Feature | O que fazer | Onde (confirmar em §4.2) | Fidelidade 1.8.9 |
|---|---|---|---|---|
| 1 | **Câmera de agachar instantânea** | Substituir a suavização da altura do olho na câmera por atribuição direta da altura alvo da pose atual. Nada em `Entity`/`Player`. | `Camera#tick` (+ `setup` se necessário) | 1.8.9 não tinha suavização: câmera "pula" 0,08 bloco imediatamente |
| 2 | **Modelo 3ª pessoa sem interpolação de sneak** | Se houver interpolação no modelo/pose de outros jogadores, torná-la binária. Se já for binária, marcar "N/A" e pular. | `HumanoidModel`/`PlayerModel`/`PlayerRenderer` | Binário |
| 3 | **Equipar item rápido** | Ajustar a velocidade da subida da mão ao trocar de slot; opção `instant` e opção `ticks` (padrão = comportamento 1.8.9 medido no projeto Legacy Fabric). Só afeta render. | `ItemInHandRenderer#tick` | Na 1.8.9 a subida era rápida e só disparava ao trocar o item, não ao mudar durabilidade |
| 4 | **Bloqueio de espada legado (visual)** | Quando o servidor der às espadas o componente `blocks_attacks` (§6.4), o cliente já entra em pose de uso. Aqui: (a) renderizar a pose 1.8 de bloqueio (espada diagonal na frente) em 1ª e 3ª pessoa; (b) **não** cancelar o swing progress ao bloquear (blockhit fica fluido); (c) não disparar animação de equipar quando a espada sobe/desce do bloqueio. Referência: LegacyBlocking. | `ItemInHandRenderer#renderArmWithItem`, `applyItemArmTransform`, renderer de 3ª pessoa da pose `BLOCK` | Reproduzir as transformações da `ItemRenderer` 1.8.9 (rotações/translação da pose de bloqueio) |
| 5 | **Swing 1.7/1.8** | Curva e amplitude do swing da mão iguais à 1.8.9 (e variante 1.7). Não mexer em `swingTime` da entidade. | `ItemInHandRenderer` (funções de transform do ataque) | Copiar fórmulas da `ItemRenderer` 1.8.9 (sin/sqrt do progresso) |
| 6 | **View bobbing legado** | Bobbing da mão e da câmera com a fórmula da 1.8.9; sub-toggles "só mão" / "só câmera". | `GameRenderer#bobView` e onde a mão aplica bobbing | `EntityRenderer#setupViewBobbing` da 1.8.9 |
| 7 | **Hurt cam e flash de dano** | Toggle A: remover tilt da câmera ao tomar dano (verificar se basta forçar `damageTiltStrength = 0` ou se precisa de mixin em `bobHurt`). Toggle B: manter/remover o overlay vermelho na entidade. | `GameRenderer#bobHurt`, `LivingEntityRenderer#getOverlayCoords` | 1.8.9 tinha ambos; aqui o objetivo é *limpeza*, então os padrões dos presets 1.7/1.8 podem manter o flash e remover o tilt |
| 8 | **FOV de sprint** | Toggle + multiplicador; preset 1.8 igual ao modificador da 1.8.9. Só afeta o cálculo de FOV do render. | `AbstractClientPlayer#getFieldOfViewModifier` (ou equivalente) | `EntityPlayerSP#getFovModifier` da 1.8.9 |
| 9 | **Posição/escala de itens na mão 1.8** | Itens maiores/mais próximos como na 1.8. Opcional; baixa prioridade. | `ItemInHandRenderer` transforms | `ItemRenderer` 1.8.9 |
| 10 | **Sons legados** | Conforme §5.6. Opcional. | `sounds.json` embutido | — |
| 11 | **Handshake** | §5.5. Implementar antes das features visuais (Fase 4). | `network/` | — |
| 12 | **Espelho visual do "escudo ao agachar"** (§5.9) | Só quando o servidor declarar `rules.shieldOnSneak: true`: (a) enquanto o jogador local agacha com escudo na mão principal ou secundária, **renderizar** a pose de escudo erguido em 1ª pessoa e em F5 — **sem** chamar `startUsingItem` (isso reduziria a velocidade ×0,2, engoliria cliques de ataque e dispararia o auto-release: mudaria mecânica e pacotes, violando §5.1); (b) ao soltar o agachar, voltar à pose normal; (c) em `Minecraft#startUseItem`, pular o escudo (não prever o uso e não enviar `use_item` dele — o servidor negaria de qualquer forma). Nenhum outro item é afetado. | `ItemInHandRenderer#renderArmWithItem` (pose do escudo em 1ª pessoa), `PlayerRenderer#getArmPose`/`HumanoidModel` (pose em F5), `Minecraft#startUseItem`, `LocalPlayer#isShiftKeyDown` (só leitura) | Não existe na 1.8.9; comportamento Bedrock (§6.6.1), adotado para não roubar o clique direito da espada |

**Explicitamente fora de escopo do mod** (registrar em README para evitar pedidos futuros): mecânica antiga de sneak, hit delay, reach, "hit select", auto-clicker, remoção da penalidade de miss, qualquer alteração de pacotes fora da exceção da §5.1.

### 5.8 Estrutura do código do mod

```
mod/src/client/java/[pacote]/
  LegacyFeelClient.java              entrypoint: carrega config, registra payloads, eventos de join
  config/LegacyFeelConfig.java       modelo + load/save JSON
  config/LegacyFeelConfigScreen.java YACL
  config/ServerPolicy.java           estado da política recebida do servidor (por conexão): forceOff + rules (§5.5/§5.9)
  network/HandshakePayloads.java     CustomPacketPayload + codecs
  network/HandshakeClient.java       envio de HELLO, recepção de WELCOME/POLICY
  feature/Features.java              enum com id estável de cada feature (usado no JSON e na config)
  feature/ShieldSneakMirror.java     estado do espelho da feature 12: "renderizar escudo erguido agora?" (só leitura de isShiftKeyDown + mão)
  mixin/camera/CameraMixin.java                       (feature 1)
  mixin/model/HumanoidModelMixin.java                 (feature 2, se aplicável)
  mixin/hand/ItemInHandRendererMixin.java             (features 3, 4, 5, 6-mão, 9, 12 em 1ª pessoa)
  mixin/render/GameRendererMixin.java                 (features 6-câmera, 7A)
  mixin/render/LivingEntityRendererMixin.java         (feature 7B)
  mixin/render/PlayerRendererMixin.java               (feature 12 em F5: pose de braço BLOCK)
  mixin/player/AbstractClientPlayerMixin.java         (feature 8)
  mixin/input/MinecraftMixin.java                     (feature 12: pular o escudo em startUseItem quando a regra está ativa)
  debug/DebugOverlay.java            linha no F3
mod/src/client/resources/
  fabric.mod.json
  [nome].client.mixins.json
  assets/[nome]/lang/pt_br.json, en_us.json
  assets/[nome]/icon.png
```

Todo mixin deve: ter `@Unique` em campos/métodos próprios; preferir `@ModifyExpressionValue`/`@WrapOperation` (MixinExtras) a `@Overwrite`; ler a config **uma vez por frame** via um holder estático já resolvido (nunca `JSON.parse` no hot path); e ter um comentário de 2–4 linhas em pt-BR explicando *por que é só render*.

### 5.9 Espelho visual das regras do servidor (§6.6) — o que o mod pode e não pode fazer

O servidor é o único dono das duas exceções ao 1:1 (§6.6). O mod **nunca as implementa**; ele apenas *prevê visualmente* o que o servidor vai fazer, e só quando `WELCOME`/`POLICY` trouxe `rules.<regra>: true`. Sem plugin, sem regra, sem espelho.

| Regra do servidor | O que o mod faz | O que o mod NÃO faz |
|---|---|---|
| `shieldOnSneak` (§6.6.1) | Feature 12: pose de escudo erguido em 1ª pessoa/F5 enquanto agacha com escudo na mão; não prevê nem envia `use_item` do escudo por clique direito | Não chama `startUsingItem`/`stopUsingItem` na entidade local; não toca em `handleKeybinds`, movimento, cliques de ataque ou no pacote `RELEASE_USE_ITEM`. Se o servidor forçar o estado de uso e o cliente vanilla responder com `RELEASE_USE_ITEM`, o mod **deixa acontecer igual** — o fluxo de pacotes tem de ser idêntico ao vanilla; quem descarta é o servidor |
| `armorSwap` (§6.6.2) | Nada. A troca já é comportamento vanilla do cliente 26.2 | Não bloqueia nem "restaura o 1.8": nenhum mixin em `Item#use` de armadura ou no componente `equippable` |

Teste mental adicional para esta seção: *"um cliente vanilla 26.2 neste mesmo servidor, com o mesmo input, enviaria exatamente os mesmos pacotes na mesma ordem?"* Se não, o espelho está errado.

---

## 6. Componente B — Plugin Paper `LegacyFeel-Server` (`plugin/`)

### 6.1 Plugins de terceiros que o servidor precisa (instalar no backend Paper)

| Plugin | Papel | Observações |
|---|---|---|
| **ViaVersion + ViaBackwards + ViaRewind** | Deixar clientes 1.8.9 (e 1.7.10) entrarem no backend 26.2 | **Só no backend**, nunca no Velocity (exigência do Grim). ViaRewind é o addon para 1.8.x/1.7.x |
| **PacketEvents** | Dependência do Grim | versão exigida pelo Grim |
| **GrimAC** | Anticheat; nosso "juiz" de que o mod não altera mecânica | Nunca exemptar jogadores do mod. Configurar alertas em console e comando para consultar violações |
| **LegacyFeel-Server** (este projeto) | Handshake, modos, combate legado, itens | — |
| (opcional) PlaceholderAPI | Expor `%legacyfeel_mode%` para scoreboard/tab | só se já usado na rede |

### 6.2 Módulo `handshake`

- `paper-plugin.yml`, Java conforme Paper 26.2, build com **paperweight-userdev**.
- Registrar canal `[namespace]:handshake` como incoming e outgoing (`Messenger`).
- Receber `HELLO` → validar JSON (tamanho, `v` suportado, campos obrigatórios) → armazenar `ClientInfo` no jogador (`PersistentDataContainer` **não**; usar mapa em memória por UUID, limpo no quit) → responder `WELCOME` → em seguida `POLICY` da arena atual.
- **Só enviar** para jogadores que registraram o canal (ouvir `PlayerRegisterChannelEvent` ou checar `player.getListeningPluginChannels()`); clientes sem o mod e clientes 1.8 via Via nunca devem receber payload nosso.
- Rate-limit: no máximo 1 `HELLO` a cada 2 s por jogador; excesso é ignorado e logado em debug.
- Timeout: se em 5 s após o join não chegou `HELLO`, o modo é decidido pelas outras regras (§6.3).

### 6.3 Módulo `modes` — detecção e API

```java
enum ClientMode { MOD, VIA_LEGACY, VANILLA_MODERN, UNKNOWN }
```

Regras, nessa ordem:
1. Recebeu `HELLO` válido → `MOD` (guardar versão do mod e features ativas).
2. Senão, `Via.getAPI().getPlayerVersion(uuid)` ≤ protocolo da 1.8.9 (47) → `VIA_LEGACY`. (Protocolo 1.7.10 = 5.) **(VERIFICAR API atual do ViaVersion.)**
3. Senão → `VANILLA_MODERN`.
4. Antes de qualquer decisão → `UNKNOWN` (tratar como `VANILLA_MODERN` para fins de jogo).

API pública (`LegacyFeelAPI`, exposta via `ServicesManager`):
- `ClientMode getMode(UUID)`; `Optional<ClientInfo> getClientInfo(UUID)`; `boolean hasFeature(UUID, String featureId)`;
- evento `ClientModeResolvedEvent(Player, ClientMode)` para plugins de fila/minigame;
- `setPolicy(Player, Set<String> forceOff, String reason)` para arenas específicas (ex.: ranked).
- Comando admin `/legacyfeel info <jogador>` e `/legacyfeel stats` (contagem por modo — é a métrica de conversão que o humano quer acompanhar).

### 6.4 Módulo `combat` — combate legado no servidor (especificar em `docs/COMBAT.md`)

Aplicado a **todos** os jogadores, independente do modo. Cada item é uma chave de config com o valor 1.8.9 como padrão e possibilidade de perfil `1.7`.

| Mecânica | Como fazer no Paper 26.2 (**VERIFICAR** nomes de API) | Valor 1.8.9 |
|---|---|---|
| Sem cooldown de ataque | Atributo `ATTACK_SPEED` do jogador com base alta (ex.: 1024) no join/respawn; restaurar ao sair de arenas legadas se a rede também tiver modos modernos | — |
| Sem sweep attack | Cancelar `EntityDamageEvent` com causa `ENTITY_SWEEP_ATTACK`; opcional: suprimir partícula de sweep via PacketEvents | — |
| Invulnerabilidade entre hits | Manter `maximumNoDamageTicks = 20` (1.8.9 usava 20 com a regra "dano maior nos ticks 10–20 substitui") — o vanilla moderno tem a mesma regra; só garantir que nada mude | 20 |
| Knockback 1.8 | Cancelar o KB vanilla (`EntityKnockbackByEntityEvent` do Paper, ou zerar via atributo `ATTACK_KNOCKBACK` + resistência) e aplicar velocity própria: horizontal `0.4` na direção atacante→vítima com `motion/2` antes, vertical `+0.4` (cap 0.4), e se o atacante estava em sprint: extra `0.5` horizontal na direção do olhar + `0.1` vertical e cancelar sprint do atacante (`setSprinting(false)`). Fricção/ar conforme config. Aplicar **no servidor** (o Grim acompanha velocity enviada pelo servidor) | como descrito |
| Bloqueio com espada | Aplicar componente de dados `blocks_attacks` a todas as espadas entregues em kits (e reaplicar em `PlayerItemHeldEvent`/inventário se necessário): redução de dano 50%, sem delay, sem desabilitar item, sem som de escudo (ou som de bloqueio clássico se existir nos assets). **Não** usar `consumable`. Escudo: o clique direito **nunca** o ativa — negar o uso do item em `PlayerInteractEvent` **apenas quando o item usado é o escudo** (cancelar o evento de forma genérica quebra a troca de armadura, §6.6.2); a ativação passa a ser por agachar (§6.6.1) | dano/2 |
| Dano crítico e encantamentos | Manter vanilla; anotar diferenças em COMBAT.md se aparecerem nos testes | — |
| Vara de pesca (KB de rod) | Opcional: reproduzir KB da 1.8 ao acertar jogador com anzol | opcional |
| Fome/regeneração/poções/maçã dourada | Fora de escopo nesta fase | — |

O módulo deve ter `/legacycombat reload` e um modo `debug` que loga, por hit, os vetores de KB aplicados (para o teste §8.2).

### 6.5 Módulo `items`

- Kit de teste `/lfkit`: espada de diamante com `blocks_attacks`, arco, flechas, varas, poções — para os testes manuais.
- Comando `/lfarena` que teleporta para uma arena plana (mundo `arena_flat`, `flat`, PvP ligado, sem dano de queda opcional).

### 6.6 Módulo `qol` — as duas exceções ao 1:1 (inspiradas no Bedrock)

Aplicadas a **todos** os clientes (MOD, VIA_LEGACY, VANILLA_MODERN), como tudo no servidor. Cada uma é uma chave em `qol.yml`, padrão ligada, anunciada ao mod em `rules` do `WELCOME`/`POLICY` (§5.5). Especificar em `docs/COMBAT.md`, junto com o combate. Fora destas duas, **não existe** exceção ao 1:1 sem aprovação do humano.

#### 6.6.1 Escudo ao agachar (`qol.shieldOnSneak`, padrão `true`)

**Regra:** o escudo **nunca** é ativado pelo clique direito (o clique direito é 100% da espada/bloqueio 1.8). Ele fica erguido **enquanto o jogador agacha com um escudo na mão principal ou secundária** (comportamento Bedrock). Soltou o agachar, ou o escudo saiu da mão → baixa. Tudo o mais do escudo (bloqueio frontal, desativação por machado, durabilidade, som) fica **vanilla** — só o gatilho muda.

**Como fazer (abordagem recomendada: reaproveitar a lógica vanilla de bloqueio):**

1. **Reconciliar a cada tick** (barato: só jogadores online): `desejado = agachado && escudoNaMão`. Se `desejado` e o servidor ainda não está usando o escudo → `ServerPlayer#startUsingItem(mão)` (NMS via paperweight; **VERIFICAR** se o Paper 26.2 ganhou API para isso — ver §4.5). Se não `desejado` e o item em uso é um escudo **iniciado por nós** → `stopUsingItem()`. Guardar por jogador a flag "uso forçado por nós", para nunca interferir em uso legítimo (comer, arco, bloqueio com espada iniciado pelo clique direito). Se o jogador iniciar um uso legítimo enquanto agacha (ex.: bloquear com a espada), o uso legítimo vence; quando ele soltar, a reconciliação ergue o escudo de novo.
2. **Armadilha 1 — o cliente solta sozinho.** Ao receber o flag de "usando item" na entity data, o cliente vanilla vê que o botão direito não está pressionado e envia `RELEASE_USE_ITEM` (`Minecraft#handleKeybinds`), soltando localmente. Enquanto o uso forçado estiver ativo, **descartar** esse pacote via PacketEvents, em prioridade **acima do Grim** (o Grim pode flagar "release sem use"; confirmar a prioridade do listener dele). Consequência aceita: em cliente vanilla 26.2 e 1.8.9, quem agacha **não vê o próprio escudo erguido** (só um piscar de 1 tick); os outros jogadores veem, e o bloqueio funciona. O cliente com mod vê a pose pelo espelho (feature 12). Registrar no README como limitação conhecida do cliente sem mod.
3. **Armadilha 2 — clique direito no escudo.** Negar o uso em `PlayerInteractEvent` resolve o servidor, mas o cliente vanilla já previu o uso (escudo levantado, movimento ×0,2) e fica dessincronizado até soltar. Ao negar, reenviar ao próprio jogador a entity data de `LIVING_ENTITY_FLAGS` atual (flag desligado): o cliente limpa o `useItem` ao receber (`LivingEntity#onSyncedDataUpdated`). **VERIFICAR** no fonte 26.2 se esse comportamento do cliente ainda existe.
4. **Decisão obrigatória em COMBAT.md — atacar com o escudo erguido.** O cliente vanilla em estado de uso engole cliques de ataque; o cliente em uso forçado (que soltou localmente) **não** engole e envia o ataque. Para os três clientes darem o mesmo resultado, o servidor decide e impõe. Recomendação: igual ao escudo vanilla, **não ataca enquanto bloqueia** → cancelar `EntityDamageByEntityEvent` cujo causador está em uso forçado (o módulo de KB deve respeitar o cancelamento). A alternativa (atacar permitido) só com aprovação do humano.
5. **Velocidade.** Como o cliente não está em estado de uso, quem agacha com escudo anda na velocidade de agachar (×0,3), não ×0,3×0,2. É a mecânica escolhida, igual para todos; o Grim prevê movimento a partir dos pacotes do cliente, então não flaga. Registrar em COMBAT.md.
6. **Fallback** se a abordagem forçada gerar flags do Grim ou piscar inaceitável: implementar o bloqueio no `EntityDamageEvent` (ângulo frontal, redução e desativação por machado lendo o componente `blocks_attacks` do escudo) e mostrar a pose só para os **outros** jogadores, editando o `SetEntityData` que sai para eles (PacketEvents). Mais código, zero briga com o cliente. Documentar a escolha e o motivo.

Clientes 1.8.9 (Via/ViaRewind): o escudo não existe na 1.8.9 — o ViaRewind o mapeia para outro item (anotar qual em RECON.md). A regra vale igual (é servidor), mas o item raramente estará na mão desses jogadores. Testar mesmo assim (§8.6).

#### 6.6.2 Troca de armadura por clique direito (`qol.armorSwap`, padrão `true`)

**Fato:** isso **já é vanilla do Java desde a 1.19.4** — clicar com o botão direito segurando uma peça de armadura com o slot ocupado troca as duas (em 26.2 é governado pelo componente `minecraft:equippable`, campo `swappable`; **VERIFICAR** `DataComponentTypes.EQUIPPABLE` no Javadoc). Como o cálculo é do servidor, o cliente 1.8.9 via Via também ganha o comportamento: ele envia o "usar item" (o Via traduz para `use_item`), o servidor troca e devolve `SetSlot` para o slot de armadura e para a hotbar.

**Portanto o trabalho aqui é negativo — não quebrar o que já existe:**

- **Não** reverter para o 1.8 ("slot ocupado → nada acontece") em nome do 1:1. Esta é a exceção; está aprovada pelo humano.
- **Não** cancelar `PlayerInteractEvent` de forma genérica em nenhum módulo (o negar-uso do escudo da §6.6.1 e qualquer handler de espada devem checar o item). Um cancelamento genérico é o jeito mais fácil de quebrar a troca sem perceber.
- **Não** remover nem alterar `equippable` das peças entregues em kits. Peça "não trocável" (Curse of Binding é vanilla) é decisão de gameplay e vai para COMBAT.md.
- Se alguma arena pedir `qol.armorSwap: false`, aí sim negar o uso quando o slot já estiver ocupado — implementar como *opt-out*, nunca como padrão.

---

## 7. Servidor de testes (`testserver/`)

### 7.1 Objetivo

Um ambiente que sobe com um comando, reproduz a topologia de produção (proxy + backend), roda o anticheat como juiz e aceita três tipos de cliente: vanilla 26.2, 26.2 + mod, 1.8.9.

### 7.2 Topologia local

```
localhost:25565  Velocity (itzg/mc-proxy, TYPE=VELOCITY)   modern forwarding, online-mode conforme §7.4
   └─► paper:25566  Paper 26.2 (itzg/minecraft-server, TYPE=PAPER)
          plugins: ViaVersion, ViaBackwards, ViaRewind, PacketEvents, GrimAC, LegacyFeel-Server
          mundos: arena_flat (flat, pvp=true)
```

### 7.3 `docker-compose.yml` (esqueleto — **VERIFICAR** variáveis na documentação itzg)

```yaml
services:
  velocity:
    image: itzg/mc-proxy
    environment:
      TYPE: VELOCITY
      # ver docs para versão/plugins; montar velocity.toml e forwarding.secret
    ports: ["25565:25565"]
    volumes: ["./velocity:/server"]
  paper:
    image: itzg/minecraft-server
    environment:
      EULA: "TRUE"
      TYPE: PAPER
      VERSION: "26.2"
      ONLINE_MODE: "FALSE"            # o proxy faz a autenticação
      MEMORY: 4G
      # plugins via MODRINTH_PROJECTS / PLUGINS / SPIGET_RESOURCES — confirmar sintaxe e ids
    volumes: ["./paper:/data", "../plugin/build/libs:/plugins-dev:ro"]
    expose: ["25566"]
```

- `paper/config/paper-global.yml` → `proxies.velocity.enabled: true`, `online-mode: true`, `secret` igual ao `forwarding.secret`; `spigot.yml` → `settings.bungeecord: false`.
- `velocity.toml` → `player-info-forwarding-mode = "modern"`, `[servers] paper = "paper:25566"`, `try = ["paper"]`.
- `scripts/up.sh` sobe tudo, copia o jar do plugin buildado para `paper/plugins/`, mostra logs; `scripts/down.sh` derruba; `scripts/reset.sh` apaga mundos e logs; `scripts/grim-violations.sh` extrai as flags do log para o TESTING.md.
- Alternativa sem Docker (documentar em `testserver/README.md`): baixar jars do Paper/Velocity pela API oficial do PaperMC e rodar com `java -jar`.

### 7.4 Contas e clientes de teste

- **Mod + vanilla 26.2:** o `runClient` do Loom já sobe o jogo em modo de desenvolvimento (conta offline, nome `Player###`). Para isso funcionar pelo proxy, o ambiente de teste roda com `online-mode = false` no Velocity **apenas em localhost**. Nunca subir em produção assim.
- **1.8.9:** launcher oficial com perfil 1.8.9 (ou Lunar 1.8.9) conectando em `localhost:25565`; a tradução é feita pelo Via no backend.
- **Lunar 26.2 + mod:** exige conta Microsoft real e o Lunar instalado na máquina do humano. A IA prepara o jar e um passo a passo; o humano executa e cola o resultado no TESTING.md. A IA marca "não testado por mim" até receber o resultado.
- **Segundo jogador para luta:** segunda instância do `runClient` (Loom permite `runClient` com outro nome via argumento) ou um segundo launcher offline. Para testes de 12 jogadores/FPS, opcional: NPCs de um plugin de fake players — servem para FPS, **não** para anticheat.

### 7.5 Verificação do ambiente (Definition of Done da Fase 2)

- [ ] Cliente vanilla 26.2 entra pelo proxy e cai em `arena_flat`.
- [ ] Cliente 1.8.9 entra pelo proxy; `/viaversion` no console mostra protocolo 47.
- [ ] Grim carregado sem erros; um jogador vanilla andando 2 min não gera flag.
- [ ] `LegacyFeel-Server` carrega; `/legacyfeel info <jogador>` mostra `VANILLA_MODERN` e `VIA_LEGACY` corretamente.
- [ ] Logs acessíveis por `scripts/logs.sh`.

---

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

## 9. Entregáveis finais (Fase 7)

1. `mod/build/libs/[nome]-<versão>.jar` e `plugin/build/libs/legacyfeel-server-<versão>.jar`, com versões semânticas e changelog.
2. `README.md` raiz: o que é, arquitetura (diagrama da §2), como instalar para jogador (Lunar add-on Fabric passo a passo, Modrinth App, Prism, Feather), como desenvolver, como subir o servidor de testes.
3. `docs/` completo (RECON, MIXINS, PROTOCOL, COMBAT, TESTING preenchido, BLOCKERS, HANDOFF).
4. Um `.mrpack` de exemplo (mod + Sodium + servidor pré-configurado) em `dist/`, pronto para publicar no Modrinth. **Não publicar**; o humano publica.
5. Lista de itens marcados "não testado por mim" que o humano precisa executar (Lunar, Feather, conta real).

---

## 10. `docs/HANDOFF.md` — formato obrigatório ao fim de cada sessão

```markdown
# HANDOFF — <data> — sessão <n>
## Estado
- Fase atual: <n>. Features concluídas: [...]. Em andamento: [...].
## O que funciona (testado)
- ...
## O que NÃO foi testado por mim e por quê
- ...
## Decisões tomadas (e por quê)
- ...
## Bloqueios abertos → ver BLOCKERS.md
## Próximos 3 passos concretos
1. ...
## Comandos para retomar
- `cd mod && ./gradlew runClient`
- `cd testserver && ./scripts/up.sh`
```

---

## 11. Riscos conhecidos e como tratar

| Risco | Tratamento |
|---|---|
| **Lunar sobrescreve nossas animações** | Esperado. Documentar feature a feature; priorizar as que o Lunar não toca; considerar contato com a parceria do Lunar quando houver base de jogadores |
| **Minecraft 26.3 lançado em 15/09/2026** | Ficar em 26.2 até toda a stack ter builds estáveis; registrar mudanças em PORTING.md |
| **Migração OpenGL→Vulkan** | Não usar GL cru; só transforms/pose stack via API do jogo |
| **Licença GPL do Animatium** | Decidir em §4.4 antes de qualquer cópia |
| **Assets Mojang** | Não embutir; ver §5.6 |
| **`custom_payload` × ViaBackwards (#1308)** | Só enviar para quem registrou o canal; testar 26.1.x → 26.2 se a rede aceitar 26.1 |
| **Grim flagando KB custom** | KB aplicado 100% no servidor; investigar antes de exemptar; nunca exemptar jogadores do mod |
| **Nomes de classe diferentes em 26.2** | Nenhum mixin sem confirmar em `genSources`; MIXINS.md é a fonte da verdade |
| **Vantagem mecânica acidental** | Teste de não-interferência (§8.1) obrigatório por feature |
| **Escudo ao agachar brigando com o cliente** (auto-release, cliques de ataque engolidos, previsão do clique direito) | Seguir as armadilhas da §6.6.1; descarte do `RELEASE_USE_ITEM` acima do Grim; decisão explícita sobre atacar bloqueando; se instável, fallback do item 6 |
| **Perseguir o 1:1 e quebrar a troca de armadura** (que já é vanilla) | §6.6.2 é exceção aprovada; nenhum cancelamento genérico de `PlayerInteractEvent`; teste de regressão em §8.6 |

---

## 12. Links de partida (a IA deve abrir e registrar em RECON.md o que ainda vale)

- Fabric: `https://fabricmc.net/develop/` · `https://fabricmc.net/develop/template/` · `https://docs.fabricmc.net/develop/` · `https://docs.fabricmc.net/develop/porting/` · `https://github.com/FabricMC/fabric-example-mod` · `https://github.com/FabricMC/fabric-loader/releases` · `https://fabricmc.net/2026/03/14/261.html`
- Bibliotecas: `https://modrinth.com/mod/fabric-api` · `https://modrinth.com/mod/yacl` · `https://modrinth.com/mod/cloth-config` · `https://modrinth.com/mod/modmenu` · `https://modrinth.com/mod/sodium` · `https://modrinth.com/mod/iris`
- 1.8.9 para referência: `https://legacyfabric.net/`
- Mods de referência: `https://github.com/Legacy-Visuals-Project/Animatium` · `https://github.com/lowercasebtw/old-animations` · `https://github.com/PvPLand/LegacyBlocking` · `https://github.com/Legacy-Visuals-Project/Animatium-Legacy`
- Paper/Velocity: `https://docs.papermc.io/paper/dev/getting-started/` · `https://docs.papermc.io/velocity/` · `https://docs.papermc.io/velocity/server-compatibility/` · `https://papermc.io/downloads`
- Via: `https://hangar.papermc.io/ViaVersion` · `https://github.com/ViaVersion/ViaVersion` · `https://docs.viaversion.com`
- Anticheat: `https://github.com/GrimAnticheat/Grim` · `https://github.com/retrooper/packetevents`
- Docker: `https://docker-minecraft-server.readthedocs.io/`
- Distribuição: `https://www.lunarclient.com/news/how-to-add-your-own-mods-to-lunar-client` · `https://modrinth.com`

---

## 13. Sua primeira mensagem de resposta

Não escreva código ainda. Responda com:

1. Confirmação de que leu o documento e a lista de campos da §0 que ainda estão vazios.
2. As três primeiras ações que vai executar na Fase 1 e quanto tempo estima.
3. Qualquer contradição ou ambiguidade que encontrou neste documento.

Depois execute a Fase 1 e pare na aprovação.