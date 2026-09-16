# MIXINS — estado de implementação

Data: 15/09/2026. Alvos conferidos nos fontes locais gerados por Loom 1.17.21, Minecraft 26.2. Linhas relativas a `reference/minecraft-26.2/net/minecraft/`.

| # | Mixin previsto | Classe/método verificado e linha | Injeção proposta | Limite de não interferência |
|---|---|---|---|---|
| 1 | CameraMixin | client/Camera.java:83 tick(); :249 alignWithEntity(float) | Implementado: Inject TAIL usa altura visual 1,54 ao agachar | Só câmera; nunca escreve pose, altura ou estado da entidade |
| 2 | HumanoidModelMixin | client/model/HumanoidModel.java:197 setupAnim(T), ramo booleano :262 | Implementado: Inject TAIL repõe pivôs da pose 1.8.9 | Somente modelo renderizado; dimensões da entidade permanecem modernas |
| 3 | ItemInHandRendererMixin | client/renderer/ItemInHandRenderer.java:615 tick(); :609 shouldInstantlyReplaceVisibleItem(ItemStack,ItemStack) | Implementado: ModifyConstant no passo visual 0.4; modos legacy, instant e ticks | Somente alturas do renderer; não modifica inventário, slot ou cooldown |
| 4 | ItemInHandRendererMixin + ajuste de pose humanoide | mesmo arquivo :415 submitArmWithItem(AbstractClientPlayer,float,float,InteractionHand,float,ItemStack,float,PoseStack,SubmitNodeCollector,int); :649 itemUsed(InteractionHand); HumanoidModel.java:197 setupAnim(T) | WrapOperation nos transforms; Inject seletivo em itemUsed para impedir reset apenas visual | Uso real vem do servidor/vanilla; swing só na pose, sem escrever swingTime |
| 5 | ItemInHandRendererMixin | :328 applyItemArmAttackTransform(PoseStack,HumanoidArm,float); :601 swingArm(float,PoseStack,int,HumanoidArm) | WrapOperation/ModifyExpressionValue | Curvas de transformação; progresso real intacto. Perfil 1.7 ainda requer fontes próprios |
| 6 | GameRendererMixin | client/renderer/GameRenderer.java:320 bobView(CameraRenderState,PoseStack), chamadas :345 e :534 | WrapOperation nas chamadas e transforms | Separar chamadas da mão/câmera; não mudar distância percorrida no jogador |
| 7A | GameRendererMixin ou estado de render | :298 bobHurt(CameraRenderState,PoseStack) | Ajuste no estado renderizado ou WrapOperation seletivo | Não persistir mudança de opção vanilla; preservar tilt de morte salvo decisão explícita |
| 7B | LivingEntityRendererMixin | client/renderer/entity/LivingEntityRenderer.java:133 getOverlayCoords(LivingEntityRenderState,float) | ModifyExpressionValue | Apenas coordenada do overlay, não hurtTime da entidade |
| 8 | AbstractClientPlayerMixin e avaliação da câmera | client/player/AbstractClientPlayer.java:88 getFieldOfViewModifier(boolean,float); Camera.java:163 tickFov() | ModifyReturnValue ou ModifyExpressionValue | Só retorno do FOV; atributos e sprint não alterados |
| 9 | ItemInHandRendererMixin | :338 applyItemArmTransform(PoseStack,HumanoidArm,float), :415 submitArmWithItem | WrapOperation nos transforms | Escala/posição visual; baixa prioridade |
| 10 | Nenhum inicialmente | Índices de assets em evidence | Resource pack sounds.json, se validado | Sem redistribuir sons; fidelidade ainda pendente |
| 11 | Nenhum | Fabric PayloadTypeRegistry.serverboundPlay/clientboundPlay; ClientPlayConnectionEvents.JOIN | Eventos públicos | Único canal autorizado; limite estrito e política por conexão |
| 12 visual | LegacyFeelClient | client/player/LocalPlayer.java:532 startUsingItem(InteractionHand) | Implementado: estado estritamente local enquanto sneak+shield | O servidor decide defesa pelo sneak; o estado é limpo ao levantar ou trocar o item |
| 12 input | MinecraftMixin | client/Minecraft.java:1774 startUseItem(), chamada useItem :1826 | Implementado após aprovação: shield retorna PASS | Espada continua utilizável e item na outra mão não impede blockhit |
| combate | MinecraftMixin | client/Minecraft.java:1700 startAttack() | Implementado: remove missTime de 10 ticks e libera ataque com espada em uso | Ativo somente no preset LegacyFeel e sujeito a `forceOff` do servidor |
| animações PvP | ItemInHandRendererMixin | ItemInHandRenderer.submitArmWithItem/renderItem | Compõe swing com bloqueio e consumo; restaura posições 1.7 de blockhit, comida, vara, arco e swing | Durante comida/bebida o clique cria somente animação local e não envia ataque |
| estabilidade do bloqueio | ItemInHandRendererMixin | ItemInHandRenderer.tick/itemUsed | Impede que ataque ou novo clique de defesa reinicie a altura da espada | Troca real de item/slot continua usando a animação de equipar configurada |
| previsão de blocos 1.8 | ClientCommonPacketListenerMixin + ClientLevelMixin | ClientCommonPacketListenerImpl.send; ClientLevel.handleBlockChangedAck/tick | Identifica sequências de `use item on` e retém por poucos ticks a previsão local quando o ViaVersion sintetiza ACK para backend 1.8.8 | Não atrasa ACK exclusivo de mineração; só ativa em `CLASSIC_PARITY` com capability explícita; rejeições ainda são desfeitas após o prazo |
| bloqueio em terceira pessoa | PlayerItemInHandLayerMixin | PlayerItemInHandLayer.submitArmWithItem | Remove a transformação moderna e aplica a sequência clássica completa, posicionando a espada diante do peito | Ativo apenas para a mão que está usando uma espada; suporta mão esquerda e agachamento |
| áudio da vara | ClientLevelMixin | ClientLevel.playLocalSound | Troca o lançamento moderno pelo `random.bow` original da 1.8.9 e remove o som moderno de recolhimento | A versão antiga não emitia som próprio ao recolher; pitch e volume do lançamento são preservados |
| tint de dano | EquipmentLayerRendererMixin | EquipmentLayerRenderer.renderLayers | Aplica o overlay vermelho também à armadura, como a opção `1.7 Damage` do OldAnimationsMod | Não altera tablist, HUD ou inventário |

## Condições de implementação

Config resolvida fora do hot path; campos próprios com @Unique; sem @Overwrite quando uma injeção localizada basta. Conferir chamadas e side effects, não somente nome da classe. MinecraftMixin não é automaticamente “só render”: suprimir uma chamada ao renderer pode sê-lo, suprimir interação de rede não.

## Testes exigidos

Por feature: toggles runtime, presets, comparação gravada, roteiro determinístico de 30s com estado/pacotes, 10min Grim. Build, inicialização do servidor, carregamento dos mixins e conexão do cliente 26.2 passaram. A comparação manual de sensação e a partida prolongada ainda dependem do teste do jogador.
