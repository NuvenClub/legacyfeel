# BLOCKERS — 15/09/2026

| ID | Situação | Próximo passo |
|---|---|---|
| B01 | Resolvido: aprovação recebida em 15/09/2026 | Desenvolvimento liberado |
| B02 | Topologia modern não atende 1.8.9 | Aprovar forwarding legacy com isolamento de backend |
| B03 | Regra de pacotes contradiz supressão use_item | Aprovar proposta puramente visual ou formalizar exceção e seus testes |
| B04 | Descarte de release “acima do Grim” não garante invisibilidade e pode desalinhar previsão | Teste dirigido com par Grim/PacketEvents fixo; avaliar fallback |
| B05 | Não encontrada release Grim marcada para 26.2; alpha usa dependência snapshot | Decidir alpha de teste ou aguardar release; sem trocar silenciosamente |
| B06 | Resolvido para comparação inicial | Laboratório Java/Paper local criado fora do NuvenClub |
| B07 | Perfil 1.7.10, áudio e compatibilidade dos launchers não validados | Medir antes de anunciar fidelidade/suporte |
| B08 | Resolvido pela identidade já configurada no repositório NuvenClub | Usada apenas no repositório local LegacyFeel |

## Resolvidos

- Java global 21 insuficiente: instalado Java25 isolado em .tools, sem mudança global.
- Wrapper permaneceu no download: interrompido; distribuição Gradle9.5.1 baixada e hash verificado; genSources de ambas versões passou.
- Métodos históricos ausentes: substituídos no plano pelos nomes efetivamente decompilados.

O primeiro incremento implementa o handshake, o perfil de câmera ao agachar, o kit e regras básicas do laboratório. Testes com clientes gráficos ainda são necessários.
