# Instruções para o Claude Code

Projeto: Sistema de Clínica com Agendamento em microsserviços (Trabalho 4 — Unifor ADS).
O dono do projeto é o Daniel, estudante. Ele precisa **entender e apresentar** o código, então simplicidade vale mais que sofisticação.

## Antes de qualquer coisa
1. Leia o `PLANO.md` inteiro.
2. Veja na seção **7. Status** qual é o próximo checkpoint pendente.
3. Trabalhe **somente nesse checkpoint**.

## Como trabalhar em cada checkpoint
1. Diga em poucas linhas o que vai fazer.
2. Implemente seguindo exatamente os **contratos da seção 4** e os **padrões da seção 3**.
3. Rode os testes do "Pronto quando" e mostre o resultado.
4. Se passou: marque `[x]` nas tarefas e ☑ na tabela de status, faça o commit com a mensagem indicada.
5. **Pare** e entregue um resumo curto:
   - o que foi feito e em quais arquivos
   - como rodar o que foi feito
   - 2 ou 3 pontos do código que o Daniel deve saber explicar ao professor
6. Espere o Daniel dizer "próximo" antes de começar o checkpoint seguinte.

## Regras
- Não mude rotas, nomes de campos, portas ou formatos de JSON do `PLANO.md` sem perguntar.
- Não instale nada globalmente no Mac (Homebrew, etc.) sem o Daniel aprovar.
- Não adicione bibliotecas além das listadas no plano.
- Não faça `git push` sem o Daniel pedir.
- Se um teste falhar, corrija antes de marcar o checkpoint como feito. Se travar, explique o erro e pergunte.
- Comentários e mensagens de erro em português.
- Máquina: MacBook Air M1 (arm64) — use imagens Docker e dependências compatíveis.
