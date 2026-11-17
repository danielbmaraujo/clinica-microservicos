# PLANO — Sistema de Clínica com Agendamento (Microsserviços)

Trabalho 4 · Daniel Barbosa Melhado de Araújo · Unifor ADS

Objetivo: sistema **simples, mas funcionando de ponta a ponta**, no estilo microsserviços, igual ao diagrama C4 entregue.
Cada serviço é um processo separado, em linguagem diferente, com banco próprio, e se comunica **pela rede via HTTP/REST + JSON**.

---

## 1. Visão geral

| Parte | Linguagem / Stack | Endereço | Banco |
|---|---|---|---|
| Frontend | React (Vite) | http://localhost:3000 | — |
| Serviço de Pacientes | Python 3.11+ · FastAPI · SQLAlchemy | http://localhost:8001 | MySQL `db_pacientes` · localhost:3308 |
| Serviço de Agendamento | Java 17 · Spring Boot 3 · Maven · JPA | http://localhost:8002 | MySQL `db_agendamento` · localhost:3307 |
| Serviço de Notificações | Node.js 20 · Express · Mongoose | http://localhost:8003 | MongoDB `db_notificacoes` · localhost:27017 |

Comunicação entre serviços:

- Frontend → Pacientes, Agendamento e Notificações (HTTP)
- Agendamento → Pacientes: `GET /pacientes/{id}` (valida se o paciente existe e está ativo)
- Agendamento → Notificações: `POST /notificacoes` (avisa marcação, remarcação e cancelamento)
- **Nenhum serviço acessa o banco de outro.**

---

## 2. Estrutura de pastas

Repositório próprio (github.com/danielbmaraujo/clinica-microservicos). A raiz do repositório é a pasta do projeto:

```
clinica-microservicos/
├── CLAUDE.md
├── PLANO.md
├── README.md
├── .gitignore
├── docker-compose.yml          # só os 3 bancos
├── servico-pacientes/          # Python
├── servico-agendamento/        # Java
├── servico-notificacoes/       # Node.js
└── frontend/                   # React
```

---

## 3. Padrões obrigatórios (valem para todos os serviços)

1. **JSON sempre em camelCase** (`dataNascimento`, `pacienteId`), inclusive no Python.
2. **Erros sempre no formato** `{"erro": "mensagem em português"}` com o status HTTP certo:
   400 dado inválido/regra violada · 404 não encontrado · 409 conflito · 503 serviço dependente fora do ar.
3. **Todo serviço tem `GET /health`** → `{"status": "ok", "servico": "<nome>"}`.
4. **URLs de outros serviços e dados de banco vêm de variáveis de ambiente**, com valor padrão para rodar local.
   Ex.: `PACIENTES_URL=http://localhost:8001`, `NOTIFICACOES_URL=http://localhost:8003`.
5. **CORS liberado para `http://localhost:3000`** em todos os serviços.
6. Datas: `YYYY-MM-DD`; data e hora: `YYYY-MM-DDTHH:MM` (ex.: `2026-11-20T09:00`).
7. Código simples, comentários curtos em português, **sem bibliotecas além das listadas** neste plano.
8. Cada serviço cria suas tabelas sozinho ao iniciar (SQLAlchemy `create_all`, JPA `ddl-auto=update`).

---

## 4. Contratos das APIs (não alterar sem autorização do Daniel)

### 4.1 Serviço de Pacientes — :8001

Paciente:
```json
{ "id": 1, "nome": "Ana Souza", "cpf": "12345678909", "dataNascimento": "1995-04-10",
  "telefone": "85999990000", "email": "ana@email.com", "ativo": true }
```

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| POST | `/pacientes` | Cadastrar (sem `id` e `ativo`) | 201 paciente · 400 CPF inválido · 409 CPF já cadastrado |
| GET | `/pacientes?busca=` | Listar; `busca` filtra por nome ou CPF (opcional) | 200 lista |
| GET | `/pacientes/{id}` | Detalhes | 200 · 404 |
| PUT | `/pacientes/{id}` | Atualizar dados cadastrais | 200 · 404 · 409 |
| DELETE | `/pacientes/{id}` | **Desativa** (`ativo=false`), não apaga | 204 · 404 |

