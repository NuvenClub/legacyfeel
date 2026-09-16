# HANDOFF — 15/09/2026 — primeiro incremento

## Estado

- Aprovação da Fase 1 recebida.
- Experimento criado em `legacyfeel`, fora do repositório NuvenClub, que permaneceu somente como referência de estrutura.
- Pacote `br.club.nuven.legacyfeel`, servidor `NuvenClub`, licença MIT.
- Laboratório direto Paper 26.2 em `127.0.0.1:25565`, com tradução até 1.8.9.

## Implementado

- Mod Fabric 26.2 com configuração persistente, tecla F8 e câmera imediata ao agachar.
- Troca visual de item configurável (`legacy`, `instant` ou 1–20 ticks); o padrão 0.4 coincide com o código 1.8.9 consultado.
- Handshake `legacyfeel:handshake` com JSON UTF-8 enquadrado pelo comprimento VarInt padrão do Fabric.
- Envio do `HELLO` aguarda o registro do canal, com tentativas limitadas a cinco segundos; `forceOff` passa a valer em runtime.
- Plugin Paper com classificação do cliente, diagnóstico, kit, troca de armadura, remoção de sweep e velocidade de ataque sem recarga.
- Espada moderna com animação de bloqueio e fórmula histórica de dano `(dano + 1) / 2` aplicada no servidor.
- Scripts de instalação, build e início com checksums de Paper, ViaVersion, ViaBackwards, ViaRewind, PacketEvents e Grim.

## Verificado

- Build do plugin e seus testes unitários: passou.
- Build do mod Fabric: passou.
- Feature 2 marcada como N/A: o modelo 26.2 já seleciona a pose de agachamento de forma binária.
- Paper 26.2 iniciou e encerrou de forma limpa.
- Seis plugins foram habilitados; `/legacyfeel stats` respondeu no console.
- ViaVersion reconheceu protocolo 26.2/776.

## Pendente de teste gráfico

- Entrada real do cliente 1.8.9 e do cliente Fabric 26.2.
- Comparação visual da câmera e confirmação do handshake após o registro do canal.
- Combate entre dois clientes, knockback, armadura e Grim sob latência.
- Espelho visual de escudo ao agachar, mantido desligado.
- Grim avisou que ViaBackwards em servidor 1.21.2+ tem suporte incompleto para veículos antigos. Isso não impede o teste inicial de combate, mas entra na matriz de compatibilidade.

## Próximos passos

1. Executar a matriz manual 1.8.9 × 26.2 descrita no README do laboratório, incluindo os três modos de equipar.
2. Corrigir qualquer diferença observada no handshake/câmera/mão e medir o comportamento de bloqueio.
3. Depois da comparação direta, adicionar Velocity com forwarding legacy para validar a topologia completa do NuvenClub.
