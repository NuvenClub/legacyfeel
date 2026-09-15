# RECON — 15/09/2026

## Resultado

Reconhecimento estático realizado; fontes 26.2 e 1.8.9 gerados pelo Loom e examinados. A implementação aguarda a aprovação exigida em REQUEST.md §1.1.3/§1.2. Não confundir disponibilidade de artefatos com compatibilidade comprovada em jogo. Pendências e propostas em APPROVAL.md e BLOCKERS.md.

## 1. Ambiente e toolchain

Diretório criado em `victor/legacyfeel`, preservando os projetos existentes. Java global encontrado: 21. Docker CLI presente, mas o daemon Linux não estava disponível. Gradle global não encontrado. Nenhum servidor foi iniciado.

| Componente | Evidência de 15/09/2026 / escolha de reconhecimento |
|---|---|
| Minecraft | 26.2, release no manifesto Mojang; Java majorVersion 25 |
| Java local | Temurin 25.0.4.1+1, ZIP SHA-256 conferido com API Adoptium |
| Gradle | 9.5.1, versão do template; ZIP SHA-256 conferido |
| Loom | 1.17.21, release do Maven; genSources passou. Template oferecia 1.17-SNAPSHOT; não foi mantida versão dinâmica |
| Loader | 0.19.5 stable, API Fabric |
| Fabric API | 0.160.0+26.2 release; resolvida no build |
| YACL | 3.9.6+26.2-fabric release disponível; ainda não adicionada |
| Cloth Config | 26.2.155+fabric, fallback desnecessário enquanto YACL disponível |
| Mod Menu | 20.0.2 release |
| Sodium / Iris | mc26.2-0.9.2-fabric / 1.11.4+26.2-fabric; apenas compatibilidade futura |
| Paper | 26.2 build 124 STABLE, API Fill; não iniciado |
| Velocity | API Fill lista 4.2.0; build específico ainda a fixar na Fase 2 |
| ViaVersion / ViaBackwards | 5.11.0 release com 26.2 nos metadados Modrinth |
| ViaRewind | 4.1.3 release com 26.2 nos metadados |
| Grim | 2.3.74-8eb5f28 alpha para 26.2; não foi encontrada release 26.2 no filtro Modrinth. GitHub latest é v2.3.73, cujo anúncio cobre 1.21.10/11 |
| PacketEvents | v2.13.0 release anuncia 26.2; o commit atual do Grim usa 2.13.1+4d40422-SNAPSHOT. Fixar par compatível antes do teste |