Regras: CPF com 11 dígitos numéricos e dígitos verificadores válidos; CPF único; nome obrigatório.

### 4.2 Serviço de Agendamento — :8002

Profissional:
```json
{ "id": 1, "nome": "Dr. Carlos Lima", "especialidade": "Fisioterapia" }
```
Consulta:
```json
{ "id": 1, "pacienteId": 1, "profissionalId": 1, "dataHora": "2026-11-20T09:00",
  "status": "AGENDADA" }
```
Status possíveis: `AGENDADA`, `REMARCADA`, `CANCELADA`.

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| POST | `/profissionais` | Cadastrar profissional | 201 · 400 |
| GET | `/profissionais?especialidade=` | Listar (filtro opcional) | 200 lista |
| GET | `/horarios-livres?profissionalId=1&data=2026-11-20` | Horários livres do dia | 200 `["08:00","09:00",...]` · 404 profissional |
| POST | `/consultas` | Marcar `{pacienteId, profissionalId, dataHora}` | 201 · 400 · 404 · 409 · 503 |
| GET | `/consultas?pacienteId=&data=` | Listar (filtros opcionais) | 200 lista |
| GET | `/consultas/{id}` | Detalhes | 200 · 404 |
| PUT | `/consultas/{id}/remarcar` | Remarcar `{dataHora}` | 200 · 400 · 404 · 409 |
| PUT | `/consultas/{id}/cancelar` | Cancelar | 200 · 400 · 404 |

Regras de negócio:
- Horário de atendimento: **08:00 às 17:00, de hora em hora** (último horário 17:00), segunda a sexta.
- `dataHora` precisa ser hora cheia, dentro do expediente e **no futuro** → senão 400.
- **Sem conflito**: o mesmo profissional não pode ter duas consultas não canceladas no mesmo horário → 409.
- Antes de marcar, chama `GET {PACIENTES_URL}/pacientes/{id}`:
  404 → responde 404 "Paciente não encontrado" · `ativo=false` → 400 "Paciente inativo" · serviço fora → 503.
- Consulta cancelada não pode ser remarcada nem cancelada de novo → 400.
- Depois de marcar, remarcar ou cancelar, chama `POST {NOTIFICACOES_URL}/notificacoes`.
  **Se o Notificações estiver fora do ar, a operação continua valendo** (só registra um aviso no log).

### 4.3 Serviço de Notificações — :8003

Notificação:
```json
{ "id": "6740f1...", "pacienteId": 1, "consultaId": 1, "tipo": "CONFIRMACAO",
  "mensagem": "Sua consulta foi marcada para 20/11/2026 às 09:00.",
  "dataEnvio": "2026-11-16T15:30:00Z" }
```
Tipos: `CONFIRMACAO`, `REMARCACAO`, `CANCELAMENTO`, `LEMBRETE`.

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| POST | `/notificacoes` | Registrar `{pacienteId, consultaId, tipo, mensagem}` | 201 · 400 |
| GET | `/notificacoes/paciente/{pacienteId}` | Listar avisos do paciente (mais recentes primeiro) | 200 lista |

O "envio" é simulado: grava no banco e escreve no console (`[NOTIFICAÇÃO] paciente 1: ...`).

### 4.4 Frontend — :3000

Três telas, com menu simples no topo:
1. **Pacientes**: formulário de cadastro + lista com busca + botão desativar.
2. **Consultas**: escolher paciente → profissional → data → horário livre → marcar; lista de consultas com botões remarcar e cancelar.
3. **Avisos**: escolher paciente e ver as notificações dele.

Mostrar a mensagem de `erro` da API na tela quando algo falhar. Usar `fetch` (sem axios). CSS simples, sem framework de UI.

