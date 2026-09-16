# MIXINS — estado de implementação

Data: 15/09/2026. Alvos conferidos nos fontes locais gerados por Loom 1.17.21, Minecraft 26.2. Linhas relativas a `reference/minecraft-26.2/net/minecraft/`. Features 1 e 3 estão implementadas; a 2 foi confirmada como N/A.

| # | Mixin previsto | Classe/método verificado e linha | Injeção proposta | Limite de não interferência |
|---|---|---|---|---|
| 1 | CameraMixin | client/Camera.java:83 tick(); :249 alignWithEntity(float) | Implementado: Inject TAIL ajusta somente os dois campos de altura interpolada da câmera | Só câmera; nunca escreve pose, altura ou estado da entidade |
| 2 | Nenhum | client/model/HumanoidModel.java:197 setupAnim(T), ramo booleano :262 | N/A | Pose já binária; não alterar entidade ou dimensões |
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
| 12 visual | ItemInHandRendererMixin + AvatarRendererMixin | :415 submitArmWithItem; client/renderer/entity/player/AvatarRenderer.java:164 extractRenderState(AvatarlikeEntity,AvatarRenderState,float), :90 getArmPose(Avatar,ItemStack,InteractionHand) | Alteração de render state ou leituras restritas ao renderer | Só local, somente regra ativa, respeitar uso legítimo/desativação do escudo; não usar startUsingItem |
| 12 input | **Bloqueado** | client/Minecraft.java:1774 startUseItem(), chamada useItem :1826 | Não implementar até decisão | Suprimir use_item diverge da sequência vanilla e contradiz §5.9/§8.1 |

## Condições de implementação

Config resolvida fora do hot path; campos próprios com @Unique; sem @Overwrite quando uma injeção localizada basta. Conferir chamadas e side effects, não somente nome da classe. MinecraftMixin não é automaticamente “só render”: suprimir uma chamada ao renderer pode sê-lo, suprimir interação de rede não.

## Testes exigidos

Por feature: toggles runtime, presets, comparação gravada, roteiro determinístico de 30s com estado/pacotes, 10min Grim. Build e inicialização do servidor passaram; as comparações gráficas e partidas ainda não foram executadas.
