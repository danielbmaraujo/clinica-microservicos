# Sistema de Clínica com Agendamento — Microsserviços

Trabalho 4 · Daniel Barbosa Melhado de Araújo · Unifor ADS

Sistema simples de clínica para **cadastrar pacientes, marcar/remarcar/cancelar consultas e avisar o paciente**.
É dividido em **3 microsserviços independentes**, cada um em uma linguagem diferente, com **banco de dados próprio**,
conversando entre si **pela rede via HTTP/REST + JSON**. Um frontend em React usa os três.

Diagrama C4 da arquitetura: [docs/diagrama-c4.pdf](docs/diagrama-c4.pdf)

```mermaid
flowchart LR
    U([Usuário]) --> F[Frontend React<br/>:3000]
    F -->|HTTP| P[Pacientes<br/>Python/FastAPI<br/>:8001]
    F -->|HTTP| A[Agendamento<br/>Java/Spring Boot<br/>:8002]
    F -->|HTTP| N[Notificações<br/>Node/Express<br/>:8003]
    A -->|GET /pacientes/id| P
    A -->|POST /notificacoes| N
    P --- DP[(MySQL<br/>db_pacientes<br/>:3308)]
    A --- DA[(MySQL<br/>db_agendamento<br/>:3307)]
    N --- DN[(MongoDB<br/>db_notificacoes<br/>:27017)]
```

## Portas

| Parte | Tecnologia | Endereço | Banco (porta no Mac) |
|---|---|---|---|
| Frontend | React 19 + Vite | http://localhost:3000 | — |
| Serviço de Pacientes | Python · FastAPI · SQLAlchemy | http://localhost:8001 | MySQL `db_pacientes` (3308) |
| Serviço de Agendamento | Java 17 · Spring Boot 3 · JPA | http://localhost:8002 | MySQL `db_agendamento` (3307) |
| Serviço de Notificações | Node.js · Express · Mongoose | http://localhost:8003 | MongoDB `db_notificacoes` (27017) |

> O MySQL de Pacientes usa a porta **3308** no Mac porque a 3306 já é usada por um MySQL local.

Comunicação entre os serviços:
- **Agendamento → Pacientes**: `GET /pacientes/{id}` antes de marcar (paciente existe? está ativo?). Se o Pacientes estiver fora → **503**.
- **Agendamento → Notificações**: `POST /notificacoes` ao marcar, remarcar e cancelar. Se o Notificações estiver fora → a consulta **continua valendo** (só um aviso no log).
- **Nenhum serviço acessa o banco de outro.**

## Pré-requisitos

- Python 3.11+ · Java 17+ · Maven · Node.js 20+ · Docker Desktop (aberto)

## Como rodar

### Jeito rápido

```bash
./iniciar.sh
```

O script sobe os bancos, espera ficarem prontos, instala as dependências (só na primeira vez)
e mostra os 4 comandos para abrir cada serviço em um terminal.

### Passo a passo (manual)

Abra um terminal na raiz do projeto e siga **nesta ordem**:

**1. Bancos de dados** (Docker)
```bash
docker compose up -d
docker compose ps        # os 3 devem estar "Up"
```
Na primeira vez, espere uns 20 segundos para o MySQL terminar de iniciar.

**2. Serviço de Pacientes** (novo terminal)
```bash
cd servico-pacientes
python3 -m venv venv                # só na primeira vez
source venv/bin/activate
pip install -r requirements.txt     # só na primeira vez
uvicorn main:app --port 8001 --reload
```

**3. Serviço de Agendamento** (novo terminal)
```bash
cd servico-agendamento
mvn spring-boot:run
```

**4. Serviço de Notificações** (novo terminal)
```bash
cd servico-notificacoes
npm install     # só na primeira vez
npm start
```

**5. Frontend** (novo terminal)
```bash
cd frontend
npm install     # só na primeira vez
npm run dev
```

Abra **http://localhost:3000**.

Conferir se está tudo no ar:
```bash
curl localhost:8001/health
curl localhost:8002/health
curl localhost:8003/health
```

Cada serviço cria suas tabelas sozinho e já vem com dados de exemplo:
3 pacientes (Ana Souza, Bruno Costa, Carla Mendes) e 3 profissionais (Dr. Carlos Lima, Dra. Beatriz Rocha, Dr. Marcos Alves).

### Desligar / zerar

```bash
# Ctrl+C em cada terminal dos serviços, depois:
docker compose down        # para os bancos (mantém os dados)
docker compose down -v     # para os bancos e APAGA os dados (volta aos exemplos)
```

## Configuração (variáveis de ambiente)

Todas têm valor padrão para rodar local; só mude se precisar.