---

## 5. Checkpoints

Regra: **um checkpoint por vez**. Só avança quando o "Pronto quando" passar e o Daniel confirmar.
Ao terminar cada um: marcar `[x]`, fazer o commit com a mensagem indicada e parar.

### ☑ CP0 — Preparar ambiente
- [x] Verificar instalados no Mac: `python3 --version` (3.11+), `java -version` (17+), `mvn -v`, `node -v` (20+), `docker -v`
- [x] Listar o que falta e passar ao Daniel o comando de instalação (Homebrew) — **não instalar sem ele aprovar**
- [x] Criar as pastas da seção 2 na raiz do repositório e conferir o `.gitignore`

Pronto quando: todas as ferramentas respondem com a versão.
Commit: `chore: estrutura inicial do trabalho 4 (microsserviços)`

### ☑ CP1 — Bancos de dados
- [x] `docker-compose.yml` com 3 containers: `mysql-pacientes` (3308 → 3306 no container; a 3306 do Mac já é usada por um MySQL local), `mysql-agendamento` (3307), `mongo-notificacoes` (27017), com volumes
- [x] Usuário/senha simples de desenvolvimento (`clinica` / `clinica123`), banco já criado no MySQL

Pronto quando: `docker compose up -d` e `docker compose ps` mostra os 3 rodando.
Commit: `feat: docker-compose com os bancos de cada serviço`

### ☑ CP2 — Serviço de Pacientes (Python)
- [x] FastAPI + SQLAlchemy + PyMySQL + uvicorn, `requirements.txt`
- [x] Todas as rotas da seção 4.1, validação de CPF, `/health`, CORS
- [x] Popular 3 pacientes de exemplo se o banco estiver vazio

Pronto quando:
```bash
curl localhost:8001/health
curl -X POST localhost:8001/pacientes -H "Content-Type: application/json" \
  -d '{"nome":"Teste","cpf":"52998224725","dataNascimento":"2000-01-01","telefone":"85900000000","email":"t@t.com"}'
curl localhost:8001/pacientes
# repetir o POST com o mesmo CPF → 409
```
Commit: `feat(pacientes): serviço de pacientes em Python/FastAPI`

### ☑ CP3 — Agendamento, parte 1: profissionais e horários (Java)
- [x] Projeto Spring Boot 3 (Maven) com Web, Data JPA, MySQL Driver, Validation
- [x] Entidade Profissional + rotas `/profissionais`, `/horarios-livres`, `/health`, CORS
- [x] Popular 3 profissionais de exemplo se vazio
- [x] Tratamento global de erros no formato `{"erro": ...}`

Pronto quando:
```bash
curl localhost:8002/health
curl localhost:8002/profissionais
curl "localhost:8002/horarios-livres?profissionalId=1&data=<próximo dia útil>"
```
Commit: `feat(agendamento): profissionais e horários livres em Java/Spring Boot`

### ☑ CP4 — Agendamento, parte 2: consultas + chamada ao Pacientes
- [x] Entidade Consulta + rotas `/consultas` (marcar, listar, detalhar, remarcar, cancelar)
- [x] Todas as regras de negócio da seção 4.2
- [x] Cliente HTTP (`RestClient`) que chama `GET /pacientes/{id}`, com URL vinda de `PACIENTES_URL`

Pronto quando:
```bash
curl -X POST localhost:8002/consultas -H "Content-Type: application/json" \
  -d '{"pacienteId":1,"profissionalId":1,"dataHora":"<dia útil futuro>T09:00"}'   # 201
# mesmo horário de novo → 409
# pacienteId 999 → 404
# parar o serviço de Pacientes e tentar marcar → 503
```
Commit: `feat(agendamento): consultas com validação de paciente via rede`

### ☑ CP5 — Serviço de Notificações (Node.js)
- [x] Express + Mongoose + cors, `package.json` com script `npm start`
- [x] Rotas da seção 4.3, `/health`, log no console ao registrar

