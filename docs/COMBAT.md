# COMBAT — estado do laboratório

O incremento aprovado está implementado no laboratório. O servidor permanece autoritativo para dano.

## Perfil 1.8 medido nos fontes

- Bloqueio: (1+dano)/2 antes de armadura/encantamentos, condicionado pelo tipo de dano. Diverge do dano/2 solicitado; corrigir mediante aprovação em APPROVAL.md.
- KB básico: velocidade anterior de todos os eixos /2; impulso horizontal de magnitude 0,4 na direção de repulsão; vertical +0,4 limitada a 0,4. Tratar direção degenerada e resistência conforme fonte.
- KB de sprint/encantamento: +0,5 horizontal por nível efetivo, +0,1 vertical; atacante X/Z ×0,6 e sprint desligado. Não aplicar impulso duas vezes em eventos diferentes.
- A janela de invulnerabilidade foi zerada por pedido de golpes consecutivos em torno de 9 CPS; `ATTACK_SPEED` permanece em 1024.
- Remover dano de sweep com evento específico; não cancelar interações genéricas.

## QoL

O escudo defende no servidor apenas com sneak. O cliente modificado mostra a pose local ao agachar e ignora defesa por clique direito. A espada continua atacando durante o uso para permitir blockhit.

O kit inclui machado de ferro com atributo visual total de 6 e o evento de dano fixa machados em 6 antes das reduções do alvo.

Não afirmar zero flags: Grim lê entity data e release, e cancelamento PacketEvents não o oculta automaticamente. Fallback de bloqueio no servidor e pose para observadores é candidato explícito do pedido, mas também exige testar tipos de dano, ângulo, sons, durabilidade e cooldown.

Velocidade de sneak sem estado local de uso é a intenção do pedido; o valor efetivo depende de atributos/USE_EFFECTS. Medir nas três versões, não declarar ×0,3 universal sem teste. No mod, apenas pose, sem mudar estado de uso ou RELEASE_USE_ITEM.

Fonte dos valores e linhas em RECON.md. Perfil 1.7 e semântica completa do componente BLOCKS_ATTACKS ainda precisam de implementação e validação.