| Serviço | Variáveis (padrão) |
|---|---|
| Pacientes | `PORT` (8001), `MYSQLHOST` (localhost), `MYSQLPORT` (3308), `MYSQLUSER` (clinica), `MYSQLPASSWORD` (clinica123), `MYSQLDATABASE` (db_pacientes), `FRONTEND_URL` (vazio) |
| Agendamento | `PORT` (8002), `MYSQLHOST` (localhost), `MYSQLPORT` (3307), `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE` (db_agendamento), `PACIENTES_URL` (http://localhost:8001), `NOTIFICACOES_URL` (http://localhost:8003), `FRONTEND_URL` (vazio) |
| Notificações | `PORT` (8003), `MONGO_URL` (mongodb://clinica:clinica123@localhost:27017/db_notificacoes?authSource=admin), `FRONTEND_URL` (vazio) |
| Frontend | `VITE_PACIENTES_URL`, `VITE_AGENDAMENTO_URL`, `VITE_NOTIFICACOES_URL` (padrão: os 3 serviços em localhost) |

`FRONTEND_URL` é a origem do frontend publicado, liberada no CORS além de `http://localhost:3000`. Para publicar no Railway, veja o [DEPLOY.md](DEPLOY.md).

## APIs

Padrões em todos os serviços: JSON em camelCase · erros no formato `{"erro": "mensagem"}` · `GET /health` · CORS liberado para `http://localhost:3000` (e para `FRONTEND_URL`, se definida).

**Pacientes (:8001)** — documentação interativa em http://localhost:8001/docs
| Método | Rota | Descrição |
|---|---|---|
| POST | `/pacientes` | Cadastrar (201 · 400 CPF inválido · 409 CPF já cadastrado) |
| GET | `/pacientes?busca=` | Listar, filtrando por nome ou CPF |
| GET | `/pacientes/{id}` | Detalhes (404) |
| PUT | `/pacientes/{id}` | Atualizar (404 · 409) |
| DELETE | `/pacientes/{id}` | Desativa (`ativo=false`), não apaga (204) |

**Agendamento (:8002)**
| Método | Rota | Descrição |
|---|---|---|
| POST | `/profissionais` | Cadastrar profissional |
| GET | `/profissionais?especialidade=` | Listar |
| GET | `/horarios-livres?profissionalId=1&data=2026-11-20` | Horários livres do dia |
| POST | `/consultas` | Marcar `{pacienteId, profissionalId, dataHora}` (201 · 400 · 404 · 409 · 503) |
| GET | `/consultas?pacienteId=&data=` | Listar |
| GET | `/consultas/{id}` | Detalhes |
| PUT | `/consultas/{id}/remarcar` | Remarcar `{dataHora}` |
| PUT | `/consultas/{id}/cancelar` | Cancelar |

Regras: atendimento de segunda a sexta, 08:00 às 17:00, de hora em hora, sempre no futuro;
um profissional não pode ter duas consultas no mesmo horário; consulta cancelada não pode ser remarcada.

**Notificações (:8003)**
| Método | Rota | Descrição |
|---|---|---|
| POST | `/notificacoes` | Registrar `{pacienteId, consultaId, tipo, mensagem}` |
| GET | `/notificacoes/paciente/{pacienteId}` | Avisos do paciente (mais recentes primeiro) |

O envio é simulado: grava no MongoDB e escreve no console `[NOTIFICAÇÃO] paciente 1: ...`.

## Roteiro de demonstração (5 minutos)

Antes: rode `./iniciar.sh`, abra os 4 terminais lado a lado e o navegador em http://localhost:3000.

**1. Arquitetura (30 s)** — Mostrar o diagrama C4 e a tabela de portas: 3 serviços, 3 linguagens, 3 bancos,
comunicação só por HTTP.

**2. Cadastrar paciente (40 s)** — Tela **Pacientes**:
- Cadastrar com CPF `12345678900` → aparece **"CPF inválido"** (erro vindo do Python).
- Cadastrar "Maria Demo" com CPF `11122233396` → aparece na lista.

**3. Marcar consulta (1 min)** — Tela **Consultas**:
- Escolher Maria Demo → Dr. Carlos Lima → próximo dia útil → horário 09:00 → **Marcar consulta**.
- Mostrar que o 09:00 **sumiu** dos horários livres.
- Explicar: o Agendamento (Java) perguntou ao Pacientes (Python) **pela rede** se a Maria existe e está ativa.

**4. Notificação (40 s)**
- Mostrar no terminal do Notificações a linha `[NOTIFICAÇÃO] paciente ...: Sua consulta foi marcada para ...`.
- Tela **Avisos** → Maria Demo → aparece a confirmação.
- Voltar em Consultas, **Remarcar** para 10:00 e depois **Cancelar**. Em Avisos aparecem os 3 avisos.

**5. Derrubar o Notificações (1 min)** — mostra que os serviços são independentes:
- `Ctrl+C` no terminal do Notificações.
- Marcar uma nova consulta → **funciona normalmente**. No terminal do Agendamento aparece o `WARN` "Não foi possível enviar a notificação".
- Tela Avisos → mostra "Serviço indisponível: http://localhost:8003". O resto do sistema continua funcionando.
- Religar com `npm start`.

**6. Derrubar o Pacientes (1 min)** — dependência obrigatória:
- `Ctrl+C` no terminal do Pacientes.
- Tentar marcar consulta → aparece **"Serviço de Pacientes indisponível"** (HTTP 503): sem saber se o paciente existe, não dá para marcar com segurança.
- A lista de consultas e os avisos **continuam funcionando**, porque não dependem do Pacientes.
- Religar com `uvicorn main:app --port 8001 --reload` e marcar de novo → funciona.

**Fechamento (10 s)** — Cada serviço pode cair, ser atualizado ou trocar de banco sem derrubar os outros,
desde que mantenha o contrato da API.

## Estrutura

```
clinica-microservicos/
├── docker-compose.yml       # os 3 bancos
├── iniciar.sh               # sobe os bancos e mostra os comandos
├── docs/diagrama-c4.pdf
├── servico-pacientes/       # Python · FastAPI
├── servico-agendamento/     # Java · Spring Boot
├── servico-notificacoes/    # Node.js · Express
└── frontend/                # React · Vite
```
