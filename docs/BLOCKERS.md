# BLOCKERS — 15/09/2026

| ID | Situação | Próximo passo |
|---|---|---|
| B01 | Aprovação obrigatória ao fim da Fase 1 | Revisar APPROVAL.md e MIXINS.md, preencher §0 |
| B02 | Topologia modern não atende 1.8.9 | Aprovar forwarding legacy com isolamento de backend |
| B03 | Regra de pacotes contradiz supressão use_item | Aprovar proposta puramente visual ou formalizar exceção e seus testes |
| B04 | Descarte de release “acima do Grim” não garante invisibilidade e pode desalinhar previsão | Teste dirigido com par Grim/PacketEvents fixo; avaliar fallback |
| B05 | Não encontrada release Grim marcada para 26.2; alpha usa dependência snapshot | Decidir alpha de teste ou aguardar release; sem trocar silenciosamente |
| B06 | Docker daemon indisponível nesta sessão | Iniciar ambiente na Fase 2 ou seguir alternativa Java local |
| B07 | Perfil 1.7.10, áudio e compatibilidade dos launchers não validados | Medir antes de anunciar fidelidade/suporte |
| B08 | Commit inicial recusado: Git sem user.name/user.email | Usuário informar identidade de autoria; arquivos já preparados no índice, sem inventar identidade |

## Resolvidos

- Java global 21 insuficiente: instalado Java25 isolado em .tools, sem mudança global.
- Wrapper permaneceu no download: interrompido; distribuição Gradle9.5.1 baixada e hash verificado; genSources de ambas versões passou.
- Métodos históricos ausentes: substituídos no plano pelos nomes efetivamente decompilados.

Nenhuma feature consumiu o limite de duas horas, pois a fase de implementação ainda não começou.
