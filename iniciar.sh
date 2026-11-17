#!/usr/bin/env bash
# Sobe os bancos de dados, prepara as dependências (só na primeira vez)
# e mostra os comandos para abrir cada serviço em um terminal.
set -e
cd "$(dirname "$0")"
RAIZ="$(pwd)"

echo "==> Subindo os bancos de dados (Docker)..."
docker compose up -d

echo "==> Aguardando os bancos ficarem prontos..."
until docker exec mysql-pacientes mysqladmin ping -h 127.0.0.1 -uclinica -pclinica123 --silent >/dev/null 2>&1; do sleep 2; done
echo "    mysql-pacientes    OK (porta 3308)"
until docker exec mysql-agendamento mysqladmin ping -h 127.0.0.1 -uclinica -pclinica123 --silent >/dev/null 2>&1; do sleep 2; done
echo "    mysql-agendamento  OK (porta 3307)"
until docker exec mongo-notificacoes mongosh -u clinica -p clinica123 --quiet --eval "db.adminCommand('ping')" >/dev/null 2>&1; do sleep 2; done
echo "    mongo-notificacoes OK (porta 27017)"

echo "==> Conferindo dependências..."
if [ ! -d servico-pacientes/venv ]; then
  echo "    Instalando dependências do Pacientes (Python)..."
  python3 -m venv servico-pacientes/venv
  servico-pacientes/venv/bin/pip install -q --disable-pip-version-check -r servico-pacientes/requirements.txt
fi
if [ ! -d servico-notificacoes/node_modules ]; then
  echo "    Instalando dependências do Notificações (Node)..."
  (cd servico-notificacoes && npm install --silent)
fi
if [ ! -d frontend/node_modules ]; then
  echo "    Instalando dependências do Frontend (React)..."
  (cd frontend && npm install --silent)
fi
echo "    OK"

cat <<FIM

Bancos prontos! Agora abra 4 terminais e rode um comando em cada:

  1) Pacientes (Python) ...... http://localhost:8001
     cd "$RAIZ/servico-pacientes" && source venv/bin/activate && uvicorn main:app --port 8001 --reload

  2) Agendamento (Java) ...... http://localhost:8002
     cd "$RAIZ/servico-agendamento" && mvn spring-boot:run

  3) Notificações (Node) ..... http://localhost:8003
     cd "$RAIZ/servico-notificacoes" && npm start

  4) Frontend (React) ........ http://localhost:3000
     cd "$RAIZ/frontend" && npm run dev

Depois abra http://localhost:3000 no navegador.
Para desligar os bancos: docker compose down
FIM