Pronto quando:
```bash
curl localhost:8003/health
curl -X POST localhost:8003/notificacoes -H "Content-Type: application/json" \
  -d '{"pacienteId":1,"consultaId":1,"tipo":"CONFIRMACAO","mensagem":"Teste"}'
curl localhost:8003/notificacoes/paciente/1
```
Commit: `feat(notificacoes): serviço de notificações em Node.js/Express`

### ☑ CP6 — Integração Agendamento → Notificações
- [x] Agendamento chama `POST /notificacoes` ao marcar, remarcar e cancelar (URL de `NOTIFICACOES_URL`)
- [x] Mensagem em português com data e hora formatadas (`20/11/2026 às 09:00`)
- [x] Se Notificações estiver fora, a consulta é salva mesmo assim (log de aviso)

Pronto quando: marcar uma consulta e ver o aviso em `GET /notificacoes/paciente/1`; parar o Notificações, marcar outra e receber 201 mesmo assim.
Commit: `feat: agendamento envia notificações pela rede`

### ☑ CP7 — Frontend (React)
- [x] Projeto Vite + React na porta **3000** (`vite.config.js`)
- [x] Arquivo `src/api.js` com as 3 URLs base e as funções de chamada
- [x] As 3 telas da seção 4.4

Pronto quando: pelo navegador dá para cadastrar um paciente, marcar uma consulta num horário livre, remarcar, cancelar e ver os avisos.
Commit: `feat(frontend): interface React consumindo os 3 microsserviços`

### ☐ CP8 — Fechamento e apresentação
- [ ] `README.md`: o que é o sistema, diagrama (link para o PDF), como rodar cada parte (ordem e comandos), tabela de portas
- [ ] Script `iniciar.sh` que sobe os bancos e mostra os comandos para abrir cada serviço em um terminal
- [ ] Roteiro de demonstração (5 minutos) no README: fluxo de marcar consulta + derrubar um serviço para mostrar que é independente
- [ ] Teste de ponta a ponta seguindo o roteiro, do zero

Pronto quando: o Daniel consegue subir tudo seguindo só o README.
Commit: `docs: README, script de inicialização e roteiro de demonstração`

---

## 6. Extras (só se sobrar tempo, depois do CP8)
- Botão "enviar lembrete" na consulta (tipo `LEMBRETE`)
- Dockerfile para cada serviço e tudo subindo no `docker-compose`
- Testes automatizados simples (pytest, JUnit, Jest)

---

## 7. Status

| Checkpoint | Status | Observações |
|---|---|---|
| CP0 Ambiente | ☑ | Python 3.13, Java 21, Maven 3.10 (via ~/.mavenrc usa Java 21), Node 22, Docker 29. MySQL Pacientes na porta 3308 |
| CP1 Bancos | ☑ | mysql:8.4 (3308 e 3307) e mongo:7 (27017). Mongo com usuário root `clinica` → usar `?authSource=admin` na conexão |
| CP2 Pacientes | ☑ | Rodar: `cd servico-pacientes && source venv/bin/activate && uvicorn main:app --port 8001 --reload`. Exemplos: Ana (id 1), Bruno (2), Carla (3) |
| CP3 Agendamento 1 | ☑ | Spring Boot 3.5.16, Java 17. Rodar: `cd servico-agendamento && mvn spring-boot:run` |
| CP4 Agendamento 2 | ☑ | Notificações ficam para o CP6. Timeout de 3 s na chamada ao Pacientes |
| CP5 Notificações | ☑ | Express 5, Mongoose 9. Rodar: `cd servico-notificacoes && npm install && npm start` |
| CP6 Integração | ☑ | `NotificacoesClient.java`; falha no aviso só gera WARN no log |
| CP7 Frontend | ☑ | React 19 + Vite 8. Rodar: `cd frontend && npm install && npm run dev`. Testado no navegador pelo Daniel |
| CP8 Fechamento | ☐ | |
