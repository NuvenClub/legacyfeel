# Ambiente de testes — ainda não implementado

A Fase 2 aguarda aprovação da topologia corrigida em ../docs/APPROVAL.md. Não existe comando up funcional nesta entrega de reconhecimento.

Proposta: proxy em 127.0.0.1:25565; Paper na rede privada em 25566; ViaVersion/ViaBackwards/ViaRewind, PacketEvents e Grim apenas backend. Forwarding legacy para aceitar cliente 1.8.9. Offline somente em localhost.

Docker CLI encontrado, daemon indisponível. Alternativa futura: jars oficiais do Paper/Velocity pela API Fill e Java25, com backend ligado apenas a localhost. Não foram gerados segredos, aceitos EULAs por configuração nem iniciados serviços.