Fontes: [Fabric develop](https://fabricmc.net/develop/), [API Loader](https://meta.fabricmc.net/v2/versions/loader/26.2), [Maven Loom](https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml), [template oficial](https://github.com/FabricMC/fabric-example-mod), [Mojang](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json), [Adoptium](https://api.adoptium.net/v3/assets/latest/25/hotspot?architecture=x64&image_type=jdk&os=windows), [Paper](https://fill.papermc.io/v3/projects/paper/versions/26.2/builds), [Velocity](https://fill.papermc.io/v3/projects/velocity), [Grim](https://github.com/GrimAnticheat/Grim/releases), [PacketEvents](https://github.com/retrooper/packetevents/releases/tag/v2.13.0).

Consultas Modrinth: `https://api.modrinth.com/v2/project/<slug>/version?game_versions=%5B%2226.2%22%5D`. Slugs: fabric-api, yacl, cloth-config, modmenu, sodium, iris, viaversion, viabackwards, viarewind, grimac, packetevents. Respostas preservadas em `evidence/modrinth-*.json`. Não escolher automaticamente o primeiro resultado: pode ser NeoForge, alpha ou snapshot.

As respostas brutas HTML/XML/JSON permanecem locais e fora do Git; URLs e resultados resumidos ficam neste relatório. Logs, inventário de commits e comparações compactas são versionados.

### Correção do build

26.1 passou a não ter ofuscação: usar `net.fabricmc.fabric-loom`, `implementation`/`compileOnly`/`runtimeOnly`, e não `officialMojangMappings()`/`modImplementation`/`remapJar`. Java 25 obrigatório. Fontes oficiais: [post 26.1](https://fabricmc.net/2026/03/14/261.html), [porting 26.2](https://docs.fabricmc.net/develop/porting/). Split client/common ativado no build de reconhecimento, sem classes de implementação.

O gerador web foi aberto, mas exige JavaScript no conteúdo consultado; foi usado o template oficial CC0. Não se concluiu que o gerador não oferece 26.2. MixinExtras aparece na metadata do Loader (0.5.5), sem necessidade de biblioteca adicional.

## 2. Proveniência dos fontes

- 26.2: `mod/.gradle/loom-cache/minecraftMaven/.../*sources.jar`, extraídos em `reference/minecraft-26.2`. `evidence/genSources-local.log`: BUILD SUCCESSFUL, 1m48s.
- 1.8.9: projeto de consulta `.tools/legacy-1.8.9`, Legacy Fabric yarn build 604, Loom 1.16.3 e Legacy Looming 1.16.1 resolvidos do template. Fontes em `reference/minecraft-1.8.9`. `evidence/genSources-1.8.9.log`: BUILD SUCCESSFUL, 2m6s.
- Nomes 1.8.9 abaixo são do Legacy Fabric, não nomes Mojang. Nenhum fonte foi obtido de mirror de Minecraft.
- 1.7.10 ainda não decompilada; preset 1.7 não pode ser anunciado como validado.

Todos os caminhos/linhas abaixo se referem a essas extrações locais, que devem ser regeneradas ao trocar versões. Inventário de commits das referências: `evidence/reference-commits.tsv`.

## 3. Alvos 26.2 confirmados

Base: `reference/minecraft-26.2/net/minecraft/`.

| Área | Arquivo:linha e resultado |
|---|---|
| Câmera | `client/Camera.java:83`: tick interpola altura com fator 0,5. `:249`: alignWithEntity(float) interpola eyeHeightOld→eyeHeight; não existe o antigo setup como ponto equivalente |
| Equipar | `client/renderer/ItemInHandRenderer.java:615`: tick, alturas antigas e atuais; limite ±0,4. `:609` considera componentes que ignoram animação |
| Bloqueio/swing | mesmo arquivo `:415`: submitArmWithItem; `:486`: leitura do uso; `:501`: BLOCK já tem transform para item que não é ShieldItem; `:328` applyItemArmAttackTransform; `:649` itemUsed altera apenas altura renderizada |
| Bob/hurt | `client/renderer/GameRenderer.java:298` e `:320`: recebem CameraRenderState e PoseStack; chamadas separadas em `:345` e `:534`, permitem avaliar mão/câmera separadamente |
| Overlay | `client/renderer/entity/LivingEntityRenderer.java:133`: getOverlayCoords(LivingEntityRenderState,float) |
| FOV | `client/player/AbstractClientPlayer.java:88`: getFieldOfViewModifier(boolean,float); `client/Camera.java:163`: outra suavização em tickFov, avaliar fidelidade completa |
| Sneak modelo | `client/model/HumanoidModel.java:262`: ramo booleano isCrouching. `client/renderer/entity/HumanoidMobRenderer.java:62`: atribuição direta. Feature 2 dispensável para essa pose |
| Jogador | `client/renderer/entity/player/AvatarRenderer.java:78` e `:90`: getArmPose; `:164`: extractRenderState. PlayerRenderer não é o nome atual |
| Miss | `client/Minecraft.java:1700`, `:1708`, `:1762`: startAttack e missTime=10. Não alterar |
| Uso local | `client/player/LocalPlayer.java:532`, `:541`, `:562`: estado local próprio e sincronização de flags; não basta ler só LivingEntity |
| Auto-release | `client/Minecraft.java:2009`: sem keyUse chama releaseUsingItem e consome cliques de ataque |
| Movimento usando | `client/player/LocalPlayer.java:675`: escala por itemUseSpeedMultiplier(), agora vem de USE_EFFECTS, não constante 0,2 embutida nesse ponto |
| Espada/armadura | `world/item/Item.java:188`: use considera EQUIPPABLE e BLOCKS_ATTACKS; `:298`: animação BLOCK para componente; `world/item/equipment/Equippable.java:54`: swappable default true; `:128`: troca, incluindo previsão cliente e autoridade servidor |
| Payload | `network/protocol/common/ServerboundCustomPayloadPacket.java:14`: 32767 bytes; Clientbound equivalente `:16`: 1048576. Limite próprio de 8191 bytes é mais restritivo |

`core/component/DataComponents.java:116` marca DAMAGE como ignoreSwapAnimation: a afirmação de que durabilidade sempre dispara animação moderna não procede nessa versão.

## 4. Medidas da 1.8.9

Base: `reference/minecraft-1.8.9/net/minecraft/`.

- `entity/player/PlayerEntity.java:1628`: olho 1,62 e redução de 0,08 ao agachar. Remover suavização usando a altura moderna não reproduz esse deslocamento antigo. Proposta: deslocamento somente da câmera relativo à altura em pé, sem mudar olho real nem raycast.
- `client/render/item/HeldItemRenderer.java:447`: equip avança por clamp(target-current, -0,4, +0,4); troca referência abaixo de 0,1. `item/ItemStack.java:299` compara também dano e NBT; portanto “não anima ao mudar durabilidade” não é garantia universal do vanilla antigo.
- Mesmo renderer `:205`: swing traduz por (-0,4 sin(π√s), 0,2 sin(2π√s), -0,2 sin(πs)). `:228`: base (0,56;-0,52;-0,71999997), deslocamento Y -0,6e, rotação Y45; rotações Y -20 sin(πs²), Z -20 sin(π√s), X -80 sin(π√s), escala 0,4.
- `:263`: bloco acrescenta translação (-0,5;0,2;0), Y30, X-80, Y60, nesta ordem. Reimplementar matematicamente sobre transforms modernos; não copiar chamadas GL.
- `client/render/GameRenderer.java:490`: bob usa fase negativa da distância interpolada; X=sin(πfase)·bob·0,5; Y=-abs(cos(πfase)·bob); Zrot=sin(πfase)·bob·3; Xrot=abs(cos(πfase-0,2)·bob)·5; há ainda pitch adicional dos campos field_6752/field_6753. A origem desses campos precisa entrar na implementação, não só as constantes.
- `:468`: hurt normalizado, sin(π·hurt⁴), rotação -14 vezes esse valor, entre rotações de direção; há tilt de morte separado.
- `client/network/AbstractClientPlayerEntity.java:87`: FOV inclui voo ×1,1, razão de velocidade (v/w+1)/2, fallback finito e arco até ×0,85. Não equivale a um multiplicador fixo de sprint.
- `client/MinecraftClient.java:1168`: doAttack, cooldown de miss 10. Confirmado, preservar.
- **Correção de dano:** `entity/player/PlayerEntity.java:829` aplica (1+dano)/2 ao bloquear, antes de armadura/encantamentos. `dano/2` do prompt não é 1:1.
- **Correção de KB:** `entity/LivingEntity.java:759` divide todos os eixos por 2, acrescenta 0,4 vertical e limita a 0,4; `entity/player/PlayerEntity.java:969` acrescenta KB de sprint/encantamento e reduz X/Z do atacante por 0,6, além de desligar sprint. Ainda precisa modelar contexto completo e validar em partidas.

## 5. Referências e licenças

| Projeto | Licença observada | Alvos/uso da leitura e diferença proposta |
|---|---|---|
| [Animatium](https://github.com/Legacy-Visuals-Project/Animatium) | GPL-3.0 + linking exception declarada no README e gradle.properties | Camera.alignWithEntity, ItemInHandRenderer.submitArmWithItem/swingArm, GameRenderer. Referência de organização; excluir alterações de eyeHeight/hitbox/miss e reimplementar sem cópia |
| [old-animations](https://github.com/lowercasebtw/old-animations) | fabric.mod.json declara ARR; sem licença permissiva encontrada | GameRenderer.tiltViewWhenHurt/bobView/renderHand/renderWorld e mixins de entidades. Não copiar; aviso do README sobre sneak banível reforça excluir mecânica |
| [LegacyBlocking](https://github.com/PvPLand/LegacyBlocking) | CC0-1.0, LICENSE e metadata | SwingProgressMixin: HeldItemRenderer.renderFirstPersonItem aplica swing; EquipProgressMixin: MinecraftClient.doItemUse evita reset visual. Usa CONSUMABLE, incompatível com nossa especificação; estudar ideia e usar BLOCKS_ATTACKS |
| [Animatium-Legacy](https://github.com/Legacy-Visuals-Project/Animatium-Legacy) | LGPL-3.0 acompanhada do texto GPL | ItemRenderer e ModelBiped.setRotationAngles, entre outros; comparar organização, sem importar código ou GL para 26.2 |

Não foi copiado código desses mods para o mod/plugin. Clones não são dependências. Recomenda-se implementação original MIT, sujeita à escolha do usuário. “GPL exige publicar tudo na internet” é simplificação incorreta: as obrigações de distribuição e fornecimento de fonte devem ser respeitadas conforme o caso. [GPL FAQ](https://www.gnu.org/licenses/gpl-faq.html#GPLRequireSourcePostedPublic).

Os mappings não tornam Minecraft open source. A [EULA](https://www.minecraft.net/en-us/eula) distingue mods originais de distribuição do jogo modificado. A nota histórica de [mappings Mojang](https://www.minecraft.net/en-us/article/minecraft-snapshot-19w36a) não implica permissão de redistribuir os fontes. Para 26.2 não há artefato de mappings no manifesto usado.

## 6. Paper, Via e Grim

- Javadoc 26.2 baixado diretamente contém `LivingEntity.startUsingItem(EquipmentSlot)` em `evidence/org_bukkit_entity_LivingEntity.html:3085`. HAND/OFF_HAND somente. A extração da busca web omitiu esse membro; prevalece a página completa salva. Não assumir necessidade de NMS para iniciar uso.
- Confirmados ATTACK_SPEED, BLOCKS_ATTACKS, EQUIPPABLE, BlocksAttacks.blocksAttacks() e Equippable.swappable(). APIs de data components são versionadas/experimentais; compilar contra a build escolhida.
- Links: [LivingEntity](https://jd.papermc.io/paper/26.2/org/bukkit/entity/LivingEntity.html), [BlocksAttacks](https://jd.papermc.io/paper/26.2/io/papermc/paper/datacomponent/item/BlocksAttacks.html), [data components](https://docs.papermc.io/paper/dev/data-component-api/), [userdev](https://docs.papermc.io/paper/dev/userdev/), [plugin messaging](https://docs.papermc.io/paper/dev/plugin-messaging/).
- ViaAPI.java:103 mantém getPlayerVersion(UUID); :114 expõe getPlayerProtocolVersion(UUID). Rejeitar versão desconhecida/negativa antes de comparar ≤47, para não classificá-la como legado.
- ViaRewind `common/src/main/resources/assets/viarewind/data/item-mappings-1.9.json:54`: escudo 442 → 425 (banner), “1.9 Shield”. A aparência final através de toda a cadeia 26.2→1.8.9 não foi testada.
- [ViaBackwards #1308](https://github.com/ViaVersion/ViaBackwards/issues/1308) aparece aberta/unconfirmed, cenário 26.1.2→26.2 com proxy; não presumir corrigida nem mesma causa no backend.
- Grim README:43 exige Via somente backend. `PacketPlayerDigging.java:31` usa LOW e `:65` processa RELEASE_USE_ITEM sem guarda de cancelamento nesse ramo. `PacketSelfMetadataListener.java:201` também acompanha flag de uso. PacketEvents `PacketListenerPriority.java:22` e `EventManager.java:82`: LOWEST roda antes, eventos continuam nos listeners mesmo cancelados. A estratégia do prompt não comprova invisibilidade para Grim.

## 7. Networking Fabric

Fonte exata resolvida: `reference/fabric-networking-resolved`, módulo 6.3.4+2989c6a09e da API 0.160.0+26.2. `PayloadTypeRegistry.java:103/110`: serverboundPlay()/clientboundPlay(), não os nomes antigos playC2S/playS2C. CustomPacketPayload e StreamCodec continuam aplicáveis; identificador é Identifier.

`ClientPlayNetworkAddon.java:60/66` invoca JOIN **antes** de anunciar canais. Responder imediatamente ao HELLO somente se já registrado pode perder WELCOME. Planejar HELLO no tick seguinte ou enfileirar a resposta até PlayerRegisterChannelEvent. Exigir simultaneamente HELLO válido e registro, não enviar para qualquer vanilla que registre o nome.

Troca de backend: não afirmar que JOIN sempre resolve todos os caminhos de proxy; testar reconfiguration e reset de política. Não acrescentar pacote de desafio sem especificar revisão do protocolo. Wire format ainda precisa fixar JSON puro vs prefixo VarInt; proposta em PROTOCOL.md.

Documentação consultada: [networking](https://docs.fabricmc.net/develop/networking), [events](https://docs.fabricmc.net/develop/events/), [rendering](https://docs.fabricmc.net/develop/rendering/basic-concepts), [mixins](https://docs.fabricmc.net/develop/mixins/accessors), [geração de fontes](https://docs.fabricmc.net/develop/getting-started/generating-sources).

## 8. Topologia, assets e distribuição

- [Velocity forwarding](https://docs.papermc.io/velocity/player-information-forwarding/): modern incompatível com clientes <1.13; proposta legacy para clientes 1.8.9, backend acessível só pelo proxy. Com isso spigot.settings.bungeecord=true e proxies.velocity.enabled=false. Aprovação necessária por alterar arquitetura expressa.
- Em modo de testes offline, configurações de autenticação precisam corresponder; o prompt mistura online-mode false no proxy e true no forwarding. Bind local deve ser 127.0.0.1:25565:25565, não 25565:25565.
- [itzg Paper](https://docker-minecraft-server.readthedocs.io/en/latest/types-and-platforms/server-types/paper/) confirma TYPE=PAPER e seleção de versão. [proxy](https://github.com/itzg/docker-mc-proxy) documenta TYPE=VELOCITY. Configurar SERVER_PORT=25566 explicitamente: expose não muda a porta de escuta. Imagens Java 25 e digests a fixar na Fase 2; daemon indisponível hoje.
- Índices de assets 1.8.9/26.2 foram baixados. `evidence/audio-comparison.json`: cinco sons damage/hit e fall antigos sem hash idêntico no índice moderno. Isso não prova ausência de som reencodificado equivalente. Não embutir .ogg; feature sonora opcional fica pendente de conferir eventos e audição, sem promessa de fidelidade.
- [Lunar](https://www.lunarclient.com/news/how-to-add-your-own-mods-to-lunar-client) documenta mods Fabric de terceiros; Animatium README registra sobreposição de configurações. Lunar/Feather não foram executados; detecção passiva permanece unknown, sem inventar IDs. Suporte a .mrpack em cada launcher deve ser testado na distribuição; nenhum pack publicado.
