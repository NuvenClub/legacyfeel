# Aprovação da Fase 1

Reconhecimento pronto para revisão em 15/09/2026. Este arquivo contém **propostas**, não decisões já aprovadas.

## Campos pendentes

ID/namespace sugeridos legacyfeel; confirmar nome do servidor, pacote Java e licença. Recomendação: MIT para implementação original, sem copiar código dos mods GPL/ARR. Não inferir nome dos outros projetos existentes nesta pasta.

## Proposta recomendada

1. Manter alvo 26.2 e corrigir toolchain conforme build verificado: Java25, Gradle9.5.1, Loom1.17.21, Loader0.19.5, API0.160.0+26.2. Adicionar YACL/Mod Menu só na fase de implementação.
2. Usar forwarding legacy no Velocity, Via somente backend, Paper sem porta publicada e proxy limitado a localhost nos testes. Não adicionar BungeeGuard sem autorização de dependência extra.
3. Preservar rigorosamente pacotes vanilla no mod: implementar só pose do escudo; deixar supressão de use_item desabilitada e não implementada até revisão explícita. Isso deixa a correção da previsão/uso do escudo a cargo do servidor. Se isso não for suficiente, documentar e revisar o desenho, sem esconder diferença no teste.
4. Presets 1.8 mantêm hurt tilt/flash e fórmula de equipar medida; opções de limpeza/instant podem ficar em configuração personalizada, sem chamar de preset fiel. Aplicar deslocamento antigo só à câmera. Perfil 1.7 aguarda medição própria.
5. Corrigir bloqueio para (dano+1)/2 antes da armadura, e incluir desaceleração horizontal ×0,6 do atacante no modelo de KB. A alternativa dano/2 seria uma terceira exceção ao 1:1, que precisa de autorização explícita.
6. Tratar escudo forçado como experimento, sem garantia prévia de zero flags. Preferir avaliar primeiro o fallback servidor descrito em §6.6.1(6), mantendo a opção de API vanilla se os testes provarem estabilidade. Nenhuma isenção no Grim.
7. Aceitar testar a build alpha identificada do Grim e fixar PacketEvents compatível, ou aguardar uma release 26.2; não apresentar a stack como toda estável.
8. Corrigir a ordem de testes: a Fase 2 comprova proxy/vanilla/Via/Grim; comandos de LegacyFeel só entram na verificação após a Fase 3. Troca de backend e condições de handshake precisam de testes próprios.

## Outras ambiguidades documentadas

- Política: regras ausentes em WELCOME são false; POLICY faz merge só nas chaves presentes. Ausência em POLICY não deve apagar estado anterior. Ao trocar backend, limpar todo estado antigo.
- Redução de movimento no uso é definida por USE_EFFECTS em 26.2; não assumir multiplicadores universais sem ler componente.
- Pose de escudo não pode ser simplesmente sneak&&shield quando machado desativou escudo ou arco/comida/espada tem prioridade. O protocolo pode precisar anunciar estado efetivo, além da regra; especificar antes de adicionar mensagens.
- HELLO no JOIN ocorre antes do registro de canais na API resolvida. Prevenir resposta perdida sem enviar payload a clientes sem suporte.
- Modo MOD é autodeclarado, não comprovação de cliente confiável. Regras de combate nunca dependem dessa declaração.
- Equippable faz previsão local também; sucesso via 1.8.9 deve ser observado nos dois slots, não presumido por existir API no servidor.
- FIFO de pacotes de gameplay não inclui handshake, que é uma exceção explícita e necessária à comparação.

Aprovar estas correções e MIXINS.md libera a Fase 2; não representa aprovação de publicar, de copiar GPL/ARR ou de acrescentar dependências não permitidas.
