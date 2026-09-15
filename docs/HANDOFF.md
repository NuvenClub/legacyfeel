# HANDOFF — 15/09/2026 — sessão 1

## Estado

- Fase atual: 1, reconhecimento estático entregue para aprovação. Fase 0: diretórios criados.
- Features concluídas: nenhuma. Não há jar instalável, servidor configurado ou código de mixin.
- Em andamento: decisão humana sobre correções em APPROVAL.md e campos da §0.

## O que funciona (testado)

- Java25 local e Gradle9.5.1, hashes dos downloads conferidos.
- genSources 26.2 com Loom1.17.21 passou; fontes extraídos para reference/minecraft-26.2.
- genSources 1.8.9 com Legacy Fabric build604 passou; fontes extraídos para reference/minecraft-1.8.9.
- Clones das quatro referências e ferramentas de rede registrados com commit em evidence/reference-commits.tsv.
- Documentos e tabela de mixins fundamentados em leitura de fontes locais.

## O que NÃO foi testado por mim e por quê

- Todas as features, handshake, partidas, FPS, Grim e proxy: fases de implementação ainda não autorizadas após o checkpoint.
- Docker daemon não disponível; consulta falhou. Nenhum serviço foi iniciado.
- Lunar/Feather/Modrinth App/Prism/Sodium/Iris/Voice Chat: clientes não executados.
- 1.7.10: fonte ainda não gerado; não anunciar fidelidade.
- Áudio: comparação de hashes não encontrou os cinco arquivos antigos idênticos; audição e resolução de eventos pendentes.

## Decisões tomadas (e por quê)

- Criar subpasta legacyfeel para preservar projetos preexistentes na raiz victor.
- Instalar ferramentas somente em .tools, sem mudar Java global.
- Usar template oficial CC0 para preparar apenas geração de fontes; nenhum código dos mods de referência importado.
- Manter 26.2, não portar automaticamente para 26.3.
- Não implementar input do escudo devido à contradição de pacotes no próprio pedido.
- Licença, pacote e nome do servidor NÃO foram escolhidos. As propostas em APPROVAL.md aguardam resposta.

## Bloqueios abertos → ver BLOCKERS.md

Modern forwarding não atende cliente1.8.9; hipótese de descarte acima do Grim não demonstrada; matriz Grim/PacketEvents inclui alpha/snapshot; dano do prompt diverge do fonte1.8.9.

## Próximos 3 passos concretos

1. Obter aprovação de APPROVAL.md/MIXINS.md e valores finais da §0. Não saltar esse checkpoint explícito do usuário.
2. Fixar par Grim/PacketEvents e build Velocity, preparar ambiente local com topologia aprovada e comprovar entrada dos clientes; decidir Docker ou Java local conforme disponibilidade.
3. Implementar plugin handshake/modes e depois mod handshake/config; validar fio JSON, registro de canais e troca de backend antes das features visuais.

## Comandos para retomar

Partindo de `C:/Users/euvic/OneDrive/Documents/ChatGPT/victor/legacyfeel`, PowerShell:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/java25/jdk-25.0.4.1+1').Path
Set-Location mod
../.tools/gradle/gradle-9.5.1/bin/gradle.bat genSources --console=plain --no-daemon
```

Fontes1.8.9: mesmo Gradle local a partir de `.tools/legacy-1.8.9`. Wrapper padrão disponível, mas seu download ficou parado e foi interrompido; distribuição local funcionou. Não executar exemplos runClient/up.sh do prompt ainda: não foram preparados.

## Observações para revisão

- O Javadoc completo salvo mostra startUsingItem(EquipmentSlot), apesar de a extração web não encontrá-lo.
- Networking resolvido usa serverboundPlay/clientboundPlay e anuncia registro de canais depois de JOIN; existe corrida no handshake do prompt.
- Preservar reference/ fora do Git/build; não redistribuir fontes do Minecraft.
- genSources não valida comportamento do produto; TESTING.md conserva checklist não executado.
- Revisão `git diff --cached --check` passou. Tentativa de commit `docs(recon): verificar fontes e registrar plano LegacyFeel` recusada por ausência de identidade Git. Arquivos estão preparados no índice; nenhum commit foi criado, nenhum remoto configurado. Não inventar nome/email do usuário.
