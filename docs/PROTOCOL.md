# PROTOCOL — versão 1 implementada

Canal `legacyfeel:handshake`, versão 1. Mensagens `HELLO`, `WELCOME` e `POLICY` conforme REQUEST.md §5.5.

## Wire format proposto

O codec padrão `ByteBufCodecs.stringUtf8(8191)` do Fabric/Minecraft escreve um comprimento VarInt seguido do JSON UTF-8. O plugin implementa o mesmo enquadramento. O decodificador rejeita comprimento negativo, acima de 8191 ou maior que o corpo disponível. JSON inválido é ignorado sem interromper o servidor.

## Ordem e estado

- Registrar codecs serverboundPlay e clientboundPlay antes da conexão.
- O cliente envia `HELLO` no evento de entrada. A corrida com `minecraft:register` permanece na lista de teste com cliente real; a próxima revisão deve adicionar retry único e limitado se a medição confirmar perda.
- Guardar ClientInfo por UUID em memória; limpar quit. Rate limit 1 HELLO/2s; timeout de classificação 5s, sem bloquear HELLO válido tardio.
- WELCOME inicia estado: rules ausente/chave ausente → false. POLICY atualiza apenas rules presentes. forceOff precisa de semântica definida: proposta substituir conjunto quando presente, manter quando ausente.
- Reset completo de políticas em desconexão/troca de backend. Reanúncio e evento real de mudança de backend ainda precisam de teste com Velocity; não supor que toda troca dispara um JOIN novo.
- Sem plugin: features locais operam, regras de espelho false, sem retries contínuos.
- getPlayerVersion(UUID) desconhecido/negativo não entra em VIA_LEGACY. Considerar ProtocolVersion conhecido ≤47; UNKNOWN joga com mesmas regras de todos.

## Testes pendentes

HELLO antes/depois de registro, 50 payloads inválidos, excesso de tamanho, desconexão no envio, política parcial, reconexão e troca real de backend, ausência do plugin, clientes sem mod. Meta local <200ms depende de medição; não foi medida.
