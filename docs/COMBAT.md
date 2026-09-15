# COMBAT — especificação pendente de aprovação

Nenhuma mecânica implementada ou validada em jogo. Mesmo tratamento para MOD/VIA_LEGACY/VANILLA_MODERN/UNKNOWN.

## Perfil 1.8 medido nos fontes

- Bloqueio: (1+dano)/2 antes de armadura/encantamentos, condicionado pelo tipo de dano. Diverge do dano/2 solicitado; corrigir mediante aprovação em APPROVAL.md.
- KB básico: velocidade anterior de todos os eixos /2; impulso horizontal de magnitude 0,4 na direção de repulsão; vertical +0,4 limitada a 0,4. Tratar direção degenerada e resistência conforme fonte.
- KB de sprint/encantamento: +0,5 horizontal por nível efetivo, +0,1 vertical; atacante X/Z ×0,6 e sprint desligado. Não aplicar impulso duas vezes em eventos diferentes.
- Preservar invulnerabilidade e regra de diferença de dano; verificar interação com dano cancelado, critical, encantamentos e escudo em testes. ATTACK_SPEED alto sozinho não comprova combate 1.8 completo.
- Remover dano de sweep com evento específico; não cancelar interações genéricas.

## QoL

Recomendação de decisão: impedir ataques enquanto bloqueia com escudo forçado, igualmente em todos os clientes. Manter armorSwap true, incluindo Curse of Binding e demais restrições vanilla; opt-out apenas quando configurado.

Escudo por sneak: API Paper startUsingItem(EquipmentSlot) existe no Javadoc baixado. Cuidado com auto-release, previsão, cooldown de machado, prioridade de uso legítimo, troca de mão, morte e logout. Não reiniciar continuamente escudo desativado. Estado forçado precisa ter proprietário e limpeza.

Não afirmar zero flags: Grim lê entity data e release, e cancelamento PacketEvents não o oculta automaticamente. Fallback de bloqueio no servidor e pose para observadores é candidato explícito do pedido, mas também exige testar tipos de dano, ângulo, sons, durabilidade e cooldown.

Velocidade de sneak sem estado local de uso é a intenção do pedido; o valor efetivo depende de atributos/USE_EFFECTS. Medir nas três versões, não declarar ×0,3 universal sem teste. No mod, apenas pose, sem mudar estado de uso ou RELEASE_USE_ITEM.

Fonte dos valores e linhas em RECON.md. Perfil 1.7 e semântica completa do componente BLOCKS_ATTACKS ainda precisam de implementação e validação.
