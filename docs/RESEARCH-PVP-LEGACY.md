# Decisões de compatibilidade PvP 1.7.10/1.8.9

Esta nota registra a análise feita em 16/09/2026. O laboratório executa Paper 26.2 com cliente Fabric 26.2 e aceita também o cliente 1.8.9 pela pilha ViaVersion. O objetivo é separar três camadas que frequentemente são confundidas: resposta visual a cada clique, aceitação de dano pelo servidor e movimento/knockback após um acerto aceito.

## Base vanilla confirmada

| Comportamento | 1.8.9 | Decisão no laboratório |
|---|---|---|
| Alcance de sobrevivência | 3,0 blocos | 3,0; nenhum aumento artificial |
| Margem da hitbox no raycast | 0,1 bloco | 0,1 pelo componente `ATTACK_RANGE` do Paper/OCM |
| Resistência após dano | máximo de 20 ticks; na segunda metade, dano maior pode substituir o anterior | 20 ticks. Nunca zerar a resistência a cada evento |
| Bloqueio com espada | dano final `(dano + 1) / 2` | OCM é a implementação principal; LegacyFeel mantém a mesma fórmula apenas como fallback sem OCM |
| Swing normal | duração usual de 6 ticks e reinício após metade da animação | remover o atraso de clique errado não altera a cadência de dano |
| Knockback | base horizontal/vertical 0,4; limite vertical 0,4; bônus de sprint 0,5/0,1 | valores clássicos do módulo `old-player-knockback` |

O “delay muito baixo” vinha de `setNoDamageTicks(0)` no evento de dano. Isso transformava CPS em dano efetivo. O código foi removido. A animação continua respondendo aos cliques, mas dano, som de acerto aceito e knockback seguem o tick do servidor.

## Repositórios avaliados

| Projeto e revisão analisada | O que acerta | Uso no LegacyFeel |
|---|---|---|
| BukkitOldCombatMechanics `c736c44`; laboratório usa release 2.6.0 `9a28315` | Pipeline de dano clássico, sword block no Paper, ferramenta/crítico/armadura/regeneração, rod, projéteis, reach e knockback testados em várias versões | Integrado ao quick lab. `attack-frequency=20`, reach 3,0, margem 0,1, módulos clássicos e knockback antigo. Evitamos duplicar a redução de espada no plugin próprio |
| ShieldFixes `57923dc` | Corrige estado visual remoto e sons de escudo; preserva o atraso moderno de 5 ticks | Não integrado: o atraso moderno e o clique direito do escudo conflitam com a regra de escudo somente ao agachar. A lição útil é sincronizar pose remota a partir do estado do servidor |
| MouseDelayFix `78cac28` | Corrige MC-67665, um erro antigo de direção do raycast que usava rotação da cabeça | Não portado: o raycast de 26.2 já contém a correção; o mod não remove cooldown nem melhora hitreg moderno |
| BetterHitreg `993c081` | Prediz som/partícula no cliente e usa aproximadamente 475 ms como janela de hit clássico | Não integrado: é feedback especulativo e pode mostrar “ghost hit”. Como controlamos o servidor, a confirmação deve vir do dano real |
| ping-equalizer `dd8fc0a` (`26.x`) | Atrasa pacotes para equalizar ping | Não integrado: adiciona latência e altera ordem/timing, prejudicando o laboratório local. Só faz sentido como experimento separado |
| ConsumableOptimizer `070c9ea` (`26.2`) | Predição de consumo e reconciliação de efeitos | Não integrado por enquanto: a animação de sword block do OCM usa componente consumível e os dois podem disputar o mesmo estado |
| combat-control `3834bb8` | Referência Fabric muito completa: ataque durante uso, rod clássica, hitboxes, ferramentas e variante de KB sem escala | Usado como referência para o blockhit do cliente. Não pode substituir o plugin em um backend Paper |
| sword-blocking-mechanics `93e4d39` (`26.2.x`) | Transformações visuais de bloqueio 1.7 e componentes modernos | Referência de render. Parry e redução própria não foram trazidos porque não pertencem ao comportamento vanilla antigo |
| DamageTintPlus `b4fcb03` | Amplia o tint vermelho para equipamento e itens | Não afeta registro, timing ou knockback; além disso, a revisão disponível é 1.20.4. Fica fora do núcleo PvP 26.2 |
| BactroMod `c77b65d` | Desloca a camada de fogo na tela com uma transformação simples | Portado apenas como low fire configurável (`-0,3`). Fullbright, fog e outras preferências não foram trazidos |

## Vara de pesca

- O lançamento antigo usa `random.bow` em volume `0,5` e pitch `0,4 / (random * 0,4 + 0,8)`. O mod inclui o OGG original da 1.8.9, em vez de reutilizar a gravação diferente presente na 26.2.
- A 1.7/1.8 não tinha o evento sonoro moderno de recolhimento. O cliente remove apenas `FISHING_BOBBER_RETRIEVE`; demais sons de pesca permanecem intactos.
- A transformação visual do OldAnimationsMod desloca a vara em `(0,08, -0,027, -0,33)` e aplica escala `(0,93, 1, 1)`. Essa transformação está ativa junto do swing clássico de lançar/recolher.
- Trajetória, gravidade, knockback ao acertar e cancelamento do puxão em jogadores continuam sob autoridade dos módulos `fishing-rod-velocity` e `old-fishing-knockback` do OldCombatMechanics, evitando duas implementações concorrentes.

## Arquitetura ativa

- **OldCombatMechanics:** autoridade para mecânicas clássicas do servidor, incluindo knockback, vara, ferramenta, crítico, armadura, regeneração, sword block e hitbox.
- **LegacyFeel Server:** handshake, regras específicas do experimento, escudo ao agachar, kit e fallback quando OCM estiver ausente.
- **LegacyFeel Fabric:** câmera/agachamento, animação de mão, blockhit real durante uso da espada, escudo visual ao agachar, tooltip solicitado e low fire.
- **ViaVersion/ViaBackwards/ViaRewind:** comparação de protocolo com 1.8.9; não definem as regras de dano.

## Telemetria e GrimAC

`/legacyfeel record start [jogador]` inicia uma sessão e `/legacyfeel record stop [jogador]` grava um CSV. A captura observa o pacote de ataque antes do processamento e o correlaciona com o evento final do Paper. Tentativas sem evento de dano permanecem visíveis como `no_damage_event`, em vez de desaparecerem das estatísticas. O relatório inclui distância até a hitbox, protocolo, ping, sprint, dano, frames de resistência e velocidade antes/depois do knockback.

O GrimAC continua verificando reach, movimento e knockback. Duas opções foram ajustadas especificamente para o combate legado:

- `reset-item-usage-on-attack: false`: atacar não encerra o uso da espada durante blockhit;
- `PacketOrderI.exempt-placing-while-digging: true`: aceita a ordem de pacotes das animações 1.7 sem desabilitar as demais verificações.

## Próximas medições

1. Medir em vídeo de 60 fps uma sequência de 5, 9, 12 e 16 CPS. O braço deve permanecer responsivo; dano e knockback devem aparecer apenas nos hits aceitos.
2. Comparar o deslocamento de um alvo parado com hit normal, W-tap e S-tap em 1.8.9 e 26.2, usando os mesmos blocos de referência.
3. Testar rod-hit-hit, reel sem puxar o jogador e trajetória até cerca de 12 blocos.
4. Validar blockhit com item na mão secundária e confirmar que o clique do escudo continua desativado.
