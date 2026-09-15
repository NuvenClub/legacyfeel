# PROTOCOL — rascunho de reconhecimento

Canal proposto legacyfeel:handshake, versão 1; não implementado. Mensagens HELLO, WELCOME e POLICY conforme REQUEST.md §5.5.

## Wire format proposto

JSON UTF-8 puro como corpo de custom payload, sem writeUTF e sem prefixo VarInt interno. O codec Fabric deve consumir/escrever exatamente o corpo; o plugin recebe os mesmos bytes. Máximo 8191 bytes, limites adicionais para strings, lista de features, profundidade e quantidade de chaves. Rejeitar UTF-8 inválido, JSON inválido, tipos inesperados e versões não suportadas, sem stacktrace por spam.

## Ordem e estado

- Registrar codecs serverboundPlay e clientboundPlay antes da conexão.
- JOIN da API resolvida precede minecraft:register: agendar HELLO para tick posterior e/ou aguardar registro no servidor antes de responder. Não responder sem registro **e** HELLO válido.
- Guardar ClientInfo por UUID em memória; limpar quit. Rate limit 1 HELLO/2s; timeout de classificação 5s, sem bloquear HELLO válido tardio.
- WELCOME inicia estado: rules ausente/chave ausente → false. POLICY atualiza apenas rules presentes. forceOff precisa de semântica definida: proposta substituir conjunto quando presente, manter quando ausente.
- Reset completo de políticas em desconexão/troca de backend. Reanúncio e evento real de mudança de backend ainda precisam de teste com Velocity; não supor que toda troca dispara um JOIN novo.
- Sem plugin: features locais operam, regras de espelho false, sem retries contínuos.
- getPlayerVersion(UUID) desconhecido/negativo não entra em VIA_LEGACY. Considerar ProtocolVersion conhecido ≤47; UNKNOWN joga com mesmas regras de todos.

## Testes pendentes

HELLO antes/depois de registro, 50 payloads inválidos, excesso de tamanho, desconexão no envio, política parcial, reconexão e troca real de backend, ausência do plugin, clientes sem mod. Meta local <200ms depende de medição; não foi medida.
